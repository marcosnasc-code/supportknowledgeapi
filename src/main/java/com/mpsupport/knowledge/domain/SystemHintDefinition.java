package com.mpsupport.knowledge.domain;

import java.util.ArrayList;
import java.util.List;

public record SystemHintDefinition(
        String id,
        String displayName,
        List<String> aliases,
        List<String> keywords,
        List<String> negativeKeywords,
        double weight
) {
    public List<String> allTerms() {
        List<String> terms = new ArrayList<>();
        terms.add(displayName);
        if (aliases != null) {
            terms.addAll(aliases);
        }
        if (keywords != null) {
            terms.addAll(keywords);
        }
        return terms;
    }
}
