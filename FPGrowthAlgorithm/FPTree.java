import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

class FPTree {
    private final Node root;
    private final Map<String, HeaderEntry> headerTable; //header table for O(1) operations
    private final AtomicInteger nodeCount = new AtomicInteger(0); //total nodes in tree
    //private int nodeCount = new Integer(0);
    //not too sure what an AtomicInteger is even after googling it

    //header table
    private static class HeaderEntry {
        Node head; //first node of an item
        Node tail; //last node of the same item
        int totalSupport; //# of times item appears in database
    }

    public FPTree() {
        this.root = new Node(null);
        this.headerTable = new HashMap<>();
    }


    public Node insertTransaction(List<String> transaction) {
        Node currentNode = root;

        //for each item in transaction
        for (String item : transaction) {
            //if child has the same item inc count
            Node child = currentNode.getChild(item);
            if (child != null) {
                child.incCount();
            } else {
                //new item create node
                child = new Node(item);
                currentNode.addChild(child);
                nodeCount.incrementAndGet();
                //nodeCount++;
                //atomic increment++ method (good for memory)

                //header table update O(1)
                HeaderEntry entry = headerTable.computeIfAbsent(item, k -> new HeaderEntry());
                if (entry.head == null) {
                    entry.head = child;
                } else {
                    entry.tail.next = child;
                }
                entry.tail = child;
                entry.totalSupport += child.count;
            }
            currentNode = child;
        }
        return currentNode;
    }

    public Map<List<String>, Integer> findConditionalPatterns(String item) {
        Map<List<String>, Integer> conditionalPatterns = new HashMap<>();
        HeaderEntry entry = headerTable.get(item);

        //if item isn't in table return empty
        if (entry == null) return conditionalPatterns;

        Node node = entry.head;
        //for each node in the linked list
        while (node != null) {
            //to store prefixes (capacity 16 for efficiency)
            List<String> prefixPath = new ArrayList<>(16);
            Node parent = node.parent;
            //tree traversal to find prefix path
            while (parent != null && parent.item != null) {
                prefixPath.add(parent.item);
                parent = parent.parent;
            }
            if (!prefixPath.isEmpty()) {
                //saves prefix path into all paterns
                Collections.reverse(prefixPath);
                conditionalPatterns.merge(prefixPath, node.count, Integer::sum);
            }
            node = node.next;
        }
        return conditionalPatterns;
    }
}