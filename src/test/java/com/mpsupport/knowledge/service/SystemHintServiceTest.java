package com.mpsupport.knowledge.service;

import com.mpsupport.knowledge.config.SystemHintsProperties;
import com.mpsupport.knowledge.domain.SystemHintDefinition;
import com.mpsupport.knowledge.dto.SistemaSugeridoItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SystemHintServiceTest {

    SystemHintService service;

    @BeforeEach
    void setUp() {
        SystemHintsProperties properties = new SystemHintsProperties();
        properties.setMinConfidence(0.35);
        properties.setTopN(3);
        properties.setDeclaredBoostMultiplier(1.5);

        List<SystemHintDefinition> systems = List.of(
                new SystemHintDefinition(
                        "MPCLOUD",
                        "MPCloud",
                        List.of("mpcloud", "mp cloud"),
                        List.of("mpcloud"),
                        null,
                        1.0
                ),
                new SystemHintDefinition(
                        "ATENA",
                        "Atena",
                        List.of("atena"),
                        List.of("atena"),
                        null,
                        1.0
                ),
                new SystemHintDefinition(
                        "GP",
                        "GP",
                        List.of("gp"),
                        List.of("gp"),
                        null,
                        0.8
                )
        );

        service = new SystemHintService(systems, properties);
    }

    @Test
    void suggest_ranksMpcloudWhenMentioned() {
        List<SistemaSugeridoItem> items = service.suggest(
                "Erro ao fazer upload no MPCloud para o usuário externo",
                null
        );

        assertThat(items).isNotEmpty();
        assertThat(items.getFirst().id()).isEqualTo("MPCLOUD");
        assertThat(items.getFirst().displayName()).isEqualTo("MPCloud");
    }

    @Test
    void resolveDeclared_matchesDisplayName() {
        assertThat(service.resolveDeclared("MPCloud")).map(SystemHintDefinition::id).contains("MPCLOUD");
        assertThat(service.resolveDeclared("MPCLOUD")).map(SystemHintDefinition::id).contains("MPCLOUD");
    }

    @Test
    void suggest_boostsDeclaredSystem() {
        List<SistemaSugeridoItem> items = service.suggest(
                "problema genérico de login",
                "MPCloud"
        );

        assertThat(items).isNotEmpty();
        assertThat(items.getFirst().id()).isEqualTo("MPCLOUD");
    }

    @Test
    void listCatalog_returnsAllSystems() {
        assertThat(service.listCatalog()).hasSize(3);
    }
}
