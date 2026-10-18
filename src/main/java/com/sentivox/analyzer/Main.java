package com.sentivox.analyzer;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

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
        ps.close();
    }

    private static void run(String[] args) throws Exception {
        HashTable128 allWords = DataLoader.loadSentimentData(DataLoader.readDataset("cf-data.json"));
        HashTable128 stopWords = DataLoader.loadStopWords(DataLoader.readDataset("stopwords.ts"));

        SentimentAnalyzer analyzer = new SentimentAnalyzer(allWords, stopWords);
        Map<String, Object> out = new LinkedHashMap<>();

        if (args.length > 0) {
            out.put("result", analyzer.startBatchAnalysis(Arrays.asList(args)));
            print(Json.stringify(out, true));
            return;
        }

        System.out.print("\n ==> Enter the string :- ");
        Scanner sc = new Scanner(System.in);
        String user_input = sc.nextLine();
        sc.close();
        
        print(Json.stringify(analyzer.startSingleAnalysis(user_input), true));
    }
}
