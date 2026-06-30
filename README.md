# Mekano API

Este projeto usa Quarkus, o framework Java Supersônico e Subatômico.

Para saber mais sobre o Quarkus, acesse o site oficial: <https://quarkus.io/>.

## Executando a aplicação em modo de desenvolvimento

Você pode executar a aplicação em modo dev, que habilita live coding:

```shell script
./mvnw quarkus:dev
```

> **_NOTA:_** O Quarkus inclui uma Dev UI, disponível apenas em modo dev em <http://localhost:8080/q/dev/>.

## Empacotando e executando a aplicação

A aplicação pode ser empacotada com:

```shell script
./mvnw package
```

Isso produz o arquivo `quarkus-run.jar` no diretório `target/quarkus-app/`.
Note que este não é um _über-jar_ — as dependências são copiadas para `target/quarkus-app/lib/`.

A aplicação fica executável com `java -jar target/quarkus-app/quarkus-run.jar`.

Se quiser gerar um _über-jar_, execute:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

A aplicação empacotada como _über-jar_ fica executável com `java -jar target/*-runner.jar`.

## Criando um executável nativo

Você pode criar um executável nativo com:

```shell script
./mvnw package -Dnative
```

Ou, se não tiver o GraalVM instalado, pode gerar o executável nativo dentro de um container:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

Depois é só executar: `./target/mekano-1.0.0-SNAPSHOT-runner`

Para saber mais sobre executáveis nativos, consulte <https://quarkus.io/guides/maven-tooling>.

## Código incluído

### REST

Início rápido para Web Services REST.

[Seção do guia relacionada...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)

## Geração de chaves JWT

Esta seção descreve como gerar localmente o par de chaves Ed25519 (EdDSA)
usado pelo SmallRye JWT para verificar tokens MicroProfile JWT.
A chave privada é estritamente para uso local em dev — **nunca** deve ser commitada
(`.gitignore` veta `privateKey*.pem` e `**/privateKey.pem`).

### 1. Criar diretório de secrets (fora do repositório)

A chave privada reside em `~/.mekano/secrets/privatekey.pem`, um diretório
fora do repositório git — eliminando o risco de versionamento acidental.

```bash
mkdir -p ~/.mekano/secrets
```

### 2. Gerar par Ed25519 via `openssl`

> **Atenção:** o LibreSSL que vem com o macOS não suporta Ed25519. Use o OpenSSL do Homebrew:
> `brew install openssl && $(brew --prefix openssl)/bin/openssl` nos comandos abaixo.

```bash
# 1) Gerar chave privada Ed25519 (PKCS#8)
openssl genpkey -algorithm Ed25519 -out ~/.mekano/secrets/privatekey.pem

# 2) Extrair chave pública para o classpath
openssl pkey -in ~/.mekano/secrets/privatekey.pem -pubout -out mekano-rest/src/main/resources/publicKey.pem
```

### 3. Onde cada arquivo vive (e por quê)

- `mekano-rest/src/main/resources/publicKey.pem` — **rastreado** no git.
  É empacotado no artefato e usado pelo Quarkus para verificar tokens em
  runtime via `mp.jwt.verify.publickey.location`.
- `~/.mekano/secrets/privatekey.pem` — **fora** do repositório git, em
  diretório dedicado no perfil do usuário. O caminho é configurado em
  `auth-config.yml` via `${user.home}/.mekano/secrets/privatekey.pem`.
- Os testes automatizados **não** dependem desses arquivos: o
  `JwtTestProfile` gera um par Ed25519 em memória programaticamente,
  o que torna a suíte amigável para CI e reprodutível.

### 4. Issuer do JWT

O issuer configurado em `auth-config.yml` é `mekano-api`. Para sobrescrever em runtime,
exporte a variável antes de subir a aplicação:

```bash
export MP_JWT_VERIFY_ISSUER=mekano-api
./mvnw -pl mekano-rest quarkus:dev
```

### 5. Personalizar caminho da chave privada (produção)

Em ambientes produtivos, o caminho da chave privada pode ser sobrescrito
via variável de ambiente:

```bash
export SMALLRYE_JWT_SIGN_KEY_LOCATION=/etc/secrets/jwt/privatekey.pem
```
