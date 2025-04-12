import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class FPGrowth {
    private static double minSupport;
    private static List<List<String>> transactions = new ArrayList<>();
    private static Map<String, Integer> itemSupport = new HashMap<>();
    private static List<String> frequentItems;
    private static FPTree fpTree;
    private static int totalMiningSteps = 0;
    private static int completedMiningSteps = 0;

    public static void main(String[] args) {
        long programStartTime = System.currentTimeMillis();

        if (args.length != 2) {
            System.out.println("arg1 -> folder name, arg2 -> minSupport [0,1)");
            return;
        }

        String input = args[0];
        minSupport = Double.parseDouble(args[1]);

        try {
            if (minSupport < 0.0 || minSupport > 1.0) {
                throw new IllegalArgumentException("Min support must be between 0 and 1.");
            }

            System.out.println("Minimum support: " + minSupport);

            long start = System.currentTimeMillis();
            readTransactionsFromFolder(input);
            long end = System.currentTimeMillis();
            System.out.printf("Read %d transactions in %d ms\n", transactions.size(), (end - start));

            // Calculate min support count (# of items in database needed to meet minsup)
            int minSupCount = (int) Math.ceil(transactions.size() * minSupport);

            long start2 = System.currentTimeMillis();
            buildFPTree(minSupCount);
            long end2 = System.currentTimeMillis();
            System.out.printf("Built FP-Tree in %d ms\n", (end2 - start2));

            long start3 = System.currentTimeMillis();
            Map<List<String>, Integer> frequentItemsets = mineFrequentItemsets(minSupCount);
            long end3 = System.currentTimeMillis();
            System.out.printf("Mined %d frequent itemsets in %d ms\n", frequentItemsets.size(), (end3 - start3));

            String outputFolder = "output";
            createOutputFolder(outputFolder);
            long programEnd = System.currentTimeMillis();
            long computationTime = programEnd - programStartTime;
            printResults(programStartTime, programEnd, frequentItemsets);
            saveResults(outputFolder, frequentItemsets, minSupport, computationTime);

        } catch (IOException e) {
            System.err.println(e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
        }
    }

    private static void printResults(long startTime, long endTime,
                                     Map<List<String>, Integer> frequentItemsets) {
        System.out.println("Computation Time: " + (endTime - startTime) + " ms");
        System.out.println("Transactions: " + transactions.size());
        System.out.println("Freq items count: " + frequentItems.size());
        System.out.println("Freq itemsets count: " + frequentItemsets.size());
    }

    private static void createOutputFolder(String folderName) throws IOException {
        File folder = new File(folderName);
        if (!folder.exists() && !folder.mkdir()) {
            throw new IOException();
        }
    }

    private static void saveResults(String outputFolder,
                                    Map<List<String>, Integer> frequentItemsets,
                                    double minSup, long computationTime) throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String outputFile = outputFolder + "/frequent_itemsets_" + timestamp + ".txt";

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
            writer.println("Minimum Support: " + minSup);
            writer.println("Minimum Support Count: " + (int) Math.ceil(transactions.size() * minSup));
            writer.println("Transactions in database: " + transactions.size());
            writer.println("Frequent items count: " + frequentItems.size());
            writer.println("Frequent itemsets count: " + frequentItemsets.size());
            writer.println("Computation Time: " + computationTime + " ms");
            writer.println();
            writer.println("Frequent Itemsets:");

            // Sort itemsets by support (descending) and then by size (ascending)
            List<Map.Entry<List<String>, Integer>> sortedEntries = new ArrayList<>(frequentItemsets.entrySet());
            sortedEntries.sort((e1, e2) -> {
                int cmp = e2.getValue().compareTo(e1.getValue());
                if (cmp == 0) return Integer.compare(e1.getKey().size(), e2.getKey().size());
                return cmp;
            }
            );
            // Write each itemset with its support count
            for (Map.Entry<List<String>, Integer> entry : sortedEntries) {
                writer.println(entry.getKey() + " appears " + entry.getValue() + " times.");
            }
        }
    }

    private static void readTransactionsFromFolder(String folderPath) throws IOException {
        File folder = new File(folderPath);
        if (!folder.isDirectory()) {
            throw new IOException("pathname or directory error");
        }
        File[] files = folder.listFiles();
        if (files == null) {
            throw new IOException("no file in input folder");
        }
        for (File file : files) {
            if (file.isFile()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.trim().isEmpty()) continue;
                        List<String> transaction = Arrays.stream(line.split("[\\s,]+")).filter(s -> !s.isEmpty()).collect(Collectors.toList());
                        if (!transaction.isEmpty()) {
                            transactions.add(transaction);
                        }
                    }
                }
            }
        }
    }

    private static void buildFPTree(int minSupCount) {
        // Pass 1: Calculate item supports
        for (List<String> transaction : transactions) {
            for (String item : transaction) {
                itemSupport.put(item, itemSupport.getOrDefault(item, 0) + 1);
            }
        }

        // Filter infrequent items and sort by support (descending)
        frequentItems = itemSupport.entrySet().stream().filter(entry -> entry.getValue() >= minSupCount).sorted((e1, e2) ->
                        {
                            int cmp = e2.getValue().compareTo(e1.getValue());
                            if (cmp == 0) return e1.getKey().compareTo(e2.getKey());
                            return cmp;
                        }
                    ).map(Map.Entry::getKey).collect(Collectors.toList());

        // Pass 2: Build FP-Tree
        fpTree = new FPTree();
        for (List<String> transaction : transactions) {
            List<String> filtered = transaction.stream().filter(item -> itemSupport.get(item) >= minSupCount).sorted(Comparator.comparingInt(item -> frequentItems.indexOf(item))).collect(Collectors.toList());
            fpTree.insertTransaction(filtered);
        }
    }

    private static Map<List<String>, Integer> mineFrequentItemsets(int minSupCount) {
        Map<List<String>, Integer> frequentItemsets = new HashMap<>();
        totalMiningSteps = frequentItems.size();
        completedMiningSteps = 0;

        // Adds single frequent items
        for (String item : frequentItems) {
            frequentItemsets.put(Collections.singletonList(item), itemSupport.get(item));
        }

        // Mine conditional patterns for each item ( least frequent to most)
        for (int i = frequentItems.size() - 1; i >= 0; i--) {
            String item = frequentItems.get(i);
            System.out.printf("Processing item %d/%d: %s\n", (frequentItems.size() - i), frequentItems.size(), item);

            long start4 = System.currentTimeMillis();
            Map<List<String>, Integer> conditionalPatterns = fpTree.findConditionalPatterns(item);

            for (Map.Entry<List<String>, Integer> entry : conditionalPatterns.entrySet()) {
                if (entry.getValue() >= minSupCount) {
                    List<String> newPattern = new ArrayList<>(entry.getKey());
                    newPattern.add(item);
                    Collections.sort(newPattern, Comparator.comparingInt(it -> frequentItems.indexOf(it)));
                    frequentItemsets.put(newPattern, entry.getValue());
                }
            }

            completedMiningSteps++;
            long end4 = System.currentTimeMillis();
            System.out.printf("%d ms (%d/%d items)\n", (end4 - start4), completedMiningSteps, totalMiningSteps);
        }

        return frequentItemsets;
    }
}