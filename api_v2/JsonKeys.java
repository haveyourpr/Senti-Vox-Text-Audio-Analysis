/**
 * Port of JSON_KEYS from "../utils/keys" in the TypeScript project.
 * NOTE: utils/keys.ts was NOT included in the uploaded zip, so these string values are inferred from the
 * SentimentResult field names (snake_case). If your keys.ts uses different strings, change them here only.
 */
public final class JsonKeys {
    private JsonKeys() {}
    public static final String positiveScore    = "positive_score";
    public static final String negativeScore    = "negative_score";
    public static final String polarityScore    = "polarity_score";
    public static final String sentiment        = "sentiment";
    public static final String positiveWords    = "positive_words";
    public static final String negativeWords    = "negative_words";
    public static final String unidentifiedWords = "unidentified_words";
    public static final String keywords         = "keywords";
    public static final String wordCloud        = "word_cloud";
}
