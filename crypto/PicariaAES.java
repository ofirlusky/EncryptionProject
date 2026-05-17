package crypto;

/**
 * PicariaAES — AES-128 מלא (10 סיבובים), עצמאי לחלוטין ב-crypto package.
 *
 * מותאם מ-StrongAES של הפרויקט — לוגיקת SubBytes / ShiftRows /
 * MixColumns / AddRoundKey זהה ותקינה. ההבדל:
 *   StrongAES  → מצפה ל-String בינארי של 1408 תווים (מפתח מורחב מראש)
 *   PicariaAES → מקבל byte[] מ-KeyGenerator ועושה Key Schedule פנימי
 *
 * ממשק ציבורי:
 *   encryptBlock(byte[16])  → byte[16]
 *   decryptBlock(byte[16])  → byte[16]
 *   printRoundKeys()
 */
public class PicariaAES {

    // ── S-Box ─────────────────────────────────────────────────────────────
    private static final int[] SBOX = {
            0x63,0x7c,0x77,0x7b,0xf2,0x6b,0x6f,0xc5,0x30,0x01,0x67,0x2b,0xfe,0xd7,0xab,0x76,
            0xca,0x82,0xc9,0x7d,0xfa,0x59,0x47,0xf0,0xad,0xd4,0xa2,0xaf,0x9c,0xa4,0x72,0xc0,
            0xb7,0xfd,0x93,0x26,0x36,0x3f,0xf7,0xcc,0x34,0xa5,0xe5,0xf1,0x71,0xd8,0x31,0x15,
            0x04,0xc7,0x23,0xc3,0x18,0x96,0x05,0x9a,0x07,0x12,0x80,0xe2,0xeb,0x27,0xb2,0x75,
            0x09,0x83,0x2c,0x1a,0x1b,0x6e,0x5a,0xa0,0x52,0x3b,0xd6,0xb3,0x29,0xe3,0x2f,0x84,
            0x53,0xd1,0x00,0xed,0x20,0xfc,0xb1,0x5b,0x6a,0xcb,0xbe,0x39,0x4a,0x4c,0x58,0xcf,
            0xd0,0xef,0xaa,0xfb,0x43,0x4d,0x33,0x85,0x45,0xf9,0x02,0x7f,0x50,0x3c,0x9f,0xa8,
            0x51,0xa3,0x40,0x8f,0x92,0x9d,0x38,0xf5,0xbc,0xb6,0xda,0x21,0x10,0xff,0xf3,0xd2,
            0xcd,0x0c,0x13,0xec,0x5f,0x97,0x44,0x17,0xc4,0xa7,0x7e,0x3d,0x64,0x5d,0x19,0x73,
            0x60,0x81,0x4f,0xdc,0x22,0x2a,0x90,0x88,0x46,0xee,0xb8,0x14,0xde,0x5e,0x0b,0xdb,
            0xe0,0x32,0x3a,0x0a,0x49,0x06,0x24,0x5c,0xc2,0xd3,0xac,0x62,0x91,0x95,0xe4,0x79,
            0xe7,0xc8,0x37,0x6d,0x8d,0xd5,0x4e,0xa9,0x6c,0x56,0xf4,0xea,0x65,0x7a,0xae,0x08,
            0xba,0x78,0x25,0x2e,0x1c,0xa6,0xb4,0xc6,0xe8,0xdd,0x74,0x1f,0x4b,0xbd,0x8b,0x8a,
            0x70,0x3e,0xb5,0x66,0x48,0x03,0xf6,0x0e,0x61,0x35,0x57,0xb9,0x86,0xc1,0x1d,0x9e,
            0xe1,0xf8,0x98,0x11,0x69,0xd9,0x8e,0x94,0x9b,0x1e,0x87,0xe9,0xce,0x55,0x28,0xdf,
            0x8c,0xa1,0x89,0x0d,0xbf,0xe6,0x42,0x68,0x41,0x99,0x2d,0x0f,0xb0,0x54,0xbb,0x16
    };

    // ── Inverse S-Box ─────────────────────────────────────────────────────
    private static final int[] RSBOX = {
            0x52,0x09,0x6a,0xd5,0x30,0x36,0xa5,0x38,0xbf,0x40,0xa3,0x9e,0x81,0xf3,0xd7,0xfb,
            0x7c,0xe3,0x39,0x82,0x9b,0x2f,0xff,0x87,0x34,0x8e,0x43,0x44,0xc4,0xde,0xe9,0xcb,
            0x54,0x7b,0x94,0x32,0xa6,0xc2,0x23,0x3d,0xee,0x4c,0x95,0x0b,0x42,0xfa,0xc3,0x4e,
            0x08,0x2e,0xa1,0x66,0x28,0xd9,0x24,0xb2,0x76,0x5b,0xa2,0x49,0x6d,0x8b,0xd1,0x25,
            0x72,0xf8,0xf6,0x64,0x86,0x68,0x98,0x16,0xd4,0xa4,0x5c,0xcc,0x5d,0x65,0xb6,0x92,
            0x6c,0x70,0x48,0x50,0xfd,0xed,0xb9,0xda,0x5e,0x15,0x46,0x57,0xa7,0x8d,0x9d,0x84,
            0x90,0xd8,0xab,0x00,0x8c,0xbc,0xd3,0x0a,0xf7,0xe4,0x58,0x05,0xb8,0xb3,0x45,0x06,
            0xd0,0x2c,0x1e,0x8f,0xca,0x3f,0x0f,0x02,0xc1,0xaf,0xbd,0x03,0x01,0x13,0x8a,0x6b,
            0x3a,0x91,0x11,0x41,0x4f,0x67,0xdc,0xea,0x97,0xf2,0xcf,0xce,0xf0,0xb4,0xe6,0x73,
            0x96,0xac,0x74,0x22,0xe7,0xad,0x35,0x85,0xe2,0xf9,0x37,0xe8,0x1c,0x75,0xdf,0x6e,
            0x47,0xf1,0x1a,0x71,0x1d,0x29,0xc5,0x89,0x6f,0xb7,0x62,0x0e,0xaa,0x18,0xbe,0x1b,
            0xfc,0x56,0x3e,0x4b,0xc6,0xd2,0x79,0x20,0x9a,0xdb,0xc0,0xfe,0x78,0xcd,0x5a,0xf4,
            0x1f,0xdd,0xa8,0x33,0x88,0x07,0xc7,0x31,0xb1,0x12,0x10,0x59,0x27,0x80,0xec,0x5f,
            0x60,0x51,0x7f,0xa9,0x19,0xb5,0x4a,0x0d,0x2d,0xe5,0x7a,0x9f,0x93,0xc9,0x9c,0xef,
            0xa0,0xe0,0x3b,0x4d,0xae,0x2a,0xf5,0xb0,0xc8,0xeb,0xbb,0x3c,0x83,0x53,0x99,0x61,
            0x17,0x2b,0x04,0x7e,0xba,0x77,0xd6,0x26,0xe1,0x69,0x14,0x63,0x55,0x21,0x0c,0x7d
    };

    // קבועי סיבוב (Rcon) לתזמון המפתח — אחד לכל סיבוב 1..10
    private static final int[] RCON = {
            0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80, 0x1b, 0x36
    };

    private static final int ROUNDS     = 10;
    private static final int BLOCK_SIZE = 16;

    // 11 מפתחות סיבוב [round][row][col] — עמודה-ראשית כמו מצב AES
    private final byte[][][] roundKeys;

    // =========================================================
    // Constructor
    // =========================================================

    /**
     * @param key  byte[] מ-KeyGenerator (32 בתים).
     *             האלגוריתם משתמש ב-16 הבתים הראשונים (AES-128).
     */
    public PicariaAES(byte[] key) {
        byte[] key128 = new byte[BLOCK_SIZE];
        System.arraycopy(key, 0, key128, 0, Math.min(key.length, BLOCK_SIZE));
        this.roundKeys = expandKey(key128);
    }

    // =========================================================
    // Key Schedule — AES-128
    // =========================================================

    /**
     * מרחיב מפתח של 16 בתים ל-11 מפתחות סיבוב.
     * לוגיקה: 44 words (int), כל 4 words = מפתח סיבוב אחד.
     */
    private byte[][][] expandKey(byte[] key) {
        int[] W = new int[44];

        // 4 words ראשונים = המפתח המקורי
        for (int i = 0; i < 4; i++) {
            W[i] = ((key[4*i]   & 0xFF) << 24) |
                    ((key[4*i+1] & 0xFF) << 16) |
                    ((key[4*i+2] & 0xFF) <<  8) |
                    (key[4*i+3] & 0xFF);
        }

        // 40 words נוספים לפי תזמון מפתח AES-128
        for (int i = 4; i < 44; i++) {
            int temp = W[i - 1];
            if (i % 4 == 0) {
                // RotWord: סיבוב שמאלה של word
                // SubWord: S-Box על כל בית
                // XOR עם Rcon
                temp = subWord(rotWord(temp)) ^ (RCON[i/4 - 1] << 24);
            }
            W[i] = W[i - 4] ^ temp;
        }

        // ארגון ל-11 מטריצות 4×4 (עמודה-ראשית)
        byte[][][] rk = new byte[ROUNDS + 1][4][4];
        for (int r = 0; r <= ROUNDS; r++) {
            for (int c = 0; c < 4; c++) {
                int word = W[r * 4 + c];
                rk[r][0][c] = (byte)((word >> 24) & 0xFF);
                rk[r][1][c] = (byte)((word >> 16) & 0xFF);
                rk[r][2][c] = (byte)((word >>  8) & 0xFF);
                rk[r][3][c] = (byte)( word        & 0xFF);
            }
        }
        return rk;
    }

    private int rotWord(int w) {
        return ((w << 8) | ((w >>> 24) & 0xFF));
    }

    private int subWord(int w) {
        return (SBOX[(w >> 24) & 0xFF] << 24) |
                (SBOX[(w >> 16) & 0xFF] << 16) |
                (SBOX[(w >>  8) & 0xFF] <<  8) |
                SBOX[ w        & 0xFF];
    }

    // =========================================================
    // Encryption — 10 סיבובים
    // =========================================================

    /**
     * מצפין בלוק של 16 בתים.
     * מבנה:  AddRoundKey(0)
     *        × 9 { SubBytes → ShiftRows → MixColumns → AddRoundKey(r) }
     *        SubBytes → ShiftRows → AddRoundKey(10)
     */
    public byte[] encryptBlock(byte[] input) {
        byte[][] state = toState(input);

        addRoundKey(state, roundKeys[0]);                    // סיבוב 0

        for (int round = 1; round < ROUNDS; round++) {
            subBytes(state);                                  // החלפה לא-לינארית
            shiftRows(state);                                 // הזזת שורות
            mixColumns(state);                               // ערבוב עמודות (GF)
            addRoundKey(state, roundKeys[round]);
        }

        subBytes(state);                                      // סיבוב אחרון — ללא MixColumns
        shiftRows(state);
        addRoundKey(state, roundKeys[ROUNDS]);

        return fromState(state);
    }

    // =========================================================
    // Decryption — 10 סיבובים הפוכים
    // =========================================================

    /**
     * מפענח בלוק של 16 בתים.
     * מבנה:  AddRoundKey(10)
     *        × 9 { InvShiftRows → InvSubBytes → AddRoundKey(r) → InvMixColumns }
     *        InvShiftRows → InvSubBytes → AddRoundKey(0)
     */
    public byte[] decryptBlock(byte[] input) {
        byte[][] state = toState(input);

        addRoundKey(state, roundKeys[ROUNDS]);

        for (int round = ROUNDS - 1; round >= 1; round--) {
            invShiftRows(state);
            invSubBytes(state);
            addRoundKey(state, roundKeys[round]);
            invMixColumns(state);
        }

        invShiftRows(state);
        invSubBytes(state);
        addRoundKey(state, roundKeys[0]);

        return fromState(state);
    }

    // =========================================================
    // AES Steps
    // =========================================================

    private void subBytes(byte[][] s) {
        for (int r = 0; r < 4; r++)
            for (int c = 0; c < 4; c++)
                s[r][c] = (byte) SBOX[s[r][c] & 0xFF];
    }

    private void invSubBytes(byte[][] s) {
        for (int r = 0; r < 4; r++)
            for (int c = 0; c < 4; c++)
                s[r][c] = (byte) RSBOX[s[r][c] & 0xFF];
    }

    // שורה i מוזזת שמאלה ב-i מיקומים
    private void shiftRows(byte[][] s) {
        for (int r = 1; r < 4; r++) {
            byte[] row = s[r].clone();
            for (int c = 0; c < 4; c++)
                s[r][c] = row[(c + r) % 4];
        }
    }

    private void invShiftRows(byte[][] s) {
        for (int r = 1; r < 4; r++) {
            byte[] row = s[r].clone();
            for (int c = 0; c < 4; c++)
                s[r][c] = row[(c - r + 4) % 4];
        }
    }

    // מטריצת MixColumns ב-GF(2^8):
    // [2 3 1 1]
    // [1 2 3 1]
    // [1 1 2 3]
    // [3 1 1 2]
    private void mixColumns(byte[][] s) {
        for (int c = 0; c < 4; c++) {
            byte s0 = s[0][c], s1 = s[1][c], s2 = s[2][c], s3 = s[3][c];
            s[0][c] = (byte)(gm(s0,2) ^ gm(s1,3) ^ s2        ^ s3      );
            s[1][c] = (byte)(s0       ^ gm(s1,2) ^ gm(s2,3)  ^ s3      );
            s[2][c] = (byte)(s0       ^ s1       ^ gm(s2,2)  ^ gm(s3,3));
            s[3][c] = (byte)(gm(s0,3) ^ s1       ^ s2        ^ gm(s3,2));
        }
    }

    // מטריצת InvMixColumns ב-GF(2^8):
    // [0e 0b 0d 09]
    // [09 0e 0b 0d]
    // [0d 09 0e 0b]
    // [0b 0d 09 0e]
    private void invMixColumns(byte[][] s) {
        for (int c = 0; c < 4; c++) {
            byte s0 = s[0][c], s1 = s[1][c], s2 = s[2][c], s3 = s[3][c];
            s[0][c] = (byte)(gm(s0,0x0e)^gm(s1,0x0b)^gm(s2,0x0d)^gm(s3,0x09));
            s[1][c] = (byte)(gm(s0,0x09)^gm(s1,0x0e)^gm(s2,0x0b)^gm(s3,0x0d));
            s[2][c] = (byte)(gm(s0,0x0d)^gm(s1,0x09)^gm(s2,0x0e)^gm(s3,0x0b));
            s[3][c] = (byte)(gm(s0,0x0b)^gm(s1,0x0d)^gm(s2,0x09)^gm(s3,0x0e));
        }
    }

    private void addRoundKey(byte[][] s, byte[][] rk) {
        for (int r = 0; r < 4; r++)
            for (int c = 0; c < 4; c++)
                s[r][c] ^= rk[r][c];
    }

    // =========================================================
    // GF(2^8) — כפל בשדה גלואה
    // =========================================================

    /**
     * כפל ב-GF(2^8) עם הפולינום הבלתי-פריק 0x11b (x^8+x^4+x^3+x+1).
     * @param a  בית ראשון
     * @param b  מכפיל (int, תמיד ≤ 0x0e בשימוש של AES)
     */
    private byte gm(byte a, int b) {
        byte p = 0;
        for (int i = 0; i < 8; i++) {
            if ((b & 1) != 0) p ^= a;
            boolean hiBit = (a & 0x80) != 0;
            a = (byte)(a << 1);
            if (hiBit) a ^= 0x1b;     // reduction mod 0x11b
            b >>>= 1;
        }
        return p;
    }

    // =========================================================
    // State ↔ Bytes — עמודה-ראשית (Column-Major)
    // =========================================================

    // state[row][col] = input[col*4 + row]
    private byte[][] toState(byte[] in) {
        byte[][] s = new byte[4][4];
        for (int c = 0; c < 4; c++)
            for (int r = 0; r < 4; r++)
                s[r][c] = in[c * 4 + r];
        return s;
    }

    private byte[] fromState(byte[][] s) {
        byte[] out = new byte[BLOCK_SIZE];
        for (int c = 0; c < 4; c++)
            for (int r = 0; r < 4; r++)
                out[c * 4 + r] = s[r][c];
        return out;
    }

    // =========================================================
    // Print Helpers
    // =========================================================

    public void printRoundKeys() {
        System.out.println("\n  === AES-128 Key Schedule (11 Round Keys) ===");
        for (int r = 0; r <= ROUNDS; r++) {
            System.out.printf("  RK[%2d]: ", r);
            for (int row = 0; row < 4; row++)
                for (int col = 0; col < 4; col++)
                    System.out.printf("%02X", roundKeys[r][row][col] & 0xFF);
            System.out.println();
        }
        System.out.println("  ============================================");
    }

    public static String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) sb.append(String.format("%02X", b & 0xFF));
        return sb.toString();
    }
}
