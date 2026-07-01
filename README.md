# Mekano API

API REST para gestão de oficina mecânica — ordens de serviço, clientes, veículos, estoque e faturamento.

## Pré-requisitos

- **Java 17** (configurado via `JAVA_HOME`)
- **Docker Desktop** ou **Rancher Desktop** instalado e em execução
- **Maven Wrapper** (`./mvnw` incluso no projeto — não precisa instalar Maven)

## Como inicializar o projeto

### Dev (Docker Compose + keygen automático)

```bash
# 1. Build + sobe tudo (postgres, keygen, app)
docker compose build && docker compose up -d

# 2. A app estará em http://localhost:8080
#    Swagger UI: http://localhost:8080/q/swagger-ui
```

> O `Dockerfile.jvm` é multi-stage: compila o JAR internamente (sem precisar de Maven instalado na máquina host) e depois gera a imagem runtime. O serviço `keygen` gera o par de chaves Ed25519 na primeira execução.

### Prod (bind mount da chave do host)

Use `docker-compose.prod.yml`:

```bash
# 1. Gerar chave no host (uma vez)
./mekano-rest/keygen.sh

# 2. Copiar a chave privada para /etc/mekano/secrets/
cp ~/.mekano/secrets/privatekey.pem /etc/mekano/secrets/
cp ~/.mekano/secrets/publicKey.pem /etc/mekano/secrets/

# 3. Subir em produção
docker compose -f docker-compose.prod.yml up -d
```

### Dev local (sem Docker)

```bash
# 1. Suba apenas o banco
docker compose up -d postgres

# 2. Gere as chaves JWT (necessário apenas uma vez)
./mekano-rest/keygen.sh

# 3. Inicie o Quarkus em modo dev
./mvnw quarkus:dev
```

> O Quarkus Dev UI estará disponível em <http://localhost:8080/q/dev/>.
> Flyway, JWT e CORS são configurados automaticamente.

## Build completo e testes

Para verificar se o projeto compila 100% e todos os testes passam:

```powershell
./mvnw -B -ntp verify -pl mekano-rest -am
```

## Autenticação JWT

O projeto utiliza **Ed25519 (EdDSA)** para assinar e verificar tokens via `quarkus-smallrye-jwt`.

- A chave **pública** (`publicKey.pem`) fica no classpath (segura para commit)
- A chave **privada** (`privatekey.pem`) **nunca** entra na imagem Docker nem no repositório
- Todos os endpoints REST exigem `@RolesAllowed` (exceto `/auth/login`, `/auth/refresh`, `/auth/logout`)
- Perfil `prod` lê as chaves de `/etc/mekano/secrets/` (bind mount)

### Geração de chaves (local)

```bash
# Gera ~/.mekano/secrets/privatekey.pem + publicKey.pem no classpath
./mekano-rest/keygen.sh
```

### Docker Dev

O serviço `keygen` no `docker-compose.yml` gera as chaves automaticamente usando Alpine + OpenSSL, armazenando em um volume nomeado (`mekano_secrets`).

### Docker Prod

Em produção, a chave privada é montada via bind mount de `/etc/mekano/secrets/`. O `keygen.sh` deve ser executado uma vez no host antes do deploy.

### Seed admin

A migration **V32** cria um usuário admin inicial:

| Campo | Valor |
|-------|-------|
| E-mail | `admin@mekano.com.br` |
| Senha | `Mekano@2024` |
| Role | `admin` |

## Testes

### Rodar todos os testes do projeto

Executa os testes de todos os módulos: `mekano-domain`, `mekano-application`, `mekano-infrastructure`, `mekano-rest`.

```bash
./mvnw test
```

```powershell
mvn test
```

### Rodar apenas um módulo específico

```bash
./mvnw test -pl mekano-domain -am
```

- `-pl mekano-domain` → executa apenas esse módulo
- `-am` (also make) → compila dependências necessárias

### Rodar apenas um teste específico

```bash
./mvnw test -pl mekano-domain -Dtest=PlacaVeiculoTest
```

```powershell
.\mvnw.cmd test -pl mekano-domain -Dtest=PlacaVeiculoTest
```

### Rodar múltiplos testes específicos

```powershell
.\mvnw.cmd test `
  -pl mekano-domain `
  -Dtest="PlacaVeiculoTest,VeiculoTest"
```

### Rodar um único método de teste

```powershell
.\mvnw.cmd test `
  -pl mekano-domain `
  -Dtest=PlacaVeiculoTest#deveCriarPlacaAntigaValida
```

### Mostrar apenas as falhas

```powershell
.\mvnw.cmd test -q
```

### Clean + verify (ciclo Maven completo)

```powershell
.\mvnw.cmd clean verify
```

## Postman

Importe o arquivo **`Mekano API v1.0.postman_collection.json`** (raiz do projeto) no Postman para testar todos os endpoints.

## Estrutura do projeto

O projeto segue **Clean Architecture** com 4 módulos Maven:

| Módulo | Função |
|--------|--------|
| `mekano-domain` | Entidades, Value Objects, Ports, Eventos |
| `mekano-application` | Casos de uso (serviços da aplicação) |
| `mekano-infrastructure` | JPA, Repositórios, Mappers, Flyway |
| `mekano-rest` | Recursos REST, DTOs, Config |
