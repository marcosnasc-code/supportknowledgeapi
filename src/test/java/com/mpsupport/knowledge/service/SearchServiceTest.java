package com.mpsupport.knowledge.service;

import com.mpsupport.knowledge.IntegrationTestBase;
import com.mpsupport.knowledge.domain.ChunkSource;
import com.mpsupport.knowledge.dto.SearchFilters;
import com.mpsupport.knowledge.dto.SearchMode;
import com.mpsupport.knowledge.dto.SearchRequest;
import com.mpsupport.knowledge.dto.SearchResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = "app.import.csv.charset=UTF-8")
class SearchServiceTest extends IntegrationTestBase {

    @Autowired
    SearchService searchService;

    @Autowired
    CsvImportService csvImportService;

    @BeforeEach
    void importSampleData() throws Exception {
        String csv = """
                Ref,Descrição,Log público,Solução,Log privado
                100,Erro ao anexar documento na tela principal,Usuário reiniciou navegador,Orientado a limpar cache do navegador,Nota interna
                200,Problema de login com senha expirada,,,
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "tickets.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );
        csvImportService.importTicketsCsv(file);
    }

    @Test
    void search_findsMatchingChunkByPortugueseText() {
        SearchResponse response = searchService.search(new SearchRequest(
                "anexar documento",
                5,
                null,
                SearchMode.TEXT
        ));

        assertThat(response.totalMatches()).isGreaterThanOrEqualTo(1);
        assertThat(response.results()).isNotEmpty();
        assertThat(response.results().getFirst().ticketId()).isEqualTo("100");
        assertThat(response.results().getFirst().snippet()).containsIgnoringCase("anexar");
    }

    @Test
    void search_filtersBySource() {
        SearchResponse response = searchService.search(new SearchRequest(
                "cache navegador",
                10,
                new SearchFilters(List.of(ChunkSource.SOLUCAO)),
                SearchMode.TEXT
        ));

        assertThat(response.results()).isNotEmpty();
        assertThat(response.results())
                .allMatch(item -> item.source().equals(ChunkSource.SOLUCAO.name()));
    }

    @Test
    void search_noMatch_returnsEmptyResults() {
        SearchResponse response = searchService.search(new SearchRequest(
                "xyztermoquenaoexiste12345",
                5,
                null,
                SearchMode.TEXT
        ));

        assertThat(response.totalMatches()).isZero();
        assertThat(response.results()).isEmpty();
    }
}
