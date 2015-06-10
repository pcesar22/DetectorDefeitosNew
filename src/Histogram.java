import org.opencv.core.Mat;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;


public class Histogram {

    private int height, width;
    private int FILTER_VALUE = 25;  // We don't care about pixels that have intensity levels below this.
    private int LOWEST_TO_PRINT = 15; // We don't print values below this level

    public Histogram(Mat grayScaleImage) {

        JFrame frame = new JFrame();

        this.height = grayScaleImage.height();
        this.width = grayScaleImage.width();

        Map<Integer, Integer> mapHistory = getMapFromData(grayScaleImage);
        frame.setLayout(new BorderLayout());
        frame.add(new JScrollPane(new Graph(mapHistory)));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /**
     * Gets the data from a Mat image and converts it into a map, that captures
     * the histogram in a key-value pair
     *
     * @param grayScaleImage
     * @return
     */
    public Map<Integer, Integer> getMapFromData(Mat grayScaleImage) {

        int[][] data = new int[width][height];
        for (int c = 0; c < width; c++) {
            for (int r = 0; r < height; r++) {
                data[c][r] = (int) (grayScaleImage.get(r, c)[0]);
            }
        }
        Map<Integer, Integer> mapHistory = new TreeMap<Integer, Integer>();
        for (int c = 0; c < data.length; c++) {
            for (int r = 0; r < data[c].length; r++) {
                int value = data[c][r];
                int amount = 0;
                if (mapHistory.containsKey(value)) {
                    amount = mapHistory.get(value);
                    amount++;
                } else {
                    amount = 1;
                }
                /*
				 * if (value > 25) mapHistory.put(value, amount); else
				 * mapHistory.put(value, 1);
				 */
                mapHistory.put(value, amount);
            }
        }
        return mapHistory;
    }

    public int[] convertFromMapToInt(Map<Integer, Integer> mapHistory) {
        int[] hist = new int[256];
        for (Integer key : mapHistory.keySet()) {
            hist[key.intValue()] = mapHistory.get(key);
        }
        return hist;
    }


    /**
     * Average intensity of a histogram
     *
     * @param histogram list of 256 elements containing the stem value for each pixel intensity
     * @return the average Intensity level
     */
    public double getAverageIntensity(int[] histogram) {
        int length = histogram.length;
        double averageIntensity;
        int sum = 0, weightedSum = 0;
        for (int i = 0; i < length; i++) {
            sum += histogram[i];
            weightedSum += i * histogram[i];
        }
        averageIntensity = (1.0 * weightedSum) / sum;

        // debug
        System.out.println("TEST getAverageIntensity = " + averageIntensity);

        return averageIntensity;
    }

    public double getMeanGrayValue(int begin, int end, int[] histogram) {
        // gets the mean gray value for a specified range, inclusive.

        int sum = 0, weightedSum = 0;
        double meanGrayValue;
        for (int i = begin; i <= end; i++) {
            sum += histogram[i];
            weightedSum += i * histogram[i];
        }

        meanGrayValue = 1.0 * weightedSum / sum;
        System.out.println("DEBUG meanGrayValue = " + meanGrayValue);
        return meanGrayValue;

    }

    /************************
     * WINDOWING ALGORITHMS *
     ************************/

    /**
     * Function returns a rectangular window. Pixels are 0 below minValue, and 1 above it
     * @param minValue
     * @return list of 256 elements containing the multiplier for each pixel
     */

    public int[] getWindowSharp(int minValue) {
        int[] window = new int[256];

        for (int i = 0; i < 256; i++) {
            if (i < minValue)
                window[i] = 0;
            else
                window[i] = 1;
        }
        return window;

    }

    /**
     * Function returns a smooth window.
     * @param minOneValue all values above this are 1
     * @param halfValue this is where the smooth function is equal to 0.5
     * @return list of 256 elements containing the multiplier for each pixel
     */

    public double[] getWindowQuadratic(int minOneValue, double halfValue) {
        // minOneValue is the minimum value for which the window has gain 1
        // maxOneValue is the maximum value for which the window has gain 1

        double[] window = new double[256];

        // We are modeling the region window[0] ---> window[minOnevalue] as a
        // quadratic
        // of the form p(x) = bx^2 + cx. Chosen parameters:
        // p(0) = 0
        // p(minOneValue/2) = 0.1
        // p(minOneValue) = 1

        int midValue = (int) Math.floor(minOneValue * 1.0 / 2);
        double c = (halfValue * Math.pow(minOneValue, 3) - Math
                .pow(midValue, 3))
                / (1.0 * minOneValue * minOneValue * midValue * midValue * (minOneValue - midValue));
        double b = (halfValue - c * midValue * midValue)
                / (midValue * midValue * midValue);

        for (int i = 0; i <= minOneValue; i++) {
            window[i] = Math.abs(b * i * i * i + c * i * i);
            if (window[i] < 0.001)
                window[i] = 0;

        }
        for (int i = minOneValue + 1; i < 256; i++) {
            window[i] = 1;
        }
        return window;

    }


    /***************************
     * THRESHOLDING ALGORITHMS *
     ***************************/

    /**
     * Uses the Global thresholding value, that simply gets the threshold that
     * divide the intensity levels evenly
     *
     * @param histogram list of 256 elements containing the stem values for every pixel
     * @return the thresold
     */
    public double getThresholdGlobalThresholding(int[] histogram) {
        int COUNTER_MAX = 1000; // Maximum number of iterations allowed
        int counter = 0;
        int histSize = histogram.length;
        double thresholdNew = getAverageIntensity(histogram);
        double thresholdOld;

        double R1, R2; // mean Gray values

        do {
            R1 = getMeanGrayValue(0, (int) thresholdNew, histogram);
            R2 = getMeanGrayValue((int) thresholdNew + 1, histSize - 1,
                    histogram);
            thresholdOld = thresholdNew;
            thresholdNew = (R1 + R2) / 2;
            counter++;

        } while (thresholdNew - thresholdOld < 0.1 && counter < COUNTER_MAX);

        //System.out.println("DEBUG thresholdNew = " + thresholdNew);
        return thresholdNew;
    }

    /**
     * Function that uses the OtsuMethod to generate a optimal threshold.
     * Note that since this only work with two classes, there is an assumption that
     * all the black pixels are excluded from the list
     * @param histogram list of 256 elements containing the stem values for every pixel intensity
     * @return the threshold
     */
    public double getThresholdOtsuMethod(int[] histogram) {

        // Clear low values for now
        // Later, we want to attribute a smaller weight
        // double[] windowQuadratic = getWindowQuadratic(20, 0.01);
        System.out.println("FILTER_VALUE = " + FILTER_VALUE);
        int totalPixels = 0;
        int[] windowSharp = getWindowSharp(FILTER_VALUE);
        for (int i = 0; i < 256; i++) {
            histogram[i] = (int) Math
                    .round(1.0 * histogram[i] * windowSharp[i]);
            totalPixels += histogram[i];
        }


        double u = getAverageIntensity(histogram); // average image intensity

        System.out.println("DEBUG u value = " + u);

        double u1; // Class means
        double varMax = 0; // maximum intraclass variance = minimum interclass variance
        double q1, q2; // class probabilities
        double sigmaB; // inter-class variance
        int threshOfMax = (int) u;

		/*
		 * Between class variance sigmaB^2 = q1*(1-q1)*(u1-u2)^2 --> minimize!
		 * OR 
		 * sigmaB^2 = [u(t) - u*theta(t)]^2/[theta(t)*(1-theta(t))]
		 * theta(t)--> Points classified as background 
		 * 1 - theta(t) --> Points classified as object
		 * 
		 * Within-class variance sigmaW^2 = q1*sigma1^2 + q2*sigma2^2
		 */

        // initialization
        q1 = 1.0 * histogram[0] / totalPixels;
        q2 = 1 - q1;
        u1 = 0;

        // Recursion
        for (int t = 1; t < 256; t++) {

            if (q1 * q2 != 0)
                sigmaB = (u1 - u * q1) * (u1 - u * q1) / (q1 * q2);
            else
                sigmaB = 0;

//            System.out.println("DEBUG iteration = " + t + " ... sigmaVal = "
//                    + sigmaB);
            q1 = q1 + 1.0 * histogram[t] / totalPixels;
            q2 = 1 - q1;

            u1 = u1 + 1.0 * t * histogram[t] / totalPixels;

            if (sigmaB > varMax) {
                varMax = sigmaB;
                threshOfMax = t;
            }
        }

        return threshOfMax;
    }


    /**
     * Applies a gaussian filter to get rid of peaks. The threshold should ideally
     * be between the two "maxima", in the point of zero derivative.
     *
     * This method assumes that this occurs. Errors might result in the current implementation
     * if there is not a "valley" right after a maxima.
     *
     * In the case that it doesn't occur, then the algorithm searches for the case
     * where the derivative starts to increase right after it starts to decrease (
     * pretty much the point with second derivative = 0)
     *
     * @param histogram2
     * @return The threshold using the zeroDerivative method
     */
    public int getThresholdZeroDerivative(int[] histogram2) {

        int histSize = histogram2.length;
        int threshold = -1;
        // Normalize vector
        int totalPixels = 0;
        for (int i = 0; i < 256; i++) {
            totalPixels += histogram2[i];
        }
        // Ok the 2 is just so I can do this ;D
        double histogram[] = new double[256];
        for (int i = 0; i < 256; i++) {
            histogram[i] = 1.0f * histogram2[i] / totalPixels;
        }

        // Create Gaussian filter
        double sigma = 5.0;
        double Ng = 6 * sigma + 1; // 3*sigma for each side
        double[] gfilter = new double[(int) (Ng + 0.5d)];
        for (int i = 0; i < Ng; i++) {
            gfilter[i] = Math.exp(-(i - 3 * sigma) * (i - 3 * sigma) / (2 * sigma * sigma))
                    / Math.sqrt(2 * Math.PI * sigma * sigma);
        }

        // DEBUG
        System.out.println("DEBUG: the gaussian filter is : " + Arrays.toString(gfilter));
        System.out.println("DEBUG: the original signal is : "
                + Arrays.toString(histogram));
        // Convolve with histogram
        double filteredSignal[] = convolve(histogram, gfilter);

        //DEBUG
        checkConvolve();
        System.out.println("DEBUG: the convolved signal is : "
                + Arrays.toString(filteredSignal));

        // Find maxsize
        int maxVal = 0;
        for (int i = 0; i < histSize; i++) {
            if (filteredSignal[i] > filteredSignal[maxVal]) {
                maxVal = i;
            }
        }
        System.out.println("DEBUG: maximum value is : " + maxVal);

        // Find zero derivative after the first maxima
        for (int i = maxVal; i < 252; i++) {
            if ((filteredSignal[i + 1] > filteredSignal[i]) &&
                    (filteredSignal[i + 2] > filteredSignal[i + 1]) &&
                    (filteredSignal[i + 3] > filteredSignal[i + 2])) {
                threshold = i;
                break;
            }
        }

        if (threshold < 0) // this means that there was no "maximum" point
        {
            // try to find points with small slope
            double slope, nextSlope, nextNextSlope; // one more for ensurance
            boolean proceedToTest = false;
            for (int i = maxVal; i < 252; i++) {
                slope = Math.abs(filteredSignal[i + 1] - filteredSignal[i]);
                nextSlope = Math.abs(filteredSignal[i + 2] - filteredSignal[i + 1]);
                nextNextSlope = Math.abs(filteredSignal[i + 3] - filteredSignal[i + 2]);

                if ((!proceedToTest) && (nextNextSlope < nextSlope) && (nextSlope < slope)) {
                    proceedToTest = true; // This means that the slope is decreasing, and we want
                    // to capture when it starts increasing again
                }


                if (proceedToTest) {

                    if (nextNextSlope > nextSlope && nextSlope > slope) {
                        threshold = i;
                        break;
                    }
                }
            }
        }

        return threshold - 15;

    }

    /**
     * Since the parameter being used by the
     *
     * @param histogram
     * @return The threshold using the integral method
     */
    public int getThresholdIntegralMethod(int[] histogram) {
        int threshold = -1;


        return threshold;


    }

    /**
     * Graph class used to plot the histogram
     */
    protected class Graph extends JPanel {

        private static final long serialVersionUID = 1L; // not sure
        protected static final int MIN_BAR_WIDTH = 4;

        private Map<Integer, Integer> mapHistory; // Map that contains the key-value pairs for the histogram stem plot

        public Graph(Map<Integer, Integer> mapHistory) {
            this.mapHistory = mapHistory;
            // int width = (mapHistory.size() * MIN_BAR_WIDTH) + 11;
            int width = 256 * MIN_BAR_WIDTH + 11;
            Dimension minSize = new Dimension(width, 128);
            Dimension prefSize = new Dimension(width, 256);
            setMinimumSize(minSize);
            setPreferredSize(prefSize);
        }

        /**
         * Function that creates the rectangles, lines, etc. and displays the histogram
         * In a panel.
         * @param g graphics object to paint
         */
        @Override
        protected void paintComponent(Graphics g) {

            int otsuThreshold, globalThreshold, zeroThreshold;

            super.paintComponent(g);
            if (mapHistory != null) {
                // Get the threshold via algorithm
                int[] histogram = convertFromMapToInt(mapHistory);

                otsuThreshold = (int) getThresholdOtsuMethod(histogram);
                globalThreshold = (int) getThresholdGlobalThresholding(histogram);
                zeroThreshold = getThresholdZeroDerivative(histogram);


                System.out.println("DEBUG otsuThreshold = " + otsuThreshold);
                System.out
                        .println("DEBUG globalThreshold = " + globalThreshold);
                System.out.println("DEBUG zeroThreshold = " + zeroThreshold);

                // Dimension bookkeeping
                int xOffset = 5;
                int yOffset = 5;
                int width = getWidth() - 1 - (xOffset * 2);
                int height = getHeight() - 1 - (yOffset * 2);

                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setColor(Color.DARK_GRAY);
                g2d.drawRect(xOffset, yOffset, width, height);
				/*
				 * int barWidth = Math.max( MIN_BAR_WIDTH, (int)
				 * Math.floor((float) width / (float) mapHistory.size()));
				 */

                int windowHeight = height / 2;
                int barWidth = MIN_BAR_WIDTH;
                // System.out.println("width = " + width + "; size = "
                // + mapHistory.size() + "; barWidth = " + barWidth);
                int maxValue = 0;

                // Find maxValue of the histogram to aid in printing
                for (Integer key : mapHistory.keySet()) {
                    int value = mapHistory.get(key);
                    if (key >= LOWEST_TO_PRINT) maxValue = Math.max(maxValue, value);
                }

                int xPos = xOffset;
                // int transformedThreshold = (int) ((float) threshold
                // * (float) mapHistory.size() / 256.0);
                int transformedThreshold = globalThreshold; // save some
                // rewrites

                for (int key = 0; key < 256; key++) {
                    int value;
                    if (mapHistory.containsKey(key)) {
                        value = mapHistory.get(key);
                    } else {
                        value = 0;
                    }

                    int barHeight;
                    if (key < LOWEST_TO_PRINT) barHeight = 0;
                    else {
                        barHeight = Math
                                .round(((float) value / (float) maxValue) * height);
                    }
                    g2d.setColor(new Color(key, key, key));

                    int yPos = height + yOffset - barHeight;

                    Rectangle2D bar = new Rectangle2D.Float(xPos, yPos,
                            barWidth, barHeight);

                    // LOGIC TO PRINT OUT THE COLORED RECTANGLES!!!
                    // IMPORTANT
                    if (transformedThreshold == (xPos - xOffset) / barWidth) {
                        g2d.setColor(Color.RED);
                        g2d.fill(bar);
                    } else if (otsuThreshold == (xPos - xOffset) / barWidth) {
                        g2d.setColor(Color.GREEN);
                        g2d.fill(bar);
                    } else if (zeroThreshold == (xPos - xOffset) / barWidth) {
                        g2d.setColor(Color.BLUE);
                        g2d.fill(bar);
                    } else
                        g2d.fill(bar);
                    g2d.setColor(Color.DARK_GRAY);

                    g2d.draw(bar);

                    // Show the window used
                    g2d.setColor(Color.CYAN);
                    if (key > FILTER_VALUE)
                        g2d.drawLine(xPos, windowHeight, xPos - barWidth,
                                windowHeight);
                    else {
                        if (key == FILTER_VALUE)
                            g2d.drawLine(xPos, height - 2, xPos, windowHeight);

                        g2d.drawLine(xPos, height - 2, xPos - barWidth,
                                height - 2);

                    }

                    xPos += barWidth;

                }

                // TEXT TO SHOW THE LEGEND OF THE COLORS USED
                String threshValues1 = "Threshold from Otsu (green): "
                        + otsuThreshold;
                String threshValues2 = "Threshold from Global (red): "
                        + globalThreshold;
                String threshValues3 = "Threshold from Zero (blue): "
                        + zeroThreshold;

                String highlightWindow = "In cyan is the window to clear background";

                g2d.setColor(Color.BLACK);
                g2d.drawString(threshValues1, width - 250, 20);
                g2d.drawString(threshValues2, width - 250, 35);
                g2d.drawString(threshValues3, width - 250, 50);
                g2d.drawString(highlightWindow, width - 250, 75);

                g2d.dispose();

            }
        }
    }

    /** Calculates the convolution between two vectors. Assumes filterSize < histogramSize
     * @param histogram list of 256 elements containing the stem values for every pixel
     * @param filter filter containing the weights for convolution
     * @return the new histogram values after filtering has been applied
     */
    double[] convolve(double histogram[], double filter[]) {

        // assumes filterSize < histogramSize
        int histSize = histogram.length;
        int filterSize = filter.length;
        int answerlength = histSize + filterSize - 1;

        double answer[] = new double[answerlength];

        // zero padded for convolution
        double paddedHistogram[] = new double[answerlength];

        for (int i = 0; i < histSize; i++) {
            paddedHistogram[i] = histogram[i];
        }

        // Apply convolution
        for (int i = 0; i < answerlength; i++) {
            answer[i] = 0;
            if (i < filterSize) {
                for (int j = 0; j <= i; j++) {
                    answer[i] += paddedHistogram[i - j] * filter[j];
                }
            } else {
                for (int j = 0; j <= filterSize - 1; j++) {
                    answer[i] += paddedHistogram[i - j] * filter[j];
                }
            }
        }

        return answer;
    }

    /**
     * Simple function to debug the convolution function
     * Creates two vectors and prints out their result
     */
    private void checkConvolve() {
        double hist[] = {1, 2, 3, 5, 4};
        double filter[] = {1.1, 1.2};

        System.out.println("Histogram dummy is: " + Arrays.toString(hist));
        System.out.println("Filter dummy is: " + Arrays.toString(filter));

        double result[] = convolve(hist, filter);
        System.out.println("Convolution is : " + Arrays.toString(result));


    }
}
