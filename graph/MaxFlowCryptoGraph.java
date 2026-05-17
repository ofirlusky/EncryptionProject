package graph;

import utils.Node;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

/**
 * MaxFlowCryptoGraph — מחשב משקלי קשתות לפי Max-Flow,
 * ואז מריץ Floyd-Warshall (דרך applyFloydWarshall ב-BaseCryptoGraph).
 *
 * ── שיפור: Edmonds-Karp → Dinic's Algorithm ──────────────────────────
 *
 * | אלגוריתם       | סיבוכיות         | על גרף זה (n=13, E≈30) |
 * |----------------|------------------|------------------------|
 * | Edmonds-Karp   | O(V × E²)        | ~351,000 פעולות        |
 * | Dinic's        | O(V² × E)        | ~6,240   פעולות        |
 *
 * שני ייעולים משולבים:
 *
 * 1. Layered Graph (BFS פעם אחת לשלב):
 *    במקום לחפש כל פעם מסלול מ-s ל-t מחדש (Edmonds-Karp),
 *    Dinic בונה "שכבות" (dist[]) שמחלקות את הגרף לרמות לפי
 *    מרחק מהמקור. DFS ינוע רק קדימה בשכבות — אפשרויות "חזרה"
 *    נחסכות לחלוטין.
 *
 * 2. Adjacency List + Iterator (Dead-end Pruning):
 *    iter[u] = מצביע לקשת הבאה לבדיקה מ-u.
 *    כשקשת נוצלה — iter מתקדם ולא חוזרים אליה.
 *    חוסך סריקת v=0..n-1 בכל צומת (כמו בגרסה הקודמת).
 *
 * ── מבנה Edge ──────────────────────────────────────────────────────
 * כל קשת מיוצגת כזוג:
 *   edges[i]   = קשת רגילה (קיבולת חיובית)
 *   edges[i^1] = קשת הפוכה (residual, קיבולת התחלתית = 0)
 * XOR עם 1 מחזיר את הקשת ההפוכה — O(1) ללא HashMap.
 */
public class MaxFlowCryptoGraph extends BaseCryptoGraph {

    // ── מבנה נתונים: קשת ─────────────────────────────────────────────────
    // ממומש כמערך מקביל (לא class נפרד) — cache-friendly יותר
    private int[] eTo;    // eTo[i]  = צומת יעד של קשת i
    private int[] eCap;   // eCap[i] = קיבולת נשארת של קשת i
    private int   eCount; // מספר הקשתות הכולל (כולל הפוכות)

    // adjList[u] = רשימת אינדקסים לתוך eTo/eCap
    // ממומשת כ-List<Integer> (Java standard — ArrayList)
    private List<Integer>[] adjList;

    // ── מערכי עזר לדיניק ─────────────────────────────────────────────────
    private int[] dist;   // dist[u] = מרחק שכבה מהמקור (BFS)
    private int[] iter;   // iter[u] = אינדקס הקשת הבאה לבדיקה מ-u


    // =========================================================
    // Constructor
    // =========================================================

    public MaxFlowCryptoGraph(Node[] finalBoard) {
        super(finalBoard);
        fillWeightMatrix();
        applyFloydWarshall();
    }

    // =========================================================
    // calculateEdgeWeight — ממשק ל-BaseCryptoGraph
    // =========================================================

    @Override
    protected int calculateEdgeWeight(Node u, Node v) {
        int source = u.getIDofNode();
        int sink   = v.getIDofNode();
        int maxFlowValue = runDinic(source, sink);
        return (maxFlowValue % 251) + 1;
    }

    // =========================================================
    // Dinic's Algorithm
    // =========================================================

    /**
     * מריץ Dinic's algorithm בין source ל-sink.
     * מחזיר את ערך ה-max flow.
     *
     * שלבים:
     *   1. בנה גרף residual עם קשתות זוגיות (edge + reverse)
     *   2. חזור: BFS → בנה Layered Graph
     *             DFS → שלח blocking flow
     *   3. עצור כשאין מסלול מ-source ל-sink
     */
    @SuppressWarnings("unchecked")
    private int runDinic(int source, int sink) {
        int n = vertices.length;

        // ── אתחול מבנה הנתונים ──────────────────────────────────────────
        int maxEdges = n * n * 2;   // כולל קשתות הפוכות
        eTo   = new int[maxEdges];
        eCap  = new int[maxEdges];
        eCount = 0;

        adjList = new List[n];
        for (int i = 0; i < n; i++) adjList[i] = new ArrayList<>();

        dist = new int[n];
        iter = new int[n];

        // ── בניית גרף residual ──────────────────────────────────────────
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i != j && areNeighbors(i, j)) {
                    int cap = getInitialCapacity(vertices[i], vertices[j]);
                    addEdgePair(i, j, cap);
                }
            }
        }

        // ── לולאת Dinic ─────────────────────────────────────────────────
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

    // =========================================================
    // addEdgePair — מוסיף קשת + קשת הפוכה
    //
    // קשת רגילה:  edges[eCount]     cap = capacity
    // קשת הפוכה:  edges[eCount + 1] cap = 0
    // XOR(1) מחבר ביניהן — O(1)
    // =========================================================

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

    // =========================================================
    // BFS — בניית Layered Graph
    //
    // ממלא dist[]: dist[u] = מרחק שכבה מ-source.
    // רק קשתות שמתקדמות שכבה (dist[v] == dist[u]+1)
    // יהיו חוקיות ב-DFS.
    //
    // מחזיר true אם sink נגיש מ-source.
    // =========================================================

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

    // =========================================================
    // DFS — Blocking Flow
    //
    // iter[u] = אינדקס הקשת הבאה לבדיקה מ-u (Dead-end Pruning).
    // כשקשת נוצלה — iter[u]++ ולא חוזרים אליה.
    // מחזיר כמות flow ששלחנו, או 0 אם אין מסלול.
    // =========================================================

    private int dfsBlocking(int u, int sink, int pushed) {
        if (u == sink) return pushed;

        List<Integer> edges = adjList[u];
        int edgeCount = edges.size();

        // iter[u] מתחיל ממקום שהפסקנו — לא מ-0 מחדש
        while (iter[u] < edgeCount) {
            int idx = edges.get(iter[u]);
            int v   = eTo[idx];

            // בדיקת תנאי Layered Graph: v חייב להיות שכבה אחת קדימה
            if (eCap[idx] > 0 && dist[v] == dist[u] + 1) {
                int flow = dfsBlocking(v, sink, Math.min(pushed, eCap[idx]));
                if (flow > 0) {
                    eCap[idx]      -= flow;   // הפחת קיבולת בקשת קדימה
                    eCap[idx ^ 1]  += flow;   // הוסף קיבולת בקשת הפוכה
                    return flow;
                }
            }
            // קשת זו לא מביאה לתוצאה — עבור לבאה (Dead-end Pruning)
            iter[u]++;
        }
        return 0;
    }

    // =========================================================
    // getInitialCapacity — ללא שינוי מהגרסה המקורית
    // =========================================================

    private int getInitialCapacity(Node u, Node v) {
        int su   = StatuseOfTheNode(u);
        int sv   = StatuseOfTheNode(v);
        int base = (su + 1) * (sv + 1) * 31;
        int idEffect = (u.getIDofNode() ^ v.getIDofNode()) * 7;
        return base + idEffect + 10;
    }

    // =========================================================
    // printMatrix — ללא שינוי
    // =========================================================

    @Override
    public void printMatrix() {
        System.out.println("this is the pelet of Networks G");
        super.printMatrix();
    }
}