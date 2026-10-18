package com.sentivox.analyzer;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;

public final class SentimentAnalyzer {
    public static final String SENTIMENT_POSITIVE = "Positive";
    public static final String SENTIMENT_NEGATIVE = "Negative";
    public static final String SENTIMENT_NEUTRAL  = "Neutral";
    private static final int POSITIVE_SCORE = 1;
    private static final int NEGATIVE_SCORE = 2;
    private static final int TOP_K = 20;

    private final HashTable128 allWords;    // ALL_WORDS  (hash -> 0/1/2)
    private final HashTable128 stopWords;   // STOP_WORDS (hash set)

    public SentimentAnalyzer(HashTable128 allWords, HashTable128 stopWords) {
        this.allWords = allWords;
        this.stopWords = stopWords;
    }

    /** interface SentimentResult */
    private static final class SentimentResult {
        int positiveScore = 0, negativeScore = 0;
        double polarityScore = 0;
        String sentiment = "";
        final Set<String> positiveWords = new LinkedHashSet<>();
        final Set<String> negativeWords = new LinkedHashSet<>();
        final Set<String> unidentifiedWords = new LinkedHashSet<>();
        final Map<String, Integer> keywords = new LinkedHashMap<>();
    }

    private Map<String, Object> toJsonResult(SentimentResult r) {
        Map<String, Integer> topK = MinHeap.getTopKWords(r.keywords, TOP_K);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put(JsonKeys.positiveScore, r.positiveScore);
        result.put(JsonKeys.negativeScore, r.negativeScore);
        result.put(JsonKeys.polarityScore, r.polarityScore);
        result.put(JsonKeys.sentiment, r.sentiment);
        result.put(JsonKeys.positiveWords, new ArrayList<>(r.positiveWords));
        result.put(JsonKeys.negativeWords, new ArrayList<>(r.negativeWords));
        result.put(JsonKeys.unidentifiedWords, new ArrayList<>(r.unidentifiedWords));
        result.put(JsonKeys.keywords, topK);
        result.put(JsonKeys.wordCloud, WordCloud.generateWordCloud(topK));
        return result;
    }

    private void analyzeSingleComment(String comment, SentimentResult r) {
        for (Token token : Tokenizer.tokenize(comment)) {
            if (!token.tag.equals("word")) continue;
            String lowered = token.value.toLowerCase(Locale.ROOT);
            long[] h = Xxh3.hash128(lowered);

            int sentiment = allWords.get(h[0], h[1]);          // -1 == undefined
            if (sentiment == -1) r.unidentifiedWords.add(lowered);
            else if (sentiment == POSITIVE_SCORE) { r.positiveScore += 1; r.positiveWords.add(lowered); }
            else if (sentiment == NEGATIVE_SCORE) { r.negativeScore += 1; r.negativeWords.add(lowered); }

            if (!stopWords.contains(h[0], h[1])) r.keywords.merge(lowered, 1, Integer::sum);
        }
    }

    private static void prepareResult(SentimentResult r) {
        double polarity = (r.positiveScore - r.negativeScore) / (0.000001 + r.positiveScore + r.negativeScore);
        r.polarityScore = Math.round(polarity * 1000) / 1000.0;     // 3 decimals; Math.round == JS Math.round (half up)
        if (r.polarityScore < 0) r.sentiment = SENTIMENT_NEGATIVE;
        else if (r.polarityScore > 0) r.sentiment = SENTIMENT_POSITIVE;
        else r.sentiment = SENTIMENT_NEUTRAL;
    }

    public Map<String, Object> startSingleAnalysis(String comment) {
        SentimentResult r = new SentimentResult();
        analyzeSingleComment(comment, r);
        prepareResult(r);
        return toJsonResult(r);
    }

    public Map<String, Object> startBatchAnalysis(List<String> comments) {
        SentimentResult r = new SentimentResult();
        for (String c : comments) analyzeSingleComment(c, r);
        prepareResult(r);
        return toJsonResult(r);
    }
}
