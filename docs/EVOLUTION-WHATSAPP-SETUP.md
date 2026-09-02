# Configuração do WhatsApp (Evolution API)

Guia para conectar um número de WhatsApp à instância Evolution API, permitindo que o
Mekano envie notificações de orçamento e receba respostas SIM/NÃO por webhook.

## Índice

1. [Pré-requisitos](#1-pré-requisitos)
2. [Recuperar credenciais](#2-recuperar-credenciais)
3. [Criar a instância](#3-criar-a-instância)
4. [Conectar o WhatsApp (pairing code)](#4-conectar-o-whatsapp-pairing-code)
5. [Verificar conexão](#5-verificar-conexão)
6. [Fallback: QR code via PNG](#6-fallback-qr-code-via-png)
7. [Problemas conhecidos](#7-problemas-conhecidos)
8. [Smoke test](#8-smoke-test)
9. [Persistência da sessão](#9-persistência-da-sessão)

---

## 1. Pré-requisitos

- Stack rodando: EKS (Kubernetes) **ou** Docker Compose local (`docker compose up -d`)
- Variáveis de ambiente configuradas:

| Variável | Descrição |
|----------|-----------|
| `EVOLUTION_API_KEY` | Chave global da Evolution API (autentica as chamadas REST) |
| `EVOLUTION_INSTANCE_NAME` | Nome da instância (padrão: `mekano`) |
| `EVOLUTION_WEBHOOK_TOKEN` | Token validado pelo webhook (`x-webhook-token`) |
| `EVOLUTION_SERVER_URL` | URL pública da Evolution API (para QR/pairing) |

No ambiente **local** o `.env` já contém defaults:

```bash
cp .env.example .env
# Edite com valores seguros para produção
```

No **EKS** os secrets são injetados pelo pipeline CD a partir dos GitHub Secrets.

---

## 2. Recuperar credenciais

### EKS (Kubernetes)

```bash
# Chave global (AUTHENTICATION_API_KEY)
kubectl -n mekano-system get secret evolution-secret \
  -o jsonpath='{.data.AUTHENTICATION_API_KEY}' | base64 -d; echo

# Token do webhook
kubectl -n mekano-system get secret mekano-secret \
  -o jsonpath='{.data.EVOLUTION_WEBHOOK_TOKEN}' | base64 -d; echo
```

### Docker Compose (local)

```bash
grep EVOLUTION_API_KEY .env | cut -d= -f2
grep EVOLUTION_WEBHOOK_TOKEN .env | cut -d= -f2
```

> Guarde os dois valores — você vai usá-los em todos os passos seguintes.

---

## 3. Criar a instância

Exporte as variáveis (bash) ou use `set` (cmd):

```bash
export EVOLUTION_KEY="<AUTHENTICATION_API_KEY>"
export WEBHOOK_TOKEN="<EVOLUTION_WEBHOOK_TOKEN>"
```

Defina a URL base conforme seu ambiente:

| Ambiente | URL base |
|----------|----------|
| EKS (via ingress) | `http://<elb>/evolution` |
| Docker Compose local | `http://localhost:5033` |

```bash
export BASE="http://localhost:5033"        # local
# export BASE="http://meu-elb/evolution"   # EKS
```

**Criar a instância com `qrcode: false`.** Isso é importante: o pairing code não funciona
se o QR já tiver sido gerado automaticamente no `create`.

```bash
curl -s -X POST "$BASE/instance/create" \
  -H "apikey: $EVOLUTION_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "instanceName": "mekano",
    "integration": "WHATSAPP-BAILEYS",
    "qrcode": false,
    "token": "'"$WEBHOOK_TOKEN"'",
    "webhook": {
      "enabled": true,
      "url": "http://mekano:8080/api/v1/webhooks/evolution",
      "byEvents": false,
      "base64": false,
      "events": ["MESSAGES_UPSERT"],
      "headers": { "x-webhook-token": "'"$WEBHOOK_TOKEN"'" }
    }
  }'
```

> **Nota:** O campo `token` deve ser igual ao `EVOLUTION_WEBHOOK_TOKEN`. Isso faz o
> fallback de autenticação funcionar: a Evolution API envia o `apikey` da instância
> no corpo do webhook, e o Mekano valida esse campo contra `evolution.webhook-token`.
> (Decisão CR-02 — ver `AGENTS.md`.)

Response esperado: `{"instance":{"status":"close",...}}`.

Se preferir PowerShell (Windows):

```powershell
$body = @{
  instanceName = "mekano"; integration = "WHATSAPP-BAILEYS"; qrcode = $false
  token = $env:WEBHOOK_TOKEN
  webhook = @{ enabled = $true; url = "http://mekano:8080/api/v1/webhooks/evolution"
               byEvents = $false; base64 = $false; events = @("MESSAGES_UPSERT")
               headers = @{ "x-webhook-token" = $env:WEBHOOK_TOKEN } }
} | ConvertTo-Json -Depth 5

Invoke-RestMethod -Method Post -Uri "$BASE/instance/create" `
  -Headers @{ apikey = $env:EVOLUTION_KEY } `
  -ContentType "application/json" -Body $body
```

---

## 4. Conectar o WhatsApp (pairing code)

Com a instância em estado `close`, solicite o **pairing code**. O número deve estar
no formato E.164: `55` + DDD + 9 dígitos (ex.: `5551999999999`).

```bash
curl -s "$BASE/instance/connect/mekano?number=55DDDNUMEROCOMPLETO" \
  -H "apikey: $EVOLUTION_KEY"
```

Response esperado:

```json
{"pairingCode":"VZLR6Z1Z","code":"...","base64":"...","count":1}
```

O código de 8 caracteres (`VZLR6Z1Z`) expira em **~40 segundos**. Imediatamente:

1. Abra o WhatsApp no celular do número informado
2. **Menu** (⋮) → **Aparelhos conectados**
3. **Conectar pelo número de telefone**
4. Digite o código de 8 caracteres

Se expirar, basta chamar o mesmo `curl` novamente — não precisa recriar a instância.

---

## 5. Verificar conexão

```bash
curl -s "$BASE/instance/connectionState/mekano" -H "apikey: $EVOLUTION_KEY"
```

Esperado:

```json
{"instance":{"instanceName":"mekano","state":"open"}}
```

---

## 6. Fallback: QR code via PNG

Se o `pairingCode` vier `null` (causas: número sem WhatsApp, rate limit da Meta,
ou QR já em andamento), você pode salvar a imagem do QR e escanear.

### No Windows (PowerShell)

Um comando só que chama o connect, extrai o base64 e abre a imagem:

```powershell
$r = curl.exe -s "$BASE/instance/connect/mekano" -H "apikey: $EVOLUTION_KEY" | ConvertFrom-Json
$b = $r.qrcode.base64 -replace '^data:image/png;base64,',''
[IO.File]::WriteAllBytes("$env:TEMP\qr-mekano.png", [Convert]::FromBase64String($b))
Invoke-Item "$env:TEMP\qr-mekano.png"
```

### No Linux/macOS

```bash
curl -s "$BASE/instance/connect/mekano" -H "apikey: $EVOLUTION_KEY" \
  | jq -r '.qrcode.base64' \
  | sed 's/^data:image\/png;base64,//' \
  | base64 -d > /tmp/qr-mekano.png && open /tmp/qr-mekano.png
```

Escanee o QR com WhatsApp → **Aparelhos conectados** (válido ~40s; se expirar, repita).

---

## 7. Problemas conhecidos

| Sintoma | Causa | Solução |
|---------|-------|---------|
| `pairingCode: null` no connect | Instância foi criada com `qrcode: true` e já disparou o fluxo de QR | Delete (`DELETE /instance/delete/{inst}`) e recrie com `qrcode: false` |
| `pairingCode: null` mesmo com `qrcode: false` | Número informado não tem WhatsApp, ou a Meta está em rate limit para pairing | Tente com outro número válido, ou use o fallback do QR (seção 6) |
| `Cannot DELETE /instance/mekano` | Endpoint errado — não é `DELETE /instance/{inst}` | Use `DELETE /instance/delete/{inst}` |
| Webhook retorna 401 | Token `x-webhook-token` divergente entre a instância e o config do Mekano | Verifique se o token na criação da instância e o `EVOLUTION_WEBHOOK_TOKEN` são o mesmo valor |
| Instância aparece `close` após connect | QR/pairing expirou antes do escaneamento/digitação | Chame o connect novamente e escaneie/digite imediatamente |
| Mensagem não chega no WhatsApp | Instância não conectada, ou `EVOLUTION_API_URL` no Mekano aponta para URL errada | Verifique `connectionState` (deve ser `open`) e logs: `kubectl logs deploy/mekano \| grep -i evolution` |
| Mensagem chega mas "SIM" não aprova | Token do webhook inválido ou não configurado na instância | Confirme que `webhook.headers.x-webhook-token` foi enviado no create e bate com `EVOLUTION_WEBHOOK_TOKEN` |

---

## 8. Smoke test

Depois de confirmar `"state":"open"`, faça o teste ponta a ponta:

1. Cadastre um cliente com o celular que você conectou (`POST /api/v1/clientes`)
2. Crie uma OS para esse cliente + veículo
3. Finalize o diagnóstico da OS (gera orçamento em status `PENDENTE`)
4. O Mekano envia a notificação via WhatsApp: "Olá {nome}, seu orçamento ficou em R$ X..."
5. No WhatsApp, responda **SIM**
6. O webhook é entregue a `POST /api/v1/webhooks/evolution`, validado pelo token,
   e o orçamento é aprovado (OS avança para `EM_EXECUCAO`)

Para diagnosticar falhas:

```bash
# Logs do Mekano — busca mensagens de notificação e webhook
kubectl -n mekano-system logs deploy/mekano | grep -i "evolution\|webhook\|whatsapp"

# Logs da Evolution API — busca delivery e erros
kubectl -n mekano-system logs deploy/evolution-api --tail 100
```

---

## 9. Persistência da sessão

A sessão do WhatsApp (Baileys) é armazenada em dois lugares:

| Local | Detalhe |
|-------|---------|
| PostgreSQL (db `evolution`) | Dados estruturados da instância, mensagens, contatos |
| PVC `evolution-instances` | Arquivos de sessão Baileys (`/evolution/instances/`) |

A sessão sobrevive a restarts do pod e da aplicação. O QR/pairing code só é
necessário na **primeira conexão** — após o primeiro `"state":"open"`, reconexões
são automáticas.

Para reconectar forçadamente (ex.: resetar a instância do WhatsApp):

```bash
curl -s -X DELETE "$BASE/instance/logout/mekano" -H "apikey: $EVOLUTION_KEY"
# Depois reconecte (QR ou pairing code) — ver seção 4 ou 6
```

---

*Última atualização: 2026-09-02*