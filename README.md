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

## Geração de chaves JWT (Phase 8)

Esta seção descreve como gerar localmente o par de chaves RSA usado pelo SmallRye JWT
para verificar tokens MicroProfile JWT (EXT-09). A chave privada é estritamente
dev-local — **nunca** deve ser commitada (D-08, `.gitignore` veta `privateKey*.pem`).

### 1. Comandos `openssl` (gerar par RSA em PKCS#8)

A ordem abaixo é obrigatória — o SmallRye JWT exige a chave privada em formato
PKCS#8 (G7):

```bash
# 1) Gera chave privada RSA 2048 bits (formato PKCS#1)
openssl genrsa -out privateKey.pem 2048

# 2) Converte para PKCS#8 sem cifragem — formato aceito pelo SmallRye JWT
openssl pkcs8 -topk8 -nocrypt -inform pem -in privateKey.pem -outform pem -out privateKey_pkcs8.pem

# 3) Extrai a chave pública correspondente para o classpath do adapter
openssl rsa -pubout -in privateKey.pem -out mekano-adapter/src/main/resources/publicKey.pem
```

### 2. Onde cada arquivo vive (e por quê)

- `mekano-adapter/src/main/resources/publicKey.pem` — **rastreado** no git
  (D-09). É empacotado no artefato e usado pelo Quarkus para verificar tokens em
  runtime via `mp.jwt.verify.publickey.location`.
- `privateKey.pem` e `privateKey_pkcs8.pem` — **fora** do controle de versão
  (D-08). O `.gitignore` raiz veta o padrão `privateKey*.pem`. Use a chave
  PKCS#8 apenas no seu fluxo local de emissão de tokens (curl/Postman).
- Os testes automatizados **não** dependem desses arquivos: o
  `JwtTestProfile` (plano 08-05) gera um par RSA em memória programaticamente,
  o que torna a suíte CI-friendly e reprodutível.

### 3. Variável de ambiente `MP_JWT_ISSUER`

O issuer default configurado em `application.properties` é
`https://mekano.fiap.com.br/auth`. Para sobrescrever em runtime (D-05),
exporte `MP_JWT_ISSUER` antes de subir a aplicação:

```bash
export MP_JWT_ISSUER=https://meu-issuer-local/auth
./mvnw -pl mekano-adapter quarkus:dev
```