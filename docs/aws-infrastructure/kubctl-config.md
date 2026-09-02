# 1. Configurar acesso ao Kubernetes após o provisionamento

Após o provisionamento da infraestrutura AWS/EKS, é necessário configurar o acesso ao cluster Kubernetes utilizando o **AWS CloudShell**.

> **Execução:** todos os comandos desta seção devem ser executados no **AWS CloudShell**.

---

## 1.1. Validar a identidade AWS utilizada

Antes de configurar o acesso ao cluster, valide qual identidade AWS está sendo utilizada pelo CloudShell.

Execute:

```bash
aws sts get-caller-identity
```

### Resultado esperado

```json
{
    "UserId": "AIDARAVRPLNPKVEOBAKQH",
    "Account": "070165420894",
    "Arn": "arn:aws:iam::070165420894:user/mekano-eks-admin"
}
```

> **Importante:** o `Account` e o `Arn` devem corresponder à conta AWS e à identidade com permissão de acesso ao cluster EKS.

---

## 1.2. Configurar o contexto do cluster EKS

Configure o `kubectl` para utilizar o cluster `mekano-eks` na região `us-east-1`.

Execute:

```bash
aws eks update-kubeconfig \
  --region us-east-1 \
  --name mekano-eks
```

### Resultado esperado

```text
Updated context arn:aws:eks:us-east-1:070165420894:cluster/mekano-eks in /home/cloudshell-user/.kube/config
```

Esse comando atualiza o arquivo de configuração do Kubernetes:

```text
/home/cloudshell-user/.kube/config
```

e adiciona o contexto referente ao cluster EKS.

---

## 1.3. Confirmar o contexto Kubernetes ativo

Verifique qual contexto do Kubernetes está atualmente selecionado:

```bash
kubectl config current-context
```

### Resultado esperado

```text
arn:aws:eks:us-east-1:070165420894:cluster/mekano-eks
```

O contexto deve apontar para o cluster:

```text
mekano-eks
```

---

## 1.4. Verificar os acessos configurados no cluster

O Amazon EKS utiliza **Access Entries** para controlar quais identidades AWS podem acessar o cluster.

Execute:

```bash
aws eks list-access-entries \
  --cluster-name mekano-eks \
  --region us-east-1
```

### Resultado esperado

```json
{
    "accessEntries": [
        "arn:aws:iam::070165420894:role/MekanoGitHubActionsRole",
        "arn:aws:iam::070165420894:role/aws-service-role/eks.amazonaws.com/AWSServiceRoleForAmazonEKS",
        "arn:aws:iam::070165420894:role/mekano-workers-eks-node-group-20260829191931824600000004",
        "arn:aws:iam::070165420894:user/mekano-eks-admin"
    ]
}
```

A presença do usuário:

```text
arn:aws:iam::070165420894:user/mekano-eks-admin
```

indica que o usuário administrador está configurado como uma identidade autorizada no cluster.

> **Observação:** os ARNs relacionados aos recursos AWS podem variar de acordo com o ambiente e o provisionamento realizado pelo Terraform.

---

## 1.5. Criar Access Entry para o usuário administrador

Caso o usuário `mekano-eks-admin` ainda não esteja presente na lista de Access Entries, crie uma entrada de acesso para ele.

Execute:

```bash
aws eks create-access-entry \
  --cluster-name mekano-eks \
  --principal-arn arn:aws:iam::070165420894:user/mekano-eks-admin \
  --type STANDARD \
  --region us-east-1
```

### Resultado esperado

```json
{
    "accessEntry": {
        "clusterName": "mekano-eks",
        "principalArn": "arn:aws:iam::070165420894:user/mekano-eks-admin",
        "kubernetesGroups": [],
        "accessEntryArn": "arn:aws:eks:us-east-1:070165420894:access-entry/mekano-eks/user/070165420894/mekano-eks-admin/d4d0278f-441d-a794-4b1b-3a4b7c669a34",
        "createdAt": "2026-08-29T20:03:16.208000+00:00",
        "modifiedAt": "2026-08-29T20:03:16.208000+00:00",
        "tags": {},
        "username": "arn:aws:iam::070165420894:user/mekano-eks-admin",
        "type": "STANDARD"
    }
}
```

> **Atenção:** se o usuário já estiver presente em `list-access-entries`, não é necessário executar novamente o comando de criação.

---

## 1.6. Associar permissões administrativas ao usuário

Após garantir que o usuário possui uma Access Entry, associe a política administrativa do EKS.

Execute:

```bash
aws eks associate-access-policy \
  --cluster-name mekano-eks \
  --principal-arn arn:aws:iam::070165420894:user/mekano-eks-admin \
  --policy-arn arn:aws:eks::aws:cluster-access-policy/AmazonEKSClusterAdminPolicy \
  --access-scope type=cluster \
  --region us-east-1
```

### Resultado esperado

```json
{
    "clusterName": "mekano-eks",
    "principalArn": "arn:aws:iam::070165420894:user/mekano-eks-admin",
    "associatedAccessPolicy": {
        "policyArn": "arn:aws:eks::aws:cluster-access-policy/AmazonEKSClusterAdminPolicy",
        "accessScope": {
            "type": "cluster",
            "namespaces": []
        },
        "associatedAt": "2026-08-29T20:04:04.896000+00:00",
        "modifiedAt": "2026-08-29T20:04:04.896000+00:00"
    }
}
```

A política:

```text
AmazonEKSClusterAdminPolicy
```

concede acesso administrativo ao cluster no escopo definido.

---

## 1.7. Confirmar novamente as Access Entries

Após configurar o acesso, valide novamente as entradas existentes:

```bash
aws eks list-access-entries \
  --cluster-name mekano-eks \
  --region us-east-1
```

### Resultado esperado

```json
{
    "accessEntries": [
        "arn:aws:iam::070165420894:role/MekanoGitHubActionsRole",
        "arn:aws:iam::070165420894:role/aws-service-role/eks.amazonaws.com/AWSServiceRoleForAmazonEKS",
        "arn:aws:iam::070165420894:role/mekano-workers-eks-node-group-20260829191931824600000004",
        "arn:aws:iam::070165420894:user/mekano-eks-admin"
    ]
}
```

---

## 1.8. Confirmar a política associada ao usuário

Verifique quais políticas de acesso estão associadas ao usuário administrador:

```bash
aws eks list-associated-access-policies \
  --cluster-name mekano-eks \
  --principal-arn arn:aws:iam::070165420894:user/mekano-eks-admin \
  --region us-east-1
```

### Resultado esperado

```json
{
    "associatedAccessPolicies": [
        {
            "policyArn": "arn:aws:eks::aws:cluster-access-policy/AmazonEKSClusterAdminPolicy",
            "accessScope": {
                "type": "cluster",
                "namespaces": []
            },
            "associatedAt": "2026-08-29T20:04:04.896000+00:00",
            "modifiedAt": "2026-08-29T20:04:04.896000+00:00"
        }
    ],
    "clusterName": "mekano-eks",
    "principalArn": "arn:aws:iam::070165420894:user/mekano-eks-admin"
}
```

O resultado deve indicar a política:

```text
AmazonEKSClusterAdminPolicy
```

com escopo:

```text
type: cluster
```

---

## 1.9. Atualizar novamente o kubeconfig

Após as alterações de acesso, atualize novamente a configuração do `kubectl`:

```bash
aws eks update-kubeconfig \
  --region us-east-1 \
  --name mekano-eks
```

### Resultado esperado

```text
Updated context arn:aws:eks:us-east-1:070165420894:cluster/mekano-eks in /home/cloudshell-user/.kube/config
```

---

## 1.10. Validar os nodes do cluster

Por fim, valide se o `kubectl` consegue acessar os nodes do cluster:

```bash
kubectl get nodes
```

### Resultado esperado

Os nomes e tempos podem variar, mas os nodes devem apresentar status `Ready`.

```text
NAME                           STATUS   ROLES    AGE   VERSION
ip-10-20-12-250.ec2.internal   Ready    <none>   35m   v1.34.10-eks-cb19647
ip-10-20-31-189.ec2.internal   Ready    <none>   35m   v1.34.10-eks-cb19647
```

### Resultado da validação

Se os nodes estiverem com:

```text
STATUS = Ready
```

o acesso ao cluster Kubernetes foi configurado corretamente.

---

# 2. Validar a aplicação no Kubernetes

Após confirmar o acesso ao cluster, valide se os componentes da **Mekano-API** estão executando corretamente.

> **Execução:** os comandos desta seção devem ser executados no **AWS CloudShell**.

---

## 2.1. Verificar os Pods da aplicação

Execute:

```bash
kubectl get pods -n mekano-system
```

### Resultado esperado

Os nomes dos Pods e seus tempos podem variar. O importante é que os componentes estejam com status `Running` e os containers estejam `Ready`.

Exemplo:

```text
NAME                              READY   STATUS    RESTARTS   AGE
evolution-api-64bb99cffd-4vck    1/1     Running   0          21m
mekano-7b9f8dfcbc-9ccl4           1/1     Running   0          10m
mekano-7b9f8dfcbc-kxdvc           1/1     Running   0          10m
```

### O que deve ser validado

A aplicação deve apresentar:

```text
mekano          Running
evolution-api   Running
```

Além disso, o campo `READY` deve indicar que todos os containers esperados estão prontos.

Por exemplo:

```text
1/1
```

---

## 2.2. Verificar os Services da aplicação

Após validar os Pods, verifique os Services disponíveis no namespace:

```bash
kubectl get svc -n mekano-system
```

### Resultado esperado

```text
NAME            TYPE        CLUSTER-IP       EXTERNAL-IP   PORT(S)     AGE
evolution-api   ClusterIP   172.20.76.52     <none>        5033/TCP     22m
mekano          ClusterIP   172.20.109.57    <none>        8080/TCP     21m
```

Os Services são responsáveis por disponibilizar os Pods dentro da rede do cluster Kubernetes.

Neste ambiente:

```text
evolution-api → porta 5033
mekano        → porta 8080
```

O acesso público da aplicação será realizado posteriormente através do **Ingress Controller** e do Load Balancer provisionado na AWS.

---

# 3. Obter a URL pública da aplicação

Após validar os Pods e Services, obtenha o endereço público disponibilizado pelo **Ingress Controller**.

> **Execução:** os comandos desta seção devem ser executados no **AWS CloudShell**.

---

## 3.1. Obter o hostname do Load Balancer

Execute:

```bash
LB=$(kubectl get svc ingress-nginx-controller \
  -n ingress-nginx \
  -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')

echo "$LB"
```

O comando:

1. Localiza o Service `ingress-nginx-controller`;
2. Obtém o hostname do Load Balancer;
3. Armazena o resultado na variável `LB`;
4. Exibe o endereço público no terminal.

### Resultado esperado

O hostname será semelhante a:

```text
a1030cb9b6ca844a59abc37d02301e9a-354223907.us-east-1.elb.amazonaws.com
```

> **Observação:** o hostname do Load Balancer é gerado pela AWS e pode ser diferente em cada provisionamento.

---

## 3.2. Exibir os endereços públicos da aplicação

Com a variável `LB` configurada, execute:

```bash
echo "API:     http://$LB/"
echo "Health:  http://$LB/q/health"
echo "OpenAPI: http://$LB/q/openapi"
echo "Swagger: http://$LB/q/swagger-ui/"
```

### Resultado esperado

```text
API:     http://a1030cb9b6ca844a59abc37d02301e9a-354223907.us-east-1.elb.amazonaws.com/
Health:  http://a1030cb9b6ca844a59abc37d02301e9a-354223907.us-east-1.elb.amazonaws.com/q/health
OpenAPI: http://a1030cb9b6ca844a59abc37d02301e9a-354223907.us-east-1.elb.amazonaws.com/q/openapi
Swagger: http://a1030cb9b6ca844a59abc37d02301e9a-354223907.us-east-1.elb.amazonaws.com/q/swagger-ui/
```

---

## 3.3. Acessar o Swagger da aplicação

Neste momento, a **Mekano-API já possui um endpoint público**, permitindo acessar a documentação interativa da API através do Swagger UI.

A URL será:

```text
http://<LOAD_BALANCER>/q/swagger-ui/
```

Por exemplo:

```text
http://a1030cb9b6ca844a59abc37d02301e9a-354223907.us-east-1.elb.amazonaws.com/q/swagger-ui/
```

O endereço pode ser aberto diretamente em um navegador para visualizar e testar os endpoints disponibilizados pela API.

---

## 3.4. Endpoints públicos disponíveis

Após o provisionamento e configuração do Ingress, os principais endpoints públicos são:

| Endpoint | Finalidade |
| --- | --- |
| `/` | Endpoint principal da API |
| `/q/health` | Verificação de saúde da aplicação |
| `/q/openapi` | Especificação OpenAPI |
| `/q/swagger-ui/` | Interface gráfica do Swagger |

### Fluxo da infraestrutura

O acesso externo à aplicação segue o fluxo:

```text
Internet
   │
   ▼
AWS Load Balancer
   │
   ▼
Ingress NGINX
   │
   ▼
Kubernetes Service
   │
   ▼
Mekano-API Pods
   │
   ▼
Aplicação
```

Dessa forma, após concluir os passos **32, 33 e 34**, é possível confirmar:

- acesso administrativo ao cluster EKS;
- comunicação do `kubectl` com o Kubernetes;
- nodes do cluster em estado `Ready`;
- Pods da aplicação em execução;
- Services corretamente registrados;
- Load Balancer público disponível;
- acesso externo à Mekano-API;
- Swagger UI disponível para testes da API.
