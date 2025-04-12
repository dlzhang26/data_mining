import java.util.HashMap;
import java.util.Map;

class Node {
    final String item;
    int count;
    Node parent;
    Node next;
    final Map<String, Node> children;

    public Node(String item) {
        this.item = item;
        this.count = 1;
        this.children = new HashMap<>(4);
    }

    public Node getChild(String item) {
        return children.get(item);
    }

    public void addChild(Node child) {
        children.put(child.item, child);
        child.parent = this;
    }

    public void incCount() {
        count++;
    }

    public void incCount(int delta) {
        count += delta;
    }
}