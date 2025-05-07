import java.io.*;
import java.util.*;

public class calculateARErrors {
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            return;
        }

        String inputFolder = args[0];
        double minSupport = Double.parseDouble(args[1]);

        System.out.println("Running FPGrowth and FPGrowthApprox.");

        //run exact and approximate mining
        Map<List<String>, Integer> exactItemsets = FPGrowth.run(inputFolder, minSupport);
        Map<List<String>, Integer> approxItemsets = FPGrowthApprox.run(inputFolder, minSupport);
        int totalTransactions = FPGrowth.getTransactionCount();

        double freqErrorSum = 0.0;
        double confErrorSum = 0.0;
        int ruleCount = 0;

        for (Map.Entry<List<String>, Integer> entry : exactItemsets.entrySet()) {
            List<String> itemset = entry.getKey();
            int support = entry.getValue();
            if (itemset.size() < 2) continue;

            int size = itemset.size();
            for (int mask = 1; mask < (1 << size) - 1; mask++) {
                List<String> A = new ArrayList<>();
                List<String> B = new ArrayList<>();

                for (int i = 0; i < size; i++) {
                    if ((mask & (1 << i)) != 0) A.add(itemset.get(i));
                    else B.add(itemset.get(i));
                }

                if (A.isEmpty() || B.isEmpty()) continue;

                // Exact support and confidence
                double exactFreq = support / (double) totalTransactions;
                int aSupport = exactItemsets.getOrDefault(A, 0);
                if (aSupport == 0) continue;
                double exactConf = support / (double) aSupport;

                // Approximate support and confidence
                int approxSup = approxItemsets.getOrDefault(itemset, 0);
                int approxASup = approxItemsets.getOrDefault(A, 0);
                if (approxASup == 0) continue;
                double approxFreq = approxSup / (double) totalTransactions;
                double approxConf = approxSup / (double) approxASup;
                freqErrorSum += Math.abs(exactFreq - approxFreq);
                confErrorSum += Math.abs(exactConf - approxConf);
                ruleCount++;
            }
        }

        double arFreqError = ruleCount == 0 ? 0 : freqErrorSum / ruleCount;
        double arConfError = ruleCount == 0 ? 0 : confErrorSum / ruleCount;

        String outputFile = "output/ar_errors.txt";
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
            writer.println("ArFreqEr: " + arFreqError);
            writer.println("ArConfEr: " + arConfError);
            writer.println("Rules compared: " + ruleCount);
        }
        System.out.println("ArFreqEr: " + arFreqError);
        System.out.println("ArConfEr: " + arConfError);
    }
}
