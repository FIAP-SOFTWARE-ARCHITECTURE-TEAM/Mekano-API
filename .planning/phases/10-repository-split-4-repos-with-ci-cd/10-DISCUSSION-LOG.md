# Phase 10: Repository Split — 4 repos with CI/CD - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-09-22
**Phase:** 10-repository-split-4-repos-with-ci-cd
**Areas discussed:** State + Flyway (DQ1+DQ5+DQ7), Deploy + segredos (DQ6+DQ8), Mecânica do split (DQ3+DQ4)

---

## State + Flyway (DQ1+DQ5+DQ7)

### Flyway ownership
| Option | Description | Selected |
|--------|-------------|----------|
| Fica na Mekano-API | App owns schema, migrate-at-start, 33 V-files já no repo | ✓ |
| Migra para Mekano-DB | Flyway CLI no CD do DB | |
| Dividir | DDL/seeds DB, migração API | |

**User's choice:** Fica na Mekano-API (Recomendado)

### VPC ownership
| Option | Description | Selected |
|--------|-------------|----------|
| VPC na Infra + remote_state | key mekano/infra mantida; DB consome outputs | ✓ |
| VPC no Mekano-DB | inverte ordem de apply | |
| Módulo compartilhado | 5º repo — fora do requirement | |

**User's choice:** VPC na Infra + remote_state (Recomendado)

### State surgery
| Option | Description | Selected |
|--------|-------------|----------|
| state mv manual com backup | pull+ARN snapshot, gate humano, plan duplo | ✓ |
| moved blocks declarativos | automático no próximo plan | |
| import + state rm | janela de double-create | |

**User's choice:** state mv manual com backup (Recomendado)

### Bootstrap
| Option | Description | Selected |
|--------|-------------|----------|
| Nunca re-rodar | só head-bucket/describe-table | ✓ |
| Re-rodar nos dois repos | idempotente mas desnecessário | |

**User's choice:** Nunca re-rodar (Recomendado)

### Apply ordering
| Option | Description | Selected |
|--------|-------------|----------|
| Documentar + falha segura | README/DEPLOY; remote_state falha com erro claro | |
| Gate automático entre repos | DB espera run verde do Infra via API GitHub | ✓ |
| depends_on inline | não faz sentido cross-repo | |

**User's choice:** Gate automático entre repos

### RDS deletion_protection
| Option | Description | Selected |
|--------|-------------|----------|
| Ligar deletion_protection | true como primeira mudança no DB | ✓ |
| Manter como está | menos diff, menos drift | |

**User's choice:** Ligar deletion_protection (Recomendado)

### Plan timing
| Option | Description | Selected |
|--------|-------------|----------|
| Plan em PR, apply em main | padrão GitOps | ✓ |
| Plan só no main | menos feedback em PR | |
| Plan em PR + re-plan no main | defesa em profundidade | |

**User's choice:** Plan em PR, apply em main (Recomendado)

### Runbook location
| Option | Description | Selected |
|--------|-------------|----------|
| STATE-SPLIT.md no repo Infra | versionado, revisável | ✓ |
| Só na documentação GSD | some com a fase | |
| Nos dois READMEs | risco de drift | |

**User's choice:** STATE-SPLIT.md no repo Infra (Recomendado)

---

## Deploy + segredos (DQ6+DQ8)

### kubectl/helm owner
| Option | Description | Selected |
|--------|-------------|----------|
| Mekano-Infra | manifestos + apply no repo Infra | ✓ |
| Mekano-API | app dono do deploy | |
| Híbrido | API dispara repo_dispatch no Infra | |

**User's choice:** Mekano-Infra (Recomendado)

### OIDC model
| Option | Description | Selected |
|--------|-------------|----------|
| 1 role, sub por repo | condição ref:refs/heads/main | ✓ |
| 4 roles separadas | least-privilege total | |
| Role + policies por condição | complexidade desnecessária | |

**User's choice:** 1 role, sub por repo (Recomendado)

### DB endpoint propagation
| Option | Description | Selected |
|--------|-------------|----------|
| SSM Parameter Store | apply DB grava; API lê | ✓ |
| remote_state na API | acopla API ao state do banco | |
| GH variable manual | propenso a drift | |

**User's choice:** SSM Parameter Store (Recomendado)

### Environment gate
| Option | Description | Selected |
|--------|-------------|----------|
| production + aprovador | 4 repos, required reviewers | ✓ |
| Sem aprovação | push errado aplica direto | |
| Gate só em Infra/DB | API/Lambda automáticos | |

**User's choice:** production + aprovador (Recomendado)

### JWT key distribution
| Option | Description | Selected |
|--------|-------------|----------|
| Chaves só na API | privateKey nunca em repo; publicKey tracked | ✓ |
| Espelhar via GH secrets | prepara pra Lambda emitir token | |
| Secrets Manager AWS | mais infra pro assignment | |

**User's choice:** Chaves só na API (Recomendado)

### GHCR path
| Option | Description | Selected |
|--------|-------------|----------|
| Auto via owner + recriar secret | path muda com org nova | |
| Migrar imagens antigas | desnecessário | |
| Migrar para ECR | foge do escopo | |
| *(free text)* manter FIAP | path GHCR continua na org FIAP | ✓ |

**User's choice:** vamos manter fiap (free text) → GHCR permanece sob org FIAP

---

## Mecânica do split (DQ3+DQ4)

### Org destino dos repos
| Option | Description | Selected |
|--------|-------------|----------|
| Repos em TechMinds-Group | convenção Nome-Camada | |
| Tudo na org FIAP | coerente com GHCR e repo antigo | ✓ |

**User's choice:** Tudo na org FIAP

### Split mechanics + old repo fate
| Option | Description | Selected |
|--------|-------------|----------|
| filter-repo + archive | preserva ~580 commits; archive+redirect | ✓ |
| Copy-in sem história | 1 commit inicial | |
| Promover develop primeiro | promote antes de dividir | |

**User's choice:** filter-repo + archive (Recomendado)

---

## the agent's Discretion

- Estrutura exata dos workflows decompostos da cd.yml por repo
- Ordem das tasks de preflight
- Formato do DEPLOY.md / Mermaid
- Publicar db_endpoint também como output TF além do SSM

## Deferred Ideas

- DQ2 Lambda scope/runtime/IaC — não discutido; abrir no plan
- Módulo VPC compartilhado (5º repo)
- Migração GHCR → ECR
- Escopo auth CPF do brief anterior (fora do requirement)
