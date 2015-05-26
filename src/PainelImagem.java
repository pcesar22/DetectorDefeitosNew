import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

@SuppressWarnings("serial")
/*Painel de exibição de imagem.
 Esta classe foi customizada a fim de permitir que possamos 
 desenhar nela e exibir informações sobre a imagem a medida
 que passamos o mouse sobre ela
 */

public class PainelImagem extends JPanel {
    //Corte de eliminação de detalhes
    private Componente forma_geometrica;
    //Corte de seleção da área de interesse
    private Componente selecao;

    //Ultimas coordenadas do ponteiro do mouse. Quando indefinidas, são -1
    private int x_antigo = -1;
    private int y_antigo = -1;

    //Imagem sendo exibida
    private Image Display;          
    
	/* Imagem exibida de forma tratável pelo Opencv em diversas escalas de cores */

    private Mat imagem_inicial;
    private Mat imagem_HSV;
    private Mat imagem_gray;
    private Mat imagem_canny;

    /*Variáveis que dizem se estamos desenhando no momento e se
    estamos fazendo um corte de eliminação de detalhes ou de
    seleção de área de interesse
    */

    private boolean retira;
    private boolean seleciona;

    // Processador de imagens
    private ProcessadorImagem processador;
    
    /*Métodos para retornar a imagem a ser exibida de forma tratável pelo Opencv
     em diversas escalas de cores
     */

    public Mat retornaImagemInicial() {
        return imagem_inicial;
    }

    public Mat retornaImagemHSV() {
        return imagem_HSV;
    }

    public Mat retornaImagemGray() {
        return imagem_gray;
    }

    public Mat retornaImagemCanny() {
        return imagem_canny;
    }

    //Métodos para retornar os cortes de detalhes e de seleção
    public Componente getCorte() {
        return this.forma_geometrica;
    }

    public Componente getSelecao() {
        return this.selecao;
    }

    /***************************
     * *** FUNÇÕES BLACK BOX ****
     ***************************/

    //Função para definir o estado do mecanismo de desenho
    public boolean podeDesenhar(boolean ret, boolean sel) {
        if (imagem_inicial == null) {
            JOptionPane.showMessageDialog(null, "Imagem nula! Selecione uma imagem", "Erro", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        this.retira = ret;
        this.seleciona = sel;

        return true;
    }

    /* Função para converter uma imagem Mat do Opencv em uma BufferedImage */
    public BufferedImage converteMatBufferedImage(Mat input, boolean colorido) {
        byte[] data = new byte[input.rows() * input.cols() * (int) (input.elemSize())];
        input.get(0, 0, data);

        BufferedImage image = new BufferedImage(input.cols(), input.rows(), BufferedImage.TYPE_BYTE_GRAY);
        if (colorido) {
            image = new BufferedImage(input.cols(), input.rows(), BufferedImage.TYPE_3BYTE_BGR);
        }
        image.getRaster().setDataElements(0, 0, input.cols(), input.rows(), data);
        return image;
    }

    //Construtor da classe PainelImagem
    public PainelImagem() {

        retira = false;
        seleciona = true;

        processador = new ProcessadorImagem();

        setBorder(BorderFactory.createLineBorder(Color.black));

        forma_geometrica = new Componente();
        selecao = new Componente();
        Display = null;
        imagem_inicial = null;
        
        /*No momento em que clicamos na imagem, dependendo
          do estado do mecanismo de desenho, atualizamos os cortes e
          as posições anteriores do mouse 
         */

        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (retira) {
                    if (imagem_inicial != null) {
                        Point k = new Point(e.getX(), e.getY());
                        if (seleciona)
                            forma_geometrica.add(k);
                        else
                            selecao.add(k);

                        x_antigo = k.x;
                        y_antigo = k.y;
                        repaint();
                    } else {
                        JOptionPane.showMessageDialog(null, "Não é possível cortar uma imagem nula.", "Erro", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        
        	/*Ao passarmos o mouse sobre a imagem, mostramos
            algumas informações da imagem na barra de título da
        	interface gráfica*/
        addMouseMotionListener(new MouseAdapter() {


            /*No momento em que arrastamos o mouse na imagem, dependendo
            do estado do mecanismo de desenho, atualizamos os cortes e
            as posições anteriores do mouse 
           */
            public void mouseDragged(MouseEvent e) {
                if (retira && imagem_inicial != null) {
                    Point k = new Point(e.getX(), e.getY());
	            	
	            	/*Este trecho de código tem como objetivo gerar
	            	 cortes conexos, pois ao movermos o mouse, por vezes 
	            	 a sua posição não varia de forma contínua na tela,
	            	 havendo alguns saltos de posição (como teletransportes).
	            	 Assim, estamos adicionando os pontos intermediários entre
	            	 duas posições consecutivas do mouse no corte
	            	 */

                    if (x_antigo != -1) {
                        int inicial = x_antigo;
                        int fim = k.x;
                        int i = inicial;
                        while (i != fim) {
                            Point j = new Point();
                            j.x = i;
                            j.y = y_antigo;
                            if (seleciona)
                                forma_geometrica.add(j);
                            else
                                selecao.add(j);

                            if (inicial < fim) i += 1;
                            else i -= 1;
                        }

                        inicial = y_antigo;
                        fim = k.y;
                        i = inicial;

                        while (i != fim) {
                            Point j = new Point();
                            j.x = k.x;
                            j.y = i;
                            if (seleciona)
                                forma_geometrica.add(j);
                            else
                                selecao.add(j);

                            if (inicial < fim) i += 1;
                            else i -= 1;
                        }
                    }

                    x_antigo = k.x;
                    y_antigo = k.y;
                    repaint();
                }
            }
        });

    }

    /*Código para a eliminação de detalhes*/
    //Pinta de preto quando for retirado
    public void ApagaDetalhe(Mat input, Componente borda) {
        int cols = input.cols();
        int lin = input.rows();

        for (int i = 0; i < cols; i++) {
            int state = 0;
            ArrayList<org.opencv.core.Point> pra_apagar = new ArrayList<org.opencv.core.Point>();
            ArrayList<org.opencv.core.Point> reserva = new ArrayList<org.opencv.core.Point>();
            for (int j = 0; j < lin; j++) {
                org.opencv.core.Point k = new org.opencv.core.Point(i, j);
                if (borda.hasPoint(k) && state == 0) {
                    pra_apagar.add(k);
                    state = 1;
                } else if (!borda.hasPoint(k) && state == 1) {
                    state = 2;
                } else if (borda.hasPoint(k) && state == 2) {
                    pra_apagar.add(k);
                    state = 3;
                } else if (!borda.hasPoint(k) && state == 3) {
                    reserva.add(k);
                } else if (borda.hasPoint(k) && state == 3) {
                    pra_apagar.add(k);
                    for (org.opencv.core.Point bc : reserva) pra_apagar.add(bc);
                }

                if (state == 2) {
                    pra_apagar.add(k);
                }
            }
            if (state == 3) {
                for (org.opencv.core.Point k : pra_apagar) {
                    double cor[] = {0, 0, 0};
                    input.put((int) k.y, (int) k.x, cor);
                }
            }
        }


    }

    /*Código para eliminar as partes fora da região de interesse*/
    public void ApagaFora(Mat input, Componente borda) {
        int cols = input.cols();
        int lin = input.rows();

        for (int i = 0; i < cols; i++) {
            int state = 0;
            HashMap<org.opencv.core.Point, Boolean> pra_nao_apagar = new HashMap<org.opencv.core.Point, Boolean>();
            ArrayList<org.opencv.core.Point> reserva = new ArrayList<org.opencv.core.Point>();
            ArrayList<org.opencv.core.Point> reserva2 = new ArrayList<org.opencv.core.Point>();
            for (int j = 0; j < lin; j++) {
                org.opencv.core.Point k = new org.opencv.core.Point(i, j);
                if (borda.hasPoint(k) && state == 0) {
                    pra_nao_apagar.put(k, true);
                    state = 1;
                } else if (!borda.hasPoint(k) && state == 1) {
                    state = 2;
                } else if (borda.hasPoint(k) && state == 2) {
                    pra_nao_apagar.put(k, true);
                    for (org.opencv.core.Point bc : reserva2) pra_nao_apagar.put(bc, true);
                    state = 3;
                } else if (!borda.hasPoint(k) && state == 3) {
                    reserva.add(k);
                } else if (borda.hasPoint(k) && state == 3) {
                    pra_nao_apagar.put(k, true);
                    for (org.opencv.core.Point bc : reserva) pra_nao_apagar.put(bc, true);
                }

                if (state == 2) {
                    reserva2.add(k);
                }


            }

            for (int j = 0; j < lin; j++) {
                org.opencv.core.Point b = new org.opencv.core.Point(i, j);
                if (pra_nao_apagar.containsKey(b)) {

                } else {
                    double cor[] = {0, 0, 0};
                    input.put((int) b.y, (int) b.x, cor);
                }
            }
        }
    }

    /*Código que desenha a imagem sendo exibida e eventualmente o corte
    que está sendo feito*/
    public void paintComponent(Graphics g) {
        if (Display != null) {
            super.paintComponent(g);
            g.drawImage(Display, 0, 0, this);

            if (retira) {
                for (org.opencv.core.Point k : forma_geometrica.getCorpo()) {
                    g.setColor(Color.RED);
                    g.fillRect((int) k.x, (int) k.y, 1, 1);
                }

                for (org.opencv.core.Point k : selecao.getCorpo()) {
                    g.setColor(Color.BLUE);
                    g.fillRect((int) k.x, (int) k.y, 1, 1);
                }
            }
        }
    }

    // Imprime a quantidade de pixels;

    //Atualiza a imagem exibida diante de um corte
    public void atualizaImagemCorte() {
        if (imagem_inicial != null) {
            processador.setCorte(forma_geometrica);
            ApagaDetalhe(imagem_inicial, forma_geometrica);
            Imgproc.cvtColor(imagem_inicial, this.imagem_HSV, Imgproc.COLOR_RGB2HSV);
            Imgproc.cvtColor(imagem_inicial, this.imagem_gray, Imgproc.COLOR_RGB2GRAY);
            BufferedImage display_mat = converteMatBufferedImage(imagem_inicial, true);
            Display = new ImageIcon(display_mat).getImage();
            podeDesenhar(false, true);
            forma_geometrica = new Componente();
            repaint();
        } else {
            JOptionPane.showMessageDialog(null, "Imagem nula! Selecione uma imagem", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    //Atualiza a imagem exibida diante de uma seleção de região de interesse
    public void atualizaImagemProcessamento() {
        if (imagem_inicial != null) {
            ApagaFora(imagem_inicial, selecao);
            Imgproc.cvtColor(imagem_inicial, this.imagem_HSV, Imgproc.COLOR_RGB2HSV);
            Imgproc.cvtColor(imagem_inicial, this.imagem_gray, Imgproc.COLOR_RGB2GRAY);
            BufferedImage display_mat = converteMatBufferedImage(imagem_inicial, true);
            Display = new ImageIcon(display_mat).getImage();
            podeDesenhar(false, true);
            selecao = new Componente();
            repaint();
        } else {
            JOptionPane.showMessageDialog(null, "Imagem nula! Selecione uma imagem", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    /********************************
     * *** MÉTODOS PROCESSMAMENTO ****
     ********************************/

    //Realiza o processamento da imagem sendo exibida
    public BufferedImage executeProcessing(Mat input, double curvMin,
                                           double curvMax,
                                           double sigma, double distInf) {

        BufferedImage img = converteMatBufferedImage(input, true);

        BufferedImage returnImg = processador.processImage(img, curvMin,
                curvMax, sigma, distInf);
        Display = new ImageIcon(returnImg).getImage();
        repaint();

        return returnImg;
    }

    //Atualiza a imagem a ser exibida
    public void abreImagem(File a) throws IOException {

        // Procedimento para leitura da imagem -- MACACO -- CAGAVEL
        BufferedImage img = ImageIO.read(a);
        byte[] data = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
        this.imagem_inicial = new Mat(img.getHeight(), img.getWidth(), CvType.CV_8UC3);
        this.imagem_inicial.put(0, 0, data);

        // *** MODIFICACAO DO TAMANHO DA IMAGEM ***
        imagem_inicial = processador.scaleImage(imagem_inicial);
        // Imgproc.resize(imagem_inicial, imagem_inicial, new Size(800, 800));

        // *** ELIMINACAO DO RUIDO ***
        Imgproc.GaussianBlur(imagem_inicial, imagem_inicial, new Size(5, 5),
                0.8);

        this.imagem_HSV = imagem_inicial.clone();
        this.imagem_gray = imagem_inicial.clone();
        this.imagem_canny = imagem_inicial.clone();

        Imgproc.cvtColor(imagem_inicial, this.imagem_HSV, Imgproc.COLOR_RGB2HSV);
        Imgproc.cvtColor(imagem_inicial, this.imagem_gray,
                Imgproc.COLOR_RGB2GRAY);


        BufferedImage display_mat = converteMatBufferedImage(imagem_inicial,
                true);
        Display = new ImageIcon(display_mat).getImage(); // Ainda nao sei o que
        // isso faz

        // forma_geometrica = new Componente();
        repaint();

    }

}
