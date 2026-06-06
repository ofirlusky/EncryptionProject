package game;

import utils.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PicariaGame {

    public static final int TOTAL_NODES = 13;

    private List<Move> gameHistory;


    // מיקום זה הצומת ואז יש את השכנים - רשימת סמיכויות
    public static final int[][] neighbors =
            {
            {1, 3, 5},
            {0, 2, 3, 4, 6},
            {1, 4, 7},
            {0, 1, 5, 6},
            {1, 2, 6, 7},
            {0, 3, 6, 8, 10},
            {1, 3, 4, 5, 7, 8, 9, 11},
            {2, 4, 6, 9, 12},
            {5, 6, 10, 11},
            {6, 7, 11, 12},
            {5, 8, 11},
            {6, 8, 9, 10, 12},
            {7, 9, 11}
    };

    // לגבי coords המיקום הוא הצומת ואז הערך הוא הנקודה
    public static final int[][] COORDS = {
            {0,0}, {2,0}, {4,0}, {1,1}, {3,1}, {0,2}, {2,2}, {4,2}, {1,3}, {3,3}, {0,4}, {2,4}, {4,4}
    };


    // history מבנה נתונים בונה גרף היסטוריית משחק גרף heliro -


    private Node[] board;
    private Player playerNumberOne;
    private Player playerNumberTwo;
    private PlayerID currentTurn;
    private int numberOfpiecesPlaced = 0;
    private Node selectedNode = null;
    private boolean isGameOver = false;


    // מערך של מילונים , האינדקס במערך מייצג את הצומת ואז יש המון מילונים שמראים את הכיוון ואת השכנים שזהם ה values בכיוון הזה
    // מבנה נתונים קריטי רצח כי בלי זה לא הייתי יכול לבדוק נצחון
    private static Map<Direction, List<Integer>>[] neighborsByDir;

    public PicariaGame() {
        this.board = new Node[TOTAL_NODES];
        for(int i = 0; i < TOTAL_NODES; i++) {
            this.board[i] = new Node(i);
        }
        this.playerNumberOne = new Player(PlayerID.PLAYER_ONE);
        this.playerNumberTwo = new Player(PlayerID.PLAYER_TWO);
        this.currentTurn = PlayerID.PLAYER_ONE;
        this.isGameOver = false;
        this.numberOfpiecesPlaced = 0;
        this.gameHistory = new ArrayList<>();

        buildNeighborsByDir();
    }


    //
    private void buildNeighborsByDir() {
        neighborsByDir = new HashMap[TOTAL_NODES];
        // מאתחל מילון עבור כל צומת
        for (int i = 0; i < TOTAL_NODES; i++) {
            neighborsByDir[i] = new HashMap<>();
            // עובר על כל שכן של צומת
            for (int neighborId : neighbors[i]) {
                // מחשב את ההפרש בין הצומת לשכן
                // כמה זז ימינה או שמאלה
                int dx = COORDS[neighborId][0] - COORDS[i][0];
                // כמה זז למעלה או למטה
                int dy = COORDS[neighborId][1] - COORDS[i][1];
                //
                Direction dir = getDirection(dx, dy);
                // ממלא את המערך במיקום הצומת שהערך הוא כיוון שמצביע על הערך צומת שכן
                neighborsByDir[i].computeIfAbsent(dir, k -> new ArrayList<>()).add(neighborId);
            }
        }
    }


    //  ממיר מספר לכיוון
    private Direction getDirection(int dx, int dy) {
        if (dy == 0)
        {
            return Direction.HORIZONTAL;
        }
        if (dx == 0)
        {
            return Direction.VERTICAL;
        }
        if ((dx > 0) == (dy > 0))
        {
            return Direction.DIAG_DOWN_RIGHT;
        }
        return Direction.DIAG_DOWN_LEFT;
    }


    public void playTurn(int nodeID) {
        if(isGameOver) {
            System.out.println("The game is already over!");
            return;
        }

        Node clickedNode = board[nodeID];

        if(numberOfpiecesPlaced < 6) {
            handleDropPhase(clickedNode);
        } else {
            handleMovePhase(clickedNode);
        }
    }


    // בהתחלה במשחק שמים ישר 6 שחקנים
    private void handleDropPhase(Node clickedNode) {
        if(GameRules.isPeaceFree(clickedNode)) {
            if(currentTurn == PlayerID.PLAYER_ONE) {
                clickedNode.setCurrentPieceValue(PieceValue.OCCUPIED_P1);
                playerNumberOne.updateNumberOfSoldiers();
            } else {
                clickedNode.setCurrentPieceValue(PieceValue.OCCUPIED_P2);
                playerNumberTwo.updateNumberOfSoldiers();
            }

            numberOfpiecesPlaced++;
            checkEndGame();
            if(!isGameOver) {
                switchTurn();
            }
        } else {
            System.out.println("Slot already occupied!");
        }
    }




    // כאשר שלב ההנחה הסתיים אז שחקנים מזיזים כרגיל ז
    // selectedNode = מי אני רוצה להזיז
    // clickedNode = לאיפה אני רוצה להזיז
    // הקלט פה מייצג גם לחיצה על צומת למעבר וגם מעבר
    private void handleMovePhase(Node clickedNode) {
        if (selectedNode == null) {
            if (isCurrentPlayerPiece(clickedNode)) {
                selectedNode = clickedNode;
            }
        }
        else {
            if (isCurrentPlayerPiece(clickedNode)) {
                selectedNode = clickedNode;
                return;
            }

            if (GameRules.isValidMove(selectedNode, clickedNode, neighbors)) {


                // שומר בהיסטוריה את המהלך
                recordMove(selectedNode.getIDofNode(), clickedNode.getIDofNode());

                clickedNode.setCurrentPieceValue(selectedNode.getCurrentPieceValue());
                selectedNode.setCurrentPieceValue(PieceValue.EMPTY);
                selectedNode = null;

                checkEndGame();
                if (!isGameOver) {
                    switchTurn();
                }
            } else {
                selectedNode = null;
            }
        }
    }




    private void switchTurn() {
        currentTurn = (currentTurn == PlayerID.PLAYER_ONE) ? PlayerID.PLAYER_TWO : PlayerID.PLAYER_ONE;
    }

    private void checkEndGame() {
        PlayerID winner = GameRules.checkWin(board, neighborsByDir,gameHistory);
        if (winner != null) {
            isGameOver = true;
        }
    }

    private boolean isCurrentPlayerPiece(Node node) {
        if (currentTurn == PlayerID.PLAYER_ONE && node.getCurrentPieceValue() == PieceValue.OCCUPIED_P1) return true;
        if (currentTurn == PlayerID.PLAYER_TWO && node.getCurrentPieceValue() == PieceValue.OCCUPIED_P2) return true;
        return false;
    }

    public void recordMove(int from, int to) {
        gameHistory.add(new Move(from, to));
    }

    public List<Move> getGameHistory() {
        return gameHistory;
    }
    // Getters
    public Node[] getBoard() { return board; }
    public PlayerID getCurrentTurn() { return currentTurn; }
    public boolean isGameOver() { return isGameOver; }
    public Node getSelectedNode() { return selectedNode; }

    public int getNumberOfpiecesPlaced() {
        return numberOfpiecesPlaced;
    }

    public static Map<Direction, List<Integer>>[] getNeighborsByDir() { return neighborsByDir; }
}