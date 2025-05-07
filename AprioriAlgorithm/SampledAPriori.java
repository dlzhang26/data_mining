import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLOutput;
import java.util.*;


/**
 * This Class is the sample based apriori class. It iterates through the transactions to compute the appropriate d bound,
 * then calculated the appropriate sample size based on the d bound. Next, it uses reservoir sampling to obtain the
 * sample of transactions and calls the Apriori algorithm based on the sample.
 */

public class SampledAPriori {

    Set<Integer> I = new HashSet<>();
    public List<Set<Integer>> transactions;
    List<Set<Integer>> sample;

    /**
     * THe SampledAPriori constructor takes in a path to the dataset and creates a list of sets to store the stranactions.
     * @param path the Path to the corresponding file
     */

    public SampledAPriori(Path path) {
        transactions = new ArrayList<>();
        try(BufferedReader br = Files.newBufferedReader(path)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] lineSplit = line.split(" ");
                Set<Integer> transaction = new HashSet<>();
                for (String stringItem : lineSplit) {
                    int item = Integer.parseInt(stringItem);
                    I.add(item);
                    transaction.add(item);
                }
                transactions.add(transaction);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method computes the d bound based on the combination of the longest transaction size and the number of times
     * that it appears.
     * @return the d bound based on the list of transactions
     */

    public int computeDBound() {
        int dBound = 1;
        Set<Set<Integer>> seen = new HashSet<>();

        // a custom comparator used to compare the of the sets and can break ties lexicographically
        Comparator<Set<Integer>> bySizeThenLex = (a, b) -> {
            int cmp = Integer.compare(a.size(), b.size());
            if (cmp != 0) return cmp;

            // optional: break ties lexicographically
            Iterator<Integer> itA = a.iterator(), itB = b.iterator();
            while (itA.hasNext() && itB.hasNext()) {
                int x = itA.next(), y = itB.next();
                if (x != y) return Integer.compare(x, y);
            }
            return 0;
        };
        SortedSet<Set<Integer>> R;

        Set<Integer> t = transactions.get(0);
        seen.add(t);
        Set<Set<Integer>> T = new HashSet<>();
        T.add(t);
        Map<Integer, Integer> countByLength = new HashMap<>();
        countByLength.merge(t.size(), 1, (a,b) -> a + b);

        //iterates through the transactions and add valid transactions
        for (Set<Integer> transaction : transactions) {
            if (transaction.size() > dBound && !transaction.equals(I) && !T.contains(transaction)) {
                R = new TreeSet<>(bySizeThenLex);
                R.addAll(T);
                R.add(transaction);
                countByLength.merge(transaction.size(), 1, (a,b) -> a + b);

                int maxLength = 0;
                for (int len : countByLength.keySet()) {
                    maxLength = Math.max(maxLength, len);
                }

                // returns the d bound as the largest transaction size such that there are at least the size worth's of
                // transactions are greater than that size.
                int totalCount = 0;
                for (int i = maxLength; maxLength > 0; i--) {
                    int lengthCount = countByLength.getOrDefault(i, 0);

                    if (lengthCount + totalCount >= i) {
                        dBound = i;
                        break;
                    }
                    totalCount = totalCount + lengthCount;
                }

                Iterator<Set<Integer>> descIt = ((TreeSet<Set<Integer>>) R).descendingIterator();
                while (descIt.hasNext() && T.size() < dBound) {
                    T.add(descIt.next());
                }
            }
        }
        return dBound;
    }

    /**
     * This method returns the ablsolute sample size based on the d bound, based on the Matteo Paper
     * @param dBound
     * @param C
     * @param eps
     * @param delta
     * @return the sample size necessary
     */

    public int computeSampleSizeAbsolute(int dBound, int C, double eps, double delta) {
        double formula = ((4*C) / Math.pow(eps, 2)) * (dBound + Math.log(1 / delta));
        return Math.min(transactions.size(), (int) Math.ceil(formula));
    }

    /**
     * This method returns the relative sample size based on the d bound, based on the Matteo Paper
     * @param dBound
     * @param C
     * @param eps
     * @param delta
     * @return the sample size necessary
     */

    public int computeSampleSizeRelative(int dBound, int C, double eps, double delta, double minSupport) {
        double formula = ((4*(2+eps)*C) / (Math.pow(eps, 2)) * minSupport * (2-eps))
                * ((dBound * Math.log((2+eps)/(minSupport*(2-eps)))) + Math.log(1 / delta));
        return Math.min(transactions.size(), (int) Math.ceil(formula));
    }

    /**
     * This method builds the reservoir sample with a given sample size
     * @param sampleSize the size of the desired reservoir sample
     * @return a list of transactions, with each transaction stored in a set
     */

    public List<Set<Integer>> buildSample(int sampleSize) {
        if (sampleSize >= transactions.size()) {
            return transactions;
        }

        List<Set<Integer>> copy = new ArrayList<>(transactions);
        Collections.shuffle(copy, new Random());
        return copy.subList(0, sampleSize);
    }

    public static void main(String args[]) {
        //String pathPrefix = "/Users/slatertot21/Python Projects/cosc_254/Final Project/AprioriSampling/input/";
        //Path transactionPath = Path.of(pathPrefix + "accidents.dat");
        Path transactionPath = Path.of("accidents.txt");
        //Path transactionPath = Path.of("retail.txt");
        double minFreq= 0.7;

        double startTime = System.currentTimeMillis();

        SampledAPriori alg = new SampledAPriori(transactionPath);
        int dBound = alg.computeDBound();
        System.out.println("D-Bound = " + dBound);
        int absSampleSize = alg.computeSampleSizeAbsolute(dBound, 1, 0.1, 0.1);
        int relSampleSize = alg.computeSampleSizeRelative(dBound, 1, 0.1, 0.1, minFreq);
        System.out.println("Sample Size Abs = " + absSampleSize);
        System.out.println("Sample Size Rel = " + relSampleSize);
        alg.sample = alg.buildSample(absSampleSize);

        APriori aPriori = new APriori(alg.sample);
        Map<Set<Integer>, Integer> frequentItemSets = aPriori.run(minFreq);


        double endTime = System.currentTimeMillis();
        double time = endTime - startTime;
        System.out.println("Runtime for Min Freq: " + minFreq + "\nTime: " + time);

    }
}
