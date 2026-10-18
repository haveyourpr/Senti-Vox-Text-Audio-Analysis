package com.sentivox.controller;

import io.micronaut.core.type.Argument;
import io.micronaut.json.JsonMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Minimal request-body validation standing in for Elysia's t.Object / t.String / t.Array schemas.
 * Bodies are parsed by hand so invalid JSON gets the same error response as a schema violation.
 */
final class JsonBody {
    private JsonBody() {}

    /** Thrown when the body does not match the expected schema; the message is returned to the client. */
    static final class InvalidBodyException extends Exception {
        InvalidBodyException(String message) { super(message); }
    }

    static Map<String, Object> parseObject(JsonMapper jsonMapper, String raw) throws InvalidBodyException {
        if (raw == null || raw.isBlank()) throw new InvalidBodyException("Expected a JSON object body");
        Map<String, Object> body;
        try {
            body = jsonMapper.readValue(raw, Argument.mapOf(String.class, Object.class));
        } catch (IOException | RuntimeException e) {
            throw new InvalidBodyException("Malformed JSON body: " + e.getMessage());
        }
        if (body == null) throw new InvalidBodyException("Expected a JSON object body");
        return body;
    }

    static String requireString(Map<String, Object> body, String key) throws InvalidBodyException {
        if (body.get(key) instanceof String s) return s;
        throw new InvalidBodyException("Expected property '" + key + "' to be string");
    }

    static List<String> requireStringArray(Map<String, Object> body, String key) throws InvalidBodyException {
        if (body.get(key) instanceof List<?> list) {
            for (int i = 0; i < list.size(); i++)
                if (!(list.get(i) instanceof String))
                    throw new InvalidBodyException("Expected property '" + key + "[" + i + "]' to be string");
            @SuppressWarnings("unchecked")
            List<String> strings = (List<String>) list;
            return strings;
        }
        throw new InvalidBodyException("Expected property '" + key + "' to be array of strings");
    }
}
