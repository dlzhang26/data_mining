import java.util.*;

/**
 * The node class is where each itemset is stored. it has pointers to its parent and list of children and the support itself.
 */
public class Node {
    SortedSet<Integer> itemset;
    SortedSet<Node> children;
    Node parent;
    Integer support;

    /**
     * The node constructor initalizes an itemset with its support. Any children of the node is added to a sorted set and
     * ordered based on the lexicographic order of the itemsets
     * @param inItemset the itemset to create a node
     * @param inSupport the itemsets corresponding support
     */

    public Node(SortedSet<Integer> inItemset, Integer inSupport) {
        this.itemset = inItemset;
        this.support = inSupport;
        this.children = new TreeSet<>(new Comparator<Node>() {
            @Override
            public int compare(Node n1, Node n2) {
                ArrayList<Integer> a1 = new ArrayList<>(n1.itemset);
                ArrayList<Integer> a2 = new ArrayList<>(n2.itemset);
                for (int i = 0; i < a1.size(); i++) {
                    int difference = a1.get(i) - a2.get(i);
                    if (difference != 0) {
                        return difference;
                    }
                }
                return 0;
            }
        });
    }

    public SortedSet<Node> getChildren() {
        return children;
    }
    public void addChild(Node child) {
        child.parent = this;
        children.add(child);
    }
}
