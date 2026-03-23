import java.awt.Point;

public class PicariaGame {

    public static final int TOTAL_NODES=13;
    private Node board[]; // 0 empty , 1 player1 , 2 player2

    private Player playerNumberOne;
    private Player playerNumberTwo;
    private PlayerID currentTurn;


    private final int[][] neighbors = {
            {1, 3, 5},                // 0 רשימת סמיכויות לפי האינדקס
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

    public PicariaGame() {
        this.board = new Node[TOTAL_NODES];
        for(int i=0; i< PicariaGame.TOTAL_NODES; i++)
        {
            this.board[i] = new Node(i);
        }

        this.playerNumberOne = new Player(PlayerID.PLAYER_ONE);
        this.playerNumberTwo = new Player(PlayerID.PLAYER_TWO);
        this.currentTurn = PlayerID.PLAYER_ONE;
    }
}