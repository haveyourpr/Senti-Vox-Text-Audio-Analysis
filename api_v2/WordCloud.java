import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class WordCloud {
    private WordCloud() {}

    private static final class Placed {
        final String text; final double size, x, y, width, height;
        Placed(String text, double size, double x, double y, double width, double height) {
            this.text = text; this.size = size; this.x = x; this.y = y; this.width = width; this.height = height;
        }
    }

    private static final String[] COLORS = {
        "#6366f1", "#8b5cf6", "#ec4899", "#f43f5e", "#f59e0b", "#10b981", "#3b82f6"
    };

    public static String generateWordCloud(Map<String, Integer> keywords) {
        final double width = 500, height = 500, padding = 2;

        List<Map.Entry<String, Integer>> words = new ArrayList<>(keywords.entrySet());
        words.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));   // stable, descending by frequency

        List<Placed> placed = new ArrayList<>();
        int maxFreq = words.size() > 0 ? words.get(0).getValue() : 1;
        int minFreq = words.size() > 0 ? words.get(words.size() - 1).getValue() : 1;

        for (Map.Entry<String, Integer> word : words) {
            String text = word.getKey();
            double fontSize = (maxFreq == minFreq) ? 25 : 14 + (((double) (word.getValue() - minFreq)) / (maxFreq - minFreq)) * 36;
            double wordWidth = text.length() * fontSize * 0.6;
            double wordHeight = fontSize;

            double angle = 0, radius = 0;
            while (radius < Math.max(width, height) * 0.7) {
                double x = width / 2 + radius * StrictMath.cos(angle) - wordWidth / 2;
                double y = height / 2 + radius * StrictMath.sin(angle) - wordHeight / 2;

                boolean collision = false;
                for (Placed p : placed) {
                    if (x < p.x + p.width + padding && x + wordWidth + padding > p.x &&
                        y < p.y + p.height + padding && y + wordHeight + padding > p.y) {
                        collision = true;
                        break;
                    }
                }

                if (!collision && x >= 10 && x + wordWidth <= width - 10 && y >= 10 && y + wordHeight <= height - 10) {
                    placed.add(new Placed(text, fontSize, x, y, wordWidth, wordHeight));
                    break;
                }
                angle += 0.15;
                radius += 0.4;
            }
        }

        StringBuilder elements = new StringBuilder();
        for (int i = 0; i < placed.size(); i++) {
            Placed w = placed.get(i);
            if (i > 0) elements.append("\n    ");
            elements.append("<text x=\"").append(Json.jsNumber(w.x))
                    .append("\" y=\"").append(Json.jsNumber(w.y + w.height * 0.8))
                    .append("\" font-family=\"Segoe UI, Roboto, Helvetica, Arial, sans-serif\" font-size=\"")
                    .append(Json.jsNumber(w.size)).append("\" fill=\"").append(COLORS[i % COLORS.length])
                    .append("\" font-weight=\"bold\">").append(w.text).append("</text>");
        }

        return "<svg width=\"500\" height=\"500\" viewBox=\"0 0 500 500\" xmlns=\"http://www.w3.org/2000/svg\" style=\"background-color: transparent;\">\n"
             + "    " + elements + "\n  </svg>";
    }
}
