package graph;

import game.Move;
import utils.Node;
import utils.Sorter;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class EulerianCryptoGraph {



    // adjList[0] -> [1, 2]
    // המיקום מייצג צומת שמצביע על השכנים שלו - גרף מכוון
    private List<Integer>[] adjList;

    // מייצג כמה צמתים יש בגרף
    private int numNodes;


    // הפלט הסופי מהווה חלק ממפתח ההצפנה !
    private List<Integer> keyStream;



    public EulerianCryptoGraph(int totalNodes, List<Move> gameHistory) {
        this.numNodes = totalNodes;

        adjList = new ArrayList[numNodes];
        for (int i = 0; i < numNodes; i++) {
            adjList[i] = new ArrayList<>();
        }

        buildBaseRing();
        overlayHistory(gameHistory);
        balanceGraph();
        int startNode = 0;

        if (gameHistory != null && !gameHistory.isEmpty()) {
            // נתחיל את הריצה על איפה שהתחיל המשחק
            startNode = gameHistory.get(0).getFromNode();
        }
        this.keyStream = runHierholzer(startNode);
    }



    // בונה גרף מעגלי מכוון
    // הפונקציה הכרחית כי היא נותנת - הבטחת קשירות חזקה (Strong Connectivity)
    // אם שחקנים שיחקו רק על כמה צמתים בודדים אז מקבלים רק אותם - אני רציתי שכולם יהיה קשורים
    private void buildBaseRing() {
        for (int i = 0; i < numNodes; i++) {
            // בזכות מודולו זה יוצר מעגל בסוף
            int nextNode = (i + 1) % numNodes;
            adjList[i].add(nextNode);
        }
    }

    // מעדכנת בad את היסטוריית המהלכים - כל מהחל שבוצע זה מוסיף לצומת שממנו יצאה המהלך את השכן שהוא היעד של המהלך
    private void overlayHistory(List<Move> history) {
        for (Move move : history) {
            adjList[move.getFromNode()].add(move.getToNode());
        }
    }

    public void printGraph() {
        System.out.println("\n=== הגרף האוילרי (לפני איזון) ===");
        for (int i = 0; i < numNodes; i++) {
            System.out.println("צומת " + i + " -> " + adjList[i]);
        }
    }




    // כדי שאלגוריתם  Hierholzer יצליח למצוא מעגל אוילרי, חייב להתקיים חוק האיזון
    //   בגרף מכוון: לכל צומת בנפרד, מספר החצים שנכנסים אליו חייב להיות שווה בדיוק למספר החצים שיוצאים ממנו.
    // פונקציה זו נועדה לאזן את הגרף אם צריך ..
    public void balanceGraph() {
        int[] inDegree  = new int[numNodes];
        int[] outDegree = new int[numNodes];



        // מילוי המערכים , סופר כמה קשתות נכנסות יש וכמה יוצאות עבור כל קשת
        for (int i = 0; i < numNodes; i++) {
            outDegree[i] = adjList[i].size();
            for (int target : adjList[i]) {
                inDegree[target]++;
            }
        }


        // כמובן הם כל הזמן משתנות הדרישה לכניסה ויציאה בהתאם למהלכי המשחק
        // אותו צומת יכולה להופיע כמה פעמים
        // צמתים שיש להם יותר מדי כניסות, ולכן הם חייבים שייצאו מהם עוד חצים כדי להתאזן.
        List<Integer> needsOut = new ArrayList<>();
        // : צמתים שיש להם יותר מדי יציאות, ולכן הם חייבים שייכנסו אליהם עוד חצים.
        List<Integer> needsIn  = new ArrayList<>();

        for (int i = 0; i < numNodes; i++) {
            int balance = inDegree[i] - outDegree[i];
            if (balance > 0) {
                for (int j = 0; j < balance; j++) needsOut.add(i);
            } else if (balance < 0) {
                for (int j = 0; j < Math.abs(balance); j++) needsIn.add(i);
            }
        }


        // הכניסות החסרות שווה תמיד לסך היציאות החסרות
        for (int i = 0; i < needsOut.size(); i++) {
            adjList[needsOut.get(i)].add(needsIn.get(i));
        }
    }

    public List<Integer> runHierholzer(int startNode) {


        // ממיין בשביל דטרמיניזם
        for (int i = 0; i < numNodes; i++) {
            if (adjList[i].size() > 1) {
                Sorter.mergeSortList(adjList[i], 0, adjList[i].size() - 1);
            }
        }

        // עותק זמני כי אני לא רוצה להרוס את המקור
        List<Integer>[] tempAdj = new ArrayList[numNodes];
        for (int i = 0; i < numNodes; i++) {
            tempAdj[i] = new ArrayList<>(adjList[i]);
        }

        Stack<Integer> currPath = new Stack<>();
        List<Integer>  circuit  = new ArrayList<>();
        currPath.push(startNode);

        while (!currPath.isEmpty()) {
            int u = currPath.peek();

            if (!tempAdj[u].isEmpty()) {
                int nextV = tempAdj[u].remove(0);
                currPath.push(nextV);
            } else {
                circuit.add(currPath.pop());
            }
        }


        // צומת נכנס לרשימת הסיכום (circuit) אך ורק כשהוא נתקע במבוי סתום (כשאין ממנו יותר קשתות פנויות ב-tempAdj).
        return manualReverse(circuit);
    }

    public List<Integer> getKeyStream() {
        return this.keyStream;
    }

    public void printKeyStream() {
        System.out.println("\n***************************************************");
        System.out.println("   GENERATED EULERIAN KEYSTREAM (Encryption Ready)   ");
        System.out.println("***************************************************");
        System.out.println("Start Node: " + (keyStream != null && !keyStream.isEmpty() ? keyStream.get(0) : "N/A"));
        System.out.println("Length: "     + (keyStream != null ? keyStream.size() : 0) + " values");
        System.out.println("Key: "        + keyStream);
        System.out.println("***************************************************\n");
    }

    private List<Integer> manualReverse(List<Integer> list) {
        List<Integer> reversed = new ArrayList<>();
        for (int i = list.size() - 1; i >= 0; i--) {
            reversed.add(list.get(i));
        }
        return reversed;
    }
}