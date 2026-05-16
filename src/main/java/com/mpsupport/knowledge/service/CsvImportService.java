package com.mpsupport.knowledge.service;

import com.mpsupport.knowledge.config.ImportCsvProperties;
import com.mpsupport.knowledge.dto.CsvImportResponse;
import com.mpsupport.knowledge.dto.ImportRowError;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CsvImportService {

    private static final Logger log = LoggerFactory.getLogger(CsvImportService.class);
    private static final long LOG_EVERY_N_ROWS = 10_000;

    private final ImportCsvProperties props;
    private final ImportBatchService importBatchService;
    private final ChunkPersistenceService chunkPersistenceService;

    public CsvImportService(
            ImportCsvProperties props,
            ImportBatchService importBatchService,
            ChunkPersistenceService chunkPersistenceService
    ) {
        this.props = props;
        this.importBatchService = importBatchService;
        this.chunkPersistenceService = chunkPersistenceService;
    }

    public CsvImportResponse importTicketsCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo vazio ou ausente.");
        }

        UUID batchId = UUID.randomUUID();
        long processedRows = 0;
        long skippedRows = 0;
        long chunksCreated = 0;
        long rowsRead = 0;
        List<ImportRowError> errors = new ArrayList<>();

        log.info("Iniciando import CSV batchId={} arquivo={} tamanhoBytes={}",
                batchId, file.getOriginalFilename(), file.getSize());

        importBatchService.startBatch(batchId, file.getOriginalFilename());

        try {
            String[] expected = {
                    props.getTicketIdHeader(),
                    props.getUserDescriptionHeader(),
                    props.getPublicLogHeader(),
                    props.getPrivateLogHeader(),
                    props.getFinalSolutionHeader()
            };

            Charset charset = resolveCharset();
            log.info("Import CSV usando charset={}", charset.name());

            try (InputStreamReader reader = new InputStreamReader(file.getInputStream(), charset);
                 CSVParser parser = csvFormat().parse(reader)) {

                validateHeadersPresent(parser.getHeaderNames(), expected);

                for (CSVRecord record : parser) {
                    rowsRead++;
                    long rowNumber = record.getRecordNumber();

                    if (rowsRead % LOG_EVERY_N_ROWS == 0) {
                        log.info("Import CSV batchId={} linhasLidas={} processadas={} ignoradas={} chunks={}",
                                batchId, rowsRead, processedRows, skippedRows, chunksCreated);
                    }

                    if (isRowEffectivelyEmpty(record, expected)) {
                        skippedRows++;
                        continue;
                    }

                    String ticketId = safeGet(record, props.getTicketIdHeader());
                    if (ticketId.isEmpty()) {
                        errors.add(new ImportRowError(rowNumber, "TICKET_ID_MISSING",
                                "Linha com conteúdo mas sem identificador do chamado."));
                        continue;
                    }

                    processedRows++;
                    chunksCreated += chunkPersistenceService.persistFromRecord(batchId, ticketId, record);
                }
            }

            chunkPersistenceService.flushRemaining();
            importBatchService.completeBatch(batchId, processedRows, skippedRows, chunksCreated);

            log.info("Import CSV concluído batchId={} linhasLidas={} processadas={} ignoradas={} chunks={} erros={}",
                    batchId, rowsRead, processedRows, skippedRows, chunksCreated, errors.size());

            return new CsvImportResponse(batchId, processedRows, skippedRows, chunksCreated, errors);
        } catch (IllegalArgumentException e) {
            importBatchService.failBatch(batchId);
            throw e;
        } catch (Exception e) {
            importBatchService.failBatch(batchId);
            throw new IllegalStateException("Falha ao ler CSV: " + e.getMessage(), e);
        }
    }

    private Charset resolveCharset() {
        try {
            return Charset.forName(props.getCharset().strip());
        } catch (Exception e) {
            throw new IllegalArgumentException("Charset inválido em app.import.csv.charset: " + props.getCharset());
        }
    }

    private CSVFormat csvFormat() {
        char sep = switch (props.getDelimiter().strip().toUpperCase()) {
            case "SEMICOLON" -> ';';
            default -> ',';
        };
        return CSVFormat.DEFAULT.builder()
                .setDelimiter(sep)
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(false)
                .setTrim(true)
                .build();
    }

    private static boolean isRowEffectivelyEmpty(CSVRecord record, String[] headers) {
        for (String h : headers) {
            if (!safeGet(record, h).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static String safeGet(CSVRecord record, String header) {
        try {
            String v = record.get(header);
            return v == null ? "" : v.strip();
        } catch (IllegalArgumentException ex) {
            return "";
        }
    }

    private static void validateHeadersPresent(List<String> fileHeaders, String[] expected) {
        for (String col : expected) {
            boolean found = fileHeaders.stream().anyMatch(h -> h != null && h.strip().equals(col));
            if (!found) {
                throw new IllegalArgumentException(
                        "Cabeçalho obrigatório ausente no CSV: \"" + col + "\". Cabeçalhos encontrados: " + fileHeaders
                );
            }
        }
    }
}
