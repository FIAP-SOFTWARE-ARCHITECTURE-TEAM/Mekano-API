# Executando o projeto Locahost

## 1. Ferramentas necessárias

Para executar e validar o projeto localmente, recomenda-se utilizar as seguintes ferramentas:

### JDK 17 (configurado via `JAVA_HOME`)

Projeto elaborado em linguagem JAVA com framework QUARKUS;

### IDE / Editor de Código

Exemplos:

- Visual Studio Code
- IntelliJ IDEA
- Outra IDE compatível com Java/Maven

Permite editar, executar, depurar e gerenciar o projeto **Mekano-API**.

### Desktop Container Runtime

Exemplos:

- Docker Desktop
- Rancher Desktop

Permite executar e gerenciar os containers localmente, além de possibilitar o gerenciamento de imagens, volumes e demais recursos relacionados à execução dos containers.

### Database Client

Exemplos:

- DBeaver
- Oracle SQL Developer
- DataGrip

Permite conectar-se ao banco de dados local para consultar e visualizar os dados persistidos pela aplicação.

### API Client

Exemplos:

- Postman
- Insomnia
- Bruno

Permite realizar requisições aos endpoints da **Mekano-API**, facilitando a execução manual e visual dos testes e validações da API.

---

## 2. Compilação do projeto e execução dos testes unitários

Execute, na raiz do projeto, o seguinte comando:

```bash
mvn -B -ntp verify -pl mekano-rest -am
```

Esse comando realiza o processo de build dos módulos necessários do projeto e executa os testes configurados no Maven.

---

## 3. Subindo os containers com Docker Compose

Após a compilação do projeto, execute na raiz do projeto:

```bash
docker compose up -d --build
```

O comando irá:

1. Construir as imagens necessárias para a execução da aplicação;
2. Construir a imagem da **Mekano-API**, utilizando JDK 17;
3. Criar e iniciar os containers definidos no `docker-compose.yml`;
4. Disponibilizar a aplicação para execução local.

> **Nota:** O parâmetro `--build` força a reconstrução das imagens quando houver alterações que impactem o processo de build.

A aplicação estara disponivel em:

- API:        <http://localhost:8080>
- Swagger UI: <http://localhost:8080/q/swagger-ui>
- Health:     <http://localhost:8080/q/health/live>

> O `Dockerfile.jvm` e multi-stage: compila o JAR internamente (sem precisar de Maven instalado na maquina host) e depois gera a imagem runtime. O servico `keygen` gera o par de chaves Ed25519 na primeira execução.

## 4. Execução dos testes funcionais

Os testes funcionais da API estão definidos na Postman Collection:

```text
./newman/Mekano_API_V2.0.postman_collection.json
```

A execução automatizada da Collection pode ser realizada utilizando o **Newman**:

```bash
newman run .\newman\Mekano_API_V2.0.postman_collection.json
```

### Newman

**Newman** é uma ferramenta de linha de comando que permite executar automaticamente Collections do Postman armazenadas em arquivos JSON.

No projeto **Mekano-API**, o Newman é utilizado para executar os testes funcionais dos endpoints da API de forma automatizada.

Por ser executado via linha de comando, o Newman também pode ser utilizado em pipelines de **CI/CD**, permitindo que os testes sejam executados automaticamente sem a necessidade de abrir uma interface gráfica.

Fluxo simplificado:

```text
Postman Collection (.json)
          │
          ▼
       Newman
          │
          ▼
   Requisições HTTP
          │
          ▼
      Mekano-API
          │
          ▼
   Testes / Assertions
          │
       PASS / FAIL
```

---

## 5. Swagger UI

Após a aplicação estar em execução, a documentação interativa da API pode ser acessada através do Swagger UI:

**[http://localhost:8080/q/swagger-ui](http://localhost:8080/q/swagger-ui)**

O Swagger UI permite visualizar os endpoints disponíveis e realizar requisições diretamente pela interface gráfica.

## 6. Destruindo containers e subindo novamente

Após já ter subido os containers com build 'docker compose up -d --build' é possível destruir os containers e volumes persistidos pelo comando:

```bash
docker compose down -v
```

E posteriormente, para subir novamente, basta executar (sem o build):

```bash
docker compose up -d
```
