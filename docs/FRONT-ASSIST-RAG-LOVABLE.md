# Prompt para Lovable — Ajustar Front do Assist RAG

Crie/ajuste a tela de assistência RAG para consumir a API local `support-knowledge-api`.

## Objetivo da tela

O usuário deve preencher um caso de suporte e receber:

- evidências encontradas na base;
- soluções encontradas em chamados semelhantes;
- análise do caso;
- próxima ação recomendada;
- perguntas a fazer ao usuário;
- rascunho de handoff para desenvolvimento;
- sistemas sugeridos.

## Endpoints

Base URL configurável:

```text
http://localhost:8080/api/v1
```

### 1. Carregar dropdown de sistemas

Ao abrir a tela, chamar:

```http
GET /systems
```

Resposta:

```json
[
  { "id": "MPCLOUD", "displayName": "MPCloud" },
  { "id": "ATENA", "displayName": "Atena" }
]
```

Usar `displayName` no label do dropdown e enviar `displayName` ou `id` em `casoAtual.sistemaDeclarado`.

Incluir opção vazia:

```text
Não informado / detectar automaticamente
```

### 2. Enviar caso para o assistente

Ao clicar em "Analisar caso", chamar:

```http
POST /assist
Content-Type: application/json
```

Payload:

```json
{
  "casoAtual": {
    "descricaoUsuario": "Texto digitado no campo descrição",
    "contextoAdicional": "Texto opcional digitado no campo contexto",
    "sistemaDeclarado": "MPCloud"
  },
  "topK": 10,
  "modo": "CITADO_OBRIGATORIO_PARA_SOLUCOES",
  "modoBusca": "HYBRID"
}
```

Se o sistema não for informado, enviar:

```json
"sistemaDeclarado": null
```

## Campos do formulário

- `descricaoUsuario`: textarea obrigatório, label "Descrição do usuário".
- `contextoAdicional`: textarea opcional, label "Contexto adicional".
- `sistemaDeclarado`: select opcional, label "Sistema".
- `modo`: select avançado, padrão `CITADO_OBRIGATORIO_PARA_SOLUCOES`, com opção `PERMITIR_HIPOTESES`.
- `modoBusca`: select avançado, padrão `HYBRID`, com opções `TEXT`, `SEMANTIC`, `HYBRID`.
- `topK`: number avançado, padrão 10, mínimo 1, máximo 20.

Os campos avançados podem ficar recolhidos em "Opções avançadas".

## Renderização da resposta

Exemplo de resposta:

```json
{
  "queryUsadaNaBusca": "erro ao anexar documento",
  "modoBusca": "HYBRID",
  "topK": 10,
  "modo": "CITADO_OBRIGATORIO_PARA_SOLUCOES",
  "evidencias": [
    {
      "referencia": "E1",
      "ticketId": "100",
      "source": "DESCRICAO",
      "snippet": "Erro ao anexar documento na tela principal...",
      "solucaoRelacionada": {
        "snippet": "Orientado a limpar cache do navegador...",
        "citation": {
          "chunkId": "uuid",
          "ticketId": "100",
          "source": "SOLUCAO"
        }
      },
      "chunkId": "uuid",
      "scoreBusca": 0.42
    }
  ],
  "solucoesEncontradas": [
    {
      "resumo": "Orientar limpeza de cache do navegador.",
      "referencias": ["E1"]
    }
  ],
  "analiseDoCaso": "O caso é semelhante a chamados anteriores de anexação...",
  "proximaAcaoRecomendada": "Testar limpeza de cache e validar novamente o anexo.",
  "hipoteses": [],
  "perguntasAoUsuario": [
    "Pode enviar print da tela completa com a mensagem de erro?"
  ],
  "rascunhoHandoff": {
    "sistema": { "valor": "MPCloud", "origem": "DECLARADO", "referencia": null },
    "local": { "valor": "Tela de anexos", "origem": "CITACAO", "referencia": "E1" },
    "erro": { "valor": "Erro ao anexar documento", "origem": "CITACAO", "referencia": "E1" },
    "pedido": { "valor": null, "origem": "NAO_IDENTIFICADO", "referencia": null }
  },
  "sistemaSugerido": "MPCloud",
  "sistemasSugeridos": [
    { "id": "MPCLOUD", "displayName": "MPCloud", "score": 1.0, "confianca": "ALTA" }
  ],
  "metadados": {
    "modeloChat": "llama3.2:1b",
    "llmUsado": true,
    "aviso": null
  }
}
```

### Layout recomendado

Mostrar no topo:

- badge do sistema sugerido (`sistemaSugerido`);
- badge do modo de busca;
- aviso quando `metadados.llmUsado === false`.

Seções:

1. **Próxima ação recomendada**
   - Card em destaque usando `proximaAcaoRecomendada`.

2. **Soluções encontradas**
   - Lista de cards a partir de `solucoesEncontradas`.
   - Mostrar `resumo`.
   - Mostrar chips das `referencias` (`E1`, `E2`).
   - Ao clicar no chip, rolar até a evidência correspondente.

3. **Análise do caso**
   - Texto de `analiseDoCaso`.

4. **Perguntas ao usuário**
   - Checklist usando `perguntasAoUsuario`.

5. **Rascunho de handoff**
   - Mostrar campos:
     - Sistema
     - Local
     - Erro
     - Pedido
   - Exibir `origem` como badge:
     - `CITACAO`: verde
     - `DECLARADO`: azul
     - `HEURISTICA_SISTEMA`: amarelo
     - `INFERENCIA`: laranja
     - `NAO_IDENTIFICADO`: cinza
   - Criar botão "Copiar handoff" no formato:

```text
Sistema: ...
Local: ...
Erro: ...
Pedido: ...
```

6. **Evidências**
   - Card por item de `evidencias`.
   - Mostrar `referencia`, `ticketId`, `source`, `scoreBusca`, `snippet`.
   - Se existir `solucaoRelacionada`, mostrar um subcard "Solução do mesmo chamado" com `solucaoRelacionada.snippet`.

7. **Sistemas sugeridos**
   - Lista compacta com `displayName`, `score` e `confianca`.

## Regras de UX

- Enquanto carrega, mostrar loading.
- Se `/assist` falhar, mostrar erro amigável.
- Se `metadados.llmUsado` for `false`, mostrar alerta:
  "O Ollama falhou ou retornou JSON inválido. A resposta abaixo usa fallback com evidências e soluções citadas."
- Não esconder evidências quando não houver solução.
- Sempre exibir `queryUsadaNaBusca` em uma área recolhível "Detalhes técnicos".

## Observação

Não passar dados pela URL. O endpoint correto é `POST /assist` com JSON no body.
