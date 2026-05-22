package com.mpsupport.knowledge.service;

import com.mpsupport.knowledge.IntegrationTestBase;
import com.mpsupport.knowledge.dto.EmbeddingIndexRequest;
import com.mpsupport.knowledge.dto.EmbeddingStatusResponse;
import com.mpsupport.knowledge.integration.OllamaEmbeddingClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@TestPropertySource(properties = "app.import.csv.charset=UTF-8")
class EmbeddingIndexServiceTest extends IntegrationTestBase {

    @Autowired
    EmbeddingIndexService embeddingIndexService;

    @Autowired
    CsvImportService csvImportService;

    @MockBean
    OllamaEmbeddingClient ollamaEmbeddingClient;

    @BeforeEach
    void importAndMockEmbeddings() throws Exception {
        float[] fake = unitVector768();

        when(ollamaEmbeddingClient.embed(anyString())).thenReturn(fake);
        when(ollamaEmbeddingClient.embedBatch(org.mockito.ArgumentMatchers.anyList()))
                .thenAnswer(invocation -> {
                    int size = invocation.getArgument(0, java.util.List.class).size();
                    java.util.List<float[]> list = new java.util.ArrayList<>();
                    for (int i = 0; i < size; i++) {
                        list.add(fake);
                    }
                    return list;
                });

        String csv = """
                Ref,Descrição,Log público,Solução,Log privado
                50,Erro ao consolidar movimento,,,
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
    void index_persistsEmbeddings() {
        EmbeddingIndexRequest request = new EmbeddingIndexRequest(10, true);
        embeddingIndexService.index(request);

        EmbeddingStatusResponse status = embeddingIndexService.getStatus();
        assertThat(status.embeddedChunks()).isEqualTo(1);
        assertThat(status.missingChunks()).isZero();
    }

    private static float[] unitVector768() {
        float[] v = new float[768];
        v[0] = 1.0f;
        return v;
    }
}
