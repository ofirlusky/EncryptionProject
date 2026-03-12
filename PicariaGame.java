import java.awt.Point;

public class PicariaGame {

    private final int totalNodes = 13;
    private int[] board;

    // --- הוספנו את המשתנים האלה לניהול התור ---
    private int currentPlayer;
    private int piecesPlaced;

    private final int[][] neighbors = {
            {1, 3, 5},                // 0
            {0, 2, 3, 4, 6},          // 1
            {1, 4, 7},                // 2
            {0, 1, 5, 6},             // 3
            {1, 2, 6, 7},             // 4
            {0, 3, 6, 8, 10},         // 5
            {1, 3, 4, 5, 7, 8, 9, 11},// 6
            {2, 4, 6, 9, 12},         // 7
            {5, 6, 10, 11},           // 8
            {6, 7, 11, 12},           // 9
            {5, 8, 11},               // 10
            {6, 8, 9, 10, 12},        // 11
            {7, 9, 11}                // 12
    };

    private final Point[] nodeCoords = {
            new Point(0, 0), new Point(2, 0), new Point(4, 0),
            new Point(1, 1), new Point(3, 1),
            new Point(0, 2), new Point(2, 2), new Point(4, 2),
            new Point(1, 3), new Point(3, 3),
            new Point(0, 4), new Point(2, 4), new Point(4, 4)
    };

    public PicariaGame() {
        this.board = new int[totalNodes];
        this.currentPlayer = 1; // שחקן 1 מתחיל
        this.piecesPlaced = 0;  // מתחילים עם 0 חיילים על הלוח
    }

    // Getters
    public int getTotalNodes() { return totalNodes; }
    public int[] getBoard() { return board; }
    public int[] getNeighborsOf(int nodeIndex) { return neighbors[nodeIndex]; }
    public Point getNodeCoord(int nodeIndex) { return nodeCoords[nodeIndex]; }

    // --- הנה הפונקציות שהיו חסרות לך! ---
    public int getCurrentPlayer() { return currentPlayer; }
    public int getPiecesPlaced() { return piecesPlaced; }

    // הלוגיקה שמניחה חייל ומחליפה תור
    public boolean placePiece(int nodeIndex) {
        if (piecesPlaced < 6 && board[nodeIndex] == 0) {

            // אסור לשים חייל במרכז בתור הראשון
            if (piecesPlaced == 0 && nodeIndex == 6) {
                System.out.println("Cannot place the first piece in the center!");
                return false;
            }

            // מניחים חייל
            board[nodeIndex] = currentPlayer;
            piecesPlaced++;

            // מעבירים תור
            currentPlayer = (currentPlayer == 1) ? 2 : 1;
            return true;
        }
        return false;
    }




    // פונקציית עזר קטנה: בודקת במערך השכנים אם יש קו ישיר בין שני צמתים
    private boolean isNeighbor(int from, int to) {
        for (int neighbor : neighbors[from]) {
            if (neighbor == to) {
                return true; // מצאנו חיבור!
            }
        }
        return false; // עברנו על כל השכנים ואין חיבור
    }

    // הפונקציה המרכזית שמבצעת את התזוזה
    public boolean movePiece(int fromNode, int toNode) {
        // 1. האם החייל שייך לשחקן הנוכחי?
        if (board[fromNode] != currentPlayer) {
            System.out.println("This is not your piece!");
            return false;
        }

        // 2. האם יעד התזוזה ריק?
        if (board[toNode] != 0) {
            System.out.println("Target node is not empty!");
            return false;
        }

        // 3. האם יש קו בין הצמתים?
        if (!isNeighbor(fromNode, toNode)) {
            System.out.println("Invalid move! Nodes are not connected.");
            return false;
        }

        // --- אם הגענו לפה, המהלך חוקי ב-100%! ---

        // מבצעים את התזוזה: מוחקים מהמיקום הישן, וכותבים במיקום החדש
        board[fromNode] = 0;
        board[toNode] = currentPlayer;

        // מעבירים את התור לשחקן הבא
        currentPlayer = (currentPlayer == 1) ? 2 : 1;

        return true; // מדווחים ל-GUI שהפעולה בוצעה בהצלחה
    }


    // כל האפשרויות ל-3 בשורה על הלוח שלנו (לפי מספרי הצמתים)
    private final int[][] winningLines = {
            // שורות אופקיות
            {0, 1, 2}, {5, 6, 7}, {10, 11, 12},
            // עמודות אנכיות
            {0, 5, 10}, {1, 6, 11}, {2, 7, 12},
            // אלכסונים ראשיים (ארוכים)
            {0, 3, 6}, {3, 6, 9}, {6, 9, 12},
            {2, 4, 6}, {4, 6, 8}, {6, 8, 10},
            // אלכסונים משניים (קצרים)
            {1, 3, 5}, {1, 4, 7}, {5, 8, 11}, {7, 9, 11}
    };

    // פונקציה שבודקת אם יש מנצח ומחזירה את המספר שלו (1 או 2). אם אין, מחזירה 0.
    public int checkWinner() {
        for (int[] line : winningLines) {
            int a = line[0];
            int b = line[1];
            int c = line[2];

            // אם בצומת הראשון יש שחקן (לא אפס), והוא שווה לשני, והשני שווה לשלישי - יש מנצח!
            if (board[a] != 0 && board[a] == board[b] && board[b] == board[c]) {
                return board[a];
            }
        }
        return 0; // אף אחד לא ניצח עדיין
    }
}