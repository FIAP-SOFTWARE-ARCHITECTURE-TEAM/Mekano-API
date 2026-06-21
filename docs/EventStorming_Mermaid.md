# Diagramas de Agregados — Domain-Driven Design

> Transcrição dos diagramas de agregados identificados no board de Event Storming.  
> Fonte: [Miro Board](https://miro.com/app/board/uXjVHD4vUnU=/)

---

## 1. Contexto de Ordem de Serviço

O contexto de **Ordem de Serviço (OS)** é responsável por todo o ciclo de vida de uma OS: desde a identificação do cliente e veículo, passando pelo diagnóstico, geração e aprovação de orçamento, execução do serviço e entrega do veículo.

```mermaid
classDiagram
    direction TB

    class OrdemDeServico {
        <<Aggregate Root>>
        + id: UUID
        + numero: String
        + status: StatusOS
        + dataCriacao: DateTime
        + dataEntrada: DateTime
        + criarOS()
        + indicarEntradaVeiculo()
        + iniciarDiagnostico()
        + incluirServicosInsumos()
        + finalizarAnalise()
        + atualizarStatus(novoStatus)
        + iniciarExecucao()
        + finalizarExecucao()
        + cancelar()
    }

    class StatusOS {
        <<Enumeration>>
        RECEBIDA
        EM_DIAGNOSTICO
        AGUARDANDO_APROVACAO
        CANCELADA
        EM_EXECUCAO
        FINALIZADA
        ENTREGUE
    }

    class Cliente {
        <<Entity>>
        + id: UUID
        + cpf: String
        + cnpj: String
        + nome: String
        + email: String
        + telefone: String
    }

    class Veiculo {
        <<Entity>>
        + id: UUID
        + placa: String
        + modelo: String
        + marca: String
        + ano: Integer
    }

    class ItemOS {
        <<Entity>>
        + id: UUID
        + tipo: TipoItem
        + descricao: String
        + quantidade: Integer
        + valorUnitario: Decimal
    }

    class TipoItem {
        <<Enumeration>>
        SERVICO
        PECA_INSUMO
    }

    class Orcamento {
        <<Entity>>
        + id: UUID
        + valorTotal: Decimal
        + dataGeracao: DateTime
        + dataEnvio: DateTime
        + dataExpiracao: DateTime
        + status: StatusOrcamento
        + gerar()
        + enviarParaCliente()
        + aprovar()
        + reprovar()
    }

    class StatusOrcamento {
        <<Enumeration>>
        GERADO
        ENVIADO
        APROVADO
        REPROVADO
        EXPIRADO
    }

    class PoliticaSLA {
        <<Value Object>>
        + tempoMaximoAprovacao: Duration
        + estaExpirado(dataGeracao): Boolean
    }

    OrdemDeServico "1" --> "1" StatusOS : possui
    OrdemDeServico "1" --> "1" Cliente : pertence a
    OrdemDeServico "1" --> "1" Veiculo : refere-se a
    OrdemDeServico "1" --> "0..*" ItemOS : contém
    OrdemDeServico "1" --> "0..1" Orcamento : possui
    OrdemDeServico "1" --> "1" PoliticaSLA : regida por
    ItemOS "1" --> "1" TipoItem : é do tipo
    Orcamento "1" --> "1" StatusOrcamento : possui
```

### Eventos de Domínio

```mermaid
flowchart LR
    EV1([Ordem de Serviço Criada]) --> EV2([Cliente Identificado])
    EV2 --> EV3([Veículo Identificado])
    EV3 --> EV4([Serviço Incluído])
    EV4 --> EV5([OS atualizada para RECEBIDA])
    EV5 --> EV6([Diagnóstico Iniciado])
    EV6 --> EV7([OS atualizada para EM DIAGNÓSTICO])
    EV7 --> EV8([OS atualizada com serviços e insumos])
    EV8 --> EV9([Orçamento gerado])
    EV9 --> EV10([Orçamento enviado para o cliente])
    EV10 --> EV11([OS atualizada para AGUARDANDO APROVAÇÃO])
    EV11 -->|Aprovado| EV12([Orçamento Aprovado])
    EV11 -->|Reprovado / SLA expirado| EV13([OS ATUALIZADA PARA CANCELADA])
    EV12 --> EV14([Execução iniciada])
    EV14 --> EV15([Status atualizado EM EXECUÇÃO])
    EV15 --> EV16([Execução finalizada])
    EV16 --> EV17([OS atualizada para FINALIZADA])
    EV17 --> EV18([Cobrança emitida])
    EV18 --> EV19([Pagamento confirmado])
    EV19 --> EV20([Veículo entregue])
    EV20 --> EV21([OS status atualizada ENTREGUE])
```

---

## 2. Contexto de Gestão de Estoque

O contexto de **Gestão de Estoque** é responsável por controlar o saldo de peças e insumos, realizar reservas, gerar requisições de compra e registrar entradas via nota fiscal.

```mermaid
classDiagram
    direction TB

    class Estoque {
        <<Aggregate Root>>
        + id: UUID
        + verificarDisponibilidade(itemId, qtd): Boolean
        + reservarPecas(osId, itens): ReservaEstoque
        + retirarPecas(osId, itens)
        + atualizarSaldo(itemId, quantidade)
        + verificarEstoqueMinimo(): List~AlertaEstoque~
    }

    class ItemEstoque {
        <<Entity>>
        + id: UUID
        + codigo: String
        + descricao: String
        + unidade: String
        + saldoAtual: Integer
        + estoqueMinimo: Integer
        + reservado: Boolean
        + estaAbaixoMinimo(): Boolean
        + calcularEstoqueMinimo(tempoReposicao, consumoDiario): Integer
    }

    class ReservaEstoque {
        <<Entity>>
        + id: UUID
        + osId: UUID
        + status: StatusReserva
        + dataReserva: DateTime
        + itensReservados: List~ItemReservado~
        + cancelar()
    }

    class ItemReservado {
        <<Value Object>>
        + itemEstoqueId: UUID
        + quantidade: Integer
        + flagReservada: Boolean
    }

    class StatusReserva {
        <<Enumeration>>
        ATIVA
        RETIRADA
        CANCELADA
    }

    class RequisicaoDeCompra {
        <<Aggregate Root>>
        + id: UUID
        + osId: UUID
        + status: StatusRequisicao
        + dataCriacao: DateTime
        + itens: List~ItemRequisicao~
        + cancelar()
        + listar()
    }

    class ItemRequisicao {
        <<Entity>>
        + id: UUID
        + itemEstoqueId: UUID
        + descricao: String
        + quantidadeSolicitada: Integer
        + motivoRequisicao: MotivoRequisicao
    }

    class MotivoRequisicao {
        <<Enumeration>>
        VINCULADO_OS
        ESTOQUE_MINIMO
    }

    class StatusRequisicao {
        <<Enumeration>>
        EM_ABERTO
        EM_ANDAMENTO
        FINALIZADO
        CANCELADO
    }

    class NotaFiscal {
        <<Entity>>
        + id: UUID
        + numero: String
        + fornecedor: String
        + dataCadastro: DateTime
        + itens: List~ItemNotaFiscal~
        + requisicaoCompraId: UUID
    }

    class ItemNotaFiscal {
        <<Value Object>>
        + itemEstoqueId: UUID
        + quantidade: Integer
        + valorUnitario: Decimal
    }

    class AlertaEstoque {
        <<Value Object>>
        + itemEstoqueId: UUID
        + saldoAtual: Integer
        + estoqueMinimo: Integer
    }

    Estoque "1" --> "0..*" ItemEstoque : gerencia
    Estoque "1" --> "0..*" ReservaEstoque : registra
    Estoque "1" --> "0..*" AlertaEstoque : gera
    ReservaEstoque "1" --> "0..*" ItemReservado : contém
    ReservaEstoque "1" --> "1" StatusReserva : possui
    RequisicaoDeCompra "1" --> "0..*" ItemRequisicao : contém
    RequisicaoDeCompra "1" --> "1" StatusRequisicao : possui
    RequisicaoDeCompra "1" --> "0..1" NotaFiscal : origina
    ItemRequisicao "1" --> "1" MotivoRequisicao : tem motivo
    NotaFiscal "1" --> "0..*" ItemNotaFiscal : contém
```

### Eventos de Domínio

```mermaid
flowchart LR
    EV1([Orçamento Aprovado]) --> CMD1[Verificar Estoque]
    CMD1 --> EV2([Peças/Insumos disponíveis reservados])
    CMD1 --> EV3([Req de compra feita])
    EV2 --> EV4([Peças/Insumos com estoque atualizado])
    EV3 --> CMD2[Listar Requisições de Compra]
    CMD2 --> CMD3[Cadastrar pçs/insumos]
    CMD3 --> EV5([Peças/Insumos Comprados])
    EV5 --> CMD4[Cadastrar Nota]
    CMD4 --> EV6([Nota cadastrada pelo FINANCEIRO])
    EV6 --> CMD5[Atualiza Saldo do Estoque]
    CMD5 --> EV7([Estoque atualizado])
    EV7 --> EV8([Peças/Insumos com estoque atualizado])
    CMD5 --> EV9([Identificado Saldo menor que estoque mínimo])
    EV9 --> CMD6[Cadastra Requisição de Compra]
    CMD6 --> EV10([Req de compra feita - por estoque mínimo])
```

---

## 3. Contexto de Ordem de Pagamento

O contexto de **Ordem de Pagamento** é responsável pela emissão de cobranças, processamento e confirmação de pagamentos após a conclusão do serviço, bem como pela entrega do veículo ao cliente.

```mermaid
classDiagram
    direction TB

    class OrdemDePagamento {
        <<Aggregate Root>>
        + id: UUID
        + osId: UUID
        + valorTotal: Decimal
        + status: StatusPagamento
        + dataCriacao: DateTime
        + emitirCobranca()
        + registrarPagamento(metodoPagamento)
        + confirmarPagamento()
        + atualizarStatusOS()
    }

    class StatusPagamento {
        <<Enumeration>>
        PENDENTE
        CONFIRMADO
    }

    class Cobranca {
        <<Entity>>
        + id: UUID
        + valor: Decimal
        + dataEmissao: DateTime
        + metodoPagamento: MetodoPagamento
        + referenciaExterna: String
        + emitir()
    }

    class MetodoPagamento {
        <<Enumeration>>
        CARTAO_CREDITO
        CARTAO_DEBITO
        PIX
        BOLETO
        DINHEIRO
    }

    class Pagamento {
        <<Entity>>
        + id: UUID
        + valor: Decimal
        + dataPagamento: DateTime
        + dataConfirmacao: DateTime
        + status: StatusPagamento
        + referenciaServicoBancario: String
        + confirmar()
    }

    class ServicoBancario {
        <<External Service>>
        + processarPagamento(cobrancaId): Boolean
        + consultarStatus(referenciaId): StatusPagamento
    }

    class EntregaVeiculo {
        <<Entity>>
        + id: UUID
        + osId: UUID
        + dataEntrega: DateTime
        + responsavelEntrega: String
        + clienteRecebedor: String
        + registrar()
    }

    OrdemDePagamento "1" --> "1" StatusPagamento : possui
    OrdemDePagamento "1" --> "1" Cobranca : gera
    OrdemDePagamento "1" --> "0..1" Pagamento : registra
    OrdemDePagamento "1" --> "0..1" EntregaVeiculo : permite
    Cobranca "1" --> "1" MetodoPagamento : usa
    Pagamento "1" --> "1" StatusPagamento : possui
    OrdemDePagamento ..> ServicoBancario : integra com
```

### Eventos de Domínio

```mermaid
flowchart LR
    EV1([OS atualizada para FINALIZADA]) --> CMD1[Emitir Cobrança]
    CMD1 --> EV2([Cobrança emitida])
    EV2 --> CMD2[Preencher campo PAGAMENTO PENDENTE]
    CMD2 --> EV3([Campo atualizado PAGAMENTO PENDENTE])
    EV3 --> CMD3[Realizar Pagamento]
    CMD3 --> SVC[Serviço de Pagamento - Banco]
    SVC --> EV4([Pagamento confirmado])
    EV4 --> CMD4[Atualizar campo PAGAMENTO CONFIRMADO]
    CMD4 --> EV5([Campo atualizado PAGAMENTO CONFIRMADO])
    EV5 --> CMD5[Entregar Veículo ao Cliente]
    CMD5 --> EV6([Veículo entregue])
    EV6 --> CMD6[Atualizar Status OS para ENTREGUE]
    CMD6 --> EV7([OS status atualizada ENTREGUE])
```

---

## Legenda

| Símbolo | Significado |
|---|---|
| `<<Aggregate Root>>` | Raiz do agregado — ponto de entrada e controle de consistência |
| `<<Entity>>` | Entidade com identidade própria dentro do agregado |
| `<<Value Object>>` | Objeto de valor imutável, sem identidade própria |
| `<<Enumeration>>` | Enumeração de estados ou tipos |
| `<<External Service>>` | Serviço externo ao bounded context |
| `EV` | Evento de Domínio |
| `CMD` | Comando |
| `POL` | Política (reação automática a um evento) |
| `AT` | Ator (usuário ou sistema que dispara o comando) |
| `ML` | Mock-up / Tela |
| `SE` | Sistema Externo |
| `AG` | Agregado |
