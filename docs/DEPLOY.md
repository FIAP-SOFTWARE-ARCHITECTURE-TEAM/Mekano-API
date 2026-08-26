# Deploy — Mekano API

Guia de deploy local e homologação via Docker Compose.

## Pré-requisitos

- Docker Engine 24+
- Docker Compose v2+
- 4 GB de RAM livre (mínimo para todos os serviços)

## Serviços

| Serviço | Porta | Descrição |
|---------|-------|-----------|
| `postgres` | 5432 | PostgreSQL 16 (banco principal) |
| `mekano` | 8080 | API REST (Quarkus) |
| `evolution-api` | 5033 | Evolution API (gateway WhatsApp) |
| `evolution-postgres` | — | PostgreSQL para Evolution API |
| `evolution-redis` | — | Cache para Evolution API |
| `keygen` | — | Geração de chaves JWT (roda uma vez) |

## Setup Inicial

### 1. Copiar variáveis de ambiente

```bash
cp .env.example .env
```

Edite `.env` com valores seguros para produção:

```env
DB_USER=mekano
DB_PASSWORD=<senha_forte>
EVOLUTION_API_KEY=<chave_api_global>
EVOLUTION_INSTANCE_NAME=mekano
EVOLUTION_INSTANCE_TOKEN=<token_instancia>
EVOLUTION_DB_PASSWORD=<senha_forte_evolution>
EVOLUTION_WEBHOOK_TOKEN=<token_webhook>
```

### 2. Gerar chaves JWT (produção)

Se não existir `~/.mekano/secrets/`, o container `keygen` gera automaticamente ao subir. Para gerar manualmente:

```bash
mkdir -p ~/.mekano/secrets
openssl genpkey -algorithm Ed25519 -out ~/.mekano/secrets/privatekey.pem
openssl pkey -in ~/.mekano/secrets/privatekey.pem -pubout -out ~/.mekano/secrets/publicKey.pem
chmod 600 ~/.mekano/secrets/privatekey.pem
```

### 3. Subir os serviços

```bash
docker compose up -d
```

### 4. Verificar saúde

```bash
docker compose ps
curl http://localhost:8080/q/health/live
```

## Comandos

| Comando | Descrição |
|---------|-----------|
| `docker compose up -d` | Subir todos os serviços (detached) |
| `docker compose down` | Parar e remover containers |
| `docker compose down -v` | Parar, remover containers E volumes (⚠ destrói dados) |
| `docker compose logs -f mekano` | Logs em tempo real da API |
| `docker compose logs -f evolution-api` | Logs em tempo real da Evolution API |
| `docker compose ps` | Status dos serviços |
| `docker compose restart mekano` | Reiniciar apenas a API |

## Variáveis de Ambiente

### Obrigatórias

| Variável | Descrição | Default |
|----------|-----------|---------|
| `DB_USER` | Usuário PostgreSQL | `mekano` |
| `DB_PASSWORD` | Senha PostgreSQL | `mekano` |
| `EVOLUTION_API_KEY` | Chave global da Evolution API | — |
| `EVOLUTION_INSTANCE_NAME` | Nome da instância WhatsApp | `mekano` |
| `EVOLUTION_INSTANCE_TOKEN` | Token da instância | — |
| `EVOLUTION_DB_PASSWORD` | Senha PostgreSQL da Evolution | `evolution` |
| `EVOLUTION_WEBHOOK_TOKEN` | Token do webhook (x-webhook-token) | — |

### Opcionais

| Variável | Descrição | Default |
|----------|-----------|---------|
| `EVOLUTION_API_URL` | URL interna da Evolution API | `http://evolution-api:5033` |

## Profiles

| Profile | Uso | Banco |
|---------|-----|-------|
| `%dev` | Desenvolvimento local (quarkus:dev) | PostgreSQL localhost:5433 ou H2 |
| `%test` | Testes automatizados | H2 in-memory |
| `%prod` | Docker Compose / deploy | PostgreSQL no container |

## Troubleshooting

### API não sobe (exit code 1)

```bash
docker compose logs mekano | tail -50
```

Causas comuns:
- Chaves JWT não encontradas → verificar volume `mekano_secrets`
- PostgreSQL não pronto → aguardar healthcheck
- Flyway migration falhou → verificar logs do postgres

### Evolution API não conecta

```bash
docker compose logs evolution-api | tail -50
```

Causas comuns:
- PostgreSQL da Evolution não pronto → verificar `evolution-postgres`
- Redis não pronto → verificar `evolution-redis`
- `AUTHENTICATION_API_KEY` não configurado → verificar `.env`

### Porta 5432 já em uso

```bash
# Parar other PostgreSQL
docker stop <container_name>
# Ou mudar a porta no docker-compose.yml
ports:
  - "5433:5432"
```

### Limpar tudo (reset completo)

```bash
docker compose down -v
docker compose up -d --build
```

## Backup e Restore

Ver `scripts/backup-db.sh` para automação de backup do banco de dados.

### Backup manual

```bash
docker compose exec postgres pg_dump -U mekano mekano > backup_$(date +%Y%m%d).sql
```

### Restore manual

```bash
cat backup_20260101.sql | docker compose exec -T postgres psql -U mekano mekano
```
