package com.mpsupport.knowledge.util;

import java.text.Normalizer;
import java.util.Locale;

public final class TextNormalizer {

    private TextNormalizer() {
    }

    public static String normalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String nfd = Normalizer.normalize(text, Normalizer.Form.NFD);
        String withoutAccents = nfd.replaceAll("\\p{M}+", "");
        return withoutAccents.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").strip();
    }

    public static boolean containsTerm(String normalizedText, String term) {
        if (normalizedText.isEmpty() || term == null || term.isBlank()) {
            return false;
        }
        String normalizedTerm = normalize(term);
        if (normalizedTerm.isEmpty()) {
            return false;
        }
        if (normalizedTerm.length() <= 3) {
            return (" " + normalizedText + " ").contains(" " + normalizedTerm + " ");
        }
        return normalizedText.contains(normalizedTerm);
    }
}
