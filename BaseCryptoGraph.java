import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class BaseCryptoGraph {

    protected Node[] vertices;
    protected List<Edge> edges;
    protected int[][] weightMatrix;
    protected final int INF = Integer.MAX_VALUE / 2;

    protected static Map<Direction, List<Integer>>[] neighborsByDir;



    public BaseCryptoGraph(Node[] finalBoard) {
        this.vertices = finalBoard;
        this.edges = new ArrayList<>();
        neighborsByDir = PicariaGame.getNeighborsByDir();


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


    public int StatuseOfTheNode(Node node)
    {
        int su = 0;
        switch (node.getCurrentPieceValue()) {
            case OCCUPIED_P1:
                su = 1;
                break;
            case OCCUPIED_P2:
                su = 2;
                break;
        }
        return su;
    }

    public void updateWeightMatrix() {
        int size = vertices.length;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                this.weightMatrix[i][j] = calculateEdgeWeight(vertices[i], vertices[j]);
            }
        }
    }


    // פונקציה שהבנים יממשו כל אחד לפי הצרכים שלו
    protected abstract void printSpecificDetails();

    public void printGraphReport() {
        System.out.println("\n====================================================");
        System.out.println("REPORT FOR: " + this.getClass().getSimpleName());
        System.out.println("====================================================");

        // 1. הדפסת הנתונים הספציפיים של הבן (קרוסקל ידפיס MST, ביטים ידפיס חישובים)
        printSpecificDetails();

        // 2. הדפסת המטריצה הגנרית
        System.out.println("\nAdjacency Matrix:");
        int size = vertices.length;

        // כותרות
        System.out.print("ID\t");
        for (int i = 0; i < size; i++) System.out.print(i + "\t");
        System.out.println("\n" + "-".repeat(size * 8));

        for (int i = 0; i < size; i++) {
            System.out.print(i + " |\t");
            for (int j = 0; j < size; j++) {
                int val = weightMatrix[i][j];
                if (val >= INF) {
                    System.out.print("∞\t");
                } else {
                    System.out.print(val + "\t");
                }
            }
            System.out.println();
        }
        System.out.println("====================================================\n");
    }


    public void printMatrix() {
        System.out.println("\n--- Adjacency Matrix (Final Weights) ---");
        int n = vertices.length;

        // כותרת עמודות
        System.out.print("ID\t");
        for (int i = 0; i < n; i++) System.out.print(i + "\t");
        System.out.println("\n" + "-".repeat(n * 8));

        for (int i = 0; i < n; i++) {
            // כותרת שורה
            System.out.print(i + " |\t");
            for (int j = 0; j < n; j++) {
                int val = weightMatrix[i][j];

                if (val >= INF) {
                    System.out.print("∞\t"); // הדפסת אינסוף בצורה יפה
                } else if (i == j) {
                    System.out.print("0\t"); // אלכסון
                } else {
                    System.out.print(val + "\t");
                }
            }
            System.out.println();
        }
    }




    public int[][] getWeightMatrix() { return weightMatrix; }
    public List<Edge> getEdges() { return edges; }


}