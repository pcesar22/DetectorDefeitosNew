import javax.swing.*;
import java.awt.image.BufferedImage;

/*Esta classe mostra uma janela com uma imagem dentro.
 É utilizada neste caso para mostrar o resultado do Threshold
 */


@SuppressWarnings("serial")
class MostraImagem extends JFrame {

    private BufferedImage img;
    private JLabel imagem;
    private String title;

    //Seta o título da janela
    public void setaTitulo(String title) {
        this.title = title;
        setTitle(this.title);
    }

	/*Para exibir uma imagem, enviamos uma BufferedImage.
	A fim de exibirmos a imagem, é necessário ainda utilizar
	o método setVisible(true)*/

    MostraImagem(BufferedImage img) {

        this.img = img;
        setSize(this.img.getWidth(), this.img.getHeight());

        imagem = new JLabel(new ImageIcon(this.img));
        getContentPane().add(imagem);
    }

}

