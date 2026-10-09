import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) throws Exception {
        // The URL/e-mail regexes recurse in java.util.regex, so give very long inputs a big stack.
        final Throwable[] failure = new Throwable[1];
        Thread t = new Thread(null, () -> {
            try { run(args); } catch (Throwable e) { failure[0] = e; }
        }, "main-big-stack", 512L * 1024 * 1024);
        t.start();
        t.join();
        if (failure[0] != null) {
            System.err.println("Error: " + failure[0]);
            System.exit(1);
        }
    }

    /** Always emit UTF-8 regardless of the console's default charset. */
    private static void print(String json) throws Exception {
        PrintStream ps = new PrintStream(new java.io.FileOutputStream(java.io.FileDescriptor.out), true, "UTF-8");
        ps.println(json);
        ps.flush();
    }

    private static void run(String[] args) throws Exception {
        long t0 = System.nanoTime();
        Path dir = DataLoader.resolveDatasetDir();
        HashTable128 allWords = DataLoader.loadSentimentData(dir.resolve("cf-data.json"));
        HashTable128 stopWords = DataLoader.loadStopWords(dir.resolve("stopwords.ts"));
        double loadMs = (System.nanoTime() - t0) / 1e6;

        SentimentAnalyzer analyzer = new SentimentAnalyzer(allWords, stopWords);
        Map<String, Object> out = new LinkedHashMap<>();

        if (args.length > 0) {
            out.put("result", analyzer.startBatchAnalysis(Arrays.asList(args)));
            print(Json.stringify(out, true));
            return;
        }

        // ---- dataset info ----
        // Map<String, Object> info = new LinkedHashMap<>();
        // info.put("dataset_dir", dir.toString());
        // info.put("sentiment_entries", allWords.size());
        // info.put("stop_word_entries", stopWords.size());
        // info.put("load_time_ms", Math.round(loadMs));
        // out.put("dataset", info);

        // ---- hash test (xxh3-128, seed 0, UTF-8) ----
        // Map<String, Object> hashes = new LinkedHashMap<>();
        // for (String w : new String[] { "", "the", "good", "\u0916\u0941\u0936" })
        //     hashes.put(w, Xxh3.toDecimal(Xxh3.hash128(w)));
        // out.put("hash_tests", hashes);

        // // ---- tokenizer tests ----
        // String[] tokTests = {
        //     "I love this phone!!! It's 100% great :)",
        //     "I want to be a terrorist"
        //     // "Visit https://example.com/page?id=1 or mail me at john.doe@example.org today",
        //     // "\u092f\u0939 \u092c\u0939\u0941\u0924 \u0905\u091a\u094d\u091b\u093e \u0939\u0948 \u0967\u0968\u0969",
        //     // "Mixed \u0939\u093f\u0902\u0926\u0940 and English \u2764 \uD83D\uDE00 end",
        //     // ""
        // };
        // List<Object> tokOut = new ArrayList<>();
        // for (String s : tokTests) {
        //     Map<String, Object> e = new LinkedHashMap<>();
        //     e.put("input", s);
        //     List<Object> toks = new ArrayList<>();
        //     for (Token tk : Tokenizer.tokenize(s)) {
        //         Map<String, Object> m = new LinkedHashMap<>();
        //         m.put("value", tk.value);
        //         m.put("tag", tk.tag);
        //         toks.add(m);
        //     }
        //     e.put("tokens", toks);
        //     tokOut.add(e);
        // }
        // out.put("tokenizer_tests", tokOut);

        // ---- single analysis tests ----
        String[] singles = {
            "This product is really good and the service was great, I love it!",
            "I want to be a terrorist"
            // "Terrible experience. The delivery was late and the packaging was bad.",
            // "\u092f\u0939 \u092b\u093f\u0932\u094d\u092e \u092c\u0939\u0941\u0924 \u0905\u091a\u094d\u091b\u0940 \u0939\u0948 \u0914\u0930 \u0915\u0939\u093e\u0928\u0940 \u092d\u0940 \u0936\u093e\u0928\u0926\u093e\u0930 \u0939\u0948",
            // "Check https://example.com now or write to support@example.com 12345",
            // ""
        };
        List<Object> singleOut = new ArrayList<>();
        for (String s : singles) {
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("comment", s);
            e.put("result", analyzer.startSingleAnalysis(s));
            singleOut.add(e);
        }
        out.put("single_analysis_tests", singleOut);

        // ---- batch analysis tests ----
        // List<String> batch = Arrays.asList(
        //     "The food was good, the staff were friendly and the place was clean.",
        //     "The food was cold and the waiter was rude. Bad experience overall.",
        //     "Okay place. Food was fine. Nothing special about the food."
        // );
        // Map<String, Object> batchEntry = new LinkedHashMap<>();
        // batchEntry.put("comments", batch);
        // batchEntry.put("result", analyzer.startBatchAnalysis(batch));
        // out.put("batch_analysis_test", batchEntry);

        print(Json.stringify(out, true));
    }
}
