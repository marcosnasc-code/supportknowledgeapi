package com.mpsupport.knowledge.dto;

import com.mpsupport.knowledge.config.ImportCsvProperties;

/**
 * Cabeçalhos esperados na importação (útil para conferir no Insomnia antes de subir o CSV).
 */
public record ExpectedCsvHeadersResponse(
        String delimiter,
        String charset,
        String ticketIdHeader,
        String userDescriptionHeader,
        String publicLogHeader,
        String privateLogHeader,
        String finalSolutionHeader
) {
    public static ExpectedCsvHeadersResponse from(ImportCsvProperties p) {
        return new ExpectedCsvHeadersResponse(
                p.getDelimiter(),
                p.getCharset(),
                p.getTicketIdHeader(),
                p.getUserDescriptionHeader(),
                p.getPublicLogHeader(),
                p.getPrivateLogHeader(),
                p.getFinalSolutionHeader()
        );
    }
}
