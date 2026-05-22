package com.mpsupport.knowledge.util;

public final class VectorUtils {

    private VectorUtils() {
    }

    public static String toVectorLiteral(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(embedding[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    /**
     * Reduz texto para caber no contexto do modelo de embedding.
     * Usa início + fim do texto (útil para logs longos: erro no começo, desfecho no final).
     */
    public static String truncateForEmbedding(String text, int maxChars) {
        if (text == null) {
            return "";
        }
        String normalized = text.strip().replaceAll("\\s+", " ");
        if (normalized.length() <= maxChars) {
            return normalized;
        }
        if (maxChars < 50) {
            return normalized.substring(0, maxChars);
        }

        String ellipsis = " ... ";
        int budget = maxChars - ellipsis.length();
        int head = (int) (budget * 0.65);
        int tail = budget - head;
        return normalized.substring(0, head) + ellipsis + normalized.substring(normalized.length() - tail);
    }
}
