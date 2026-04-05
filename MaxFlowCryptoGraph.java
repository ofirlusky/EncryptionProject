import java.util.*;

public class MaxFlowCryptoGraph extends BaseCryptoGraph {

    public MaxFlowCryptoGraph(Node[] finalBoard) {
        super(finalBoard);
        fillWeightMatrix();
    }

    @Override
    protected int calculateEdgeWeight(Node u, Node v) {
        int source = u.getIDofNode();
        int sink = v.getIDofNode();
        int maxFlowValue = runEdmondsKarp(source, sink);
        return (maxFlowValue % 251) + 1;
    }

    private int runEdmondsKarp(int source, int sink) {
        int n = vertices.length;

        int[][] capacity = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (areNeighbors(i, j)) {
                    capacity[i][j] = getInitialCapacity(vertices[i], vertices[j]);
                }
            }
        }

        int maxFlow = 0;
        int[] parent = new int[n];

        while (bfs(capacity, source, sink, parent)) {

            int pathFlow = Integer.MAX_VALUE;
            for (int v = sink; v != source; v = parent[v]) {
                int u = parent[v];
                pathFlow = Math.min(pathFlow, capacity[u][v]);
            }

            for (int v = sink; v != source; v = parent[v]) {
                int u = parent[v];
                capacity[u][v] -= pathFlow;
                capacity[v][u] += pathFlow;
            }

            maxFlow += pathFlow;
        }

        return maxFlow;
    }


    private boolean bfs(int[][] residualGraph, int s, int t, int[] parent) {
        boolean[] visited = new boolean[vertices.length];
        Queue<Integer> queue = new LinkedList<>();

        queue.add(s);
        visited[s] = true;
        parent[s] = -1;

        while (!queue.isEmpty()) {
            int u = queue.poll();

            for (int v = 0; v < vertices.length; v++) {
                if (!visited[v] && residualGraph[u][v] > 0) {
                    if (v == t) {
                        parent[v] = u;
                        return true;
                    }
                    queue.add(v);
                    parent[v] = u;
                    visited[v] = true;
                }
            }
        }
        return false;
    }


    private int getInitialCapacity(Node u, Node v) {
        int su = StatuseOfTheNode(u);
        int sv = StatuseOfTheNode(v);
        int base = (su + 1) * (sv + 1) * 31;
        int idEffect = (u.getIDofNode() ^ v.getIDofNode()) * 7;

        return base + idEffect + 10;
    }

    @Override
    public  void printMatrix() {
        System.out.println("this is the pelet of Networks G");
        super.printMatrix();

    }
}