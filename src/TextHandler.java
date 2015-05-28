import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

// Class has methods for reading the inputs from txt and gathering data

public class TextHandler {


    public double[] readFile(String filestring) throws IOException {

        //STRUCTURE FOR THE PARAMETERS IS:
        //<sigma>,<thresh>,<mincurv>
//wj
        // another commetn
        double csvValues[] = new double[3]; //CSV = comma-separated-values
        ArrayList<String> stringlist = new ArrayList<String>();
        Scanner s = null;
        // String linestring;
        String[] divided;

        try {
            BufferedReader reader = new BufferedReader(new FileReader(
                    filestring));
            s = new Scanner(reader);

            while (s.hasNext()) {
                stringlist.add(s.nextLine());

            }

            // gets the LAST element of the list
            divided = stringlist.get(stringlist.size() - 1).split(",");
            csvValues[0] = Double.valueOf(divided[0]);
            csvValues[1] = Double.valueOf(divided[1]);
            csvValues[2] = Double.valueOf(divided[2]);

        } finally {
            if (s != null) {
                s.close();
            }
        }

        return csvValues;
    }

    public void writeFile(double[] Values, String filestring) throws IOException {

        double sigma = Values[0];
        double thresh = Values[1];
        double curv = Values[2];

        try (PrintWriter out = new PrintWriter(new BufferedWriter(
                new FileWriter(filestring, true)))) {
            out.println(sigma + "," + thresh + "," + curv);
        } catch (IOException e) {
            //exception handling left as an exercise for the reader
        }


    }

}