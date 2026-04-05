import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class BaseCryptoGraph {

    protected Node[] vertices;
    protected int[][] weightMatrix;
    protected final int INF = Integer.MAX_VALUE / 2;

    protected static Map<Direction, List<Integer>>[] neighborsByDir;



    public BaseCryptoGraph(Node[] finalBoard) {
        this.vertices = finalBoard;
        neighborsByDir = PicariaGame.getNeighborsByDir();


        int size = vertices.length;
        this.weightMatrix = new int[size][size];

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                weightMatrix[i][j] = (i == j) ? 0 : INF;
            }
        }



    }



    protected void fillWeightMatrix() {
        int size = vertices.length;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (i == j) {
                    weightMatrix[i][j] = 0;
                }
                else if (areNeighbors(i, j)) {
                    weightMatrix[i][j] = calculateEdgeWeight(vertices[i], vertices[j]);
                }
                else {
                    weightMatrix[i][j] = INF;
                }
            }
        }
    }
    protected boolean areNeighbors(int uId, int vId) {
        if (neighborsByDir[uId] == null) return false;

        for (List<Integer> neighborsList : neighborsByDir[uId].values()) {
            if (neighborsList.contains(vId)) {
                return true;
            }
        }
        return false;
    }

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








    public void printMatrix() {
        System.out.println("\n--- Adjacency Matrix (Final Weights) ---");
        int n = vertices.length;

        System.out.print("ID\t");
        for (int i = 0; i < n; i++) System.out.print(i + "\t");
        System.out.println("\n" + "-".repeat(n * 8));

        for (int i = 0; i < n; i++) {
            System.out.print(i + " |\t");
            for (int j = 0; j < n; j++) {
                int val = weightMatrix[i][j];

                if (val >= INF) {
                    System.out.print("∞\t");
                } else if (i == j) {
                    System.out.print("0\t");
                } else {
                    System.out.print(val + "\t");
                }
            }
            System.out.println();
        }
    }



}