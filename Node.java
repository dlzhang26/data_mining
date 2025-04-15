import java.util.HashMap;
import java.util.Map;
import java.util.*;

class Node {
    final String item;
    int count; //support count for node (how many same items)
    Node parent;
    Node next;
    final Map<String, Node> children; //map to store all child nodes
    //creates a node of count 1 and sets children to a new hashmap with capacity 4 for efficiency)
    public Node(String item) {
        this.item = item;
        this.count = 1;
        this.children = new HashMap<>(4);
    }
    //getter method
    public Node getChild(String item) {
        return children.get(item);
    }
    //setter/adder method
    public void addChild(Node child) {
        children.put(child.item, child);
        child.parent = this;
    }
    //increases count of node
    public void incCount() {
        count++;
    }
}