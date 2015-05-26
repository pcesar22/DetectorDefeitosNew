import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

/*Esta classe é responsável por processar uma Componente e 
 medir curvaturas, derivadas e demais parâmetros. Nesta classe, 
 devemos inserir a lógica para identificação e classificação de um erro
 */

public class ProcessaCurva {


    private int LIMIT = 10;

    /**
     * Calculates the average value of each curve by simply summing their y values
     * and dividing them by 2.
     *
     * @param input input image
     * @param comp  connected component
     * @return the ArrayList of the average values
     */

    public ArrayList<Point> drawAverageCurve(Mat input, Componente comp) {

        int n_colunas = input.cols();
        int n_linhas = input.rows();

        ArrayList<Point> curva = new ArrayList<Point>();

        for (int c = 0; c < n_colunas; c++) {
            int l_medio = 0;
            int count = 0;

            for (int i = 0; i < n_linhas; i++) {
                if (comp.hasPoint(new Point(c, i))) {
                    l_medio += i;
                    count += 1;
                }
            }
            if (count != 0) {
                l_medio /= count;
                curva.add(new Point(c, l_medio));
            }
        }
        return curva;
    }

    //Insere pontos no meio da curva média a fim de evitar descontinuidades
    public void fillOutPoints(ArrayList<Point> curva, int indice) {
        Point p1 = curva.get(indice);
        Point p2 = curva.get(indice + 1);
        if (Math.abs(p1.y - p2.y) > 1) {
            if (p2.y > p1.y) {
                for (int i = (int) p1.y + 1; i < p2.y - 1; i++) {
                    curva.add(indice + 1, new Point(p1.x, i));
                }
            } else {
                for (int i = (int) p2.y + 1; i < p1.y - 1; i++) {
                    curva.add(indice + 1, new Point(p1.x, i));
                }
            }
        }
    }

    /**
     * Retorna o kernel gaussiano bem como a sua primeira
     e segunda derivadas
     * @param sigma
     * @param M
     * @return
     */

    //primeira linha da matriz - kernel gaussiano
    //segunda linha - derivada do kernel
    //terceira linha - segunda derivada

    public double[][] getGaussianDerivatives(double sigma, int M) {
        int L = (M - 1) / 2;

        double sigma_sq = sigma * sigma;
        double sigma_quad = sigma_sq * sigma_sq;

        double[] gaussian = new double[M];
        double[] dg = new double[M];
        double[] d2g = new double[M];
        double[][] ret = new double[3][M];

        Mat g = Imgproc.getGaussianKernel(M, sigma, CvType.CV_64F);

        for (double i = -L; i < L + 1.0; i += 1.0) {
            int idx = (int) (i + L);
            gaussian[idx] = g.get(idx, 0)[0];
            dg[idx] = (-i / sigma_sq) * g.get(idx, 0)[0];
            d2g[idx] = (-sigma_sq + i * i) / sigma_quad * g.get(idx, 0)[0];
        }

        ret[0] = gaussian;
        ret[1] = dg;
        ret[2] = d2g;

        return ret;
    }

    /**
     * Estima a derivada em um ponto específico em uma curva
     utilizando para isto o kernel gaussiano e suas derivadas

     * @param x
     * @param n
     * @param sigma
     * @param g
     * @param dg
     * @param d2g
     * @param isOpen
     * @return
     */

    public double[] getXDerivative(double[] x, int n, double sigma, double[] g, double[] dg, double[] d2g, boolean isOpen) {
        int L = (g.length - 1) / 2;

        double gx = 0;
        double dgx = 0;
        double d2gx = 0.0;

        for (int k = -L; k < L + 1; k++) {
            double x_n_k;
            if (n - k < 0) {
                if (isOpen) {
                    x_n_k = x[-(n - k)];
                } else {
                    x_n_k = x[x.length + (n - k)];
                }
            } else if (n - k > x.length - 1) {
                if (isOpen) {
                    if (n == 0) {
                        System.out.println(n + k);
                    }
                    x_n_k = x[n + k];
                } else {
                    x_n_k = x[(n - k) - (x.length)];
                }
            } else {
                x_n_k = x[n - k];
            }
            gx += x_n_k * g[k + L]; //gaussians go [0 -> M-1]
            dgx += x_n_k * dg[k + L];
            d2gx += x_n_k * d2g[k + L];
        }

        double[] ret = new double[3];
        ret[0] = gx;
        ret[1] = dgx;
        ret[2] = d2gx;

        return ret;
    }

    /**
     * Estima a derivada em todos os pontos de uma curva
     utilizando para isto o kernel gaussiano e suas derivadas

     * @param x
     * @param sigma
     * @param g
     * @param dg
     * @param d2g
     * @param isOpen
     * @return
     */

    public double[][] getXDerivativeCurve(double[] x, double sigma, double[] g, double[] dg, double[] d2g, boolean isOpen) {
        double[] gx = new double[x.length];
        double[] dx = new double[x.length];
        double[] d2x = new double[x.length];

        for (int i = 0; i < x.length; i++) {
            double ret[] = getXDerivative(x, i, sigma, g, dg, d2g, isOpen);
            gx[i] = ret[0];
            dx[i] = ret[1];
            d2x[i] = ret[2];
        }

        double[][] r = new double[3][x.length];
        r[0] = gx;
        r[1] = dx;
        r[2] = d2x;

        return r;
    }
	
	/*Processa uma curva, identificando pontos em que ocorre
	 mudança do sinal da curvatura (kappa) e pontos de derivada zero*/

    public void processComponent(Mat initialMatImage,
                                 Componente currentComponent, double curvMin,
                                 double curvMax, double sigma, double distInf, boolean mostraTudo) {

        ArrayList<Point> averagePointArray = drawAverageCurve(initialMatImage,
                currentComponent);

        for (int i = 0; i < averagePointArray.size() - 2; i++) {
            fillOutPoints(averagePointArray, i);
        }

        for (Point p : averagePointArray) {
            initialMatImage.put((int) p.y, (int) p.x,
                    new double[]{0, 255, 0});
        }

        double[] pointsX = new double[averagePointArray.size()];
        double[] pointsY = new double[averagePointArray.size()];

        for (int i = 0; i < averagePointArray.size(); i++) {
            pointsX[i] = averagePointArray.get(i).x;
            pointsY[i] = averagePointArray.get(i).y;
        }
        int M = Math.min((int) Math.round((10.0 * sigma + 1.0) / 2.0) * 2 - 1, pointsX.length);

        double[][] gaussianDerivatives = getGaussianDerivatives(sigma, M);

        double[] g, dg, d2g;
        g = gaussianDerivatives[0];
        dg = gaussianDerivatives[1];
        d2g = gaussianDerivatives[2];

        double[][] xDerivativeCurve = getXDerivativeCurve(pointsX, sigma, g, dg, d2g, true);
        double[][] yDerivativeCurve = getXDerivativeCurve(pointsY, sigma, g, dg, d2g, true);

        double[] dX = xDerivativeCurve[1];//derivada
        double[] ddX = xDerivativeCurve[2];//segunda derivada
        double[] dY = yDerivativeCurve[1];
        double[] ddY = yDerivativeCurve[2];

        ArrayList<Point> smoothCurve = new ArrayList<Point>();

        for (int i = 0; i < xDerivativeCurve[0].length; i++) {
            Point p = new Point(xDerivativeCurve[0][i], yDerivativeCurve[0][i]);
            smoothCurve.add(p);
        }

        double[] kappa = new double[xDerivativeCurve[0].length];
        double[] kappa_norm = new double[xDerivativeCurve[0].length];

        for (int i = 0; i < xDerivativeCurve[0].length; i++) {
            kappa_norm[i] = Math.abs((dX[i] * ddY[i] - ddX[i] * dY[i]))
                    / Math.pow(dX[i] * dX[i] + dY[i] * dY[i], 1.5);
        }
        for (int i = 0; i < xDerivativeCurve[0].length; i++) {
            kappa[i] = (dX[i] * ddY[i] - ddX[i] * dY[i])
                    / Math.pow(dX[i] * dX[i] + dY[i] * dY[i], 1.5);
        }

        System.out.println("inicio de componente");
        int[] ehMudanca = filterInflectionPoints(kappa, (int) distInf);
        int[] ehCurvaZero = getCurvatureInflectionPoints(kappa);

        for (int i = LIMIT; i < kappa_norm.length - LIMIT; i++) {

            // Parte para visualizar curvaturas

            // Scalar valorRGB = acharRGB(kappa_norm[i]);
            // if (mostraTudo) {
            // Core.circle(inicial, new Point(r_x[0][i], r_y[0][i]), 2,
            // valorRGB);
            // }

            System.out.println(kappa_norm[i]);

            if (ehMudanca[i] == 1 && kappa_norm[i] > curvMin) {
                Core.circle(initialMatImage, new Point(xDerivativeCurve[0][i], yDerivativeCurve[0][i]),
                        3,
                        new Scalar(0, 0, 255));
            }
            if (ehCurvaZero[i] == 1) {
                Core.circle(initialMatImage, new Point(xDerivativeCurve[0][i], yDerivativeCurve[0][i]),
                        2,
                        new Scalar(255, 0, 0));
            }

            // if (kappa[i] > 0.005) {
            // Core.circle(inicial, new Point(r_x[0][i], r_y[0][i]), 2,
            // new Scalar(255, 0, 0));
            // } else if (kappa[i] < -0.005) {
            // Core.circle(inicial, new Point(r_x[0][i], r_y[0][i]), 2,
            // new Scalar(0, 0, 255));
            // }

			/*
			 * if (kappa_norm[i] > 0.04) { if (mostraTudo) Core.circle(inicial,
			 * new Point(r_x[0][i], r_y[0][i]), 2, new Scalar(255, 0, 0)); }
			 */

			/*
			 * if(dY[i]/dX[i]<0 && dY[i+1]/dX[i+1]>0 || dY[i]/dX[i]>0 &&
			 * dY[i+1]/dX[i+1]<0){ if(mostraTudo)
			 * 
			 * Core.circle(inicial, new Point(r_x[0][i], r_y[0][i]), 2, new
			 * Scalar(0, 255, 255), -1);
			 * 
			 * } //quando tem um zero crossing na curvatura (kappa), faz um
			 * circulo vermelho no lugar
			 * 
			 * /* if ((kappa[i] < 0 && kappa[i+1] > 0) || kappa[i] > 0 &&
			 * kappa[i+1] < 0) { if(mostraTudo) Core.circle(inicial, new
			 * Point(r_x[0][i],r_y[0][i]), 2, new Scalar(255,0,0),-1); }
			 */
        }
        System.out.println("fim de componente");
        System.out.println();

    }

    public int[] getCurvatureInflectionPoints(double[] kappa) {
        int[] kappa_novo = new int[kappa.length];
        int tamanho = kappa.length;
        for (int i = 0; i < tamanho; i++) {
            kappa_novo[i] = 0;
        }

        // ArrayList<Integer> indicesValidos = new ArrayList<Integer>();

        boolean sinal;
        for (int i = 1; i < tamanho - 1; i++) {
            sinal = (kappa[i] > 0);
            if ((kappa[i + 1] > 0 && !sinal) || (kappa[i + 1] < 0 && sinal)) {
                kappa_novo[i] = 1;
            }
        }
        return kappa_novo;
    }

    public int[] filterInflectionPoints(double[] kappa, int distInf) {

        int[] kappa_novo = new int[kappa.length];
        int[] pontosMudanca = getCurvatureInflectionPoints(kappa);

        int tamanho = kappa.length;
        for (int i = 0; i < tamanho; i++) {
            kappa_novo[i] = 0;
        }
        int cont = 0;
        int maximo = distInf;
        boolean tavalendo = false;
        ArrayList<Integer> indicesValidos = new ArrayList<Integer>();

        for (int i = LIMIT; i < pontosMudanca.length - LIMIT; i++) {
            if (cont < maximo) {
                if (!tavalendo && pontosMudanca[i] == 1) {
                    tavalendo = true;
                } else if (tavalendo) {
                    cont++;
                    indicesValidos.add(i);
                    if (pontosMudanca[i] == 1) {
                        for (Integer j : indicesValidos) {
                            kappa_novo[j] = 1;
                        }
                        cont = 0;
                    }
                }
            } else {
                tavalendo = false;
                indicesValidos.clear();
                cont = 0;
            }
        }
        return kappa_novo;
    }

    public double getMax(double[] klist, double threshold) {
        double kmax = 0;
        for (int i = 10; i < klist.length - 10; i++) {
            if (klist[i] < threshold && klist[i] > kmax)
                kmax = klist[i];
        }
        return kmax;
    }

    public double getMin(double[] klist) {
        double kmin = klist[0];
        for (int i = 0; i < klist.length; i++) {
            if (klist[i] < kmin)
                kmin = klist[i];
        }
        return kmin;
    }

    public Scalar getRGBValue(double kNorm) {
        double r, g, b;
        if (kNorm < 0.12) {
            r = 0;
            g = 0;
            b = 0.5 + 4 * kNorm;
        } else if (kNorm >= 0.12 && kNorm < 0.37) {
            r = 0;
            g = 4 * (kNorm - 0.12);
            b = 1;
        } else if (kNorm >= 0.37 && kNorm < 0.62) {
            r = 4 * (kNorm - 0.37);
            g = 1;
            b = 1 - 4 * (kNorm - 0.37);
        } else if (kNorm >= 0.62 && kNorm < 0.87) {
            r = 1;
            g = 1 - 4 * (kNorm - 0.62);
            b = 0;
        } else {
            r = 1 - 4 * (kNorm - 0.87);
            g = 0;
            b = 0;
        }

        r = (int) (r * 255);
        g = (int) (g * 255);
        b = (int) (b * 255);

        return new Scalar(r, g, b);
    }
}
