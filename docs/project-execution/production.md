# Execução em Produção

## Infraestrutura AWS com Kubernetes

A infraestrutura de produção da **Mekano-API** utiliza recursos da **AWS** e **Kubernetes**, sendo provisionada e gerenciada por meio de **Terraform**.

O processo de provisionamento da infraestrutura foi estruturado para ser executado de forma automatizada através do pipeline de **Continuous Deployment (CD)**.

### Regra principal

> **A infraestrutura principal não deve ser criada ou alterada manualmente utilizando `terraform apply`.**

O fluxo normal para alterações de infraestrutura deve seguir:

```text
Git
 │
 ▼
GitHub
 │
 ▼
GitHub Actions
 │
 ▼
Terraform
 │
 ▼
AWS / Kubernetes
```

O **AWS CloudShell** pode ser utilizado para consultas, leituras e diagnósticos da infraestrutura.

Alterações normais na infraestrutura, entretanto, devem ser realizadas através do fluxo automatizado de **Git → GitHub Actions → Terraform**.

---

## Provisionamento e desprovisionamento da infraestrutura

Para definir a ação que será executada pelo pipeline, deve-se alterar o arquivo:

```text
./terraform/.infra-action
```

O arquivo deve conter uma das seguintes opções:

### `apply`

Utilizado para realizar o **provisionamento da infraestrutura**.

```text
apply
```

### `destroy`

Utilizado para realizar o **desprovisionamento da infraestrutura**.

```text
destroy
```

### Fluxo

```text
./terraform/.infra-action
          │
          ├── apply
          │     │
          │     ▼
          │  Provisionamento
          │     │
          │     ▼
          │  AWS / Kubernetes
          │
          └── destroy
                │
                ▼
          Desprovisionamento
                │
                ▼
             AWS / Kubernetes
```

> **Atenção:** A opção `destroy` deve ser utilizada com cautela, pois remove os recursos de infraestrutura gerenciados pelo Terraform.

## Acompanhamento do processo

### 1. Github

Após ter alterado o arquivo 'apply | destroy', realiza-se um 'commit' na branch atual e um 'push' para 'origin'

```bash
git commit -am "Alterei para 'apply' e estou provisionando a infraestrtura."
```

```bash
git push origin
```

É possível acompanhar todo o processo por meio da ferramenta 'git actions' do github.

- <https://github.com/FIAP-SOFTWARE-ARCHITECTURE-TEAM/Mekano-API/actions>

Após a finalização do processo, é possível fazer a conferência da infraestrutura acessando a AWS:

- <https://aws.amazon.com/pt/>

Account ID -> 070165420894
login -> mekano-eks-admin
password -> #######

### Configurar kubectl depois do provisionamento

Após o processo (ci | cd) ter finalizado, é necessário configurar manualemnte o kubectl para posterior criação da URL de acesso da Mekano-API.

Abaixo segue o link de um passo a passo do que executar na CLOUDSHELL da AWS.

- [Configurando kubectl](../aws-infrastructure/kubctl-config.md)

## Arquitetura Operacional

O fluxo esperado é:

```text
Analista
   |
   | altera terraform/.infra-action
   |   apply   = provisionar/reprovisionar
   |   destroy = desprovisionar
   |
   | commit + push
   v
GitHub
   |
   v
GitHub Actions
   |
   +-- Resolve infrastructure action
   |      |
   |      +-- apply
   |      |    +-- Build/Test
   |      |    +-- Docker Build + Push GHCR
   |      |    +-- Terraform Plan
   |      |    +-- Terraform Apply
   |      |    +-- Deploy Kubernetes
   |      |
   |      +-- destroy
   |           +-- Cleanup Kubernetes/Ingress
   |           +-- Terraform Plan -destroy
   |           +-- Terraform Apply do plano de destroy
   |
   v
AWS
```

O backend do Terraform fica separado:

```text
terraform/bootstrap
        |
        +-- S3
        |     fiap-mekano-tfstate-070165420894
        |
        +-- DynamoDB
              mekano-terraform-locks
```

A stack principal:

```text
terraform/
   |
   +-- VPC
   +-- NAT Gateway
   +-- EKS
   +-- Node Group
   +-- EBS CSI
   +-- RDS Mekano
   +-- RDS Evolution
   +-- ElastiCache Redis
   +-- IAM / Security Groups
```
