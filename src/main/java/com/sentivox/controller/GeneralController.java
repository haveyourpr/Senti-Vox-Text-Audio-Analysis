package com.sentivox.controller;

import com.sentivox.SentiVoxBase;
import com.sentivox.analyzer.JsonKeys;
import com.sentivox.analyzer.Token;
import com.sentivox.service.AnalyzerService;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.json.JsonMapper;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Controller
@Tag(name = "General")
public class GeneralController {
    private final AnalyzerService analyzer;
    private final JsonMapper jsonMapper;

    public GeneralController(AnalyzerService analyzer, JsonMapper jsonMapper) {
        this.analyzer = analyzer;
        this.jsonMapper = jsonMapper;
    }

    @Schema(name = "TokenizeRequest")
    public record TokenizeRequest(
            @Schema(name = JsonKeys.comment, description = "Text to tokenize", requiredMode = Schema.RequiredMode.REQUIRED) String comment) {}

    @Get(produces = MediaType.APPLICATION_JSON)
    @Operation(summary = "Welcome message")
    public Map<String, Object> welcome() {
        return SentiVoxBase.WELCOME_MESSAGE;
    }

    @Get(uri = "/health", produces = MediaType.APPLICATION_JSON)
    @Operation(summary = "Health check")
    public Map<String, Object> health() {
        return SentiVoxBase.HEALTH_MESSAGE;
    }

    @Get(uri = "/langs", produces = MediaType.APPLICATION_JSON)
    @Operation(summary = "Supported languages")
    public List<String> langs() {
        return SentiVoxBase.SUPPORTED_LANGUAGES;
    }

    @Hidden
    @Get("/docs")
    public HttpResponse<?> docs() {
        return HttpResponse.redirect(URI.create("/redoc/index.html"));
    }

    @Post(uri = "/tokenize", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    @Operation(summary = "Tokenizes your given text")
    @RequestBody(required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = TokenizeRequest.class)))
    public CompletableFuture<HttpResponse<Object>> tokenize(@Body @Nullable String rawBody) {
        String comment;
        try {
            comment = JsonBody.requireString(JsonBody.parseObject(jsonMapper, rawBody), JsonKeys.comment);
        } catch (JsonBody.InvalidBodyException e) {
            return CompletableFuture.completedFuture(
                    HttpResponse.<Object>status(HttpStatus.BAD_REQUEST).body(SentiVoxBase.error(e.getMessage())));
        }

        return analyzer.tokenize(comment).thenApply(tokens -> {
            List<Map<String, Object>> out = new ArrayList<>(tokens.size());
            for (Token t : tokens) out.add(SentiVoxBase.ordered("value", t.value, "tag", t.tag));
            return HttpResponse.<Object>ok(out);
        });
    }
}
