# TECH CHALLENGE - OFICINA MECÂNICA

## Autor(es)
* Conrado Moura (conrado.moura@icloud.com)
* Elias Ferreira (eliaspsm2@gmail.com)
* Giovanni Brasil (giovannisbrasil@gmail.com)
* Roger Toledo (rogertoledo28@gmail.com)
* Victor Souza (victor.souza2210@gmail.com)

## 1. Objetivo da Solução

Uma oficina mecânica de médio porte, especializada em manutenção de veículos, enfrenta dificuldades para escalar seus serviços com qualidade e eficiência. O processo atual de atendimento, diagnóstico, execução e entrega dos veículos é conduzido de forma desorganizada — com anotações manuais e planilhas —, o que gera erros na priorização de atendimentos, falhas no controle de peças e insumos, dificuldade em acompanhar o status dos serviços, perda de histórico de clientes e veículos, e ineficiência no fluxo de orçamentos e autorizações.

O objetivo deste projeto é desenvolver a primeira versão (MVP) do back-end de um **Sistema Integrado de Atendimento e Execução de Serviços** para a oficina, aplicando os princípios de **Domain-Driven Design (DDD)** e boas práticas de qualidade e segurança de software. A solução permitirá:

- Gerenciar o ciclo de vida completo das Ordens de Serviço (OS), desde a criação até a entrega do veículo;
- Controlar o estoque de peças e insumos com reservas e requisições de compra;
- Gerar e enviar orçamentos automaticamente ao cliente para aprovação;
- Oferecer ao cliente visibilidade em tempo real do andamento do serviço via API;
- Garantir gestão administrativa segura com autenticação e controle de acesso.

## 2. Escopo

O projeto abrange o desenvolvimento do back-end monolítico em arquitetura em camadas, contemplando os seguintes bounded contexts e funcionalidades:

### 2.1 Contexto de Ordem de Serviço
- Identificação de cliente por CPF/CNPJ e cadastro do veículo (placa, marca, modelo, ano);
- Criação de OS com inclusão de serviços solicitados e peças/insumos necessários;
- Geração automática de orçamento e envio ao cliente para aprovação;
- Gestão do ciclo de vida da OS com os status: **Recebida → Em Diagnóstico → Aguardando Aprovação → Em Execução → Finalizada → Entregue** (e **Cancelada** em caso de reprovação ou estouro de SLA);
- Consulta pública do status da OS pelo cliente via API.

### 2.2 Contexto de Gestão de Estoque
- CRUD de peças e insumos com controle de saldo;
- Reserva automática de peças disponíveis ao aprovar orçamento;
- Geração de Requisições de Compra para peças indisponíveis ou abaixo do estoque mínimo;
- Registro de Nota Fiscal de entrada e atualização automática do saldo em estoque;
- Alerta de estoque mínimo calculado por fórmula: `tempo de reposição × consumo médio diário`.

### 2.3 Contexto de Ordem de Pagamento
- Emissão automática de cobrança ao finalizar a execução do serviço;
- Integração com serviço bancário externo para processamento do pagamento;
- Confirmação de pagamento e liberação da entrega do veículo;
- Atualização do status da OS para **Entregue** após confirmação e entrega.

### 2.4 Gestão Administrativa
- CRUD de clientes, veículos, serviços e peças/insumos;
- Listagem e detalhamento de ordens de serviço com filtros por data, status e cliente;
- Monitoramento do tempo médio de execução dos serviços;
- Autenticação JWT para APIs administrativas.

### 2.5 Fora do escopo (fase 1)
- Front-end / interface gráfica;
- Aplicativo mobile do cliente;
- Módulo financeiro/contábil completo;
- Integração real com múltiplos gateways de pagamento.

## 3. Tecnologias utilizadas
> Pontuar as tecnologias utilizadas (Backend, Frontend, docs, banco, versionamento, Ferramentas de teste, Segurança, etc.)
- API REST em linguagem JAVA com framework Quarkus;
- Junit5 e Mockito para testes automatizados;
- Documentação Swagger da API;
- Autenticação JWT;
- versionamento GIT e repositorio GITHUB;
- Plataforma Miro e Figma para documentação de fluxos;

## 4. Requisitos

### 4.1 Requisitos Funcionais

#### Contexto de Ordem de Serviço

| ID   | Nome do Requisito               | Descrição                                                                                                          | Atores Envolvidos              | Entradas                                            | Saídas / Resultados                                                    | Regras de Negócio / Observações                                                                          |
|------|---------------------------------|--------------------------------------------------------------------------------------------------------------------|--------------------------------|-----------------------------------------------------|------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------|
| RF01 | Gerenciar Clientes              | Permitir o cadastro, edição, consulta e exclusão de clientes                                                       | Administrador, Atendente       | Nome, CPF ou CNPJ, e-mail, telefone                 | Cliente persistido, atualizado ou removido                             | CPF/CNPJ deve ser único no sistema; validar formato de CPF e CNPJ                                        |
| RF02 | Gerenciar Veículos              | Permitir o cadastro, edição, consulta e exclusão de veículos                                                       | Administrador, Atendente       | Placa, marca, modelo, ano, cliente vinculado         | Veículo persistido, atualizado ou removido                             | Placa deve ser única no sistema; validar formato de placa                                                |
| RF03 | Gerenciar Serviços              | Permitir o cadastro, edição, consulta e exclusão de tipos de serviço disponíveis na oficina                        | Administrador                  | Nome, descrição, valor unitário                     | Serviço cadastrado ou atualizado                                       | Valor deve ser maior que zero                                                                            |
| RF04 | Criar Ordem de Serviço (OS)     | Criar uma nova OS identificando cliente (CPF/CNPJ) e veículo (placa), registrando entrada e serviços solicitados   | Atendente                      | CPF/CNPJ do cliente, placa do veículo, serviços solicitados | OS criada com status **Recebida** e momento de entrada registrado | Se cliente ou veículo não existir, permitir cadastro no mesmo fluxo; status inicial obrigatório: Recebida |
| RF05 | Iniciar Diagnóstico da OS       | Atualizar o status da OS para **Em Diagnóstico** e permitir inclusão de serviços e peças identificados pelo mecânico | Mecânico                       | ID da OS, lista de serviços e peças/insumos         | OS atualizada com serviços/peças e status **Em Diagnóstico**           | Apenas OS com status **Recebida** pode iniciar diagnóstico                                               |
| RF06 | Gerar e Enviar Orçamento        | Gerar orçamento automaticamente ao finalizar diagnóstico e enviá-lo ao cliente por e-mail                          | Sistema (automático)           | Serviços e peças da OS, estoque disponível          | Orçamento gerado, enviado ao cliente, OS atualizada para **Aguardando Aprovação** | Sistema deve verificar disponibilidade de estoque para informar prazo; envio automático por e-mail        |
| RF07 | Aprovar ou Reprovar Orçamento   | Permitir que o cliente aprove ou reprove o orçamento via API                                                       | Cliente                        | ID da OS, decisão (aprovar / reprovar)              | OS atualizada para **Em Execução** (aprovado) ou **Cancelada** (reprovado) | Orçamento expirado por SLA também cancela a OS; OS cancelada não pode ser reaberta                        |
| RF08 | Executar e Finalizar OS         | Registrar início e fim da execução do serviço, atualizando status da OS                                            | Mecânico                       | ID da OS                                            | OS atualizada para **Em Execução** e depois **Finalizada**             | Apenas OS **Em Execução** pode ser finalizada; ao finalizar, emitir cobrança automaticamente              |
| RF09 | Acompanhar Status da OS         | Permitir consulta pública do status de uma OS pelo cliente                                                         | Cliente                        | ID ou número da OS                                  | Status atual e histórico de atualizações da OS                         | Endpoint público, sem necessidade de autenticação                                                        |
| RF10 | Listar e Detalhar OS            | Listar todas as OS com filtros e consultar detalhes de uma OS específica                                           | Administrador, Atendente       | Filtros (data, status, cliente, veículo), ID da OS  | Lista paginada de OS / detalhes completos com serviços, peças e valores | Requer autenticação; suportar paginação                                                                  |
| RF11 | Monitorar Tempo Médio de Execução | Calcular e exibir o tempo médio de execução dos serviços com base no histórico de OS finalizadas                 | Administrador                  | Período de referência (data início / fim)           | Métricas de tempo médio por tipo de serviço                            | Usado para análise gerencial; requer autenticação                                                        |

#### Contexto de Gestão de Estoque

| ID   | Nome do Requisito                     | Descrição                                                                                                     | Atores Envolvidos            | Entradas                                              | Saídas / Resultados                                                      | Regras de Negócio / Observações                                                                                       |
|------|---------------------------------------|---------------------------------------------------------------------------------------------------------------|------------------------------|-------------------------------------------------------|--------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------|
| RF12 | Gerenciar Peças e Insumos             | Permitir CRUD de peças e insumos com controle de saldo em estoque                                             | Administrador, Almoxarife    | Código, descrição, unidade, saldo, estoque mínimo, valor | Peça/insumo cadastrado ou atualizado                                    | Saldo não pode ficar negativo; estoque mínimo = tempo de reposição × consumo médio diário                             |
| RF13 | Reservar Peças ao Aprovar Orçamento   | Ao aprovar orçamento, reservar automaticamente as peças/insumos disponíveis em estoque para a OS              | Sistema (automático)         | ID da OS, lista de peças necessárias                  | Peças reservadas no estoque (flag reservada = true); Requisição de Compra aberta para peças indisponíveis | Reserva é um flag no estoque; peças reservadas não ficam disponíveis para outras OS                                   |
| RF14 | Gerenciar Requisições de Compra       | Permitir listar, visualizar e cancelar Requisições de Compra geradas automaticamente pelo sistema              | Financeiro, Almoxarife       | ID da requisição, ação (cancelar)                     | Requisição com status atualizado                                         | Status possíveis: Em Aberto, Em Andamento, Finalizado, Cancelado; não cancelar se vinculada a OS ativa                |
| RF15 | Registrar Nota Fiscal e Entrada       | Cadastrar NF de entrada de peças referenciando a Requisição de Compra, atualizando o saldo em estoque         | Almoxarife, Financeiro       | Dados da NF (número, fornecedor, itens, valores), ID da Requisição | Saldo do estoque atualizado; Requisição marcada como Finalizada        | Ao atualizar saldo, verificar se ainda há itens abaixo do estoque mínimo e gerar nova requisição se necessário        |
| RF16 | Retirar Peças para Execução           | Registrar a saída das peças reservadas do estoque ao iniciar a execução do serviço                            | Almoxarife                   | ID da OS                                              | Saldo do estoque atualizado (peças debitadas); reserva encerrada        | Apenas peças previamente reservadas para aquela OS podem ser retiradas                                                |

#### Contexto de Ordem de Pagamento

| ID   | Nome do Requisito                   | Descrição                                                                                              | Atores Envolvidos          | Entradas                              | Saídas / Resultados                                                         | Regras de Negócio / Observações                                                                  |
|------|-------------------------------------|--------------------------------------------------------------------------------------------------------|----------------------------|---------------------------------------|-----------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------|
| RF17 | Emitir Cobrança ao Finalizar OS     | Ao finalizar a execução, emitir cobrança automaticamente e atualizar o campo de pagamento para Pendente | Sistema (automático)       | ID da OS finalizada                   | Cobrança emitida, campo de pagamento atualizado para **Pendente**           | Gerado automaticamente ao transitar para status Finalizada                                       |
| RF18 | Registrar e Confirmar Pagamento     | Receber confirmação do serviço bancário externo e registrar o pagamento como confirmado                | Cliente, Serviço Bancário  | Referência do pagamento, status do banco | Campo de pagamento atualizado para **Confirmado**                          | Integração com serviço externo de pagamento (banco); pagamento não confirmado bloqueia entrega   |
| RF19 | Registrar Entrega do Veículo        | Registrar a entrega do veículo ao cliente e atualizar o status final da OS                             | Administrativo             | ID da OS, dados da entrega            | OS atualizada para **Entregue**; entrega registrada com data e responsável  | Apenas OS com pagamento **Confirmado** pode ter veículo entregue                                 |


### 4.2 Requisitos Não Funcionais
* Backend monolítico;
* Arquitetura em camadas;
* Banco SQL Relacional;
* API RESTful com documentação Swagger;
* Dockerfile;
* Implementação de autenticação JWT para APIs administrativas;
* Validação dos dados sensíveis (CPF/CNPJ, placa de veículo);
* Testes unitários e de integração para os principais fluxos

## 5. Domain Drive Design

### 5.1 StoryTelling
> link: https://www.figma.com/board/wx7kmepPOFfWmXFR4geq0m/FIAP?node-id=45-880&t=prDEFxIbr6jIWrNm-0

### 5.2 Event Storming 
> link: https://miro.com/app/board/uXjVHD4vUnU=/

## 6. Arquitetura da Solução

    - Sistema Monolito em API REST
    - Organização em arquitetura de camadas

## Checklist de Qualidade
| Critério                  | Atendido          | Observação            |
|---------------------------|-------------------|-----------------------|
| Testes automatizados      | OK                | 80%                   |
| Pipeline CI/CD configurado| OK                | GitLab + SonarQube    |
| Documentação da API       | OK                | Swagger disponível    | 