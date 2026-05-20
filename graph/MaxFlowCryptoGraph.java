package graph;

import utils.Node;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

public class MaxFlowCryptoGraph extends BaseCryptoGraph {



    private int[] eTo;    // eTo[i]  = צומת יעד של קשת i
    private int[] eCap;   // eCap[i] = קיבולת נשארת של קשת i
    private int   eCount; // מספר הקשתות הכולל (כולל הפוכות)

    // adjList[u] = רשימת אינדקסים לתוך eTo/eCap
    // ממומשת כ-List<Integer> (Java standard — ArrayList)
    private List<Integer>[] adjList;

    // מערכי עזר שיצרתי
    private int[] dist;   // dist[u] = מרחק שכבה מהמקור (BFS)
    private int[] iter;   // iter[u] = אינדקס הקשת הבאה לבדיקה מ-u



    public MaxFlowCryptoGraph(Node[] finalBoard) {
        super(finalBoard);
        fillWeightMatrix();
        applyFloydWarshall();
    }



    @Override
    protected int calculateEdgeWeight(Node u, Node v) {
        int source = u.getIDofNode();
        int sink   = v.getIDofNode();
        int maxFlowValue = runDinic(source, sink);
        return (maxFlowValue % 251) + 1;
    }



    private int runDinic(int source, int sink) {
        int n = vertices.length;


        int maxEdges = n * n * 2;   // כולל קשתות הפוכות
        eTo   = new int[maxEdges];
        eCap  = new int[maxEdges];
        eCount = 0;

        adjList = new List[n];
        for (int i = 0; i < n; i++) adjList[i] = new ArrayList<>();

        dist = new int[n];
        iter = new int[n];

        // בניית גרף
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i != j && areNeighbors(i, j)) {
                    int cap = getInitialCapacity(vertices[i], vertices[j]);
                    addEdgePair(i, j, cap);
                }
            }
        }

        //  לולאת Dinic
        int maxFlow = 0;

        while (bfsLayered(source, sink, n)) {
            // איפוס iter לתחילת רשימת הקשתות של כל צומת
            for (int i = 0; i < n; i++) iter[i] = 0;

            int flow;
            // שלח blocking flow בכל מעבר DFS עד שאין יותר
            while ((flow = dfsBlocking(source, sink, Integer.MAX_VALUE)) > 0) {
                maxFlow += flow;
            }
        }

        return maxFlow;
    }


    private void addEdgePair(int from, int to, int capacity) {
        // קשת קדימה
        adjList[from].add(eCount);
        eTo[eCount]  = to;
        eCap[eCount] = capacity;
        eCount++;

        // קשת הפוכה (residual)
        adjList[to].add(eCount);
        eTo[eCount]  = from;
        eCap[eCount] = 0;
        eCount++;
    }


    private boolean bfsLayered(int source, int sink, int n) {
        Arrays.fill(dist, -1);
        dist[source] = 0;

        Deque<Integer> queue = new ArrayDeque<>();
        queue.add(source);

        while (!queue.isEmpty()) {
            int u = queue.poll();
            for (int idx : adjList[u]) {
                int v = eTo[idx];
                // מתקדמים רק בקשתות עם קיבולת נשארת לצמתים שלא ביקרנו
                if (eCap[idx] > 0 && dist[v] < 0) {
                    dist[v] = dist[u] + 1;
                    queue.add(v);
                }
            }
        }

        return dist[sink] >= 0;
    }



    private int dfsBlocking(int u, int sink, int pushed) {
        if (u == sink) return pushed;

        List<Integer> edges = adjList[u];
        int edgeCount = edges.size();


        while (iter[u] < edgeCount) {
            int idx = edges.get(iter[u]);
            int v   = eTo[idx];


            if (eCap[idx] > 0 && dist[v] == dist[u] + 1) {
                int flow = dfsBlocking(v, sink, Math.min(pushed, eCap[idx]));
                if (flow > 0) {
                    eCap[idx]      -= flow;   // הפחת קיבולת בקשת קדימה
                    eCap[idx ^ 1]  += flow;   // הוסף קיבולת בקשת הפוכה
                    return flow;
                }
            }

            iter[u]++;
        }
        return 0;
    }



    private int getInitialCapacity(Node u, Node v) {
        int su   = StatuseOfTheNode(u);
        int sv   = StatuseOfTheNode(v);
        int base = (su + 1) * (sv + 1) * 31;
        int idEffect = (u.getIDofNode() ^ v.getIDofNode()) * 7;
        return base + idEffect + 10;
    }



    @Override
    public void printMatrix() {
        System.out.println("this is the pelet of Networks G");
        super.printMatrix();
    }
}