package com.sentivox.analyzer;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Collection;
import java.util.Map;

/** Minimal JSON writer (no libraries). Number/string output follows JSON.stringify conventions. */
public final class Json {
    private Json() {}

    public static String stringify(Object o, boolean pretty) {
        StringBuilder sb = new StringBuilder();
        write(sb, o, pretty, 0);
        return sb.toString();
    }

    /** Shortest round-trip decimal, printed like JavaScript's Number#toString for normal ranges (1e-6 <= |x| < 1e21). */
    public static String jsNumber(double d) {
        if (Double.isNaN(d) || Double.isInfinite(d)) return "null";
        if (d == 0) return "0";
        for (int p = 1; p <= 17; p++) {
            BigDecimal bd = new BigDecimal(d).round(new MathContext(p));
            if (bd.doubleValue() == d) {
                String s = bd.stripTrailingZeros().toPlainString();
                return s;
            }
        }
        return Double.toString(d);
    }

    private static void write(StringBuilder sb, Object o, boolean pretty, int depth) {
        if (o == null) sb.append("null");
        else if (o instanceof String) quote(sb, (String) o);
        else if (o instanceof Double || o instanceof Float) sb.append(jsNumber(((Number) o).doubleValue()));
        else if (o instanceof Number || o instanceof Boolean) sb.append(o);
        else if (o instanceof Map) {
            Map<?, ?> m = (Map<?, ?>) o;
            if (m.isEmpty()) { sb.append("{}"); return; }
            if (pretty && m.size() <= 2 && allScalar(m.values())) { write(sb, o, false, depth); return; }
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                nl(sb, pretty, depth + 1);
                quote(sb, String.valueOf(e.getKey()));
                sb.append(pretty ? ": " : ":");
                write(sb, e.getValue(), pretty, depth + 1);
            }
            nl(sb, pretty, depth);
            sb.append('}');
        } else if (o instanceof Collection) {
            Collection<?> c = (Collection<?>) o;
            if (c.isEmpty()) { sb.append("[]"); return; }
            if (pretty && allScalar(c)) { write(sb, o, false, depth); return; }
            sb.append('[');
            boolean first = true;
            for (Object v : c) {
                if (!first) sb.append(',');
                first = false;
                nl(sb, pretty, depth + 1);
                write(sb, v, pretty, depth + 1);
            }
            nl(sb, pretty, depth);
            sb.append(']');
        } else quote(sb, o.toString());
    }

    private static boolean allScalar(Collection<?> values) {
        for (Object v : values) if (v instanceof Map || v instanceof Collection) return false;
        return true;
    }

    private static void nl(StringBuilder sb, boolean pretty, int depth) {
        if (!pretty) return;
        sb.append('\n');
        for (int i = 0; i < depth; i++) sb.append("  ");
    }

    private static void quote(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else if (Character.isSurrogate(c) && !(Character.isHighSurrogate(c) && i + 1 < s.length() && Character.isLowSurrogate(s.charAt(i + 1)))
                             && !(Character.isLowSurrogate(c) && i > 0 && Character.isHighSurrogate(s.charAt(i - 1))))
                        sb.append(String.format("\\u%04x", (int) c));      // lone surrogate (well-formed JSON.stringify)
                    else sb.append(c);
            }
        }
        sb.append('"');
    }
}
