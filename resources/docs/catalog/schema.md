# catalog-service — schema reference

> **Plain-language reference for the `catalog` database.** Every column, its type, its constraints,
> and *why* it exists that way.
>
> Companion to `plan.md` Day 2. The `catalog` service owns three tables: `sources`, `events`,
> `subscriptions`. It does **not** own users — that is `identity`'s job.
>
> **Rule for the agent:** when the schema changes, update this file. Do not re-explain the schema
> in chat if it is already written here — link to it instead.

---

## 1. `sources` — where the information comes from

A tiny fixed list of suppliers (TMDB, IGDB, our seed data, manual entry). Four rows, forever.

| Column | Type | Constraints | Why it exists |
|---|---|---|---|
| `id` | `smallint` | PRIMARY KEY | internal short number; 2 bytes is honest for 4 rows |
| `code` | `varchar(20)` | NOT NULL, UNIQUE | short machine name: `TMDB`, `IGDB`, `SEED`, `MANUAL` |
| `display_name` | `varchar(100)` | NOT NULL | human-readable name |
| `details_retention_days` | `integer` | NOT NULL, > 0 | **how long we may keep the rich details** — a licence rule, not a preference |
| `cache_ttl_seconds` | `integer` | NOT NULL, > 0 | how long a cached copy may be served before re-fetching |
| `expiry_policy` | `varchar(20)` | NOT NULL, DEFAULT `'ANONYMISE'`, CHECK IN (`'KEEP_TITLE'`,`'ANONYMISE'`,`'DELETE_ALL'`) | **one switch, three settings**: what happens when the retention clock runs out — see §8 |
| `enabled` | `boolean` | NOT NULL, DEFAULT `true` | is this feed switched on |
| `created_at` | `timestamptz` | NOT NULL, DEFAULT `now()` | |
| `updated_at` | `timestamptz` | NOT NULL, DEFAULT `now()` | |

**Why these live in the database and not in `application.yml`:** TMDB and IGDB have *different*
legal limits. If those numbers were in code, changing one would mean a deploy. In a table, an admin
changes a row — and the change is auditable.

---

## 2. `events` — the things with dates

This table is deliberately split into **two halves**: a *skeleton* that is kept indefinitely, and
*details* that expire.

### 2a. The skeleton — always present

| Column | Type | Constraints | Why it exists |
|---|---|---|---|
| `id` | `uuid` | PRIMARY KEY | generated in the app; safe to log and to put in URLs |
| `source_id` | `smallint` | NOT NULL, FK → `sources(id)` | which supplier this came from |
| `external_id` | `varchar(64)` | NOT NULL | the supplier's own product number |
| `category` | `varchar(16)` | NOT NULL, CHECK IN (`GAME`,`MOVIE`,`TV_SERIES`,`ANIME`,`LOCAL_EVENT`) | what kind of thing it is |
| `title` | `varchar(300)` | NOT NULL | **kept forever** — powers autocomplete, history and lists |
| `release_date` | `date` | NULL | `NULL` means "TBA" |
| `release_precision` | `varchar(10)` | NOT NULL, DEFAULT `'UNKNOWN'`, CHECK IN (`DAY`,`MONTH`,`YEAR`,`UNKNOWN`) | how exact we are |
| `status` | `varchar(16)` | NOT NULL, DEFAULT `'ANNOUNCED'`, CHECK IN (`ANNOUNCED`,`CONFIRMED`,`RELEASED`,`DELAYED`,`CANCELLED`) | lifecycle of the release |
| `availability` | `varchar(16)` | NOT NULL, DEFAULT `'FRESH'`, CHECK IN (`FRESH`,`STALE`,`GONE`) | are the details present, expired, or gone from the source |
| `details_expired_at` | `timestamptz` | NULL | when the details were purged; `NULL` while details are present |
| `created_at` | `timestamptz` | NOT NULL, DEFAULT `now()` | |
| `updated_at` | `timestamptz` | NOT NULL, DEFAULT `now()` | |
| `version` | `bigint` | NOT NULL, DEFAULT `0` | optimistic lock, so two syncs cannot silently overwrite each other |

**Table-level constraints**

| Constraint | Definition | Why |
|---|---|---|
| `events_source_external_uk` | UNIQUE (`source_id`, `external_id`) | makes ingestion **idempotent**: importing the same item twice updates instead of duplicating. Also the key that lets us re-fetch an expired item back into the *same* row |
| `events_precision_chk` | `(release_precision = 'UNKNOWN' AND release_date IS NULL) OR (release_precision <> 'UNKNOWN' AND release_date IS NOT NULL)` | precision and date must tell the same story — you cannot claim "unknown" while storing a date |

### 2b. The details — expire after `details_retention_days`

| Column | Type | Constraints | Why it exists |
|---|---|---|---|
| `synopsis` | `text` | NULL | licensed description; the most obviously protected field |
| `poster_url` | `text` | NULL | image reference |
| `genres` | `text[]` | NOT NULL, DEFAULT `'{}'` | display + filter only; a list is enough |
| `platforms` | `text[]` | NOT NULL, DEFAULT `'{}'` | games only |
| `network` | `varchar(120)` | NULL | TV series |
| `venue` | `varchar(200)` | NULL | local events |
| `city` | `varchar(120)` | NULL | local events — also a filter |
| `country` | `char(2)` | NULL | ISO-3166 alpha-2 |
| `external_url` | `text` | NULL | link back to the source (also attribution) |
| `source_updated_at` | `timestamptz` | NULL | when the *source* says it changed |
| `last_synced_at` | `timestamptz` | NULL | when *we* last fetched the details |

When details expire, these columns are set to `NULL` / `'{}'` and `availability` becomes `'STALE'`.
The skeleton row is never removed (unless the source's policy is `DELETE_ALL`).

---

## 3. `subscriptions` — who wants to be told about what

| Column | Type | Constraints | Why it exists |
|---|---|---|---|
| `id` | `uuid` | PRIMARY KEY | |
| `user_id` | `uuid` | NOT NULL, **no foreign key** | users live in `identity`'s database; a foreign key would require a shared database, which is the distributed-monolith anti-pattern (`plan.md` §3.1) |
| `event_id` | `uuid` | NOT NULL, FK → `events(id)` ON DELETE CASCADE | an event belonging to another service is a *real* relationship, so this one gets enforced |
| `notify_on_new` | `boolean` | NOT NULL, DEFAULT `true` | tell me when it is announced |
| `notify_on_update` | `boolean` | NOT NULL, DEFAULT `true` | tell me when the date moves |
| `lead_time_minutes` | `integer` | NOT NULL, DEFAULT `1440`, CHECK BETWEEN 0 AND 43200 | how early to remind: 1440 = 1 day, 10080 = 7 days, max 30 days |
| `channels` | `text[]` | NOT NULL, DEFAULT `'{EMAIL}'` | `EMAIL`, `IN_APP`, `CALENDAR` |
| `created_at` | `timestamptz` | NOT NULL, DEFAULT `now()` | |
| `updated_at` | `timestamptz` | NOT NULL, DEFAULT `now()` | |
| `ended_at` | `timestamptz` | NULL | when the subscription stopped being live; `NULL` = active |
| `end_reason` | `varchar(16)` | NULL, CHECK IN (`USER`,`EVENT_ENDED`,`TIER_DOWNGRADE`) | **why** it ended — a single timestamp covers all three cases so "is it active?" stays one predicate |
| `version` | `bigint` | NOT NULL, DEFAULT `0` | optimistic lock |

---

## 4. The indexes, and the one that carries the design

### On `subscriptions`

| Index | Definition | What it is for |
|---|---|---|
| `subscriptions_active_uk` | UNIQUE (`user_id`, `event_id`) **WHERE `ended_at IS NULL`** | ⭐ at most one **live** subscription per person per event |
| `subscriptions_user_active_idx` | (`user_id`) WHERE `ended_at IS NULL` | "my subscriptions" and the free-tier count of 2 |
| `subscriptions_event_idx` | (`event_id`) WHERE `ended_at IS NULL` | fan-out: "who wants to know about this event?" |

**The starred one is a *partial* unique index** — a uniqueness rule that only applies to rows matching
the `WHERE` clause. In everyday terms: **"the bouncer only checks the guest list for people who
haven't been crossed out."** That one rule solves three separate problems:

| Problem | Why it is already solved |
|---|---|
| Double-tap "subscribe" | the second insert is refused **by the storage**, not by application code |
| Unsubscribe, then resubscribe | the old row is crossed out, so it is outside the rule and a new row is allowed |
| Two requests arriving at the same instant | only one can be stored, even if both pass the application's own checks |

The important property: application code can be bypassed by a bug, a retry, or a race. The storage cannot.

### On `events`

| Index | Definition | What it is for |
|---|---|---|
| `events_release_date_idx` | (`release_date`) WHERE `status <> 'CANCELLED'` | the main screen: upcoming releases in date order |
| `events_genres_gin` | GIN (`genres`) | filtering by genre (a GIN index is the "quick-find list" for *lists of values*) |
| `events_platforms_gin` | GIN (`platforms`) | filtering by platform |
| `events_city_idx` | (`city`) WHERE `city IS NOT NULL` | local events filtered by city |
| `events_last_synced_idx` | (`last_synced_at`) | ingestion diffing: "what has not been refreshed in a while" |
| `events_title_search_idx` | GIN on a `tsvector` of `title` | free-text search (built on Day 2.6) |

---

## 5. How expiry works — the lazy refresh

This is the part that is easy to get wrong. Retention is a **legal** deadline, not a technical one.

### The three states

| `availability` | Meaning | What a user sees |
|---|---|---|
| `FRESH` | details are present and inside the retention window | the full event page |
| `STALE` | details were purged; only the skeleton remains | title, date, status + "details being refreshed" |
| `GONE` | the source says the item no longer exists | title + "no longer listed at the source" (kept for history) |

### What happens when the retention clock runs out

1. A purge job nulls the **details** columns, sets `details_expired_at = now()` and `availability = 'STALE'`.
   The skeleton (`id`, `source_id`, `external_id`, `title`, `release_date`, `status`) **stays**.
2. `subscriptions` rows are untouched — the event row still exists, so nothing silently disappears.
3. The next time a user touches that event (opens it, or it appears in their list):
   - we **immediately** return the skeleton — no waiting on the network;
   - in the **background**, we try to re-fetch the details from the source.
4. Re-fetch outcomes:
   - **success** → details repopulated, `last_synced_at = now()`, `availability = 'FRESH'`,
     `details_expired_at = NULL`. The retention clock starts again.
   - **source says 404/410** → `availability = 'GONE'`. Never retried; the skeleton stays for history.
   - **source is down or throttling** → stays `STALE`, and we back off. The user still gets the
     skeleton, never an error page.

### The two guards that make this safe

| Risk | Guard |
|---|---|
| 10,000 users open the same expired event at once → 10,000 upstream calls | **single-flight**: one Redis lock per event id (`rehydrate:{eventId}`); everyone else gets the stale copy while one caller does the fetch |
| The read path becomes slow because it now waits for a third party | the re-fetch is **asynchronous**. The rule "no third-party calls on the read path" becomes "**no user request ever *blocks* on a third party**" — the user gets stale data instantly |

This pattern has a name worth knowing: **stale-while-revalidate**. Serve what you have, refresh behind
the scenes, and never make the user wait for freshness.

---

## 6. Decisions, and the alternatives that were rejected

| Decision | Chosen | Rejected | Why |
|---|---|---|---|
| Primary key | `uuid` | `bigserial` | generated in the app (no round-trip), safe to log and expose, no cross-service collisions. Cost: 16 bytes and random insert order — the fix is UUIDv7, which is time-ordered |
| Timestamps | `timestamptz` | `timestamp` | store an *instant*. `timestamp` is a wall clock with no timezone — the classic source of "off by five hours" bugs, because CI is UTC and users are not |
| `release_date` | `date` + `precision` | `timestamptz` | a release *date* is the same calendar day everywhere. Adding a time invents precision we do not have |
| Enumerations | `varchar` + `CHECK` | native Postgres `enum` | native enums are awkward to extend (`ALTER TYPE`), cannot be reordered, and are hard to remove |
| Genres / platforms | `text[]` + GIN | junction tables | we only display and filter them. A junction table earns its cost only when the values have their own attributes (a parent genre, a slug, a colour) |
| `user_id` | plain `uuid` | foreign key | the users table lives in another service; an FK means a shared database, which collapses the service boundary |
| Unsubscribe | soft delete | hard `DELETE` | keeps history, frees the quota slot, and makes an accidental unsubscribe recoverable |
| Retention | purge **details**, keep **skeleton** | delete the whole row | deleting the row would silently destroy subscriptions, break autocomplete and erase history (see §5) |
| Re-fetch timing | on demand, in the background | a nightly job re-fetching everything | a job would hammer the source for items nobody looks at; on-demand is cheaper and fresher |
| Duplicate re-fetch | single-flight lock | rely on the cache only | without a lock, one popular expired event becomes a burst of simultaneous upstream calls |

---

## 7. Open questions

| # | Question | Decide on |
|---|---|---|
| Q1 | Exact `details_retention_days` and `cache_ttl_seconds` per source — the real numbers from each licence | Day 9 |
| Q2 | **Is a title "licensed content"?** Keeping the title forever is what makes autocomplete work, but some terms say "delete all data". Verify per source, and keep `expiry_policy` configurable because of it | Day 10B |
| Q3 | For a source whose policy is `DELETE_ALL`, what happens to live subscriptions pointing at a purged event? (cascade today) | Day 13 |
| Q4 | Does the free-text search index cover only `title` (always present) or `synopsis` too (blanks when `STALE`)? | Day 2.6 |
---

## 8. `expiry_policy` — the one switch that decides what happens at the deadline

**First, a correction.** Earlier versions of this document had *two* overlapping switches: an
`expiry_policy` (`KEEP_STUB` / `DELETE_ALL`) and a separate "title policy" (`KEEP_TITLE` / `ANONYMISE`
/ `DELETE_ALL`). That was confusing, and confusion about a legal setting is dangerous. **It is now one
column with three values:**

| `expiry_policy` | When the clock runs out | What survives | Autocomplete | Exposure |
|---|---|---|---|---|
| `KEEP_TITLE` | nothing is purged; we simply stop refreshing | everything | works | highest — **only** if the supplier's terms allow indefinite storage |
| `ANONYMISE` *(default)* | details nulled; `title` replaced with a placeholder | `id`, `source_id`, `external_id`, `release_date`, `status`, plus any admin override | that item disappears from suggestions until refreshed | low |
| `DELETE_ALL` | the whole row is removed (and subscriptions cascade) | nothing | gone | none |

### How the purge actually works

```
Every night (scheduled job, batched — never one giant transaction):

  for each enabled source:
      cutoff = now() - (source.details_retention_days days)

      find events
        where source_id   = this source
          and availability = 'FRESH'
          and last_synced_at < cutoff        -- past the deadline
        limit 500                            -- a batch, then commit and continue

      for each row → apply source.expiry_policy
```

Rules the purge job must obey:

1. **Never touch the override columns** (`title_override`, `release_date_override`, `overridden_fields`,
   `override_*`). Those are our data, not the supplier's (§10).
2. **Never touch `subscriptions`.** The reminder was already scheduled in `notification` (§9).
3. **Batch, commit, repeat** — so a crash resumes rather than restarting, and no lock is held for minutes.
4. **Idempotent** — running it twice must be harmless.
5. **Emit `EventDetailsExpired`** to the outbox, so other services can react (Day 11).

### Why this lives per source and not in config

TMDB and IGDB may have different deadlines, and a new supplier might be stricter than both. A column
means an admin changes a number, and the change is auditable — no redeploy, no code change.

**Rejected: decorating the title (`"The Last of Us (expired)"`).** The protected expression is still
there. A retention clause is about *retaining data obtained from the supplier*, not about the exact
string. Removing the title is the safe transformation; annotating it is not.


**The tradeoff nobody escapes:** you cannot simultaneously keep a title for autocomplete *and* treat that title as protected. One of the two has to give. That is a product decision, not an engineering one — but the engineering response is to make it a per-source switch so the answer can differ per supplier.

**What survives under every policy:** `external_id` (a number — an identifier, not an expression) and `release_date` (a fact). Those two are what let us re-fetch the item back into the same row.
*Caveat worth knowing:* individual facts are generally not protected by copyright, but the EU's *sui generis database right* can protect a substantial **compilation** even when the individual facts are free. This is a legal question; the engineering answer is the per-source switch, plus reading each supplier's terms before signing up.

---

## 9. How reminders survive an expired event

**Principle: the obligation is captured when the user subscribes, not when the reminder fires.**

If the reminder were computed *at firing time*, then purging the release date would silently cancel the user's reminder — the worst possible outcome, because the user never finds out. So instead:

| Moment | What happens |
|---|---|
| User subscribes | `catalog` writes an outbox row, `notification` consumes `EventSubscribed` and stores `remind_at = release_date − lead_time`. **The reminder now exists independently of the event row.** |
| Release date changes | `catalog` emits `EventDateChanged` → `notification` recomputes `remind_at` |
| Source says the item is gone | `catalog` sets `availability = 'GONE'`, emits `EventGone` → `notification` cancels the reminder and tells the user |
| Retention purge runs | **nothing** — the purge never touches a reminder that has already been scheduled |
| Reminder fires | uses the stored `remind_at`; the event's `availability` only affects *how rich* the message is, never *whether* it is sent |

### The three triggers for re-hydration

On-demand refresh has a blind spot, and it is exactly the notification case:

| # | Trigger | Why |
|---|---|---|
| 1 | A user opens the event | the obvious case |
| 2 | The event appears in a user's list | also user-driven |
| 3 | **The reminder scheduler, a look-ahead window before `remind_at`** | **notifications have no user request.** Nobody is looking at the event, so the *system* must look — otherwise a user is reminded using a stale date |

Trigger 3 is the one pure on-demand design misses. Without it, an event nobody browses is reminded from a date that could be months out of date.

---

## 10. Editorial overrides — "our data" vs "the supplier's data"

This is the part of the design that makes expiry survivable, and it is worth separating clearly:

| Kind of data | Who owns it | Subject to the supplier's retention clause? |
|---|---|---|
| **Sourced** — whatever we fetched from TMDB/IGDB | the supplier | **yes** — it expires |
| **Authored** — what an admin typed as an editorial decision | **us** | **no** — it is our own record |

Extra columns on `events` (never purged):

| Column | Type | Constraints | Meaning |
|---|---|---|---|
| `title_override` | `varchar(300)` | NULL | admin-authored title |
| `release_date_override` | `date` | NULL | admin-authored date |
| `overridden_fields` | `text[]` | NOT NULL, DEFAULT `'{}'` | which fields the admin has taken over — the **precedence marker** |
| `override_author_id` | `uuid` | NULL | which user made the change (plain id — `identity` owns users) |
| `override_updated_at` | `timestamptz` | NULL | when |

**Reading an event:** the effective value is `COALESCE(override_column, sourced_column)`.

**Three rules that keep this honest:**
1. The **purge never touches** `title_override` / `release_date_override`. They are ours, forever.
2. A **source sync never overwrites** a field listed in `overridden_fields`. It updates the sourced column only, then emits `SourceChangedUnderOverride` so an admin can review.
3. An override is legitimate **because of authorship, not because of how it was typed**. Transcribing the supplier's title into a box — with or without copy-paste — is still copying their expression. Blocking paste changes the keystrokes, not the provenance.

**Therefore:** the override layer is a *product feature* (it matches `projectDetails.md` §10, "manual override of source data"), **not** a legal workaround. The legal protection comes from `sources.expiry_policy` (§8), not from an admin retyping a value.

---

## 11. Hot events — details that keep changing

Some events churn constantly: a game delayed four times, a concert rescheduled twice. This creates **three separate problems**, and they need three separate answers.

| Problem | Symptom | Mechanism |
|---|---|---|
| **Freshness** | users see a date that moved days ago | adaptive polling — see the tiers below |
| **Notification spam** | three "date changed" emails in one afternoon; users unsubscribe | coalescing window + per-subscription rate limit + a significance rule |
| **Write contention** | several ingestion workers hammering one row / one partition | single-flight refresh + optimistic locking + partition-key choice (H4) |

### Extra columns to support it

| Column | Type | Meaning |
|---|---|---|
| `details_hash` | `char(64)` | hash of the detail fields — a change only counts if the hash differs |
| `last_change_at` | `timestamptz` | when a *meaningful* change was last seen; drives tiering and decay |

### Freshness tiers (adaptive polling)

| Tier | Qualifies when | Refresh cadence |
|---|---|---|
| `HOT` | many subscribers, **or** changed within the last 7 days, **or** releasing within 7 days | every 5–15 minutes |
| `WARM` | moderate interest | hourly |
| `COLD` | few subscribers, far from release | daily |

Two rules make this work:
- **Promotion** happens automatically from the signals above — no admin needed.
- **Decay**: a hot event that stops changing drops back down. Otherwise you poll a finished event forever.

### Notification rules for churn

| Rule | Why |
|---|---|
| **Coalesce** several changes inside a window (e.g. 15 min) into one notice | a live announcement stream should produce one email, not five |
| **Rate-limit per subscription**: at most one date-change notice per event per 24 h; later changes go into a digest | this is what stops users unsubscribing out of irritation |
| **Significance rule**: notify only if the date moves by ≥ 1 day, or the status changes | a re-worded synopsis is not worth a ping |
| **Idempotent reschedule**: cancel the old reminder by (`subscriptionId`, `version`) before scheduling the new one | otherwise the user gets two reminders for one event |

### Cheaper freshness checks (conditional requests)

| Technique | What it does | Availability |
|---|---|---|
| `ETag` + `If-None-Match` | server answers `304 Not Modified` with no body | some APIs support it (TMDB does); must be detected per source |
| `Last-Modified` + `If-Modified-Since` | same idea with a timestamp | patchier support |
| the source's own `updated_at` field | compare against our stored `source_updated_at` | only if the source exposes one |
| `details_hash` comparison | fetch, then compare fingerprints ourselves | always works — the fallback |

**Important nuance most people miss:** a `304` saves **bandwidth and parsing**, but it **still counts as a request against the rate limit**. So conditional requests help you fit inside the budget; they do not make polling free.

**`details_hash` and the tier are different mechanisms:**

| Mechanism | Question it answers | When it runs |
|---|---|---|
| `details_hash` | *"did anything actually change?"* | every fetch |
| freshness tier | *"how often should we look?"* | scheduling |
| admin-assigned tier | a **manual override** of the automatic tier | same override pattern as the fields in §10 |

**Warning:** an admin-set `HOT` with no end date is a permanent cost leak — it polls forever. Time-box it ("hot until 2026-04-01") or let it decay with the automatic signals.

---

## 12. Sub-events: series → seasons → episodes, and tours → dates

Some "events" contain other events. A series has seasons, a season has episodes, a tour has dates. This needs an explicit hierarchy, not an afterthought.

### Extra columns

| Column | Table | Type | Constraints | Meaning |
|---|---|---|---|---|
| `parent_event_id` | `events` | `uuid` | NULL, FK → `events(id)` ON DELETE CASCADE | the container: a series, a season, a tour |
| `subscription_scope` | `subscriptions` | `varchar(20)` | NOT NULL, DEFAULT `'THIS_EVENT'`, CHECK IN (`THIS_EVENT`,`INCLUDE_CHILDREN`) | does this subscription also cover future children? |

It is a **self-referencing** table: `events.parent_event_id → events.id`. Descendants are found with a recursive query (`WITH RECURSIVE`) — a standard SQL feature, no extra table needed.

### Contained vs related — do not confuse them

| Relationship | Example | Modelled as |
|---|---|---|
| **Contained in** | episode ⊆ season ⊆ series; one concert date ⊆ a tour | `parent_event_id` |
| **Related** | a *sequel* film, a remake, a spin-off | **not** a parent link — a separate event, possibly with a "related" link later |

A sequel is not a child. Getting this wrong makes "subscribe to the series" accidentally mean "notify me about every future film in the franchise".

### Tier inheritance (the user's suggestion — adopted)

- A child **inherits its parent's freshness tier** unless it has its own `tier_override`.
- So an admin marks the *series* as `HOT` once, and every new episode is polled on the same cadence.
- Overriding a single child still works — same precedence pattern as §10.

### The subscription and quota consequences

| Question | Answer |
|---|---|
| What does "subscribe to a series" mean? | `subscription_scope = 'INCLUDE_CHILDREN'` → we notify on each new child event |
| How many quota slots does it cost? | **One.** It is one subscription row, even if it produces 24 notifications a season |
| 24 episodes drop at once — 24 emails? | **No.** Coalesce into one digest ("season 3 is out — 24 episodes"). This is the churn-control rule from §11 applied to hierarchy |
| Which event does the reminder attach to? | the **child** (the episode's own air date), never the parent |
| What if a parent expires but children do not? | they expire independently; the parent's skeleton keeps the hierarchy navigable |

**Open question:** should a series subscription count as one slot or as N? One is friendlier and simpler; N is "fairer" to the 2-event free tier. Recorded for Day 8.

### Refinement: three subscription scopes, not two

A two-value flag cannot express the three intents users actually have. So:

| `subscription_scope` | Means | Typical use |
|---|---|---|
| `THIS_EVENT` | **only** this one | "just this concert date", "just episode 5" |
| `THIS_AND_FUTURE` | this one **and every later sibling in the same container** | "from episode 5 onwards" |
| `INCLUDE_CHILDREN` | the **whole container** | "the entire series", "the whole season", "the tour" |

This is what your rule ("subscribing to an episode automatically covers the next one") really means — but
made **explicit at subscribe time** instead of happening invisibly. The behaviour is the same; the user
consents to it. Silent auto-subscription is the kind of thing users discover later and resent.

---

## 13. Lifecycle: when does an event stop mattering

`status` answers *"what do we know about the release?"*. It does **not** answer *"is this still worth
refreshing, and may a user still subscribe?"*. Those are different questions, so they get different
columns.

| Column | Type | Constraints | Meaning |
|---|---|---|---|
| `active_until` | `timestamptz` | NULL | the point after which this event no longer needs refreshing or subscribing. `NULL` = not yet determined → treat as active |

How it is set:

| Event kind | `active_until` |
|---|---|
| one-off (film, game, anime, single concert) | `release_date + grace` (grace ≈ 7 days, to catch post-release corrections) |
| container (series, season, tour) | the **last child's** date `+ grace`; `NULL` while any child is still upcoming |
| cancelled | `now()` |

### The rule you asked for, made safe

> **"An event goes cold when its release date passes."** — nearly right, but *"release date passed"*
> is not the same as *"finished"*.

Two cases where the simple version fails:

- **An airing series** — the first episode airs, the parent's `release_date` passes, and 9 weeks of
  episodes are still to come. Going cold there would blind you to the rest of the season.
- **A postponed concert** — the date passes, then the venue reschedules. Going cold means you never
  see the change, and the user turns up to a closed door.

So the rule becomes: **cold when `active_until` has passed**, and `active_until` already accounts for
"still airing" and the grace period.

### And the cost-leak fix you were after

| Situation | Behaviour |
|---|---|
| Event is active | normal tiers: `HOT` / `WARM` / `COLD` |
| Event past `active_until` | **`ARCHIVED`** — never polled again, regardless of any admin setting |
| Admin marks an event `HOT` | **capped** — it cannot outlive `active_until + grace` |
| Admin marks a `HOT` event with no end date | still auto-archives at `active_until`, so the leak is closed by the business rule, not by remembering to clean up |

**Yes — that fixes it.** Not by discipline, but by making "finished" a fact the system can compute.

**Subscription consequence:** once an event is archived, **new subscriptions are refused** ("this has
ended"). Existing subscribers get one final notice ("this event has ended") and the subscription is
archived — **never silently dropped**.

---

## 14. Data-quality reports (the "please check this" flag)

The feature you described: a user marks a wrong-looking event, adds a comment, and an admin reviews it.
It is **not** a refetch — it costs nothing upstream, and it carries *what* looks wrong, which a refetch
never would.

| Column | Type | Constraints | Meaning |
|---|---|---|---|
| `id` | `uuid` | PRIMARY KEY | |
| `event_id` | `uuid` | NOT NULL, FK → `events(id)` ON DELETE CASCADE | which event |
| `reporter_id` | `uuid` | NOT NULL, **no foreign key** | who flagged it (identity owns users) |
| `comment` | `varchar(1000)` | NULL | free text: "the date moved to March" |
| `status` | `varchar(16)` | NOT NULL, DEFAULT `'OPEN'`, CHECK IN (`'OPEN'`,`'RESOLVED'`,`'DISMISSED'`) | the admin queue state |
| `resolved_by` | `uuid` | NULL | which admin handled it |
| `resolved_at` | `timestamptz` | NULL | when |
| `created_at` | `timestamptz` | NOT NULL, DEFAULT `now()` | |

**Indexes**

| Index | Purpose |
|---|---|
| UNIQUE (`event_id`, `reporter_id`) **WHERE `status = 'OPEN'`** | one open report per person per event — the anti-spam rule, same partial-index trick as `subscriptions` |
| (`status`, `created_at`) | the admin queue: oldest open report first |
| (`event_id`) | "how many people flagged this?" → sort the queue by signal strength |

**Rules**
1. Reporting never triggers a fetch. It creates a work item.
2. The admin queue is ordered by **open report count**, so the most-disputed events rise to the top.
3. Resolved and dismissed reports are **kept, never deleted** — they are the audit trail of data quality.
4. Only paid/allowed roles may report, and only once per event while open — otherwise it is a spam channel.

---

## 15. Where this database runs

Short answer: **Amazon RDS for PostgreSQL, in a private subnet, with a separate one-off task that runs the migrations before each deploy.**

| Environment | Where the database lives | Why |
|---|---|---|
| **Local dev** | PostgreSQL in Docker Compose | free, disposable, and the *same version* as production — so "works locally, fails in prod" almost disappears |
| **CI** | **Testcontainers** — a fresh container per test run | tests must not share a database, or they interfere with each other and flake |
| **Dev / staging** | RDS `db.t4g.micro`, single-AZ, 7-day backups | cheap; correctness matters here, uptime does not |
| **Production** | **RDS Postgres, Multi-AZ**, automated backups + point-in-time recovery, deletion protection ON | failover in ~1–2 minutes; restore to any second in the retention window |

### What we are deliberately NOT doing

| Option | Why not |
|---|---|
| Postgres in a container (ECS/Kubernetes) | container storage is temporary. A restart, a reschedule or a node failure can take the data with it. **Never for the system of record** |
| Self-managed Postgres on EC2 | you then own backups, failover, patching, disk monitoring and 3am restores. That is a job, not a setting |
| One giant database for all services | that is the **distributed monolith** — a schema change breaks four services at once (`plan.md` §3.1) |
| DynamoDB | we need joins and multi-row transactions; a different data model for a different problem |

### Network placement — the part that matters most

```
Internet
   |
 [ public subnet ]     ALB only
   |
 [ private app subnet ]     ECS/EKS tasks — no public IP
   |
 [ private data subnet ]    RDS + ElastiCache — no route to the internet at all
```

- RDS is **never publicly accessible** and has **no public IP**.
- The security group allows port 5432 **only from the application's security group** — not from the VPC, not from `0.0.0.0/0`.
- No bastion host. If an engineer needs `psql`, use **SSM Session Manager port-forwarding** or a temporary tunnel — no permanently open port to the internet.

### Multi-AZ vs read replica — two different jobs

| | Multi-AZ | Read replica |
|---|---|---|
| Purpose | **availability** | **read scaling** |
| Standby readable? | **no** — it just stands there until needed | yes |
| Failover | automatic, ~1–2 min | manual promotion |
| Costs | double the instance | another instance |

They are not alternatives — production can have both. Multi-AZ buys uptime; a replica buys throughput for the read-heavy catalogue.

### Connections — the trap nobody counts

```
5 services × 6 tasks × 10 pooled connections = 300 connections
A small RDS instance allows roughly 85–200
```

So: keep HikariCP pools **small** (5–10 per instance, not 50) and put **RDS Proxy** in front so hundreds of app connections multiplex onto a few database ones. Without this, a quiet Tuesday becomes an outage the day you scale the service.

### How migrations reach production

**Never on application startup.** With six instances booting together, six Flyway runs race for the same lock, and a failing migration then blocks every instance from starting.

Instead, in the deploy pipeline, in this order:

| Step | Action |
|---|---|
| 1 | Run Flyway as a **separate one-off task** (ECS run-task / init container / CI step) |
| 2 | Only if it succeeds, roll out the new service tasks |
| 3 | Old and new code must both work against the new schema → **expand / contract** (`plan.md` Day 18.6) |

### Secrets

The master password lives in **AWS Secrets Manager**, rotated, and injected into the task at runtime via the IAM execution role — never in the repository, never in an image, never in an environment file. (Stronger option for later: **IAM database authentication**, where there is no password at all.)

### Cost ballpark (for `plan.md` §3.3's budget warning)

| Item | Rough monthly |
|---|---|
| `db.t4g.micro`, single-AZ | ~$15 |
| Same, **Multi-AZ** | ~$30 |
| 20 GB gp3 storage | ~$2–3 |
| Backups | free up to the storage size |
| RDS Proxy | ~$0.015 per vCPU-hour |

A small production database is roughly **$35–60/month** — one of the cheaper parts of the AWS bill, and one of the least worth self-hosting.







