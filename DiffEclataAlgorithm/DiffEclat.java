import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class DiffEclat {

    private List<SortedSet<Integer>> transactions;
    private double minSup;
    private int minSupCount;
    private Path transactionPath;
    private int setSize;
    private double startTime;
    private double endTime;

    public DiffEclat() {
        System.out.println("STARTING DIFF-ECLAT");
        transactionPath = null;
    }

    public void setup(Path inputPath) {
        transactionPath = inputPath;
    }

    public DiffEclatTree run2(double minimumSupport) {
        System.out.println("- Run new support threshold");
        minSup = minimumSupport;

        System.out.println("    " + "- Count supports");
        // For each transaction (line) in dataset count item support
        Map<Integer, Integer> itemSupports = new HashMap<>();
        setSize = 0;
        try(BufferedReader br = Files.newBufferedReader(transactionPath)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] lineSplit = line.split(" ");
                for (String stringItem : lineSplit) {
                    int item = Integer.parseInt(stringItem);
                    itemSupports.merge(item, 1, (a,b) -> a + b);
                }
                setSize++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("    " + "- Determine frequent items");
        // Determine frequent items
        Map<Integer, Integer> frequentItemSupports = new HashMap<>();
        minSupCount = (int) Math.ceil(minSup * setSize);
        for (Map.Entry<Integer, Integer> entry : itemSupports.entrySet()) {
            if (entry.getValue() >= minSupCount) {
                frequentItemSupports.put(entry.getKey(), entry.getValue());
            }
        }
        // Clear outdated hashmap to save space
        itemSupports = new HashMap<>();

        System.out.println("    " + "- Create diffsets for frequent items");
        // Create diffsets for frequent items
        Map<Integer, BitSet> itemDiffs = new HashMap<>();
        HashSet<Integer> transaction;
        int transactionLine = 0;
        try(BufferedReader br = Files.newBufferedReader(transactionPath)) {
            String line;
            while ((line = br.readLine()) != null) {

                // Convert transaction string to hashset
                String[] lineSplit = line.split(" ");
                transaction = new HashSet<>();
                for (String stringItem : lineSplit) {
                    int item = Integer.parseInt(stringItem);
                    transaction.add(item);
                }

                // Add transaction to diffset of each item not found
                for (Map.Entry<Integer, Integer> itemPair : frequentItemSupports.entrySet()) {
                    if (!transaction.contains(itemPair.getKey())) {
                        BitSet diffset = itemDiffs.getOrDefault(itemPair.getKey(), new BitSet());
                        diffset.set(transactionLine);
                        itemDiffs.put(itemPair.getKey(), diffset);
                    }
                }
                transactionLine++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("    " + "- Create 1-set triples");
        // Create triple for each item
        List<Triple> triples = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : frequentItemSupports.entrySet()) {
            Integer item = entry.getKey();
            SortedSet<Integer> itemset = new TreeSet<>();
            itemset.add(item);
            int itemSupport = frequentItemSupports.get(item);
            BitSet diffset = itemDiffs.get(item);
            triples.add(new Triple(itemset, diffset, itemSupport));
        }

        System.out.println("    " + "- Run DEclat");
        // Run DEclat
        DiffEclatTree diffTree = new DiffEclatTree();
        DiffEclat(triples, minSupCount, diffTree);

        // Return tree
        return diffTree;
    }

    public DiffEclatTree run(double minimumSupport) {
        System.out.println("- New support threshold");
        minSup = minimumSupport;

        System.out.println("    " + "- Count supports and tidsets");
        // For each transaction (line) in dataset count item support and build item tid-sets
        Map<Integer, Integer> itemSupports = new HashMap<>();
        Map<Integer, BitSet> itemTids = new HashMap<>();
        List<Integer> allTids = new ArrayList<>();
        int transactionNum = 0;
        try(BufferedReader br = Files.newBufferedReader(transactionPath)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] lineSplit = line.split(" ");
                for (String stringItem : lineSplit) {
                    int item = Integer.parseInt(stringItem);
                    itemSupports.merge(item, 1, (a,b) -> a + b);
                    BitSet tidset = itemTids.getOrDefault(item, new BitSet());
                    tidset.set(transactionNum);
                    itemTids.put(item, tidset);
                }
                allTids.add(transactionNum);
                transactionNum++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("    " + "- Create 1-set triples");
        // Create triple for each item
        minSupCount = (int) Math.ceil(minSup * transactionNum);
        List<Triple> triples = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : itemSupports.entrySet()) {
            Integer item = entry.getKey();
            SortedSet<Integer> itemset = new TreeSet<>();
            itemset.add(item);
            int itemSupport = itemSupports.get(item);
            if (itemSupport >= minSupCount) {
                BitSet itemTidset = itemTids.get(item);
                BitSet diffset = new BitSet();
                for (Integer tid : allTids) {
                    if (!itemTidset.get(tid)) {
                        diffset.set(tid);
                    }
                }
                triples.add(new Triple(itemset, diffset, itemSupport));
            }
        }

        System.out.println("    " + "- Run DiffEclat");
        // Run DEclat
        DiffEclatTree diffTree = new DiffEclatTree();
        DiffEclat(triples, minSupCount, diffTree);

        // Return tree
        return diffTree;
    }

    private void DiffEclat(List<Triple> frequentTriples, Integer minSupport, DiffEclatTree tree) {
        for (int i = 0; i < frequentTriples.size(); i++) {

            tree.insertItemset(frequentTriples.get(i).getItemset(), frequentTriples.get(i).getSupport());

            List<Triple> newFrequentTriples = new ArrayList<>();
            for (int j = i + 1; j < frequentTriples.size(); j++) {
                SortedSet<Integer> itemA = frequentTriples.get(i).getItemset();
                SortedSet<Integer> itemB = frequentTriples.get(j).getItemset();
                SortedSet<Integer> finalItem = new TreeSet<>(itemA);
                finalItem.addAll(itemB);

                BitSet diffsetA = frequentTriples.get(i).getDiffset();
                BitSet diffsetB = frequentTriples.get(j).getDiffset();
//                System.out.println("ItemA = " + itemA + ", ItemB = " + itemB);
//                System.out.println("DiffsetA = " + diffsetA + ", DiffsetB = " + diffsetB);
                BitSet finalDiffset = (BitSet) diffsetB.clone();
                finalDiffset.andNot(diffsetA);

                int finalSupport = frequentTriples.get(i).getSupport() - finalDiffset.cardinality();

                if (finalSupport >= minSupport) {
                    newFrequentTriples.add(new Triple(finalItem, finalDiffset, finalSupport));
                }
            }

            if (newFrequentTriples.size() > 0) {
                DiffEclat(newFrequentTriples, minSupport, tree);
            }
        }
    }

    public static void main(String[] args) {
        fulltests();
    }

    public static void test() {
        Path filePath = Paths.get("/Users/slatertot21/Python Projects/cosc_254/Final Project/DiffEclat/Datasets/accidents.dat");

        DiffEclat eclat = new DiffEclat();
        eclat.setup(filePath);
        DiffEclatTree result = eclat.run(0.5);
        System.out.println();
        System.out.println("PRINT TREE");
        result.printTree();
        System.out.println();
        System.out.println("Tree Size: " + result.size());
    }

    public static void fulltests() {
        double startTime;
        double endTime;

        // Setup paths for testing
        HashSet<String> setNames = new HashSet<>();
        setNames.add("accidents");
        //setNames.add("retail");
//        setNames.add("T10I4D100K");
//        setNames.add("T40I10D100K");
//        setNames.add("webdocs");



        // Setup thresholds for testing
        //SortedSet<Double> thresholds = new TreeSet<>();
//, 0.05,0.01,0.005,0.001,0.0001
        Double [] thresholds = { 0.14,0.13, 0.12, 0.11};

//        for (double i = 0.5; i <= 0.001; i += 0.01) {
//            thresholds.add(i);
//        }

        // Initialize eclat
        DiffEclat eclat = new DiffEclat();
        DiffEclatTree result;

        // Setup CSV output file
        StringBuilder csvContent = new StringBuilder();
        csvContent.append("Threshold,# Itemsets, Length Longest Itemset,Runtime(s)\n");

        int count = 1;
        for (String name : setNames) {
            eclat.setup(Paths.get("accidents.txt"));

            //eclat.setup(Paths.get("/Users/slatertot21/Python Projects/cosc_254/Final Project/DiffEclat/Datasets/" + name + ".dat"));
            for (double threshold : thresholds) {
                startTime = System.currentTimeMillis();

                result = eclat.run(threshold);

                endTime = System.currentTimeMillis();

                System.out.println();
                System.out.println("DATASET = " + count + " with THRESHOLD = " + threshold);
                System.out.println("    Itemsets = " + result.size());
                System.out.println("    Length Longest Itemset = " + result.depth());
                System.out.println("    Runtime = " + (endTime-startTime)/1000 + "s");

                // Append the results as a CSV row.
                csvContent.append(count).append(",")
                        .append(threshold).append(",")
                        .append(result.size()).append(",")
                        .append(result.depth()).append(",")
                        // Dividing by 1000 to convert milliseconds to seconds.
                        .append((endTime - startTime)/1000).append("\n");
            }
            count++;

            // Output CSV file
            try (FileWriter writer = new FileWriter("Datasets/" + name + "results.csv")) {
                writer.write(csvContent.toString());
                System.out.println("CSV file written successfully.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
