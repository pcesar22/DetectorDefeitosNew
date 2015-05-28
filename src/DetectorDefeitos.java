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
    private int WINDOW_WIDTH = 640;
    private int WINDOW_HEIGHT = 480;
    private int CONTROL_HEIGHT = 100;
    private int PAINEL_WIDTH = 350;
    private int FOLGA = 10;

    private int PAINEL_HEIGHT = WINDOW_HEIGHT - CONTROL_HEIGHT;

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

        // Button labels
        String selectImageLabel = "Choose Image";
        String importParametersLabel = "Import Parameters";
        String removeAreaLabel = "Remove unwanted region";
        String selectAreaLabel = "Select wanted region";
        String testThresholdLabel = "Test threshold";
        String processImageLabel = "Process image";
        String histogramLabel = "Histogram";
        String saveParametersLabel = "Save parameters";

        // Tooltip labels ... html tags are just to use the <br> that breaks the line
        String selectImageToolTip = "<html>Select image to be processed";
        String importParametersTooltip = "<html>Import parameters from a saved text file with the same name as the image, <br> " +
                " but with a .txt extension </html>";
        String removeAreaToolTip = "<html>Remove unwanted area. Select a point and form a closed contour, <br>  " +
                "making sure to end at the same point </html>";
        String selectAreaToolTip = "<html>Select desired area. Select a point and form a closed contour, <br> " +
                "making sure to end at the same point </html>";
        String testThresholdToolTip = "<html>Test the chosen threshold. We want the connected components <br> >" +
                "to be as significant as possible </html>";
        String processImageToolTip = "<html>Apply the image processing algorithm and save the image in the <br> " +
                "same folder </html>";
        String histogramToolTip = "<html>Makes a histogram plot of the pixels in the image. <br> " +
                "Also displays the automated threshold using various algorithms </html>";
        String saveParametersToolTip = "<html>Save current parameters to a text file with the same name as <br> " +
                "the image, with a .txt extension </html>";



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
            e.printStackTrace();
        }

        //Filtro de imagens para a caixa de seleção
        jpg_filtro = new FileNameExtensionFilter("Imagens JPG e GIF", "jpg", "gif");

		/*Inicializa as caixas de checagem do Threshold (Em uma configuração
		 que em geral funciona para as imagens em estudo. Utilizando
		 apenas a variável V mínima. O máximo está setado para o 
		 maior V possível.
		 */

        //Initialize the UI labels
        sigmaJLabel = new JLabel(" sigma: ");
        minCurvatureJLabel = new JLabel(" curvMin: ");
        maxCurvatureJLabel = new JLabel(" curvMax: ");
        minDistanceJLabel = new JLabel(" distInf: ");
        thresholdJLabel = new JLabel(" V_min: ");

        //Initialize the interactive text boxes
        sigmaJTextField = new JTextField();
        minCurvatureJTextField = new JTextField();
        maxCurvatureJTextField = new JTextField();
        minDistanceJTextField = new JTextField();
        thresholdJTextField = new JTextField();

        updateStrings();

		
		/*Panel that glues all components related to the UI (user interface), where we
        want to have interactive text fields.
		 */
        controlParametersJPanel = new JPanel();
        controlParametersJPanel.setLayout(new GridLayout(3, 2));

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

        //Select image button - opens up a prompt for the user to choose the image file to process
        selectJButton = new JButton(selectImageLabel);
        selectJButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    //abre um diálogo para a seleção da imagem
                    openFile();
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(null, "Erro ao abrir o arquivo", "Erro", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        selectJButton.setBorder(BorderFactory.createLineBorder(Color.orange.brighter(), 2));
        selectJButton.setBackground(Color.orange.darker().darker());
        selectJButton.setToolTipText(processImageToolTip);



        // Button to remove unwanted area
        removeAreaJButton = new JButton(removeAreaLabel);
        removeAreaJButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (removeAreaJButton.getText()
                        .equals("Remove unwanted area")) {

                    /**
                     * First true - informs the panel that we are drawing
                     * Second true - informs the panel that we are removing area
                     */

                    boolean deuCerto = pn.podeDesenhar(true, true);

                    if (deuCerto) {
                        removeAreaJButton.setText("Stop cut");
                        removeAreaJButton.setBackground(Color.RED);
                    }
                } else {
                    removeAreaJButton.setText("Remove unwanted area");
                    removeAreaJButton.setBackground(null);
                    pn.atualizaImagemCorte();

                }

            }
        });
		
		/*Botão para a realização de cortes na imagem
		(Corte de seleção de área de interesse)*/
        cropAreaJButton = new JButton(selectAreaLabel);
        cropAreaJButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (cropAreaJButton.getText().equals("Select area of interest")) {
                    /*informa ao painel que estamos em processo de
					desenho (primeiro true) e que é um desenho de
					eliminação de detalhe (false)*/

                    boolean deuCerto = pn.podeDesenhar(true, false);
                    if (deuCerto) {
                        cropAreaJButton.setText("Stop cut");
                        cropAreaJButton.setBackground(Color.BLUE);
                    }
                } else {
                    cropAreaJButton.setText("Select area of interest");
                    cropAreaJButton.setBackground(null);
                    pn.atualizaImagemProcessamento();

                }

            }
        });

        processJButton = new JButton(processImageLabel);
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

        //FANCY!
        processJButton.setBorder(BorderFactory.createLineBorder(Color.BLUE.darker(), 2));
        processJButton.setBackground(Color.cyan.darker().darker());


        saveJButton = new JButton(saveParametersLabel);
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
        saveJButton.setBorderPainted(true);
        saveJButton.setBackground(Color.GRAY);


        loadJButton = new JButton(importParametersLabel);
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
                updateStrings();
                validate();
                repaint();

            }
        });
        loadJButton.setBorderPainted(true);
        loadJButton.setBackground(Color.GRAY);

        testThresholdJButton = new JButton(testThresholdLabel);
        testThresholdJButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
				/*Apenas abre uma janela com o resultado do Theshold
				para fins de teste*/
                if (pn.retornaImagemInicial() != null) {
                    Mat resultado = realizaThreshold();

                    MostraImagem k = new MostraImagem(pn.converteMatBufferedImage(resultado, true));
                    k.setVisible(true);
                } else {
                    JOptionPane.showMessageDialog(null, "Null image! Select an Image",
                            "Error", JOptionPane.ERROR_MESSAGE);
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
                updateStrings();
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
                updateStrings();
                validate();
                repaint();
            }
        });

        // Botao para gerar o histograma
        histJButton = new JButton(histogramLabel);
        histJButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                new Histogram(pn.retornaImagemInicial());
            }
        });


        // Set the tooltips for the buttons

        cropAreaJButton.setToolTipText(selectAreaToolTip);
        processJButton.setToolTipText(processImageToolTip);
        selectJButton.setToolTipText(selectImageToolTip);
        removeAreaJButton.setToolTipText(removeAreaToolTip);
        saveJButton.setToolTipText(saveParametersToolTip);
        loadJButton.setToolTipText(importParametersTooltip);
        histJButton.setToolTipText(histogramToolTip);
        testThresholdJButton.setToolTipText(testThresholdToolTip);

        //Mostra o número de defeitos encontrados (Não foi utilizado ainda)
        spacerJLabel = new JLabel("============================================");
        numberOfDefectsJLabel = new JLabel("  Select the part of interest ");

        //Button panel that glues all buttons
        buttonJPanel = new JPanel();
        buttonJPanel.setLayout(new GridLayout(12, 1));

        // Buttons should be in order of how they should be displayed
        buttonJPanel.add(selectJButton);

        buttonJPanel.add(removeAreaJButton);
        buttonJPanel.add(cropAreaJButton);

        buttonJPanel.add(testThresholdJButton);
        buttonJPanel.add(histJButton);

        buttonJPanel.add(saveJButton);
        buttonJPanel.add(loadJButton);

        buttonJPanel.add(processJButton);

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
                        - CONTROL_HEIGHT,
                PAINEL_WIDTH - FOLGA, CONTROL_HEIGHT);

        controlParametersJPanel.setBorder(BorderFactory
                .createLineBorder(Color.black));

        //adds all the panels to the main window
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
    public int openFile() throws IOException {
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

    private void updateStrings() {
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


