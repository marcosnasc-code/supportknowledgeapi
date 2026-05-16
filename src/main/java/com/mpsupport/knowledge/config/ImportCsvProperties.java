package com.mpsupport.knowledge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.import.csv")
public class ImportCsvProperties {

    /**
     * Nome exato da coluna do identificador do chamado no CSV.
     */
    private String ticketIdHeader = "ID";

    private String userDescriptionHeader = "Descrição do usuário";
    private String publicLogHeader = "Log público do atendimento";
    private String privateLogHeader = "Log privado do atendimento";
    private String finalSolutionHeader = "Solução final dada";

    /**
     * COMMA ou SEMICOLON.
     */
    private String delimiter = "COMMA";

    /**
     * Charset do arquivo exportado (ex.: WINDOWS-1252, UTF-8, ISO-8859-1).
     */
    private String charset = "UTF-8";

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public String getTicketIdHeader() {
        return ticketIdHeader;
    }

    public void setTicketIdHeader(String ticketIdHeader) {
        this.ticketIdHeader = ticketIdHeader;
    }

    public String getUserDescriptionHeader() {
        return userDescriptionHeader;
    }

    public void setUserDescriptionHeader(String userDescriptionHeader) {
        this.userDescriptionHeader = userDescriptionHeader;
    }

    public String getPublicLogHeader() {
        return publicLogHeader;
    }

    public void setPublicLogHeader(String publicLogHeader) {
        this.publicLogHeader = publicLogHeader;
    }

    public String getPrivateLogHeader() {
        return privateLogHeader;
    }

    public void setPrivateLogHeader(String privateLogHeader) {
        this.privateLogHeader = privateLogHeader;
    }

    public String getFinalSolutionHeader() {
        return finalSolutionHeader;
    }

    public void setFinalSolutionHeader(String finalSolutionHeader) {
        this.finalSolutionHeader = finalSolutionHeader;
    }
}
