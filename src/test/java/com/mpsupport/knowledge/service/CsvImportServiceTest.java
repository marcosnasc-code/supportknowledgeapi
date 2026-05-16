package com.mpsupport.knowledge.service;

import com.mpsupport.knowledge.IntegrationTestBase;
import com.mpsupport.knowledge.dto.CsvImportResponse;
import com.mpsupport.knowledge.repository.KnowledgeChunkRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = "app.import.csv.charset=UTF-8")
class CsvImportServiceTest extends IntegrationTestBase {

    @Autowired
    CsvImportService csvImportService;

    @Autowired
    KnowledgeChunkRepository knowledgeChunkRepository;

    @Test
    void importSample_persistsChunksAndCounts() throws Exception {
        String csv = """
                Ref,Descrição,Log público,Solução,Log privado
                1,Erro ao salvar,Reiniciou app,Reinstalar módulo,Notas internas
                2,Só descrição,,,
                ,,,,
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "tickets.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );

        CsvImportResponse response = csvImportService.importTicketsCsv(file);

        assertThat(response.processedRows()).isEqualTo(2);
        assertThat(response.skippedRows()).isEqualTo(1);
        assertThat(response.chunksCreated()).isEqualTo(5);
        assertThat(response.errors()).isEmpty();
        assertThat(knowledgeChunkRepository.count()).isEqualTo(5);
        assertThat(knowledgeChunkRepository.countDistinctTicketId()).isEqualTo(2);
    }

    @Test
    void reimport_sameTicket_upsertsWithoutDuplicating() throws Exception {
        String csv = """
                Ref,Descrição,Log público,Solução,Log privado
                1,Primeira versão,,,
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "tickets.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );

        csvImportService.importTicketsCsv(file);

        String csvUpdated = """
                Ref,Descrição,Log público,Solução,Log privado
                1,Segunda versão,,,
                """;

        MockMultipartFile fileUpdated = new MockMultipartFile(
                "file",
                "tickets.csv",
                "text/csv",
                csvUpdated.getBytes(StandardCharsets.UTF_8)
        );

        csvImportService.importTicketsCsv(fileUpdated);

        assertThat(knowledgeChunkRepository.count()).isEqualTo(1);
        assertThat(knowledgeChunkRepository.findAll().getFirst().getContent()).isEqualTo("Segunda versão");
    }
}
