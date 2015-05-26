import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;

/*Esta classe é responsável pela extração de componentes de uma imagem 
 binarizada e por chamar a classe de processamento de curvas. É uma classe
 extremamente importante pois sem uma extração eficiente de componentes, 
 não existe processamento eficiente
 */

public class ProcessadorImagem {

	/*Corte de eliminação de detalhes, a ser considerado 
	no momento da geração de componentes*/

    private Componente corte;

    // Dimensoes da imagem
    private int IMAGE_WIDTH;
    private int IMAGE_HEIGHT;

    private int IMAGE_WIDTH_MAX = 850;
    private int IMAGE_HEIGHT_MAX = 700;

    // Construtor
    ProcessadorImagem() {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        corte = new Componente();
    }

    public void getDimension(Mat input) {
        this.IMAGE_WIDTH = input.width();
        this.IMAGE_HEIGHT = input.height();
    }

    public Mat scaleImage(Mat input) {
        Mat output = input;
        int newWidth, newHeight;
        getDimension(input);
        if (IMAGE_HEIGHT > IMAGE_WIDTH) {

            if (IMAGE_HEIGHT > IMAGE_HEIGHT_MAX) {
                newHeight = IMAGE_HEIGHT_MAX;
                newWidth = IMAGE_WIDTH * IMAGE_HEIGHT_MAX / IMAGE_HEIGHT;
            } else {
                newHeight = IMAGE_HEIGHT;
                newWidth = IMAGE_WIDTH;
            }
        } else {
            if (IMAGE_WIDTH > IMAGE_WIDTH_MAX) {
                newWidth = IMAGE_WIDTH_MAX;
                newHeight = IMAGE_HEIGHT * IMAGE_WIDTH_MAX / IMAGE_WIDTH;
            } else {
                newWidth = IMAGE_WIDTH;
                newHeight = IMAGE_HEIGHT;
            }
        }

        Imgproc.resize(output, input, new Size(newWidth, newHeight));
        return output;
    }

    public void setCorte(Componente corte) {
        this.corte = corte;
    }

    // Função para converter uma imagem Mat em uma BufferedImage
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

    // Métodos utilizados na binarização
    boolean ehPreto(double val) {
        double thresh = 150;
        if (val < thresh) return true;
        return false;
    }

    boolean ehBranco(double val) {
        double thresh = 150;
        if (val > thresh) return true;
        return false;
    }

    // Função que identifica fornteiras entre preto e branco na imagem
    public Mat encontra_fronteiras(Mat input) {
        int linhas = input.rows();
        int colunas = input.cols();

        Mat output = new Mat(input.rows(), input.cols(), CvType.CV_8UC3);

        double cor1[] = {0, 0, 0};
        double cor2[] = {255, 255, 255};

        for (int c = 0; c < colunas; c++) {
            for (int l = 0; l < linhas; l++) {

                if (l != linhas - 1 && l != 0 && c != colunas - 1 && c != 0) {

                    double atual = input.get(l, c)[0];

                    double baixo = input.get(l + 1, c)[0];
                    double cima = input.get(l - 1, c)[0];
                    double direita = input.get(l, c + 1)[0];
                    double esquerda = input.get(l, c - 1)[0];


                    if (ehBranco(atual) && ehPreto(baixo)) {
                        output.put(l, c, cor1);

                    } else if (ehPreto(atual) && ehBranco(baixo)) {
                        output.put(l, c, cor1);

                    } else if (ehBranco(atual) && ehPreto(cima)) {
                        output.put(l, c, cor1);

                    } else if (ehPreto(atual) && ehBranco(cima)) {
                        output.put(l, c, cor1);

                    } else if (ehBranco(atual) && ehPreto(direita)) {
                        output.put(l, c, cor1);

                    } else if (ehPreto(atual) && ehBranco(direita)) {
                        output.put(l, c, cor1);

                    } else if (ehBranco(atual) && ehPreto(esquerda)) {
                        output.put(l, c, cor1);

                    } else if (ehPreto(atual) && ehBranco(esquerda)) {
                        output.put(l, c, cor1);

                    } else {
                        output.put(l, c, cor2);
                    }
                } else {
                    output.put(l, c, cor2);
                }

            }
        }

        //Elimina os pontos do corte
        for (Point k : corte.getCorpo()) {
            Core.circle(output, k, 2, new Scalar(255, 255, 255), 2);

        }

        return output;
    }
	
	
	/* ****************************************** */
	/* METODOS PARA ENCONTRAR COMPONENTES CONEXOS */
	/* ****************************************** */

    // Encontra componente conexo e bota no ArrayList Argumento
    private void encontra_conexo(Mat a, int x, int y, ArrayList<Point> atual) {
        int numLinhas = (int) (a.size().height);
        int numColunas = (int) (a.size().width);

        //150 são os pontos já visitados e 255 são os pontos brancos
        if (a.get(y, x)[0] == 255 || a.get(y, x)[0] == 150.0) {

        } else {
            double[] cor = {150, 0, 0};
            a.put(y, x, cor);

            Point p = new Point(x, y);
            atual.add(p);

            boolean direita = x + 1 < numColunas;
            boolean esquerda = x - 1 >= 0;
            boolean cima = y + 1 < numLinhas;
            boolean baixo = y - 1 >= 0;
            boolean diagonal_1 = direita && cima;
            boolean diagonal_2 = direita && baixo;
            boolean diagonal_3 = esquerda && cima;
            boolean diagonal_4 = esquerda && baixo;

            if (direita) {
                encontra_conexo(a, x + 1, y, atual);
            }
            if (esquerda) {
                encontra_conexo(a, x - 1, y, atual);
            }
            if (cima) {
                encontra_conexo(a, x, y + 1, atual);
            }
            if (baixo) {
                encontra_conexo(a, x, y - 1, atual);
            }
            if (diagonal_1) {
                encontra_conexo(a, x + 1, y + 1, atual);
            }
            if (diagonal_2) {
                encontra_conexo(a, x + 1, y - 1, atual);
            }
            if (diagonal_3) {
                encontra_conexo(a, x - 1, y + 1, atual);
            }
            if (diagonal_4) {
                encontra_conexo(a, x - 1, y - 1, atual);
            }
        }
    }

    // Retorno uma arrayList de componentes Conexos de toda a figura
    public ArrayList<Componente> encontra_tudo(Mat a) {

        int numLinhas = (int) (a.size().height);
        int numColunas = (int) (a.size().width);

        ArrayList<Componente> todos;
        todos = new ArrayList<Componente>();

        for (int j = 0; j < numLinhas; j++) {
            for (int i = 0; i < numColunas; i++) {

                if (a.get(j, i)[0] == 0) {
                    ArrayList<Point> atual = new ArrayList<Point>();
                    encontra_conexo(a, i, j, atual);
                    todos.add(new Componente(atual));
                }
            }
        }
	    
	    
	    /*Elimina componentes com menos de 10 pontos da imagem
	     (eliminação de ruido)
	     */
        int thresh = 10;

        ArrayList<Componente> para_apagar = new ArrayList<Componente>();

        for (Componente b : todos) {
            ArrayList<Point> k = b.getCorpo();
            boolean apaga = elimina_detalhe(a, k, thresh);
            if (apaga) para_apagar.add(b);
        }
        for (Componente k : para_apagar) todos.remove(k);

        return todos;
    }

    // Elimina todos os componentes cujo tamanho for menor que thresh
    public boolean elimina_detalhe(Mat a, ArrayList<Point> componente, int thresh) {
        boolean deve_apagar = true;

        if (componente.size() > thresh) {
            deve_apagar = false;
        } else {
            deve_apagar = true;
        }

        for (Point k : componente) {
            if (deve_apagar) {
                double cor[] = {255, 255, 255};
                a.put((int) k.y, (int) k.x, cor);
            } else {
                double cor[] = {0, 0, 0};
                a.put((int) k.y, (int) k.x, cor);
            }
        }

        return deve_apagar;
    }
	
	
	/* ********************************* */
	/* REALIZA O PROCESSAMENTO DA IMAGEM */
	/* ********************************* */

    public BufferedImage processImage(BufferedImage img,
                                      double curvMin, double curvMax, double sigma, double distInf) {
        //obtem o objeto Mat
        byte[] data = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
        Mat imagem_inicial = new Mat(img.getHeight(), img.getWidth(), CvType.CV_8UC3);
        imagem_inicial.put(0, 0, data);

        //Diminui a imagem para otimizar o tempo de processamento
        // Imgproc.resize(imagem_inicial, imagem_inicial, new Size(800, 800));
        imagem_inicial = scaleImage(imagem_inicial);

        Mat preto_e_branco = new Mat(img.getHeight(), img.getWidth(), CvType.CV_8UC1);

        Imgproc.cvtColor(imagem_inicial, preto_e_branco, Imgproc.COLOR_RGB2GRAY);


        Mat pontos_fronteira = encontra_fronteiras(preto_e_branco);
        ArrayList<Componente> componentes = encontra_tudo(pontos_fronteira);

        ProcessaCurva pc = new ProcessaCurva();

        // Processa a curva para cada componente conexo
        for (Componente c : componentes) {
            pc.processComponent(pontos_fronteira, c, curvMin, curvMax, sigma,
                    distInf, true);
        }

        return this.converteMatBufferedImage(pontos_fronteira, true);

    }
}