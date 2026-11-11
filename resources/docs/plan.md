# EXR — Plan & Agent Operating Manual

> **Product:** EXR (Event eXploreR) — release-date & live-event tracking with notifications.
> **User goal:** learn production-grade system design/architecture hands-on (Java 21 + microservices) using this project as the vehicle, and prepare for a system-design interview.
> **Last updated:** Day 0 — 2026-10-28 (rev 13: local environment verified — JDK 21 activated via user-level `JAVA_HOME`, Maven confirmed running on Java 21; environment table added as section 11.3)
> **Companion docs:** `resources/docs/projectDetails.md` (feature spec) · `resources/docs/decisions.md` (ADR log, created Phase 1)

---

## 0. RESUME HERE (state as of 2026-10-28, end of Day 0)

**Status: planning complete, ZERO code written.** The workspace holds `need.txt`, `.gitignore`, `.idea/` (IntelliJ) and `resources/docs/`. There is **no `pom.xml`, no Maven wrapper, and no `.git` repository** yet. No git history exists.

**Docs live in `resources/docs/`** — `plan.md` (this file) and `projectDetails.md`. They were moved there from `docs/` at the end of Day 0.

> **KNOWN ISSUE — FIXED (rev 12).** The `.gitignore` previously contained a bare `resources/` pattern. A pattern with no leading slash matches a directory named `resources` **at any depth**, so it silently ignored:
> - `resources/docs/**` — these planning docs,
> - **`src/main/resources/**`** — every `application.yml`, Flyway migration and GraphQL schema,
> - **`src/test/resources/**`** — test config and WireMock stubs.
>
> Proven with `git check-ignore`: `.gitignore:90:resources/` matched `src/main/resources/application.yml`. **A fresh clone would neither build nor run, and none of the design work would be in version control.**
> **Resolution:** the pattern was removed and replaced with an explanatory warning comment inside `.gitignore`; re-verified with `git check-ignore` that all three paths are now trackable. **Lesson for later:** never add a directory pattern without a leading slash, and never name a docs folder `resources` in a Java project.

**What the project is:** EXR — an event / release-date tracker (games, movies, TV series, anime, live local events) that notifies subscribers before they miss a release. Java 21 + Spring Boot 3 microservices on AWS, built as the vehicle for learning production system design. Full feature spec: `resources/docs/projectDetails.md`.

**Architecture in one line:** microservices from a monorepo — `api-gateway`, `identity`, `catalog`, `notification`, `payment` (+ an optional thin `graphql-bff`), database-per-service, Kafka for domain events via a transactional outbox, SQS/SNS for fan-out and delayed reminders, Redis for cache + rate limiting + idempotency. Details in sections 3, 3.1, 3.4.

**How to restart in three steps**
1. Read section 1 (working rules) and this block.
2. Read section 9 (curriculum) and section 11 (progress).
3. Run the TOPIC GATE (rule 3) on the next topic, and resolve the pending decisions below.

**PENDING DECISIONS — ask the user before teaching anything.** These were raised but never answered:

| # | Decision | Options | Reference |
|---|---|---|---|
| P1 | Adopt the **walking-skeleton reorder**? | (a) yes — thin end-to-end slice + a minimal early deploy, then deepen; (b) no — keep the current day order | section 3.3, items 1–2 |
| P2 | **Which topic starts** the next session? | Day 1.1 repo skeleton · Day 2 domain+Postgres · Day 10 threading · Day 21 Spring Batch · Day 22 GraphQL — user has not chosen | section 9 |
| P3 | Is **Day 23 = JVM & core-Java internals**? | yes / no | section 9, Phase EXTRA |
| P4 | Is **Day 24 = Kubernetes essentials for a Java dev**? | yes / no | section 9, Phase EXTRA |
| P5 | Adopt any **Tier 2** additions? | read replica · load shedding · feature flags · saga orchestration — individually | section 3.2 |
| P6 | **How deep on OIDC?** OAuth2/OIDC + JWT are *already* covered (Days 4, 5, 7.2, 7.6, 18.2, 22.7). The genuine gap is OIDC as an **external IdP**: OIDC discovery (`.well-known`), `id_token` vs `access_token`, nonce/state, token introspection (RFC 7662), and the **build-vs-buy "who is the authorization server"** decision (own it vs Keycloak vs Auth0/Cognito) | yes / no | section 9, Days 5–6 |

**RESOLVED at the start of Day 1 (2026-10-28):** **P1** = keep the current day order (no walking-skeleton reorder) · **P2** = start at **Day 1.1** (repo skeleton) · **P6** = defer — Days 4–7 stay as written, no external IdP for now. · **P3/P4/P5** remain open (nothing blocked by them before Day 17).

**Also still open:** the five questions in section 12 (git remote, AWS account + budget, TMDB/IGDB keys, Datadog availability, realistic session length).

**Next concrete action once P1 and P2 are answered:** run the topic gate, then create the remaining Day 1.1 artefacts **directly in the repo**, one step at a time with an explanation each time (rules 4 + 5) — root `pom.xml` (aggregator + parent), `.mvn/wrapper` + `mvnw`/`mvnw.cmd`, `.editorconfig`, `README.md`, `ci.yml`. (`.gitignore` already exists and is fixed — rev 12.)

**Standing constraints (do not violate):** write code directly, but **one step per turn and explain it** (rules 4 + 5) · topic gate before every topic (rule 3) · never change git history unasked (rule 10) · keep scope small (rule 8) · always give tradeoff + failure mode + 10x answer (rule 12) · never re-bundle threading / third-party APIs / circuit breakers (section 1.1).

---

## 1. INSTRUCTIONS TO THE AGENT — read this first when resuming

1. Read **this file** and **`resources/docs/projectDetails.md`** before doing anything. They are the source of truth for scope. (Both live in `resources/docs/`.)
2. **Never dump the whole architecture at once.** One topic per session, incremental, small steps.
3. **TOPIC GATE (hard rule):** before starting any topic listed in section 9, ask the user:
   *"Do you want to learn `<topic>`, or do you already know it and want to skip to `<next>`?"*
   Wait for the answer. Then tick the checkbox in section 9. The user already knows some of this — never re-teach basics unasked.
4. **The agent writes code directly into the repo** — granted by the user on Day 0 (rev 12). Create files at their absolute paths, one step at a time, then explain them. Still show the meaningful parts in chat (a config file, a schema, a method worth discussing) and always state the path. Never bundle several unrelated files into one turn.
5. **Incremental teaching — DO NOT GO TOO FAST (user's explicit instruction).** One concept, or one class/file, per turn. Always explain *before* writing: what it is, why it exists, and what breaks without it. Then write it, then walk through the important lines. **Stop and wait** for the user to confirm understanding before the next step. If the user says "continue", advance exactly one step — not a whole phase. No walls of text, no five-file dumps, no unexplained code.
6. **After writing or changing code:** verify it — read the file back, then run the build (`mvnw -q -pl <module> test`, or `mvnw -q compile` while a module has no tests yet). Fix compile errors before teaching the next topic. Validation is the agent's job, not the user's; **never claim something works until it has actually built or run**.
7. **Production-first (workspace rule 15):** always give the pattern a real high-load service would use and say *why*. Explicitly call out when a simpler shortcut is **not** production-safe.
8. **Keep the project SMALL.** The user is time-constrained. 5 thin services. No extra features unless asked. Every module must be justified by a concept we are learning.
9. **Docs last** (workspace rule 14): core code + verification first, `.md` files last. No code snippets inside explanation `.md` files — class/method names only.
10. **Never touch git history** (no commit/amend/reset/push) unless the user asks in that turn. Show `git status` instead.
11. **Update section 9 checkboxes and section 11 (Progress) at the end of every session**, and bump "Last updated". This file is the resume point — keep it accurate.
12. **Interview framing (mandatory for every topic):** always include
    (a) the **tradeoff table** (option A vs B, when to pick which),
    (b) the **failure mode** (what breaks, how it degrades),
    (c) the **"what happens at 10x / 100x load"** answer.
    That is what a system-design interview actually grades.
13. **Topic sessions must be < ~45 min of reading.** If a topic needs more, split it and say so.

---

### 1.1 `need.txt` — canonical interpretation (do NOT re-bundle these topics)

The original line reads *"threading access to 3rd party APIs and circuit breakers"*. The user has confirmed this was **punctuation shorthand, not one concept**. It is **four separate topics**:

1. **Threading / concurrency** — used everywhere it is appropriate (request threads, executors, schedulers, listener concurrency, fan-out pools), *not* only around third-party calls. See section 4.3.
2. **Access to third-party APIs** — client design, timeouts, auth, pagination, error taxonomy, rate budgets. Day 10B.
3. **Circuit breakers** — Resilience4j, plus bulkhead/time limiter/fallback. Day 10C.
4. *(separately in the same list)* unit testing, git/CI-CD, AWS, Terraform, Datadog, Postgres+Hibernate, Redis, Spring Security, OAuth2+JWT, SQS/Kafka/SNS, rate limiting.

**Rule for the agent:** teach 1, 2 and 3 as **independent topics with their own gates**. Never present "threads for third-party calls" as the whole threading topic.

---

## 2. What we are building (one paragraph)

EXR aggregates release dates and live local events from third-party sources (TMDB for movies/TV, IGDB for games, a seeded provider for anime + local events). Events are ingested, normalised, cached and stored in our own DB within the retention window each source licence allows. Guests browse; registered free users may subscribe to at most 2 events; paid users get unlimited; admins manage events and users (except developers); developers have unrestricted access. When a subscribed event is created, updated or approaching release, the user is notified via notification channels and calendar reminders. A mock payment gateway upgrades a free user to paid.

---

## 3. Target architecture (v1, deliberately small)

```
                    +--------------+
  React SPA ------->| api-gateway  |  routing / CORS / JWT validation / rate limit / correlation id
  (Vite + TS)       +------+-------+
                           |  (internal HTTP; service tokens later)
     +---------------------+----------------------+--------------------+
     v                     v                      v                    v
+----------+        +--------------+      +---------------+    +--------------+
| identity |<------>|   catalog    |      | notification  |    |   payment    |
|  users   |  HTTP  | events       |      | reminders     |    | orders       |
| auth/JWT |        | subscriptions|      | email / ICS   |    | webhook      |
| tiers    |        | search/cache |      | channels      |    | idempotency  |
+----+-----+        +------+-------+      +-------+-------+    +------+-------+
     |                     |                      ^                   |
     | Postgres            | Postgres             | SQS               | Postgres
     |                     | Redis (cache)        |
     |                     | 3rd-party APIs (TMDB/IGDB, virtual threads + Resilience4j)
     |                     v                      |
     |             +---------------+              |
     +------------>|  Kafka (bus)  |--------------+
                   | domain events |  outbox -> topic -> consumers
                   +---------------+
```

**Why these 5 and no more:** each is a *different scaling profile / failure domain* — the only honest justification for splitting a service.
- `catalog`: read-heavy + third-party-bound → read replicas + cache + async ingestion.
- `identity`: write-light, security-critical, strongly consistent → small, protected, nothing cached in front of tokens.
- `notification`: queue-driven, bursty, external-IO-bound → scale on queue depth, never on CPU.
- `payment`: money → isolated blast radius, strong consistency, idempotent, audited.
- `gateway`: single north-south entry point → edge concerns only, zero business logic.

A 6th, **thin** `graphql-bff` is added in section 3.4 (GraphQL for multiple client types). It is optional: drop it and you are back to 5 services, at the cost of losing the GraphQL topic.

### 3.1 Architecture style: microservices from a monorepo — and how we avoid a *distributed monolith*

**Four independent axes.** Confusing them is the source of the question "is this a monolith or microservices?":

| Axis | EXR's choice | Important note |
|---|---|---|
| Source/repo topology | **Monorepo** (one Maven aggregator) | Says *nothing* about deployment. A monorepo is not a monolith. |
| Deployment topology | **5 independently deployable Spring Boot processes** (containers / ECS tasks) | This is what makes it microservices. |
| Data ownership | **One logical database per service**; no cross-service joins, no shared entities, no shared Flyway history | The strongest and least reversible boundary. |
| Communication | Sync REST for queries; **Kafka/SQS for state changes**; no distributed transactions (outbox + saga instead) | Sync-only = distributed monolith. |

**Verdict: microservices.** Monorepo is a *repository* decision (D-001), orthogonal to architecture.

**Honest production verdict — and this matters for the interview.** At EXR's real scale (a few thousand users, one team), a **modular monolith** would be the *correct* production choice. We are splitting into services because the learning objective is distributed-systems design. Say that out loud in an interview; "we split because microservices are modern" is a red flag, "we split because the ingestion workload has a different scaling and failure profile from the read path" is an answer.

| | Modular monolith | Microservices |
|---|---|---|
| Wins when | small team, one deploy cadence, low ops budget, domain still being discovered | different scaling profiles, independent release cadence, separate failure domains, large orgs with team-per-service |
| Costs | one process = one blast radius; scaling is all-or-nothing | network latency + partial failure, eventual consistency, distributed debugging, ops overhead |
| EXR relevance | would be the right *product* choice | the right *learning* choice; also genuinely justified for ingestion (Day 10) vs read path |

**Distributed-monolith anti-patterns we must actively avoid (guardrail checklist):**
1. A **shared database** or any cross-service join → each service owns its tables, always.
2. **Domain entities or business logic in `libs/exr-common`** → it holds only web/observability primitives (error model, correlation ID, DTO records). If it ever imports JPA, the boundary has failed.
3. **Synchronous call chains longer than one hop** in a user request (`gateway → A → B → C`) → multiplies latency and failure probability. Use a query, a cache, or an event.
4. **Lockstep deployments** — if any change forces deploying two services at once, the boundary is wrong.
5. **Chatty calls** (N+1 over the network) → batch or denormalise.
6. **Shared mutable state** via a "common" table → that is a hidden monolith.

**Escape hatch (optional track, decide on Day 1):** if time runs short, Days 2–9 could build `catalog` + `identity` as **modules inside one Boot application**, then extract them on Days 11+ once Kafka/outbox exist. That is literally how real teams do it (strangler-fig extraction), and it is a *better* interview story than starting distributed. Cost: rework. Benefit: working product sooner, and you experience the extraction decision. **Default remains 5 services unless the user says otherwise.**

**Explicitly deferred (and why):** API composition/BFF, service mesh, CQRS/event-sourcing, multi-region, sharding, GraphQL. Each is revisited on Day 20 as "when would I add this".

### 3.2 Hardening backlog — gaps interviewers probe that v1 did not cover

These are not extra features. They are **decisions the original plan left implicit**, which medium/large-company interviews reliably ask about. All are cheap: they attach to existing days.

| # | Gap | Why it gets asked | Added to | Status |
|---|---|---|---|---|
| H1 | **East-west security** — how does `catalog` trust a call claiming to come from `identity`? | Edge JWT validation does not secure internal calls; large orgs run zero-trust internals | Day 7 (7.6) | planned |
| H2 | **Service discovery** — how do services find each other without hardcoded hosts? | ECS Cloud Map vs EKS CoreDNS vs injected URLs | Day 7 (7.7) | planned |
| H3 | **Postgres full-text search** — `tsvector` + GIN vs `LIKE` vs OpenSearch | "how do you search a million titles?" is a standard probe | Day 2 (2.6) | planned |
| H4 | **Hot partition on release day** — Kafka partition-key choice | keying by `eventId` gives ordering but creates a hot partition when 1M users watch one release | Day 11 (11.6) | planned |
| H5 | **Outbox table growth** — purge/archive strategy | an unbounded outbox is the classic "we forgot" production bug | Day 11 (11.7) | planned |
| H6 | **GDPR distributed delete** across 4 services | right-to-be-forgotten in a distributed system needs an event + tombstones, not a `DELETE` | Day 13 (13.6) | planned |
| H7 | **Liveness vs readiness** + graceful shutdown draining | "why was my pod killed mid-request?" | Day 16 (16.6) | planned |
| H8 | **Trace sampling strategy** and cost control | head vs tail sampling; the Datadog bill at volume | Day 16 (16.7) | planned |
| H9 | **RPO/RTO + an actual restore drill** | a backup you never restored is not a backup | Day 17 (17.6) | planned |
| H10 | **Who runs Flyway** — app startup vs a separate job; expand/contract | 10 instances starting at once; lock contention; rollback safety | Day 18 (18.6) | planned |
| H11 | **In-flight draining during deploy** — Kafka rebalance, SQS visibility extension | "how do you deploy without dropping work?" | Day 18 (18.7) | planned |
| H12 | **Rate-limit algorithms** — token bucket vs sliding window vs leaky bucket + fairness | "how do you stop one tenant starving the rest?" | Day 7 (7.4, extended) | planned |

**Tier 2 — optional additions (pending the user's yes/no):** read replica + replication-lag handling (Day 17) · load shedding, priority queues and `Retry-After` under overload (Day 10C) · runtime feature flags as a real component (Day 18) · saga *orchestration* (state machine) instead of pure choreography (Day 14).

**Tier 3 — deliberately NOT adding**, but be ready to answer "would you add X?": multi-region active-active, service mesh, CQRS/event-sourcing, multi-tenancy, a hand-rolled API gateway, Kubernetes operators, Apollo-style schema federation (only *mentioned* on Day 22.10).


### 3.3 Honest assessment & risk register (agent's view — rev 7)

**Where this project is strong**
- Java 21 features land in *real* contexts (records, sealed types, pattern matching, virtual threads), not exercises.
- Concurrency **correctness** — quota races, idempotent consumers, duplicate webhooks, optimistic locking — the thing that separates mid from senior, and that tutorials skip.
- The deployment path is realistic and complete: image → ECR → ECS/EKS → ALB → RDS/ElastiCache, Terraform, GitHub Actions with OIDC, plus migrations/blue-green/rollback/draining.
- The *combination* of technologies is the real value: you cannot learn how the outbox bridges Postgres and Kafka, or how Redis serves caching + rate limiting + idempotency, from single-topic tutorials.

**Where it is weak (accept these consciously)**
| Risk | Severity | Mitigation |
|---|---|---|
| **Java/JVM fundamentals underweighted** — collections internals, generics/erasure, JMM, GC/heap, classloading, JFR, heap dumps, profiling. Interview rounds test these heavily. | **high** | Add a Phase EXTRA day (proposed Day 22) for JVM + core-Java internals |
| **No DSA / algorithm practice.** This project will not make you pass a coding round. | **high** | Keep a separate, parallel DSA track — do not treat this project as a substitute |
| **AWS cost** — NAT (~$32/mo) + ALB (~$20) + EKS control plane (~$73) + RDS Multi-AZ + ElastiCache realistically **$150–350/month** if left running. | **high** | LocalStack-first; `terraform destroy` discipline; AWS Budgets alarm; treat real AWS as a 2–3 day exercise, not a standing environment |
| **Breadth over depth** — 5 services × 12 technologies risks "familiar with everything, deep in nothing". | medium | Deliberately master **3**: concurrency correctness, caching, event-driven/outbox. Be honest that the rest is "used it, know the tradeoffs" |
| **Thin on Kubernetes internals** — matters if the target runs EKS. | medium | Optional Phase EXTRA day (proposed Day 23): k8s essentials for a Java dev |
| **No real incident experience** — dashboards built, but no 3am debugging. | medium | Do the Day 20.3 failure drills *for real*: kill Redis, throttle TMDB, pause the consumer |
| **LocalStack and Datadog are simulations** — IAM pain, SQS throughput/visibility limits, and Datadog's 14-day trial are not felt locally. | low | Use Micrometer/Prometheus locally, document Datadog wiring, and state the difference honestly in interviews |

**The risk that actually matters: not finishing.** Twenty-plus sessions, with the classic stall at **Days 4–7** (Spring Security / OAuth2 / JWT) because it is fiddly and unrewarding. Worse, **cloud + CI/CD + observability sit at Days 16–18**, so running out of time leaves a locally-working app with *no deployment story* — the part interviews weight most.

**Proposed changes (pending the user's decision)**
1. **Walking-skeleton milestone first:** a thin end-to-end vertical slice — gateway + identity + catalog + one subscribe flow + Docker Compose + CI — before deepening any topic. Guarantees a demoable, deployable system early.
2. **Pull a minimal deploy forward:** one ECS Fargate service + Terraform + GitHub Actions deploy by roughly session 5, then iterate. Deployment becomes a background habit instead of a cliff at Day 17.
3. **Phase EXTRA Day 22:** JVM & core-Java internals (collections, generics, JMM, GC, JFR, heap dumps).
4. **Phase EXTRA Day 23:** Kubernetes essentials for a Java developer (only if targeting EKS shops).
5. **Separate DSA track** — explicitly out of scope here, practised elsewhere.
6. **Pick three deep topics**, and be candid about the rest.
7. **Break things for real** — failure drills are exercises, not reading.

**What this project is NOT:** a coding-interview trainer, a Java-language deep dive, or a Kubernetes course.

### 3.4 GraphQL at the edge (BFF) — coexisting with REST

**Decision:** REST remains the **internal (east-west) contract** between services *and* the external contract for simple, cacheable, high-volume reads. GraphQL is added as the **client-facing read API** in a thin `graphql-bff` service.

```
web SPA / mobile / admin
        |
   api-gateway            (edge: rate limit, JWT validation, correlation id)
        |
        +-- /api/**   --> REST   --> catalog / identity / notification   (simple + cacheable reads, all writes)
        |
        +-- /graphql  --> graphql-bff  (Spring for GraphQL)
                              |  east-west REST calls, user JWT forwarded
                              +--> identity, catalog, notification
```

**Why GraphQL earns its place here:** the SPA, a future mobile app and the admin panel need *different shapes* of `Event + Subscription + User`. Without it you maintain N bespoke REST endpoints per client — over-fetching on mobile, waterfall calls on web. That is the real justification; "we have multiple clients" alone is not.

**Why not GraphQL everywhere:** service-to-service calls have known shapes, low variety, and benefit from HTTP caching, simple contracts and generated clients. GraphQL's flexibility is a cost there, not a benefit.

**Why the BFF is a separate service:** the BFF is a 6th (thin) deployable with a distinct scaling profile (read fan-out, client-specific) and it keeps client shapes out of the domain services. **Rejected alternative:** embedding GraphQL inside `catalog-service` — its resolvers would reach across domains synchronously, which is the distributed-monolith smell from section 3.1. **Tradeoff:** if you want to hold the service count at 5, the honest alternative is to skip GraphQL entirely.

**Costs that must be designed for, not discovered:**
| Cost | Mitigation (taught on Day 22) |
|---|---|
| **N+1 resolution** — every naive GraphQL API is an N+1 machine | `@BatchMapping` / DataLoader, batch by key |
| **No HTTP caching** — GraphQL is a POST | persisted queries (GET + hash), CDN caching by hash, response caching |
| **Query-cost DoS** — one deep query can cost thousands of calls | depth + complexity limits, persisted-query allow-list, disable introspection in prod |
| **Authorization is per field**, and one query can span domains | field-level `@PreAuthorize`, forwarded user JWT (ties to 7.6) |
| **Rate limiting by request count becomes wrong** | rate limit by **query cost** — this changes the Day 7 gateway design |
| **Schema governance / breaking changes** | additive changes, `@deprecated`, breaking-change detection in CI |
| **Observability** | mandatory operation names, per-resolver traces in Datadog |

---

## 4. Tech stack (locked decisions — change only via section 10)


| Concern | Choice | Version target |
|---|---|---|
| Language | Java 21 (LTS): records, sealed types, pattern matching, virtual threads | 21 |
| Framework | Spring Boot 3.x / Spring Framework 6 | Boot 3.3+ |
| Build | Maven multi-module + Maven Wrapper | Maven 3.9 |
| Edge | Spring Cloud Gateway (WebFlux) | Spring Cloud 2023.x |
| Security | Spring Security 6, `spring-security-oauth2-resource-server` (Nimbus JOSE), OAuth2 Client for social login | 6.x |
| DB | PostgreSQL 16, Flyway migrations, Hibernate 6 / JPA | PG 16 |
| Cache | Redis 7 + Spring Data Redis (Lettuce) | Redis 7 |
| Messaging | Kafka (domain events + outbox) and AWS SQS/SNS (fan-out, delayed delivery) | Kafka 3.x |
| Resilience | Resilience4j: circuit breaker, retry, bulkhead, rate limiter, time limiter | 2.x |
| Batch processing | Spring Batch 5 (chunk-oriented, restartable jobs) — see Phase EXTRA | 5.x |
| Client API (GraphQL) | Spring for GraphQL in a thin `graphql-bff` service, coexisting with REST — see Phase EXTRA | 1.x |
| Cloud | AWS: ECS Fargate (default) + EKS (Terraform variant), ALB, RDS, ElastiCache, S3, SQS/SNS, Secrets Manager, ECR | — |
| IaC | Terraform, module per concern, remote state in S3 + DynamoDB lock | 1.7+ |
| Observability | Datadog: APM traces, logs, custom metrics, dashboards, monitors, SLOs (`dd-java-agent`) | — |
| CI/CD | GitHub Actions → OIDC → ECR → ECS/EKS, Flyway gate, blue/green + rollback | — |
| Testing | JUnit 5, Mockito, Spring Boot Test, Testcontainers, WireMock, Awaitility, ArchUnit; k6/Gatling for load | — |
| API docs | springdoc-openapi (OpenAPI 3) + generated TS client for React | — |
| UI | React 18 + TypeScript + Vite, TanStack Query, minimal styling | — |
| Local infra | Docker Compose (Postgres, Redis, Kafka) + LocalStack (SQS/SNS/S3/SES) | — |

**Deliberate non-choices:** no Lombok (records + explicit code), no MapStruct (hand-written mappers, small surface), no shared entity/common-domain library (services own their data; sharing entities = coupling).

### 4.1 Technology coverage map — every item from `need.txt`, and where it is taught

| Technology (from need.txt) | Taught on | Concrete artefact in this project |
|---|---|---|
| **Java 21** language features | Day 1–3, 10, 11 | records for DTOs + events, sealed interfaces for the event hierarchy, pattern matching in mappers, virtual threads in ingestion, `SequencedCollection` |
| **System design / architecture** | Day 0, 1, 2, 20 | service boundaries, context + container diagrams, capacity estimation, failure drills, tradeoff tables |
| **Microservices + scaling** | Day 2, 7, 16, 17 | 5 services, statelessness, horizontal autoscaling, read replicas, per-service DB ownership |
| **Unit testing** | Day 2, 3, 15 | JUnit 5 + Mockito + AssertJ on domain logic |
| **Integration / slice testing** | Day 2, 3, 15 | Testcontainers (Postgres, Redis, Kafka), `@WebMvcTest`, WireMock, Awaitility |
| **Git** | Day 1 | trunk-based flow, Conventional Commits, feature branches, PR + required checks |
| **CI/CD + GitHub Actions** | Day 1, 18 | `ci.yml` (build/test/scan), `cd.yml` (OIDC → ECR → deploy), blue/green + rollback |
| **AWS** | Day 12, 16, 17 | ECR, ECS Fargate, EKS, ALB + ACM, RDS, ElastiCache, S3, SQS, SNS, SES, Secrets Manager, IAM, CloudWatch |
| **API Gateway** | Day 7, 17 | Spring Cloud Gateway at the edge; AWS API Gateway vs ALB vs Spring Cloud Gateway compared and chosen on Day 17 |
| **Terraform** | Day 17 | modules per concern, remote state in S3 + DynamoDB lock, dev/prod envs, plan/apply, drift |
| **Datadog (monitoring)** | Day 16 | `dd-java-agent`, APM traces across gateway→service→DB→Kafka, log correlation, custom metrics, dashboards, monitors, SLOs |
| **PostgreSQL** | Day 2 | schema design, index strategy, isolation levels, `EXPLAIN`, zero-downtime migrations, PITR |
| **Hibernate / JPA** | Day 2 | entity mapping, `@Version` optimistic locking, `@EntityGraph` against N+1, `SELECT ... FOR UPDATE`, transactions |
| **Redis caching** | Day 9 | cache-aside, key design, TTL + jitter, stampede protection, invalidation; Redis also backs rate limiting, token denylist and idempotency keys |
| **Spring Security** | Day 4, 7 | `SecurityFilterChain`, BCrypt/Argon2, stateless config, method security, role hierarchy |
| **OAuth2 + JWT** | Day 5, 6, 10 | resource server + JWKS, access/refresh with rotation, social login (auth-code + PKCE), IGDB client-credentials flow |
| **Kafka** | Day 11 | topics/partitions/consumer groups, transactional outbox, idempotent consumer, DLQ, schema versioning |
| **SQS** | Day 12, 13 | visibility timeout, long polling, DLQ + redrive, delayed messages for reminders, LocalStack |
| **SNS** | Day 12 | fan-out to multiple queues, subscription filter policies |
| **Threading / concurrency** | Day 8, 10, 11–13 | platform vs virtual threads, `ExecutorService`/`ThreadPoolExecutor` tuning, `CompletableFuture`, locks/atomics/`volatile`, thread safety, MDC + `ThreadLocal`, structured concurrency, deadlocks and thread dumps, race conditions (quota, double-spend) |
| **Third-party API access** | Day 10B | TMDB + IGDB clients, connection/read timeouts, retries with jitter, pagination, per-source rate budgets, error taxonomy, partial-failure handling, WireMock |
| **Circuit breakers / resilience** | Day 7, 10C | Resilience4j circuit breaker, retry, bulkhead, time limiter, rate limiter, fallbacks/degradation, correct composition order, chaos testing |
| **Rate limiting** | Day 7, 9, 10 | gateway Redis `RequestRateLimiter` (per-IP/per-user/per-route), per-source outbound budgets, 429 semantics |
| **Docker / containers** | Day 16 | multi-stage Dockerfiles, layered jars, non-root, JVM container flags, Docker Compose dev stack |
| **Spring Batch** *(Phase EXTRA)* | Day 21 | chunk-oriented reader/processor/writer, restartability, skip/retry, partitioning, distributed job safety |
| **GraphQL** *(Phase EXTRA)* | Day 22 | schema-first SDL, `@BatchMapping`/DataLoader N+1 fix, persisted queries + caching, query cost limits, field-level authz, coexisting with REST |
| **Observability (supporting)** | Day 16 | Micrometer metrics, health checks, structured logging, correlation IDs |

### 4.2 Not covered in depth (say so up front, so there are no surprises)

| Topic | Why it is out of scope | Where it is at least *mentioned* |
|---|---|---|
| Kubernetes internals (operators, CRDs, service mesh) | EKS is used as a deployment target only; deep k8s is a separate curriculum | Day 17 |
| gRPC / GraphQL | REST + events is enough to teach the architecture concepts | Day 20 (when to add) |
| Prometheus/Grafana | Datadog is the chosen tool from `need.txt` | Day 16 (equivalents noted) |
| Serverless (Lambda, API Gateway HTTP APIs, DynamoDB) | Different architectural style; would double the surface | Day 17 (comparison only) |
| Real Stripe integration | Mock gateway teaches the same concepts (webhooks, idempotency, saga) | Day 14 |
| Native mobile, SSR/Next.js | UI is explicitly low priority | Day 19 |


### 4.3 Where threads actually appear in EXR (the threading map)

Two different things are usually confused: **parallelism** (doing more work at once) and **concurrency correctness** (not corrupting state when work overlaps). EXR needs both, in different places.

**A. Parallelism — where we deliberately use threads**

| # | Where | Thread model | Why this model | Failure mode if done wrong |
|---|---|---|---|---|
| 1 | Web request handling (all services) | Tomcat thread-per-request, platform threads, pool ≈ 200 | Spring MVC is blocking and simple | Pool exhaustion: one slow downstream blocks every thread → whole service 503s. *This is why no third-party call is allowed on the read path.* |
| 2 | Ingestion fan-out to TMDB/IGDB | **Java 21 virtual threads**, one per outbound call, bounded by a bulkhead + rate limiter | Cheap blocking IO without reactive complexity | Unbounded creation → 429 storm, OOM, or hammering the source. Must be bounded. |
| 3 | Scheduled jobs (ingestion, purge, reconciliation, warm-up) | `@Scheduled` pool, size 1–2 per instance | Periodic, low volume | Two instances run the same job → duplicate work; needs a distributed lock (ShedLock / DB advisory lock) |
| 4 | Kafka consumers (notification) | N listener threads, **concurrency ≤ partition count** | Parallel consumption per partition | Concurrency > partitions = idle threads; blocking inside the listener → rebalance storm and lag |
| 5 | SQS consumers (notification) | `maxConcurrentMessages` + long polling | Absorbs bursty reminder traffic | Processing longer than the visibility timeout → duplicate delivery (so the consumer must be idempotent) |
| 6 | Outbox relay → Kafka | single dedicated poller thread, `FOR UPDATE SKIP LOCKED` | Low volume, must preserve order | Two relay instances publishing the same row without a claim → duplicate events |
| 7 | Notification fan-out (email + `.ics` rendering) | bounded pool, **bulkhead per channel** | One slow SMTP must not starve the in-app channel | Thread starvation, cascading timeouts, DLQ flooding |
| 8 | Async work inside a request (payment) | preferred: **none** — write the outbox row in the transaction and return `202 Accepted` | Request latency stays low and consistent | Fire-and-forget task dies with the pod → work silently lost |

**B. Concurrency correctness — the bugs that are actually asked about in interviews**
- **Quota race:** two parallel "subscribe" requests both read `count = 1` and both insert → a free user ends up with 3 subscriptions. Fix: atomic DB constraint / `SELECT ... FOR UPDATE` / Redis counter — one of them, chosen and justified on Day 8.
- **Duplicate subscribe / duplicate webhook:** needs idempotency keys + unique constraints, not just a lock.
- **Lost update on event sync:** two ingestion runs updating the same event → `@Version` optimistic locking + retry.
- **Double reminder:** the same event notified twice → dedupe key in the notification log.

**C. Thread-safety rules we will hold to**
- No mutable state in Spring singletons; all shared beans effectively immutable.
- `ThreadLocal` (and MDC correlation IDs, `SecurityContext`) needs care with virtual threads — the same carrier thread is reused, and inheritable locals do not propagate the way they do on platform threads.
- Never pool virtual threads; they are not a scarce resource. Do bound them with a bulkhead/rate limiter instead.
- Avoid `synchronized` around blocking IO on virtual threads (pinning on Java 21; fixed only later) — use `ReentrantLock`.
- Every executor gets a **bounded queue and an explicit rejection policy** — that is our backpressure. `newCachedThreadPool()` is banned.
- Bulkheads are thread-pool/semaphore limits: Resilience4j bulkhead is how we cap concurrency per dependency.

**D. What threads will NOT be used for**
- Scaling the read path. More threads do not fix a saturated DB or CPU — that is horizontal scaling, read replicas and caching (Days 9, 17).
- Replacing a queue. Threads live inside one process and die with the pod; durability needs Kafka/SQS.

*Covered in detail on **Day 8** (correctness), **Day 10** (threads), **Day 10B** (third-party clients) and **Day 10C** (circuit breakers), then applied on Days 11–14.*

### 4.4 Complete tool & technology inventory (grouped — the single reference list)

| Layer | Tools / technologies |
|---|---|
| **Language & runtime** | Java 21 (LTS), JVM, records, sealed types, pattern matching, virtual threads, structured concurrency |
| **Build** | Maven 3.9 multi-module, Maven Wrapper, `dependencyManagement`, Surefire + Failsafe |
| **Frameworks** | Spring Boot 3.x, Spring Framework 6, Spring Cloud Gateway, Spring Data JPA, Spring Data Redis, Spring Security 6, Spring for GraphQL, Spring Batch 5, springdoc-openapi, Micrometer |
| **API & contracts** | REST + JSON, RFC 7807 `ProblemDetail`, OpenAPI 3, GraphQL SDL, iCalendar (`.ics`) |
| **Security** | Spring Security, OAuth2 / OIDC, JWT (Nimbus JOSE, JWKS), BCrypt / Argon2, PKCE, refresh-token rotation, token revocation |
| **Data (OLTP)** | PostgreSQL 16, Hibernate 6 / JPA, Flyway migrations, HikariCP, `tsvector` + GIN full-text search, optimistic locking |
| **Cache & fast state** | Redis 7, Lettuce, cache-aside, TTL + jitter, rate-limit counters, token denylist, idempotency keys |
| **Messaging** | Apache Kafka + Spring for Apache Kafka, AWS SQS, AWS SNS, transactional outbox, dead-letter queues |
| **Resilience** | Resilience4j (circuit breaker, retry, bulkhead, rate limiter, time limiter), HTTP timeouts, backpressure |
| **Testing** | JUnit 5, Mockito, AssertJ, Spring Boot Test, Testcontainers (Postgres / Redis / Kafka), WireMock, Awaitility, ArchUnit, `@WebMvcTest`, `@GraphQlTest`, k6 or Gatling (load), PIT (optional) |
| **Local infrastructure** | Docker, Docker Compose, LocalStack (SQS / SNS / S3 / SES), MailHog (email) |
| **Cloud (AWS)** | ECR, ECS Fargate (default), EKS (variant), ALB + ACM, RDS Postgres, ElastiCache Redis, S3, SQS, SNS, SES, Secrets Manager, IAM, CloudWatch, AWS Budgets |
| **Infrastructure as code** | Terraform 1.7+, modules per concern, S3 + DynamoDB remote state, `envs/{dev,prod}` |
| **CI/CD** | GitHub Actions, OIDC federation to AWS (no static keys), dependency + secret scanning, Trivy image scan, SBOM |
| **Observability** | Datadog APM / logs / metrics / dashboards / monitors / SLOs, `dd-java-agent`, Micrometer, structured JSON logging, correlation IDs, JFR + heap dumps (diagnostics) |
| **Frontend** | React 18, TypeScript, Vite, TanStack Query, TypeScript client generated from OpenAPI |
| **Developer tooling** | Git + Conventional Commits, IntelliJ IDEA / VS Code, Postman or `.http` files, Maven Wrapper |

---

## 5. Repository layout (target)


```
exr/
  pom.xml                     aggregator + parent (dependencyManagement, plugin config)
  mvnw / mvnw.cmd / .mvn/     Maven wrapper
  .gitignore  .editorconfig  README.md
  docs/
    plan.md                   THIS FILE - agent manual + curriculum + progress
    projectDetails.md         feature spec / product requirements
    decisions.md              ADR log (created Phase 1)
    interview-notes.md        per-topic tradeoff + failure-mode cheat sheet
  libs/
    exr-common/               error model (RFC 7807), correlation-id filter, shared DTO records
                              KEEP TINY: web/observability primitives only, NEVER domain entities
  services/
    api-gateway/
    identity-service/
    catalog-service/
    notification-service/
    payment-service/
  ui/web/                     React + Vite + TS SPA
  infra/
    docker/docker-compose.yml + localstack init
    terraform/                modules + envs/{dev,prod}
  .github/workflows/          ci.yml, cd.yml, security.yml
```

**Monorepo rationale:** one PR changes a contract and its consumer atomically; the user is solo, so repo-per-service only adds overhead. The tradeoff (and when to split) is discussed on the CI/CD day.

---

## 6. Non-negotiable engineering conventions

- Package root `com.kodegravity.exr.<service>`, layered `api` (controllers/DTOs) → `domain` (entities, domain services) → `infra` (JPA, Redis, clients, messaging).
- **Never expose JPA entities in controllers.** API DTOs are Java records; mapping is explicit.
- Config via `application.yml` + env vars. **No secrets in the repo** — AWS Secrets Manager in cloud, git-ignored `.env` locally.
- All errors → RFC 7807 `ProblemDetail` with `traceId` + `correlationId`.
- Every inbound request carries `X-Correlation-Id` (generated at the gateway if absent) and it is propagated into Kafka headers, SQS message attributes and outbound HTTP.
- Every mutable table: `id` (UUID), `created_at`, `updated_at`, `version` (optimistic lock).
- Time in UTC (`Instant`), render in user TZ; money as `BigDecimal`.
- Tests: unit tests for domain logic, Testcontainers for repository/service integration, WireMock for third-party; no `@SpringBootTest` for pure logic.
- Commits: Conventional Commits (`feat:` `fix:` `test:` `chore:` `docs:` `infra:`), one logical change per commit.

---

## 7. Definition of done (per topic)

A topic is DONE only when:
1. Code exists in the repo, compiles, and the user pasted it themselves.
2. Tests for the topic run green locally, and are the right *kind* of test.
3. The user can answer, in their own words, the **tradeoff / failure-mode / 10x-scale** questions for the topic.
4. The topic row in section 9 is ticked and section 11 is updated.
5. If a real decision was made → an entry is added to `docs/decisions.md`.

---

## 8. Interview readiness mapping (why each topic exists)

| Interview signal | Practised on |
|---|---|
| Requirements → service boundaries | Day 1–2 |
| Data modelling, indexes, retention | Day 2 |
| Caching strategy, stampede, invalidation | Day 9 |
| AuthN vs AuthZ, token design, RBAC | Day 4–7 |
| Async/event-driven, outbox, idempotency | Day 11–12 |
| Resilience: CB, retry, bulkhead, backpressure | Day 7, 10C |
| Threading & concurrency (design + debugging) | Day 10 |
| Third-party integration & graceful degradation | Day 10B |
| Concurrency correctness (quota, double-spend) | Day 8, 14 |
| Observability: SLI/SLO, tracing, alerting | Day 16 |
| Cloud topology, cost vs complexity | Day 17 |
| Safe delivery: migrations, blue/green, rollback | Day 18 |
| Capacity estimation & failure drills | Day 20 |

---

## 9. Curriculum — phased, with checkboxes

Legend: `[x]` done · `[ ]` not started · `[~]` in progress · `[-]` deliberately skipped by the user.
**A "day" = one study session, not a calendar day.** Before each day's first topic, run the TOPIC GATE (section 1 rule 3).

### Phase 0 — Foundations & repo hygiene
**DAY 1 — Repo skeleton + git + first pipeline**
- [x] 1.1 Monorepo layout, aggregator `pom.xml`, Maven wrapper, `.gitignore`, `.editorconfig`, `README.md` — **done**; directories are created as modules appear (git cannot store empty directories)
- [x] 1.2 Git model: trunk-based vs GitFlow, Conventional Commits, feature branches, PR review, `.gitattributes` — **done**; repo initialised on `main`, LF enforced in the index, first commit `c0985c1`
- [x] 1.3 GitHub Actions smoke pipeline (`ci.yml`: checkout → JDK 21 → `mvnw verify`), branch protection, required checks — **done**; first run on `master` succeeded ([run 42940713385](https://github.com/electronik54/TW_systemArch/actions/runs/42940713385)). Branch protection still to be enabled in the GitHub UI (needs the check name `Build and test (Java 21)`)
- [x] 1.4 `libs/exr-common`: RFC 7807 error model + correlation-id filter — **done**: `CorrelationIdFilter` (sanitised, MDC, `finally` cleanup), `ProblemSupport` + `GlobalExceptionHandler` (RFC 7807, no internal leakage, Spring statuses preserved), `ExrCommonAutoConfiguration` + `AutoConfiguration.imports`. **11 unit tests green.** Remaining: `@WebMvcTest` coverage of the handler belongs to Day 3
- *Exit test:* ✅ **MET** — `./mvnw verify` is green locally **and** in Actions (run 42940713385), and the user can explain why gates run on **both** PR and push, not just one (see section 11.4).

### Phase 1 — Domain, data, first service
**DAY 2 — Domain modelling + PostgreSQL + Hibernate**
- [ ] 2.1 Bounded contexts and ownership: which service owns which table; why no cross-service joins
- [ ] 2.2 ERD: events, sources, subscriptions, users, roles, audit, outbox, notification log (+ indexes and why)
- [ ] 2.3 Flyway migrations (`V1__init.sql`), naming conventions, forward-only migrations, zero-downtime rules
- [ ] 2.4 JPA mapping with Hibernate 6: records vs entities, `@Version`, `@Embedded`, avoid N+1 (`@EntityGraph`)
- [ ] 2.5 Testcontainers integration test for repositories
- [ ] 2.6 Full-text search in Postgres: `tsvector` + GIN index vs `LIKE`/trigram vs OpenSearch — pick and justify *(H3)*
- *Exit test:* user can justify each index and explain why the catalogue is not `JOIN`ed to users.

**DAY 3 — Catalog service REST API**
- [ ] 3.1 Controller/domain/infra layering, DTO records, explicit mappers
- [ ] 3.2 Search & filtering: page vs cursor pagination (decision), sort whitelisting, SQL injection safety
- [ ] 3.3 Bean validation, `ProblemDetail` error contract, `@ControllerAdvice`
- [ ] 3.4 OpenAPI via springdoc + `@WebMvcTest` slice tests
- *Exit test:* API contract in Swagger UI; user can defend the pagination choice.

### Phase 2 — Identity, security, edge
**DAY 4 — Spring Security fundamentals in the identity service**
- [ ] 4.1 Users, credentials, BCrypt/Argon2, account states (enabled/locked/suspended)
- [ ] 4.2 `SecurityFilterChain` for a stateless API, CSRF rationale, CORS vs CSRF
- [ ] 4.3 Registration/login endpoints, password policy, brute-force lockout
- *Exit test:* user can explain stateless vs session auth and why CSRF is disabled.

**DAY 5 — OAuth2 + JWT (access & refresh)**
- [ ] 5.1 JWT anatomy, signing (RSA vs HMAC), claims design, `jti`, `aud`, `iss`, expiry
- [ ] 5.2 Access token (short) + refresh token with **rotation** and reuse detection; token families
- [ ] 5.3 Resource server config, JWKS endpoint, key rotation, `JwtDecoder`
- [ ] 5.4 `@PreAuthorize`, method security, role hierarchy `GUEST < FREE_USER < PAID_USER < ADMIN < DEVELOPER`
- [ ] 5.5 Revocation strategy (denylist in Redis, `tokenVersion`) — and why "JWT logout" is hard
- *Exit test:* user can design token TTLs and explain revocation tradeoffs.

**DAY 6 — OAuth2 social login (optional topic, gate it)**
- [ ] 6.1 Authorization-code flow, PKCE, redirect URIs, why the SPA should not hold client secrets
- [ ] 6.2 Google/GitHub login, account linking by verified email, what to store
- *Exit test:* user can draw the code flow and explain PKCE.

**DAY 7 — API Gateway (edge)**
- [ ] 7.1 Spring Cloud Gateway routes, predicates, filters; north-south vs east-west traffic
- [ ] 7.2 JWT validation at the edge + trust boundary; what the gateway must NOT do
- [ ] 7.3 Correlation-id injection + request logging
- [ ] 7.4 Rate limiting (Redis `RequestRateLimiter`): per-IP, per-user, per-endpoint; 429 semantics, burst vs sustained; **algorithm choice — fixed window vs sliding window vs token bucket vs leaky bucket — and fairness (no single caller starving the rest)** *(H12)*
- [ ] 7.5 Gateway resilience: timeouts, retries (only idempotent), circuit breaker at the edge
- [ ] 7.6 **East-west security**: how `catalog` authenticates a call from `identity` — propagated user JWT vs client-credentials service tokens vs mTLS; trust boundary, audience/`aud` claim per service *(H1)*
- [ ] 7.7 **Service discovery**: ECS Cloud Map vs EKS CoreDNS vs injected env URLs; why hardcoded hosts break blue/green *(H2)*
- *Exit test:* user can explain where rate limiting belongs and why not in each service.

### Phase 3 — Subscriptions, caching, ingestion
**DAY 8 — Subscriptions + entitlements + concurrency**
- [ ] 8.1 Where subscriptions live (open question #2) — decision + consequences
- [ ] 8.2 Quota enforcement that is actually atomic: DB constraint / `SELECT ... FOR UPDATE` / Redis counter (pick one, justify)
- [ ] 8.3 Idempotent subscribe (idempotency key), unique constraint, optimistic locking, retry safety
- [ ] 8.4 Entitlement check: synchronous call vs tier claim in JWT vs cached entitlement (open question #3)
- *Exit test:* user can explain how two parallel requests cannot both take the last free slot.

**DAY 9 — Redis caching**
- [ ] 9.1 Cache-aside vs write-through vs write-behind; what is safe to cache and what is not
- [ ] 9.2 Key design, TTL strategy, source retention vs cache TTL, per-source TTL config
- [ ] 9.3 Stampede / thundering herd: lock, single-flight, jittered TTL, stale-while-revalidate
- [ ] 9.4 Invalidation: TTL-first vs explicit evict; cache versioning; multi-instance consistency
- [ ] 9.5 Metrics: hit ratio, evictions; Redis as rate-limit + idempotency + denylist store
- *Exit test:* user can describe a cache outage degrading to DB but never to 500s.

**DAY 10 — Threading & concurrency in Java 21** *(standalone topic — threads are used across the whole app, not only for third-party calls)*
- [ ] 10.1 Thread lifecycle, platform threads, `Runnable`/`Callable`, why `new Thread()` is never the answer in a service
- [ ] 10.2 `ExecutorService` internals: `ThreadPoolExecutor` core/max/queue/keep-alive, **bounded queue + rejection policy = backpressure**, naming threads for debuggability
- [ ] 10.3 Virtual threads (Java 21): what they solve, carrier threads, pinning, never pool them, `spring.threads.virtual.enabled`, `ThreadLocal`/MDC/`SecurityContext` pitfalls
- [ ] 10.4 Shared-state safety: `synchronized` vs `ReentrantLock` vs atomics vs `volatile`, the Java Memory Model in one page, concurrent collections, immutable-by-default beans
- [ ] 10.5 `CompletableFuture` composition, timeouts, exception handling; **structured concurrency** as the cleaner alternative
- [ ] 10.6 Deadlock/livelock, thread dumps (`jstack`), diagnosing a stuck service; scheduled jobs + distributed locks (ShedLock)
- *Exit test:* user can say, for each of the 8 places in section 4.3, which thread model is used and why.

**DAY 10B — Third-party API integration** *(its own topic: talking to someone else's service is where most outages come from)*
- [ ] 10B.1 Client design: `RestClient`/`WebClient`, connection pool sizing, **connect vs read timeouts**, `User-Agent`, compression
- [ ] 10B.2 Auth to sources: API keys vs OAuth2 client-credentials (IGDB via Twitch), secret handling, token caching
- [ ] 10B.3 Pagination, incremental sync (changed-since), mapping external DTOs → internal model, source-data versioning
- [ ] 10B.4 Error taxonomy: 4xx (don't retry) vs 5xx/429 (retry with jitter, respect `Retry-After`), idempotency of outbound calls
- [ ] 10B.5 Testing with WireMock (slow, down, 429, malformed) + per-source rate budgets
- *Exit test:* user can explain what happens to the catalogue API when TMDB is down or throttling for an hour.

**DAY 10C — Resilience: circuit breakers & friends**
- [ ] 10C.1 Failure modes: fail-fast (refused) vs fail-slow (hanging) vs partial; why timeouts come first
- [ ] 10C.2 Circuit breaker states (closed/open/half-open), sliding window, failure-rate vs slow-call thresholds, minimum calls
- [ ] 10C.3 Bulkhead (semaphore vs thread-pool), time limiter, and **correct composition order**: bulkhead → rate limiter → retry → circuit breaker → time limiter
- [ ] 10C.4 Fallbacks & graceful degradation: stale cache, partial response, feature flag off — never a user-visible 500
- [ ] 10C.5 Observability + chaos: CB state metrics, alerting on open circuits, fault injection tests
- *Exit test:* user can narrate a 429 storm and a hanging upstream, and say what the user sees in each case.

### Phase 4 — Event-driven
**DAY 11 — Kafka + transactional outbox**
- [ ] 11.1 Kafka model: topic, partition, key, offset, consumer group, rebalancing, ordering guarantees
- [ ] 11.2 Why "DB write + publish" is not atomic → transactional outbox + relay/poller (or Debezium)
- [ ] 11.3 Producer config (acks, idempotence, retries), consumer config (manual ack, max.poll, backpressure)
- [ ] 11.4 Idempotent consumer + dedupe table, at-least-once vs exactly-once, poison message → DLQ
- [ ] 11.5 Event schema/versioning strategy (records + versioned topics, backward compatibility)
- [ ] 11.6 **Partition-key choice and hot partitions**: key by `eventId` (ordering per event, but one blockbuster release = one hot partition) vs key by `userId` (spread, no ordering) — decide and justify *(H4)*
- [ ] 11.7 **Outbox lifecycle**: purge/archive published rows, retention window, and why the outbox must not become the system of record *(H5)*
- *Exit test:* user can explain a lost-event and a duplicate-event scenario and the fix for each.

**DAY 12 — SQS + SNS (AWS messaging)**
- [ ] 12.1 SQS standard vs FIFO, visibility timeout, long polling, DLQ + redrive policy
- [ ] 12.2 SNS fan-out to multiple SQS queues; subscription filters
- [ ] 12.3 Delayed messages / scheduled reminders, message attributes, idempotency keys
- [ ] 12.4 Kafka vs SQS/SNS: throughput, ordering, replay, ops cost — when to use which
- [ ] 12.5 LocalStack for local dev
- *Exit test:* user can justify using both Kafka and SQS in the same system.

### Phase 5 — Notification, payment
**DAY 13 — Notification service**
- [ ] 13.1 Reminder scheduling: cron vs delayed message vs scheduler + "due" table (survives restarts)
- [ ] 13.2 Notification preferences, quiet hours, timezone handling, deduplication
- [ ] 13.3 Email via SES/SMTP + templating; in-app notification centre
- [ ] 13.4 Calendar: `.ics` generation, `VEVENT`/`VALARM`, feed URL design, ETag/`If-None-Match`
- [ ] 13.5 Delivery tracking: notification log table, retries, DLQ, provider bounce handling
- [ ] 13.6 **GDPR distributed delete**: a `UserDeleted` event fanning out to catalog/notification/payment, tombstones vs hard delete, anonymised audit trail, and how to prove deletion completed *(H6)*
- *Exit test:* user can explain how a 100k-user release-day burst is absorbed.

**DAY 14 — Mock payment + webhooks + saga**
- [ ] 14.1 Orders, plans, provider abstraction (mock now, Stripe later), price as config
- [ ] 14.2 Webhook security: signature verification, replay window, idempotency key, unique constraint
- [ ] 14.3 Saga / compensating transactions for "paid but upgrade failed"; reconciliation job
- [ ] 14.4 Audit trail and financial-grade logging; why money never goes in a cache
- *Exit test:* user can walk through a duplicate webhook and a crash-between-steps scenario.

### Phase 6 — Quality, cloud, delivery
**DAY 15 — Testing strategy**
- [ ] 15.1 Test pyramid for microservices; what unit vs slice vs integration vs contract vs E2E each buy you
- [ ] 15.2 Testcontainers for Postgres/Redis/Kafka; WireMock for third parties; Awaitility for async
- [ ] 15.3 Contract testing (Spring Cloud Contract or Pact) between gateway/service/UI
- [ ] 15.4 Coverage gates, mutation testing (PIT) as an optional stretch, flaky-test policy
- [ ] 15.5 Load testing with k6/Gatling: read-heavy catalogue, subscribe burst, reminder fan-out
- *Exit test:* user can say which test would have caught each of our previous bugs.

**DAY 16 — Docker, local stack, observability (Datadog)**
- [ ] 16.1 Dockerfiles: multi-stage build, layered jars, non-root user, JVM container flags (`MaxRAMPercentage`)
- [ ] 16.2 Docker Compose dev stack + LocalStack; one-command local environment
- [ ] 16.3 Datadog agent: APM traces (gateway → service → DB → Kafka), log correlation, unified service tagging — **decide `correlationId` vs `traceId`**: unify them (`correlationId` = `traceId` when one exists, else generate) or log both. Remember traces are **sampled**, correlation ids are not, and non-HTTP callers (batch, webhooks, browsers) have no trace to offer
- [ ] 16.4 Custom metrics + RED/USE dashboards, cache hit ratio, CB state, queue depth, consumer lag
- [ ] 16.5 SLI/SLO/error budget, monitors + alert routing, on-call basics, runbooks
- [ ] 16.6 **Liveness vs readiness**: what belongs in each probe (readiness checks dependencies, liveness must not), startup probes, and graceful shutdown draining in-flight requests *(H7)*
- [ ] 16.7 **Trace sampling**: head vs tail sampling, always-keep-errors, sampling rates per endpoint, and controlling ingest cost *(H8)*
- *Exit test:* user can trace one subscribe request end-to-end and point at the right dashboard.

**DAY 17 — Terraform + AWS**
- [ ] 17.1 Terraform fundamentals: state (S3 + DynamoDB lock), modules, workspaces/envs, plan vs apply, drift
- [ ] 17.2 Network: VPC, public/private subnets, NAT, security groups vs NACLs, ALB + TLS (ACM)
- [ ] 17.3 Data: RDS Postgres (Multi-AZ, backups, PITR), ElastiCache Redis (replication, failover), S3, Secrets Manager
- [ ] 17.4 Compute: ECS Fargate (service, task def, autoscaling on CPU/queue) vs EKS (node groups, HPA, IRSA) — pick the default and defend it
- [ ] 17.5 IAM least privilege, GitHub OIDC role, ECR, cost guardrails
- [ ] 17.6 **RPO/RTO definition + a real restore drill**: define targets, restore RDS from a snapshot into a scratch instance, time it, and document the result *(H9)*
- *Exit test:* user can draw the AWS topology and estimate a monthly cost ballpark.

**DAY 18 — CI/CD with GitHub Actions**
- [ ] 18.1 Pipeline design: PR (build+test+scan) vs main (build image) vs release (deploy), caching, matrix builds
- [ ] 18.2 Security: OIDC to AWS (no long-lived keys), secret scanning, dependency scanning, image scanning (Trivy)
- [ ] 18.3 Artifacts & versioning: immutable image tags, SBOM, provenance
- [ ] 18.4 Deploy strategies: rolling vs blue/green vs canary; ECS/EKS specifics; Flyway migration ordering
- [ ] 18.5 Rollback & smoke tests; post-deploy verification; manual approval gates for prod
- [ ] 18.6 **Who runs migrations**: Flyway on app startup vs a separate one-off job/init container; lock contention with N instances; **expand/contract (parallel-change) pattern** for zero-downtime schema changes *(H10)*
- [ ] 18.7 **Draining in-flight work**: `server.shutdown=graceful`, deregister from the load balancer first, Kafka consumer pause/rebalance, SQS visibility extension — deploy without dropping requests or messages *(H11)*
- *Exit test:* user can describe a safe deploy and its rollback in under 2 minutes.

### Phase 7 — UI + consolidation
**DAY 19 — React thin slice (low priority for the user)**
- [ ] 19.1 Vite + TS project, env config, API client generated from OpenAPI
- [ ] 19.2 Auth flow in the SPA: token storage tradeoffs, silent refresh, route guards by role
- [ ] 19.3 Event list + filters, event detail, subscribe/unsubscribe with optimistic UI
- [ ] 19.4 Calendar feed URL + `.ics` download, notification centre, mock upgrade page
- [ ] 19.5 Skip entirely if time is short — UI is explicitly low priority
- *Exit test:* end-to-end guest → register → subscribe → reminder visible.

**DAY 20 — Consolidation & interview drills**
- [ ] 20.1 Full architecture walkthrough on a whiteboard: 5-minute version and 30-minute version
- [ ] 20.2 Capacity estimation drill (users, events, QPS, storage, queue depth)
- [ ] 20.3 Failure drills: Redis down, Kafka lag, TMDB 429 storm, DB failover, AZ loss, webhook storm
- [ ] 20.4 Tradeoff recap + `docs/interview-notes.md` cheat sheet finalised
- [ ] 20.5 STAR stories from this project (a bug found by a test, a decision reversed, a perf win)
- [ ] 20.6 A one-page architecture summary in `README.md` (context, containers, the 3 flows, the tradeoff table) — the thing to read the night before the interview
- [ ] 20.7 Answer the "would you add X?" list from section 3.2 Tier 3 out loud, with a reason for each

---

### Phase EXTRA — elective / bonus topics (open-ended, user-extensible)

> **Purpose:** the user adds extra topics here as we go. Nothing in this phase blocks the core curriculum; it can be taken any time after Phase 6, or whenever the user asks for it.
> **Numbering:** starts at 21 so no existing day shifts. Day 20 (consolidation) remains the final drill day; if this phase grows large, add a second consolidation day (20B) after it.
> **Rule:** every topic added here still gets its own TOPIC GATE before teaching.
>
> **Placement decision (make on Day 21):** Spring Batch jobs are **run-to-completion** processes, not always-on services. Either (a) a `batch` package inside `catalog-service` sharing its datasource, or (b) a separate deployable (`catalog-batch`) built from the same Maven module and deployed as a one-off ECS task / k8s `Job`. Decide and justify on Day 21.2 — the tradeoff is code sharing and DB ownership vs blast radius and independent scaling.

**DAY 21 — Spring Batch (bulk & restartable processing)** *(requested by the user)*
- [ ] 21.1 What Spring Batch actually solves: chunk-oriented `ItemReader` → `ItemProcessor` → `ItemWriter`, `Job`/`Step`, `JobRepository`, `JobLauncher`, `JobParameters`, `JobExecution`/`StepExecution` — and why it is not just a `@Scheduled` loop (Spring Batch 5 on Boot 3: no `@EnableBatchProcessing`, explicit `TransactionManager`, `JobBuilder`/`StepBuilder` API)
- [ ] 21.2 Where EXR needs it: initial backfill of 100k+ titles from TMDB/IGDB, nightly source re-sync/repair, retention purge, payment reconciliation, notification digest generation
- [ ] 21.3 Restartability: `JobInstance` identity by parameters, `ExecutionContext` checkpointing, re-running a failed job — and why the writer must be idempotent (upsert)
- [ ] 21.4 Chunk size vs transaction size vs memory; `faultTolerant()` with skip/retry limits; what a poisoned record does; skip listeners that log the offending record
- [ ] 21.5 Readers: cursor vs paging (`JdbcPagingItemReader`), JPA reader pitfalls, ordering requirements, and why you never stream a whole table into memory
- [ ] 21.6 Scaling a batch job: multi-threaded step vs `AsyncItemProcessor` vs **partitioning** (local `PartitionHandler`, later remote) vs simply running N independent jobs — pick one and justify
- [ ] 21.7 Distributed safety: one shared `JobRepository`, `JobExplorer` for monitoring, and why only one instance may launch a given job (ShedLock / clustered Quartz); Spring Batch + Quartz vs k8s `CronJob` vs an ECS scheduled task
- [ ] 21.8 Batch + our architecture: a batch job writes to its own service DB directly (never through REST) and writes **outbox rows in the same chunk transaction** — the tie-in to Day 11
- [ ] 21.9 Observability: Spring Batch metrics → Micrometer → Datadog; job duration, read/write/skip counts, alerts on failure and on duration regression
- [ ] 21.10 When **not** to use Spring Batch: real-time/streaming (use Kafka consumers), sub-second jobs, anything needing backpressure — plus the tradeoff table vs Airflow / Step Functions / Kafka Streams
- *Exit test:* user can explain why the initial backfill is a batch job while the incremental sync is a scheduled service job.

**DAY 22 — GraphQL alongside REST (client-facing BFF)** *(requested by the user — multiple client types)*
- [ ] 22.1 When GraphQL earns its place vs a REST BFF vs plain REST — the tradeoff table; why "multiple clients" is the justification and over/under-fetching is the problem it actually solves
- [ ] 22.2 Spring for GraphQL, schema-first: SDL, `@QueryMapping`/`@MutationMapping`/`@SchemaMapping`, `DataFetcher`, type mapping, error handling
- [ ] 22.3 **N+1 and `@BatchMapping`/DataLoader** — batching by key, per-request cache, and how it interacts with our REST fan-out
- [ ] 22.4 Pagination and Relay-style connections (cursor-based) — reconciled with the Day 3 REST pagination decision
- [ ] 22.5 **Caching**: POST means no HTTP cache — persisted queries (GET + hash), edge/CDN caching by hash, response caching, per-field TTLs, and what must never be cached (per-user data)
- [ ] 22.6 **Security**: query depth + complexity/cost limits, persisted-query allow-listing, introspection disabled in prod, and rate limiting **by query cost, not request count** (revisit the Day 7 gateway design)
- [ ] 22.7 Authorization: field-level `@PreAuthorize`, forwarding the user JWT from the BFF east-west (ties to 7.6), and the "one query spans two domains" problem
- [ ] 22.8 Error semantics: HTTP 200 with an `errors` array and partial data vs our RFC 7807 model; mapping domain errors to `extensions` codes; mutation idempotency
- [ ] 22.9 Schema evolution: additive changes, `@deprecated`, no URL versioning — contrasted with REST versioning; schema governance + breaking-change detection in CI
- [ ] 22.10 Subscriptions over WebSocket for live updates (and why polling/SSE is often the better answer); federation/stitching — *mentioned only, not built*; per-resolver tracing + operation names in Datadog; `@GraphQlTest` slice tests
- *Exit test:* user can justify which operations stay REST and which become GraphQL, and explain how a malicious deep query is stopped.

**Reserved slots for future EXTRA topics (the user will add these as we proceed):**

| Day | Topic | Status |
|---|---|---|
| 23 | *(proposed: JVM & core-Java internals)* | pending user decision |
| 24 | *(proposed: Kubernetes essentials for a Java dev)* | pending user decision |
| 25 | *(reserved — to be chosen by the user)* | not set |

---

## 10. Decision log (mirror of `docs/decisions.md`, one line each)

| # | Date | Decision | Why | Reversible? |
|---|---|---|---|---|
| D-001 | Day 0 | Monorepo with a Maven aggregator | solo dev, atomic contract changes | yes |
| D-002 | Day 0 | 5 services: gateway, identity, catalog, notification, payment | different scaling/failure profiles | yes |
| D-003 | Day 0 | Kafka for domain events, SQS/SNS for fan-out + delayed reminders | both are learning goals; genuinely different jobs | yes |
| D-004 | Day 0 | ECS Fargate default target, EKS in Terraform as variant | lower ops cost at this size; EKS still taught | yes |
| D-005 | Day 0 | ~~Code is delivered in chat, the user pastes it~~ — **superseded by D-012** | original user instruction | yes |
| D-006 | Day 0 | Threading, third-party API access and circuit breakers are **three independent curriculum topics** (Days 10, 10B, 10C) | user clarified `need.txt` punctuation; threads are used app-wide, not only for outbound calls | yes |
| D-007 | Day 0 | Architecture style = **microservices from a monorepo** (5 deployable services, DB-per-service, no distributed transactions) | learning goal is distributed design; monorepo is orthogonal to deployment topology. Known honest tradeoff: a modular monolith would be the better *product* choice at this scale (see section 3.1) | partially — an optional "start modular, extract later" track exists |
| D-008 | Day 0 | Adopt the **12-item hardening backlog** (section 3.2, H1–H12) into the existing days; keep Tier 2 optional and Tier 3 out of scope | these are the decisions medium/large-company interviews probe, and they cost one task each rather than a new module | yes — tasks can be dropped if time runs short |
| D-009 | Day 0 | User is targeting **medium-to-large companies that run microservices** → keep the 5-service split, and keep the modular-monolith note only as an interview answer | explicit user confirmation | no |
| D-010 | Day 0 | Added **Phase EXTRA** — an open-ended, user-extensible phase numbered from Day 21, starting with **Spring Batch** (Day 21) | user request; Spring Batch fits the initial TMDB/IGDB backfill, retention purge and payment reconciliation, and teaches restartability/idempotency | yes |
| D-011 | Day 0 | **GraphQL is added alongside REST** as a thin client-facing `graphql-bff` service (Day 22, section 3.4). REST stays the internal east-west contract and the contract for simple/cacheable reads | user wants to learn GraphQL because multiple client types need different data shapes. Cost accepted: a 6th deployable, N+1/caching/query-cost concerns. Alternative if service count must stay at 5: skip GraphQL | yes |
| D-012 | Day 0 | **The agent writes code directly into the repo**, one step per turn, explaining before and after — supersedes D-005 | user explicitly granted write access, and asked for step-by-step teaching so no step is missed | yes |

---

## 11. Progress log & where we are

### 11.1 Session history (Day 0 — planning session)
| Rev | What changed |
|---|---|
| rev 1 | Initial plan: architecture, tech stack, repo layout, 20-day curriculum, decision log, guardrails |
| rev 2 | Added 4.1 technology coverage map (`need.txt` → day) and 4.2 out-of-scope list |
| rev 3 | Added 4.3 threading map — the 8 places threads appear in EXR |
| rev 4 | Section 1.1 `need.txt` interpretation; split Day 10 into Days 10 / 10B / 10C |
| rev 5 | Section 3.1 architecture style (microservices from a monorepo) + distributed-monolith guardrails |
| rev 6 | Section 3.2 hardening backlog H1–H12 woven into Days 2, 7, 11, 13, 16, 17, 18, 20; microservices confirmed as the target market |
| rev 7 | **Phase EXTRA** added (open-ended, from Day 21); Day 21 = Spring Batch; slots 22–24 reserved |
| rev 8 | Section 3.3 honest assessment & risk register + 7 proposed changes |
| rev 9 | Section 3.4 GraphQL-as-BFF + Phase EXTRA Day 22; corrected the Tier 3 list which had excluded GraphQL |
| rev 10 | Added section 0 RESUME HERE; refreshed the progress log, work status and open questions |
| rev 11 | Docs moved to `resources/docs/`; recorded the `.gitignore` `resources/` bug as a known issue in section 0; added pending decision P6 (OIDC depth) |
| rev 12 | **Fixed the `.gitignore` `resources/` bug** (bare pattern removed, warning comment added, re-verified); user granted the agent write access to the repo (rule 4 rewritten); rule 5 strengthened to "do not go too fast"; added section 4.4 complete tool/technology inventory |
| rev 13 | Activated **JDK 21** via user-level `JAVA_HOME` (old values backed up to `C:\Users\Nikhil\jdk-env-backup.txt`); verified `mvn` now runs on Java 21; recorded the whole local environment in section 11.3 |

### 11.2 Work status
| Area | Status |
|---|---|
| Planning / architecture | **DONE** — `resources/docs/plan.md` (this file) |
| Product spec | **DONE** — `resources/docs/projectDetails.md` |
| Any code | **STARTED** — `libs/exr-common`: correlation-id filter + 5 passing tests; error model pending |
| `.gitignore` | **DONE and verified** — the `resources/` bug is fixed (rev 12) |
| Build files (`pom.xml`, Maven wrapper) | **DONE** — root aggregator `pom.xml` (Java 21, `packaging=pom`) + Maven wrapper pinning **Maven 3.9.15**; verified with `mvnw.cmd -v` |
| `README.md`, `.editorconfig`, `.gitattributes` | **DONE** |
| git repository | **DONE** — `origin` = `https://github.com/electronik54/TW_systemArch.git`, **default branch is `master`** (not `main`), first commits pushed |
| CI pipeline | **DONE** — `.github/workflows/ci.yml`, first run green on `master` (run 42940713385). `mvnw` is committed as `100755` |
| Docs location | **MOVED** — `docs/` → `resources/docs/` (end of Day 0) |
| Local infra (Docker Compose, LocalStack) | **NOT STARTED** |
| Curriculum Days 1–22 | **NOT STARTED** — 0 of ~150 checkboxes ticked |

### 11.3 Local environment (verified 2026-10-28)

| Tool | Version / location | Notes |
|---|---|---|
| **JAVA_HOME** (user) | `C:\Program Files\Java\jdk-21.0.10` | set in rev 12; **Maven and `mvnw` read this → they build with Java 21** ✅ |
| JDK 21 | 21.0.10 | `javac 21.0.10` verified ✅ |
| Java on PATH | **JDK 17** (`C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot`) | machine-level PATH is evaluated before user PATH, so a bare `java -version` still reports 17 |
| Maven | 3.9.15 | verified running on Java 21 once `JAVA_HOME` is set ✅ |
| Git | 2.45.1 | ✅ |
| Docker | 29.6.1 installed | ⚠️ **daemon not running** — start Docker Desktop before Day 2 (Postgres/Redis) |
| Node / npm | 24.13.0 / 10.5.2 | ✅ (needed Day 19) |
| AWS CLI | 2.34.2 | ✅ |
| Terraform | **not installed** | needed Day 17 only |
| Env backup | `C:\Users\Nikhil\jdk-env-backup.txt` | previous `JAVA_HOME`/`PATH` values, in case of rollback |

**Known caveat (harmless, but know the answer if asked):** bare `java -version` reports **17** in a fresh terminal, because the *machine* PATH lists Microsoft JDK 17 first and a *user* PATH entry cannot override a machine entry. Our builds are unaffected — `mvnw` and Maven resolve the JDK from `JAVA_HOME`, which is exactly how CI pipelines and containers do it. To make `java` itself report 21, reorder or remove that entry in the **System** environment variables (needs admin).

**Resume point:** see section 0. Nothing is implemented; the next action is the topic gate plus pending decisions P1 and P2.

---

### 11.4 Phase 0 revision — in plain words

**What Phase 0 delivered**

| # | Thing | Plain meaning |
|---|---|---|
| 1 | Maven parent POM (`packaging=pom`) | The master control file. Holds every app under it and controls all library versions in one place. |
| 2 | Maven wrapper (`mvnw`) | A copy of the build tool stored *with the code*, so every machine uses the same one. |
| 3 | `.gitignore` | The "do not upload this" list — secrets, build output, IDE files. |
| 4 | `.editorconfig` | A polite **request** to editors about spacing and line endings. |
| 5 | `.gitattributes` | The firm **rule** git obeys when storing files — no CRLF in the repository, ever. |
| 6 | `README.md` | The front door: what this is, how to build it, what the conventions are. |
| 7 | `.github/workflows/ci.yml` | **GitHub Actions** (I sometimes call it "the robot"): GitHub's own machines rebuild and test the project on every upload. GitHub only looks in `.github/workflows/`; the runner is a fresh Linux VM with **no Maven installed** — hence the wrapper. |
| 8 | `libs/exr-common` | The small shared box: request "claim tickets" + one error shape, wired in automatically. |

**Note — who actually consumes `exr-common`:** it is a **library**, not a service; it never runs alone, it is compiled into whichever service declares it. `identity`, `catalog`, `notification` and `payment` are servlet MVC, so the auto-configuration applies. The **`api-gateway` is reactive (WebFlux/Netty)** — servlet `Filter` and `@RestControllerAdvice` do not exist there, so it needs its own reactive correlation-id handling (Day 7). Spring Batch jobs and Kafka-only workers are non-web, so the condition correctly backs off. Consumption is opt-in via a `pom.xml` dependency; the parent provides the version.

**The four separate things that had to be pinned** (this is the reproducibility interview answer)
1. **JDK 21** → `JAVA_HOME` on a machine, `actions/setup-java` in CI
2. **Build tool** → the Maven wrapper (`.mvn/wrapper/maven-wrapper.properties`)
3. **Library versions** → the parent POM
4. **Line endings** → `.gitattributes`

Reproducibility is not one setting; it is all four.

**Why the pipeline runs on both `pull_request` and `push`:** the PR run tests the *merged* result (what would actually land), so a broken branch never reaches `master`; the push run guards `master` itself against direct pushes and merge commits.

**Bugs caught in Phase 0 — all of them invisible in the code**
1. A `resources/` rule in `.gitignore` would have silently dropped `src/main/resources`, `src/test/resources` **and** the planning docs from the repo.
2. `mvnw` was committed as `100644` → `Permission denied` on Linux and in CI, while working fine on Windows.
3. Two JDKs were installed and Maven was quietly using 17.
4. Four files were CRLF while the rule demanded LF — proof that `.editorconfig` advises but `.gitattributes` enforces.

**Test status:** 11 unit tests, all green; `./mvnw verify` green locally **and** in Actions.

**Deliberately deferred:** `@WebMvcTest` coverage of the exception handler (Day 3) · the branch-protection toggle in the GitHub UI (needs check name `Build and test (Java 21)`) · Postgres/Redis/Kafka (Days 2, 9, 11).

**Decisions still open, none blocking Phase 1:** P3 (Day 23 JVM internals), P4 (Day 24 Kubernetes), P5 (Tier 2 additions).

---

## 12. Open questions for the user (ask when relevant)

**Blocking — ask at the start of the next session:** see section 0, pending decisions **P1–P6**.

**Non-blocking — ask when the relevant day arrives:**

1. Is there a GitHub remote already, or do we `git init` locally first? *(Day 1)*
2. Do you have an AWS account, and is a small monthly spend acceptable? *(Day 17)* — if not: LocalStack-only plus a written Terraform plan.
3. Do you have TMDB/IGDB API keys, or should ingestion run against the seeded provider + WireMock only? *(Day 10)*
4. Is Datadog available (trial), or do we use Micrometer/Prometheus locally and document the Datadog wiring? *(Day 16)*
5. How much time per session do you realistically have (30 min / 1 h / 2 h)? This decides whether a "day" gets split.

---

## 13. Guardrails / do-nots

- Do not add services, features or libraries that are not tied to a curriculum topic.
- Do not teach three concepts in one turn; if a topic is too big, split it and say so.
- Writing code into the repo is allowed (rule 4), but never more than one step at a time, and never without explaining it before and after (rule 5).
- Do not run `git commit/push/reset` unasked.
- Do not claim something works until it compiles and its tests run.
- Do not skip the tradeoff / failure-mode / 10x framing — it is the whole point of the exercise.





