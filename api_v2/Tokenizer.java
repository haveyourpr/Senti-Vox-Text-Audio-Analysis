import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;
import java.util.regex.Pattern;

public final class Tokenizer {
    private Tokenizer() {}

    // ---------- URL / e-mail removal (same regexes as the Python source) ----------
    private static final String WS = "\\s\\x1c-\\x1f";    // python's \s on str also matches U+001C..U+001F
    private static final String LOCAL = "A-Za-z0-9!#$%&'*+/=?^_`{|}~\\-";
    private static final String LABEL = "[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?";
    private static final String EMAIL =
            "(?:mailto:)?" +
            "(?<![" + LOCAL + ".])" +
            "[" + LOCAL + "]+(?:\\.[" + LOCAL + "]+)*" +
            "@(?:" + LABEL + "\\.)+[A-Za-z]{2,}";
    private static final String CH = "[^" + WS + "()<>\"']";
    private static final String URL =
            "(?:(?:https?|ftps?)://|www\\.)" +
            "(?:" + CH + "|\\(" + CH + "*\\))*" +
            "(?:\\(" + CH + "*\\)|[^" + WS + "()<>\"'.,;:!?\\]}])";

    private static final Pattern PATTERN = Pattern.compile(
            EMAIL + "|" + URL, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    public static String removeUrlsAndEmails(String text) {
        if (text == null || text.isEmpty()) return text;
        if (text.indexOf('@') >= 0 || text.contains("://") || text.toLowerCase(java.util.Locale.ROOT).contains("www."))
            text = PATTERN.matcher(text).replaceAll("");
        return text;
    }

    private static IntPredicate range(int a, int b) { return c -> c >= a && c <= b; }

    private static final IntPredicate[] DIGITS = {
        range('0', '9'),          // English
        range(2406, 2415),        // Hindi
        range(2534, 2543),        // Bengali
        range(2790, 2799),        // Gujarati
        range(2662, 2671),        // Gurumukhi
        range(3046, 3055),        // Tamil
        range(3174, 3183),        // Telugu
        range(3302, 3311),        // Kannada
        range(3430, 3439),        // Malayalam
    };
    private static final IntPredicate[] SCRIPTS = {
        c -> (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z'),   // English
        range(2304, 2431),        // Hindi
        range(2432, 2559),        // Bengali
        range(2688, 2815),        // Gujarati
        range(2560, 2687),        // Gurumukhi
        range(2944, 3071),        // Tamil
        range(3072, 3199),        // Telugu
        range(3200, 3327),        // Kannada
        range(3328, 3455),        // Malayalam
    };

    public static List<Token> tokenize(String text) {
        List<Token> out = new ArrayList<>();
        text = removeUrlsAndEmails(text);
        if (text == null) return out;
        int index = 0, len = text.length();

        while (index < len) {
            int cp = text.codePointAt(index);
            IntPredicate check = null;
            String tag = null;

            for (IntPredicate d : DIGITS) if (d.test(cp)) { check = d; tag = "number"; break; }
            if (check == null) for (IntPredicate s : SCRIPTS) if (s.test(cp)) { check = s; tag = "word"; break; }

            if (check == null) {
                if (cp == ' ' || cp == '\t' || cp == '\n') { index++; continue; }
                int n = Character.charCount(cp);
                out.add(new Token(text.substring(index, index + n), "punctuation"));
                index += n;
                continue;
            }

            int start = index;
            while (index < len) {
                int c = text.codePointAt(index);
                if (!check.test(c)) break;
                index += Character.charCount(c);
            }
            out.add(new Token(text.substring(start, index), tag));
        }
        return out;
    }
}
