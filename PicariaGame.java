import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PicariaGame {

    public static final int TOTAL_NODES = 13;


    //
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
    public static final int[][] COORDS = {
            {0,0}, {2,0}, {4,0}, {1,1}, {3,1}, {0,2}, {2,2}, {4,2}, {1,3}, {3,3}, {0,4}, {2,4}, {4,4}
    };


    // history מבנה נתונים בונה גרף היסטוריית משחק גרף heliro -
    // לבנות גרף של board ופה עליו פלורייד ווארשל - גרף ממושקל -


    private Node[] board;
    private Player playerNumberOne;
    private Player playerNumberTwo;
    private PlayerID currentTurn;
    private int numberOfpiecesPlaced = 0;
    private Node selectedNode = null;
    private boolean isGameOver = false;
    private Map<Direction, List<Integer>>[] neighborsByDir;

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

        buildNeighborsByDir();
    }

    private void buildNeighborsByDir() {
        neighborsByDir = new HashMap[TOTAL_NODES];
        for (int i = 0; i < TOTAL_NODES; i++) {
            neighborsByDir[i] = new HashMap<>();
            for (int neighborId : neighbors[i]) {
                int dx = COORDS[neighborId][0] - COORDS[i][0];
                int dy = COORDS[neighborId][1] - COORDS[i][1];
                Direction dir = getDirection(dx, dy);
                neighborsByDir[i].computeIfAbsent(dir, k -> new ArrayList<>()).add(neighborId);
            }
        }
    }

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
        PlayerID winner = GameRules.checkWin(board, neighborsByDir);
        if (winner != null) {
            isGameOver = true;
        }
    }

    private boolean isCurrentPlayerPiece(Node node) {
        if (currentTurn == PlayerID.PLAYER_ONE && node.getCurrentPieceValue() == PieceValue.OCCUPIED_P1) return true;
        if (currentTurn == PlayerID.PLAYER_TWO && node.getCurrentPieceValue() == PieceValue.OCCUPIED_P2) return true;
        return false;
    }

    // Getters
    public Node[] getBoard() { return board; }
    public PlayerID getCurrentTurn() { return currentTurn; }
    public boolean isGameOver() { return isGameOver; }
    public Node getSelectedNode() { return selectedNode; }

    public int getNumberOfpiecesPlaced() {
        return numberOfpiecesPlaced;
    }

    public Map<Direction, List<Integer>>[] getNeighborsByDir() { return neighborsByDir; }
}