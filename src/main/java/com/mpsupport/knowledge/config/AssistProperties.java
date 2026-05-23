package com.mpsupport.knowledge.config;

import com.mpsupport.knowledge.dto.AssistMode;
import com.mpsupport.knowledge.dto.SearchMode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.assist")
public class AssistProperties {

    private int defaultTopK = 10;
    private int maxEvidencesForLlm = 12;
    private AssistMode defaultModo = AssistMode.CITADO_OBRIGATORIO_PARA_SOLUCOES;
    private SearchMode defaultModoBusca = SearchMode.HYBRID;

    public int getDefaultTopK() {
        return defaultTopK;
    }

    public void setDefaultTopK(int defaultTopK) {
        this.defaultTopK = defaultTopK;
    }

    public int getMaxEvidencesForLlm() {
        return maxEvidencesForLlm;
    }

    public void setMaxEvidencesForLlm(int maxEvidencesForLlm) {
        this.maxEvidencesForLlm = maxEvidencesForLlm;
    }

    public AssistMode getDefaultModo() {
        return defaultModo;
    }

    public void setDefaultModo(AssistMode defaultModo) {
        this.defaultModo = defaultModo;
    }

    public SearchMode getDefaultModoBusca() {
        return defaultModoBusca;
    }

    public void setDefaultModoBusca(SearchMode defaultModoBusca) {
        this.defaultModoBusca = defaultModoBusca;
    }
}
