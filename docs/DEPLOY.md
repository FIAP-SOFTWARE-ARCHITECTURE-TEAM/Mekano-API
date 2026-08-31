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
| `mekano` | 8080 | API REST (Quarkus, container na porta 8080) |
| `evolution-api` | 5033 | Evolution API (gateway WhatsApp) |

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
EVOLUTION_SERVER_URL=https://<host-publico>:5033
EVOLUTION_WEBHOOK_TOKEN=<token_webhook>
```

### 2. Gerar e instalar chaves JWT (produção)

O compose de produção usa as chaves montadas do host em `/etc/mekano/secrets`:

```bash
mkdir -p /etc/mekano/secrets
openssl genpkey -algorithm Ed25519 -out /etc/mekano/secrets/privatekey.pem
openssl pkey -in /etc/mekano/secrets/privatekey.pem -pubout -out /etc/mekano/secrets/publicKey.pem
chmod 640 /etc/mekano/secrets/privatekey.pem
```

### 3. Subir os serviços

```bash
docker compose -f docker-compose.prod.yml up -d
```

### 4. Verificar saúde

```bash
docker compose -f docker-compose.prod.yml ps
curl http://localhost:8080/q/health/live
```

## Comandos

| Comando | Descrição |
|---------|-----------|
| `docker compose -f docker-compose.prod.yml up -d` | Subir todos os serviços (detached) |
| `docker compose -f docker-compose.prod.yml down` | Parar e remover containers |
| `docker compose -f docker-compose.prod.yml down -v` | Parar, remover containers E volumes (⚠ destrói dados) |
| `docker compose -f docker-compose.prod.yml logs -f mekano` | Logs em tempo real da API |
| `docker compose -f docker-compose.prod.yml logs -f evolution-api` | Logs em tempo real da Evolution API |
| `docker compose -f docker-compose.prod.yml ps` | Status dos serviços |
| `docker compose -f docker-compose.prod.yml restart mekano` | Reiniciar apenas a API |

## Variáveis de Ambiente

### Obrigatórias

| Variável | Descrição | Default |
|----------|-----------|---------|
| `DB_USER` | Usuário PostgreSQL | — |
| `DB_PASSWORD` | Senha PostgreSQL | — |
| `EVOLUTION_API_KEY` | Chave global da Evolution API | — |
| `EVOLUTION_SERVER_URL` | URL pública da Evolution API | — |
| `EVOLUTION_WEBHOOK_TOKEN` | Token do webhook (x-webhook-token) | — |

### Opcionais

| Variável | Descrição | Default |
|----------|-----------|---------|
| `EVOLUTION_API_URL` | URL interna da Evolution API | `http://evolution-api:5033` |
| `EVOLUTION_INSTANCE_NAME` | Nome da instância WhatsApp | `mekano` |
| `EVOLUTION_INSTANCE_TOKEN` | Token usado na criação da instância | — |

## Profiles

| Profile | Uso | Banco |
|---------|-----|-------|
| `%dev` | Desenvolvimento local (quarkus:dev) | PostgreSQL localhost:5433 ou H2 |
| `%test` | Testes automatizados | H2 in-memory |
| `%prod` | Docker Compose / deploy | PostgreSQL no container |

## Troubleshooting

### API não sobe (exit code 1)

```bash
docker compose -f docker-compose.prod.yml logs mekano | tail -50
```

Causas comuns:
- Chaves JWT não encontradas → verificar `/etc/mekano/secrets`
- PostgreSQL não pronto → aguardar healthcheck
- Flyway migration falhou → verificar logs do postgres

### Evolution API não conecta

```bash
docker compose -f docker-compose.prod.yml logs evolution-api | tail -50
```

Causas comuns:
- Banco `evolution` não criado → verificar a seção abaixo sobre volumes existentes
- `AUTHENTICATION_API_KEY` não configurado → verificar `.env`

### Banco da Evolution em volume existente

O arquivo `init-evolution-db.sql` só é executado na primeira inicialização do volume
`postgres_data`. Se o volume já existia antes desta configuração, crie o banco uma vez:

```bash
docker compose -f docker-compose.prod.yml exec postgres \
  psql -U "$DB_USER" -d mekano -c 'CREATE DATABASE evolution;'
```

### Porta 5432 já em uso

```bash
# Parar other PostgreSQL
docker stop <container_name>
# Ou mudar a porta no docker-compose.prod.yml
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
