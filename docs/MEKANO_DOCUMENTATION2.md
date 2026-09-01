# TECH CHALLENGE - OFICINA MECÂNICA

## Nome do Grupo: 36

## Autor(es)
* Conrado Moura (conrado.moura@icloud.com)    - iamconrad
* Elias Ferreira (eliaspsm2@gmail.com)        - elimafe5849
* Giovanni Brasil (giovannisbrasil@gmail.com) - Brasil_gi050794
* Roger Toledo (rogertoledo28@gmail.com)      - rogertoledo
* Victor Souza (victor.souza2210@gmail.com)   - victor_souza2210


## Link Documentação: 
  Miro        -https://miro.com/app/board/uXjVHD4vUnU=/?share_link_id=698036402431 
  Figma 	  - https://www.figma.com/board/wx7kmepPOFfWmXFR4geq0m/FIAP?node-id=0-1&t=L0gnz3Jyn21QlNyT-1
  Repositório - https://github.com/FIAP-SOFTWARE-ARCHITECTURE-TEAM/Mekano-API.git


# Relatório de Análise de Vulnerabilidades — Mekano API

**Repositório:** `FIAP-SOFTWARE-ARCHITECTURE-TEAM/Mekano-API`  
**URL:** `https://github.com/FIAP-SOFTWARE-ARCHITECTURE-TEAM/Mekano-API.git`  
**Arquivo base validado:** `RELATORIO_VULNERABILIDADES_MEKANO_API_PROFUNDO.md`  
**Data da revalidação:** 30/06/2026  
**Tipo:** Revisão cruzada do relatório anterior com os arquivos atuais do repositório público.

---

## 1. Conclusão executivaa

A análise anterior está **majoritariamente correta**, porém alguns itens precisam de ajuste de classificação:

- **Mantidos como achados relevantes:** CORS aberto, Swagger sempre incluído, PostgreSQL exposto, CI/CD sem gates de segurança, GitHub Actions sem pinagem por SHA, Docker build com `-DskipTests`, imagens sem digest e ausência de governança de segurança.
- **Reclassificados:** credenciais default, segredos via variável de ambiente, paginação e logging.
- **Corrigido como falso positivo:** `cache-config.yml` existe no módulo `mekano-infrastructure` e o módulo `mekano-rest` depende dele; portanto não deve ser tratado como vulnerabilidade confirmada.
- **Elevado de “a validar” para “confirmado por desenho”:** autorização de `UserResource`. O resource permite operações de criação, listagem, consulta e exclusão com `@RolesAllowed("user")`; a camada `UserService` não recebe identidade autenticada nem aplica ownership/autorização contextual. Isso deve ser tratado como **Broken Access Control** até que o endpoint seja restrito a `admin` ou implemente escopo por usuário.

---

## 2. Fontes revalidadas

Arquivos principais conferidos:

| Arquivo | Uso na validação |
|---|---|
| `pom.xml` | stack, módulos, Quarkus `3.36.0`, Java 17 |
| `.github/workflows/ci.yml` | ausência de SCA/SAST/container scan/SBOM e actions sem SHA |
| `mekano-rest/src/main/resources/api-config.yml` | CORS aberto |
| `mekano-rest/src/main/resources/openapi-config.yml` | Swagger UI sempre incluído |
| `mekano-rest/src/main/resources/datasource-config.yml` | defaults `DB_USER:mekano` e `DB_PASSWORD:mekano` |
| `docker-compose.yml` | defaults locais e exposição do PostgreSQL |
| `docker-compose.prod.yml` | exposição do PostgreSQL em produção e uso de env obrigatório |
| `mekano-rest/src/main/docker/Dockerfile.prod-jvm` | build com `-DskipTests` e imagens não pinadas por digest |
| `.env.example` | credenciais de exemplo `mekano/mekano` |
| `UserResource.java` | autorização ampla para operações sensíveis |
| `UserService.java` | ausência de checagem de ownership/escopo |
| `UserRepositoryImpl.java` | paginação com `normalizeSize(size)` limitada a 100 |
| `CreateUserRequest.java` | senha com apenas `@Size(min=6, max=128)` |
| `User.java` | `passwordHash` excluído do `toString()` |
| `.github/` | apenas `workflows/ci.yml`, sem Dependabot observado |
| `SECURITY.md` | não encontrado |

---

## 3. Matriz revisada dos achados

| ID original | Decisão após revalidação | Severidade revisada | Comentário |
|---|---|---:|---|
| VULN-001 | Mantida | Alta | CORS com `origins: "*"` confirmado. |
| VULN-002 | Mantida | Média/Alta | `swagger-ui.always-include: true` confirmado. |
| VULN-003 | Reclassificada | Média | Há defaults no datasource e compose local, mas `docker-compose.prod.yml` exige env com `${DB_USER:?}` e `${DB_PASSWORD:?}`. Não é credencial default direta no compose produtivo. |
| VULN-004 | Mantida | Alta | `docker-compose.prod.yml` publica PostgreSQL em `5432:5432`. |
| VULN-005 | Mantida | Alta | CI executa apenas build/test com Maven; não há SCA/SAST/container scan/SBOM. |
| VULN-006 | Mantida | Média/Alta | `actions/checkout@v4`, `actions/setup-java@v4`, `actions/cache@v4` sem pinagem por commit SHA. |
| VULN-007 | Mantida | Média/Alta | `Dockerfile.prod-jvm` empacota com `-DskipTests`. |
| VULN-008 | Mantida | Média | Imagens base usam tags, não digest SHA256. |
| VULN-009 | Reclassificada | Baixa/Média | Uso de variáveis de ambiente não é vulnerabilidade isolada; vira risco em produção sem secret manager/rotação/escopo. |
| VULN-010 | Elevada | Alta | Agora considero confirmado no desenho REST/application: `UserResource` usa role genérica `user` para CRUD sensível e `UserService` não valida ownership. |
| VULN-011 | Rebaixada | Baixa | O repository limita `size` a 100; não é DoS confirmado. Existe inconsistência no `UserResource`, que calcula `totalPages` com o `size` original. |
| VULN-012 | Reformulada | Média | O problema real é política de senha fraca: `@Size(min=6,max=128)` aceita `abc123`; não é apenas exemplo OpenAPI ruim. |
| VULN-013 | Mantida como risco a validar | Média | Healthcheck usa `/q/health/live`; health/metrics podem estar no mesmo 8080 se não houver restrição externa. Validar no deploy. |
| VULN-014 | Rebaixada | Baixa/Info | Há cuidado no domínio: `passwordHash` está excluído do `toString()`. Ainda falta política geral de redaction/masking em logs. |
| VULN-015 | Removida | N/A | Falso positivo: `cache-config.yml` existe em `mekano-infrastructure/src/main/resources`, e `mekano-rest` depende de `mekano-infrastructure`. |
| VULN-016 | Mantida | Média | Não foi observado `SECURITY.md`, Dependabot nem SBOM versionado/pipeline. |

---

## 4. Achados confirmados e recomendação final

### 4.1 CORS aberto — manter como Alta

**Evidência:** `api-config.yml` contém:

```yaml
quarkus:
  http:
    cors:
      enabled: true
      origins: "*"
```

**Validação:** confirmado.  
**Risco:** qualquer origem consegue chamar a API a partir do navegador, aumentando abuso de APIs autenticadas quando tokens são manipulados no cliente.  
**Correção:** separar por perfil e exigir `CORS_ALLOWED_ORIGINS` em produção.

```yaml
%prod:
  quarkus:
    http:
      cors:
        enabled: true
        origins: "${CORS_ALLOWED_ORIGINS}"
        methods: "GET,POST,PUT,PATCH,DELETE,OPTIONS"
        headers: "authorization,content-type,x-requested-with"
```

---

### 4.2 Swagger sempre incluído — manter como Média/Alta

**Evidência:** `openapi-config.yml` contém:

```yaml
quarkus:
  swagger-ui:
    always-include: true
```

**Validação:** confirmado.  
**Correção:** habilitar Swagger UI somente em desenvolvimento/testes.

```yaml
quarkus:
  swagger-ui:
    always-include: false

%dev:
  quarkus:
    swagger-ui:
      always-include: true
```

---

### 4.3 Credenciais default — reclassificar de Alta para Média

**Evidências:**

`datasource-config.yml`:

```yaml
username: ${DB_USER:mekano}
password: ${DB_PASSWORD:mekano}
```

`.env.example`:

```env
DB_USER=mekano
DB_PASSWORD=mekano
```

`docker-compose.yml` local:

```yaml
POSTGRES_USER: ${POSTGRES_USER:-mekano}
POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-mekano}
```

`docker-compose.prod.yml` usa forma obrigatória:

```yaml
POSTGRES_USER: ${DB_USER:?DB_USER é obrigatório}
POSTGRES_PASSWORD: ${DB_PASSWORD:?DB_PASSWORD é obrigatório}
```

**Validação:** parcialmente confirmado. O risco é real no datasource e no compose local, mas o compose produtivo já exige variáveis obrigatórias.  
**Correção:** remover fallback no perfil `%prod`.

```yaml
%prod:
  quarkus:
    datasource:
      username: ${DB_USER}
      password: ${DB_PASSWORD}
      jdbc:
        url: ${DB_URL}
```

---

### 4.4 PostgreSQL publicado em produção — manter como Alta

**Evidência:** `docker-compose.prod.yml` contém:

```yaml
ports:
  - "5432:5432"
```

**Validação:** confirmado.  
**Correção:** remover publicação da porta em produção. O serviço `mekano` já acessa o banco pela rede interna Docker.

```yaml
services:
  postgres:
    expose:
      - "5432"
    # remover ports em produção
```

---

### 4.5 CI/CD sem gates de segurança — manter como Alta

**Evidência:** `.github/workflows/ci.yml` executa apenas:

```yaml
- name: Build and test
  run: mvn -B -ntp verify -pl mekano-rest -am
```

**Validação:** confirmado. Não foram observados OWASP Dependency-Check, CodeQL, Semgrep, Trivy, SBOM, upload SARIF ou política explícita de permissões.  
**Correção mínima:** adicionar gates:

```yaml
permissions:
  contents: read
  security-events: write

steps:
  - uses: actions/checkout@<SHA>

  - name: Build and test
    run: mvn -B -ntp verify -pl mekano-rest -am

  - name: OWASP Dependency-Check
    run: mvn -B -ntp org.owasp:dependency-check-maven:check -DfailBuildOnCVSS=7

  - name: Generate SBOM
    run: mvn -B -ntp org.cyclonedx:cyclonedx-maven-plugin:makeAggregateBom

  - name: Trivy filesystem scan
    run: trivy fs --exit-code 1 --severity HIGH,CRITICAL .
```

---

### 4.6 GitHub Actions sem pinagem por SHA — manter como Média/Alta

**Evidência:** workflow usa actions versionadas por tag:

```yaml
uses: actions/checkout@v4
uses: actions/setup-java@v4
uses: actions/cache@v4
```

**Validação:** confirmado.  
**Correção:** trocar tags por SHA completo do commit da action.

---

### 4.7 Docker build com `-DskipTests` — manter como Média/Alta

**Evidência:** `Dockerfile.prod-jvm` contém:

```dockerfile
RUN ./mvnw package -pl mekano-rest -am -DskipTests -B -q
```

**Validação:** confirmado.  
**Risco:** build de imagem pode produzir artefato sem validação se alguém executar Docker build fora do CI ou se o CI for alterado.  
**Correção:** remover `-DskipTests` ou garantir que a imagem só seja construída após artefato testado.

---

### 4.8 Imagens Docker sem digest — manter como Média

**Evidência:** Dockerfiles/compose usam tags como:

```dockerfile
FROM registry.access.redhat.com/ubi9/openjdk-17:latest-or-tag
FROM registry.access.redhat.com/ubi9/openjdk-17-runtime:1.24
```

**Validação:** confirmado.  
**Correção:** pinagem por digest:

```dockerfile
FROM registry.access.redhat.com/ubi9/openjdk-17-runtime@sha256:<digest>
```

---

### 4.9 Segredos via env — reclassificar para hardening

**Validação:** o uso de variáveis de ambiente por si só não é vulnerabilidade. O risco surge em produção quando não há evidência de:

- secret manager;
- rotação de credenciais;
- separação por ambiente;
- política de acesso mínima;
- não exposição em logs/pipeline.

**Correção recomendada:** para produção, usar Kubernetes Secret + External Secrets, AWS Secrets Manager, Vault ou solução equivalente.

---

### 4.10 Broken Access Control em UserResource — elevar para Alta confirmada por desenho

**Evidência em `UserResource.java`:**

```java
@Path("/users")
@RequestScoped
@RolesAllowed("user")
public class UserResource {
```

A mesma classe expõe:

- `POST /users` — criação;
- `GET /users` — listagem de todos os usuários ativos;
- `GET /users/{id}` — consulta por UUID;
- `DELETE /users/{id}` — soft delete.

**Evidência em `UserService.java`:** métodos como `findUserById(UUID id)`, `findAllUsers(...)` e `deleteUser(UUID id)` não recebem usuário autenticado, role contextual, tenant, cliente, nem qualquer objeto de autorização.

**Validação:** o achado passa de “a validar” para **confirmado por desenho**. Ainda não é prova de exploração runtime, mas o desenho atual é inseguro para produção.

**Correção imediata sugerida:**

```java
@Path("/users")
@RequestScoped
@RolesAllowed("admin")
public class UserResource {
```

Ou, se clientes/usuários comuns precisarem acessar o próprio perfil:

```java
@GET
@Path("/me")
@RolesAllowed({"user", "cliente", "admin"})
public Response getMe(@Context SecurityContext securityContext) {
    // buscar pelo subject do JWT, nunca por UUID arbitrário informado pelo cliente
}
```

---

### 4.11 Paginação — rebaixar para Baixa

**Evidência:** `UserResource` recebe `size` direto e calcula `totalPages` com o valor original:

```java
int totalPages = (int) Math.ceil((double) total / size);
```

Mas `UserRepositoryImpl` limita o tamanho:

```java
private static int normalizeSize(int size) {
    if (size <= 0) {
        return 10;
    }
    return Math.min(size, 100);
}
```

**Validação:** não é DoS confirmado, porque a query é limitada no repository.  
**Problema restante:** inconsistência de resposta quando `size <= 0` ou `size > 100`.  
**Correção:** normalizar também no resource ou retornar `400` para `size` inválido.

---

### 4.12 Política de senha fraca — reformular como Média

**Evidência:** `CreateUserRequest.java` usa apenas:

```java
@Size(min = 6, max = 128)
private String password;
```

O exemplo `abc123` é aceito pela validação atual.  
**Validação:** o problema não é só o exemplo; é a política de senha permissiva.  
**Correção:** exigir tamanho maior e/ou política com zxcvbn/denylist/rate limit. Exemplo mínimo:

```java
@Size(min = 12, max = 128)
@Pattern(
  regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{12,}$",
  message = "Senha deve ter pelo menos 12 caracteres, letras maiúsculas, minúsculas e números"
)
private String password;
```

Para UX melhor, preferir validação por força/denylist em vez de apenas regex.

---

### 4.13 Health/metrics expostos — manter como risco a validar

**Evidência:** o projeto inclui `quarkus-smallrye-health` e `quarkus-micrometer-registry-prometheus`; o compose usa healthcheck em `/q/health/live`.  
**Validação:** risco depende do deploy. Se o 8080 estiver público, endpoints `/q/*` podem ser acessíveis junto com a API.  
**Correção:** separar porta de management, bloquear `/q/*` externamente no gateway/proxy ou restringir por rede interna.

---

### 4.14 Logging sem redaction — rebaixar para Baixa/Info

**Evidência favorável:** `User.java` exclui `passwordHash` do `toString()`.  
**Validação:** o relatório anterior estava um pouco forte. Existe cuidado com hash de senha, mas ainda não foi observada política geral de redaction para tokens, headers `Authorization`, CPF/email sensível, payloads e exceções.

---

### 4.15 `cache-config.yml` — remover como vulnerabilidade

**Validação:** falso positivo.  
O arquivo existe em:

```text
mekano-infrastructure/src/main/resources/cache-config.yml
```

Além disso, `mekano-rest/pom.xml` depende de `mekano-infrastructure`, então o recurso tende a ficar disponível no classpath. Recomenda-se apenas validar em runtime se `quarkus.config.locations=cache-config.yml` carrega corretamente a partir de dependência modular.

---

### 4.16 Governança de segurança — manter como Média

**Validação:** confirmado como gap de governança. Não foi observado:

- `SECURITY.md`;
- `.github/dependabot.yml`;
- SBOM versionado ou gerado no CI;
- política de divulgação de vulnerabilidades;
- upload SARIF;
- CodeQL/Semgrep/Trivy no workflow.

---

## 5. Nova priorização recomendada

### P0 — Bloqueadores antes de produção

1. Restringir `UserResource` para `admin` ou implementar `/users/me` com ownership.
2. Remover `ports: "5432:5432"` do `docker-compose.prod.yml`.
3. Desabilitar Swagger UI em produção.
4. Trocar CORS `*` por allowlist de origens.
5. Adicionar gates mínimos no CI: OWASP Dependency-Check, Trivy, SBOM e permissões mínimas.

### P1 — Corrigir em seguida

1. Remover fallback de credenciais no perfil `%prod`.
2. Remover `-DskipTests` do Dockerfile produtivo ou garantir build apenas pós-CI.
3. Pinagem de GitHub Actions por SHA.
4. Pinagem de imagens por digest SHA256.
5. Fortalecer política de senha.

### P2 — Hardening e governança

1. Criar `SECURITY.md`.
2. Criar `.github/dependabot.yml`.
3. Configurar CodeQL ou Semgrep.
4. Definir redaction de logs.
5. Restringir `/q/health`, `/q/metrics` e demais endpoints operacionais.

---

## 6. Resultado final da validação

| Categoria | Quantidade |
|---|---:|
| Achados mantidos | 9 |
| Achados reclassificados/reformulados | 5 |
| Achados elevados | 1 |
| Achados removidos como falso positivo | 1 |

**Veredito:** o relatório anterior é válido como base, mas a versão revisada deve substituir a matriz original para evitar falso positivo em `cache-config.yml`, reduzir exageros em credenciais/logging/paginação e dar mais peso ao problema real de autorização em `UserResource`.
