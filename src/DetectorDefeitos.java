import org.opencv.core.Mat;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/*A classe DetectorDefeitos se trata da janela principal do 
 programa, que por sua vez, chama a classe PainelImagem e a classe
 ProcessadorLinhas
 */

@SuppressWarnings("serial")

/**
 * Main class that handles the user interface
 */
public class DetectorDefeitos extends JFrame {

    private JFileChooser jfile = null;
    private TextHandler handler;
    private String FILENAME;
    double[] storedValues; //Where the parameters from FILENAME will be loaded.

    private PainelImagem pn;
    private JPanel mainJPanel, buttonJPanel, controlParametersJPanel;
    private JButton selectJButton, removeAreaJButton, processJButton,
            testThresholdJButton, cropAreaJButton, doorParamsJButton,
            mudGuardParamsJButton, histJButton, saveJButton, loadJButton;
    private JLabel numberOfDefectsJLabel, spacerJLabel;
    private FileFilter jpg_filtro;
    private JLabel sigmaJLabel, maxCurvatureJLabel, minCurvatureJLabel,
            minDistanceJLabel, thresholdJLabel;
    private JTextField sigmaJTextField, maxCurvatureJTextField,
            minCurvatureJTextField, minDistanceJTextField, thresholdJTextField;

    private int vMax = 255;

    // Multiplication constant for curvature parameters (makes input easier)
    private int FACTOR = 10000;

    // Window dimension parameters
    private int WINDOW_WIDTH = 850;
    private int WINDOW_HEIGHT = 700;
    private int CONTROLE_HEIGHT = 100;
    private int PAINEL_WIDTH = 350;
    private int FOLGA = 10;

    private int PAINEL_HEIGHT = WINDOW_HEIGHT - CONTROLE_HEIGHT;

    // Processing parameters
    private double nSigma = 11, nCmin = 0.002 * FACTOR, nCmax = 0.05;
    private int nThresh = 100, nRange = 35;

    private String sThresh, sCmin, sCmax, sRange, sSigma;

    private static int procCounter = 0; // Helps name the saved processed image file


    /**
     * Constructor for the DetectorDefeitos class.
     * The file functions as well as all the GUI buttons and interfaces
     * will be created.
     */
    DetectorDefeitos() {

        FILENAME = "Parametros/parametros";
        handler = new TextHandler();

        //Seta parâmetros da janela
        setSize(WINDOW_WIDTH + PAINEL_WIDTH + FOLGA, WINDOW_HEIGHT
                + 5 * FOLGA);
        setResizable(true);
        setTitle("Reconhecedor de defeitos v1.1");
        setLocationRelativeTo(null);

        //Apenas modifica o Look And Feel da Janela se possível
        try {
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());

                    break;
                }
            }
        } catch (Exception e) {

        }

        //Filtro de imagens para a caixa de seleção
        jpg_filtro = new FileNameExtensionFilter("Imagens JPG e GIF", "jpg", "gif");

		/*Inicializa as caixas de checagem do Threshold (Em uma configuração
		 que em geral funciona para as imagens em estudo. Utilizando
		 apenas a variável V mínima. O máximo está setado para o 
		 maior V possível.
		 */

        //Inicializa os rótulos das caixas de checagem
        sigmaJLabel = new JLabel(" sigma: ");
        minCurvatureJLabel = new JLabel(" curvMin: ");
        maxCurvatureJLabel = new JLabel(" curvMax: ");
        minDistanceJLabel = new JLabel(" distInf: ");
        thresholdJLabel = new JLabel(" V_min: ");

        //Inicializa caixas de texto onde inserimos os limites do Threshold
        sigmaJTextField = new JTextField();
        minCurvatureJTextField = new JTextField();
        maxCurvatureJTextField = new JTextField();
        minDistanceJTextField = new JTextField();
        thresholdJTextField = new JTextField();

        atualizaString();
        //Threshold que em geral funciona para as imagens em estudo
		
		/*Painel que reune todos os componentes da interface gráfica
		 relacionados com a definição de Thresholds
		 */
        controlParametersJPanel = new JPanel();
        controlParametersJPanel.setLayout(new GridLayout(3, 2)); // 7 linhas e 4
        // colunas

        controlParametersJPanel.add(thresholdJLabel);
        controlParametersJPanel.add(thresholdJTextField);
        controlParametersJPanel.add(sigmaJLabel);
        controlParametersJPanel.add(sigmaJTextField);
        controlParametersJPanel.add(minCurvatureJLabel);
        controlParametersJPanel.add(minCurvatureJTextField);
        //controlParametersJPanel.add(maxCurvatureJLabel);
        //controlParametersJPanel.add(maxCurvatureJTextField);
        //controlParametersJPanel.add(minDistanceJLabel);
        //controlParametersJPanel.add(minDistanceJTextField);

        //Painel de imagem, que é a região onde a imagem é exibida
        pn = new PainelImagem();

        //Botão de selecionar imagem
        selectJButton = new JButton("Selecionar imagem");
        selectJButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    //abre um diálogo para a seleção da imagem
                    abreArquivo();
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(null, "Erro ao abrir o arquivo", "Erro", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
		/*Botão para a realização de cortes na imagem
		(Corte de eliminação de detalhe)*/
        removeAreaJButton = new JButton("Retira area indesejada");
        removeAreaJButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (removeAreaJButton.getText()
                        .equals("Retira area indesejada")) {
					/*informa ao painel que estamos em processo de 
					desenho (primeiro true) e que é um desenho de
					eliminação de detalhe (segundo true)*/

                    boolean deuCerto = pn.podeDesenhar(true, true);

                    if (deuCerto) {
                        removeAreaJButton.setText("Parar corte");
                        removeAreaJButton.setBackground(Color.RED);
                    }
                } else {
                    removeAreaJButton.setText("Retira area indesejada");
                    removeAreaJButton.setBackground(null);
                    pn.atualizaImagemCorte();

                }

            }
        });
		
		/*Botão para a realização de cortes na imagem
		(Corte de seleção de área de interesse)*/
        cropAreaJButton = new JButton("Seleciona area desejada");
        cropAreaJButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (cropAreaJButton.getText().equals("Seleciona area desejada")) {
					/*informa ao painel que estamos em processo de 
					desenho (primeiro true) e que é um desenho de
					eliminação de detalhe (false)*/

                    boolean deuCerto = pn.podeDesenhar(true, false);
                    if (deuCerto) {
                        cropAreaJButton.setText("Parar corte");
                        cropAreaJButton.setBackground(Color.BLUE);
                    }
                } else {
                    cropAreaJButton.setText("Seleciona area desejada");
                    cropAreaJButton.setBackground(null);
                    pn.atualizaImagemProcessamento();

                }

            }
        });

        processJButton = new JButton("Processar");
        processJButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
				/*Opera o Threshold selecionado e realiza o processamento
				 da imagem
				 */

                double curvMin = converte(minCurvatureJTextField.getText())
                        / FACTOR;
                double curvMax = converte(maxCurvatureJTextField.getText());
                double sigma = converte(sigmaJTextField.getText());
                double distInf = converte(minDistanceJTextField.getText());

                Mat resultado = realizaThreshold();
                BufferedImage output = pn.executeProcessing(resultado, curvMin,
                        curvMax, sigma,
                        distInf);

                // Salvar imagem no diretorio
                try {
                    // debug
                    System.out.println(jfile.getSelectedFile().getName());

                    // Make file name
                    String fileName = jfile.getCurrentDirectory().toString()
                            + "\\"
                            + jfile.getSelectedFile().getName() + "_PROC_"
                            + procCounter + ".jpg";
                    //debug
                    System.out.println(fileName);
                    File outputfile = new File(fileName);
                    procCounter++;

                    // Write the output file
                    ImageIO.write(output, "png", outputfile);

                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
        });

        saveJButton = new JButton("Salvar");
        saveJButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    handler.writeFile(
                            new double[]{converte(sigmaJTextField.getText()),
                                    converte(thresholdJTextField.getText()),
                                    converte(minCurvatureJTextField.getText())},
                            FILENAME);
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
        });

        loadJButton = new JButton("Importar Parâmetros");
        loadJButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    storedValues = handler.readFile(FILENAME);
                    nSigma = storedValues[0];
                    nThresh = (int) storedValues[1];
                    nCmin = storedValues[2];

                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                atualizaString();
                validate();
                repaint();

            }
        });

        testThresholdJButton = new JButton("Testar Threshold");
        testThresholdJButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
				/*Apenas abre uma janela com o resultado do Theshold
				para fins de teste*/
                if (pn.retornaImagemInicial() != null) {
                    Mat resultado = realizaThreshold();

                    MostraImagem k = new MostraImagem(pn.converteMatBufferedImage(resultado, true));
                    k.setVisible(true);
                } else {
                    JOptionPane.showMessageDialog(null, "Imagem nula! Selecione uma imagem", "Erro", JOptionPane.ERROR_MESSAGE);
                }

            }
        });

        doorParamsJButton = new JButton("Porta");
        doorParamsJButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                nSigma = 14;
                nThresh = 140;
                nRange = 40;
                nCmin = 0.001;
                nCmax = 0.04;
                atualizaString();
                validate();
                repaint();
            }
        });

        mudGuardParamsJButton = new JButton("Para-lama");
        mudGuardParamsJButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                nSigma = 12;
                nThresh = 180;
                nRange = 40;
                nCmin = 0.001;
                nCmax = 0.04;
                atualizaString();
                validate();
                repaint();
            }
        });

        // Botao para gerar o histograma
        histJButton = new JButton("Histograma");
        histJButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                new TestHistogram(pn.retornaImagemInicial());
            }
        });


        //Mostra o número de defeitos encontrados (Não foi utilizado ainda)
        spacerJLabel = new JLabel("============================================");
        numberOfDefectsJLabel = new JLabel("  Selecione a parte de interesse: ");

        //Painel que reune todos os botões
        buttonJPanel = new JPanel();
        buttonJPanel.setLayout(new GridLayout(12, 1));

        buttonJPanel.add(selectJButton);
        buttonJPanel.add(loadJButton);
        buttonJPanel.add(removeAreaJButton);
        buttonJPanel.add(cropAreaJButton);
        buttonJPanel.add(testThresholdJButton);
        buttonJPanel.add(processJButton);
        buttonJPanel.add(histJButton);
        buttonJPanel.add(saveJButton);

        //buttonJPanel.add(spacerJLabel);

        //buttonJPanel.add(numberOfDefectsJLabel);
        //buttonJPanel.add(doorParamsJButton);
        //buttonJPanel.add(mudGuardParamsJButton);

        //Barra lateral de comandos
        mainJPanel = new JPanel();
        mainJPanel.setLayout(new BorderLayout());
        mainJPanel.add(buttonJPanel, BorderLayout.NORTH);
        mainJPanel.setBorder(BorderFactory.createLineBorder(Color.black));

        setLayout(null);

        pn.setBounds(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
        mainJPanel.setBounds(WINDOW_WIDTH, 0, PAINEL_WIDTH - FOLGA,
                PAINEL_HEIGHT);
        controlParametersJPanel.setBounds(WINDOW_WIDTH, WINDOW_HEIGHT
                        - CONTROLE_HEIGHT,
                PAINEL_WIDTH - FOLGA, CONTROLE_HEIGHT);

        controlParametersJPanel.setBorder(BorderFactory
                .createLineBorder(Color.black));

        //adiciona todos os paineis à janela principal
        getContentPane().add(pn);
        getContentPane().add(mainJPanel);
        getContentPane().add(controlParametersJPanel);

    }


    /**
     * @return Contains the thresholded image in Mat format
     */
    public Mat realizaThreshold() {
        Mat imagem = pn.retornaImagemInicial();
        Mat resultado = imagem.clone();
        Mat imagemHSV = pn.retornaImagemHSV();
        //Mat imagem_gray = pn.retornaImagemGray();

        double v_min = converte(thresholdJTextField.getText());
        int rows = imagem.rows();
        int cols = imagem.cols();

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                resultado.put(i, j, new double[]{0, 0, 0});
                double hsv[] = imagemHSV.get(i, j);
                boolean troca = true;
                if ((hsv[2] < v_min || hsv[2] > vMax))
                    troca = false;
                if (troca)
                    resultado.put(i, j, new double[]{255, 255, 255});
            }
        }

        return resultado;
    }


    /**
     * Opens the chosen arquive using the JFileChooser class. jfile is created
     * as a public variable, and it will manage the current directory the user is on.
     * It also remembers the last directory the user was on, for convenience.
     *
     * @return 1 if successfull, 0 if null file
     * @throws IOException
     */
    public int abreArquivo() throws IOException {
        boolean firstPass = false;
        if (jfile == null) {
            firstPass = true;
            jfile = new JFileChooser();

        }
        File ini = new File("imagens_nova_estrutura");

        if (ini.exists() && firstPass)
            jfile.setCurrentDirectory(new File("imagens_nova_estrutura"));

        jfile.setFileFilter(jpg_filtro);
        jfile.setMultiSelectionEnabled(false);
        int returnVal = jfile.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            jfile.setCurrentDirectory(jfile.getCurrentDirectory());
            firstPass = false;
        }
        File a = jfile.getSelectedFile();
        System.out.println(a.getName());

        if (a != null) {
            pn.abreImagem(a);
            return 1;
        }

        return 0;

    }

    /**
     *
     * @param k String to convert to double format
     * @return returns the double value of k. If k is empty, returns 255 (black)
     */
    double converte(String k) {
        if (k.equals("")) {
            return 255;
        } else {
            return Double.valueOf(k);
        }
    }

    /**
     * Captures all the text fields and updates the parameters.
     * Returns them back to the text field.
     */

    private void atualizaString() {
        // Atualiza os valores mostrados nos text fields

        sThresh = String.valueOf(nThresh);
        sCmin = String.valueOf(nCmin);
        sCmax = String.valueOf(nCmax);
        sRange = String.valueOf(nRange);
        sSigma = String.valueOf(nSigma);

        sigmaJTextField.setText(sSigma);
        minCurvatureJTextField.setText(sCmin);
        maxCurvatureJTextField.setText(sCmax);
        minDistanceJTextField.setText(sRange);
        thresholdJTextField.setText(sThresh);

    }

    /**
     * Main routine.
     *
     * @param args
     */
    public static void main(String args[]) {
        DetectorDefeitos janela = new DetectorDefeitos();
        janela.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        janela.setVisible(true);
    }
}


