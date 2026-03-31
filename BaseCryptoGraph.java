import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class BaseCryptoGraph {

    protected Node[] vertices;
    protected List<Edge> edges;
    protected int[][] weightMatrix;
    protected final int INF = Integer.MAX_VALUE / 2;

    public BaseCryptoGraph(Node[] finalBoard) {
        this.vertices = finalBoard;
        this.edges = new ArrayList<>();

        int size = vertices.length;
        this.weightMatrix = new int[size][size];

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                weightMatrix[i][j] = (i == j) ? 0 : INF;
            }
        }
    }

    // פה אני בונה בעצם את הגרף - לא משנה איזה גרף זה כל השלושה הולכים להשתמש בזה- ככה אני חוסך ים קוד
    protected void buildGraph(Map<Direction, List<Integer>>[] neighborsByDir) {
        for (int i = 0; i < neighborsByDir.length; i++) {
            Node sourceNode = vertices[i];

            if (neighborsByDir[i] != null) {
                for (List<Integer> neighborsList : neighborsByDir[i].values()) {
                    for (int neighborId : neighborsList) {

                        if (i < neighborId) {
                            Node destNode = vertices[neighborId];
                            int weight = calculateEdgeWeight(sourceNode, destNode);

                            Edge newEdge = new Edge(sourceNode, destNode, weight);
                            this.edges.add(newEdge);

                            weightMatrix[i][neighborId] = weight;
                            weightMatrix[neighborId][i] = weight;
                        }
                    }
                }
            }
        }
    }

    // פה כולם הולכים לדרוס את הפונקציה הזאת
    protected abstract int calculateEdgeWeight(Node u, Node v);


    public int[][] getWeightMatrix() { return weightMatrix; }
    public List<Edge> getEdges() { return edges; }
}