package com.mpsupport.knowledge.controller;

import com.mpsupport.knowledge.config.ImportCsvProperties;
import com.mpsupport.knowledge.dto.CsvImportResponse;
import com.mpsupport.knowledge.dto.ExpectedCsvHeadersResponse;
import com.mpsupport.knowledge.service.CsvImportService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/imports")
public class ImportController {

    private final CsvImportService csvImportService;
    private final ImportCsvProperties importCsvProperties;

    public ImportController(CsvImportService csvImportService, ImportCsvProperties importCsvProperties) {
        this.csvImportService = csvImportService;
        this.importCsvProperties = importCsvProperties;
    }

    @GetMapping("/csv/expected-headers")
    public ExpectedCsvHeadersResponse expectedHeaders() {
        return ExpectedCsvHeadersResponse.from(importCsvProperties);
    }

    @PostMapping(value = "/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CsvImportResponse importCsv(@RequestParam("file") MultipartFile file) {
        return csvImportService.importTicketsCsv(file);
    }
}
