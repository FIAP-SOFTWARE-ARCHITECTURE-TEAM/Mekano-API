# Feature Research

**Domain:** Mechanical Workshop Management System (Brazilian market)
**Researched:** 2026-06-20
**Confidence:** HIGH (verified across 14+ competitor products, official documentation, and Brazilian legislation)

## Feature Landscape

### Table Stakes (Users Expect These)

Features that every competitor in the Brazilian market offers. Missing these = product is not viable.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| **OS Lifecycle with Status Machine** | Core operation — tracks vehicle from reception to delivery. Every competitor has this as the central feature. | MEDIUM | 7 statuses: RECEBIDA → EM_DIAGNOSTICO → AGUARDANDO_APROVACAO → CANCELADA/EM_EXECUCAO → FINALIZADA → ENTREGUE. Must enforce state machine transitions. |
| **Client Registration (CPF/CNPJ)** | Mandatory for NF-e emission and legal compliance. Brazilian tax authority requires valid CPF/CNPJ on all invoices. | LOW | Validate CPF (11 digits) and CNPJ (14 digits) with check digits. Enforce uniqueness. Required field for OS creation. |
| **Vehicle Registration (Mercosul Plate)** | Placa is the primary vehicle identifier. Brazilian Mercosul format (ABC1D23) is legal standard since 2018. | LOW | Validate Mercosul format: 3 letters + 1 digit + 1 letter + 2 digits. Enforce uniqueness per client. Link to client. |
| **Service Type Catalog** | Necessário para compor OS — cada linha de serviço com valor unitário. | LOW | CRUD with name, description, unit value (> 0). Used when mecânico adds services during diagnosis. |
| **Parts/Supplies CRUD with Balance** | Every competitor has stock control. Basic entry/exit/balance is non-negotiable. | MEDIUM | Code, description, unit, current balance, minimum stock, unit cost. Balance cannot go negative. |
| **Budget Generation and Client Approval** | CDC Art. 40 requires formal budget before service execution. Orçamento prévio is a legal right. | MEDIUM | Auto-generate from OS items. Send to client. Client approves/rejects via public link. Without this flow, the workshop operates illegally. |
| **Payment Processing (Cobrança + Confirmação)** | Business requirement — workshop needs to get paid. Competitors offer PIX, boleto, card. | MEDIUM | Emit cobrança on OS finalization. Register payment confirmation. Block delivery until payment confirmed. |
| **Vehicle Delivery Registration** | Completes the OS lifecycle. Legal closure of the service contract. | LOW | Register delivery date, responsible party, recipient. Only possible after payment confirmed. |
| **OS Listing with Filters** | Daily operational need — atendentes and admin need to find OS by status, date, client, vehicle. | LOW | Paginated list with filters: status, date range, client name, plate. Requires auth. |
| **JWT Authentication** | Security baseline. Already implemented (Ed25519/EdDSA). | DONE | Built and validated. Roles: admin, atendente, mecanico, almoxarife. |
| **Minimum Stock Alerts** | Prevents stockout during service execution. All competitors alert when stock <= minimum. | LOW | Check after every stock movement. Formula: `replenishment time × average daily consumption`. |
| **Parts Withdrawal for Execution** | Registra saída física do estoque quando serviço começa. Controla inventário. | LOW | Only reserved parts can be withdrawn. Debits balance. Ends reservation. |

### Differentiators (Competitive Advantage)

Features that set the product apart. Not required for launch but create competitive moat.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| **SLA Monitoring with Auto-Expiration** | Few competitors offer SLA enforcement. Orçamento expires automatically after X days — legal safeguard and operational efficiency. | MEDIUM | `PoliticaSLA` value object with `tempoMaximoAprovacao`. Timer starts on budget generation. Auto-cancels OS when expired. Directly addresses CDC Art. 40 compliance gap. |
| **Public Client Status Tracking** | Clients can check OS status without login — reduces atendente phone calls. Only ~40% of competitors offer this. | MEDIUM | Public endpoint with OS number/UUID. Returns status, timeline, estimated completion. No auth required. |
| **Budget Approval via Public Link** | Client approves/rejects without calling. Links to SLA timer. | MEDIUM | Generate unique URL per budget. Client clicks Aprovar/Reprovar. Triggers OS status transition. |
| **Automatic Stock Reservation on Approval** | Links OS, orçamento, and estoque. Few competitors do this atomically. | HIGH | On budget approval, check availability → reserve parts → if insufficient, auto-generate purchase requisition. Coordinates 3 aggregates (OS, Estoque, RequisicaoCompra). |
| **Purchase Requisition Auto-Generation** | Two triggers: (1) parts unavailable for an approved OS, (2) stock below minimum. | MEDIUM | Statuses: EM_ABERTO → EM_ANDAMENTO → FINALIZADO → CANCELADO. Cannot cancel if linked to active OS. |
| **NF-e XML Import for Stock Entry** | Import supplier NF-e XML directly — auto-updates stock. Covers NF-e (product), NFS-e (service), NFС-e (consumer). | HIGH | Parse XML (NFe/XSD schema), validate against SEFAZ, update stock balance, link to purchase requisition. Requires understanding of Brazilian fiscal layout (NCM, CFOP, ICMS). |
| **Hybrid ID Strategy (Sequential PK + UUID)** | Sequential PK for DB performance + UUID for external references prevents ID enumeration. Not common in competitors. | LOW | Already implemented in BaseEntity. Expose UUID in API, use Long for joins. |
| **API-First Architecture** | Competitors are SaaS monoliths with UIs. API-first enables future mobile app, web frontend, or third-party integrations. | DONE | Clean Architecture multi-module already in place. Competitive advantage for extensibility vs closed competitors. |
| **RFC 7807 Problem Details for Errors** | Standardized error format improves client developer experience. | DONE | `ApiExceptionMapper` already implemented. Returns `application/problem+json`. |
| **Event-Driven OS Status Transitions** | Domain events (`OrdemDeServicoCriada`, `OrcamentoAprovado`, etc.) enable decoupled side-effects (stock reservation, payment emission, SLA timer). | MEDIUM | `EventPublisher` interface in domain, implementation in infrastructure. Events already mapped in Event Storming. |
| **Audit Trail with Soft Delete** | Track who created/updated records. Soft delete prevents data loss. | LOW | `BaseEntity` fields: `createdBy`, `updatedBy`, `updatedAt`, `isActive`, `deletedAt`. All queries filter `isActive = true`. |
| **Mechanic Commission Tracking** | Automatically calculate mechanic commissions per OS. Recruiting/retention tool. | LOW | Percentage per mechanic per service type. Auto-calculated on OS finalization. Dashboard for monthly commission report. |

### Anti-Features (Commonly Requested, Often Problematic)

Features that seem good but create problems for a backend-first MVP with 10-day timeline.

| Feature | Why Requested | Why Problematic | Alternative |
|---------|---------------|-----------------|-------------|
| **Real-Time Chat (Mecânico ↔ Cliente)** | "Client wants to ask mechanic questions directly" | Requires WebSocket infrastructure, message persistence, typing indicators, read receipts. Adds 3-5 days to timeline. | WhatsApp integration via API — send automatic status updates. Client calls workshop directly. |
| **Full Accounting Module (DRE, SPED)** | "Workshop needs complete financial management" | SPED Contábil is a separate complex domain with its own regulations. Would double the project scope. | Focus on cobrança/pagamento only. Accounting integration later via API export. |
| **AI Diagnostic Assistant** | "Auto-diagnose problems from symptoms" | Requires ML model training, tagged dataset, continuous improvement. Not feasible in 10-day backend sprint. | Structured checklist for mechanics. IA assistant is a v2 feature for competitors at premium tiers. |
| **Mobile App (Android/iOS)** | "Mechanics need to use it on the shop floor" | Requires separate frontend team, app store deployment, push notifications. | API-first — mobile app can be built in future phase consuming the same REST API. |
| **Real Multi-Gateway Payment Integration** | "Accept all payment methods" | Each gateway (Asaas, Stripe, Stone, Cielo) has different API, webhook format, certification process. | Simulated banking service with pluggable `ServicoBancario` interface. Real integration in v2. |
| **Online Scheduling / Agenda** | "Clients book appointments online" | Calendar logic, conflict resolution, time slot management, reminder system. Entire subdomain. | Focus on OS flow first. Scheduling can be added as a separate bounded context later. |
| **Multi-Workshop / Multi-Company** | "Owner has multiple shops" | Tenant isolation, shared/separate stock, consolidated reporting. Major architectural decision. | Single-workshop for MVP. Add tenant context in v2 if validated. |
| **Inventory Barcode/QR Code Scanning** | "Speed up stock entry/exit" | Requires hardware integration, mobile camera access, barcode library. | Manual entry via API for MVP. Add scanning support when mobile app is built. |

## Feature Dependencies

```
[Client Registration] ──requires──> [JWT Auth] (who registers?)
[Vehicle Registration] ──requires──> [Client Registration] (vinculado a cliente)

[OS Lifecycle]
    ├──requires──> [Client Registration]
    ├──requires──> [Vehicle Registration]
    └──requires──> [Service Type Catalog]

[Budget Generation] ──requires──> [OS Lifecycle] (OS must be in EM_DIAGNOSTICO)
[Budget Approval via Link] ──requires──> [Budget Generation]
[SLA Auto-Expiration] ──enhances──> [Budget Approval via Link] (adds expiration logic)

[Stock Reservation] ──requires──> [Budget Approval via Link] (triggered on approval)
[Stock Reservation] ──requires──> [Parts/Supplies CRUD]
[Purchase Requisition] ──requires──> [Stock Reservation] (when parts unavailable)

[Parts Withdrawal] ──requires──> [OS Lifecycle] (status = EM_EXECUCAO)
[Parts Withdrawal] ──requires──> [Stock Reservation] (must be reserved first)

[Payment Processing] ──requires──> [OS Lifecycle] (status = FINALIZADA)
[Vehicle Delivery] ──requires──> [Payment Processing] (pagamento must be CONFIRMADO)
[Vehicle Delivery] ──requires──> [OS Lifecycle] (status = FINALIZADA)

[NF-e XML Import] ──enhances──> [Parts/Supplies CRUD] (auto-updates stock)
[NF-e XML Import] ──enhances──> [Purchase Requisition] (closes the requisition)

[Public Status Tracking] ──requires──> [OS Lifecycle] (reads OS status)

[Mechanic Commission] ──requires──> [OS Lifecycle] (knows which mechanic did what)
[Mechanic Commission] ──requires──> [Payment Processing] (commission based on collected value)

[Audit Trail] ──enhances──> ALL features (cross-cutting)
```

### Dependency Notes

- **[Budget Approval via Link] requires [Budget Generation]:** Budget must be generated with calculated values before client can approve. The SLA timer starts on generation.
- **[Stock Reservation] requires [Budget Approval]:** The approval event triggers the domain policy `VerificarEstoque` which coordinates reservation and/or purchase requisition creation.
- **[Vehicle Delivery] requires [Payment Processing]:** Legal/operational requirement — vehicle cannot be released without payment confirmation. This is a hard business rule enforced by all competitors.
- **[Supply Chain] NF-e XML Import enhances Purchase Requisition:** The requisition-financeiro interaction: requisition → purchase → NF-e entry → stock update. The XML import is the mechanism to close the loop.
- **[Parts Withdrawal] requires [Stock Reservation]:** The `ReservaEstoque` aggregate must be in status `ATIVA` before physical withdrawal. Ensures no stock is taken without an OS context.

## MVP Definition

### Launch With (Phase 1)

Minimum viable product — what's needed to validate the concept within the 10-day timeline.

| # | Feature | Why Essential | Dependencies |
|---|---------|--------------|--------------|
| P1 | **OS Lifecycle** — full status machine | Core value proposition. Without this, there's no product. | JWT Auth (done), Client, Vehicle, Service catalog |
| P2 | **Client + Vehicle + Service CRUD** | Prerequisites for OS creation. Legal requirement (CPF/CNPJ). | JWT Auth (done) |
| P3 | **Budget Generation + Client Approval via Link** | CDC legal requirement (Art. 40). Core flow dependency for execution. | OS Lifecycle |
| P4 | **Parts/Supplies CRUD + Minimum Stock Alerts** | Stock control is operational necessity. | JWT Auth (done) |
| P5 | **Stock Reservation on Budget Approval** | Links OS and stock. Prevents double-allocation. | Budget Approval, Parts CRUD |
| P6 | **Parts Withdrawal on Execution Start** | Physical inventory control. | Stock Reservation |
| P7 | **Payment Processing + Vehicle Delivery** | Completes the business cycle. | OS Finalizada |
| P8 | **Public Client Status Tracking** | Reduces atendente workload. Differentiator. | OS Lifecycle |

### Add After Validation (Phase 2)

| # | Feature | Trigger for Adding | Dependencies |
|---|---------|-------------------|--------------|
| P9 | **SLA Monitoring + Auto-Expiration** | Budget flow stabilized. Adds legal compliance layer. | Budget Generation |
| P10 | **Purchase Requisition Auto-Generation** | Stock reservation working, need to handle insufficient stock. | Stock Reservation |
| P11 | **Mechanic Commission Tracking** | Payment flow validated, need to calculate team compensation. | Payment Processing |
| P12 | **Audit Trail Complete** | System in production, need traceability for disputes. | Cross-cutting |

### Future Consideration (Phase 3+)

| # | Feature | Why Defer |
|---|---------|-----------|
| P13 | **NF-e XML Import for Stock Entry** | Requires deep SEFAZ integration knowledge, XML parsing, fiscal layout mapping. Critical for production but can be simulated for MVP. |
| P14 | **Online Scheduling / Agenda** | Separate bounded context. OS flow must be solid first. |
| P15 | **Multi-Workshop Support** | Architectural decision (tenant isolation). Validate single-tenant first. |
| P16 | **Real Payment Gateway Integration** | Simulated `ServicoBancario` suffices for MVP. Real integration per-gateway per-phase. |
| P17 | **Mobile App** | API-first design enables this. Requires separate frontend team. |

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| OS Lifecycle (Status Machine) | CRITICAL | HIGH (7 states, domain events, state validation) | **P1** |
| Client Registration (CPF/CNPJ) | CRITICAL | LOW (validate + unique) | **P1** |
| Vehicle Registration (Mercosul Plate) | CRITICAL | LOW (validate + unique per client) | **P1** |
| Service Type Catalog | HIGH | LOW (CRUD) | **P1** |
| Budget Generation + Client Approval | CRITICAL | MEDIUM (public link, status transition) | **P1** |
| Parts/Supplies CRUD with Balance | HIGH | MEDIUM (balance invariants) | **P1** |
| Stock Reservation on Approval | HIGH | HIGH (cross-aggregate coordination) | **P1** |
| Parts Withdrawal on Execution | HIGH | LOW (validate reservation, debit) | **P1** |
| Payment Processing + Cobrança | CRITICAL | MEDIUM (cobrança emit + payment confirm) | **P1** |
| Vehicle Delivery Registration | HIGH | LOW | **P1** |
| OS Listing with Filters | HIGH | LOW | **P1** |
| Public Client Status Tracking | MEDIUM | MEDIUM (public endpoint) | **P1** |
| Minimum Stock Alerts | MEDIUM | LOW (check after movements) | **P1** |
| SLA Monitoring + Auto-Expiration | MEDIUM | MEDIUM (timer, policy) | **P2** |
| Purchase Requisition Auto-Generation | MEDIUM | MEDIUM | **P2** |
| Mechanic Commission Tracking | LOW | LOW | **P2** |
| Audit Trail with Soft Delete | MEDIUM | LOW (cross-cutting, already started) | **P2** |
| NF-e XML Import | HIGH | HIGH (SEFAZ schema, validation) | **P3** |
| Online Scheduling/Agenda | MEDIUM | HIGH (new bounded context) | **P3** |
| Multi-Workshop | MEDIUM | HIGH (tenant architecture) | **P3** |
| Real Payment Gateway | MEDIUM | HIGH (per-gateway certification) | **P3** |
| Mobile App | HIGH | VERY HIGH (frontend team needed) | **P3** |
| AI Diagnostics | LOW | VERY HIGH (requires ML infra) | **P3** |
| Real-Time Chat | LOW | HIGH (websocket infra) | **Anti-feature** |
| Full Accounting (DRE/SPED) | LOW | VERY HIGH | **Anti-feature** |

**Priority key:**
- P1: Must have for launch (MVP)
- P2: Should have, add in next iteration
- P3: Nice to have, future consideration

## Competitor Feature Analysis

| Feature | MecPro | Ultracar | Garage | AutoERP | MecânicaFlow | Our Approach |
|---------|--------|----------|--------|---------|-------------|--------------|
| **OS Digital Lifecycle** | ✅ Full Kanban | ✅ 30+ anos | ✅ Full | ✅ Full | ✅ Kanban | ✅ API-first, 7-status machine with domain events |
| **WhatsApp Integration** | ✅ Auto + Bot IA | ✅ Basic | ❌ No | ✅ Auto messages | ✅ Auto updates | ❌ Not in MVP — simulated email notification |
| **PIX Integration** | ✅ QR Code + Webhook | ✅ | ❌ | ✅ Asaas | ✅ | ⚠️ Simulated bank service for MVP |
| **NF-e / NFS-e / NFC-e** | ✅ All 3 (Premium) | ✅ | ✅ NF-e + NFC-e | ✅ NF-e + NFS-e | ✅ All 3 | ⚠️ Simulated in MVP, real XML in Phase 2-3 |
| **Budget Approval via Link** | ✅ WhatsApp link | ✅ | ✅ Online | ✅ WhatsApp link | ✅ Online link | ✅ Public link with SLA timer |
| **Public Status Tracking** | ❌ Not explicit | ❌ Not explicit | ❌ Not explicit | ❌ Not explicit | ✅ Link público | ✅ Public endpoint (differentiator) |
| **SLA Auto-Expiration** | ❌ Not explicit | ❌ | ❌ | ❌ | ❌ | ✅ SLA Policy (key differentiator) |
| **Stock XML Import** | ✅ Via NF XML | ⚠️ | ❌ | ✅ | ✅ Via XML | ⚠️ Phase 2-3 |
| **Auto Purchase Requisition** | ✅ By OS or min stock | ❌ | ❌ | ✅ By OS | ✅ By OS | ✅ Auto-generate on approval + min stock |
| **Mechanic Commission** | ✅ Ranking + % | ✅ | ✅ | ✅ | ✅ | ✅ Phase 2 |
| **API for Integration** | ✅ Planos superiores | ⚠️ Limited | ✅ Plano Master | ❌ | ❌ | ✅ API-first — core architectural principle |
| **Mobile App** | ✅ App Mecânico | ⚠️ | ❌ | ✅ Android | ❌ | ❌ API-first, defer |
| **AI Diagnostics** | ✅ IA Diagnóstica | ❌ | ❌ | ✅ Consultor IA | ❌ | ❌ Out of scope |
| **Multi-Company** | ✅ Multi-unidade | ✅ Redes | ❌ | ❌ | ❌ | ❌ Phase 3+ |

### Key Takeaways from Competitor Analysis

1. **No competitor offers SLA auto-expiration** — this is a genuine differentiator for Mekano. Competitors rely on manual SLA tracking.
2. **Public status tracking is rare** (~20% of competitors) — strong differentiator that reduces operational overhead for atendentes.
3. **API-first is our architectural moat** — competitors are closed SaaS. Our Clean Architecture multi-module API enables mobile/web/third-party adoption that competitors cannot match without rewriting.
4. **WhatsApp integration is becoming table stakes** in the Brazilian market (70%+ of competitors). We omit it from MVP but it must be the first Phase 2 addition after payment validation.
5. **NF-e emission is table stakes but XML import is differentiating** — every competitor can emit, but fewer auto-import supplier XML for stock entry. This is our Phase 2-3 priority.
6. **PIX integration with webhook auto-reconciliation** is a strong expectation. Our simulated bank service must anticipate the webhook callback pattern.

## Brazilian Market Specifics

### Legal Requirements (Must Implement Correctly)

| Requirement | Source | Impact on Features |
|-------------|--------|-------------------|
| **Orçamento prévio detalhado** | CDC Art. 40 | Budget must include: service description, parts list, labor cost, total value, payment terms, start/end dates, validity period. Client must explicitly approve. |
| **Proibição de serviços não autorizados** | CDC Art. 39, VI | Any service not in the approved budget is "free" — client can refuse payment. OS must prevent adding items after approval without re-authorization. |
| **Garantia mínima de 90 dias** | CDC Art. 26 | Services and parts have 90-day warranty. OS must record warranty start date and duration per item. |
| **CPF/CNPJ validation** | Receita Federal | Validate check digits. CPF: 11 digits. CNPJ: 14 digits. Block duplicates. |
| **Placa Mercosul format** | DENATRAN Res. 780/2019 | Format: `ABC1D23` (3 letters, 1 digit, 1 letter, 2 digits). Validate regex. |
| **NF-e/NFS-e fiscal emission** | SEFAZ (state) / Prefeitura (city) | Requires Certificado Digital ICP-Brasil. NF-e = ICMS (state), NFS-e = ISS (city). Workshop needs both Inscrição Estadual and Inscrição Municipal. Reforma Tributária 2026 changing layouts. |
| **NFS-e Nacional (mandatory Jan 2026)** | Lei Complementar 214/2024 | Unified national NFS-e standard. Municipalities must adopt or lose federal transfers. Our implementation must target the national standard API. |

### Fiscal Document Types Relevant to Workshops

| Type | Scope | Tax | Authority | When to Use |
|------|-------|-----|-----------|-------------|
| **NF-e (Modelo 55)** | Products | ICMS | SEFAZ (state) | Parts sale B2B, stock movement between branches |
| **NFS-e** | Services | ISS | Prefeitura (city) / National (2026) | Labor, diagnosis, maintenance service — this is the primary fiscal document for workshops |
| **NFC-e (Modelo 65)** | Consumer products | ICMS | SEFAZ (state) | Parts sale at counter (balcão) to end consumer |

**Key insight for workshops:** In most cases, the workshop issues a **NFS-e** for the total service (labor + parts considered inputs), not separate NF-e for parts. The dominant model is: service is the main activity, parts are inputs to the service. This simplifies fiscal logic but requires correct LC 116/2003 service code mapping.

### Currency and Payment Specifics

| Aspect | Brazilian Practice | Implementation Impact |
|--------|-------------------|----------------------|
| **PIX** | Instant payment, QR Code dynamic or static, webhook callback | Simulated in MVP, real integration must anticipate webhook reconciliation |
| **Boleto** | 1-3 business day settlement, API generation via banks | Simulated in MVP |
| **Credit Card** | Installments (parcelamento) common, 2-6% MDR | Parcelamento adds complexity — Tabela Price for interest calculation |
| **Payment Methods enum** | PIX, BOLETO, CARTAO_CREDITO, CARTAO_DEBITO, DINHEIRO | Already modeled in Event Storming as `MetodoPagamento` |

## Sources

### Competitor Products Analyzed
- **MecPro** (MagoWeb) — https://oficina.saas.magoweb.com.br/ (HIGH confidence, current site)
- **Ultracar** (Pareto) — https://ultracar.com.br/ (HIGH confidence, 30+ years, Bosch partner)
- **Garage** (Lumma Software) — https://garage.lummasoftware.com/ (HIGH confidence)
- **AutoERP** — https://www.autoerp.app.br/ (HIGH confidence, changelog public)
- **Orbicar** — https://orbicar.com.br/ (HIGH confidence)
- **MotorSW** — https://motorsw.com.br/ (HIGH confidence)
- **MecânicaFlow** — https://mecanicaflow.sistemasaas.com.br/ (HIGH confidence)
- **Wüst Software** — https://wust.dev.br/oficina (HIGH confidence, at R$79.90/m)
- **Automotive System** — https://www.automotivesystem.com.br/ (MEDIUM confidence)
- **Syscar** (Mc Cloud) — https://syscar.com.br/ (HIGH confidence, detailed plan comparison)
- **Manager Full** — https://managerfull.com/ (MEDIUM confidence)
- **Krossfy** — https://krossfy.com.br/ (MEDIUM confidence)
- **Gaud ERP** — https://gauderp.com.br/ (HIGH confidence, blog with fiscal expertise)
- **Mekanos** — https://www.mekanos.com.br/ (MEDIUM confidence)

### Legal / Regulatory Sources
- **CDC Lei 8.078/90** — Art. 35, 39, 40 — Consumer protection for workshop services (HIGH confidence)
- **LC 116/2003** — ISS service list for NFS-e classification (HIGH confidence)
- **LC 214/2024** — Reforma Tributária, NFS-e Nacional mandatory Jan 2026 (HIGH confidence, gov.br)
- **DENATRAN Res. 780/2019** — Mercosul plate format (HIGH confidence)
- **Portal NF-e** (SEFAZ) — https://www.nfe.fazenda.gov.br/ (HIGH confidence, technical notes 2025.002 for Reforma Tributária)
- **Portal NFS-e** (Receita Federal) — https://www.gov.br/nfse/ (HIGH confidence)
- **Guia CDC Oficinas** — https://blog.texaco.com.br/havoline/direitos-consumidor-oficinas-mecanicas/ (MEDIUM confidence)

### Domain-Specific References
- **Guia de Gestão de Oficina** (Sults) — https://www.sults.com.br/blog/gestao-de-oficina-mecanica/ (MEDIUM confidence, practical KPIs and SLA suggestions)
- **Orçamento Prévio Obrigatoriedade** — https://advogadospirituba.com.br/orcamento-previo-oficina-obrigatorio-direitos-consumidor/ (MEDIUM confidence, legal analysis)
- **OS Digital para Oficinas** (Ultracar blog) — https://ultracar.com.br/ordem-de-servico-digital-oficinas-mecanicas/ (MEDIUM confidence)

### Project Documentation
- **PROJECT.md** — `.planning/PROJECT.md` (HIGH confidence, current project context)
- **MEKANO_DOCUMENTATION.md** — `docs/MEKANO_DOCUMENTATION.md` (HIGH confidence, functional requirements)
- **Event Storming** — `docs/EventStorming_Mermaid.md` (HIGH confidence, aggregate definitions, 3 bounded contexts)

---
*Feature research for: Mekano — Mechanical Workshop Management System*
*Researched: 2026-06-20*
