---
phase: 8
slug: documentation-polish
status: validated
nyquist_compliant: true
wave_0_complete: true
created: 2026-08-08
---

# Phase 8: Documentation & Polish — Validation Strategy

## Test Strategy

| Area | Type | Strategy | Tooling |
|------|------|----------|---------|
| README content | Static | Section existence verification | `Select-String` |
| Mermaid diagrams | Static | Render check | `npx -p @mermaid-js/mermaid-cli mmdc` |
| Swagger UI | Integration | URL accessibility | `curl http://localhost:8080/q/swagger-ui` |
| Postman collection | Static | File existence + JSON valid | `Test-Path`, `ConvertFrom-Json` |
| Miro adjustment | Manual | Visual confirmation | Human review |
| Video demonstration | Manual | Record + upload | OBS Studio + YouTube/Drive |

## Sampling Rates

| Area | Rate | Rationale |
|------|------|-----------|
| README sections | 100% | All key sections must exist |
| Mermaid diagrams | 100% | Must render correctly |
| Swagger version bump | 100% | Config files, verifiable by grep |
| Postman collection | 100% | Single file |
| Miro | Manual | One-time adjustment |
| Video | Manual | Documented plan, user records |

## Wave 0 Gaps

No gaps identified. Documentation phase — all content is additive to existing files.

## Verification Commands

```powershell
# Verify README sections exist
Select-String -Path README.md -Pattern "## (Descrição|Quick Start|Arquitetura|API|Deploy|Testes|Troubleshooting|Vídeo Demonstrativo)"
Select-String -Path README.md -Pattern "sequenceDiagram"
Select-String -Path README.md -Pattern "Escalabilidade Automática"

# Verify Swagger version bump
Select-String -Path mekano-rest/src/main/java/com/fiap/mekano/rest/MekanoApiApplication.java -Pattern "2.0.0"
Select-String -Path mekano-rest/src/main/resources/openapi-config.yml -Pattern "2.0.0"

# Verify Postman collection
Test-Path mekano-rest/postman/Mekano-API-v2.0.postman_collection.json
```