package com.sentivox.analyzer;

/** Port of api/src/utils/keys.ts — these keys are the public API contract (the site reads them). */
public final class JsonKeys {
    private JsonKeys() {}

    // request keys
    public static final String apiKey           = "api-key";
    public static final String comment          = "comment";
    public static final String commentList      = "comment-list";

    // response keys
    public static final String positiveScore    = "pos-score";
    public static final String negativeScore    = "neg-score";
    public static final String polarityScore    = "pol-score";
    public static final String sentiment        = "senti";
    public static final String positiveWords    = "pos-words";
    public static final String negativeWords    = "neg-words";
    public static final String unidentifiedWords = "uniden-words";
    public static final String wordCloud        = "word-cloud";
    public static final String keywords         = "keywords";

    public static final String error            = "error";
}
