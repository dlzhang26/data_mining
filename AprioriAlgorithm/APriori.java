import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * The Apriori class takes in either a path or a list of sets of transactions and a minimum frequency. This class
 * returns a map of the frequent itemsets stored in a HashSet with its corresponding support
 */

public class APriori {
    List<Set<Integer>> transactions;
    Map<Integer, Integer> itemSupports;

    private double minSupport;
    private int minSupportCount;

    double startTime;
    double endTime;

    /**
     * This is the constructor used to directly read a data file and create the list of transactions
     * @param path the Path to the corresponding file
     */

    public APriori(Path path) {
        System.out.println("Starting support counting");
        transactions = new ArrayList<>();
        itemSupports = new HashMap<>();

        try(BufferedReader br = Files.newBufferedReader(path)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] lineSplit = line.split(" ");
                Set<Integer> transaction = new HashSet<>();
                for (String stringItem : lineSplit) {
                    int item = Integer.parseInt(stringItem);
                    transaction.add(item);
                    itemSupports.merge(item, 1, (a,b) -> a + b);
                }
                transactions.add(transaction);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This is the constructor used when the file has already been read elsewhere. This is used in the sampled based
     * Apriori algorithm
     * @param inputTransactions
     */

    public APriori(List<Set<Integer>> inputTransactions) {
        transactions = inputTransactions;
        itemSupports = new HashMap<>();
        for (Set<Integer> transaction : transactions) {
            for (Integer item : transaction) {
                itemSupports.merge(item, 1, (a,b) -> a + b);
            }
        }
        System.out.println("Apriori Initialized with " + transactions.size() + " transactions");
    }

    /**
     * This method uses the transactions in the constructor and an inputted minimum frequency to get the frequent itemsets
     * The method first iterates through the transactions to find the frequent items (frequent itemsets size 1). To get
     * the frequent itemsets size k=2 or more, a while loop is used to generate candidates, prune the candidates, and
     * get the candidates to return the itemsets that meet the minimum support
     * @param minimumSupport the minimum support to run the method
     * @return
     */

    public Map<Set<Integer>, Integer> run(double minimumSupport) {
        System.out.println("Start frequent 1 set creation");
        this.minSupport = minimumSupport;
        this.minSupportCount = (int) Math.ceil(minSupport * transactions.size());
        Map<Set<Integer>, Integer> allFrequentItemsets = new HashMap<>();

        Map<Set<Integer>, Integer> frequentItemsets = new HashMap<>();
        Set<Integer> itemset;
        for (Map.Entry<Integer, Integer> entry : itemSupports.entrySet()) {
            int item = entry.getKey();
            int support = entry.getValue();
            if (support >= minSupportCount) {
                itemset = new HashSet<>();
                itemset.add(item);
                frequentItemsets.put(itemset, support);
            }
        }

        int k = 1;
        while (!frequentItemsets.isEmpty()) {
            allFrequentItemsets.putAll(frequentItemsets);

            //Generate candidates and prune based on downward closure property
            Map<Set<Integer>, Integer> candidates = createCandidates(new ArrayList<>(frequentItemsets.keySet()), k + 1);
            Map<Set<Integer>, Integer> candidateCounts = new HashMap<>();

            //count support
            for (Set<Integer> transaction : transactions) {
                for (Set<Integer> candidate : candidates.keySet()) {
                    if (transaction.containsAll(candidate)) {
                        candidateCounts.merge(candidate, 1, (a, b) -> a + b);
                    }
                }
            }

            // add frequent size k itemsets
            frequentItemsets = new HashMap<>();
            for (Map.Entry<Set<Integer>, Integer> entry : candidateCounts.entrySet()) {
                if (entry.getValue() >= minSupportCount) {
                    frequentItemsets.put(entry.getKey(), entry.getValue());
                }
            }

            endTime = System.currentTimeMillis();
            System.out.println(k + "-Candidate creation took " + (endTime-startTime)/1000 + " seconds" );

            k++;
        }

        return allFrequentItemsets;
    }

    /**
     * This method creates the candidates by union two sets and checking if it is the correct. If the size is correct,
     * the itemset is added to either the candidates or the non-candidate set.
     * @param itemsets the list of size k itemsets
     * @param k the needed union size
     * @return the map of the frequent itemsets.
     */

    private Map<Set<Integer>, Integer> createCandidates(List<Set<Integer>> itemsets, int k) {

        startTime = System.currentTimeMillis();
        Map<Set<Integer>, Integer> candidates = new HashMap<>();
        Set<Set<Integer>> nonCandidates = new HashSet<>();
        int n = itemsets.size();

        // For each consecutive itemset, look at each following itemset
        for (int i = 0; i < n - 1; i++) {
            for (int j = i + 1; j < n; j++) {
                Set<Integer> candidate  = new HashSet<>(itemsets.get(i));
                candidate.addAll(itemsets.get(j));
                if (candidate.size() == k) {
                    if (!nonCandidates.contains(candidate) &&
                            !candidates.containsKey(candidate) &&
                            checkDownwardClosure(candidate, itemsets)) {
                        candidates.put(candidate, 0);
                    } else {
                        nonCandidates.add(candidate);
                    }
                }
            }
        }

        return candidates;
    }

    /**
     * This method checks the downward closure property and ensures that every subset of a potential itemset candidate is
     * frequent
     * @param candidate the potential candidate stored in a set
     * @param prevFrequentItemSets the size k-1 frequent itemsets
     * @return
     */

    public boolean checkDownwardClosure(Set<Integer> candidate, List<Set<Integer>> prevFrequentItemSets) {
        for (Integer item : candidate) {
            Set<Integer> subset = new HashSet<>(candidate);
            subset.remove(item);
            if (!prevFrequentItemSets.contains(subset)) {
                return false;
            }
        }
        return true;
    }
}