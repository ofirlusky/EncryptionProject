import com.sun.source.tree.ContinueTree;
import java.util.List;
import java.util.Map;
public class BitwiseCryptoGraph extends BaseCryptoGraph {


    //  תכונה ייחודית למחלקה הזאת ברור שזה בנוסף לתכונות שהוא מקבל מאבא שלו

    private int boardMask;

    public BitwiseCryptoGraph(Node[] finalBoard) {
        super(finalBoard);
        this.boardMask = generateBoardMask();
        fillWeightMatrix();
        FinalWightOfG();

    }

    private int generateBoardMask() {
        int mask = 0;
        for (int i = 0; i < vertices.length; i++) {

            int pieceValue = StatuseOfTheNode(vertices[i]);
            mask = mask | (pieceValue << (i * 2));
        }
        return mask;
    }


    @Override
    protected int calculateEdgeWeight(Node u, Node v) {
        int uId = u.getIDofNode();
        int vId = v.getIDofNode();
        int su = StatuseOfTheNode(u);
        int base = (uId * 17) ^ (vId * 31);
        int shifted = base << (su + 1);
        int combined = shifted ^ boardMask;
        int weight = Math.abs(combined) % 251 + 1;
        return weight;
    }

    @Override
    public  void printMatrix() {
        System.out.println("this is the pelet of Bitwise G");
        super.printMatrix();

    }



    public void FinalWightOfG() {

        int indexOfSourceNode = FindLargestNode();
        int[] DijkstraOutPut = runDijkstra(indexOfSourceNode);

        int size = this.vertices.length;
        for (int i = 0; i < size; i++) {
            for (int j = i + 1; j < size; j++) {
                if (this.weightMatrix[i][j] < INF) {
                    int originalWeight = this.weightMatrix[i][j];
                    int newWeight = originalWeight + DijkstraOutPut[i] + DijkstraOutPut[j];
                    int finalCryptoWeight = (newWeight % 251) + 1;
                    this.weightMatrix[i][j] = finalCryptoWeight;
                    this.weightMatrix[j][i] = finalCryptoWeight;
                }
            }
        }
    }

    public int FindLargestNode() {
        int indexOfMaxNode = 0;
        int maximumSum = -1;
        int size = this.vertices.length;

        for (int i = 0; i < size; i++) {
            int currentSum = 0;
            for (int j = 0; j < size; j++) {
                if (this.weightMatrix[i][j] < INF && i != j) {
                    currentSum += this.weightMatrix[i][j];
                }
            }
            if (currentSum > maximumSum) {
                maximumSum = currentSum;
                indexOfMaxNode = i;
            }
        }
        return indexOfMaxNode;
    }











    private int[] runDijkstra(int source) {
        int n = vertices.length;
        int[] dist = new int[n];
        boolean[] visited = new boolean[n];

        for (int i = 0; i < n; i++) {
            dist[i] = INF;
            visited[i] = false;
        }
        dist[source] = 0;
        for (int count = 0; count < n - 1; count++) {
            int u = -1;
            int minVal = INF;
            for (int i = 0; i < n; i++) {
                if (!visited[i] && dist[i] <= minVal) {
                    minVal = dist[i];
                    u = i;
                }
            }

            if (u == -1) break;
            visited[u] = true;
            for (int v = 0; v < n; v++) {
                if (!visited[v] && weightMatrix[u][v] < INF && dist[u] != INF
                        && dist[u] + weightMatrix[u][v] < dist[v]) {

                    dist[v] = dist[u] + weightMatrix[u][v];
                }
            }
        }
        return dist;
    }













}
