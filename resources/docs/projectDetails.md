# EXR — Project Details (product spec / requirements)

> **Status:** v1 draft — Day 0. This file is the single source of truth for *what the product does*.
> **How to use:** read before any feature work. When a feature decision changes, update the relevant section here and note it in `resources/docs/plan.md` section 10 (Decision log).
> **Architecture:** microservices from a monorepo — 5 independently deployable services, DB-per-service, no distributed transactions. Rationale, tradeoffs and the distributed-monolith guardrails are in `resources/docs/plan.md` sections 3 and 3.1.
> **Implementation plan / curriculum:** see `resources/docs/plan.md`.

---

## 1. One-liner

EXR (Event eXploreR) tracks release dates of **games, movies, web series, anime** and **live local events**, and makes sure the user never misses them — via notifications and calendar reminders.

## 2. Problem statement

Release information is scattered across many sources (TMDB, IGDB, studio sites, local venue listings), changes often, and is easy to miss. Users want **one list they control**, with **proactive reminders** at the right time, without manually checking five websites.

## 3. Goals (v1)

- Aggregate events from third-party sources into one normalised catalogue.
- Let anyone discover and browse events (guests, no login required).
- Let registered users **subscribe / unsubscribe** to events.
- Notify subscribers on: **new event**, **event update** (date/status change), and **approaching release date** (configurable lead time).
- Offer **calendar reminders** (refreshable calendar feed + single-event `.ics` download).
- Enforce **subscription quotas** per user tier.
- Provide an **admin panel** for event and user management.
- Provide a **mock payment gateway** to upgrade a free user to paid.

## 4. Explicit non-goals (v1)

- No real payment provider integration (Stripe is mocked behind the same interface).
- No social features (comments, sharing, follower feeds).
- No recommendations / ML ranking.
- No native mobile app (responsive web only).
- No multi-region, no multi-tenancy.
- No rich media (trailers, artwork beyond a poster URL).
- No ticket purchasing or venue booking.

---

## 5. User roles & capabilities

| Capability | Guest | Free user | Paid user | Admin | Developer |
|---|---|---|---|---|---|
| Browse / search events | yes | yes | yes | yes | yes |
| View event detail | yes | yes | yes | yes | yes |
| Register / login | yes | — | — | — | — |
| Subscribe / unsubscribe events | no | yes (max **2**) | yes (unlimited) | yes (unlimited) | yes (unlimited) |
| Receive notifications | no | yes | yes | yes | yes |
| Calendar feed / `.ics` | no | yes | yes | yes | yes |
| Configure channels & lead time | no | yes | yes | yes | yes |
| Upgrade via mock payment | no | yes | — | — | — |
| Admin panel access | no | no | no | yes | yes |
| Create / edit / delete events | no | no | no | yes | yes |
| Manage users (edit, suspend, change tier) | no | no | no | yes **except developers** | yes (all users) |
| View audit log | no | no | no | yes | yes |
| Feature flags / system config | no | no | no | no | yes |

**Role ordering:** `GUEST < FREE_USER < PAID_USER < ADMIN < DEVELOPER`.
**Rules that matter:**
- An **admin may never modify or suspend a developer** account (enforced server-side, not merely hidden in the UI).
- Role changes are **audited** (who, when, from → to, reason).
- A user holds exactly one tier; `DEVELOPER` implicitly satisfies `PAID_USER` and `ADMIN` capabilities.

---

## 6. Event catalogue

### 6.1 Categories (v1)
`GAME`, `MOVIE`, `TV_SERIES`, `ANIME`, `LOCAL_EVENT`.

### 6.2 Event fields
- `id`, `sourceId` (external id), `source` (TMDB / IGDB / SEED / MANUAL)
- `category`, `title`, `synopsis` (short, licence-safe)
- `releaseDate` (nullable — some titles are "TBA"), `releasePrecision` (`DAY` | `MONTH` | `YEAR` | `UNKNOWN`)
- `status` (`ANNOUNCED`, `CONFIRMED`, `RELEASED`, `DELAYED`, `CANCELLED`)
- `posterUrl`, `genres[]`, `platforms[]` (games), `network` (TV), `venue` + `city` + `country` (local events)
- `externalUrl`, `lastSyncedAt`, `sourceUpdatedAt`
- `createdAt`, `updatedAt`, `version`

### 6.3 Search & filters
Title search; filters by category, status, release-date range, platform/genre, city (local events); sort by release date (asc/desc) or recently added; paginated. **Pagination style is an open decision (Day 3).**

### 6.4 Data sources & ingestion

| Source | Covers | Notes |
|---|---|---|
| TMDB | Movies, TV series | API key + attribution required; storage/caching restricted by ToS |
| IGDB | Games | Twitch client-credentials OAuth2 flow (a bonus OAuth2 teaching example) |
| Seeded provider (own fixture data) | Anime, local live events | Stand-in for a paid feed; keeps the project self-contained and testable offline |

Ingestion rules:
- **Idempotent upsert** keyed on `(source, sourceId)`.
- Only metadata permitted by the source licence is persisted; anything else is fetched on demand and cached only.
- **Retention:** each source has a configurable `retentionDays` / `cacheTtl`; a purge job removes data past the allowed window (or replaces it with a stub that triggers re-fetch).
- Ingestion is **asynchronous and rate-limited**; a third-party outage must never break user-facing reads (serve cached/stale data instead).

---

## 7. Subscriptions

- A subscription links `userId ↔ eventId` with `notifyOnNew`, `notifyOnUpdate`, `notifyLeadTime` (7d / 3d / 1d / 1h), `channels[]`.
- Quota: **free users max 2 active subscriptions**; paid/unlimited otherwise. Enforcement must be **atomic** — concurrent requests must not exceed the quota.
- Unsubscribe is a soft delete (keeps history) but frees the quota slot immediately.
- Subscribing to an already-subscribed event is **idempotent** (success, and it does not consume a second slot).
- The subscription list is a first-class view: upcoming, past, cancelled.

## 8. Notifications & reminders

- **Triggers:** new event matching a subscription; event date/status change; approaching release date (lead time); tier/payment confirmations.
- **Channels (v1):** email (SES/SMTP; local = MailHog or LocalStack SES), in-app notification centre, calendar (`.ics` feed URL + per-event download). Web push is a stretch goal.
- **Delivery rules:** user-timezone aware; per-channel opt-in; quiet hours; **deduplication** (the same event + same change must never notify twice); retries with backoff, then DLQ.
- **Guarantee:** at-least-once delivery with an idempotent consumer → effectively-once user-visible behaviour.

## 9. Payments (mock)

- Free → paid upgrade is required to subscribe to more than 2 events.
- Flow: create order → mock provider checkout → provider **webhook** → verify signature → mark paid → upgrade tier → publish `UserUpgraded`.
- **Idempotency:** a replayed webhook must not double-charge or double-upgrade (idempotency key + unique constraint).
- **Saga / compensation:** if the tier upgrade fails after payment capture, retry, then flag for reconciliation rather than losing the payment.
- Plans and prices are **config, not code**. `DEVELOPER` and `ADMIN` never pay.

---

## 10. Admin & developer capabilities

- Event CRUD plus manual override of source data. **Precedence rule:** a manual override wins over the next automated sync (must be explicit in the model, e.g. `overriddenFields[]`).
- Trigger a manual re-sync for a source/category; view ingestion job status and failures.
- User management: search, view, change tier, suspend/reactivate, force logout (token revocation).
- Audit log of every privileged action.
- Developer-only: feature flags, retention/TTL config, replay a Kafka topic or an SQS DLQ.

## 11. UX outline (thin — the user is not focused on UI)

- **Public:** landing/list with filters + search, event detail, login/register.
- **Authenticated:** my subscriptions, notification preferences, calendar feed URL, upgrade page, notification centre.
- **Admin:** event table + editor, user table, audit log.
- Built with React 18 + TS + Vite + TanStack Query. v1 scope: auth, list, subscribe, calendar, upgrade.

## 12. Cross-cutting requirements

- **Security:** OAuth2/OIDC + JWT (short-lived access token, rotating refresh token); BCrypt/Argon2 password hashing; rate limiting at the edge; server-side authorization on every privileged action; secrets never in code.
- **Performance:** p95 read latency target < 200 ms served from cache; catalogue reads must survive a third-party outage.
- **Availability:** 99.9 % for browse/subscribe; notification delivery best-effort with retries.
- **Observability:** every request traced end-to-end; SLO dashboards; alerts on error rate, queue depth, circuit-breaker state, cache hit ratio.
- **Compliance/legal:** source attribution where required; retention limits; GDPR-style delete-my-data (deletes the user, subscriptions and tokens; keeps an anonymised audit trail).

## 13. Open questions (resolve before the relevant day)

1. Page-based vs cursor pagination for the catalogue? *(Day 3)*
2. Do subscriptions live in `catalog` (near events) or `identity` (near entitlements)? *(Day 8)*
3. Synchronous entitlement check vs tier claim inside the JWT vs cached entitlement? *(Day 7–8)*
4. Kafka **and** SQS, or just one? Both are learning goals, so they are scoped to different jobs. *(Day 11–12)*
5. Calendar: feed URL only, or also push reminders by email? *(Day 13)*
6. ECS Fargate vs EKS as the deployed target — Terraform will show both, but one must be the default. *(Day 17)*
7. Retention windows per source: exact `retentionDays` / `cacheTtl` values. *(Day 2, Day 9)*


