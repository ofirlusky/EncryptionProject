import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PicariaGame {

    public static final int TOTAL_NODES=13;
    private Node board[]; // 0 empty , 1 player1 , 2 player2
    private int directions[];
    private Player playerNumberOne;
    private Player playerNumberTwo;
    private PlayerID currentTurn;

    private int numberOfpiecesPlaced = 0;
    private Node selectedNode = null;
    private boolean isGameOver = false;


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

    private final int[][] COORDS = {
            {0,0}, {2,0}, {4,0},
            {1,1}, {3,1},
            {0,2}, {2,2}, {4,2},
            {1,3}, {3,3},
            {0,4}, {2,4}, {4,4}
    };

    private Map<Direction, List<Integer>>[] neighborsByDir;
    // זה מערך שכל תא הוא מילון ובתוכו יש מיפוי של כיוונים והשכנים , יצרתי אותו בשביל אלגוריתם לבדיקת נצחון טוב ולא מעפן





    public PicariaGame() {
        this.board = new Node[TOTAL_NODES];
        for(int i=0; i< PicariaGame.TOTAL_NODES; i++)
        {
            this.board[i] = new Node(i);
        }
        this.playerNumberOne = new Player(PlayerID.PLAYER_ONE);
        this.playerNumberTwo = new Player(PlayerID.PLAYER_TWO);
        this.currentTurn = PlayerID.PLAYER_ONE;

        buildNeighborsByDir();
    }


    private void buildNeighborsByDir() {
        neighborsByDir = new HashMap[TOTAL_NODES];

        for (int i = 0; i < TOTAL_NODES; i++) {
            neighborsByDir[i] = new HashMap<>();
            for (int j=0; j < neighbors[i].length; j++) {
                int neighborId = neighbors[i][j];

                int dx = COORDS[neighborId][0] - COORDS[i][0];
                int dy = COORDS[neighborId][1] - COORDS[i][1];

                Direction dir = getDirection(dx, dy);

                neighborsByDir[i].computeIfAbsent(dir, k -> new ArrayList<>()).add(neighborId);
            }
        }
    }


    private Direction getDirection(int dx, int dy) {
        if (dy == 0) return Direction.HORIZONTAL;
        if (dx == 0) return Direction.VERTICAL;
        if ((dx > 0) == (dy > 0)) return Direction.DIAG_DOWN_RIGHT;
        return Direction.DIAG_DOWN_LEFT;
    }

    public Map<Direction, List<Integer>>[] getNeighborsByDir() {
        return neighborsByDir;
    }


    public void platTurn(int nodeID)
    {
        if(isGameOver)
        {
            System.out.println("game is fucking over");
            return;
        }
        Node clickedNode = board[nodeID];
        if(numberOfpiecesPlaced < 6)
        {
            handleDropPhase(clickedNode);
        }
        else
        {
            handleMovePhase(clickedNode);
        }
    }

    private void handleDropPhase(Node clickedNode)
    {
        if(GameRules.isPeaceFree(clickedNode))
        {
            if(currentTurn == PlayerID.PLAYER_ONE)
            {
                clickedNode.setCurrentPieceValue(PieceValue.OCCUPIED_P1);
                playerNumberOne.updateNumberOfSoldiers();


            }
            else {
                clickedNode.setCurrentPieceValue(PieceValue.OCCUPIED_P2);
                playerNumberTwo.updateNumberOfSoldiers();

            }
            numberOfpiecesPlaced++;
            checkEndGame();
            if(!isGameOver)
            {
                switchTurn();
            }
        }
        else {
            System.out.println("can not drope there");
        }

    }

    // פה הוא מטפל לי בשני לחיצות באותו פונקציה
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
        if (currentTurn == PlayerID.PLAYER_ONE) {
            currentTurn = PlayerID.PLAYER_TWO;
        } else {
            currentTurn = PlayerID.PLAYER_ONE;
        }
    }

    private void checkEndGame() {
        PlayerID winner = GameRules.checkWin(board, neighborsByDir);
        if (winner != null) {
            isGameOver = true;
            System.out.println("!!! WE HAVE A WINNER: " + winner + " !!!");
        }
    }

    private boolean isCurrentPlayerPiece(Node node) {
        if (currentTurn == PlayerID.PLAYER_ONE && node.getCurrentPieceValue() == PieceValue.OCCUPIED_P1) {
            return true;
        }
        if (currentTurn == PlayerID.PLAYER_TWO && node.getCurrentPieceValue() == PieceValue.OCCUPIED_P2) {
            return true;
        }
        return false;
    }


    public Node[] getBoard() {
        return board;
    }

    public void setBoard(Node[] board) {
        this.board = board;
    }

    public int[] getDirections() {
        return directions;
    }

    public void setDirections(int[] directions) {
        this.directions = directions;
    }

    public Player getPlayerNumberOne() {
        return playerNumberOne;
    }

    public void setPlayerNumberOne(Player playerNumberOne) {
        this.playerNumberOne = playerNumberOne;
    }

    public Player getPlayerNumberTwo() {
        return playerNumberTwo;
    }

    public void setPlayerNumberTwo(Player playerNumberTwo) {
        this.playerNumberTwo = playerNumberTwo;
    }

    public PlayerID getCurrentTurn() {
        return currentTurn;
    }

    public void setCurrentTurn(PlayerID currentTurn) {
        this.currentTurn = currentTurn;
    }

    public int getNumberOfpiecesPlaced() {
        return numberOfpiecesPlaced;
    }

    public void setNumberOfpiecesPlaced(int numberOfpiecesPlaced) {
        this.numberOfpiecesPlaced = numberOfpiecesPlaced;
    }

    public Node getSelectedNode() {
        return selectedNode;
    }

    public void setSelectedNode(Node selectedNode) {
        this.selectedNode = selectedNode;
    }

    public boolean isGameOver() {
        return isGameOver;
    }

    public void setGameOver(boolean gameOver) {
        isGameOver = gameOver;
    }

    public int[][] getNeighbors() {
        return neighbors;
    }

    public int[][] getCOORDS() {
        return COORDS;
    }

    public void setNeighborsByDir(Map<Direction, List<Integer>>[] neighborsByDir) {
        this.neighborsByDir = neighborsByDir;
    }
}