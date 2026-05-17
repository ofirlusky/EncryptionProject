package game;

import crypto.AESFileEncryptor;
import crypto.KeyGenerator;
import graph.EulerianCryptoGraph;
import graph.BaseCryptoGraph;
import graph.MaxFlowCryptoGraph;
import utils.Node;

import java.util.List;
import java.util.Map;

public class GameRules {

    // שמות קבצי ברירת-מחדל — שנה לפי הצורך
    private static final String INPUT_FILE     = "secret.txt";
    private static final String ENCRYPTED_FILE = "secret_encrypted.bin";
    private static final String DECRYPTED_FILE = "secret_decrypted.txt";

    private GameRules() {}

    // =========================================================
    // Basic rules
    // =========================================================

    public static boolean isPeaceFree(Node node) {
        return !(node == null || node.getCurrentPieceValue() != PieceValue.EMPTY);
    }

    public static boolean isValidMove(Node start, Node end, int[][] neighbors) {
        if (!isPeaceFree(end)) return false;
        int startID = start.getIDofNode();
        int endID   = end.getIDofNode();
        for (int i = 0; i < neighbors[startID].length; i++)
            if (neighbors[startID][i] == endID) return true;
        return false;
    }

    // =========================================================
    // Win detection + full crypto pipeline
    // =========================================================

    public static PlayerID checkWin(Node[] board,
                                    Map<Direction, List<Integer>>[] neighborsByDir,
                                    List<game.Move> gameHistory) {

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

                        runFullCryptoPipeline(board, gameHistory);

                        return (val == PieceValue.OCCUPIED_P1)
                                ? PlayerID.PLAYER_ONE
                                : PlayerID.PLAYER_TWO;
                    }
                }
            }
        }
        return null;
    }

    // =========================================================
    // Full pipeline: 4 algorithms → KeyGenerator → AES file encryption
    // =========================================================

    private static void runFullCryptoPipeline(Node[] board, List<Move> gameHistory) {

        // ── שלב 1: הרצת 4 האלגוריתמים ───────────────────────────────────

        System.out.println("\n[1/4] Running Kruskal (MST + Floyd-Warshall)...");
        BaseCryptoGraph kruskal = new graph.KruskalCryptoGraph(board);
        kruskal.printKeyMatrix();

        System.out.println("\n[2/4] Running MaxFlow (Edmonds-Karp + Floyd-Warshall)...");
        BaseCryptoGraph maxFlow = new MaxFlowCryptoGraph(board);
        maxFlow.printKeyMatrix();

        System.out.println("\n[3/4] Running Bitwise (XOR mask + Dijkstra + Floyd-Warshall)...");
        BaseCryptoGraph bitwise = new graph.BitwiseCryptoGraph(board);
        bitwise.printKeyMatrix();

        System.out.println("\n[4/4] Running Eulerian path (Hierholzer)...");
        EulerianCryptoGraph euler = new EulerianCryptoGraph(13, gameHistory);
        euler.printKeyStream();

        // ── שלב 2: שילוב התוצאות למפתח 256-ביט ─────────────────────────

        System.out.println("\n>>> Combining all outputs into 256-bit encryption key...");

        byte[] finalKey = KeyGenerator.generateKey(
                kruskal.getKeyMatrix(),
                maxFlow.getKeyMatrix(),
                bitwise.getKeyMatrix(),
                euler.getKeyStream()
        );

        KeyGenerator.printKey(finalKey);

        // ── שלב 3: הצפנת קובץ ב-AES-128 CBC ────────────────────────────

        System.out.println("\n>>> Launching AES-128 CBC file encryption...");
        AESFileEncryptor encryptor = new AESFileEncryptor(finalKey);
        encryptor.encryptFile(INPUT_FILE, ENCRYPTED_FILE);
        encryptor.decryptFile(ENCRYPTED_FILE, DECRYPTED_FILE);
    }
}
