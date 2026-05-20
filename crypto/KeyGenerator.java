package crypto;

import java.util.List;


public class KeyGenerator {

    private static final long MODULUS   = 36389L;
    private static final long GENERATOR = 1500L;
    private static final int  KEY_BYTES = 32;

    // קבועי ערבוב (בהשראת קבועי SHA / FNV — מספרים ראשוניים גדולים)
    private static final int PRIME_A = 0x9E3779B1;   // יחס  (Knuth)
    private static final int PRIME_B = 0x85EBCA77;
    private static final int PRIME_C = 0xC2B2AE3D;

    private static int functionL(int x) {
        return x * x - 2 * x + 223;
    }



    public static byte[] generateKey(int[][] kruskalMatrix,
                                     int[][] maxFlowMatrix,
                                     int[][] bitwiseMatrix,
                                     List<Integer> eulerStream) {


        int n = kruskalMatrix.length;
        int[] flat = new int[n * n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                flat[i * n + j] = kruskalMatrix[i][j]
                        ^ maxFlowMatrix[i][j]
                        ^ bitwiseMatrix[i][j];
            }
        }



        for (int i = 0; i < eulerStream.size(); i++) {
            int v     = eulerStream.get(i);
            int lv    = functionL(v);
            int mixed = lv ^ (i * 31) ^ (v * 17);


            for (int k = 0; k < flat.length; k++) {
                flat[k] ^= mixed;
                flat[k]  = Integer.rotateLeft(flat[k], (v + k) & 31);
                flat[k] *= PRIME_A;
            }
        }


        byte[] seed = new byte[KEY_BYTES];
        for (int i = 0; i < flat.length; i++) {
            seed[i % KEY_BYTES]            ^= (byte)( flat[i]        & 0xFF);
            seed[(i * 3 + 7) % KEY_BYTES]  ^= (byte)((flat[i] >>  8) & 0xFF);
            seed[(i * 5 + 3) % KEY_BYTES]  ^= (byte)((flat[i] >> 16) & 0xFF);
            seed[(i * 7 + 1) % KEY_BYTES]  ^= (byte)((flat[i] >> 24) & 0xFF);
        }


        diffuse(seed);

        byte[] key = expandWithPRNG(seed);

        diffuse(key);
        diffuse(key);

        return key;
    }


    private static void diffuse(byte[] data) {
        int len = data.length;


        int acc = 0x12345678;


        for (int i = 0; i < len; i++) {
            int b = data[i] & 0xFF;

            acc ^= b;
            acc *= PRIME_A;
            acc  = Integer.rotateLeft(acc, 13);
            acc ^= (acc >>> 7);
            acc *= PRIME_B;

            data[i] = (byte)(acc & 0xFF);
        }


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

        for (int i = 0; i < len / 2; i++) {
            int j = len - 1 - i;
            int x = data[i] & 0xFF;
            int y = data[j] & 0xFF;

            int mixed = (x + y) & 0xFF;
            mixed = ((mixed << 3) | (mixed >>> 5)) & 0xFF;

            data[i] = (byte)(x ^ mixed);
            data[j] = (byte)(y ^ mixed);
        }
    }



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