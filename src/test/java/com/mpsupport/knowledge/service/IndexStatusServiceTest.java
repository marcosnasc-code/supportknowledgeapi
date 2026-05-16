package com.mpsupport.knowledge.service;

import com.mpsupport.knowledge.IntegrationTestBase;
import com.mpsupport.knowledge.dto.IndexStatusResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = "app.import.csv.charset=UTF-8")
class IndexStatusServiceTest extends IntegrationTestBase {

    @Autowired
    IndexStatusService indexStatusService;

    @Autowired
    CsvImportService csvImportService;

    @Test
    void status_reflectsPersistedIndex() throws Exception {
        String csv = """
                Ref,Descrição,Log público,Solução,Log privado
                10,Teste,,,
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "tickets.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );

        csvImportService.importTicketsCsv(file);

        IndexStatusResponse status = indexStatusService.getStatus();

        assertThat(status.totalChunks()).isEqualTo(1);
        assertThat(status.distinctTickets()).isEqualTo(1);
        assertThat(status.lastImportBatchId()).isNotNull();
        assertThat(status.lastImportStatus()).isEqualTo("DONE");
        assertThat(status.lastImportAt()).isNotNull();
    }
}
