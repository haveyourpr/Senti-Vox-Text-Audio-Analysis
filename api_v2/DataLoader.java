import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads the datasets at runtime (port of data-loader.ts + the STOP_WORDS import).
 *   dataset/cf-data.json    -> {"<uint128 decimal>": 0|1|2, ...}  (0 neutral, 1 positive, 2 negative)
 *   dataset/stopwords.ts    -> new Set<bigint>([123n, 456n, ...])  (numbers are parsed straight out of the .ts file)
 */
public final class DataLoader {
    private DataLoader() {}

    /** Finds the dataset directory: -Ddataset.dir=..., ./dataset, ./api_v2/dataset, or next to the class files. */
    public static Path resolveDatasetDir() {
        String prop = System.getProperty("dataset.dir");
        if (prop != null && !prop.isEmpty()) return Paths.get(prop);
        Path[] candidates = { Paths.get("dataset"), Paths.get("api_v2", "dataset") };
        for (Path c : candidates) if (Files.isRegularFile(c.resolve("cf-data.json"))) return c;
        try {
            Path here = Paths.get(DataLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path c = here.resolve("dataset");
            if (Files.isRegularFile(c.resolve("cf-data.json"))) return c;
        } catch (Exception ignored) { }
        return candidates[0];
    }

    /** Parses {"<decimal key>": <digit>, ...} without any JSON library (fast, ~1.4M entries). */
    public static HashTable128 loadSentimentData(Path file) throws IOException {
        byte[] b = Files.readAllBytes(file);
        HashTable128 table = new HashTable128(Math.max(16, b.length / 48));
        int i = 0, n = b.length;
        while (i < n) {
            while (i < n && b[i] != '"') i++;
            if (i >= n) break;
            i++;
            long hi = 0, lo = 0;
            while (i < n && b[i] != '"') {
                int d = b[i] - '0';
                if (d < 0 || d > 9) throw new IOException("Bad character in key near byte " + i);
                long carry = Math.multiplyHigh(lo, 10) + ((lo >> 63) & 10);   // unsigned high(lo*10)
                long m = lo * 10, nl = m + d;
                if (Long.compareUnsigned(nl, m) < 0) carry++;
                hi = hi * 10 + carry;
                lo = nl;
                i++;
            }
            i++;                                    // closing quote
            while (i < n && b[i] != ':') i++;
            i++;
            while (i < n && (b[i] == ' ' || b[i] == '\t' || b[i] == '\r' || b[i] == '\n')) i++;
            int v = 0;
            while (i < n && b[i] >= '0' && b[i] <= '9') { v = v * 10 + (b[i] - '0'); i++; }
            table.put(hi, lo, v);
        }
        return table;
    }

    /** Extracts every `<digits>n` BigInt literal from stopwords.ts. */
    public static HashTable128 loadStopWords(Path file) throws IOException {
        String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        HashTable128 set = new HashTable128(2048);
        Matcher m = Pattern.compile("(?<![\\w.])(\\d+)n\\b").matcher(text);
        while (m.find()) {
            BigInteger v = new BigInteger(m.group(1));
            set.put(v.shiftRight(64).longValue(), v.longValue(), 1);
        }
        return set;
    }
}
