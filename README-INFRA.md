# Mekano — Infraestrutura Kubernetes, Terraform e CI/CD

Esta pasta contém uma implementação de referência para os requisitos de infraestrutura do Mekano-API.

## Estrutura

- `k8s/namespace.yaml` — namespace `mekano-system`.
- `k8s/deployment.yaml` — API Mekano, 2 réplicas, probes, requests/limits e rolling update.
- `k8s/service.yaml` — `ClusterIP` na porta 8080.
- `k8s/configmap.yaml` — `QUARKUS_PROFILE` e `DB_URL`.
- `k8s/secret.yaml` — template de `DB_USER`, `DB_PASSWORD` e chaves JWT. Não coloque credenciais reais no Git.
- `k8s/hpa.yaml` — HPA `autoscaling/v2`, CPU 70%, memória 80%, min 2/max 10.
- `k8s/ingress.yaml` — Ingress NGINX opcional para `mekano.local`.
- `terraform/` — VPC, EKS, RDS PostgreSQL 16, backend S3 e locking.
- `terraform/bootstrap/` — cria uma vez o bucket S3 e a tabela DynamoDB do backend.
- `.github/workflows/cd.yml` — build, testes, Docker/GHCR, Terraform e deploy Kubernetes com rollback.

## 1. Validar manifests Kubernetes

Na raiz do repositório:

```powershell
kubectl apply -f k8s/ --dry-run=client
```

O `secret.yaml` contém apenas placeholders e serve para validação/manual. No CD, o Secret real é criado a partir dos GitHub Actions Secrets.

## 2. Testar localmente com k3d

Suba apenas o PostgreSQL local:

```powershell
docker compose up -d postgres
```

Crie o cluster:

```powershell
k3d cluster create mekano --servers 1 --agents 2
kubectl cluster-info
```

O `configmap.yaml` está configurado por padrão com `host.docker.internal:5432`, adequado para Docker Desktop/Kind no Windows. Para testar especificamente com k3d, altere temporariamente o `DB_URL` para `jdbc:postgresql://host.k3d.internal:5432/mekano`. Em EKS, o pipeline de CD substitui esse valor pelo endpoint real do RDS gerado pelo Terraform.

Crie o Secret real local usando os PEMs já usados pelo projeto:

```powershell
kubectl apply -f k8s/namespace.yaml

kubectl create secret generic mekano-secret `
  -n mekano-system `
  --from-literal=DB_USER=mekano `
  --from-literal=DB_PASSWORD=mekano `
  --from-file=privatekey.pem="$HOME/.mekano/secrets/privatekey.pem" `
  --from-file=publicKey.pem="$HOME/.mekano/secrets/publicKey.pem"
```

Para testar a imagem local no k3d:

```powershell
docker build -t mekano-api:local -f mekano-rest/src/main/docker/Dockerfile.jvm .
k3d image import mekano-api:local -c mekano
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/hpa.yaml
kubectl apply -f k8s/deployment.yaml
kubectl set image deployment/mekano mekano=mekano-api:local -n mekano-system
kubectl rollout status deployment/mekano -n mekano-system
kubectl port-forward svc/mekano 8080:8080 -n mekano-system
```

Em outro terminal:

```powershell
curl.exe http://localhost:8080/q/health/live
curl.exe http://localhost:8080/q/health/ready
kubectl get pods -n mekano-system
kubectl get hpa -n mekano-system
kubectl top pods -n mekano-system
```

## 3. Bootstrap do backend Terraform

O backend S3 precisa existir antes do `terraform init` principal.

```powershell
cd terraform/bootstrap
terraform init
terraform apply -var="state_bucket_name=mekano-terraform-state-SEU-ID-UNICO"
```

Depois:

```powershell
cd ..
Copy-Item backend.hcl.example backend.hcl
```

Edite `backend.hcl` com o bucket criado.

## 4. Provisionar EKS + RDS

No PowerShell:

```powershell
$env:TF_VAR_db_password="TroquePorUmaSenhaForte123!"
terraform init -backend-config=backend.hcl
terraform fmt -recursive
terraform validate
terraform plan
terraform apply
```

Configure o kubectl:

```powershell
aws eks update-kubeconfig --region us-east-1 --name mekano-eks
kubectl get nodes
```

Verifique o RDS:

```powershell
terraform output db_endpoint
terraform output cluster_endpoint
```

O RDS é privado. A regra de security group permite PostgreSQL/5432 somente a partir do security group dos workers EKS.

## 5. GitHub Actions

Crie estas **Repository Variables**:

- `AWS_REGION` = `us-east-1`
- `EKS_CLUSTER_NAME` = `mekano-eks`
- `TF_STATE_BUCKET` = nome globalmente único do bucket criado pelo bootstrap
- `DB_USER` = `mekano`
- `GITHUB_ACTIONS_PRINCIPAL_ARN` = ARN do IAM principal usado pelo Actions (opcional, mas recomendado)

Crie estes **Repository Secrets**:

- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `DB_PASSWORD`
- `JWT_PRIVATE_KEY` — conteúdo completo do PEM Ed25519 privado
- `JWT_PUBLIC_KEY` — conteúdo completo do PEM Ed25519 público

Após o primeiro push da imagem, deixe o pacote GHCR do `mekano-api` público para que os nodes EKS possam baixar a imagem sem `imagePullSecret`. Como evolução de segurança, use um token de pull dedicado ou ECR.

O fluxo em `main` é:

1. Maven `verify`.
2. Build da imagem Docker.
3. Push para GHCR com tag do SHA e `latest`.
4. `terraform plan/apply` para VPC + EKS + RDS.
5. Configuração do `kubectl` no EKS.
6. ConfigMap com endpoint real do RDS.
7. Secret com credenciais e PEMs vindos do GitHub Actions.
8. Aplicação dos manifests.
9. `kubectl rollout status`.
10. `kubectl rollout undo` automático se o rollout falhar.

As migrations do banco são executadas pelo próprio Quarkus/Flyway na inicialização, porque o projeto está configurado com `quarkus.flyway.migrate-at-start=true`.

## 6. Critérios de aceite

```powershell
kubectl apply -f k8s/ --dry-run=client
kubectl get svc mekano -n mekano-system
kubectl get hpa mekano -n mekano-system
kubectl describe deployment mekano -n mekano-system
```

O esperado é:

- `Service`: `ClusterIP`, `8080 -> 8080`.
- `Deployment`: 2 réplicas, resource requests/limits, startup/liveness/readiness probes.
- `HPA`: `MINPODS=2`, `MAXPODS=10`, CPU 70%, memória 80%.
