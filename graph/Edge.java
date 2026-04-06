package graph;

import utils.Node;

public class Edge {

    private Node sourceNode;
    private Node destinationNode;
    private int weightOfEdge;

    public Edge(Node sourceNode, Node destinationNode, int weightOfEdge) {
        this.sourceNode = sourceNode;
        this.destinationNode = destinationNode;
        this.weightOfEdge = weightOfEdge;
    }


    // אני בכוונה פה לא יוצר setters כי זה לא רלוונטי המשקל הוא קבוע ולא ישתנה בעתיד..

    public Node getSourceNode() {
        return sourceNode;
    }

    public Node getDestinationNode() {
        return destinationNode;
    }

    public int getWeightOfEdge() {
        return weightOfEdge;
    }


    @Override
    public String toString() {
        return "graph.Edge From utils.Node - " + this.sourceNode.getIDofNode() + "To utils.Node " + this.destinationNode.getIDofNode() +
                "Weigh - " + this.weightOfEdge;
    }
}
