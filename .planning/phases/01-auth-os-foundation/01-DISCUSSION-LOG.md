# Phase 1: Auth & OS Foundation - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-22
**Phase:** 1-Auth & OS Foundation
**Areas discussed:** Autenticação JWT, Modelo do Cliente (CPF/CNPJ), Placa do Veículo, Máquina de estados da OS, Roles e permissões, Divisão do trabalho

---

## Autenticação JWT

| Option | Description | Selected |
|--------|-------------|----------|
| Grupo único | Claim `groups` com role name | ✓ |
| Múltiplos grupos | Suporte a múltiplos papéis no JWT | |
| 15 minutos | Mais seguro, com refresh token | ✓ |
| 1 hora | Balanço segurança/usabilidade | |
| 8 horas (turno) | Dura uma jornada de trabalho | |
| Sim (refresh token) | Access curto + refresh com rotação | ✓ |
| Não (só access token) | Simplicidade, sem refresh | |
| Script no build | Geração automatizada de chaves | ✓ |
| Comando manual | openssl manual no setup | |
| Coluna na tabela users | Role como coluna na users | |
| Tabela separada user_roles | N:N flexível | ✓ |
| Email + senha | Login por email | ✓ |
| Email ou CPF + senha | Flexível para atendentes | |
| Rotação automática | Cada uso gera novo par | ✓ |
| Sim (logout endpoint) | POST /auth/logout | ✓ |
| Não (só client-side) | Limpar token no client | |
| Só admin cria users | Apenas admin gerencia | ✓ |
| Admin + atendente | Atendente também cria | |
| Só sub + groups | Mínimo necessário | |
| sub + groups + name | Incluir nome no JWT | ✓ |
| URL fixa (MP_JWT_ISSUER) | Issuer configurado via env var | ✓ |

**User's choice:** Grupo único, 15 min, refresh com rotação, script de build, user_roles table, email+senha, logout endpoint, só admin cria, sub+groups+name, issuer fixo
**Notes:** Nenhuma observação adicional

---

## Modelo do Cliente (CPF/CNPJ)

| Option | Description | Selected |
|--------|-------------|----------|
| Só CPF | Pessoa física apenas na Fase 1 | ✓ |
| Tipo explícito + documento único | PF/PJ + CPF/CNPJ | |
| VO no domain | Value Object Cpf com validação | ✓ |
| Só Bean Validation | Validação no DTO apenas | |
| Mínimos | nome, CPF, email, telefone | ✓ |
| Sim, endereço completo | logradouro, número, bairro, cidade, UF, CEP | ✓ |
| Sim, campo único | Um campo endereço livre | |
| Não (sem endereço) | Endereço fica para depois | |
| Sim (1:N) | Cliente com múltiplos veículos | ✓ |
| Não (1:1) | Um veículo por cliente | |

**User's choice:** Só CPF, VO no domain, campos mínimos + endereço completo, múltiplos veículos
**Notes:** Endereço será Value Object `Endereco` no domain

---

## Placa do Veículo

| Option | Description | Selected |
|--------|-------------|----------|
| Normalizada sem hífen | Uppercase, sem hífen no banco | ✓ |
| Formato original | Como o usuário digitou | |
| Regex único ambos | Cobre Mercosul + antigo automaticamente | ✓ |
| Regex separados + tipo | Campo formato explícito | |
| Sim (única) | UNIQUE constraint | ✓ |
| Não (duplicatas) | Permite placas iguais | |

**User's choice:** Normalizada uppercase sem hífen, regex único, placa única
**Notes:** —

---

## Máquina de Estados da OS

| Option | Description | Selected |
|--------|-------------|----------|
| Matriz completa | Todos os estados e transições desde Fase 1 | ✓ |
| Só Fase 1 | Apenas RECEBIDA → EM_DIAGNOSTICO | |
| Transition methods explícitos | `os.iniciarDiagnostico()` sem setStatus() | ✓ |
| Método genérico transitar() | `os.transitar(StatusOS.EM_DIAGNOSTICO)` | |
| Com serviços solicitados | OS já nasce com serviços | ✓ |
| Só cliente+veículo | OS vazia, mecânico adiciona | |
| Separado (ServicoExecutado + PecaUsada) | Duas entidades separadas | ✓ |
| Item genérico (serviço ou peça) | Um VO ItemOS com tipo | |

**User's choice:** Matriz completa, transition methods explícitos, OS com serviços, ServicoExecutado + PecaUsada separados
**Notes:** —

---

## Roles e Permissões

| Option | Description | Selected |
|--------|-------------|----------|
| Admin + Atendente (Clientes CRUD) | Ambos podem gerenciar clientes | ✓ |
| Só Admin (Clientes CRUD) | Apenas admin | |
| Admin + Atendente (Veículos CRUD) | Ambos podem gerenciar veículos | ✓ |
| Só Admin (Veículos CRUD) | Apenas admin | |
| Só Admin (Serviços CRUD) | Tipos de serviço configurados pelo admin | ✓ |
| Todos os perfis (OS list) | Todos veem OS; admin+atendente criam | ✓ |
| Admin + Atendente (OS list/create) | Ambos criam e listam | |
| Mecânico + admin (diagnóstico) | Ambos podem iniciar diagnóstico | ✓ |
| Só mecânico (diagnóstico) | Apenas mecânico | |
| @PermitAll (OS pública) | Sem auth, sem tracking code | ✓ |
| Com tracking code | Requer código de rastreio | |

**User's choice:** admin+atendente para clientes e veículos, só admin para serviços, todos listam OS, admin+atendente criam OS, mecânico+admin diagnosticam, @PermitAll no endpoint público
**Notes:** —

---

## Divisão do Trabalho

| Option | Description | Selected |
|--------|-------------|----------|
| Por entidade (vertical) | Cada dev pega entidade completa | ✓ |
| Auth em par + resto individual | 2 devs no auth, resto individual | |
| Por camada com review | Domain/infra/rest em paralelo | |
| Entidades primeiro, auth depois | Modelar dados antes do login | ✓ |
| Auth primeiro, depois entidades | Login primeiro para desbloquear testes | |

**User's choice:** Vertical por entidade, entidades primeiro
**Notes:** Dev1: Auth, Dev2: Cliente, Dev3: Veículo, Dev4: Serviço, Dev5: OrdemDeServico

---

## the agent's Discretion

- Detalhes de implementação não cobertos nas decisões
- Estrutura exata de testes
- Configuração específica de caches (nomes, TTLs)

## Deferred Ideas

- CNPJ (pessoa jurídica) — Fase 2
- Tracking code para consulta pública — não necessário, UUID é suficiente

---

*Audit trail: Phase 1-Auth & OS Foundation*
*Date: 2026-06-22*
