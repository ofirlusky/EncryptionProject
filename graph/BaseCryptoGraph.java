package graph;

import game.Direction;
import game.PicariaGame;
import game.PieceValue;
import utils.MinHeap;
import utils.Node;

import java.util.List;
import java.util.Map;

public abstract class BaseCryptoGraph {

    protected Node[] vertices;
    protected int[][] weightMatrix;
    protected final int INF = Integer.MAX_VALUE / 2;

    protected static Map<Direction, List<Integer>>[] neighborsByDir;

    protected int[][] keyMatrix;


    public BaseCryptoGraph(Node[] finalBoard) {
        this.vertices  = finalBoard;
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
                } else if (areNeighbors(i, j)) {
                    weightMatrix[i][j] = calculateEdgeWeight(vertices[i], vertices[j]);
                } else {
                    weightMatrix[i][j] = INF;
                }
            }
        }
    }

    protected boolean areNeighbors(int uId, int vId) {
        if (neighborsByDir[uId] == null) return false;
        for (List<Integer> neighborsList : neighborsByDir[uId].values()) {
            if (neighborsList.contains(vId)) return true;
        }
        return false;
    }

    protected abstract int calculateEdgeWeight(Node u, Node v);

    public int StatuseOfTheNode(Node node) {
        int su = 0;
        switch (node.getCurrentPieceValue()) {
            case PieceValue.OCCUPIED_P1: su = 1; break;
            case PieceValue.OCCUPIED_P2: su = 2; break;
        }
        return su;
    }

    // בכוונה אני משאיר פה את הקוד של פלוייד ווארשל , על מנת להויכח את זה שייעלתי אותו


    /*
    protected void applyFloydWarshall() {
        int n = vertices.length;
        keyMatrix = new int[n][n];
        int INF = 999999;

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    keyMatrix[i][j] = 0;
                } else if (weightMatrix[i][j] == 0) {
                    keyMatrix[i][j] = INF;
                } else {
                    keyMatrix[i][j] = weightMatrix[i][j];
                }
            }
        }

        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (keyMatrix[i][k] != INF && keyMatrix[k][j] != INF &&
                            keyMatrix[i][k] + keyMatrix[k][j] < keyMatrix[i][j]) {
                        keyMatrix[i][j] = keyMatrix[i][k] + keyMatrix[k][j];
                    }
                }
            }
        }
    }
    */



    protected void applyFloydWarshall() {
        int n   = vertices.length;
        int INF = 999999;

        int[][] edgeW = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    edgeW[i][j] = 0;
                } else if (weightMatrix[i][j] == 0 || weightMatrix[i][j] >= this.INF) {
                    edgeW[i][j] = INF;
                } else {
                    edgeW[i][j] = weightMatrix[i][j];
                }
            }
        }


        keyMatrix = new int[n][n];
        for (int src = 0; src < n; src++) {
            keyMatrix[src] = runDijkstra(src, n, edgeW, INF);
        }
    }


    private int[] runDijkstra(int src, int n, int[][] edgeW, int INF) {

        // אתחול מרחקים
        int[] dist = new int[n];
        for (int i = 0; i < n; i++) dist[i] = INF;
        dist[src] = 0;

        // מכניסים את כל הצמתים ל-MinHeap
        // MinHeap ממומש ידנית ב-utils.MinHeap (תומך ב-decreaseKey ב-O(log n))
        MinHeap heap = new MinHeap(n);
        for (int i = 0; i < n; i++) {
            heap.insert(dist[i], i);
        }

        while (!heap.isEmpty()) {
            // הוצא את הצומת עם המרחק הקטן ביותר — O(log n)
            int u = heap.extractMin();

            // אם המרחק הוא INF — כל הצמתים הנותרים בלתי נגישים
            if (dist[u] == INF) break;

            // סריקת שכנים של u וריענון מרחקים
            for (int v = 0; v < n; v++) {
                if (v == u || edgeW[u][v] >= INF) continue;

                int newDist = dist[u] + edgeW[u][v];

                if (newDist < dist[v]) {
                    dist[v] = newDist;
                    // decreaseKey — O(log n) הודות ל-posInHeap ב-MinHeap
                    if (heap.contains(v)) {
                        heap.decreaseKey(v, newDist);
                    }
                }
            }
        }

        return dist;
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
                if (val >= INF)  System.out.print("∞\t");
                else if (i == j) System.out.print("0\t");
                else             System.out.print(val + "\t");
            }
            System.out.println();
        }
    }

    public void printKeyMatrix() {
        System.out.println("floyidd warshel");
        for (int i = 0; i < keyMatrix.length; i++) {
            for (int j = 0; j < keyMatrix.length; j++) {
                if (keyMatrix[i][j] >= 999999) System.out.print("INF\t");
                else                           System.out.print(keyMatrix[i][j] + "\t");
            }
            System.out.println();
        }
    }

    public int[][] getKeyMatrix() {
        return this.keyMatrix;
    }
}