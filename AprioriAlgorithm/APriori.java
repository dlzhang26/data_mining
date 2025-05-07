import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class APriori {
    List<Set<Integer>> transactions;
    Map<Integer, Integer> itemSupports;

    private double minSupport;
    private int minSupportCount;

    double startTime;
    double endTime;

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
                    double endTime;
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
        System.out.println("Start loop");
        while (!frequentItemsets.isEmpty()) {
            System.out.println("Start creating " + k + "-candidates");
            System.out.println("   - With " + frequentItemsets.size() + " itemsets");

            allFrequentItemsets.putAll(frequentItemsets);

            //Generate candidates and prune based on downward closure property
            Map<Set<Integer>, Integer> candidates = createCandidates(new ArrayList<>(frequentItemsets.keySet()), k + 1);
            Map<Set<Integer>, Integer> candidateCounts = new HashMap<>();

            System.out.println("Support Counting Candidates");
            System.out.println("   - " + transactions.size() + " transactions");
            System.out.println("   - " + candidates.size() + " candidates");
            for (Set<Integer> transaction : transactions) {
                for (Set<Integer> candidate : candidates.keySet()) {
                    if (transaction.containsAll(candidate)) {
                        candidateCounts.merge(candidate, 1, (a, b) -> a + b);
                    }
                }
            }

            System.out.println("Last for loop");
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

    private Map<Set<Integer>, Integer> createCandidates(List<Set<Integer>> itemsets, int k) {
//        System.out.println("Start creating " + k + "-candidates");
//        System.out.println("   - With " + itemsets.size() + " itemsets");
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
//        endTime = System.currentTimeMillis();
//        System.out.println(k + "-Candidate creation took " + (endTime-startTime)/1000 + " seconds" );
        return candidates;
    }

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