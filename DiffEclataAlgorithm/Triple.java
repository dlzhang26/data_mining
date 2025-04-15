import java.util.*;

public class Triple {
    private SortedSet<Integer> itemset;
    private BitSet diffset;
    private Integer support;

    public Triple(SortedSet<Integer> inItemset, BitSet inDiffset, Integer inSupport) {
        this.itemset = inItemset;
        this.diffset = inDiffset;
        this.support = inSupport;
    }
    public SortedSet<Integer> getItemset() {
        return this.itemset;
    }
    public BitSet getDiffset() {
        return this.diffset;
    }
    public Integer getSupport() {
        return support;
    }
}
