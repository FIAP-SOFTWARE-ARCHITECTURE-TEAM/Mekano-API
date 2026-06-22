---
phase: 1
reviewers: [codex]
reviewed_at: 2026-06-22T12:17:35Z
plans_reviewed: [01-01-PLAN.md, 01-02-PLAN.md, 01-03-PLAN.md, 01-04-PLAN.md, 01-05-PLAN.md, 01-06-PLAN.md]
---

# Cross-AI Plan Review — Phase 1

## Codex Review

### Plan 01-01: Auth Foundation

**Summary:** Solid foundation plan and correctly prioritized as Wave 1 blocker, but it is currently under-specified on security-hardening details (refresh token lifecycle, revocation semantics, key management, and abuse controls). It likely delivers AUTH-01/02 baseline, but without explicit test and operational controls it carries meaningful risk.

**Strengths:**
- Clear alignment with phase goals: JWT, roles, protected endpoints
- Good technical choices for Ed25519/EdDSA with SmallRye JWT
- Includes role model (user_roles N:N) consistent with future authorization growth
- Refresh token rotation with locking shows concurrency awareness
- Touches seeding and user resource updates, helping bootstrap environments

**Concerns:**
- **[HIGH]** Refresh-token replay handling is not explicit (token family invalidation, stolen-old-token behavior, device/session scope)
- **[HIGH]** Key management not specified (where private key lives per env, rotation strategy, startup validation/fail-fast)
- **[MEDIUM]** Missing brute-force/rate-limit strategy for login/refresh endpoints
- **[MEDIUM]** Authorization matrix not mapped endpoint-by-endpoint; risk of gaps or accidental overexposure
- **[MEDIUM]** No explicit migration/backfill strategy if existing users need default roles
- **[LOW]** PESSIMISTIC_WRITE on refresh rotation may become contention hotspot under load

**Suggestions:**
- Define refresh-token model explicitly: jti, family_id, expiry, revoked-at, replaced-by, and replay response behavior
- Add auth threat-focused tests: invalid/expired token, wrong signature/alg, role mismatch, replay attempt, concurrent refresh
- Add environment-specific key loading rules and documented rotation runbook
- Add login/refresh abuse controls (rate limit + structured audit logs)
- Freeze endpoint-role matrix in a single table used by tests/docs

**Risk Assessment:** MEDIUM-HIGH

---

### Plan 01-02: Cliente CRUD

**Summary:** Well-scoped CRUD slice with good domain modeling (CPF VO, validation, soft delete). It should meet OS-01/02 if uniqueness, normalization, and delete semantics are consistently enforced across API, DB, and repository queries.

**Strengths:**
- Strong domain-first modeling with VO usage
- CPF checksum validation called out explicitly
- Soft delete included from start, matching architectural decision
- Layer split matches Clean Architecture boundaries

**Concerns:**
- **[HIGH]** CPF uniqueness under soft delete is unclear (global unique vs unique among active records)
- **[MEDIUM]** No explicit duplicate-handling contract (DB conflict -> domain/API error mapping)
- **[MEDIUM]** Partial update semantics and validation rules not described (null/blank normalization)
- **[LOW]** Endereco flattening can create schema rigidity for future address evolution

**Suggestions:**
- Define normalized CPF storage (digits-only) and unique index strategy compatible with soft delete
- Specify error mapping for conflict/validation (409 vs 422/400) and standard error payload
- Add tests for CPF edge cases, duplicate CPF, soft-deleted re-create behavior, and pagination/filtering with soft delete
- Define query defaults to exclude soft-deleted records consistently

**Risk Assessment:** MEDIUM

---

### Plan 01-03: Veiculo CRUD

**Summary:** Good focused plan with practical normalization strategy for plate handling and explicit legacy + Mercosul support. Main risk is correctness of normalization/uniqueness across input variants and migration compatibility.

**Strengths:**
- Plate VO and format constraints are explicit
- Normalized uppercase/no-hyphen storage reduces duplicate variants
- Unique constraint aligns with business expectation
- Kept scope constrained to CRUD

**Concerns:**
- **[HIGH]** Plate uniqueness with soft delete is not defined (same issue as Cliente)
- **[MEDIUM]** Regex-only validation may pass invalid edge cases if business rules expand (state/category constraints)
- **[MEDIUM]** No explicit handling for historic records already stored with hyphen/case differences
- **[LOW]** Missing explicit API contract for input echoing (original format vs normalized format in response)

**Suggestions:**
- Define one canonical normalization function used in DTO mapper + domain + DB constraints
- Add migration/data-fix strategy for pre-existing non-normalized plates
- Add parameterized tests for valid/invalid old + Mercosul formats and duplicate-by-variant cases
- Decide and document response format (canonical normalized recommended)

**Risk Assessment:** MEDIUM

---

### Plan 01-04: Servico CRUD

**Summary:** Simple and appropriately minimal plan; likely fastest of Wave 2. It meets OS-05/06 at baseline, but needs tighter money-handling and authorization test coverage to avoid subtle production issues.

**Strengths:**
- Scope is lean, reducing execution risk
- Validation at both domain and DB layers is good defense-in-depth
- Admin-only access is clearly recognized
- Avoids unnecessary VO over-design for this phase

**Concerns:**
- **[MEDIUM]** Monetary type/precision not specified (BigDecimal scale/rounding, DB column precision)
- **[MEDIUM]** Missing explicit negative/zero and precision edge-case tests
- **[MEDIUM]** Role protection must include all methods (list/get/create/update/delete), not just write endpoints
- **[LOW]** Soft delete + uniqueness/business identity not discussed (e.g., duplicate service names)

**Suggestions:**
- Standardize monetary contract (BigDecimal, fixed scale, DB NUMERIC(p,s))
- Add validation/error tests for boundary values and malformed decimals
- Add authorization matrix tests for every endpoint/method
- Define whether duplicate service descriptions are allowed

**Risk Assessment:** LOW-MEDIUM

---

### Plan 01-05: OrdemDeServico

**Summary:** This is the critical business core and the plan has good architecture choices (explicit transitions, transition matrix, optimistic locking, public status endpoint). It is directionally strong, but it currently under-specifies transactional invariants, concurrent updates, and security boundaries around public status data exposure.

**Strengths:**
- Explicit state-machine methods prevent illegal direct status mutation
- Transition matrix centralization is maintainable and testable
- @Version on OS is the right baseline for concurrent workflow updates
- Public status endpoint included to satisfy AUTH-03/OS-15
- Separate tables for child aggregates keeps model extensible

**Concerns:**
- **[HIGH]** State transition authorization is not explicit per transition (who can move from X->Y)
- **[HIGH]** Concurrency behavior unclear for simultaneous diagnose/update actions (conflict responses, retry semantics)
- **[HIGH]** Public status endpoint may leak sensitive data without strict response projection
- **[MEDIUM]** Missing invariant definitions for required associations (OS must have cliente+veiculo always; service/parts rules at each state)
- **[MEDIUM]** 8-state design in phase with only two implemented transitions risks scope creep
- **[MEDIUM]** No idempotency strategy for transition commands (duplicate request handling)
- **[LOW]** Three-table migration complexity raises rollout risk without integration migration tests

**Suggestions:**
- Define transition policy matrix including allowed roles + preconditions + postconditions
- Implement dedicated command methods for each transition with explicit domain exceptions
- Lock down public status DTO to minimum fields (status, protocol, timestamps) and avoid internal notes/costs
- Add conflict-handling contract (409 Conflict with version info) and test concurrent transitions
- Scope-control: implement only required transitions now; keep future states as planned but inactive if possible
- Add end-to-end tests covering OS create -> diagnose start, including invalid transitions and auth failures

**Risk Assessment:** HIGH

---

### Plan 01-06: Sequence Diagrams

**Summary:** Good documentation plan and correctly placed after implementation waves, but quality depends on strict synchronization with actual endpoints/rules. If done manually without validation checklist, diagrams can quickly drift from code.

**Strengths:**
- Covers key required flows, including public consultation
- Includes valid/invalid branches, which improves operational clarity
- Mermaid format is lightweight and repo-friendly
- Layer annotations support Clean Architecture communication

**Concerns:**
- **[MEDIUM]** Drift risk: diagrams may not match real authorization/error behavior
- **[LOW]** No acceptance checklist linking diagram steps to actual endpoint methods/use cases
- **[LOW]** Lifecycle diagram may over-document future transitions not implemented in this phase

**Suggestions:**
- Add a traceability table: each diagram step references concrete endpoint/use-case class
- Include explicit error paths (401, 403, 404, 409, 422) for core scenarios
- Add PR checklist item requiring diagram update when flow-affecting code changes
- Keep lifecycle diagram phase-scoped; mark future transitions as "planned"

**Risk Assessment:** LOW-MEDIUM

---

## Cross-Plan Assessment

### Summary
The wave decomposition is strong and mostly coherent with dependencies. The largest delivery risks are concentrated in auth hardening and OS state/concurrency/security semantics, not in basic CRUD implementation.

### Top Cross-Cutting Risks
- **[HIGH]** Soft delete + uniqueness strategy is unresolved across entities (Cliente/Veiculo and potentially others)
- **[HIGH]** Insufficient explicit security test plan (authn/authz matrix, replay, token abuse)
- **[MEDIUM]** Migration sequencing/data compatibility across V6-V10 may break in shared dev DBs without clear ordering/reset strategy
- **[MEDIUM]** Vertical split by entity can create integration lag in Wave 3 unless shared contracts are frozen early
- **[MEDIUM]** Test strategy appears uneven; CRUD/domain tests are implied, but cross-module integration and concurrency tests are essential for this phase

### Recommended Global Improvements
- Freeze cross-cutting contracts now: error model, soft-delete semantics, unique constraints, normalization rules
- Define a mandatory test matrix per requirement (unit + integration + security + concurrency)
- Add integration checkpoints between waves (not only at wave end) to reduce merge/integration risk with 5 developers
- Add performance/safety basics early: auth endpoint rate-limits, key loading validation, structured audit logs

### Overall Phase Risk
**MEDIUM-HIGH** — Plan quality is good and achievable in 10 days with 5 developers, but success depends on tightening security/concurrency/uniqueness details before implementation starts.

---

## Consensus Summary

Only one reviewer (Codex) completed the review. Gemini CLI failed due to authentication issues. Key findings:

### Agreed Strengths
- Clear wave decomposition with correct dependency ordering
- Strong technical choices (Ed25519, SmallRye JWT, transition matrix)
- Good domain-driven modeling with Value Objects
- Soft delete and Clean Architecture consistency across all plans

### Agreed Concerns
- Soft delete + uniqueness semantics need to be resolved globally
- Auth security hardening needs more detail (replay, key rotation, rate limits)
- OS concurrency and state transition authorization not fully specified
- Integration risk from 5 parallel devs needs active management

### Divergent Views
- N/A — single reviewer only

### Next Step
To incorporate feedback into planning: `/gsd-plan-phase 1 --reviews`
