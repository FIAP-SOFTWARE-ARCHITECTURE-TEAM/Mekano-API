# mekano

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/mekano-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Provided Code

### REST

Easily start your REST Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)

## Geração de chaves JWT (Phase 9)

Esta seção descreve como gerar localmente o par de chaves Ed25519 (ES256/EdDSA)
usado pelo SmallRye JWT para verificar tokens MicroProfile JWT (Phase 9, D-10).
A chave privada é estritamente dev-local — **nunca** deve ser commitada
(D-09, `.gitignore` veta `privateKey*.pem` e `**/privateKey.pem`).

A migração de RSA (Phase 8) para Ed25519 foi feita na Phase 9, Plan 09-03:
algoritmo mais moderno, tamanho de chave fixo (sem necessidade de `initialize(2048)`),
e suporte nativo no Java 17+ via `KeyPairGenerator.getInstance("Ed25519")`.

### 1. Criar diretório de secrets (fora do repositório)

A chave privada reside em `~/.mekano/secrets/privatekey.pem`, um diretório
fora do repositório git — eliminando o risco de versionamento acidental.

```bash
mkdir -p ~/.mekano/secrets
```

### 2. Gerar par Ed25519 via `openssl`

```bash
# 1) Gerar chave privada Ed25519 (PKCS#8)
openssl genpkey -algorithm Ed25519 -out ~/.mekano/secrets/privatekey.pem

# 2) Extrair chave pública para o classpath do adapter
openssl pkey -in ~/.mekano/secrets/privatekey.pem -pubout -out mekano-rest/src/main/resources/publicKey.pem
```

### 3. Onde cada arquivo vive (e por quê)

- `mekano-rest/src/main/resources/publicKey.pem` — **rastreado** no git.
  É empacotado no artefato e usado pelo Quarkus para verificar tokens em
  runtime via `mp.jwt.verify.publickey.location`.
- `~/.mekano/secrets/privatekey.pem` — **fora** do repositório git, em
  diretório dedicado no perfil do usuário. O caminho é configurado em
  `application.properties` via `${user.home}/.mekano/secrets/privatekey.pem`.
- Os testes automatizados **não** dependem desses arquivos: o
  `JwtTestProfile` gera um par Ed25519 em memória programaticamente,
  o que torna a suíte CI-friendly e reprodutível.

### 4. Variável de ambiente `MP_JWT_ISSUER`

O issuer default configurado em `application.properties` é
`https://mekano.fiap.com.br/auth`. Para sobrescrever em runtime,
exporte `MP_JWT_ISSUER` antes de subir a aplicação:

```bash
export MP_JWT_ISSUER=https://meu-issuer-local/auth
./mvnw -pl mekano-rest quarkus:dev
```

### 5. Personalizar caminho da chave privada (produção)

Em ambientes produtivos, o caminho da chave privada pode ser sobrescrito
via variável de ambiente `SMALLRYE_JWT_SIGN_KEY_LOCATION`:

```bash
export SMALLRYE_JWT_SIGN_KEY_LOCATION=/etc/secrets/jwt/privatekey.pem
```