# Mekano API

API REST para gestão de oficina mecânica — ordens de serviço, clientes, veículos, estoque e faturamento.

## Pré-requisitos

- **Java 17** (configurado via `JAVA_HOME`)
- **Docker Desktop** ou **Rancher Desktop** instalado e em execução
- **Maven Wrapper** (`./mvnw` incluso no projeto — não precisa instalar Maven)

## Como inicializar o projeto

1. Suba o banco PostgreSQL com Docker:

```bash
docker-compose up -d
```

2. Verifique se o container está rodando:

```bash
docker ps
```

3. Acesse o banco (opcional):

```bash
docker exec -it mekano-postgres psql -U mekano -d mekano
```

## Rodar em modo dev local

```bash
./mvnw quarkus:dev
```

> O Quarkus Dev UI estará disponível em <http://localhost:8080/q/dev/>.

O Flyway executa as migrations automaticamente ao subir a aplicação.

## Build completo e testes

Para verificar se o projeto compila 100% e todos os testes passam:

```powershell
./mvnw -B -ntp verify -pl mekano-rest -am
```

## Geração de chave JWT

O projeto utiliza **Ed25519 (EdDSA)** para assinar e verificar tokens JWT via SmallRye JWT.

### 1. Criar diretório de secrets

```bash
mkdir -p ~/.mekano/secrets
```

### 2. Gerar a chave privada

Você pode pedir para uma IA gerar o comando ou usar diretamente:

```bash
openssl genpkey -algorithm Ed25519 -out ~/.mekano/secrets/privatekey.pem
```

> **Prompt sugerido para IA:**  
> *"Generate an openssl command to create an Ed25519 private key in PKCS#8 format and save it to ~/.mekano/secrets/privatekey.pem, then extract the public key to mekano-rest/src/main/resources/publicKey.pem"*

### 3. Extrair a chave pública

```bash
openssl pkey -in ~/.mekano/secrets/privatekey.pem -pubout -out mekano-rest/src/main/resources/publicKey.pem
```

### 4. Personalizar caminho da chave (produção)

Em produção, use a variável de ambiente:

```bash
export SMALLRYE_JWT_SIGN_KEY_LOCATION=/etc/secrets/jwt/privatekey.pem
```

> **Importante:** A chave privada (`privatekey.pem`) **nunca** deve ser commitada no git — o `.gitignore` já a exclui.

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

> A migration **V32** já cria um usuário **admin** para facilitar a autenticação:
> - **E-mail:** `admin@mekano.com.br`
> - **Senha:** `Mekano@2024`

## Estrutura do projeto

O projeto segue **Clean Architecture** com 4 módulos Maven:

| Módulo | Função |
|--------|--------|
| `mekano-domain` | Entidades, Value Objects, Ports, Eventos |
| `mekano-application` | Casos de uso (serviços da aplicação) |
| `mekano-infrastructure` | JPA, Repositórios, Mappers, Flyway |
| `mekano-rest` | Recursos REST, DTOs, Config |
