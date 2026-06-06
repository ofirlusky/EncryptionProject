package crypto;

import game.Move;
import game.PieceValue;
import graph.BaseCryptoGraph;
import graph.BitwiseCryptoGraph;
import graph.EulerianCryptoGraph;
import graph.KruskalCryptoGraph;
import graph.MaxFlowCryptoGraph;
import utils.Node;

import java.util.List;

public class AvalancheTester {

    public static class AvalancheResult {


        // מייצג את מפתח ההצפנה הרגיל לפני השינוי
        public byte[] originalKey;


        // מייצג את מפתח ההצפנה החדש לאחר שנהפך ביט אחד
        public byte[] modifiedKey;

        // מספר הביטים הכולל (256)
        public int totalBits;

        // מספר הביטים ששונו
        public int changedBits;

        // תיאור של מה קרה
        public String changeDescription;

        // מספר סידורי של הביט הספציפי שנבחר להפוך
        public int flippedBitIndex;

        // ייצוג טקסטואלי של הלוח לפני השינוי
        public String boardBitsBefore;

        // ייצוג של הלוח אחרי השינוי
        public String boardBitsAfter;
    }

    // תיאור של כמה ביטים מייצגים כל משבצת
    private static final int BITS_PER_CELL = 2;




    //
    public static AvalancheResult runAvalanche(Node[] board, List<Move> gameHistory) {
        AvalancheResult r = new AvalancheResult();

        // מפתח מקורי יוצרים אותו ושומרים
        r.originalKey = computeKey(board, gameHistory);

        // קידוד הלוח לרצף ביטים (26 ביטים)
        int[] bits = boardToBits(board);
        r.boardBitsBefore = bitsToString(bits);

        // הופכים בדיוק ביט אחד
        int flipIndex = chooseFlipBit(bits);
        r.flippedBitIndex = flipIndex;

        int[] flipped = bits.clone();
        flipped[flipIndex] ^= 1;                  // XOR עם 1 = הפיכת ביט אחד
        r.boardBitsAfter = bitsToString(flipped);

        // מפענח בחזרה ללוח
        Node[] modifiedBoard = bitsToBoard(flipped, board.length);

        int cellChanged = flipIndex / BITS_PER_CELL;
        r.changeDescription = String.format(
                "Flipped bit #%d of %d  (board cell #%d, %s bit)%n" +
                        "         The board is encoded as %d bits (13 cells x 2 bits each)",
                flipIndex, bits.length, cellChanged,
                (flipIndex % 2 == 0 ? "high" : "low"),
                bits.length
        );

        // מפתח אחרי הפיכת הביט
        r.modifiedKey = computeKey(modifiedBoard, gameHistory);

        r.totalBits   = r.originalKey.length * 8;
        r.changedBits = hammingDistance(r.originalKey, r.modifiedKey);

        return r;
    }


    // דוחס את כל ה board אל ביטים
    private static int[] boardToBits(Node[] board) {
        int[] bits = new int[board.length * BITS_PER_CELL];
        for (int i = 0; i < board.length; i++) {
            int val = pieceToInt(board[i].getCurrentPieceValue());
            bits[2 * i] = (val >> 1) & 1;   // ביט גבוה
            bits[2 * i + 1] =  val & 1;   // ביט נמוך
        }
        return bits;
    }

    // לוקח ביטים והופך אותם ל board חשוב לציין ה ייצוג הוא שני ביטים עבור מצב משחק
    private static Node[] bitsToBoard(int[] bits, int boardSize) {
        Node[] board = new Node[boardSize];
        for (int i = 0; i < boardSize; i++) {
            int hi  = bits[2 * i];
            int lo  = bits[2 * i + 1];
            int val = (hi << 1) | lo;

            if (val == 3) val = 0;   // לא חוקי

            Node node = new Node(i);
            node.setCurrentPieceValue(intToPiece(val));
            board[i] = node;
        }
        return board;
    }

    // נועד להמיר מצב אל מספר בשביל להעביר לבינארי
    private static int pieceToInt(PieceValue v) {
        switch (v) {
            case OCCUPIED_P1: return 1;
            case OCCUPIED_P2: return 2;
            default:          return 0;
        }
    }

    private static PieceValue intToPiece(int v) {
        switch (v) {
            case 1:  return PieceValue.OCCUPIED_P1;
            case 2:  return PieceValue.OCCUPIED_P2;
            default: return PieceValue.EMPTY;
        }
    }


    // מביא את האינדקס שבו הופכים את הביט
    private static int chooseFlipBit(int[] bits) {
        for (int i = 0; i < bits.length; i++) {
            if (bits[i] == 1) return i;
        }
        return bits.length / 2;
    }


    // נועדה לטפל בביטים בצורה יפה יותר בשביל ההדפסה
    private static String bitsToString(int[] bits) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bits.length; i++) {
            sb.append(bits[i]);
            if ((i + 1) % 2 == 0 && i < bits.length - 1) sb.append(' ');
        }
        return sb.toString();
    }


    // מחזיר מפתח מוכן
    public static byte[] computeKey(Node[] board, List<Move> history) {
        BaseCryptoGraph kruskal = new KruskalCryptoGraph(board);
        BaseCryptoGraph maxFlow = new MaxFlowCryptoGraph(board);
        BaseCryptoGraph bitwise = new BitwiseCryptoGraph(board);
        EulerianCryptoGraph euler = new EulerianCryptoGraph(13, history);
        return KeyGenerator.generateKey(
                kruskal.getKeyMatrix(), maxFlow.getKeyMatrix(),
                bitwise.getKeyMatrix(), euler.getKeyStream());
    }

    // מחשב כמה ביטים שונו
    private static int hammingDistance(byte[] a, byte[] b) {
        int count = 0;
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            int xor = (a[i] ^ b[i]) & 0xFF;
            while (xor != 0) { count += (xor & 1); xor >>= 1; }
        }
        return count;
    }

    public static String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) sb.append(String.format("%02X", b & 0xFF));
        return sb.toString();
    }


    public static String diffMap(byte[] a, byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            int xor = (a[i] ^ b[i]) & 0xFF;
            for (int bit = 7; bit >= 0; bit--)
                sb.append(((xor >> bit) & 1) == 1 ? '#' : '.');
            sb.append((i + 1) % 8 == 0 ? '\n' : ' ');
        }
        return sb.toString();
    }
}