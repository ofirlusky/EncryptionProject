package game;
public class Move {
    private final int fromNode;
    private final int toNode;

    public Move(int fromNode, int toNode) {
        this.fromNode = fromNode;
        this.toNode = toNode;
    }

    public int getFromNode() {
        return fromNode;
    }

    public int getToNode() {
        return toNode;
    }

    @Override
    public String toString() {
        return fromNode + "->" + toNode;
    }
}