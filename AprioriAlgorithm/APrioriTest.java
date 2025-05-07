import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

/**
 * This class tests the exact Apriori method for different frequency thresholds
 */

public class APrioriTest {
    public static void main(String args[]){
        //String[] files = {"accidents.txt", "retail.txt"};
        String[] files = { "retail.txt"};

        String outputDir = "exact_output";

        new File(outputDir).mkdirs();

        double[] minFreq = {0.2, 0.1, 0.05,0.01,0.005,0.001,0.0001};

        for(String inputFile: files) {
            Path transactionPath = Path.of(inputFile);
            for (double freq : minFreq) {
                String filename = outputDir + "/minfreq_" + inputFile +"_"+ freq + ".csv";
                try (PrintWriter out = new PrintWriter(new FileWriter(filename))) {
                    out.println("min_freq,time");
                    double startTime = System.currentTimeMillis();

                    APriori aPriori = new APriori(transactionPath);
                    Map<Set<Integer>, Integer> frequentItemSets = aPriori.run(freq);

                    double endTime = System.currentTimeMillis();
                    double time = endTime - startTime;
                    System.out.println("Writing result for minFreq = " + freq + " to " + filename);
                    out.println(freq + "," + time);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }
        }
    }
}
