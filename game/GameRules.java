package game;

import utils.Node;

import java.util.List;
import java.util.Map;

/**
 * GameRules — חוקי המשחק וזיהוי ניצחון.
 *
 * שים לב: ה-crypto pipeline (4 אלגוריתמים + מפתח + AES)
 * כבר לא רץ כאן. הוא עבר ל-CryptoResultGUI שנפתח
 * אחרי שהמשתמש לוחץ "המשך לקריפטו" בחלון המשחק.
 *
 * checkWin רק מזהה ניצחון ומחזיר את המנצח —
 * ה-GUI אחראי על הפעלת הקריפטו.
 */
public class GameRules {

    private GameRules() {}

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

    /**
     * מזהה ניצחון: 3 כלים של אותו שחקן באותו כיוון.
     * מחזיר את המנצח, או null אם אין מנצח עדיין.
     *
     * הערה: gameHistory מתקבל לשם תאימות לחתימה הקיימת,
     * אך הקריפטו עצמו מורץ ע"י ה-GUI.
     */
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

                        return (val == PieceValue.OCCUPIED_P1)
                                ? PlayerID.PLAYER_ONE
                                : PlayerID.PLAYER_TWO;
                    }
                }
            }
        }
        return null;
    }
}