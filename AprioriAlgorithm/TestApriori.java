import java.nio.file.Path;
import java.util.*;

public class TestApriori {
    public static void main(String[] args) {

        String pathPrefix = "/Users/slatertot21/Python Projects/cosc_254/Final Project/AprioriSampling/input/";
        Path transactionPath = Path.of(pathPrefix + args[0]);
        double minSupport = Double.parseDouble(args[1]);

        APriori apriori = new APriori(transactionPath);
        Map<Set<Integer>, Integer> frequentItemSets = apriori.run(minSupport);

        for (Map.Entry<Set<Integer>, Integer> entry : frequentItemSets.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }
    }
}
