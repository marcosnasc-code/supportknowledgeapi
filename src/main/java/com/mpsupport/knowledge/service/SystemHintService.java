package com.mpsupport.knowledge.service;

import com.mpsupport.knowledge.config.SystemHintsProperties;
import com.mpsupport.knowledge.domain.SystemHintDefinition;
import com.mpsupport.knowledge.dto.SistemaSugeridoItem;
import com.mpsupport.knowledge.dto.SystemCatalogItem;
import com.mpsupport.knowledge.util.TextNormalizer;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class SystemHintService {

    private final List<SystemHintDefinition> systems;
    private final SystemHintsProperties properties;

    public SystemHintService(List<SystemHintDefinition> systems, SystemHintsProperties properties) {
        this.systems = systems;
        this.properties = properties;
    }

    public List<SystemCatalogItem> listCatalog() {
        return systems.stream()
                .map(s -> new SystemCatalogItem(s.id(), s.displayName()))
                .sorted(Comparator.comparing(SystemCatalogItem::displayName))
                .toList();
    }

    public Optional<SystemHintDefinition> resolveDeclared(String sistemaDeclarado) {
        if (sistemaDeclarado == null || sistemaDeclarado.isBlank()) {
            return Optional.empty();
        }
        String needle = TextNormalizer.normalize(sistemaDeclarado);
        for (SystemHintDefinition system : systems) {
            if (TextNormalizer.normalize(system.id()).equals(needle)
                    || TextNormalizer.normalize(system.displayName()).equals(needle)) {
                return Optional.of(system);
            }
            for (String alias : system.allTerms()) {
                if (TextNormalizer.normalize(alias).equals(needle)) {
                    return Optional.of(system);
                }
            }
        }
        return Optional.empty();
    }

    public List<SistemaSugeridoItem> suggest(String text, String sistemaDeclarado) {
        String normalizedText = TextNormalizer.normalize(text);
        if (normalizedText.isEmpty()) {
            return List.of();
        }

        Optional<SystemHintDefinition> declared = resolveDeclared(sistemaDeclarado);
        List<ScoredSystem> rawScores = new ArrayList<>();

        for (SystemHintDefinition system : systems) {
            double raw = scoreSystem(system, normalizedText);
            if (declared.isPresent() && declared.get().id().equals(system.id())) {
                raw *= properties.getDeclaredBoostMultiplier();
            }
            if (raw > 0) {
                rawScores.add(new ScoredSystem(system, raw));
            }
        }

        if (rawScores.isEmpty()) {
            return List.of();
        }

        double maxRaw = rawScores.stream().mapToDouble(ScoredSystem::rawScore).max().orElse(1.0);
        final double max = maxRaw <= 0 ? 1.0 : maxRaw;

        return rawScores.stream()
                .map(s -> toItem(s, max))
                .sorted(Comparator.comparingDouble(SistemaSugeridoItem::score).reversed())
                .limit(properties.getTopN())
                .toList();
    }

    public boolean textMentionsSystem(String text, SystemHintDefinition system) {
        return scoreSystem(system, TextNormalizer.normalize(text)) > 0;
    }

    private double scoreSystem(SystemHintDefinition system, String normalizedText) {
        double positive = 0;
        for (String term : system.allTerms()) {
            if (TextNormalizer.containsTerm(normalizedText, term)) {
                positive += termWeight(term);
            }
        }

        double negative = 0;
        if (system.negativeKeywords() != null) {
            for (String term : system.negativeKeywords()) {
                if (TextNormalizer.containsTerm(normalizedText, term)) {
                    negative += termWeight(term);
                }
            }
        }

        double raw = system.weight() * (positive - negative);
        return Math.max(0, raw);
    }

    private static double termWeight(String term) {
        String normalized = TextNormalizer.normalize(term);
        if (normalized.length() <= 3) {
            return 0.5;
        }
        return 1.0;
    }

    private SistemaSugeridoItem toItem(ScoredSystem scored, double maxRaw) {
        double normalizedScore = scored.rawScore() / maxRaw;
        String confianca = confiancaLabel(normalizedScore);
        return new SistemaSugeridoItem(
                scored.system().id(),
                scored.system().displayName(),
                round(normalizedScore),
                confianca
        );
    }

    private String confiancaLabel(double normalizedScore) {
        if (normalizedScore >= 0.75) {
            return "ALTA";
        }
        if (normalizedScore >= properties.getMinConfidence()) {
            return "MEDIA";
        }
        return "BAIXA";
    }

    private static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    public Optional<SistemaSugeridoItem> topSuggestion(String text, String sistemaDeclarado) {
        List<SistemaSugeridoItem> items = suggest(text, sistemaDeclarado);
        if (items.isEmpty()) {
            return Optional.empty();
        }
        SistemaSugeridoItem top = items.getFirst();
        if (resolveDeclared(sistemaDeclarado).isPresent()) {
            return Optional.of(top);
        }
        if (top.score() >= properties.getMinConfidence()) {
            return Optional.of(top);
        }
        return Optional.empty();
    }

    public String formatCatalogForPrompt() {
        return systems.stream()
                .map(s -> s.id() + " — " + s.displayName())
                .collect(java.util.stream.Collectors.joining("; "));
    }

    private record ScoredSystem(SystemHintDefinition system, double rawScore) {
    }
}
