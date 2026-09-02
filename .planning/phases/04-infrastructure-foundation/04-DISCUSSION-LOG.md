# Phase 4: Infrastructure Foundation — Discussion Log

**Date:** 2026-08-08
**Status:** Completed

## Areas Discussed

### Docker Strategy
- **User's goal:** Refinar docker-compose existente, não criar do zero. Equipe relatou dificuldades ao subir o ambiente (portas/network, outros erros).
- **Key decisions:**
  - Dockerfile JVM do Quarkus existente como base
  - Full stack docker-compose (app + banco), single `docker compose up`
  - Documentar troubleshooting no README
- **User note:** "Não me lembro ao certo dos erros, vou ter que perguntar para eles futuramente."

### K8s / Terraform / HPA / CD
- **User's decision:** Não implementar agora. Criar tasks no Azure DevOps para Elias planejar e implementar de forma independente.
- **K8s manifests (INF-02), Terraform (INF-03), CD pipeline (INF-04), HPA** → responsabilidade do Elias

## Not Discussed (but in scope)
- HPA metrics configuration (Elias decide)
- Terraform provider choice (Elias decide)
- Image registry (Elias decide)

## Deferred Ideas
- K8s em cluster real — Elias define
- CD automático — Elias define
- Terraform remoto — Elias define
- Mermaid CI/CD — documentar após Elias implementar

---

*Discussion completed: 2026-08-08*