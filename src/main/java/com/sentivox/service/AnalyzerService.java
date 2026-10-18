package com.sentivox.service;

import com.sentivox.analyzer.DataLoader;
import com.sentivox.analyzer.HashTable128;
import com.sentivox.analyzer.SentimentAnalyzer;
import com.sentivox.analyzer.Token;
import com.sentivox.analyzer.Tokenizer;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Value;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Loads the context-free datasets once at startup (port of data-loader.ts) and runs the analysis.
 *
 * The URL/e-mail regexes in {@link Tokenizer} recurse in java.util.regex, so long inputs need a big stack
 * (see analyzer/Main.java). Work is therefore run on a dedicated pool of large-stack threads instead of the
 * Netty event loop.
 */
@Context
public class AnalyzerService {
    private static final Logger LOG = LoggerFactory.getLogger(AnalyzerService.class);

    private final SentimentAnalyzer analyzer;
    private final ExecutorService executor;

    public AnalyzerService(@Value("${sentivox.analyzer.threads:0}") int threads,
                           @Value("${sentivox.analyzer.thread-stack-size-mb:512}") long stackSizeMb) throws IOException {
        long start = System.nanoTime();
        HashTable128 allWords = DataLoader.loadSentimentData(DataLoader.readDataset("cf-data.json"));
        HashTable128 stopWords = DataLoader.loadStopWords(DataLoader.readDataset("stopwords.ts"));
        this.analyzer = new SentimentAnalyzer(allWords, stopWords);
        LOG.info("Loaded {} sentiment words and {} stop words in {} ms",
                allWords.size(), stopWords.size(), (System.nanoTime() - start) / 1_000_000);

        int poolSize = threads > 0 ? threads : Runtime.getRuntime().availableProcessors();
        long stackSize = stackSizeMb * 1024 * 1024;
        AtomicInteger counter = new AtomicInteger();
        this.executor = Executors.newFixedThreadPool(poolSize, task -> {
            Thread t = new Thread(null, task, "analyzer-" + counter.incrementAndGet(), stackSize);
            t.setDaemon(true);
            return t;
        });
    }

    public CompletableFuture<Map<String, Object>> startSingleAnalysis(String comment) {
        return CompletableFuture.supplyAsync(() -> analyzer.startSingleAnalysis(comment), executor);
    }

    public CompletableFuture<Map<String, Object>> startBatchAnalysis(List<String> comments) {
        return CompletableFuture.supplyAsync(() -> analyzer.startBatchAnalysis(comments), executor);
    }

    public CompletableFuture<List<Token>> tokenize(String text) {
        return CompletableFuture.supplyAsync(() -> Tokenizer.tokenize(text), executor);
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}
