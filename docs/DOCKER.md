# PostgreSQL com pgvector no Docker

## Subir o banco

Na pasta do projeto:

```powershell
docker compose up -d
```

Aguarde o healthcheck (`docker compose ps`).

## Conectar a API

Credenciais do container: usuário **`postgres`**, senha **`postgres`**.

### IntelliJ / Cursor

1. Copie o exemplo: `copy .env.example .env` (senha `postgres`).
2. Perfis ativos: **`docker`** (ou **`local,docker`**).
3. Se ainda falhar senha: em *Run Configuration* → *Environment variables*, adicione `DB_PASSWORD=postgres`.

O Spring importa o arquivo `.env` da raiz (`spring.config.import` no `application.yml`).

### Front Lovable / Vite: CORS bloqueado

Se o console mostrar `blocked by CORS policy` ao chamar `localhost:8080` a partir de `*.lovable.app` ou `localhost:5173`:

1. Reinicie a API após atualizar o projeto (há `WebCorsConfig` com origens permitidas).
2. A API precisa estar rodando **na sua máquina** — o preview Lovable no navegador ainda usa **seu** `localhost:8080`.
3. Preview publicado na nuvem **sem** API pública continua offline; use `npm run dev` local ou túnel (ngrok).

### Erro típico: "password authentication failed"

| Causa | O que fazer |
|-------|-------------|
| Perfil só `local` com senha antiga no YAML | Ative perfil **`docker`** ou use `.env` com `DB_PASSWORD=postgres` |
| API aponta para Postgres **Windows** na 5432, não o Docker | Pare o serviço PostgreSQL local; só o container deve usar a porta 5432 |
| Volume Docker criado com outra senha | `docker compose down -v` e `docker compose up -d` (apaga dados do volume) |

Teste rápido no container:

```powershell
docker exec -it support-knowledge-postgres psql -U postgres -d support_knowledge -c "SELECT current_user;"
```

## Migrar dados do Postgres local (opcional)

Se já tinha dados no Postgres sem Docker:

```powershell
pg_dump -U postgres -h localhost -p 5432 -d support_knowledge -Fc -f backup.dump
docker compose up -d
pg_restore -U postgres -h localhost -p 5432 -d support_knowledge --clean backup.dump
```

(Ajuste host/porta se o Postgres local ainda estiver na 5432 — pare o serviço local antes para liberar a porta.)

## Conferir pgvector

```powershell
docker exec -it support-knowledge-postgres psql -U postgres -d support_knowledge -c "SELECT extname FROM pg_extension WHERE extname = 'vector';"
```

A extensão também é criada pelo Flyway (`V3__embeddings.sql`) na subida da API.

## Flyway: versão V3 duplicada

O projeto deve ter **apenas** `V3__embeddings.sql`. Se existiu `V3__enable_pgvector.sql`, foi removido (conteúdo já incluso no V3 principal).

Se a API falhar com erro de migração duplicada ou checksum:

```powershell
docker exec -it support-knowledge-postgres psql -U postgres -d support_knowledge -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
```

Se `V3` foi aplicada com o arquivo antigo e você alterou o SQL, pode ser necessário `flyway repair` ou ajuste manual — descreva o erro do log se precisar de ajuda.

## Ollama (separado)

Ollama continua na máquina host (`http://localhost:11434`), não no Docker deste compose.
