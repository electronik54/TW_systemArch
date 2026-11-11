# EXR — Documentation Index

> **START HERE.** This is the entry point for any developer or AI coding agent joining the
> project. It takes about 60 seconds to read. Everything deeper is one link away.

---

## 1. What this project is

**EXR (Event eXploreR)** tracks release dates for **games, movies, web series, anime and live
local events**, and notifies subscribers before they miss them — by email and calendar reminder.

It is *also* deliberately a **learning vehicle**: the product is the excuse, production-grade
system design is the point. Java 21 + Spring Boot 3 microservices on AWS.

## 2. Read these in this order

| # | File | What it holds | Read it when |
|---|---|---|---|
| 1 | **[`plan.md`](plan.md)** | **The operating manual.** Working rules, architecture, decisions (D-001…), the day-by-day curriculum, hardening backlog, progress log, resume point | **Always first** — especially §0 and §1 |
| 2 | [`projectDetails.md`](projectDetails.md) | Product spec: roles & quotas, event model, data sources, notifications, payments, admin/dev capabilities | Before any feature work |
| 3 | [`need.txt`](need.txt) | Learning coverage map — what we build hands-on vs what is deliberately out of scope, plus answered design questions | When asking "do we cover X?" |

## 3. Current status

| | |
|---|---|
| **Phase** | Phase 0 (foundations) complete · Day 1 complete · **next: Day 2 — data design** |
| **Code** | `libs/exr-common` only: correlation-id filter, RFC 7807 error model, auto-configuration. **11 unit tests green** |
| **Services** | **none yet** — no `services/` module exists |
| **CI** | `.github/workflows/ci.yml` green on `master` |
| **Repo** | `https://github.com/electronik54/TW_systemArch` (branch `master`) |

**Authoritative resume point:** [`plan.md` §0 (RESUME HERE)](plan.md) and §11 (progress log).
This table is a convenience copy — if the two ever disagree, `plan.md` wins.

## 4. Architecture in one line

Microservices from a monorepo: `api-gateway`, `identity`, `catalog`, `notification`, `payment`
(+ an optional thin `graphql-bff`). Database per service, Kafka for domain events via a
**transactional outbox**, SQS/SNS for fan-out and delayed reminders, Redis for cache +
rate limiting + idempotency. Rationale and guardrails: `plan.md` §3, §3.1, §3.4.

## 5. Repository layout

```
pom.xml                  Maven aggregator + parent (single source of version governance)
mvnw / mvnw.cmd          Maven wrapper (Maven pinned to 3.9.15)
.github/workflows/       ci.yml — checkout -> JDK 21 -> ./mvnw verify
libs/exr-common/         shared plumbing (request ids, error shape) — NEVER domain code
services/                one module per service (empty until Day 2+)
infra/                   docker-compose for local dev, Terraform for AWS (not yet created)
ui/web/                  React + TypeScript SPA (not yet created)
resources/docs/          this file, plan.md, projectDetails.md, need.txt
```

## 6. Rules an agent must not break

The full list is `plan.md` §1. The four that cause real damage if forgotten:

1. **One step at a time.** Explain before writing, explain after, then stop and wait. One
   concept or one file per turn — never a five-file dump.
2. **Gate every topic.** Ask whether the user already knows it before teaching it.
3. **The user runs git.** Never `git add/commit/push/amend/reset` unasked; propose the exact
   command and say *why*.
4. **Never put domain code in `libs/exr-common`.** Request ids and error shapes only. If it
   ever imports a JPA entity, the service boundaries have collapsed.

Also: `plan.md` §1.1 records that threading, third-party API access and circuit breakers are
**three separate topics** — do not re-bundle them.

## 7. How to resume work

1. Read `plan.md` §0 (RESUME HERE) — it lists the pending decisions.
2. Read `plan.md` §11 (progress) — what is done, what is not.
3. Run the topic gate, then continue from the next unticked item in §9.

> **Last updated:** Day 1 closeout. Keep this file's status table in sync whenever `plan.md`
> advances a phase.
