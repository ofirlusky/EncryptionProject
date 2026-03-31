import java.util.*;

public class KruskalCryptoGraph extends BaseCryptoGraph {

    private KruskalEdge[] allFrictionEdges;
    private int edgeCount;
    private KruskalEdge[] mstEdges;
    private int mstCount;
    private int[][] mstAdjacency;

    public KruskalCryptoGraph(Node[] board) {

        super(board);
        buildFrictionEdges();

        if (edgeCount > 1) {
            Sorter.mergeSort(this.allFrictionEdges, 0, edgeCount - 1);
        }

        runKruskal();
        buildMstAdjacency();
        precalculateMstLoads();
        updateWeightMatrix();
    }

    private void precalculateMstLoads() {
        int n = vertices.length;
        for (int i = 0; i < mstCount; i++) {
            KruskalEdge e = mstEdges[i];
            if (e == null) continue;
            int x = countSubtreeSize(e.u, e.v, n);
            int y = n - x;
            e.load = x * y;
        }
    }

    private void buildFrictionEdges() {
        int maxEdges = vertices.length * vertices.length;
        allFrictionEdges = new KruskalEdge[maxEdges];
        edgeCount = 0;

        for (int i = 0; i < vertices.length; i++) {
            if (neighborsByDir[i] == null || vertices[i] == null) continue;

            for (List<Integer> neighbors : neighborsByDir[i].values()) {
                for (int neighborIndex : neighbors) {
                    if (i < neighborIndex) {
                        int friction = calculateFriction(vertices[i], vertices[neighborIndex]);
                        allFrictionEdges[edgeCount++] = new KruskalEdge(i, neighborIndex, friction);
                    }
                }
            }
        }
    }

    private void runKruskal() {
        int n = vertices.length;
        this.mstEdges = new KruskalEdge[n - 1];
        this.mstCount = 0;

        UnionFind uf = new UnionFind(n);

        for (int i = 0; i < edgeCount; i++) {
            KruskalEdge currentEdge = allFrictionEdges[i];

            if (currentEdge == null) continue;

            int rootU = uf.find(currentEdge.u);
            int rootV = uf.find(currentEdge.v);

            if (rootU != rootV) {
                mstEdges[mstCount++] = currentEdge;
                uf.union(rootU, rootV);
            }

            if (mstCount == n - 1) {
                break;
            }
        }
    }
    private void buildMstAdjacency() {
        int n = vertices.length;
        this.mstAdjacency = new int[n][n];

        for (int i = 0; i < mstCount; i++) {
            KruskalEdge e = mstEdges[i];

            if (e != null) {
                this.mstAdjacency[e.u][e.v] = 1;
                this.mstAdjacency[e.v][e.u] = 1;
            }
        }
    }

    private int countSubtreeSize(int u, int v, int n) {
        int size = 1;

        for (int neighbor = 0; neighbor < n; neighbor++) {
            if (this.mstAdjacency[u][neighbor] == 1 && neighbor != v) {
                size += countSubtreeSize(neighbor, u, n);
            }
        }

        return size;
    }

    @Override
    protected int calculateEdgeWeight(Node u, Node v) {
        int uId = u.getIDofNode();
        int vId = v.getIDofNode();

        if (uId == vId) {
            return 0;
        }

        KruskalEdge mstEdge = findEdgeInMst(uId, vId);
        if (mstEdge != null) {
            return mstEdge.load;
        }

        if (areNeighbors(uId, vId)) {
            int pathSum = getPathWeightInTree(uId, vId, -1, 0);

            if (pathSum != -1) {
                return pathSum + 1;
            }
        }

        return INF;
    }

    private int getPathWeightInTree(int current, int target, int parent, int currentSum) {
        if (current == target) {
            return currentSum;
        }

        int n = vertices.length;

        for (int neighbor = 0; neighbor < n; neighbor++) {
            if (this.mstAdjacency[current][neighbor] == 1 && neighbor != parent) {
                KruskalEdge e = findEdgeInMst(current, neighbor);

                if (e == null) {
                    continue;
                }

                int total = getPathWeightInTree(neighbor, target, current, currentSum + e.load);

                if (total != -1) {
                    return total;
                }
            }
        }

        return -1;
    }

    private KruskalEdge findEdgeInMst(int u, int v) {
        int small = (u < v) ? u : v;
        int big = (u < v) ? v : u;

        for (int i = 0; i < this.mstCount; i++) {
            KruskalEdge e = this.mstEdges[i];

            if (e != null && e.u == small && e.v == big) {
                return e;
            }
        }

        return null;
    }

    private boolean areNeighbors(int uId, int vId) {
        if (neighborsByDir[uId] == null) return false;
        for (List<Integer> neighbors : neighborsByDir[uId].values()) {
            if (neighbors.contains(vId)) return true;
        }
        return false;
    }

    private int calculateFriction(Node u, Node v) {
        int statU = StatuseOfTheNode(u);
        int statV = StatuseOfTheNode(v);
        if (statU == 0 && statV == 0) return 1;
        if (statU == statV && statU != 0) return 3;
        if (statU == 0 || statV == 0) return 5;
        return 10;
    }

    @Override
    protected void printSpecificDetails() {
        System.out.println("MST Edges and Loads (Kruskal):");
        for (int i = 0; i < mstCount; i++) {
            KruskalEdge e = mstEdges[i];
            if (e != null) {
                System.out.println("  - Edge " + e.u + " <-> " + e.v + " | Weight: " + e.load);
            }
        }
    }
}