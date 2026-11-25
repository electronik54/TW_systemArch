# EXR — Plan & Agent Operating Manual

> **Product:** EXR (Event eXploreR) — release-date & live-event tracking with notifications.
> **User goal:** learn production-grade system design/architecture hands-on (Java 21 + microservices) using this project as the vehicle, and prepare for a system-design interview.
> **Last updated:** Day 5 (rev 48 — a full read-through audit of code and docs, which corrected the stale status blocks below; rev 47: **PR #6 landed on `master` as `8a1f420`**, and both halves of the token story are proven in the user's own Postman — a **user** token is minted at `POST /api/auth/login` and validated locally by `catalog` (no call back, no database read), a **service** token is minted at `POST /api/auth/service-token` from `X-Service-Key`, and `@PreAuthorize("hasRole('ADMIN')")` refuses that service token with **403**). **5.1 ✅ 5.3 ✅ 5.4 ✅ · 5.2 access half · 7.6 both halves built · still open: 5.2 refresh (rotation + reuse detection), 5.3 JWKS (needs RSA), 5.4 `RoleHierarchy`, 5.5 revocation (needs Redis, Day 9).** History, newest first — rev 44: **4.3 closed, verified with the user's own eyes.** Five wrong passwords left `failed_attempts = 5` with `locked_until` **in the future** and `status` still `ENABLED` — so the lock is a deadline, not a status flip — and then the **correct** password came back `401` with `failed_attempts` still `5`. That last pair is the ordering proof: the account-state flag is read **before** the password is compared, and a locked attempt never becomes a `BadCredentials` event, so nothing is written for it. 4.1 ticked with it (the three account states are now exercised, not just mapped). rev 43: **`UserLockoutTest` ran and passed** — the user's own Maven run printed `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`, with **no** Testcontainers, Docker or `jdbc:postgresql:` line anywhere in the output, which is the whole point of a domain test: it built a `User` object and did arithmetic on two fields. 4.3 is now code-complete **and** test-proven; the only thing left is the DBeaver read of `failed_attempts` / `locked_until`. rev 42: **questions must name the class and the method** — the user's verdict *"which last test? which method failed to do what?"* turned into a new rule 24 bullet that bans "the last test" and "this one" in favour of `UserLockoutTest.expiredDeadlineStartsAFreshRun` and `User.recordFailedLogin`. rev 41: **the login door is drawn, `identity-service` has its first test, and the work is committed** — `HowIsUserUpdatedOnLoginAttempt.md` gained **§12, the request-level picture** (the filter stack a request walks, the inside of `BasicAuthenticationFilter`, four consequences), a pointer in §1 and one row in the §2 cast table naming **`DaoAuthenticationProvider`** as the class that actually refuses; the account-state question was answered in one place (`DbUserDetailsService` 75–77) with two traps recorded (`status = 'LOCKED'` never expires by itself; the automatic lock is a **deadline**); `UserLockoutTest` — the module's first test class — was **written and not yet run**; the session ended on git, three commits on `feature/catalog-2.6-2.7` (`bded4a8` code, `d70881f` test, `9fe1b93` docs), **ahead 3, not pushed**. 4.3 stays `[~]` (the DBeaver row check is still the missing proof); 2.7 stays `[~]` (steps 3–4). rev 40: **the working toolchain is written down** — the services run from **IntelliJ IDEA**, HTTP requests are sent from **Postman**, and the database is inspected in **DBeaver**, so every verification step is handed over that way instead of `curl` and `psql` (§0, §4.4, §11.3). rev 37: **API compatibility policy written down** — **D-017**, curriculum item **3.5**, and a rule in §6: additive-first, deprecate-then-remove with a date, `/api/v1/...` added at the **gateway** only for a genuinely breaking change, with "breaking" defined. rev 36: **Day 4 opened — `identity-service` exists**, and its first start failed because `target/classes` had no `application.yml`, which is why Tomcat came up on **8080** and the datasource was missing; `spring-boot-maven-plugin` now makes `mvnw spring-boot:run` copy resources by construction. **New rule 24 — questions must be answerable by a beginner**: no not-yet-taught vocabulary, no two asks in one sentence, no "why" before "what happens". 2.7 stays `[~]` (steps 3–4 pending) while Day 4 runs. rev 35: **Day 2.7 started — `@Transactional` in depth.** A real service layer now exists: `catalog/application/EventService` with `importAll` as one unit of work, proven by `EventServiceTransactionTest` (the only test class without `@Transactional`, because it must let the service commit; `JdbcTemplate` is the outside observer, since `EntityManager` would answer from the persistence context). The project gained its **four-layer layout**: `api` (empty until Day 3) · `application` (services) · `domain` (model only) · `infra`. Step 2 of 4 is half done: **`V3__import_audit.sql`** adds `import_attempts`, the row that must survive a failed import via `REQUIRES_NEW`. **Resume at:** the `ImportAttempt` entity, the audit service with `Propagation.REQUIRES_NEW`, and the `flush()` inside `try` that pulls a commit-time failure back into the method. rev 34: **git is taught with pictures from now on** — the user confirmed the ASCII commit tree is easier to read than prose, so rule 23 is now unconditional: every git/GitHub step draws the tree, even when nothing moved; and the full recap session for a large team is planned in **`resources/docs/git_recap.md`** (two clones, four text files, five stages, a diagram and 2–3 questions per step). rev 33: **Day 2.6 closed** — `V2__event_search.sql` adds a generated `search_vector` (title `simple`/weight `A`, synopsis `english`/weight `B`) and `events_search_gin`; `EventSearchRepository` searches through it in plain JDBC; `EventSearchTest` proves title-beats-synopsis, "It" stays findable, and `EXPLAIN` names the index. Three teaching traps recorded: a *non-immutable* generation expression refuses to migrate, a query must be phrased in the same config as the document, and a weight multiplies `ts_rank` linearly. **Next: `2.7` `@Transactional` in depth.** rev 32: **the CI gate is enforced and was proven** — a `master` ruleset requires `Build and test (Java 21)` with an empty bypass list; the lab's red merge was reverted out and the trunk is green again; rev 31: **GitHub explanations must carry a diagram** — new rule 23, ASCII commit lines drawn with the user's own hashes, before/after when something moved; rev 30: **the squash-merge incident recorded** — three commits squashed into `86c9f58`, the next branch cut from the old local tip, the add/add conflict, and the rebuild through the reflog; rev 29: **the reduced explanation style is now the default and self-repair inside a reply is banned** — new rule 22, rule 20 widened; rev 28: the **branching strategy is now project documentation** — `resources/docs/git-workflow.md`; rev 27: **trunk-based development adopted** — `master` is the only long-lived branch, short-lived `feature/*` branches reach it by pull request with green checks, unfinished work hides behind flags; the two-branch idea from rev 26 was dropped. **Day 2.5 and the CI-gate lab closed** (18 green, CI's Postgres sidecar deleted, `master` protected and green); next: `2.6` `tsvector` + GIN)
> **Companion docs:** `resources/docs/projectDetails.md` (feature spec) · `resources/docs/git-workflow.md` (**branching strategy as architecture** — trunk-based model, PR rules, protection, tagging) · `resources/docs/decisions.md` (ADR log — **not created yet**; until it is, the `D-xxx` list in §3 and the revision log in §11.1 are the record)

---

## 0. RESUME HERE (**Day 5: 5.1 ✅ 5.3 ✅ 5.4 ✅, 5.2 half, 7.6 built · PR #6 merged · open: 5.2 refresh, 5.3 JWKS, 5.4 hierarchy, 5.5 revocation**)

**Status.** Day 1 (Phase 0) is complete, Day 2 is complete as far as 2.6 (2.7 steps 1–3 done, step 4 parked until Day 8), Day 4 is complete for 4.1–4.3 (4.4 parked until Day 7's gateway), and **Day 5 is three-quarters done**: a login returns a token, `catalog` validates it without asking anybody, method security refuses by role, and 7.6's service token exists on both sides. Two services now run side by side — `catalog-service` on **8081**, `identity-service` on **8082** — each with its **own database**:

- `services/catalog-service` exists with **Java source now**: `CatalogServiceApplication`, `application.yml` (credentials via `${EXR_DB_*}` env vars), `Source`/`Event` entities, `EventRepository`, `EventSearchRepository`, and seven test classes; `services/identity-service` exists with its own `identity` database, `users` (V1) + `failed_attempts`/`locked_until` (V2), and six tests. **33 tests green** across the reactor (11 in `exr-common`, 16 in `catalog-service`, 6 in `identity-service`) — and since **2.5** the persistence tests start their **own** `postgres:16` through Testcontainers, so `docker ps` is enough and `exr-pg` is not required.
- `V1__catalog_init.sql` — 4 tables, 19 indexes — is **written, executed and verified** against PostgreSQL 16.
- **Flyway itself is now verified** through Spring Boot: `flyway_schema_history` was created, V1 recorded with checksum `189898812`, and a second run logged *"Schema public is up to date. No migration necessary."* (idempotency proven). **V1 is now frozen — never edit it.**
- **The service boots (2.4a).** Started from IntelliJ in **3.531 s** against PostgreSQL **16.15**: Hikari pool → `Successfully validated 1 migration` (stored checksum matched) → *"up to date, no migration necessary"* → Hibernate 6.5.3 `EntityManagerFactory` → Tomcat on **8081**. The startup order is confirmed in a real log, not just in a comment.
- `libs/exr-common` is complete: correlation-id filter + RFC 7807 error model + auto-configuration.
- CI is green on `master`; repo is `https://github.com/electronik54/TW_systemArch` (default branch `master`).
- **Branch model (rev 27): TRUNK-BASED — one long-lived branch.** `master` is the product and is always releasable; every merge lands by **pull request with green checks** (no direct pushes, no second integration branch). Work happens on **short-lived `feature/*` branches** — hours to a day or two, not weeks — squash-merged and deleted. A release is a **tag on `master`** (`v0.1.0`), not a merge from another branch. Unfinished work is kept out of the product by a **flag** (`@ConditionalOnProperty` / a config value in `application.yml`), never by a branch that lives for weeks. A fix is a short `fix/*` branch off `master` — there is nothing to back-merge, which is the whole point. `git revert` is the first response to red on `master`. **The full page is `resources/docs/git-workflow.md`** — branch types and lifetimes, the ten rules with reasons, the exact branch-protection settings to enable, the red-build table by role, and the alternatives that were rejected.
- **The gate is enforced, and it was proven the hard way (rev 32, 2026-11-13).** A `master` **ruleset** requires **`Build and test (Java 21)`**, restricts deletions, blocks force pushes, and keeps an **empty bypass list** — so it binds the admin too. The lab ran out of order: PR #3 merged while its check was still running (no check was *required*, so nothing stopped it), `master` went red on `914d4fb`, its own push run **#23** failed, and the trunk came back through a **revert PR** (#4 → `c315fc3`, −39 lines), not a fix-forward. Mechanics and the two traps (a check cannot be *required* before it has reported once; `git branch -d` cannot delete a squash-merged branch): `git-workflow.md` §4 and §10.

**Doc map:** start at `index.md` → `plan.md` (this file) → `projectDetails.md`. `need.txt` is the learning coverage map; `catalog/schema.md` is the catalog data model in full; **`git-workflow.md` is the branching strategy** (read it before committing); **`git_recap.md` is the planned step-by-step git session** (sandbox, five stages, a diagram at every step) — run it on request.

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
| P5 | Adopt any **Tier 2** additions? | load shedding · feature flags · saga orchestration — individually *(read replica already promoted to 17.3)* | section 3.2 |
| P6 | **How deep on OIDC?** OAuth2/OIDC + JWT are *already* covered (Days 4, 5, 7.2, 7.6, 18.2, 22.7). The genuine gap is OIDC as an **external IdP**: OIDC discovery (`.well-known`), `id_token` vs `access_token`, nonce/state, token introspection (RFC 7662), and the **build-vs-buy "who is the authorization server"** decision (own it vs Keycloak vs Auth0/Cognito) | yes / no | section 9, Days 5–6 |

**RESOLVED at the start of Day 1 (2026-10-28):** **P1** = keep the current day order (no walking-skeleton reorder) · **P2** = start at **Day 1.1** (repo skeleton) · **P6** = defer — Days 4–7 stay as written, no external IdP for now. · **P3/P4/P5** remain open (nothing blocked by them before Day 17).

**Also still open:** the five questions in section 12 (git remote, AWS account + budget, TMDB/IGDB keys, Datadog availability, realistic session length).

**WHERE DAY 5 STANDS (rev 47).** 5.1, 5.3 and 5.4 are done and proven in the user's own Postman: a token is minted at `POST /api/auth/login`, `catalog` validates it locally with no call back and no database read, and `@PreAuthorize("hasRole('ADMIN')")` answers **403** for a token holding `ROLE_SERVICE`. **7.6 is half-open:** a service mints its own token from `X-Service-Key`, but the key is shared and HMAC lets every verifier mint. **Still open:** 5.2's refresh token (rotation + reuse detection), 5.3's JWKS (needs RSA), 5.4's `RoleHierarchy` bean, 5.5's revocation (needs Redis, Day 9). The two threads below are history — both closed.

1. **4.3 — CLOSED (rev 44), and the last proof was read on screen.** After five wrong passwords the row held `failed_attempts = 5`, a `locked_until` **in the future**, and `status = ENABLED`. Then the **correct** password returned `401` and the counter stayed `5`. Together those two readings prove the chain *and* its order: the write happens inside the filter, the lock is a **deadline** rather than a state change, and the refusal arrives **before** the password comparison, so a locked attempt is never counted as a failure.
2. **`UserLockoutTest` — CLOSED (rev 43).** Four tests, and the user's own run printed `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0` on 2026-11-17, with no container or JDBC line in the output. What they prove: the fifth failure writes a deadline about 15 minutes ahead, the fourth does not; a success clears both fields; and a failure after an expired deadline restarts the count at 1. What they do **not** prove: that Spring refuses a locked account — that needs a real request, which is thread 1.

**NEXT ACTION (rev 47) — pick the next thread: 5.2's refresh token (rotation, reuse detection, the `refresh_tokens` table), or Day 6's social login (blocked on a GitHub OAuth App, a `user_identities` table, and a nullable `password_hash` — V1 line 29 currently makes a federated user impossible).** The paragraph that follows is the **2.7 resume note from rev 35, kept as history** — all four of its steps are done (2.7 step 4, isolation and the lost update, is parked until Day 8 where the quota race gives it a reason). Where the session stopped: `EventService.importAll` is a proven transaction boundary (25 tests green on the user's machine: 11 + 14), the four-layer layout is in place (`api` · `application` · `domain` · `infra`), and `V3__import_audit.sql` created `import_attempts`. **Resume in this order:** (1) `ImportAttempt` entity in `domain`; (2) `ImportAuditService` in `application`, annotated `@Transactional(propagation = Propagation.REQUIRES_NEW)`, whose whole reason to exist is committing *after* the caller fails; (3) rework `importAll` to `try { … entityManager.flush(); } catch (RuntimeException failure) { audit.recordFailure(…); throw failure; }` — the `flush()` is the point, because the clash is otherwise only discovered at commit, outside the method; (4) a test that fails the import and then counts `import_attempts` from a **separate connection** to prove the audit row survived. Then step 3 (a checked exception **commits** by default) and step 4 (isolation levels and the lost update `@Version` already guards against).

1. **2.5 is closed** for the persistence tests: `AbstractIntegrationTest` owns the container and `EventMappingTest`, `EventFetchingTest`, `FlywayMigrationTest`, `SourceMappingTest` all extend it. `application.yml` was deliberately **not** changed — only the test context is redirected, so the app still points at `exr-pg`.
2. Then continue: **2.6** full-text search (`tsvector` + GIN — a perfect `V2` migration exercise), **2.7** `@Transactional` depth, **2.8** hierarchy, **2.9** lifecycle + reports.

**Apply rule 18 from now on:** when a step's value is watching output, hand the user the command and let them run it, then interpret the 2–3 lines that matter.

**Toolchain (rev 40) — hand every step over in these tools, not in a terminal.** The user runs the services from **IntelliJ IDEA**, sends HTTP requests from **Postman**, and inspects the database in **DBeaver**. So a verification step is written as two things: (a) the **Postman request** — method, URL, the **Authorization** tab, and the raw JSON body when there is one; and (b) the **SQL to paste into a DBeaver editor** against the `identity` connection. `curl`, `docker exec … psql` and `mvnw spring-boot:run` are no longer how this project is driven; starting a service means an IDE run configuration, and its console is where the startup log is read.

**Container hygiene:** `docker stop exr-pg` when done; `docker rm exr-pg` to delete it. It holds no data worth keeping.

**Rules the agent must apply on resume:** rule 16 — **announce the technology at the start of every step**; rule 14 — plain language first, jargon in brackets; rule 18 — **the user runs it, the agent hands over the command and reads their output** (covers `mvnw test`/`verify`, starting the app, and Docker); rule 20 — **explain in the smallest form that works, ~250 words, split rather than sprawl**; rule 21 — **every change gets file + line numbers + what + why**; rule 22 — **write it right the first time: never repair, apologise for or re-word a sentence inside a reply — delete it and send the clean one, and always bracket a technical term at first use**; rule 23 — **any explanation that names a remote branch carries an ASCII commit diagram, with a before/after pair when something moved**; rule 10 — **the user runs git, only propose commands**. **Never hand over a multi-line shell snippet:** `cmd.exe` runs each newline as a separate command and does **not** treat `'` as a quote character (only `"` groups words), so a multi-line `git commit -m` stores a mangled message and then tries to execute the leftover body lines as commands. Always give **one physical line**, and use several `-m` flags on that line for multi-paragraph messages.

**Standing constraints (do not violate):** write code directly, but **one step per turn and explain it** (rules 4 + 5) · topic gate before every topic (rule 3) · **the user runs git — only propose the command and explain why** (rule 10) · keep scope small (rule 8) · always give tradeoff + failure mode + 10x answer (rule 12) · never re-bundle threading / third-party APIs / circuit breakers (section 1.1).

---

## 1. INSTRUCTIONS TO THE AGENT — read this first when resuming

1. **Start at `resources/docs/index.md`**, then read **this file** and **`resources/docs/projectDetails.md`** before doing anything. They are the source of truth for scope.
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
10. **The user runs git; the agent advises.** From Day 2 (rev 14) the user performs all git operations themselves, as a deliberate exercise in understanding how code reaches production. The agent must (a) **never** run `git add/commit/push/amend/reset` unless explicitly asked, and (b) instead propose the **exact command** plus a one-line reason *why that command*. Read-only inspection (`git status`, `git log`, `git diff`, `git ls-files`, `git ls-remote`) is still allowed. **A commit message never travels through a shell string:** inside the agent's sandbox `git commit -m "…"` reached `cmd.exe` with its quotes stripped and tried the message words as pathspecs, so messages are written to a file and committed with `git commit -F <file>` (rev 41) — the same reason the multi-line form is banned.
11. **Update section 9 checkboxes and section 11 (Progress) at the end of every session**, and bump "Last updated". This file is the resume point — keep it accurate.
12. **Interview framing (mandatory for every topic):** always include
    (a) the **tradeoff table** (option A vs B, when to pick which),
    (b) the **failure mode** (what breaks, how it degrades),
    (c) the **"what happens at 10x / 100x load"** answer.
    That is what a system-design interview actually grades.
13. **Topic sessions must be < ~45 min of reading.** If a topic needs more, split it and say so.
14. **LANGUAGE STYLE (user request, rev 16) — this is the default for every explanation.** Explain in **plain, everyday language first**, the way you would explain it to a smart colleague from a different team. Use analogies, short sentences and concrete examples. **After** the plain explanation, name the technical term in brackets — e.g. *"the library's quick-find list (a database **index**)"* — because the user needs the vocabulary for interviews. **Never lead with jargon.** Tradeoff tables are welcome; dense prose is not. If an explanation lands flat, re-tell it simpler rather than adding detail.
   - **The bracket is mandatory, not decorative (rev 29).** The user's reason, verbatim: *"If you want to use any technical terms, put them in the bracket so that i can recognize it during the interview."* So every term gets **plain words first, then the term in brackets, then the exact code token when one exists** — *"a branch we create for one demo and delete (a **throwaway branch**, `feature/gate-lab`)"*. A reply that uses the term without ever bracketing it has failed this rule even when it is otherwise clear.
   - **The reduced shape of rule 20 is the DEFAULT shape, not the recovery shape (rev 29).** Write the first attempt the way the second attempt used to be written. See rule 22 — a reply that needs a second attempt is a defect, not a process.
   - **If the user says they did not understand: DO NOT explain in more depth — REDUCE.** Give the **3–4 facts needed right now**, name the day where the rest is covered (so nothing feels lost), and switch to a **hands-on demo** instead of more theory. Long mechanical deep-dives (e.g. Flyway internals: checksums, advisory locks, baseline, transaction boundaries) must be **offered on request only**, never volunteered.
15. **PER-SERVICE SCHEMA DOCS (user request, rev 16).** Every service that has a data model gets `resources/docs/<service-name>/schema.md`, written in the same plain language as rule 14, listing **every column, its type and its constraints**, plus the reason each choice was made. When the schema is already documented there, **do not re-explain it in chat** — point at the file. Update that file whenever the schema changes; it is the durable record, chat is not.
16. **ANNOUNCE THE TECHNOLOGY AT THE START OF EVERY STEP (user request, rev 18).** The user's goal is to learn the **technologies**, not just to accumulate files. So every step must begin with a short header naming (a) **which technology this step teaches**, (b) **where it sits in the curriculum** (e.g. "Day 2.3"), and (c) **what problem it solves** — before any file is created. When a step touches several technologies (a Maven module + a Flyway migration + SQL DDL), name all of them and say explicitly which one is **the actual lesson** and which are supporting plumbing. Never leave the user guessing why a file appeared.
17. **TELL THE USER WHEN A TOPIC BELONGS TO A LATER DAY (user request, rev 19).** When asked about a technology the curriculum schedules for a future day, **say so explicitly** — e.g. *"that's Day 18.6 — we'll do it properly then"* — and give only a short, connecting answer now. Do not pre-teach a later topic in depth; it lands far better when the earlier pieces exist to attach to. Always name the day, so the user knows nothing has been lost.
18. **LET THE USER RUN IT AND WATCH IT HAPPEN (user request, rev 20).** Whenever the point of a step is *seeing something happen* — starting the application, running a test, watching a migration apply, watching a constraint reject a row — give the user the **exact command to run in their own terminal**, then **wait** for them to paste the output, and only then explain what they are looking at. Do **not** run it yourself and paste a wall of log; the learning is in watching their own screen. When interpreting, highlight **only the 2–3 lines that matter** and ignore the rest.
    - The agent still owes verification (rule 6) — but verification happens **through the user's run**, not instead of it. If they get an unexpected result, that output is the starting point for the fix.
    - **What "do not run it yourself" covers, explicitly:** `mvnw test`, `mvnw verify`, `mvnw spring-boot:run`, `java -jar`, starting or creating Docker containers, `docker start/stop`, and anything else that connects to a real database or starts the application. Hand over the command instead.
    - **Violated on Day 2.4c (rev 22).** The agent ran `EventMappingTest` twice and the full reactor twice, and started Docker Desktop and the `exr-pg` container itself. The user's objection, verbatim: *"did you run the project by yourself? … I need to see the runtime logs to understand whats going on and how to debug issues."* The logs the agent produced were throwaway artefacts in `target/`; the understanding they contained belonged to a run the user never watched. Do not repeat this.
    - **Read-only inspection is still allowed:** reading files, `git status`, `git log`, `git diff`, `findstr`/`grep` over source. Those change nothing and start nothing.
    - Linux/Windows note: give the command in a form that works in *their* shell, and say which shell it assumes.
19. **CHECK MY UNDERSTANDING AT THE END OF EVERY STEP — ASK, DO NOT TELL IT BACK (user request, rev 21).** Every teaching turn ends with **2–3 short questions** that test whether the *idea* landed, and then the agent **stops**.
    - **Never answer your own questions in the same turn.** The user answers; the next turn reacts to that answer.
    - **Mix the three kinds:** one **recall** (*"what does `@Enumerated(STRING)` actually write into the column?"*), one **consequence** (*"what breaks if we delete it?"*), one **judgement** (*"two tables here need different choices — which, and why?"*).
    - **Test the idea, not the syntax.** *"Which annotation goes where"* is a memory test. *"Why is there no `version` column on `sources`?"* is an understanding test. Prefer the second.
    - **Never phrase one so that "yes" is the answer**, and never ask something that the same turn already stated word for word.
    - **A wrong answer becomes the next step.** Fix the misunderstanding *before* adding new material, then re-ask the same idea one step later to confirm it stuck. Do not move on politely.
    - **If all answers are right:** say so in one line, then advance exactly one step.
    - **If the user is unsure:** apply rule 14 — reduce to the 3–4 facts that matter and switch to a hands-on demo. Do not elaborate.

20. **EXPLAIN IN THE SMALLEST FORM THAT WORKS (user request, rev 22).** The user's verdict: *"Your replies are too long… Your statements are very broken most of the times and difficult to understand."* The version that finally landed was **about 200 words, four short labelled blocks, no tables** — the same material that had already failed twice at 800+ words. Length is not thoroughness; past a point it is unreadable. Apply these:
    - **Write the FIRST attempt in the reduced shape (user request, rev 29).** Verbatim: *"The style that you use the second time, make that you default style."* The second attempt was only ever better because it was forced to be smaller. So the default is now the shape the rule below describes: numbered walk-through, one idea per sentence, a named subject for every verb, no tables, roughly 200 words, every term bracketed at first use.
    - **Default budget: 250 words.** If the answer genuinely needs more, say so in one line and split it across consecutive replies. The user offered this explicitly: *"make them short or break them down into multiple replies."*
    - **One idea per sentence.** No sentence may contain two "while"s, an arrow and two mechanisms at once. That is precisely what "broken statements" means: several facts glued together with dashes instead of separated into their own sentences.
    - **Name the subject of every verb.** Not *"`updated_at` moved"* but *"Hibernate wrote a new value into `updated_at`"*. A sentence with no actor is a sentence the reader has to decode.
    - **Define a term the first time it appears**, even if it looks obvious. The user had never been told that `version` is a counter — so an entire paragraph about that counter was unreadable until the definition was added.
    - **Never write "asserted", "verified" or "proven" without saying what was checked and what value was expected.** Those words are conclusions, not explanations.
    - **Numbered steps beat prose** for anything sequential: what a test does, how a request travels, how a migration applies.
    - **Tables are for comparison only** (option A vs option B). A table whose cells are full sentences is a wall of text wearing a costume.
    - **If the user says they did not understand: apply rule 14 and REDUCE, then split.** Do not add detail, do not add tables, do not offer "a deeper explanation".
    - **A repeat attempt must be MEASURABLY smaller, not merely different.** Being asked to try again is not permission for a new angle at the same size — that is the same answer with new wallpaper. The second attempt must: (a) be **about half the words** of the first, (b) carry **one idea**, (c) contain **no table** unless the first one was a table and the table itself was the confusion, (d) replace a term with an **analogy or a numbered walk-through**, and (e) say in one line **which day the dropped detail comes back**. If the second attempt is longer than the first, the rule was broken, whatever its shape.
    - **Worked example — the failure, and the fix.**
      - Failed (one sentence, four facts, no actor): *"`version` 0 → 1 on update, `created_at` unchanged (`updatable = false`), while `updated_at` moved — both asserted in the second test."*
      - What the user said about it: *"I do not understand anything at all."*
      - Worked (numbered, one idea per step, subject named): *"Step 2: Hibernate sends an INSERT, so the new row has `version = 0`. Step 5: the test changes the title and Hibernate sends an UPDATE; while building it, Hibernate sets `version` to 1 and writes the current time into `updated_at`. It leaves `created_at` out of the UPDATE because the field is mapped `updatable = false`. Step 6: the test checks those three values."*
    - **Corollary for terminology:** when the user says a word like "mapping test" is unclear, explain the *purpose* first in one sentence, then the mechanics. Do not start with the annotations.

21. **EVERY CHANGE GETS A CHANGE LIST: FILE, LINES, WHAT, WHY (user request, rev 22).** The user cannot review work they cannot locate: *"you never mention which file and while line you changed."* After any edit — **code or docs** — state in a compact table or list: the path (e.g. `services/catalog-service/src/main/java/.../Event.java`), the **line range as it exists after the edit** (e.g. `216–231`), **what** changed in one sentence, and **why**.
    - **Doc and comment edits count.** A rewritten javadoc block is a change to the record, so it gets a row like any other.
    - **Say when code came from an earlier session.** If most of a file predates this turn, say so plainly, so the user is not misled into thinking it is new review surface. Example from rev 22: the bulk of `Event.java` and the whole second test method were written in the previous session; that turn only added two `@JdbcTypeCode` lines.
    - **Quote the line numbers where the text is now**, not where it was before the edit.
    - **Mention anything reverted, deleted or superseded**, including throwaway artefacts (generated log files, scratch scripts) so the user knows what appeared on disk and why.

22. **SAY IT RIGHT THE FIRST TIME — NO SELF-REPAIR INSIDE A REPLY (user request, rev 29).** The user's verdict, verbatim: *"when you said `My clumsy question — ignore the wording and take the answer` … You actually do that a lot. Not just while you answer but also when you explain me things. This is why i have to ask you to explain me it again."* A reply that argues with itself costs two readings, and two readings is what makes the user ask for a re-tell.
    - **Banned repairs:** *"my bad question"*, *"ignore the wording"*, *"what I meant was"*, *"let me rephrase"*, *"in other words"*, *"to put it better"*, *"or rather"*, *"actually"*, *"correction:"*. The reader must never compare two versions of the same sentence. If a sentence is wrong, **delete it before sending** and keep only the clean one.
    - **An apology is not an edit.** Never confess a badly written sentence in order to keep it. Sentence wrong → removed, silently.
    - **Re-read once before sending.** Any sentence that would need a second pass gets rewritten at that moment, not annotated later.
    - **A question must be answerable.** One sentence, one kind of answer, no pronoun pointing three paragraphs back. If the user replies *"what do you mean?"*, the question was the defect — delete it, state the fact plainly, and re-ask properly or not at all.
    - **Never smuggle the answer to your own question into a dash or a footnote** (rule 19 already forbids it). *"Clumsy question — take the answer instead"* is that same mistake wearing a confession.
    - **State the thing, not the story about the thing.** Not *"I asked this badly, what I was trying to say is…"* but simply the fact: *"After the push GitHub runs nothing. No check appears."*

23. **EVERY GITHUB EXPLANATION CARRIES A PICTURE (user request, rev 31).** The user's request, verbatim: *"Next time when you are explaining me anything related to github, create diagrams so that it is easier to learn and understand."* Git speaks in lines of text about positions nothing on screen shows; the picture is what makes a position visible.
    - **What counts as GitHub material:** branches, commits, push/pull/fetch, pull requests, merges (fast-forward, squash, rebase), conflicts, CI checks, tags, branch protection, deleted branches. If the sentence names a remote branch, the reply gets a picture.
    - **ASCII inside a plain fence**, the style already used in `git-workflow.md` §1 — never mermaid. Commits are short labels, branch names sit on the left, the arrow into `master` is the merge, the remote's state is on the right. A plain fence renders identically in chat, in the terminal and on GitHub.
    - **Draw the user's own commits, not a generic example.** `86c9f58`, `fa43faf`, `docs/flag@{1}` — the names that just scrolled past in their terminal. A generic `A → B → C` teaches nothing they can verify.
    - **Before and after are two pictures.** When the point is movement (rebuild, squash, force-push), draw the state before and the state after; one picture cannot show a move.
    - **The picture never replaces the sentence.** The sentence says what happened; the picture shows where. Both, sentence first.
    - **The same picture goes into the document** when the document explains the same mechanics, so doc and chat cannot drift apart.
    - **The user has confirmed the style works (rev 34), so it is now unconditional.** Verbatim: *"use a similar diagram you used before to explain me git the next time… make a note of this in the `resources/docs/plan.md`. its easire to understand git this way."* Every git/GitHub step therefore draws the tree — **including the steps where nothing moved**, because "nothing moved" is itself half the lesson. When the tree is not the whole story, the picture grows boxes (`(work)`, `(stage)`, `up/main`, a PR box) rather than the prose growing paragraphs.
    - **A dedicated recap session is planned and documented:** `resources/docs/git_recap.md` — a sandbox of two clones and four plain text files, five stages from "first commit" to "the secret was committed", the diagram required at every step, the questions to ask, and a coverage checklist. Run it when the user asks; never start it unprompted.

24. **ASK QUESTIONS A BEGINNER CAN ACTUALLY ANSWER (user request, rev 36).** The user's verdict, verbatim: *"You have to assume that i do not know these technologies and ask me questions accordingly."* Rule 19 requires 2–3 questions per step; this rule defines what a legal question looks like when the person answering has **never used the technology**. He is learning Java and its ecosystem from scratch — every term is new until proven otherwise.
    - **Never require the vocabulary to answer.** *"Which filter produces the 401?"* can only be answered by someone who already knows the filter names — which is the thing being taught. Rewrite it as behaviour: *"A request arrives with no token. Does it reach the controller, or is it stopped before that?"*
    - **Ask about something observable:** a log line, a port, a file, a status code, a row count, an exception message. The best questions are answered by the user's own run, not by memory of my last paragraph.
    - **Give the situation inside the question.** One sentence of setup, then one question mark. No pronouns pointing three paragraphs back, no "in that case".
    - **One ask per sentence.** Never join two questions with "and" — the second one is silently dropped, and then the step is judged failed for a reason the user never saw.
    - **Do not ask "why" before "what happens".** *"What happens"* is answerable from experience; *"why"* needs the model to exist first.
    - **Never ask whose answer is a term not yet taught** — not even one that appears in a comment he has not read.
    - **A clarification request means the question was the defect** (rule 22). Replace it with a **statement of fact** plus a simpler question. Never re-ask the same shape.
    - **Name the class and the method — never "the last test" or "this one" (rev 42).** The user's reply, verbatim: *"which last test? which method failed to do what? when you ask questions, make sure you name the methods and the class. The ambigious language in question are difficult to understand."* A positional reference ("the last test") makes the reader scan a file to discover what is being asked, and the scan is where understanding is lost. Write the names: **"In `UserLockoutTest.expiredDeadlineStartsAFreshRun`, the counter came back as 2 instead of 1 — what step did `User.recordFailedLogin` skip?"** The same applies to code the question is *about*, not only to the question's subject.
    - **Worked example — the failure.** Asked: *"You start identity now and open `http://localhost:8082/` in a browser. What status do you get, and where is the password you would need?"* The user's reply: *"I did not understand the question."* Two faults: it assumed he knew that security generates a user, and it asked two things at once. A legal version: *"Open `http://localhost:8082/` in the browser. Is there a page, or a refusal?"* — then explain what the refusal means.

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

**Tier 2 — optional additions (pending the user's yes/no):** load shedding, priority queues and `Retry-After` under overload (Day 10C) · runtime feature flags as a real component (Day 18) · saga *orchestration* (state machine) instead of pure choreography (Day 14). *(Read replica + replication-lag handling was promoted from Tier 2 to a firm task on Day 17.3 — user request, rev 17.)*

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
| **Developer tooling** | Git + Conventional Commits · **IntelliJ IDEA** (run and debug every service) · **Postman** (all HTTP requests, Basic Auth on the Authorization tab) · **DBeaver** (database inspection and ad-hoc SQL) · Maven Wrapper |

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
- **API compatibility (rev 37):** public and internal HTTP contracts are **additive by default**. A field is never renamed, repurposed or removed without a deprecation window; a version (`/api/v1/...`) appears in the path only when a breaking change is unavoidable, and it is added at the gateway, not invented per service. Breaking = removing/renaming a field, changing a type or a status code, tightening validation, or changing default sort/pagination behaviour. Every response documents what it promises; nothing documents what it happens to return today.
- **Packaging (rev 39): feature first, layers inside.** Each service is one bounded context; inside it the layers are `api` · `application` · `domain` · `infra`. Domain types live together (`User`, `UserStatus`, `UserRole`, `EmailAlreadyUsedException`) because they change for the same reason, and a *business* exception is a business rule — unlike a `PaymentGatewayTimeout`, which is an accident of one adapter and belongs in `infra`. **Split a package only when** it passes ~10–15 types, or when two unrelated reasons-to-change appear in it (then split by aggregate: `domain/user`, `domain/token`). **Never** create `entity/`, `exception/`, `enums/` folders merely to group by kind: at this size they add navigation cost and separate rules from the data they govern.

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
- [x] 1.3 GitHub Actions smoke pipeline (`ci.yml`: checkout → JDK 21 → `mvnw verify`), branch protection, required checks — **done**; first run on `master` succeeded ([run 42940713385](https://github.com/electronik54/TW_systemArch/actions/runs/42940713385)). Branch protection was enabled only on 2026-11-13 (rev 32): a `master` ruleset requiring `Build and test (Java 21)`
- [x] 1.4 `libs/exr-common`: RFC 7807 error model + correlation-id filter — **done**: `CorrelationIdFilter` (sanitised, MDC, `finally` cleanup), `ProblemSupport` + `GlobalExceptionHandler` (RFC 7807, no internal leakage, Spring statuses preserved), `ExrCommonAutoConfiguration` + `AutoConfiguration.imports`. **11 unit tests green.** Remaining: `@WebMvcTest` coverage of the handler belongs to Day 3
- *Exit test:* ✅ **MET** — `./mvnw verify` is green locally **and** in Actions (run 42940713385), and the user can explain why gates run on **both** PR and push, not just one (see section 11.4).

### Phase 1 — Domain, data, first service
**DAY 2 — Domain modelling + PostgreSQL + Hibernate**
- [ ] 2.1 Bounded contexts and ownership: which service owns which table; why no cross-service joins
- [ ] 2.2 ERD: events, sources, subscriptions, users, roles, audit, outbox, notification log (+ indexes and why)
- [x] 2.3 Flyway migrations (`V1__catalog_init.sql`), naming conventions, forward-only migrations, zero-downtime rules — **DONE**: migration executed successfully against PostgreSQL 16 (Docker `postgres:16`, database `catalog`). Result: **4 tables, 13 explicit + 6 implicit = 19 indexes**. The partial unique index was proven live — a duplicate live subscription raised `duplicate key value violates unique constraint "subscriptions_active_uk"`, and resubscribing after `ended_at` was set succeeded. **Note:** this proved the *SQL*, not *Flyway itself* — the history table/checksum/ordering path is exercised when the Spring app first starts (2.4)
- [x] 2.4 JPA mapping with Hibernate 6: records vs entities, `@Version`, `@Embedded`, avoid N+1 (`@EntityGraph`) — **2.4a ✅** the app boots and the startup order is confirmed in the log · **2.4b ✅** `Source` + `ExpiryPolicy` mapped, proven by `ddl-auto: validate` · **2.4c ✅ all 33 columns of `events` mapped** — identity, `source_id` as `@ManyToOne(LAZY)`, self-referencing `parent_event_id`, `LocalDate` + `release_precision`, the `text[]` arrays (`genres`, `platforms`, `overridden_fields`) via `@JdbcTypeCode(ARRAY)`, `@Version` optimistic locking, `@PrePersist`/`@PreUpdate` timestamps, the purge/availability fields and the editorial overrides (`COALESCE(override, sourced)`). Proven by `EventMappingTest`, 2 tests: "a foreign key is the other row, not a number" and "arrays, version and timestamps round-trip" (`version` 0 → 1; the INSERT lists all 33 columns, the UPDATE ends `where id=? and version=?`). **Lesson:** a `char(n)` column needs BOTH `columnDefinition = "char(n)"` **and** `@JdbcTypeCode(SqlTypes.CHAR)` — the definition fixes only the type *name*, while a Java `String` still binds as `Types#VARCHAR`, so `validate` fails with "found [bpchar (Types#CHAR)], expecting [char(2) (Types#VARCHAR)]". **2.4d ✅ N+1 removed at the query** — `EventRepository` (new, package `infra`) declares `findByCategory` and `@EntityGraph(attributePaths = "source") findWithSourceByCategory`; `EventFetchingTest` seeds 3 events from 3 different sources and counts Hibernate prepared statements. The user's own run printed **4** without the graph (`1 for the events + 3 for their sources`) and **1** with it. **Lessons:** (a) `@EntityGraph` is only useful against a *lazy* association — over an `EAGER` ManyToOne it changes nothing; (b) the count becomes 1 + N only when each row has a *different* supplier, because the persistence context caches a supplier by id, so shared suppliers hide the problem. Still open inside 2.4: `@Embedded` — parked deliberately, no column group in V1 needs one yet
- [x] 2.5 Testcontainers integration test for repositories — **DONE**: new `AbstractIntegrationTest` holds a **static** `PostgreSQLContainer` on `postgres:16`, starts it in a static block (it must be running before Spring resolves a datasource URL), and publishes url/user/password via `@DynamicPropertySource`; the four persistence tests extend it. **Proven by stopping `exr-pg` first** — the suite still passed, printing `jdbc:postgresql://localhost:<random>/catalog`. **Lessons:** (a) the schema still comes from **Flyway**, not Hibernate — `ddl-auto: validate` only compares entities against V1-built tables, and Flyway checksums turn on-disk drift into a startup failure; (b) the container is per **JVM**, so all test classes share one database and Spring's context cache means Flyway runs once; (c) `application.yml` was left untouched — the override is test-scoped. **Environment trap worth remembering:** Docker Engine 29 rejects any API version below its minimum with a bare **400**, and the docker-java client under Testcontainers asks for **1.32**, so Testcontainers reported *"Could not find a valid Docker environment"* even with Docker healthy and `docker ps` working. Diagnosed by speaking HTTP to the named pipe directly (`/v1.32/info` → 400, `/v1.44/info` → 200), fixed by pinning `api.version=1.44` for the surefire-forked JVM in the root `pom.xml`. **Open follow-up:** `.github/workflows/ci.yml` still starts a `postgres:16` **service container**, which these tests no longer need
- [x] 2.6 Full-text search in Postgres: `tsvector` + GIN index vs `LIKE`/trigram vs OpenSearch — **DONE, chose Postgres `tsvector` + GIN**. `V2__event_search.sql` adds `events.search_vector` as `GENERATED ALWAYS AS (...) STORED` — the effective title under `simple` at weight `A` (so "It" is not thrown away as a stopword), the synopsis under `english` at weight `B` (so "run" finds "running") — and then `events_search_gin`. `EventSearchRepository` (package `infra`) reads it through `NamedParameterJdbcTemplate`, not JPA: Hibernate has no Java type for `tsvector` and JPQL cannot call `ts_rank`, so a custom `UserType` would be machinery for three columns; the entity keeps mapping only what a human writes, and `ddl-auto: validate` stays honest. `EventSearchTest` proves four things: a title hit outranks a synopsis hit, "It" is still findable, a blank term costs nothing, and `EXPLAIN` names `events_search_gin`. **Lessons:** (a) the two-argument `to_tsvector('cfg', text)` is mandatory — the one-argument form reads a run-time setting, is therefore STABLE and not IMMUTABLE, and the migration dies with *"generation expression is not immutable"*; (b) the query must be phrased in the same config as the document — a `simple` query for "dunes" is `'dunes'` and misses the stored `'dune'`, which is exactly why the `WHERE` carries two `tsquery`s ORed together and the score is `greatest(ts_rank(...simple), ts_rank(...english))`; (c) the weight **multiplies** the rank linearly — the run printed `0.24317084 = 0.6079271 × 0.4`, so `setweight` alone produced the ordering, not word frequency; (d) a GIN index is never chosen on a table of three rows, so the test sets `enable_seqscan = off` to ask the only useful question — *could* the index answer this?; (e) `V1` stays frozen and `FlywayMigrationTest` holds the new expected list, which is where "a new migration changes what the schema means" becomes visible. **Deliberately not done:** no HTTP endpoint (search is a query, not a feature yet), no typo tolerance (`pg_trgm` would be a second index for a different question), and no cursor pagination — `ts_rank` computes over surviving rows, so an index can never stream relevance order.
- [~] 2.7 **`@Transactional` in depth** *(user request)*: where the boundary belongs (service layer, not controller), propagation (`REQUIRED` vs `REQUIRES_NEW`), rollback rules (runtime vs checked exceptions), isolation levels, and what a transaction does **not** protect you from (lost updates) — **steps 1–3 done, step 4 parked until Day 8 (rev 35 → 45)**: **step 1 ✅** `catalog/application/EventService.importAll` is the first transaction boundary in the project, and `EventServiceTransactionTest` proves it by watching the table from a **different connection** (`JdbcTemplate`), deliberately **without** `@Transactional` on the class so the service really commits; `@AfterEach` deletes its own rows because the free rollback is gone. **Lessons:** (a) `persist` sends nothing — the INSERT appears at the flush inside commit, so a clash surfaces *after* the method returns (`DataIntegrityViolationException` on `events_source_external_uk`, observed); (b) an `EntityManager` count would answer from the persistence context and hide a missing rollback — the observer must be a separate connection; (c) Spring's **test** listener rolls back by *calling* rollback, not by throwing, which is why `@Commit`/`@Rollback(false)` can override it; (d) the annotation acts only through the **proxy**, so `this.other()` inside the same bean gets no transaction; (e) **`REQUIRED`** (default) joins the caller's transaction — the inner work dies with the outer rollback; **`REQUIRES_NEW`** opens a second transaction and costs a **second pooled connection**, so 10 concurrent users on that path exhaust a 10-connection pool and the *tenth* user gets a 500 nobody caused. **Steps 2 and 3 ✅ (rev 44–45, both proven on the user's machine).** `V3__import_audit.sql` adds `import_attempts` (no index — a handful of rows a day; `error` truncated by the application so a Postgres message cannot leak purged text), because a *column* on `sources` keeps only the newest failure while a *table* keeps one row per attempt. `ImportAttempt`, `ImportAuditService` (`Propagation.REQUIRES_NEW`) and the `entityManager.flush()` inside `try` are in place, and `EventServiceTransactionTest` proves it: 2 tests green, with `insert into import_attempts …` printed **after** the `events_source_external_uk` violation — the audit row committed while the import was failing. Then **step 3** got its own proof, `CheckedExceptionCommitTest`: `BoomService.plain` (plain `@Transactional`) throws an `IOException` and the row **stays**; `BoomService.rollingBack` carries `rollbackFor = Exception.class` and the same exception leaves **nothing**. 2 tests green, both counted through `JdbcTemplate` on a second connection. **Two lessons from it:** (a) a bean registered through `@TestConfiguration` + `@Import` gets the transaction proxy like any other bean, which is how a *fake* service can demonstrate a real rule; (b) removing `rollbackFor` does **not** remove the transaction — it removes one attribute, and with it the "undo on a checked exception" instruction, so `count` returns 1 and the assertion fails with `expected: 0 but was: 1`. A missing rule is not a missing transaction. **Step 4 parked by the user's decision (rev 45):** isolation levels, the lost update and the tie-in to `@Version` — the two-connection picture was explained, the demo was not built, and the item resumes at **Day 8**, where the quota race is the same bug wearing different clothes
- [ ] 2.8 **Sub-events / hierarchy**: `events.parent_event_id` (self-reference) for series→season→episode and tour→date; `subscriptions.subscription_scope` (`THIS_EVENT` / `THIS_AND_FUTURE` / `INCLUDE_CHILDREN`); tier inheritance from the parent with a per-child override; recursive query for descendants; and "contained in" vs "related" (a sequel is *not* a child) — see `resources/docs/catalog/schema.md` §12
- [ ] 2.9 **Lifecycle & archiving**: `events.active_until` (release + grace for one-offs; last child + grace for containers; `now()` for cancelled). Archived events are never polled and refuse new subscriptions, and an admin's `HOT` marking is **capped** so it cannot outlive it. Plus the `event_reports` table (user flags an event + comment → admin queue ordered by report count) — see `resources/docs/catalog/schema.md` §13 and §14
- *Exit test:* user can justify each index and explain why the catalogue is not `JOIN`ed to users.

**DAY 3 — Catalog service REST API**
- [ ] 3.1 Controller/domain/infra layering, DTO records, explicit mappers
- [ ] 3.2 Search & filtering: page vs cursor pagination (decision), sort whitelisting, SQL injection safety
- [ ] 3.3 Bean validation, `ProblemDetail` error contract, `@ControllerAdvice`
- [ ] 3.4 OpenAPI via springdoc + `@WebMvcTest` slice tests
- [ ] 3.5 **API versioning & backward compatibility** *(user request, rev 37)*: the policy in one page — **additive first** (new field, new endpoint, never a renamed or repurposed one), `@deprecated` in OpenAPI with a removal date, and a version in the path (`/api/v1/...`) added **at the edge** only when a genuinely breaking change is unavoidable. Plus: what counts as breaking (removing a field, changing its type, changing a status code, tightening validation, changing sort order), how long a version is supported after its successor ships, how the gateway routes two versions at once during the overlap, and why internal service-to-service contracts version differently from the public one (one consumer we deploy ourselves vs thousands we do not)
- *Exit test:* API contract in Swagger UI; user can defend the pagination choice.

### Phase 2 — Identity, security, edge
**DAY 4 — Spring Security fundamentals in the identity service**
- [x] 4.1 Users, credentials, BCrypt/Argon2, account states (enabled/locked/suspended) — **ticked with 4.3 (rev 44)**: the three states are mapped in `UserStatus`, enforced through `DbUserDetailsService` (`SUSPENDED` → `disabled`, `LOCKED` → `accountLocked`), and `ENABLED` + a live deadline was exercised on a real row
- [~] 4.2 `SecurityFilterChain` for a stateless API, CSRF rationale, CORS vs CSRF — **IN PROGRESS (rev 38)**: `identity/infra/SecurityConfig` replaced Boot's default chain with a readable one (CSRF off **with the reason** — stateless, no cookie, and the condition that would make it a vulnerability; no sessions; `/api/public/**` permitted; everything else authenticated; HTTP Basic temporarily until Day 5). `IdentityController` proves the policy with one open and one protected endpoint. **A real gap was found by testing, then fixed:** an allowed request carried `X-Correlation-Id`, a **refused** one did not — `CorrelationIdFilter` was registered at `LOWEST_PRECEDENCE` while the security chain sits at `-100`, so security answered first and our filter never ran; and that 401 had an **empty** body. Fix: the filter now implements `Ordered` → `HIGHEST_PRECEDENCE`, and `RestAuthenticationEntryPoint` writes our RFC 7807 body with the id. Verified by `curl`: `401`, `application/problem+json`, header and body ids identical. **Second finding, worth an interview answer:** a `permitAll()` path still returns 401 when a client sends an **invalid** credential — `BasicAuthenticationFilter` authenticates *before* the authorization rule is consulted, so "you may skip credentials" is not "any credential will do". **Still open in 4.2:** CORS vs CSRF in practice (CORS belongs at the gateway, Day 7)
- [x] 4.3 Registration/login endpoints, password policy, brute-force lockout — **DONE (rev 44), verified end to end**: `V2__login_lockout.sql` adds `failed_attempts` + `locked_until` (a **deadline, not a flag**); `User.recordFailedLogin` / `recordSuccessfulLogin` / `isTemporarilyLocked` hold the rule; `LoginAttemptService` (`MAX_ATTEMPTS` 5, `LOCK_FOR` 15 min, `@Transactional`) does the write; `AuthenticationEventsListener` counts **only** `AuthenticationFailureBadCredentialsEvent`, so a locked account's further knocks change nothing; `DbUserDetailsService` marks a `UserDetails` locked when `status = LOCKED` **or** the deadline is still in the future. Verified in the user's own tools (2026-11-17): five wrong passwords → `failed_attempts = 5`, `locked_until` in the future, `status` still `ENABLED`; then the **correct** password → `401` with the counter still `5`, which is the ordering proof — the flag is read before the password is compared, and a locked attempt is never counted. **One lesson from the first start:** `V2` was applied, then Hibernate's `validate` refused to boot — *"wrong column type encountered in column [failed_attempts] … found [int2 (Types#SMALLINT)], but expecting [integer (Types#INTEGER)]"*. `validate` compares the **JDBC type code**, so the Java field moved to `short` (it maps to `SMALLINT` natively) instead of the migration: editing an applied migration only trades the error for a Flyway checksum mismatch. Same family as 2.4c's `char(2)` finding. **First manual run (Postman, 2026-11-17):** five wrong passwords produced five identical `401`s, each carrying our RFC 7807 body and a `correlationId` — so the refusal shape from 4.2 still holds on the login path. **The write is proven** (rev 44, above). If a future run shows `0`, there are two causes only: the email typed in Postman matched no row (`recordFailure` is a no-op for an unknown address), or the failure event never reached the listener. **Where the account state is read (added rev 41):** exactly one place — `DbUserDetailsService` lines 75–77, where `SUSPENDED` becomes `disabled(true)` and `LOCKED` becomes `accountLocked(true)`. **`UserRepository.findByEmail` carries no `status` filter** — a suspended or locked row comes back exactly like any other, and the refusal comes from the flag on the converted `UserDetails` (`DisabledException` / `LockedException`), never from an empty result. "No such user" and "this user may not log in" are two different answers. Two consequences worth remembering: `User.isActive()` (lines 144–146) has **no caller** yet, so it is dead code kept for the future admin screens; and nothing in the codebase sets `status` (only `setStatus`, also uncalled), so a `SUSPENDED` or `LOCKED` row can only come from an admin endpoint that does not exist yet, or from DBeaver during a lab.
  - 📄 **Reference page: [`HowIsUserUpdatedOnLoginAttempt.md`](HowIsUserUpdatedOnLoginAttempt.md)** — the whole flow in one page: the cast of classes, a flowchart for a wrong password, a flowchart for a correct password on a **locked** account (nothing is written; the password is never compared), how the counter resets, the row moment-by-moment, why it is driven by an **event** rather than a controller call, what lockout does **not** stop (credential stuffing), and a method → line-number quick-reference table. Read it when the flow slips out of memory. **Rev 41:** §12 adds the **request-level** half — the filter stack a request walks, the inside of `BasicAuthenticationFilter`, and the four consequences — and the module's first test, `UserLockoutTest` (4 tests, **green in the user's own run — `Tests run: 4, Failures: 0`**), covers the lockout arithmetic with no database at all.
- *Exit test:* user can explain stateless vs session auth and why CSRF is disabled.

**DAY 5 — OAuth2 + JWT (access & refresh)**
- [x] 5.1 JWT anatomy, signing (RSA vs HMAC), claims design, `jti`, `aud`, `iss`, expiry — **done (rev 45)**: `TokenService` mints `sub` (the user **id**, not the email), `email`, `roles`, `iss`, `aud`, `iat`, `exp`, `jti`; `TokenServiceTest` proves the claims read back without the key and that one flipped payload character breaks the signature. The user's own token decoded to exactly those seven claims, with `exp - iat = 900` s = `ACCESS_TOKEN_TTL`
- [~] 5.2 Access token (short) + refresh token with **rotation** and reuse detection; token families — **access token half done (rev 45)**: `POST /api/auth/login` authenticates through the *same* `AuthenticationManager` the Basic filter uses (so 4.3's account-state check, BCrypt comparison, failure counter and lockout all still apply, and the reused path was proven by both a `401 invalid-credentials` body and a `200` token), then `LoginService` reads the row for `sub` and mints the token. **Still open:** refresh token, rotation, reuse detection, and the `refresh_tokens` table they need
- [~] 5.3 Resource server config, JWKS endpoint, key rotation, `JwtDecoder` — **local verification done and proven (rev 45)**: `catalog-service` gained a `SecurityConfig` (stateless, `/api/public/**` open, everything else authenticated, bearer filter) and a `JwtDecoder` that checks signature → issuer/expiry → **audience**, in that order. `GET /api/catalog/whoami` returned `sub`, `email`, `roles`, `audience` and `issuer` **with no call to `identity` and no database read**. **Still open:** JWKS endpoint and key rotation, which only become real when signing moves to RSA (7.6); and a refusal here is still Spring's bodyless 401, because `RestAuthenticationEntryPoint` lives in identity and should move to `exr-common` on Day 7. **Trap recorded:** pasting the token *with its surrounding JSON quotes* produces a 401 that looks like a validation failure — the `WWW-Authenticate` header is what distinguishes the two, and the fix was to paste only the `eyJ…` value
- [x] 5.4 `@PreAuthorize`, method security, role hierarchy `GUEST < FREE_USER < PAID_USER < ADMIN < DEVELOPER` — **DONE for the mechanism (rev 46)**: `@EnableMethodSecurity` on catalog's `SecurityConfig`, and `GET /api/catalog/admin/ping` answers **403** for a token holding `ROLE_SERVICE`. **The 403 cost one fix:** the endpoint first answered **500**, because `exr-common`'s `@ExceptionHandler(Exception.class)` caught `AccessDeniedException` inside the servlet before Spring Security could translate it — a shared advice is a catch-all, and a catch-all is consulted before the framework's own mapping unless a higher-precedence one claims the exception first, which is what catalog's `ApiExceptionHandler` (`@Order(HIGHEST_PRECEDENCE)`) now does. **Still open:** the role **hierarchy** itself — no `RoleHierarchy` bean yet, so `ADMIN` implies nothing beyond itself and the chain in this line is documentation, not behaviour.
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
- [~] 7.6 **East-west security**: how `catalog` authenticates a call from `identity` — propagated user JWT vs client-credentials service tokens vs mTLS; trust boundary, audience/`aud` claim per service *(H1)* — **this is the answer to the user's rev-45 question "is authorization needed for inter-service communication too?"** Yes: authentication ("who is calling") and authorization ("may this caller do this") both apply east-west, and they are a *separate mechanism* from the user's login. Prerequisites in order: **5.2** (issue an access token) → **5.3** (a service validates one) → **7.1–7.2** (gateway routes and validates at the edge) → **7.6** (identity travels inward). The size check is **5.4** (`@PreAuthorize`). **Both halves built and proven locally (rev 46)**: (a) *forward the user* — `catalog` accepts a user's token and `whoami` echoes `sub`/`roles`, so identity travels inward with no call back to identity and no database read; (b) *a service's own identity* — `POST /api/auth/service-token` takes `X-Service-Key` and mints a 5-minute token whose `sub` is the **service name** and whose `roles` is `["ROLE_SERVICE"]` (`LoginService.issueServiceToken`: no `AuthenticationManager`, no user row, because nobody is involved). **What the four calls proved, in the user's own Postman:** minted → `200`; `whoami` → `200` with `sub: catalog-service`; `admin/ping` → **403**; wrong key → **401**, the same body a wrong password gets. **The sentence that separates the two ideas:** `aud` gates *which* service may present a token (`This token was not minted for audience …`), `roles` gate *what* it may do — the service token passed the first gate and failed the second. **Still open:** one **shared** key means "some trusted client", not "catalog specifically" (per-service keys with overlap windows are the fix); HMAC still means every verifier could also mint (RSA/JWKS); no mTLS; **no gateway**, so every service still listens directly on its own port
- [ ] 7.7 **Service discovery**: ECS Cloud Map vs EKS CoreDNS vs injected env URLs; why hardcoded hosts break blue/green *(H2)*
- *Exit test:* user can explain where rate limiting belongs and why not in each service.

### Phase 3 — Subscriptions, caching, ingestion
**DAY 8 — Subscriptions + entitlements + concurrency**
- [ ] 8.1 Where subscriptions live (open question #2) — decision + consequences
- [ ] 8.2 Quota enforcement that is actually atomic: DB constraint / `SELECT ... FOR UPDATE` / Redis counter (pick one, justify)
- [ ] 8.3 Idempotent subscribe (idempotency key), unique constraint, optimistic locking, retry safety
- [ ] 8.5 **Prove the bug before fixing it** *(user request)*: write a failing concurrent test that reproduces a lost update / quota overrun against the naive implementation, then fix it and watch the same test go green
- [ ] 8.4 Entitlement check: synchronous call vs tier claim in JWT vs cached entitlement (open question #3)
- *Exit test:* user can explain how two parallel requests cannot both take the last free slot.

**DAY 9 — Redis caching**
- [ ] 9.1 Cache-aside vs write-through vs write-behind; what is safe to cache and what is not
- [ ] 9.2 Key design, TTL strategy, source retention vs cache TTL, per-source TTL config
- [ ] 9.3 Stampede / thundering herd: lock, single-flight, jittered TTL, stale-while-revalidate — plus **freshness tiers** (`HOT` 5–15 min / `WARM` 1 h / `COLD` 1 day) with automatic promotion and decay, so churning events stay fresh without polling the dead ones
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
- [ ] 10B.6 **Cheap freshness checks**: `ETag`/`If-None-Match` and `Last-Modified`/`If-Modified-Since` where the source supports them, falling back to full fetch + `details_hash` comparison. **A `304` still costs a request against the rate limit** — it saves bandwidth, not quota (`resources/docs/catalog/schema.md` §11)
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
- [ ] 13.1 Reminder scheduling: cron vs delayed message vs scheduler + "due" table (survives restarts) — **and: the reminder must be computed at subscribe time, never at firing time**, so an expired/purged event cannot silently cancel it (`resources/docs/catalog/schema.md` §9)
- [ ] 13.2 Notification preferences, quiet hours, timezone handling, deduplication — plus **churn control**: coalesce several changes in a window into one notice, rate-limit to one date-change notice per event per 24 h, and notify only when the change is *significant* (date moves ≥ 1 day, or status changes)
- [ ] 13.3 Email via SES/SMTP + templating; in-app notification centre
- [ ] 13.4 Calendar: `.ics` generation, `VEVENT`/`VALARM`, feed URL design, ETag/`If-None-Match`
- [ ] 13.5 Delivery tracking: notification log table, retries, DLQ, provider bounce handling
- [ ] 13.6 **GDPR distributed delete**: a `UserDeleted` event fanning out to catalog/notification/payment, tombstones vs hard delete, anonymised audit trail, and how to prove deletion completed *(H6)*
- [ ] 13.7 **Hierarchy digests**: a series subscription with `INCLUDE_CHILDREN` produces notifications per *child* event; when several children land together (a whole season), send **one digest**, never 24 pings (`resources/docs/catalog/schema.md` §12)
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
- [ ] 17.3 Data: RDS Postgres — **Multi-AZ vs read replica vs both** (availability is *not* the same as read scaling), **replication lag** and what silently breaks when you read from a lagging replica, ElastiCache Redis (replication, failover), S3
- [ ] 17.4 Compute: ECS Fargate (service, task def, autoscaling on CPU/queue) vs EKS (node groups, HPA, IRSA) — pick the default and defend it
- [ ] 17.5 IAM least privilege, GitHub OIDC role, ECR, cost guardrails
- [ ] 17.6 **RPO/RTO definition + a real restore drill**: define targets, restore RDS from a snapshot into a scratch instance, time it, and document the result *(H9)*
- [ ] 17.7 **Disaster recovery strategies** — the ladder: backup-and-restore → pilot light → warm standby → multi-site active/active. RPO vs RTO maths for each, what each costs, and **cross-region snapshot copy** (the only thing that survives losing a region). Includes the AZ-loss drill from 20.3, done rather than read
- [ ] 17.8 **Secrets management** — AWS Secrets Manager vs SSM Parameter Store (cost/rotation/limits), automatic rotation, **IAM database authentication** (no password at all), why secrets must never sit in an image/env file/`application.yml`, how the task gets them (execution role), keeping Terraform state free of secrets, local dev with a git-ignored `.env`, and secret scanning in CI (18.2). Reference: `resources/docs/catalog/schema.md` §15
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
| 25 | *(proposed: **reactive lab** — build the same endpoint twice, JPA + Spring MVC vs R2DBC + WebFlux, then compare throughput, complexity and error handling under load)* | pending user decision |
| 26 | **Git & GitHub recap for a large team** — planned in `resources/docs/git_recap.md`: a sandbox of two clones and four plain text files, five stages (one engineer → branches → two engineers and a conflict → the rules a large team adds → when it goes wrong), a tree diagram at every step, 2–3 questions before the next step | planned (rev 34) — run when the user asks |

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
| D-013 | Day 2 | **Retention purges the *details*, never the whole row.** `events` is split into a skeleton (`id`, `source`, `external_id`, `title`, `release_date`, `status`) kept indefinitely and details that expire. Expired rows become `availability='STALE'` and are **re-hydrated on demand** from the source, asynchronously, behind a single-flight lock. Per-source `expiry_policy` (`KEEP_STUB` / `DELETE_ALL`) because the licence decides, not us | user's idea. It fixes three problems at once: deletions no longer silently destroy subscriptions, autocomplete/history keep working, and we only pay for upstream calls on items someone actually looks at. Cost: two-state records, a re-hydration path, and a per-source legal check | yes — but the `availability` column and re-hydration path would have to be added anyway |
| D-014 | Day 2 | **Per-service schema docs:** every service gets `resources/docs/<service>/schema.md` (plain language, every column + constraint + reason). The chat is not the durable record | user request; avoids re-explaining the schema and survives context loss | yes |
| D-015 | Day 2 | **Editorial override layer is IN** (`catalog/schema.md` §10): admin-authored `title_override` / `release_date_override` + `overridden_fields` precedence marker. The purge never touches them; source sync never overwrites an overridden field. Plus a **"verified accurate"** action (cheap editorial confirmation) and the **`event_reports`** crowd-sourced flag (§14). Copy-paste blocking is an admin-UX choice only — it is recorded as **not** providing legal protection; the legal shield is `expiry_policy` | user decision. It makes expiry survivable: after expiry the event still shows a title and date, because those are *our* record rather than the supplier's | yes |
| D-016 | Day 2 | **Database hosting:** local = Postgres in Docker Compose · CI = Testcontainers · dev/staging = RDS `db.t4g.micro` single-AZ · **production = Amazon RDS for PostgreSQL, Multi-AZ, private subnet, PITR, deletion protection**. Mutations never run on app startup — Flyway runs as a **separate one-off task before** the new service tasks start (§15 of `catalog/schema.md`) | managed backups/failover/patching beats self-hosting at this size; the migration ordering avoids the H10 race | yes |
| D-017 | Day 4 | **API compatibility is additive-first; a version is a last resort.** Fields are added, never renamed or repurposed; removals go through a documented deprecation window; `/api/v1/...` is introduced at the **gateway** only when a genuinely breaking change cannot be avoided (day 3.5). Breaking changes listed once, in §6 | user's question (rev 37): "are we also going to version our APIs while maintaining backward compatibility?" The honest engineering answer is that versioning is the exception, not the habit — every version is a second code path and a second support window, so additivity buys the same safety for free. Internal east-west contracts version differently because we deploy both sides | yes — but a published `/v1` cannot be withdrawn once third parties depend on it |

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
| rev 14 | **Workflow change: the user now runs every git command** (rule 10 rewritten — the agent only proposes the command and explains why). Recorded a re-teach obligation for `CorrelationIdFilter` / `ProblemSupport` **including the Spring Security filter chain**. Added `@Transactional` in depth (2.7), "prove the concurrency bug before fixing it" (8.5), and a proposed reactive lab (Day 25) |
| rev 15 | Rewrote `need.txt` as a **COVERED / NOT COVERED** map derived from the resume, with Q1–Q4 answered inside it; added **`index.md`** as the documentation entry point and repointed `README.md` and rule 1 at it; replaced the stale "zero code written" status block |
| rev 16 | **Two new working rules:** 14 = explain in **plain language first**, jargon in brackets afterwards (this is now the default style); 15 = every service gets `resources/docs/<service>/schema.md` with every column, type and constraint, and the schema is **not** re-explained in chat. Added **D-013** (retention purges *details*, keeps the *skeleton*, re-hydrates on demand) and **D-014** (per-service schema docs). Created `resources/docs/catalog/schema.md` |
| rev 17 | User asked whether read replicas, migrations, DR and secrets are covered — audit found two gaps. **Promoted read replicas from Tier 2 to a firm task (17.3)**, added **17.7 (DR strategy ladder + cross-region)** and **17.8 (secrets management)**. Also recorded D-015 (override layer accepted) and D-016 (database hosting per environment) |
| rev 18 | Added **rule 16 — announce the technology at the start of every step** (the user could not tell what each file was teaching). Created `services/catalog-service` (module + `dependencyManagement` in the parent) and wrote **`V1__catalog_init.sql`** — 4 tables, 13 explicit indexes + 6 implicit = 19. Renamed `subscriptions.unsubscribed_at` → `ended_at` + `end_reason` while writing the SQL, and synced `schema.md` |
| rev 19 | **Flyway executed and verified end-to-end.** (a) Raw SQL via `psql` against `postgres:16`: 4 tables, 19 indexes, and the partial index proven to reject a duplicate live subscription and allow a resubscribe. (b) Through **Spring Boot**: `flyway_schema_history` created, V1 recorded with checksum `189898812`, second run = *"up to date, no migration necessary"*. Added the rule 14 bullet *(if the user does not understand — REDUCE, do not deepen)* and **rule 17** *(name the day a future topic belongs to)*. Created `CatalogServiceApplication`, `application.yml`, `FlywayMigrationTest` |
| rev 20 | **Rule 18 — the user runs it and watches it happen:** give the command, wait for their output, then interpret only the 2–3 lines that matter. Refreshed section 0, work status, curriculum state and environment table to match reality (Docker running, `catalog-service` has source, 13 tests) |
| rev 21 | **Rule 19 — check understanding with 2–3 questions at the end of every step, then stop** (never answer them in the same turn; a wrong answer becomes the next step). Day **2.4b**: `ExpiryPolicy` enum + `Source` entity mapped against the real `sources` table, including the decision that the **application** stamps `created_at`/`updated_at` via `@PrePersist`/`@PreUpdate` because V1 creates **no trigger** |
| rev 22 | **Day 2.4c finished** — `Event` maps all **33** `events` columns (`text[]` arrays via `@JdbcTypeCode(ARRAY)`, `@Version`, `@PrePersist`/`@PreUpdate`, overrides) and `EventMappingTest` proves it with 2 tests; reactor total **16 green**. Recorded the `char(n)` lesson: such a column needs BOTH `columnDefinition = "char(n)"` **and** `@JdbcTypeCode(SqlTypes.CHAR)`, or `validate` fails with *"found [bpchar (Types#CHAR)], expecting [char(2) (Types#VARCHAR)]"*. **Three process rules came out of this session — the agent ran the build itself (rule 18 strengthened, violation recorded), explanations were too long and too dense (rule 20), and change locations were never stated (rule 21)** |
| rev 23 | **Day 2.4d — N+1 removed, with numbers** — new `EventRepository` in `infra` (`findByCategory` + `@EntityGraph(attributePaths = "source") findWithSourceByCategory`) and `EventFetchingTest`, which reads Hibernate's `getPrepareStatementCount()`. In the **user's** run: **4** statements without the graph, **1** with it; reactor total **18 green** (11 + 5 + 2). Also recorded why the demo needs *distinct* suppliers per event, and why an `EAGER` association would make the demo meaningless. `@Embedded` parked |
| rev 24 | **Day 2.5 — Testcontainers closed, and a genuine environment bug found by it.** New `AbstractIntegrationTest` (static `PostgreSQLContainer`, `postgres:16`, started in a static block, `@DynamicPropertySource` for url/user/password); `EventMappingTest`, `EventFetchingTest`, `FlywayMigrationTest`, `SourceMappingTest` now extend it; `application.yml` untouched. The first two runs failed with *"Could not find a valid Docker environment"* while `docker ps` worked fine — root cause was **not** networking: Docker Engine **29** answers **400** to any API version below its minimum (1.40), and docker-java asks for **1.32**. Proved by speaking raw HTTP to the named pipe (`/v1.32/info` → 400, `/v1.44/info` → 200 — including the same `Labels:["com.docker.desktop.address=npipe://\\.\pipe\\docker_cli"]` payload seen in testcontainers-java#11235), then fixed by pinning `api.version=1.44` in the root `pom.xml` (property + surefire `systemPropertyVariables`, so CI gets it too, unlike a `~/.docker-java.properties`). Final run with `exr-pg` **stopped**: **18 green** (11 + 7). **Lesson for the interview:** "works in my shell" is not evidence — the CLI and the JVM were using different API versions over the same daemon |
| rev 25 | **CI now declares no database.** On the green run of `788a08c` the `services: postgres` sidecar and the three `EXR_DB_*` env overrides were deleted from `ci.yml`, replaced by a comment explaining that every persistence test extends `AbstractIntegrationTest` and therefore brings its own `postgres:16`. Recorded the one new failure mode this opens: a `@SpringBootTest` that forgets the base class falls back to `localhost:55432`, so it passes locally where `exr-pg` runs and fails in CI. Also recorded the **cmd.exe quoting trap** in rule 10 (newlines are separate commands; `'` is not a quote character) after a multi-line `git commit -m` stored a mangled message |
| rev 26 | **Two-branch model adopted at the user's request** (`development` for day-to-day work, `master` kept releasable), with the tradeoff recorded: two long-lived branches drift, so releases become bigger merges and a hotfix must reach both. `ci.yml` now triggers on `master` **and** `development` for both `push` and `pull_request`, so a PR into `development` is gated exactly like a PR into `master`. Next: a deliberate-failure lab on a throwaway `feature/*` branch — red PR, blocked merge, uploaded surefire report, fix-forward, then merge |
| rev 27 | **Reversed rev 26: trunk-based development, single long-lived branch.** The user asked to follow the recommended model instead of a `development` branch. `ci.yml` triggers on `master` only (for `pull_request` the filter applies to the **base** branch, so every feature branch targeting `master` is still built). Recorded the two disciplines trunk-based needs — short-lived branches and flags for unfinished work — and the payoff: no drift, and a fix never has to be applied twice. Release = tag on `master` |
| rev 28 | **The branching strategy became architecture documentation instead of a chat decision.** New `resources/docs/git-workflow.md`: the trunk-based model with a diagram, branch types and lifetimes, ten rules each with its reason, the exact branch-protection settings to enable on `master`, a red-build table by role (PR / `master` / flaky / conflict), release tagging, Conventional Commits, and the rejected alternatives (GitFlow, long-lived feature branches, direct pushes) with the condition that would reverse each — plus a 30-second interview answer. Linked from `index.md` (read-order, status, layout) and from §0. Also corrected a stale claim: `resources/docs/decisions.md` was described as created in Phase 1 but **does not exist** — the `D-xxx` list in §3 and this log are the interim record. Documentation only; tests unchanged at **18 green** — plus, at the user's challenge, a new **rule 20 bullet**: *a repeat attempt must be measurably smaller, not merely different* (the doc already forbade growing an explanation; it did not say how to verify it, and the agent had been adding tables on re-tell) |
| rev 29 | **The better-on-the-second-try style became the default, and self-repair inside a reply became a rule violation.** The user's challenge: *"when you said `My clumsy question — ignore the wording and take the answer` … You actually do that a lot. Not just while you answer but also when you explain me things. This is why i have to ask you to explain me it again. The style that you use the second time, make that you default style."* Three doc changes: (1) **new rule 22** — *say it right the first time*: banned repairs (*my bad question*, *ignore the wording*, *what I meant was*, *let me rephrase*, *in other words*, *actually*, *correction:*), an apology is not an edit, a question must be answerable, and never smuggle the answer to your own question into a dash; (2) **rule 20 gains a top bullet** — the first attempt is written in the reduced shape (numbered walk-through, one idea per sentence, named subject for every verb, no tables, ~200 words) instead of the shape that used to be a second attempt; (3) **rule 14 gains two bullets** — the bracket around a term is mandatory because the user needs the vocabulary for interviews, and the reduced shape is the default shape. `index.md` §6 now lists five rules, the fifth being plain words first with the term bracketed. Documentation only; code and the 18 tests untouched |
| rev 30 | **The squash-merge incident recorded, with the rebuild that fixed it.** Three commits squashed into `86c9f58`; the next branch was cut from the **old local tip**, so the squash commit was not its ancestor and the last shared commit `6cc7dc2` had no `git-workflow.md` at all — which is why git called it an **add/add conflict** rather than an edit. The rebuild went through the **reflog** (`docs/flag@{1}`), and the three pushes came in the order `upstream does not match` → `non-fast-forward` → `forced update`. **Lesson kept:** a squash merge hides ancestry, so a branch cut from a stale tip conflicts on files it never touched — `git fetch`, then cut from `origin/master` |
| rev 31 | **GitHub explanations now come with a picture.** The user's request, verbatim: *"Next time when you are explaining me anything related to github, create diagrams so that it is easier to learn and understand."* New **rule 23** in §1: any explanation that names a remote branch carries an ASCII diagram in the same reply — plain fence, commits as short labels, branch names on the left, `master` on the right, **the user's own hashes** instead of a generic `A → B → C`; when something moved (rebuild, squash, force-push) the reply carries **two** pictures, before and after, because one picture cannot show a move; the picture never replaces the sentence that says what happened. Mermaid is ruled out on purpose: the identical fence has to render in chat, in the terminal and on GitHub. Propagated to the three places that would otherwise disagree — the §0 resume block, `index.md` §6 (now six rules, the sixth being the diagram) and the header of `git-workflow.md`. Documentation only; code and the 18 tests untouched |
| rev 32 | **The CI gate is enforced, and the lab proved it the hard way.** A `master` **ruleset** now requires **`Build and test (Java 21)`**, restricts deletions, blocks force pushes, and keeps an **empty bypass list** — the admin cannot merge red either. The lab ran out of order, and that is the lesson: PR #3 merged while run **#22** was still going, because nothing was *required*, so `914d4fb` landed and its own push run **#23** failed. The trunk came back through a **revert PR** (#4 → `c315fc3`, −39 lines) instead of a fix-forward, because the file was never wanted. Two traps recorded: GitHub only lists a check as *requireable* **after it has reported once**, and `git branch -d` cannot delete a squash-merged branch (the content is on `master`, the commits are not) — it needs `-D`. Written into `git-workflow.md` §4, §5 and §10. The `labs/squash-merge-lab.md` draft was **dropped at the user's request**; the incident lives in this log instead. Code untouched, 18 tests green, trunk green |
| rev 33 | **Day 2.6 closed — full-text search, and three traps worth remembering.** `V2__event_search.sql` adds `events.search_vector` as a **generated** column (title under `simple` at weight `A`, synopsis under `english` at weight `B`) plus `events_search_gin`; `EventSearchRepository` reads it through `NamedParameterJdbcTemplate` because Hibernate has no `tsvector` type and JPQL cannot call `ts_rank`; `EventSearchTest` proves ranking, the one-word title "It", the blank term and the `EXPLAIN` plan. Traps: (a) the one-argument `to_tsvector` is STABLE, so a generated column rejects it — *"generation expression is not immutable"*; (b) a query must be phrased in the **same config** as the document, or `dunes` never meets `dune`; (c) a weight **multiplies** `ts_rank` — the run printed `0.24317084 = 0.6079271 × 0.4`, which is `B` × one occurrence at `A`. Also learned on the way: `-DfailIfNoTests` is the wrong flag for a `-Dtest=` filter in a reactor (it is `-Dsurefire.failIfNoSpecifiedTests`), JUnit runs methods in a hash order rather than file order, and surefire's console never shows `@DisplayName`. A stray edit to `EventFetchingTest` (a comma moved to the front of a line) was reverted. Docs synced: `plan.md` §2.6, the resume point, `index.md` status table, `catalog/schema.md` Q4 |
| rev 34 | **Git is taught with pictures from now on, and the recap session is planned.** The user's request, verbatim: *"use a similar diagram you used before to explain me git the next time… its easire to understand git this way."* Rule 23 is now **unconditional**: every git/GitHub step draws the tree, including the steps where nothing moved, and when the tree is not the whole story the picture grows boxes (`(work)`, `(stage)`, `up/main`, a PR box) instead of the prose growing paragraphs. New **`resources/docs/git_recap.md`** plans the missing half of the subject — how a large team actually uses git — on a **throwaway sandbox**: one `git-recap-lab` repo, two clones (`dev-a`, `dev-b`) standing in for two engineers, four tiny text files so a conflict fits on one screen, and **five stages** (one engineer's line of commits → branches and the two shapes of merge → two engineers, one file, one conflict → the rules a large team adds: fetch vs pull, protected branch, review, required check, merge queue, `CODEOWNERS`, tag → when it goes wrong: `cherry-pick`, reflog, revert vs fix-forward, `bisect`, `stash`, `rebase -i`, `force-with-lease`, a committed secret). Each step carries the diagram it must produce, the command, and the questions to ask; the two incidents from our own log (the `86c9f58` ancestry trap and `git branch -d` refusing after a squash) are replayed in the sandbox. Registered as EXTRA slot 26 and linked from the §0 doc map; `index.md` and `git-workflow.md` point at it too. Code untouched |
| rev 35 | **Day 2.7 opened: the first real transaction boundary, and the project's four layers.** New `catalog/application/EventService` (`importAll`, `@Transactional`) — the first service in the codebase; `EventServiceTransactionTest` proves the boundary by counting rows through `JdbcTemplate` (a **different connection**) and is the **only** test class without `@Transactional`, because it must let the service commit; `@AfterEach` deletes its own rows since the free rollback is gone. Observed on the user's machine: `DataIntegrityViolationException` on `events_source_external_uk`, thrown at the **flush inside commit**, i.e. after `importAll` returned — which is why a `try`/`catch` in the method could never see it without an explicit `flush()`. **Layout decided by the user's delegation:** `api` (empty until Day 3) · `application` (services, `@Transactional`) · `domain` (entities, no Spring imports) · `infra` (repositories, clients); `EventService` was moved out of `domain`, constructor made `public`, one test import updated. **Propagation taught, step 2 half-built:** `REQUIRED` joins (inner work dies with the outer rollback), `REQUIRES_NEW` opens a second transaction at the cost of a **second pooled connection** — 10 concurrent users on that path exhaust a 10-connection pool. `V3__import_audit.sql` adds `import_attempts` (a *column* on `sources` keeps only the newest failure; a *table* keeps one row per attempt, and it carried no index on purpose). Also corrected in this session: a *checked* exception does **not** roll back (step 3 preview), the test listener rolls back by **calling** `rollback()`, and `mvnw verify` prints one `Results:` block **per module** (14 for catalog was never the total; 11 + 14 = **25 green**). Stop point: the session ended with green tests and everything uncommitted — the audit entity, the `REQUIRES_NEW` service, the `flush()` rework and its test are the resume tasks. **Log noise:** a test-only `logback-test.xml` was written and then **removed at the user's request** — do not recreate it without asking. **ANSI:** `spring.output.ansi.enabled: always` is correct but invisible under surefire, which captures the forked JVM's output into the report; colours appear only when the app itself runs in a terminal |
| rev 36–37 | **Day 4 opened, verified on screen, and three design questions answered in writing.** The user logged in with the generated password and reached the application: the 404 body was **our** RFC 7807 shape *with a `correlationId`*, so three subsystems were proven in one response — the security chain let an authenticated caller through, `exr-common`'s error model produced the body, and `CorrelationIdFilter` put the id there. Then three questions, now recorded: (1) **API versioning** — new **D-017** and new curriculum item **3.5**: additive-first, `@deprecated` with a date, `/api/v1/...` at the **gateway** only for a genuinely breaking change, with the definition of "breaking" written into §6; (2) **`SecurityConfig` vs `CorrelationIdFilter`** — one is a *blueprint* that tells Spring Security how to assemble its ~15-filter chain (it contains no filter itself), the other is a *single worker* that puts an id in the MDC; and because the security chain is registered **before** our filter, a refused request carries no correlation id — a known gap to close on Day 7; (3) **path prefixes** — `/api` is the shared public prefix, the resource segment (`/api/me`, `/api/events`) is what each service owns, and the gateway dispatches on it, so two services cannot collide | Two threads in this session. (1) **The user chose the Spring Security detour before finishing 2.7** (2.7 stays `[~]`: steps 3 and 4 pending) and asked whether Security, JWT, OAuth, token issuance, the unauth filter path and leaked-token damage are covered — answered with a coverage map: Security Day 4, JWT Day 5.1–5.3, OAuth split across Days 5/6/10B, issuance Day 5.2, filtering Day 4.2, revocation Day 5.5. Also corrected a stale tutorial style: `WebSecurityConfigurerAdapter` + `configure(HttpSecurity)` is gone in Security 6; it is a `SecurityFilterChain` bean now. (2) **New module `services/identity-service`**: pom (web + **security** + JPA + Flyway + Postgres + `spring-security-test` + `exr-common`), `IdentityServiceApplication`, `application.yml` (port **8082**, own database `identity` via `EXR_IDENTITY_DB_*`, `ddl-auto: validate`), registered in the root `<modules>`. `mvnw verify` green. **The failure, and the lesson:** the first start printed `Tomcat initialized with port 8080` and died with *"Failed to configure a DataSource: 'url' attribute is not specified"* — `target/classes` had **no `application.yml`**, so the app ran with zero configuration and every default applied (8080, no datasource). Two symptoms, one cause: *the JVM reads the classpath, not `src/main/resources`*. Fixes: `spring-boot-maven-plugin` added to the module (so `mvnw -pl services/identity-service -am spring-boot:run` copies resources by construction, and Day 16 gets its runnable jar), plus the habit of reading the **port** in the log as the first proof that config was picked up. **New rule 24** — *ask questions a beginner can answer*: the user's verdict *"You have to assume that i do not know these technologies and ask me questions accordingly"* after two questions were unanswerable; the rule bans questions that need the not-yet-taught vocabulary, multi-part asks, and "why" before "what happens", and records the worked failure |
| rev 38 | **The refusal gap found by testing, then closed.** Day 4.2 in code: `SecurityConfig` (stateless, CSRF off with its reason, `/api/public/**` open, everything else authenticated, Basic until Day 5) plus `IdentityController` with one open and one protected endpoint. Testing with `curl` exposed what reading would not: an **allowed** request carried `X-Correlation-Id`, a **refused** one carried none and had an **empty** body. Cause: Boot registers filter beans at `LOWEST_PRECEDENCE`, Spring Security's chain runs at `-100`, so security answered before our filter existed in the stack. Two changes fixed it — `CorrelationIdFilter implements Ordered` → `HIGHEST_PRECEDENCE`, and a new `RestAuthenticationEntryPoint` that writes the RFC 7807 body (with `WWW-Authenticate`, as RFC 7235 requires). Verified by `curl`: `401`, `application/problem+json`, `X-Correlation-Id` identical to the body's `correlationId`. **Bonus finding, now an interview answer:** a `permitAll()` endpoint still returns 401 if the client sends an **invalid** credential, because authentication runs before authorization — "you may skip credentials" is not "any credential will do"; Postman's `Inherit auth` reproduced it twice |
| rev 39–40 | **Packaging argued once, then the toolchain written down.** rev 39 settled the package layout in §6: **feature first, layers inside** (`api` · `application` · `domain` · `infra`), domain types kept together, and no `entity/`/`exception/`/`enums/` folders invented to group by kind — split only past ~10–15 types or when a second reason-to-change appears. rev 40 records **how this project is actually driven**, because the agent kept handing over `curl` and `docker exec … psql` for a workspace that has neither: the services run from **IntelliJ IDEA**, requests are sent from **Postman** (Basic Auth on the Authorization tab, JSON on Body → raw), and the database is inspected in **DBeaver** on `localhost:55432/identity`. Written into §0 (toolchain paragraph), §4.4 (developer tooling) and §11.3 (three rows), and the 4.3 line now records the first Postman result: five wrong passwords, five RFC 7807 `401`s with a `correlationId`, with the **row write still unproven** until it is read in DBeaver |
| rev 41 | **The login door drawn, the account-state check located, the module's first test written, and the work committed in three pieces.** (1) **Documentation:** `HowIsUserUpdatedOnLoginAttempt.md` gained **§12 — the request-level picture**: the stack a request walks (`CorrelationIdFilter` at `HIGHEST_PRECEDENCE` → `FilterChainProxy` → `SecurityContextHolderFilter` → `BasicAuthenticationFilter` → `ExceptionTranslationFilter` → `AuthorizationFilter` → controller), the inside of the login door (no header = nothing happens, the chain continues; a header = load the row, check the account state, compare the password, then success or refusal), and four consequences: there is **no `/login` endpoint**, the DB write happens **inside a filter** before the `401` is written, `permitAll()` still authenticates a client that sends an **invalid** credential, and **every** request pays a BCrypt check because nothing is cached between requests. §1 now points at §12 (lines 24–25), and the §2 cast table names the class that actually refuses (line 43): **`DaoAuthenticationProvider`**, not our code. (2) **"Where do we check `locked_until` and `status`?" answered once:** `DbUserDetailsService` lines 75–77 turn `SUSPENDED` into `disabled(true)` and `LOCKED` **or** a live deadline into `accountLocked(true)`; `User.isTemporarilyLocked` is the comparison; the refusal itself is thrown by Spring **before** `BCrypt.matches`, which is exactly why a locked account's counter cannot reset. Two traps recorded: `status = 'LOCKED'` has **no expiry** and nothing in the codebase writes it back (that needs an admin screen that does not exist), while the automatic lock is a **deadline** and reopens by itself; and `User.isActive()` still has no caller. (3) **`UserLockoutTest`** — the first test class in `identity-service` (`src/test` did not exist): four plain JUnit 5 + AssertJ tests over `recordFailedLogin` / `recordSuccessfulLogin` / `isTemporarilyLocked`, reaching the expired-deadline branch by locking with a **negative** `Duration` instead of faking a clock. **Written, never run** — unverified until the user pastes the result. (4) **Git:** three commits — `bded4a8` `feat(identity)` (16 files, +1014/−1), `d70881f` `test(identity)` (+113), `9fe1b93` `docs(identity)` (+355/−4) — branch **ahead 3**, nothing pushed; the push and the PR are the user's (rule 10). **Process lesson (rule 10 widened):** inside the agent's sandbox a `git commit -m "…"` reached `cmd.exe` with the quotes stripped, so it committed nothing and tried the message words as pathspecs (`error: pathspec 'register' did not match…`); the fix was to write the message to a file and run `git commit -F`, which is the same reason rule 10 bans multi-line snippets. |
| rev 43 | **The test ran, and the absence of a container line was the lesson.** The user ran `UserLockoutTest` in their own terminal — `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0` — and the output contained **no** Testcontainers startup, no Docker connection and no `jdbc:postgresql:` URL. That is what a **domain test** looks like: `new User(...)` plus arithmetic on `failedAttempts` and `lockedUntil`, so the test JVM loads JUnit, AssertJ and our classes and nothing else. Contrast with `EventMappingTest` in the catalog, which extends `AbstractIntegrationTest` and therefore starts `postgres:16`, because Hibernate has to send SQL. Two clarifications the user asked for, now written down: **(a)** the container is started by `AbstractIntegrationTest` alone, and nothing else in the project starts one; **(b)** in `UserLockoutTest.expiredDeadlineStartsAFreshRun`, a failure reporting the counter as `2` instead of `1` would mean `User.recordFailedLogin(int, Duration)` had skipped its expired-deadline branch — the branch that sets `failedAttempts = 0` and `lockedUntil = null` before counting — and would therefore be counting a new failure on top of a run that had already served its sentence. **Where 4.3 stands:** code complete, rule proven by four green tests, and one manual proof left — reading `failed_attempts = 5` with a future `locked_until` in DBeaver after five wrong passwords. The line numbers quoted back to the user were verified in the file before being quoted: the four `@Test` methods sit at lines 44, 69, 84 and 97, and the loop at line 48 is `attempt < MAX_ATTEMPTS`. |
| rev 44 | **4.3 closed the way a topic should close — the last two readings came from the user's own tools, and the second one proved an ordering.** After five wrong passwords, `users` held `failed_attempts = 5`, `locked_until` **in the future** and `status = ENABLED`. Then the **correct** password returned `401` and `failed_attempts` stayed `5`. What each reading settles: `status = ENABLED` proves the lock is a **deadline**, not a state change, so nothing has to un-lock the account later; `failed_attempts = 5` proves the chain `BasicAuthenticationFilter` → `AuthenticationFailureBadCredentialsEvent` → `AuthenticationEventsListener` → `LoginAttemptService.recordFailure` → `UPDATE users` ran inside a filter, with no controller involved; and `401` + an unchanged counter prove that `DaoAuthenticationProvider` reads the account-state flags **before** `BCrypt.matches`, so a locked attempt never becomes a `BadCredentials` event and is never counted. Recorded alongside: a re-teach of **how the password hash is stored** — the user remembered "a value stored per user that helps with hashing" (the **salt**) and expected a column for it. Answer: BCrypt puts the format marker `$2a$`, the cost (`10`), a fresh 22-character salt and the digest into **one 60-character string**, and that string is the whole of `users.password_hash` (`V1__identity_init.sql` line 29, `varchar(100)` to leave room for Argon2id's ~97). Written by `UserAccountService.register` line 70 through the `PasswordEncoder` bean at `SecurityConfig` lines 126–128; read back by `DbUserDetailsService` line 66; unpacked by `BCryptPasswordEncoder.matches`. `users.version` at line 46 is the optimistic-lock counter and has nothing to do with the `$2a$` marker. 4.1 was ticked with 4.3, because the three account states are now exercised on a real row rather than only mapped. |
| rev 45-46 | **Day 5 turned a login into a token, and then into two different kinds of token.** `TokenService` mints `sub` (the user id) / `email` / `roles` / `iss` / `aud` / `iat` / `exp` / `jti` under HS256; `POST /api/auth/login` reuses the same `AuthenticationManager` the Basic filter used, so 4.3's lockout still applies; `catalog` validates tokens **locally** with a `JwtDecoder` ordered signature → issuer/expiry → audience, and `whoami` answered from the token alone; `@PreAuthorize("hasRole('ADMIN')")` produced **403** only after catalog's advice took `AccessDeniedException` away from `exr-common`'s catch-all — the **500** before it is the lesson (a shared `@ExceptionHandler(Exception.class)` outranks the framework's own mapping). Then 7.6 gained its second half: `POST /api/auth/service-token` with `X-Service-Key` mints a five-minute token whose `sub` is the **service's name** and whose `roles` is `ROLE_SERVICE`. **The user's own Postman reading closed it:** that token returned `200` at `whoami` and **403** at `admin/ping` — one sentence separates *authentication* (`iss`/`aud`/signature/expiry: who is calling) from *authorization* (`roles`: what they may do). **Trap found while starting identity:** `service-key` was indented under `jwt:`, so it resolved as `exr.identity.jwt.service-key` and startup died with *Could not resolve placeholder* — in YAML the indentation **is** the property name, so a key one level too deep is a different property. |
| rev 47 | **PR #6 landed as `8a1f420`, and the merge itself became the lesson.** The six-step quiz put the trap in front of the user and he found it: the arrow ▼ must be pressed **after** `Build and test (Java 21)` turns green, because the ruleset greys the merge button out while the required check is pending — "wait" is the gate, not politeness. Two things the screen corrected: the compare page offers only **Create pull request** (the three strategies live on the PR page, so the merge question arrives one page late), and the repo's *displayed* branch is irrelevant to a PR — the two dropdowns `base` (destination) and `compare` (source) define it, and reversing them yields "no commits between". **The proof that made the cleanup safe was content, not ancestry:** `git diff --stat 9c1d5c3 8a1f420` printed nothing, so the squashed commit holds every byte of the branch even though git cannot see the branch as merged — which is exactly why `git branch -d` refuses ("not fully merged") and `-D` is the deliberate choice here. Remote branch deleted in the UI, `git fetch --prune` dropped the stale ref, local branch removed, and `master` fast-forwarded to the squash commit. **PR body convention fixed for later:** what changed, how it was verified, what is deliberately still open — so the one commit that survives the squash is readable on its own. |
| rev 48 | **A full read-through of the repository and every document, at the user's request — and the drift was all in the status blocks, never in the code.** What agrees with itself: 33 tests (11 `exr-common`, 16 `catalog-service`, 6 `identity-service`), catalog Flyway `V1`–`V3` and identity `V1`–`V2`, and both `SecurityConfig` classes matching exactly what §9 claims about them — the local `JwtDecoder`, the `@Order(HIGHEST_PRECEDENCE)` advice, the `permitAll()` list, `TokenService.issueServiceToken`. What had decayed was the narrative: §0 still called Day 4 in progress with 2.7 as the next action, §11.2 still said 18 tests and knew one service, `index.md` still said Day 2 with 23 tests and no `identity-service` in the layout, `README.md` still claimed *"Day 1 — there are no services yet"*, and `git-workflow.md` pointed at rev 27. All five corrected here, and the 2.7 resume paragraph in §0 is now explicitly labelled history. **The lesson worth keeping:** a status block decays silently because nothing compiles it — the only defence is a periodic read-through that compares the prose against `git ls-files`, the migration list and a test count. |






### 11.2 Work status
| Area | Status |
|---|---|
| Planning / architecture | **DONE** — `resources/docs/plan.md` (this file) |
| Product spec | **DONE** — `resources/docs/projectDetails.md` |
| Any code | **`libs/exr-common` done for now** — `CorrelationIdFilter`, `ProblemSupport` + `GlobalExceptionHandler`, `ExrCommonAutoConfiguration`; **11 tests green** |
| `.gitignore` | **DONE and verified** — the `resources/` bug is fixed (rev 12) |
| Build files (`pom.xml`, Maven wrapper) | **DONE** — root aggregator `pom.xml` (Java 21, `packaging=pom`) + Maven wrapper pinning **Maven 3.9.15**; verified with `mvnw.cmd -v` |
| `README.md`, `.editorconfig`, `.gitattributes` | **DONE** |
| git repository | **DONE** — `origin` = `https://github.com/electronik54/TW_systemArch.git`, **default branch is `master`** (not `main`), first commits pushed |
| CI pipeline | **DONE** — `.github/workflows/ci.yml`, first run green on `master` (run 42940713385). `mvnw` is committed as `100755`. **Updated in rev 21:** the job now starts a `postgres:16` **service container** (health-checked) and overrides `EXR_DB_URL` / `EXR_DB_USER` / `EXR_DB_PASSWORD`, because the three `@SpringBootTest` persistence tests need a real database — without it `mvnw verify` would fail on `master`. Flyway's V1 both creates the tables and seeds `sources`, so no extra setup step is needed. **Removed in rev 25:** the `services:` block and the three `EXR_DB_*` overrides were deleted — the tests start their own `postgres:16` (Testcontainers), so CI declares no database at all. **Protected in rev 32:** a `master` **ruleset** requires that check (`Build and test (Java 21)`), blocks force pushes and deletions, and has an **empty bypass list** |
| `services/catalog-service` | **RUNS** — `CatalogServiceApplication`, `application.yml` (credentials via `${EXR_DB_*}` env vars, `ddl-auto: validate`), `FlywayMigrationTest`, `SourceMappingTest`, `EventMappingTest`, `EventFetchingTest`, `EventRepository`. **16 tests green** (reactor total 33: 11 in `exr-common`, 16 here, 6 in `identity-service`); all persistence tests run against a **Testcontainers `postgres:16`** |
| `services/identity-service` | **RUNS on 8082** against its own database `identity` on the same `exr-pg` container — `User`/`UserStatus`/`UserRole`, `UserRepository`, `UserAccountService`, `LoginService`, `TokenService`, `LoginAttemptService`, `DbUserDetailsService`, `RestAuthenticationEntryPoint`, `SecurityConfig`, Flyway **V1 + V2**. **6 tests green** (4 lockout arithmetic, 2 token claims), and `/api/auth/register`, `/api/auth/login`, `/api/auth/service-token` all answered in Postman |
| Local Postgres | **RUNNING** — Docker container `exr-pg`, `postgres:16`, database `catalog`, host port `55432`. Needed only by the **running app** since 2.5 — the tests start their own |
| `V1__catalog_init.sql` | **VERIFIED** — ran clean against PostgreSQL 16. 4 tables (`sources`, `events`, `subscriptions`, `event_reports`), 13 explicit indexes + 6 implicit = **19 total**. The `subscriptions_active_uk` partial index was proven to reject a duplicate live subscription and allow a resubscribe after `ended_at` |
| Docs location | **MOVED** — `docs/` → `resources/docs/` (end of Day 0) |
| Local infra (Docker Compose, LocalStack) | **NOT STARTED** |
| Curriculum | **Day 1 complete** (1.1–1.4 ✅) · **Day 2: 2.3 ✅** (migration + Flyway verified) · **2.4a ✅** (app boots, order confirmed in the log) · **2.4b ✅** (`Source` + `ExpiryPolicy` mapped, `validate` green) · **2.4c ✅** (all 33 `events` columns mapped — arrays, `@Version`, timestamps, overrides; `EventMappingTest` 2 tests) · **2.4d ✅** (N+1 measured **4 → 1** statements with `@EntityGraph`; `EventRepository` + `EventFetchingTest`) · **2.5 ✅** (the suite starts its own `postgres:16` — `exr-pg` stopped and still 18 green; docker-java pinned to API 1.44) · **CI-gate lab ✅** (2026-11-13: the `master` ruleset is active, the red merge `914d4fb` was reverted out through PR #4 → `c315fc3`, trunk green) · **2.7 [~] in progress** (rev 35: `EventService` is the first transaction boundary and its test proves it from a separate connection; four-layer layout in place; `V3` adds `import_attempts`; resume at the `REQUIRES_NEW` audit service) · **Day 4 [~]** (rev 44: **4.1 ✅ 4.2 ✅ 4.3 ✅** — five failures wrote `failed_attempts = 5` with a future `locked_until`, the correct password came back `401` with the counter unmoved, and `UserLockoutTest` is 4 green; **4.4** CORS vs CSRF still open) · **Day 5 [~] rev 46** (**5.1 ✅ 5.3 ✅ 5.4 ✅** — tokens minted, validated locally by `catalog`, and method security refusing with **403**; **5.2** access half only; **7.6 service half ✅** — `POST /api/auth/service-token` → `whoami` `200` / `admin/ping` `403` / wrong key `401`) · **merged to `master` as `8a1f420` (PR #6, squash)** |
| Documentation set | **`index.md`** (entry point) · `plan.md` (manual) · `projectDetails.md` (spec) · `need.txt` (COVERED / NOT COVERED map) · **`git-workflow.md`** (branching strategy — trunk-based, PR rules, protection, tagging) · **`catalog/schema.md`** (catalog schema, plain language, every column + constraint) · **`HowIsUserUpdatedOnLoginAttempt.md`** (the login flow in one page + **§12 the request-level picture**, rev 41) |

### 11.3 Local environment (verified 2026-11-17 — the toolchain rows were added in rev 40)

| Tool | Version / location | Notes |
|---|---|---|
| **JAVA_HOME** (user) | `C:\Program Files\Java\jdk-21.0.10` | set in rev 12; **Maven and `mvnw` read this → they build with Java 21** ✅ |
| JDK 21 | 21.0.10 | `javac 21.0.10` verified ✅ |
| Java on PATH | **JDK 17** (`C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot`) | machine-level PATH is evaluated before user PATH, so a bare `java -version` still reports 17 |
| Maven | 3.9.15 | verified running on Java 21 once `JAVA_HOME` is set ✅ |
| Git | 2.45.1 | ✅ |
| **How services are started** | **IntelliJ IDEA** run configuration | The user starts and debugs every service from the IDE (§0 toolchain, rev 40) — a step says "run `IdentityServiceApplication`" and the startup log is read in the IDE console |
| **How requests are sent** | **Postman** | `POST /api/auth/register`, `GET /api/me`, … — Basic Auth lives on the **Authorization** tab (Type: Basic Auth), JSON bodies on **Body → raw → JSON**. The response pane shows status, headers and the RFC 7807 body |
| **How the database is viewed** | **DBeaver** | Connection `localhost:55432`, database `identity`, user `postgres`. Every schema or row check is a query pasted into a SQL editor — **re-run it after a request**, because the result grid holds the rows fetched last time |
| Docker | 29.6.1, **engine running** | container `exr-pg` = `postgres:16`, database `catalog`, port `55432`; used by the app, **not** by the tests since 2.5. **API floor: Engine 29 accepts only ≥ 1.40 while docker-java asks for 1.32** → the root `pom.xml` pins `api.version=1.44` for the surefire JVM. Stop the container with `docker stop exr-pg` |
| Node / npm | 24.13.0 / 10.5.2 | ✅ (needed Day 19) |
| AWS CLI | 2.34.2 | ✅ |
| Terraform | **not installed** | needed Day 17 only |
| Env backup | `C:\Users\Nikhil\jdk-env-backup.txt` | previous `JAVA_HOME`/`PATH` values, in case of rollback |

**Known caveat (harmless, but know the answer if asked):** bare `java -version` reports **17** in a fresh terminal, because the *machine* PATH lists Microsoft JDK 17 first and a *user* PATH entry cannot override a machine entry. Our builds are unaffected — `mvnw` and Maven resolve the JDK from `JAVA_HOME`, which is exactly how CI pipelines and containers do it. To make `java` itself report 21, reorder or remove that entry in the **System** environment variables (needs admin).

**Resume point:** see section 0. Day 5 is three-quarters done and **PR #6 is merged on `master` as `8a1f420`**; the next action is the topic gate on the next thread (5.2's refresh half, or Day 6's social login).

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

**Re-teach on demand (user request):** when `CorrelationIdFilter` and `ProblemSupport` are first wired into a real service, stop and re-explain them **including the Spring Security filter chain** (servlet filter ordering, `OncePerRequestFilter` vs security filters, where authentication sits relative to our filter) — the user has not worked with the Spring filter chain before. Do not assume this knowledge on Day 4/7.

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
- Do not run the build, the tests, the application or Docker yourself — hand over the command (rule 18). Read-only inspection is fine.
- Do not answer in walls of text. ~250 words by default; split across replies instead of sprawling (rule 20).
- Do not make an edit without stating the file, the line range, what changed and why (rule 21).
- Do not claim something works until it compiles and its tests run.
- Do not skip the tradeoff / failure-mode / 10x framing — it is the whole point of the exercise.





