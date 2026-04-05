import java.util.List;
import java.util.Map;

public class GameRules
{
    private GameRules() {
    }

    public static boolean isPeaceFree(Node node)
    {
        return !(node == null || node.getCurrentPieceValue() != PieceValue.EMPTY);
    }

    public static boolean isValidMove(Node start , Node end ,int[][]neighbors)
    {
        if (!isPeaceFree(end))
        {
            return false;
        }
        int startID = start.getIDofNode();
        int endID = end.getIDofNode();

        for(int i=0; i< neighbors[startID].length; i++)
        {
            if(neighbors[startID][i] == endID)
                return true;

        }
        return false;

    }


    public static PlayerID checkWin(Node[] board, Map<Direction, List<Integer>>[] neighborsByDir) {

        for (int u = 0; u < board.length; u++) {
            PieceValue val = board[u].getCurrentPieceValue();
            if (val == PieceValue.EMPTY) continue;

            for (Map.Entry<Direction, List<Integer>> entry : neighborsByDir[u].entrySet()) {
                List<Integer> sameDir = entry.getValue();

                if (sameDir.size() == 2) {
                    int v = sameDir.get(0);
                    int w = sameDir.get(1);

                    if (board[v].getCurrentPieceValue() == val &&
                            board[w].getCurrentPieceValue() == val) {


                            BaseCryptoGraph graph = new KruskalCryptoGraph(board);
                            graph.printMatrix();

                        //BaseCryptoGraph b = new BitwiseCryptoGraph(board);
                            //b.printMatrix();
                            return (val == PieceValue.OCCUPIED_P1) ? PlayerID.PLAYER_ONE : PlayerID.PLAYER_TWO;

                    }
                }
            }
        }
        return null;
    }











}
