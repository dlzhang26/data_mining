import java.util.*;

public class DiffEclatTree {
    private Node root;
    private Integer levelSize;
    private int size;

    public DiffEclatTree() {
        this.root = new Node(new TreeSet<>(), 0);
        this.levelSize = 0;
    }

    public void insertItemset(SortedSet<Integer> itemset, Integer support) {
        Node currentNode = root;
        Node newNode = new Node(itemset, support);
        List<Integer> sortedItemset = new ArrayList<>(itemset);

        for (int i = 0; i < levelSize; i++) {
            int prefix = sortedItemset.get(i);
            for (Node node : currentNode.children) {
                List<Integer> checkItemset = new ArrayList<>(node.itemset);
                try {
                    if (prefix == checkItemset.get(i)) {
                        currentNode = node;
                        break;
                    }
                } catch (IndexOutOfBoundsException e) {
                    System.err.println("Out of bounds for itemset: " + node.itemset + ", index = " + i + ", prefix = " + prefix);
                    System.out.println("Prefix = " + prefix);
                }
            }
            if (itemset.size() - 1 == currentNode.itemset.size()) {
                break;
            }
        }
        newNode.parent = currentNode;
        currentNode.children.add(newNode);
        size++;
        if (newNode.itemset.size() > levelSize) {
            levelSize++;
        }
    }

    public void printTree() {
        printNode(root, 0);
    }

    private void printNode(Node node, int depth) {
        if (node.itemset.size() > 0) {
            System.out.println("  ".repeat(depth) + node.itemset + " (Support: " + node.support + ")");
        }
        for (Node child : node.children) {
            printNode(child, depth + 1);
        }
    }

    public int size() {
        return size;
    }
    public int depth() {
        return levelSize;
    }

}
