import java.util.List;
import java.util.Map;
public class BitwiseCryptoGraph extends BaseCryptoGraph {


    //  תכונה ייחודית למחלקה הזאת ברור שזה בנוסף לתכונות שהוא מקבל מאבא שלו

    private int boardMask;

    public BitwiseCryptoGraph(Node[] finalBoard, Map<Direction, List<Integer>>[] neighborsByDir) {
        super(finalBoard);
        this.boardMask = generateBoardMask();
        buildGraph(neighborsByDir);
    }

    private int generateBoardMask() {
        int mask = 0;
        for (int i = 0; i < vertices.length; i++) {

            int pieceValue = StatuseOfTheNode(vertices[i]);
            mask = mask | (pieceValue << (i * 2));
        }
        return mask;
    }


    @Override
    protected int calculateEdgeWeight(Node u, Node v) {
        int uId = u.getIDofNode();
        int vId = v.getIDofNode();
        int su = StatuseOfTheNode(u);

        // מכפיל במספרים ראשוניים
        int base = (uId * 17) ^ (vId * 31);
        int shifted = base << (su + 1);
        int combined = shifted ^ boardMask;
        // בשביל שהמשקלים יהיו קטנים ויעילים
        int weight = Math.abs(combined) % 251 + 1;
        return weight;
    }

    public int StatuseOfTheNode(Node node)
    {
        int su = 0;
        switch (node.getCurrentPieceValue()) {
            case OCCUPIED_P1:
                su = 1;
                break;
            case OCCUPIED_P2:
                su = 2;
                break;
        }
        return su;
    }





}
