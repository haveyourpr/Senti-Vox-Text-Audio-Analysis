package com.sentivox.controller;

import com.sentivox.SentiVoxBase;
import com.sentivox.analyzer.JsonKeys;
import com.sentivox.service.AnalyzerService;
import com.sentivox.service.RedisService;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.json.JsonMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Controller("/cf")
@Tag(name = "Context-free analysis")
public class AnalyzerController {
    private static final Logger LOG = LoggerFactory.getLogger(AnalyzerController.class);

    private final AnalyzerService analyzer;
    private final RedisService redis;
    private final JsonMapper jsonMapper;

    public AnalyzerController(AnalyzerService analyzer, RedisService redis, JsonMapper jsonMapper) {
        this.analyzer = analyzer;
        this.redis = redis;
        this.jsonMapper = jsonMapper;
    }

    @Schema(name = "SingleCommentRequest")
    public record SingleCommentRequest(
            @Schema(name = JsonKeys.apiKey, description = "Your API key", requiredMode = Schema.RequiredMode.REQUIRED) String apiKey,
            @Schema(name = JsonKeys.comment, description = "Text to analyze", requiredMode = Schema.RequiredMode.REQUIRED) String comment) {}

    @Schema(name = "BatchCommentRequest")
    public record BatchCommentRequest(
            @Schema(name = JsonKeys.apiKey, description = "Your API key", requiredMode = Schema.RequiredMode.REQUIRED) String apiKey,
            @ArraySchema(arraySchema = @Schema(name = JsonKeys.commentList, description = "Comments to analyze", requiredMode = Schema.RequiredMode.REQUIRED)) List<String> commentList) {}

    @Post(uri = "/single", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    @Operation(summary = "Context free analysis of a single comment")
    @RequestBody(required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = SingleCommentRequest.class)))
    public CompletableFuture<HttpResponse<Object>> single(@Body @Nullable String rawBody) {
        String apiKey;
        String comment;
        try {
            Map<String, Object> body = JsonBody.parseObject(jsonMapper, rawBody);
            apiKey = JsonBody.requireString(body, JsonKeys.apiKey);
            comment = JsonBody.requireString(body, JsonKeys.comment);
        } catch (JsonBody.InvalidBodyException e) {
            return done(invalidFormat(e.getMessage(), "Invalid JSON format. Refer to documentation.",
                    SentiVoxBase.Docs.SINGLE_COMMENT_ANALYSIS));
        }

        if (apiKey.isEmpty()) return done(missingApiKey());

        return redis.deductToken(apiKey, comment.length())
                .thenCompose(tokens -> tokens == null
                        ? done(quotaExceeded())
                        : analyzer.startSingleAnalysis(comment).thenApply(AnalyzerController::ok))
                .exceptionally(AnalyzerController::internalError);
    }

    @Post(uri = "/batch", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    @Operation(summary = "Context free analysis of multiple comments")
    @RequestBody(required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = BatchCommentRequest.class)))
    public CompletableFuture<HttpResponse<Object>> batch(@Body @Nullable String rawBody) {
        String apiKey;
        List<String> commentList;
        try {
            Map<String, Object> body = JsonBody.parseObject(jsonMapper, rawBody);
            apiKey = JsonBody.requireString(body, JsonKeys.apiKey);
            commentList = JsonBody.requireStringArray(body, JsonKeys.commentList);
        } catch (JsonBody.InvalidBodyException e) {
            return done(invalidFormat(e.getMessage(), "Invalid JSON format. Refer to documentation. Error string: " + e.getMessage(),
                    SentiVoxBase.Docs.BATCH_COMMENT_ANALYSIS));
        }

        // validating format
        if (apiKey.isEmpty()) return done(missingApiKey());

        long totalLen = 0;
        for (String singleComment : commentList) totalLen += singleComment.length();

        return redis.deductToken(apiKey, totalLen)
                .thenCompose(tokens -> tokens == null
                        ? done(quotaExceeded())
                        : analyzer.startBatchAnalysis(commentList).thenApply(AnalyzerController::ok))
                .exceptionally(AnalyzerController::internalError);
    }

    private static CompletableFuture<HttpResponse<Object>> done(HttpResponse<Object> response) {
        return CompletableFuture.completedFuture(response);
    }

    private static HttpResponse<Object> ok(Map<String, Object> result) {
        return HttpResponse.<Object>ok(result);
    }

    private static HttpResponse<Object> missingApiKey() {
        return HttpResponse.<Object>status(HttpStatus.UNAUTHORIZED).body(SentiVoxBase.error("Missing API Key"));
    }

    private static HttpResponse<Object> quotaExceeded() {
        return HttpResponse.<Object>status(HttpStatus.UNAUTHORIZED).body(SentiVoxBase.error("Quota exceeded or invalid API key!"));
    }

    private static HttpResponse<Object> invalidFormat(String error, String message, Map<String, Object> details) {
        return HttpResponse.<Object>status(HttpStatus.BAD_REQUEST).body(SentiVoxBase.ordered(
                JsonKeys.error, error,
                "status", SentiVoxBase.StatusCode.INVALID_JSON_FORMAT,
                "message", message,
                "details", details));
    }

    private static HttpResponse<Object> internalError(Throwable t) {
        Throwable cause = t instanceof CompletionException && t.getCause() != null ? t.getCause() : t;
        LOG.error("Analysis request failed", cause);
        return HttpResponse.<Object>serverError().body(SentiVoxBase.error(SentiVoxBase.MSG_INTERNAL_ERROR));
    }
}
