import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class FPGrowthApprox {
    private static double minSupport;
    private static List<List<String>> transactions = new ArrayList<>();
    private static Map<String, Integer> itemSupport = new HashMap<>();
    private static List<String> frequentItems;
    private static FPTree fpTree;

    //Parameters for the VC-dimension sampling
    // Approximation error parameter (closer to 0 = more accurate)
    private static double epsilon = .1; //decrease by 1/2 to double sample size
    //test epsilon = .2, .15, .1, .05, .01
    private static double delta = 0.1;   // Probability of failure (smaller = more reliable)
    private static final double C = 1.0; // VC constant C
    //private static boolean False = false;
    private static int dboundforreturn = 0;

    // Variables for timing and error measurements
    private static long dboundTime = 0;
    private static int sampleSize = 0;

    private static int minSupReturn = 0;

    public static Map<List<String>, Integer> run(String inputDir, double support) throws IOException {
        minSupport = support;
        transactions.clear();
        itemSupport.clear();

        readTransactionsFromFolder(inputDir);
        int dBound = computeDBound();
        dboundforreturn = dBound;

        int sampleSize = Math.min(calculateSampleSize(dBound), transactions.size());
        List<List<String>> sample = reservoirSample(transactions, sampleSize);

        int minSupCount = Math.max(1, (int) Math.ceil(sample.size() * (minSupport - epsilon)));
        //minSupCount = min frequency threshold
        minSupReturn = minSupCount;

        buildFPTree(sample, minSupCount);
        return mineFrequentItemsets(minSupCount);
    }

    public static int getTransactionCount() {
        return transactions.size();
    }


    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Usage: <folder> <minSupport [0,1]>");
            return;
        }
        String input = args[0];
        minSupport = Double.parseDouble(args[1]);

        long start = System.currentTimeMillis();
        readTransactionsFromFolder(input);

        // Compute d-bound (VC-dimension upper bound)
        long dboundStart = System.currentTimeMillis();
        int dBound = computeDBound();
        dboundTime = System.currentTimeMillis() - dboundStart;
        dboundforreturn = dBound;

        // Calculate sample size using VC-dimension theory (Theorem 3.8 in paper)
        sampleSize = Math.min(calculateSampleSize(dBound), transactions.size());

        // Take reservoir sample (Algorithm 1 in paper)
        List<List<String>> sample = reservoirSample(transactions, sampleSize);

        // Adjust minimum support count for sample (Lemma 5.1)
        int minSupCount = Math.max(1, (int) Math.ceil(sample.size() * (minSupport - epsilon)));
        minSupReturn = minSupCount;

        // Build FP-tree from sample
        buildFPTree(sample, minSupCount);

        // Mine frequent itemsets from sample
        Map<List<String>, Integer> itemsets = mineFrequentItemsets(minSupCount);


        saveResults("output", itemsets, minSupport, System.currentTimeMillis() - start);
    }

    // Computes the d-bound (upper bound for VC-dimension) - Section 4.1 in paper
    private static int computeDBound() {
        int q = 1;
        Set<Set<String>> seen = new HashSet<>();
        List<Set<String>> list = new ArrayList<>();

        // This implements the greedy algorithm to find maximum q where there are
        // at least q transactions of length ≥ q (Definition 4.4)
        for (List<String> t : transactions) {
            Set<String> set = new HashSet<>(t);
            if (set.size() < q || !seen.add(set)) continue;
            list.add(set);

            // Maintain the largest q where we've seen ≥ q transactions of length ≥ q
            while (list.size() >= q) {
                q++;
                final int qFinal = q;
                list.removeIf(s -> s.size() < qFinal);
            }
        }
        return q - 1;
    }

    // Calculates sample size using VC-dimension theory - Theorem 3.8 in paper
    private static int calculateSampleSize(int d) {
        double eps = epsilon;
        // Equation (2) from paper: sample size depends on VC-dimension (d), ε, and δ
        double size = (4 * C / (eps * eps)) * (d + Math.log(1.0 / delta));
        System.out.println((int)Math.ceil(size));
        return (int) Math.ceil(size);
    }

    // Reservoir sampling algorithm - Section 3 in paper
    private static List<List<String>> reservoirSample(List<List<String>> data, int k) {
        if (k >= data.size()) return new ArrayList<>(data);
        List<List<String>> result = new ArrayList<>(data.subList(0, k));
        Random rand = new Random();

        // Standard reservoir sampling algorithm (Vitter's Method)
        for (int i = k; i < data.size(); i++) {
            int j = rand.nextInt(i + 1);
            if (j < k) result.set(j, data.get(i));
        }
        return result;
    }

    private static void buildFPTree(List<List<String>> sample, int minSup) {
        // Standard FP-tree construction (not part of approximation)
        itemSupport.clear();
        for (List<String> t : sample)
            for (String item : t)
                itemSupport.merge(item, 1, Integer::sum);

        frequentItems = itemSupport.entrySet().stream()
                .filter(e -> e.getValue() >= minSup)
                .sorted((a, b) -> b.getValue().equals(a.getValue()) ?
                        a.getKey().compareTo(b.getKey()) : b.getValue() - a.getValue())
                .map(Map.Entry::getKey).collect(Collectors.toList());

        Map<String, Integer> order = new HashMap<>();
        for (int i = 0; i < frequentItems.size(); i++) {
            order.put(frequentItems.get(i), i);
        }

        fpTree = new FPTree();
        for (List<String> t : sample) {
            List<String> filtered = t.stream()].filter(order::containsKey).sorted(Comparator.comparingInt(order::get)).collect(Collectors.toList());
            fpTree.insertTransaction(filtered);
        }
    }

    private static Map<List<String>, Integer> mineFrequentItemsets(int minSup) {
        // Standard FP-growth mining (not part of approximation)
        Map<List<String>, Integer> result = new HashMap<>();
        for (String item : frequentItems) {
            result.put(Collections.singletonList(item), itemSupport.get(item));
            for (Map.Entry<List<String>, Integer> entry :
                    fpTree.findConditionalPatterns(item).entrySet()) {
                if (entry.getValue() >= minSup) {
                    List<String> pattern = new ArrayList<>(entry.getKey());
                    pattern.add(item);
                    result.put(pattern, entry.getValue());
                }
            }
        }
        return result;
    }

    private static void readTransactionsFromFolder(String path) throws IOException {
        for (File f : Objects.requireNonNull(new File(path).listFiles())) {
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = br.readLine()) != null)
                    transactions.add(Arrays.stream(line.split("[\\s,]+")).filter(s -> !s.isEmpty()).collect(Collectors.toList()));
            }
        }
    }


    private static void saveResults(String outputFolder, Map<List<String>, Integer> frequentItemsets, double minSup, long computationTime) throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String outputFile = outputFolder + "/frequent_itemsets_" + timestamp + ".txt";
        int trueMinSupCount = (int) Math.ceil(transactions.size() * minSup);
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
            writer.println("Minimum Support: " + minSup);
            writer.println("Minimum Support Count: " +(int) Math.ceil(transactions.size() * minSup));
            writer.println("Transactions in database: " + transactions.size());
            writer.println("Frequent items count: " + frequentItems.size());
            writer.println("Frequent itemsets count: " + frequentItemsets.size());
            writer.println("Computation Time: " + computationTime + " ms");

            writer.println("DBound Computation Time: " + dboundTime + " ms");
            writer.println("Sample Size (VC): " + sampleSize);
            writer.println("Minimum Frequency Threshold: " +minSupReturn);

            writer.println("epsilon: " + epsilon);
            writer.println("delta: " + delta);
            writer.println("d-bound (q): " + dboundforreturn);
            writer.println();
            writer.println();
            writer.println("Frequent Itemsets:");

            List<Map.Entry<List<String>, Integer>> sortedEntries =
                    new ArrayList<>(frequentItemsets.entrySet());

            sortedEntries.sort((e1, e2) -> {int cmp = e2.getValue().compareTo(e1.getValue());
                if (cmp == 0)
                    return Integer.compare(e1.getKey().size(), e2.getKey().size());

                return cmp;
            });

            //for (Map.Entry<List<String>, Integer> entry : sortedEntries) {
            //    if (entry.getValue() >= trueMinSupCount) {
            //        writer.println(entry.getKey() + " appears " + entry.getValue() + " times.");
            //    }
            //}
            for (Map.Entry<List<String>, Integer> entry : sortedEntries) {
                writer.println(entry.getKey() + " appears " + entry.getValue() + " times.");
            }
        }
    }
}