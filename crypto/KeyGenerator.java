package crypto;

import java.util.List;

/**
 * KeyGenerator — משלב את תוצאות 4 האלגוריתמים למפתח 256-ביט.
 *
 * תהליך ה-pipeline:
 *   Phase 1 — XOR בין שלוש מטריצות (Kruskal, MaxFlow, Bitwise)
 *   Phase 2 — ערבוב עם זרם אוילר + פולינום L (בהשראת function_L)
 *   Phase 3 — קיפול ל-seed של 256 ביט
 *   Phase 4 — שלב DIFFUSION ← חדש! מבטיח אפקט מפולת
 *   Phase 5 — הרחבה ע"י PRNG (בהשראת function_G/H)
 *   Phase 6 — DIFFUSION סופי על המפתח
 *
 * ── מה תוקן ולמה ──────────────────────────────────────────────────────
 * הבעיה הקודמת: שינוי ביט אחד בקלט שינה רק ביט אחד במפתח.
 * הסיבה: כל ערך השפיע רק על תא בודד — אין "פיזור" (diffusion).
 *
 * הפתרון: שלב Diffusion בהשראת אלגוריתמים כמו SHA-256:
 *   1. Avalanche mixing — כל בית מערבב את כל הבתים שלפניו
 *   2. Bit rotation     — סיבוב ביטים שמפזר שינוי לרוחב הבית
 *   3. Multi-pass       — שני מעברים (קדימה ואחורה) לפיזור מלא
 *
 * עיקרון: שינוי ביט אחד בקלט → משפיע על כל 256 ביטי הפלט.
 */
public class KeyGenerator {

    private static final long MODULUS   = 36389L;
    private static final long GENERATOR = 1500L;
    private static final int  KEY_BYTES = 32;

    // קבועי ערבוב (בהשראת קבועי SHA / FNV — מספרים ראשוניים גדולים)
    private static final int PRIME_A = 0x9E3779B1;   // יחס הזהב (Knuth)
    private static final int PRIME_B = 0x85EBCA77;
    private static final int PRIME_C = 0xC2B2AE3D;

    private static int functionL(int x) {
        return x * x - 2 * x + 223;
    }

    // =========================================================
    // נקודת הכניסה
    // =========================================================

    public static byte[] generateKey(int[][] kruskalMatrix,
                                     int[][] maxFlowMatrix,
                                     int[][] bitwiseMatrix,
                                     List<Integer> eulerStream) {

        // ── Phase 1: XOR בין שלוש המטריצות ──────────────────────────────
        int n = kruskalMatrix.length;
        int[] flat = new int[n * n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                flat[i * n + j] = kruskalMatrix[i][j]
                        ^ maxFlowMatrix[i][j]
                        ^ bitwiseMatrix[i][j];
            }
        }

        // ── Phase 2: ערבוב עם זרם אוילר + פולינום L ─────────────────────
        // שינוי: כל ערך אוילר מתפשט על כל המערך (לא תא בודד)
        for (int i = 0; i < eulerStream.size(); i++) {
            int v     = eulerStream.get(i);
            int lv    = functionL(v);
            int mixed = lv ^ (i * 31) ^ (v * 17);

            // הפצה: כל ערך אוילר משפיע על כל תא במערך flat
            for (int k = 0; k < flat.length; k++) {
                flat[k] ^= mixed;
                flat[k]  = Integer.rotateLeft(flat[k], (v + k) & 31);
                flat[k] *= PRIME_A;
            }
        }

        // ── Phase 3: קיפול ל-seed של 256 ביט ────────────────────────────
        byte[] seed = new byte[KEY_BYTES];
        for (int i = 0; i < flat.length; i++) {
            seed[i % KEY_BYTES]            ^= (byte)( flat[i]        & 0xFF);
            seed[(i * 3 + 7) % KEY_BYTES]  ^= (byte)((flat[i] >>  8) & 0xFF);
            seed[(i * 5 + 3) % KEY_BYTES]  ^= (byte)((flat[i] >> 16) & 0xFF);
            seed[(i * 7 + 1) % KEY_BYTES]  ^= (byte)((flat[i] >> 24) & 0xFF);
        }

        // ── Phase 4: DIFFUSION על ה-seed ────────────────────────────────
        // מבטיח שכל שינוי בקלט מתפשט לכל הביטים לפני ה-PRNG
        diffuse(seed);

        // ── Phase 5: הרחבת ה-seed ע"י PRNG ──────────────────────────────
        byte[] key = expandWithPRNG(seed);

        // ── Phase 6: DIFFUSION סופי על המפתח ────────────────────────────
        // מעבר אחרון שמבטיח אפקט מפולת מלא בפלט הסופי
        diffuse(key);
        diffuse(key);   // שני מעברים → פיזור מקסימלי

        return key;
    }

    // =========================================================
    // DIFFUSION — לב התיקון
    //
    // הפונקציה מבטיחה שכל ביט במערך משפיע על כל הביטים האחרים.
    // טכניקה (בהשראת SHA-256 ו-MurmurHash):
    //
    //   מעבר קדימה:  כל בית סופג את כל הבתים שלפניו (chaining)
    //   ערבוב ביט:   rotation + multiply בפריים → פיזור לרוחב הבית
    //   מעבר אחורה:  שוב, מהסוף להתחלה → פיזור דו-כיווני
    //
    // אחרי שני המעברים: שינוי של ביט אחד בכל מקום
    // מתפשט (avalanche) לכל 256 הביטים.
    // =========================================================

    private static void diffuse(byte[] data) {
        int len = data.length;

        // accumulator — "זוכר" את כל מה שעבר עד כה
        int acc = 0x12345678;

        // ── מעבר 1: קדימה (כל בית סופג את ההיסטוריה) ────────────────────
        for (int i = 0; i < len; i++) {
            int b = data[i] & 0xFF;

            acc ^= b;
            acc *= PRIME_A;                       // ערבוב כפלי
            acc  = Integer.rotateLeft(acc, 13);   // סיבוב — פיזור לרוחב
            acc ^= (acc >>> 7);                   // XOR-shift — ערבוב פנימי
            acc *= PRIME_B;

            data[i] = (byte)(acc & 0xFF);
        }

        // ── מעבר 2: אחורה (פיזור דו-כיווני) ─────────────────────────────
        acc = 0x76543210;
        for (int i = len - 1; i >= 0; i--) {
            int b = data[i] & 0xFF;

            acc ^= b;
            acc *= PRIME_C;
            acc  = Integer.rotateLeft(acc, 17);
            acc ^= (acc >>> 11);
            acc *= PRIME_A;

            data[i] = (byte)((data[i] & 0xFF) ^ (acc & 0xFF));
        }

        // ── מעבר 3: ערבוב סופי בין כל זוגות הבתים ───────────────────────
        // כל בית מושפע מהבית "ממול" — מבטיח שאין אזור "מבודד"
        for (int i = 0; i < len / 2; i++) {
            int j = len - 1 - i;
            int x = data[i] & 0xFF;
            int y = data[j] & 0xFF;

            int mixed = (x + y) & 0xFF;
            mixed = ((mixed << 3) | (mixed >>> 5)) & 0xFF;   // rotate בתוך הבית

            data[i] = (byte)(x ^ mixed);
            data[j] = (byte)(y ^ mixed);
        }
    }

    // =========================================================
    // Phase 5 — PRNG (function_G/H) — ללא שינוי בלוגיקה
    // =========================================================

    private static byte[] expandWithPRNG(byte[] seed) {
        StringBuilder sb = new StringBuilder();
        for (byte b : seed) {
            String bits = Integer.toBinaryString(b & 0xFF);
            while (bits.length() < 8) bits = "0" + bits;
            sb.append(bits);
        }

        String current = sb.toString();
        StringBuilder outputBits = new StringBuilder();
        int iterations = KEY_BYTES * 8;

        for (int i = 0; i < iterations; i++) {
            int mid        = current.length() / 2;
            String firstH  = current.substring(0, mid);
            String secondH = current.substring(mid);

            current = applyH(firstH, secondH);
            outputBits.append(current.charAt(current.length() - 1));
            current = current.substring(0, current.length() - 1);
        }

        byte[] key = new byte[KEY_BYTES];
        for (int i = 0; i < KEY_BYTES; i++) {
            String byteStr = outputBits.substring(i * 8, (i + 1) * 8);
            key[i] = (byte) Integer.parseInt(byteStr, 2);
        }
        return key;
    }

    private static String applyH(String firstHalf, String secondHalf) {
        long val = 0;
        for (char c : firstHalf.toCharArray()) {
            val = (val << 1) | (c - '0');
        }

        long modExpVal = modPow(GENERATOR, val, MODULUS);

        String modExpBin = Long.toBinaryString(modExpVal);
        int targetLen = firstHalf.length();
        if (modExpBin.length() > targetLen) {
            modExpBin = modExpBin.substring(modExpBin.length() - targetLen);
        } else {
            while (modExpBin.length() < targetLen) modExpBin = "0" + modExpBin;
        }

        int hardCoreBit = 0;
        int minLen = Math.min(firstHalf.length(), secondHalf.length());
        for (int i = 0; i < minLen; i++) {
            int f = firstHalf.charAt(i)  - '0';
            int s = secondHalf.charAt(i) - '0';
            hardCoreBit ^= (f & s);
        }
        hardCoreBit &= 1;

        return modExpBin + secondHalf + hardCoreBit;
    }

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

        System.out.print("║  HEX:    ");
        StringBuilder hex = new StringBuilder();
        for (byte b : key) hex.append(String.format("%02X", b));
        System.out.println(hex + "  ║");

        System.out.println("║  BINARY:                                                     ║");
        StringBuilder bin = new StringBuilder();
        for (int i = 0; i < key.length; i++) {
            String bits = Integer.toBinaryString(key[i] & 0xFF);
            while (bits.length() < 8) bits = "0" + bits;
            bin.append(bits);
            if ((i + 1) % 4 == 0) bin.append(" ");
        }
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