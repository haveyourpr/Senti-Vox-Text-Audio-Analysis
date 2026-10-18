package com.sentivox.analyzer;

/**
 * Represents a single token produced by the Tokenizer.
 * Each token has a text value and a tag indicating its type.
 */
public class Token {
    public final String value;
    public final String tag; // "word", "number", or "punctuation"

    public Token(String value, String tag) {
        this.value = value;
        this.tag = tag;
    }

    @Override
    public String toString() {
        return "Token{value='" + value + "', tag='" + tag + "'}";
    }
}
