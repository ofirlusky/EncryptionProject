package crypto;
import java.util.List;

/**
 * KeyGenerator — משלב את תוצאות כל 4 האלגוריתמים למפתח הצפנה אחד של 256 ביט.
 *
 * תהליך ה-pipeline:
 *   Phase 1 — XOR בין שלוש מטריצות פלויד-וורשאל (Kruskal, MaxFlow, Bitwise)
 *   Phase 2 — ערבוב עם זרם אוילר + טרנספורמציה פולינומיאלית (בהשראת function_L)
 *   Phase 3 — קיפול (folding) של כל הנתונים ל-seed של 256 ביט
 *   Phase 4 — הרחבת ה-seed ע"י PRNG בהשראת function_G/H:
 *               modular exponentiation + hard-core bit בכל איטרציה
 */
public class KeyGenerator {

    // --- קבועים בהשראת ה-PRNG בפייתון ---
    private static final long MODULUS   = 36389L;
    private static final long GENERATOR = 1500L;
    private static final int  KEY_BYTES = 32;      // 256 ביט = 32 בתים (AES-256)

    // פולינום L(x) = x² − 2x + 223  (בהשראת function_L)
    private static int functionL(int x) {
        return x * x - 2 * x + 223;
    }

    // =========================================================
    // נקודת הכניסה הציבורית
    // =========================================================

    /**
     * @param kruskalMatrix  מטריצת keyMatrix של KruskalCryptoGraph  (13x13)
     * @param maxFlowMatrix  מטריצת keyMatrix של MaxFlowCryptoGraph  (13x13)
     * @param bitwiseMatrix  מטריצת keyMatrix של BitwiseCryptoGraph  (13x13)
     * @param eulerStream    רשימת הצמתים של EulerianCryptoGraph
     * @return               מפתח הצפנה של 32 בתים (256 ביט)
     */
    public static byte[] generateKey(int[][] kruskalMatrix,
                                     int[][] maxFlowMatrix,
                                     int[][] bitwiseMatrix,
                                     List<Integer> eulerStream) {

        // ── Phase 1: XOR בין שלוש המטריצות element-wise ──────────────────────
        int n = kruskalMatrix.length;          // 13
        int[] flat = new int[n * n];           // מערך שטוח של 169 ערכים

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                // XOR של שלושת המנועים: כל אחד תורם ביטים שונים
                flat[i * n + j] = kruskalMatrix[i][j]
                        ^ maxFlowMatrix[i][j]
                        ^ bitwiseMatrix[i][j];
            }
        }

        // ── Phase 2: ערבוב עם זרם אוילר + פולינום L ─────────────────────────
        // L(v) = v² − 2v + 223  ← טרנספורמציה לא-לינארית על כל ערך אוילר
        for (int i = 0; i < eulerStream.size(); i++) {
            int v      = eulerStream.get(i);
            int lv     = functionL(v);              // פולינום
            int mixed  = lv ^ (i * 31) ^ (v * 17); // XOR עם אינדקס ומשקל ראשוני
            flat[i % flat.length] ^= mixed;
        }

        // ── Phase 3: קיפול (folding) ל-seed של KEY_BYTES בתים ───────────────
        // כל ערך במערך השטוח מתפזר על שני תאים שונים ב-seed (byte נמוך + byte גבוה)
        byte[] seed = new byte[KEY_BYTES];

        for (int i = 0; i < flat.length; i++) {
            seed[i % KEY_BYTES]              ^= (byte)( flat[i]       & 0xFF);
            seed[(i * 3 + 7) % KEY_BYTES]   ^= (byte)((flat[i] >> 8) & 0xFF);
        }

        // ── Phase 4: הרחבת ה-seed ע"י PRNG (function_G/H) ───────────────────
        return expandWithPRNG(seed);
    }

    // =========================================================
    // Phase 4 — PRNG בהשראת function_G
    // =========================================================

    /**
     * ממיר את ה-seed לסדרת ביטים ומריץ עליו KEY_BYTES*8 איטרציות של function_H.
     * כל איטרציה מפיקה ביט אחד (ה-hard-core bit) שנאסף למפתח הסופי.
     */
    private static byte[] expandWithPRNG(byte[] seed) {

        // המרה ל-String בינארי (כמו Python)
        StringBuilder sb = new StringBuilder();
        for (byte b : seed) {
            String bits = Integer.toBinaryString(b & 0xFF);
            // padding ל-8 ביט
            while (bits.length() < 8) bits = "0" + bits;
            sb.append(bits);
        }

        String current = sb.toString();         // 256 ביט
        StringBuilder outputBits = new StringBuilder();

        // KEY_BYTES * 8 = 256 איטרציות → 256 ביט פלט
        int iterations = KEY_BYTES * 8;

        for (int i = 0; i < iterations; i++) {
            int mid         = current.length() / 2;
            String firstH   = current.substring(0, mid);
            String secondH  = current.substring(mid);

            // function_H: modular exponentiation + hard-core bit
            current = applyH(firstH, secondH);

            // הביט האחרון הוא ה-hard-core bit — הוא נאסף לפלט
            outputBits.append(current.charAt(current.length() - 1));

            // מסירים את הביט שנאסף (current חוזר לאורכו המקורי)
            current = current.substring(0, current.length() - 1);
        }

        // המרה חזרה לבתים
        byte[] key = new byte[KEY_BYTES];
        for (int i = 0; i < KEY_BYTES; i++) {
            String byteStr = outputBits.substring(i * 8, (i + 1) * 8);
            key[i] = (byte) Integer.parseInt(byteStr, 2);
        }

        return key;
    }

    // =========================================================
    // function_H — לב ה-PRNG (מקביל לפייתון)
    // =========================================================

    /**
     * מקבל שני חצאי מחרוזת בינארית.
     * 1. מחשב GENERATOR^(firstHalf כמספר בינארי) mod MODULUS  → modExpBin
     * 2. מחשב hard-core bit = XOR של (firstHalf[i] AND secondHalf[i])
     * 3. מחזיר modExpBin + secondHalf + hardCoreBit
     *
     * האורך של הפלט = |firstHalf| + |secondHalf| + 1 = |input| + 1
     * (הביט הנוסף נאסף ואז מוסר בלולאת expandWithPRNG, כך שהאורך נשמר)
     */
    private static String applyH(String firstHalf, String secondHalf) {

        // ── חלק א': modular exponentiation ────────────────────────────────
        // פרסור firstHalf כמספר בינארי
        long val = 0;
        for (char c : firstHalf.toCharArray()) {
            val = (val << 1) | (c - '0');
        }

        long modExpVal = modPow(GENERATOR, val, MODULUS);

        // המרה ל-String בינארי באורך |firstHalf|
        String modExpBin = Long.toBinaryString(modExpVal);
        int targetLen = firstHalf.length();
        if (modExpBin.length() > targetLen) {
            // לוקחים את targetLen הביטים הנמוכים
            modExpBin = modExpBin.substring(modExpBin.length() - targetLen);
        } else {
            while (modExpBin.length() < targetLen) modExpBin = "0" + modExpBin;
        }

        // ── חלק ב': hard-core bit ─────────────────────────────────────────
        // XOR של (firstHalf[i] AND secondHalf[i]) לכל i
        int hardCoreBit = 0;
        int minLen = Math.min(firstHalf.length(), secondHalf.length());
        for (int i = 0; i < minLen; i++) {
            int f = firstHalf.charAt(i)  - '0';
            int s = secondHalf.charAt(i) - '0';
            hardCoreBit ^= (f & s);
        }
        hardCoreBit &= 1;   // וידוא שנשאר 0 או 1

        // ── חיבור הפלט ───────────────────────────────────────────────────
        return modExpBin + secondHalf + hardCoreBit;
    }

    // =========================================================
    // עזר: modular exponentiation יעיל (square-and-multiply)
    // =========================================================

    private static long modPow(long base, long exp, long mod) {
        if (mod == 1) return 0;
        long result = 1;
        base %= mod;
        while (exp > 0) {
            if ((exp & 1L) == 1L) result = result * base % mod;
            exp >>= 1;
            base = base * base % mod;
        }
        return result;
    }

    // =========================================================
    // הדפסת המפתח
    // =========================================================

    public static void printKey(byte[] key) {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║            FINAL ENCRYPTION KEY  —  256-bit                 ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");

        // הקסדצימלי
        System.out.print("║  HEX:    ");
        StringBuilder hex = new StringBuilder();
        for (byte b : key) hex.append(String.format("%02X", b));
        System.out.println(hex + "  ║");

        // בינארי (חלוקה לקבוצות של 32 ביט לנוחות קריאה)
        System.out.println("║  BINARY:                                                     ║");
        StringBuilder bin = new StringBuilder();
        for (int i = 0; i < key.length; i++) {
            String bits = Integer.toBinaryString(key[i] & 0xFF);
            while (bits.length() < 8) bits = "0" + bits;
            bin.append(bits);
            if ((i + 1) % 4 == 0) bin.append(" ");
        }
        // הדפסה בשתי שורות (128 ביט כל אחת)
        String binStr = bin.toString();
        System.out.println("║  " + binStr.substring(0, binStr.length() / 2).trim() + "  ║");
        System.out.println("║  " + binStr.substring(binStr.length() / 2).trim()     + "  ║");

        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.println("║  Length: " + key.length * 8 + " bits  (" + key.length
                + " bytes)  →  Ready for AES-256               ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();
    }
}
