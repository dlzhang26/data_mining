import java.io.*;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public class Experiments {
    public static void main(String args[]){
        //String[] files = {"test.txt"};
        String[] files = {"accidents.txt", "retail.txt"};

        String outputDir = "sampling_output";

        new File(outputDir).mkdirs();

        double[] minFreq = { 0.35};

        for(String inputFile: files) {
            Path transactionPath = Path.of(inputFile);
            for (double freq : minFreq) {
                String filename = outputDir + "/minfreq_" + inputFile +"_"+ freq + ".csv";
                try (PrintWriter out = new PrintWriter(new FileWriter(filename))) {
                    out.println("min_freq,time");
                    double startTime = System.currentTimeMillis();
                    SampledAPriori alg = new SampledAPriori(transactionPath);
                    int dBound = alg.computeDBound();
                    int absSampleSize = alg.computeSampleSizeAbsolute(dBound, 1, 0.1, 0.1);
                    //int relSampleSize = alg.computeSampleSizeRelative(dBound, 1, 0.1, 0.1, freq);

                    alg.sample = alg.buildSample(absSampleSize);

                    APriori aPriori = new APriori(alg.sample);
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
