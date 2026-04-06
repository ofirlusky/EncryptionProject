package game;

public class Player
{
    public static final int maxSoldier=3;
    private PlayerID playerID;
    private int numberOfpiecesPlaced;


    public Player(PlayerID playerID) {
        this.playerID = playerID;
        this.numberOfpiecesPlaced = 0;

    }


    public void updateNumberOfSoldiers()
    {
        this.numberOfpiecesPlaced++;
    }


    public boolean IsThreeSoldiers()
    {
        return this.numberOfpiecesPlaced == maxSoldier;
    }




// getters and setters
    public PlayerID getPlayerID() {
        return this.playerID;
    }


    public int getNumberOfpiecesPlaced() {
        return this.numberOfpiecesPlaced;
    }

    public void setNumberOfpiecesPlaced(int numberOfpiecesPlaced) {
        this.numberOfpiecesPlaced = numberOfpiecesPlaced;
    }
}
