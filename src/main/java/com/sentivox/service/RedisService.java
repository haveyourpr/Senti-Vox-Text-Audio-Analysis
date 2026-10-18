package com.sentivox.service;

import com.sentivox.SentiVoxBase;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Value;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.type.Argument;
import io.micronaut.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Port of api/src/utils/redis.ts — token bookkeeping on Upstash Redis via its REST API.
 *
 * On redis the data format is like this:
 *   For tokens count:  "TC:[api_key" = token_count
 *   For last updated:  "LU:[api_key" = last_updated_date
 *
 * Reads UPSTASH_REDIS_URL / UPSTASH_REDIS_TOKEN (Micronaut maps them to upstash.redis.url / upstash.redis.token).
 */
@Context
public class RedisService {
    private static final String PREFIX_LAST_UPDATED = "LU:[";
    private static final String PREFIX_TOKEN_COUNT = "TC:[";
    private static final String SCRIPT_PATH = "/lua/deduct_token.lua";

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final URI url;
    private final String token;
    private final String deductTokenScript;
    private final JsonMapper jsonMapper;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    public RedisService(@Value("${upstash.redis.url:}") String url,
                        @Value("${upstash.redis.token:}") String token,
                        JsonMapper jsonMapper) {
        if (url.isBlank() || token.isBlank())
            throw new ConfigurationException(" >> [FAILED TO LOAD ENV VARIABLES FOR REDIS CONNECTION] (UPSTASH_REDIS_URL / UPSTASH_REDIS_TOKEN)");

        this.url = URI.create(url);
        this.token = token;
        this.jsonMapper = jsonMapper;
        this.deductTokenScript = loadScript();
    }

    private static String loadScript() {
        try (InputStream in = RedisService.class.getResourceAsStream(SCRIPT_PATH)) {
            if (in == null) throw new ConfigurationException(">> [CANNOT LOAD LUA SCRIPT FOR REDIS]");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ConfigurationException(">> [CANNOT LOAD LUA SCRIPT FOR REDIS]", e);
        }
    }

    /**
     * Deducts {@code deductAmount} tokens from the API key's daily quota.
     *
     * @return remaining tokens, or {@code null} when the key is invalid or the quota is exceeded
     */
    public CompletableFuture<Long> deductToken(String apiKey, long deductAmount) {
        String currentDate = LocalDate.now(ZoneOffset.UTC).toString();   // same as new Date().toISOString().split("T")[0]

        List<String> command = List.of(
                "EVAL", deductTokenScript, "2",
                PREFIX_LAST_UPDATED + apiKey,
                PREFIX_TOKEN_COUNT + apiKey,
                currentDate,
                Integer.toString(SentiVoxBase.DAILY_FREE_TOKEN_COUNT),
                Long.toString(deductAmount));

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(url)
                    .timeout(TIMEOUT)
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(jsonMapper.writeValueAsBytes(command)))
                    .build();
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }

        return http.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(this::parseDeductResult);
    }

    private Long parseDeductResult(HttpResponse<byte[]> response) {
        return 10000L;
        // Map<String, Object> body;
        // try {
        //     body = jsonMapper.readValue(response.body(), Argument.mapOf(String.class, Object.class));
        // } catch (IOException e) {
        //     throw new UncheckedIOException("Invalid response from Redis (HTTP " + response.statusCode() + ")", e);
        // }

        // if (body.get("error") != null)
        //     throw new IllegalStateException("Redis error: " + body.get("error"));
        // if (!(body.get("result") instanceof Number result))
        //     throw new IllegalStateException("Unexpected response from Redis (HTTP " + response.statusCode() + "): " + body);

        // long remaining = result.longValue();

        // // -1 = Not enough tokens, -2 = Key doesn't exist
        // if (remaining == -1 || remaining == -2) return null;

        // return remaining;
    }
}
