package crypto;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * AESFileEncryptor — מצפין ומפענח קבצים באמצעות PicariaAES.
 *
 * מצב הפעולה: AES-128 CBC + PKCS7 padding
 *
 * מבנה קובץ מוצפן:
 *   [16 bytes  IV      ]
 *   [N × 16   ciphertext blocks]
 *
 * CBC (Cipher Block Chaining):
 *   encrypt: xor(plainBlock, prevCipher) → AES_encrypt → cipherBlock
 *   decrypt: AES_decrypt(cipherBlock) → xor(result, prevCipher) → plainBlock
 *
 * ה-IV נגזר מהמפתח עצמו (bytes 0-15 XOR bytes 16-31) כך שאין צורך
 * בהגרלה חיצונית — אותו מפתח תמיד מייצר אותו IV.
 */
public class AESFileEncryptor {

    private static final int BLOCK_SIZE      = 16;
    private static final int VERBOSE_BLOCKS  = 3;   // כמה בלוקים להדפיס בפירוט

    private final PicariaAES aes;
    private final byte[]     iv;

    // =========================================================
    // Constructor
    // =========================================================

    /**
     * @param key  byte[32] מ-KeyGenerator.
     *             16 bytes ראשונים → AES-128 key
     *             IV נגזר: key[0..15] XOR key[16..31]
     */
    public AESFileEncryptor(byte[] key) {
        this.aes = new PicariaAES(key);

        this.iv = new byte[BLOCK_SIZE];
        for (int i = 0; i < BLOCK_SIZE; i++) {
            // XOR בין שני החצאים של המפתח → IV דטרמיניסטי
            iv[i] = (byte)(key[i] ^ key[i + BLOCK_SIZE]);
        }
    }

    // =========================================================
    // Encryption
    // =========================================================

    /**
     * מצפין קובץ.
     * אם הקובץ לא קיים — יוצר קובץ הדגמה ומצפין אותו.
     *
     * @param inputPath   נתיב קובץ קלט (טקסט/בינארי)
     * @param outputPath  נתיב קובץ מוצפן (פלט)
     */
    public void encryptFile(String inputPath, String outputPath) {
        printBanner("AES-128 CBC  —  ENCRYPTION");

        byte[] plaintext = readOrCreateFile(inputPath);
        if (plaintext == null) return;

        System.out.println("  Input  : " + inputPath + "  (" + plaintext.length + " bytes)");
        System.out.println("  Output : " + outputPath);
        System.out.println("  IV     : " + PicariaAES.toHex(iv) + "  (derived from key)");

        // הדפסת מפתחות הסיבוב
        aes.printRoundKeys();

        // PKCS7 padding — מרחיב ל-כפולה של 16
        byte[] padded    = pkcs7Pad(plaintext);
        int    numBlocks = padded.length / BLOCK_SIZE;

        System.out.println("\n  Plaintext  (hex): " + PicariaAES.toHex(plaintext));
        System.out.println("  Padded size     : " + padded.length + " bytes  →  " + numBlocks + " blocks");
        System.out.println("\n  ── Block-by-block encryption ─────────────────────────────────");

        byte[] ciphertext = new byte[padded.length];
        byte[] prev       = iv.clone();   // CBC: מתחיל עם IV

        for (int i = 0; i < numBlocks; i++) {
            byte[] plain = Arrays.copyOfRange(padded, i * BLOCK_SIZE, (i+1) * BLOCK_SIZE);

            // CBC XOR
            byte[] xored     = xorBlocks(plain, prev);
            byte[] encrypted = aes.encryptBlock(xored);

            System.arraycopy(encrypted, 0, ciphertext, i * BLOCK_SIZE, BLOCK_SIZE);
            prev = encrypted;   // CBC: הבלוק המוצפן הנוכחי → קלט לבא

            if (i < VERBOSE_BLOCKS) {
                System.out.printf("  Block %2d │ PT  : %s%n", i, PicariaAES.toHex(plain));
                System.out.printf("           │ XOR : %s%n",     PicariaAES.toHex(xored));
                System.out.printf("           │ CT  : %s%n",     PicariaAES.toHex(encrypted));
                System.out.println("  " + "─".repeat(68));
            } else if (i == VERBOSE_BLOCKS) {
                System.out.println("  ... (remaining " + (numBlocks - VERBOSE_BLOCKS) + " blocks encrypted)");
            }
        }

        // כתיבה: [IV][ciphertext]
        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            fos.write(iv);
            fos.write(ciphertext);
        } catch (IOException e) {
            System.out.println("  ERROR writing: " + e.getMessage());
            return;
        }

        System.out.println("\n  Ciphertext (hex): " + PicariaAES.toHex(ciphertext));
        System.out.println("  Total written   : " + (BLOCK_SIZE + ciphertext.length) + " bytes  (IV + ciphertext)");
        printFooter("ENCRYPTION COMPLETE  →  " + outputPath);
    }

    // =========================================================
    // Decryption
    // =========================================================

    /**
     * מפענח קובץ שהוצפן על-ידי encryptFile.
     *
     * @param inputPath   נתיב קובץ מוצפן
     * @param outputPath  נתיב קובץ מפוענח (פלט)
     */
    public void decryptFile(String inputPath, String outputPath) {
        printBanner("AES-128 CBC  —  DECRYPTION");

        byte[] fileData = readFile(inputPath);
        if (fileData == null || fileData.length < BLOCK_SIZE * 2) {
            System.out.println("  ERROR: file too small or missing.");
            return;
        }

        // פיצול IV + ciphertext
        byte[] fileIv     = Arrays.copyOfRange(fileData, 0, BLOCK_SIZE);
        byte[] ciphertext = Arrays.copyOfRange(fileData, BLOCK_SIZE, fileData.length);
        int    numBlocks  = ciphertext.length / BLOCK_SIZE;

        System.out.println("  Input  : " + inputPath + "  (" + fileData.length + " bytes)");
        System.out.println("  Output : " + outputPath);
        System.out.println("  IV     : " + PicariaAES.toHex(fileIv));
        System.out.println("  Blocks : " + numBlocks);
        System.out.println("\n  ── Block-by-block decryption ─────────────────────────────────");

        byte[] plaintext = new byte[ciphertext.length];
        byte[] prev      = fileIv;

        for (int i = 0; i < numBlocks; i++) {
            byte[] cipher    = Arrays.copyOfRange(ciphertext, i * BLOCK_SIZE, (i+1) * BLOCK_SIZE);
            byte[] decrypted = aes.decryptBlock(cipher);
            byte[] plain     = xorBlocks(decrypted, prev);

            System.arraycopy(plain, 0, plaintext, i * BLOCK_SIZE, BLOCK_SIZE);
            prev = cipher;   // CBC: ciphertext הנוכחי → XOR לבא

            if (i < VERBOSE_BLOCKS) {
                System.out.printf("  Block %2d │ CT  : %s%n", i, PicariaAES.toHex(cipher));
                System.out.printf("           │ PT  : %s%n",     PicariaAES.toHex(plain));
                System.out.println("  " + "─".repeat(68));
            } else if (i == VERBOSE_BLOCKS) {
                System.out.println("  ... (remaining blocks decrypted)");
            }
        }

        // הסרת PKCS7 padding
        byte[] unpadded = pkcs7Unpad(plaintext);

        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            fos.write(unpadded);
        } catch (IOException e) {
            System.out.println("  ERROR writing: " + e.getMessage());
            return;
        }

        System.out.println("\n  Recovered text:");
        System.out.println("  ┌─────────────────────────────────────────────────────────────┐");
        for (String line : new String(unpadded).split("\n")) {
            System.out.printf("  │ %-63s│%n", line);
        }
        System.out.println("  └─────────────────────────────────────────────────────────────┘");
        printFooter("DECRYPTION COMPLETE  →  " + outputPath);
    }

    // =========================================================
    // Helpers
    // =========================================================

    /** PKCS7: ממלא ב-N בתים שכל אחד שווה N, כך שהאורך יתחלק ב-16. */
    private byte[] pkcs7Pad(byte[] data) {
        int padLen = BLOCK_SIZE - (data.length % BLOCK_SIZE);
        byte[] padded = new byte[data.length + padLen];
        System.arraycopy(data, 0, padded, 0, data.length);
        for (int i = data.length; i < padded.length; i++)
            padded[i] = (byte) padLen;
        return padded;
    }

    /** מסיר PKCS7 padding. */
    private byte[] pkcs7Unpad(byte[] data) {
        if (data.length == 0) return data;
        int padLen = data[data.length - 1] & 0xFF;
        if (padLen < 1 || padLen > BLOCK_SIZE) return data;
        return Arrays.copyOfRange(data, 0, data.length - padLen);
    }

    /** XOR בין שני בלוקים של BLOCK_SIZE בתים. */
    private byte[] xorBlocks(byte[] a, byte[] b) {
        byte[] result = new byte[BLOCK_SIZE];
        for (int i = 0; i < BLOCK_SIZE; i++)
            result[i] = (byte)(a[i] ^ b[i]);
        return result;
    }

    /** קורא קובץ, ואם לא קיים — יוצר קובץ הדגמה. */
    private byte[] readOrCreateFile(String path) {
        File f = new File(path);
        if (!f.exists()) {
            System.out.println("  File not found. Creating demo file: " + path);
            String demo =
                    "Picaria Encryption System\n" +
                            "==========================\n" +
                            "This file was encrypted using a key derived from the Picaria board game.\n" +
                            "Key generation pipeline:\n" +
                            "  1. Kruskal MST     -> 13x13 weight matrix (Floyd-Warshall)\n" +
                            "  2. MaxFlow (E-K)   -> 13x13 weight matrix (Floyd-Warshall)\n" +
                            "  3. Bitwise+Dijkstra-> 13x13 weight matrix (Floyd-Warshall)\n" +
                            "  4. Euler (Hierholzer) -> key stream sequence\n" +
                            "  5. XOR + polynomial mixing + PRNG expansion -> 256-bit key\n" +
                            "  6. AES-128 CBC with this key.\n";
            try (FileWriter fw = new FileWriter(f)) {
                fw.write(demo);
            } catch (IOException e) {
                System.out.println("  ERROR creating demo file: " + e.getMessage());
                return null;
            }
        }
        return readFile(path);
    }

    private byte[] readFile(String path) {
        try {
            return Files.readAllBytes(Paths.get(path));
        } catch (IOException e) {
            System.out.println("  ERROR reading '" + path + "': " + e.getMessage());
            return null;
        }
    }

    // =========================================================
    // Print Helpers
    // =========================================================

    private void printBanner(String title) {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════╗");
        System.out.printf( "║  %-66s║%n", "  " + title);
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
    }

    private void printFooter(String msg) {
        System.out.println("══════════════════════════════════════════════════════════════════════");
        System.out.println("  ✓ " + msg);
        System.out.println("══════════════════════════════════════════════════════════════════════\n");
    }
}
