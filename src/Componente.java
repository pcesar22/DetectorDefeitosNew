import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.HashMap;

/*A classe Componente representa um conjunto de pontos que 
 forma uma franja na imagem. Ela possui um arraylist com todos os pontos
 integrantes, bem como uma hash table que permite a verificação rápida 
 da pertinência de um ponto. Também são computados os valores máximos e
 mínimos das coordenadas x e y dentro da componente
 */

public class Componente {


    private ArrayList<Point> pointArray;
    HashMap<Point, Boolean> hash;

    double y_max;
    double y_min;
    double x_max;
    double x_min;

    //(public/private) tipo_da_saida nome_metodo(argumentos)

    private HashMap<Point, Boolean> criaHash(ArrayList<Point> conjunto) {
        HashMap<Point, Boolean> retorno = new HashMap<Point, Boolean>();

        y_max = -100000;
        x_max = -100000;
        y_min = 100000;
        x_min = 100000;

        for (Point k : conjunto) {
            if (k.x > x_max) x_max = k.x;
            if (k.x < x_min) x_min = k.x;
            if (k.y > y_max) y_max = k.y;
            if (k.y < y_min) y_min = k.y;

            retorno.put(k, true);
        }
        return retorno;
    }

    //Podemos criar uma componente fornecendo a lista de pontos
    Componente(ArrayList<Point> corpo) {
        this.pointArray = corpo;
        this.hash = criaHash(this.pointArray);
    }

    //Ou então criar uma componente vazia
    Componente() {
        this.pointArray = new ArrayList<Point>();
        this.hash = new HashMap<Point, Boolean>();
    }

	/*Funções para a adição de pontos. Existem duas funções pois
	Existem duas classes Point diferentes. Uma vindo da biblioteca
	Java.awt e outra vinda do Opencv. No caso, a classe Componente
	utiliza a classe Point do Opencv, mas podemos adicionar pontos da 
	java.awt, que são covertidos antes de serem adicionados*/

    public void add(Point k) {
        this.pointArray.add(k);
        this.hash.put(k, true);
    }

    public void add(java.awt.Point k) {
        Point k2 = new Point();
        k2.x = k.x;
        k2.y = k.y;
        this.pointArray.add(k2);
        this.hash.put(k2, true);
    }

    /*Retorna o conjunto de pontos de uma componente
    conexa*/
    public ArrayList<Point> getCorpo() {
        return this.pointArray;
    }

    //Verifica se um ponto pertence ou não a uma dada componente
    public boolean hasPoint(Point k) {
        return this.hash.containsKey(k);
    }
}
