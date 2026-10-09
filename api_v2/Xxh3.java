import java.nio.charset.StandardCharsets;

/**
 * XXH3 128-bit (seed = 0, default secret) implemented from scratch.
 * Equivalent to XXH3_128(Buffer.from(str), 0n) from the "xxh3-ts" package.
 * Result is returned as two unsigned 64-bit halves (hi, lo); the 128-bit
 * value is (hi << 64) | lo.
 */
public final class Xxh3 {
    private Xxh3() {}

    private static final long P32_1 = 0x9E3779B1L, P32_2 = 0x85EBCA77L, P32_3 = 0xC2B2AE3DL;
    private static final long P64_1 = 0x9E3779B185EBCA87L, P64_2 = 0xC2B2AE3D27D4EB4FL,
            P64_3 = 0x165667B19E3779F9L, P64_4 = 0x85EBCA77C2B2AE63L, P64_5 = 0x27D4EB2F165667C5L;
    private static final long PRIME_MX1 = 0x165667919E3779F9L, PRIME_MX2 = 0x9FB21C651E98DF25L;

    // The canonical 192-byte kSecret from the XXH3 specification.
    private static final byte[] SECRET = toBytes(new int[] {
        0xb8,0xfe,0x6c,0x39,0x23,0xa4,0x4b,0xbe,0x7c,0x01,0x81,0x2c,0xf7,0x21,0xad,0x1c,
        0xde,0xd4,0x6d,0xe9,0x83,0x90,0x97,0xdb,0x72,0x40,0xa4,0xa4,0xb7,0xb3,0x67,0x1f,
        0xcb,0x79,0xe6,0x4e,0xcc,0xc0,0xe5,0x78,0x82,0x5a,0xd0,0x7d,0xcc,0xff,0x72,0x21,
        0xb8,0x08,0x46,0x74,0xf7,0x43,0x24,0x8e,0xe0,0x35,0x90,0xe6,0x81,0x3a,0x26,0x4c,
        0x3c,0x28,0x52,0xbb,0x91,0xc3,0x00,0xcb,0x88,0xd0,0x65,0x8b,0x1b,0x53,0x2e,0xa3,
        0x71,0x64,0x48,0x97,0xa2,0x0d,0xf9,0x4e,0x38,0x19,0xef,0x46,0xa9,0xde,0xac,0xd8,
        0xa8,0xfa,0x76,0x3f,0xe3,0x9c,0x34,0x3f,0xf9,0xdc,0xbb,0xc7,0xc7,0x0b,0x4f,0x1d,
        0x8a,0x51,0xe0,0x4b,0xcd,0xb4,0x59,0x31,0xc8,0x9f,0x7e,0xc9,0xd9,0x78,0x73,0x64,
        0xea,0xc5,0xac,0x83,0x34,0xd3,0xeb,0xc3,0xc5,0x81,0xa0,0xff,0xfa,0x13,0x63,0xeb,
        0x17,0x0d,0xdd,0x51,0xb7,0xf0,0xda,0x49,0xd3,0x16,0x55,0x26,0x29,0xd4,0x68,0x9e,
        0x2b,0x16,0xbe,0x58,0x7d,0x47,0xa1,0xfc,0x8f,0xf8,0xb8,0xd1,0x7a,0xd0,0x31,0xce,
        0x45,0xcb,0x3a,0x8f,0x95,0x16,0x04,0x28,0xaf,0xd7,0xfb,0xca,0xbb,0x4b,0x40,0x7e
    });
    private static byte[] toBytes(int[] a) { byte[] b = new byte[a.length]; for (int i = 0; i < a.length; i++) b[i] = (byte) a[i]; return b; }

    private static long r64(byte[] b, int o) {
        return (b[o] & 0xffL) | (b[o+1] & 0xffL) << 8 | (b[o+2] & 0xffL) << 16 | (b[o+3] & 0xffL) << 24
             | (b[o+4] & 0xffL) << 32 | (b[o+5] & 0xffL) << 40 | (b[o+6] & 0xffL) << 48 | (b[o+7] & 0xffL) << 56;
    }
    private static long r32(byte[] b, int o) {
        return (b[o] & 0xffL) | (b[o+1] & 0xffL) << 8 | (b[o+2] & 0xffL) << 16 | (b[o+3] & 0xffL) << 24;
    }

    private static long xorshift(long v, int s) { return v ^ (v >>> s); }

    private static long avalanche(long h) {
        h = xorshift(h, 37); h *= PRIME_MX1; return xorshift(h, 32);
    }
    private static long xxh64Avalanche(long h) {
        h ^= h >>> 33; h *= P64_2; h ^= h >>> 29; h *= P64_3; return h ^ (h >>> 32);
    }
    private static long rrmxmx(long h, long len) {
        h ^= Long.rotateLeft(h, 49) ^ Long.rotateLeft(h, 24);
        h *= PRIME_MX2; h ^= (h >>> 35) + len; h *= PRIME_MX2; return xorshift(h, 28);
    }

    /** returns {hi, lo} of 64x64 -> 128 unsigned multiply */
    private static long mulHi(long a, long b) { return Math.multiplyHigh(a, b) + ((a >> 63) & b) + ((b >> 63) & a); }

    private static long mul128Fold64(long a, long b) { return (a * b) ^ mulHi(a, b); }

    private static long mix16(byte[] in, int io, int so, long seed) {
        long lo = r64(in, io), hi = r64(in, io + 8);
        return mul128Fold64(lo ^ (r64(SECRET, so) + seed), hi ^ (r64(SECRET, so + 8) - seed));
    }

    /** Hash UTF-8 bytes of the string. Returns {hi, lo}. */
    public static long[] hash128(String s) { return hash128(s.getBytes(StandardCharsets.UTF_8)); }

    public static long[] hash128(byte[] in) {
        int len = in.length;
        long hi, lo;
        if (len <= 16) {
            if (len > 8) {            // 9..16
                long bitflipL = r64(SECRET, 32) ^ r64(SECRET, 40);
                long bitflipH = r64(SECRET, 48) ^ r64(SECRET, 56);
                long inLo = r64(in, 0), inHi = r64(in, len - 8);
                long x = inLo ^ inHi ^ bitflipL;
                long mHi = mulHi(x, P64_1), mLo = x * P64_1;
                mLo += (long) (len - 1) << 54;
                inHi ^= bitflipH;
                mHi += inHi + (inHi & 0xFFFFFFFFL) * (P32_2 - 1);
                mLo ^= Long.reverseBytes(mHi);
                long hHi = mulHi(mLo, P64_2), hLo = mLo * P64_2;
                hHi += mHi * P64_2;
                lo = avalanche(hLo); hi = avalanche(hHi);
            } else if (len >= 4) {    // 4..8
                long in1 = r32(in, 0), in2 = r32(in, len - 4);
                long bitflip = (r64(SECRET, 16) ^ r64(SECRET, 24));
                long keyed = (in1 + (in2 << 32)) ^ bitflip;
                long mHi = mulHi(keyed, P64_1 + ((long) len << 2));
                long mLo = keyed * (P64_1 + ((long) len << 2));
                mHi += mLo << 1;
                mLo ^= mHi >>> 3;
                mLo = xorshift(mLo, 35); mLo *= PRIME_MX2; mLo = xorshift(mLo, 28);
                mHi = avalanche(mHi);
                lo = mLo; hi = mHi;
            } else if (len > 0) {     // 1..3
                int c1 = in[0] & 0xff, c2 = in[len >> 1] & 0xff, c3 = in[len - 1] & 0xff;
                long combinedL = ((long) c1 << 16) | ((long) c2 << 24) | c3 | ((long) len << 8);
                long combinedH = Integer.rotateLeft(Integer.reverseBytes((int) combinedL), 13) & 0xffffffffL;
                long bitflipL = (r32(SECRET, 0) ^ r32(SECRET, 4));
                long bitflipH = (r32(SECRET, 8) ^ r32(SECRET, 12));
                lo = xxh64Avalanche((combinedL ^ bitflipL) & 0xffffffffL);
                hi = xxh64Avalanche((combinedH ^ bitflipH) & 0xffffffffL);
            } else {
                lo = xxh64Avalanche(r64(SECRET, 64) ^ r64(SECRET, 72));
                hi = xxh64Avalanche(r64(SECRET, 80) ^ r64(SECRET, 88));
            }
        } else if (len <= 128) {
            long accLo = (long) len * P64_1, accHi = 0;
            if (len > 32) {
                if (len > 64) {
                    if (len > 96) {
                        long[] r = mix32(accLo, accHi, in, 48, in, len - 64, 96); accLo = r[0]; accHi = r[1];
                    }
                    long[] r = mix32(accLo, accHi, in, 32, in, len - 48, 64); accLo = r[0]; accHi = r[1];
                }
                long[] r = mix32(accLo, accHi, in, 16, in, len - 32, 32); accLo = r[0]; accHi = r[1];
            }
            long[] r = mix32(accLo, accHi, in, 0, in, len - 16, 0); accLo = r[0]; accHi = r[1];
            return finishMid(accLo, accHi, len);
        } else if (len <= 240) {
            long accLo = (long) len * P64_1, accHi = 0;
            int nbRounds = len / 32;
            for (int i = 0; i < 4; i++) {
                long[] r = mix32(accLo, accHi, in, 32 * i, in, 32 * i + 16, 32 * i); accLo = r[0]; accHi = r[1];
            }
            accLo = avalanche(accLo); accHi = avalanche(accHi);
            for (int i = 4; i < nbRounds; i++) {
                long[] r = mix32(accLo, accHi, in, 32 * i, in, 32 * i + 16, 3 + 32 * (i - 4)); accLo = r[0]; accHi = r[1];
            }
            long[] r = mix32(accLo, accHi, in, len - 16, in, len - 32, 136 - 17 - 16);
            return finishMid(r[0], r[1], len);
        } else {
            return longHash(in, len);
        }
        return new long[] { hi, lo };
    }

    // XXH128_mix32B: acc.low += mix16(in1, secret+so); acc.low ^= in2.lo + in2.hi;
    //                acc.high += mix16(in2, secret+so+16); acc.high ^= in1.lo + in1.hi
    private static long[] mix32(long accLo, long accHi, byte[] a, int ao, byte[] b, int bo, int so) {
        accLo += mix16(a, ao, so, 0);
        accLo ^= r64(b, bo) + r64(b, bo + 8);
        accHi += mix16(b, bo, so + 16, 0);
        accHi ^= r64(a, ao) + r64(a, ao + 8);
        return new long[] { accLo, accHi };
    }

    private static long[] finishMid(long accLo, long accHi, int len) {
        long lo = accLo + accHi;
        long hi = (accLo * P64_1) + (accHi * P64_4) + ((long) len * P64_2);
        lo = avalanche(lo);
        hi = -avalanche(hi);
        return new long[] { hi, lo };
    }

    // ---- long input (> 240 bytes) ----
    private static final int STRIPE = 64, SECRET_CONSUME = 8, ACC_NB = 8;
    private static final int SECRET_SIZE = 192;
    private static long[] longHash(byte[] in, int len) {
        long[] acc = { P32_3, P64_1, P64_2, P64_3, P64_4, P32_2, P64_5, P32_1 };
        int nbStripesPerBlock = (SECRET_SIZE - STRIPE) / SECRET_CONSUME;      // 16
        int blockLen = STRIPE * nbStripesPerBlock;                            // 1024
        int nbBlocks = (len - 1) / blockLen;
        for (int n = 0; n < nbBlocks; n++) {
            for (int s = 0; s < nbStripesPerBlock; s++) accumulate512(acc, in, n * blockLen + s * STRIPE, s * SECRET_CONSUME);
            scramble(acc, SECRET_SIZE - STRIPE);
        }
        int nbStripes = ((len - 1) - blockLen * nbBlocks) / STRIPE;
        for (int s = 0; s < nbStripes; s++) accumulate512(acc, in, nbBlocks * blockLen + s * STRIPE, s * SECRET_CONSUME);
        accumulate512Last(acc, in, len);
        long lo = mergeAccs(acc, 11, (long) len * P64_1);
        long hi = mergeAccs(acc, SECRET_SIZE - 64 - 11, ~((long) len * P64_2));
        return new long[] { hi, lo };
    }
    private static void accumulate512Last(long[] acc, byte[] in, int len) {
        accumulate512(acc, in, len - STRIPE, SECRET_SIZE - STRIPE - 7);
    }
    private static void accumulate512(long[] acc, byte[] in, int io, int so) {
        for (int i = 0; i < ACC_NB; i++) {
            long dv = r64(in, io + 8 * i);
            long dk = dv ^ r64(SECRET, so + 8 * i);
            acc[i ^ 1] += dv;
            acc[i] += (dk & 0xffffffffL) * (dk >>> 32);
        }
    }
    private static void scramble(long[] acc, int so) {
        for (int i = 0; i < ACC_NB; i++) {
            long a = acc[i];
            a = xorshift(a, 47); a ^= r64(SECRET, so + 8 * i); a *= P32_1;
            acc[i] = a;
        }
    }
    private static long mergeAccs(long[] acc, int so, long start) {
        long r = start;
        for (int i = 0; i < 4; i++)
            r += mul128Fold64(acc[2 * i] ^ r64(SECRET, so + 16 * i), acc[2 * i + 1] ^ r64(SECRET, so + 16 * i + 8));
        return avalanche(r);
    }

    /** Convenience: 128-bit value as unsigned decimal string (same as BigInt result in TS). */
    public static String toDecimal(long[] h) {
        return new java.math.BigInteger(1, new byte[] {
            (byte)(h[0]>>>56),(byte)(h[0]>>>48),(byte)(h[0]>>>40),(byte)(h[0]>>>32),(byte)(h[0]>>>24),(byte)(h[0]>>>16),(byte)(h[0]>>>8),(byte)h[0],
            (byte)(h[1]>>>56),(byte)(h[1]>>>48),(byte)(h[1]>>>40),(byte)(h[1]>>>32),(byte)(h[1]>>>24),(byte)(h[1]>>>16),(byte)(h[1]>>>8),(byte)h[1]
        }).toString();
    }
}
