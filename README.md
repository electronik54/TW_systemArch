# EXR — Event eXploreR

Tracks release dates for **games, movies, web series, anime and live local events**, and notifies
subscribers before they miss them — by email and calendar reminder.

> **Status:** Day 1 of the build. The repository skeleton exists; there are no services yet.
> Read [`resources/docs/plan.md`](resources/docs/plan.md) first — it holds the curriculum, the
> architecture decisions and the progress log.

## Architecture in one line

Microservices from a monorepo — `api-gateway`, `identity`, `catalog`, `notification`, `payment`
(+ an optional thin `graphql-bff`) — database per service, Kafka for domain events via a
transactional outbox, SQS/SNS for fan-out and delayed reminders, Redis for cache and rate limiting.

Full rationale: `resources/docs/plan.md` sections 3, 3.1, 3.4.

## Prerequisites

| Requirement | Why |
|---|---|
| **JDK 21** | The build targets Java 21. `JAVA_HOME` must point at it — Maven reads `JAVA_HOME`, not the `java` on `PATH` |
| **Docker Desktop** | Required from Day 2 onwards (PostgreSQL, Redis, Kafka, LocalStack) |

Maven itself is **not** a prerequisite — it is pinned by the wrapper.

## Build and test

```bash
./mvnw verify          # Linux / macOS / Git Bash
mvnw.cmd verify        # Windows (cmd / PowerShell)
```

Always use the wrapper, never a globally installed `mvn`: the wrapper pins Maven **3.9.15**
(`.mvn/wrapper/maven-wrapper.properties`), so your machine, your teammate's machine and CI all
build with the same Maven. The **JDK** is a separate concern and comes from `JAVA_HOME`.

## Repository layout

```
pom.xml                  Maven aggregator + parent (single source of version governance)
mvnw / mvnw.cmd          Maven wrapper (pinned to 3.9.15)
.editorconfig            Editor-agnostic formatting: UTF-8, LF, 4-space indent
libs/exr-common          shared web/observability primitives — never domain entities
services/                independently deployable Spring Boot services
infra/                   docker-compose for local dev, Terraform for AWS
ui/web/                  React + TypeScript SPA
resources/docs/          plan.md (curriculum + decisions + progress), projectDetails.md (spec)
```

## Documentation

| File | Contents |
|---|---|
| `resources/docs/plan.md` | Curriculum by day, architecture decisions (D-001…), hardening backlog, progress log |
| `resources/docs/projectDetails.md` | Product spec: roles and quotas, event model, sources, notifications, payments |

## Conventions

- **Conventional Commits** — `feat:`, `fix:`, `test:`, `docs:`, `chore:`, `infra:`
- **UTF-8, LF line endings**, 4-space indent for Java and 2-space for YAML/JSON/TS (see `.editorconfig`)
- **No secrets in the repository** — `.env` is git-ignored; cloud secrets live in AWS Secrets Manager
- **Errors** use RFC 7807 `ProblemDetail` with a correlation id
- **Never expose JPA entities** from a controller — API DTOs are records
