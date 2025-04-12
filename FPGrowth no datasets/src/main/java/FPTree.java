import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

class FPTree {
    private final Node root;
    private final Map<String, HeaderEntry> headerTable;
    private final AtomicInteger nodeCount = new AtomicInteger(0);

    private static class HeaderEntry {
        Node head;
        Node tail;
        int totalSupport;
    }

    public FPTree() {
        this.root = new Node(null);
        this.headerTable = new HashMap<>();
    }

    public Node insertTransaction(List<String> transaction) {
        Node currentNode = root;

        for (String item : transaction) {
            Node child = currentNode.getChild(item);
            if (child != null) {
                child.incCount();
            } else {
                child = new Node(item);
                currentNode.addChild(child);
                nodeCount.incrementAndGet();

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

        if (entry == null) return conditionalPatterns;

        Node node = entry.head;
        while (node != null) {
            List<String> prefixPath = new ArrayList<>(16);
            Node parent = node.parent;
            while (parent != null && parent.item != null) {
                prefixPath.add(parent.item);
                parent = parent.parent;
            }

            if (!prefixPath.isEmpty()) {
                Collections.reverse(prefixPath);
                conditionalPatterns.merge(prefixPath, node.count, Integer::sum);
            }
            node = node.next;
        }
        return conditionalPatterns;
    }
}