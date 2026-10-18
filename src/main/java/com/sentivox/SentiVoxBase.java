package com.sentivox;

import com.sentivox.analyzer.JsonKeys;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Port of api/src/base.ts — base configuration, constants and static response bodies. */
public final class SentiVoxBase {
    private SentiVoxBase() {}

    // compile-time constants (also used by @OpenAPIDefinition in Application)
    public static final String APP_NAME = "SentiVox API";
    public static final String APP_VERSION = "1.0.0";
    public static final String APP_URL = "https://senti-vox-api.onrender.com";
    public static final String APP_DESCRIPTION =
            "SentiVox is a simple, fast, multilingual and accurate API to perform sentiment analysis on the text and find the sentiment along with a word cloud.";
    public static final String APP_DOCUMENTATION = "https://senti-vox-api.onrender.com/docs";

    public static final String AUTHOR_NAME = "SAKSHAM JOSHI";
    public static final String AUTHOR_EMAIL = "social.sakshamjoshi@gmail.com";
    public static final String AUTHOR_PORTFOLIO = "https://sakshamjoshi.vercel.app";

    public static final int DAILY_FREE_TOKEN_COUNT = 10000;

    public static final List<Map<String, Object>> AUTHOR_DETAILS = List.of(
            ordered(
                    "name", AUTHOR_NAME,
                    "email", AUTHOR_EMAIL,
                    "linkedin", "https://www.linkedin.com/in/sakshamjoshi27",
                    "github", "https://github.com/saksham-joshi",
                    "portfolio", AUTHOR_PORTFOLIO),
            ordered(
                    "name", "SUBHAM TIWARI",
                    "email", "subhamt958@gmail.com",
                    "linkedin", "https://www.linkedin.com/in/subham-tiwari-ab38971b4/",
                    "github", "https://github.com/W0nder0fy0u"));

    /** LANG.supported (Bengali, Kannada, Gujarati, Punjabi, Telugu and Urdu are not enabled yet). */
    public static final List<String> SUPPORTED_LANGUAGES = List.of("English", "Hindi", "Marathi", "Tamil");

    public static final class StatusCode {
        private StatusCode() {}
        public static final int SUCCESS = 200;
        public static final int INVALID_JSON_FORMAT = 400;
        public static final int INVALID_ENDPOINT = 404;
        public static final int MISSING_OR_INVALID_AUTH = 401;
        public static final int VALID_REQUEST_BUT_FAILED_VALIDATION = 422;
    }

    public static final class Docs {
        private Docs() {}

        public static final Map<String, Object> SINGLE_COMMENT_ANALYSIS = ordered(
                "endpoint", "/cf/single",
                "method", "POST",
                "format", ordered(
                        JsonKeys.apiKey, "string - Your API key",
                        JsonKeys.comment, "text to analyze"));

        public static final Map<String, Object> BATCH_COMMENT_ANALYSIS = ordered(
                "endpoint", "/cf/batch",
                "method", "POST",
                "format", ordered(
                        JsonKeys.apiKey, "string - Your API key",
                        JsonKeys.commentList, List.of("comment1", "comment2", "...")));
    }

    public static final Map<String, Object> HEALTH_MESSAGE = ordered("status", "healthy");

    public static final Map<String, Object> WELCOME_MESSAGE = ordered(
            "message", "Welcome to " + APP_NAME,
            "description", APP_DESCRIPTION,
            "version", APP_VERSION,
            "endpoints", ordered(
                    "/cf/single", "POST - Context free analysis of a single comment",
                    "/cf/batch", "POST - Context free analysis of multiple comments",
                    "/health", "GET - Health check",
                    "/docs", "GET - Documentations",
                    "/langs", "GET - Supported languages",
                    "/tokenize", "POST - Tokenizes your given text"),
            "supported_languages", SUPPORTED_LANGUAGES,
            "author", AUTHOR_DETAILS,
            "documentation", APP_DOCUMENTATION);

    public static final String MSG_INTERNAL_ERROR = "Internal Server Error! We are fixing. Kindly wait!!";
    public static final String MSG_INVALID_ENDPOINT = "Invalid Endpoint! Refer to documentation on '/docs' endpoint";

    /** Insertion-ordered map from alternating key/value pairs, so JSON output keeps the declared order. */
    public static Map<String, Object> ordered(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    public static Map<String, Object> error(String message) {
        return ordered(JsonKeys.error, message);
    }
}
