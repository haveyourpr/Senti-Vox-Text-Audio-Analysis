public final class HashTable128 {
    private long[] hi, lo;
    private byte[] val;      // -1 = empty slot
    private int mask, size;

    public HashTable128(int expected) {
        int cap = 16;
        while (cap < expected * 2) cap <<= 1;
        alloc(cap);
    }

    private void alloc(int cap) {
        hi = new long[cap]; lo = new long[cap]; val = new byte[cap];
        java.util.Arrays.fill(val, (byte) -1);
        mask = cap - 1;
    }

    private static int slot(long h, long l, int mask) {
        long x = h * 0x9E3779B97F4A7C15L ^ l;
        x ^= x >>> 32; x *= 0xD6E8FEB86659FD93L; x ^= x >>> 32;
        return (int) x & mask;
    }

    public void put(long h, long l, int v) {
        if ((size + 1) * 2 > val.length) grow();
        int i = slot(h, l, mask);
        while (val[i] != -1) {
            if (hi[i] == h && lo[i] == l) { val[i] = (byte) v; return; }
            i = (i + 1) & mask;
        }
        hi[i] = h; lo[i] = l; val[i] = (byte) v; size++;
    }

    /** @return stored value, or -1 when the key is absent (the "undefined" case in the TS code). */
    public int get(long h, long l) {
        int i = slot(h, l, mask);
        while (val[i] != -1) {
            if (hi[i] == h && lo[i] == l) return val[i];
            i = (i + 1) & mask;
        }
        return -1;
    }

    public boolean contains(long h, long l) { return get(h, l) != -1; }
    public int size() { return size; }

    private void grow() {
        long[] oh = hi, ol = lo; byte[] ov = val;
        alloc(ov.length << 1);
        size = 0;
        for (int i = 0; i < ov.length; i++) if (ov[i] != -1) put(oh[i], ol[i], ov[i]);
    }
}
