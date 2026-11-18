# How is the user row updated on a login attempt?

> **What this file answers.** A login is just an HTTP request, and a *failed* one changes the
> database. This page shows which class writes to `users`, when it writes, and why a correct
> password on a locked account changes nothing.
> **Written for:** Day 4.3 (brute-force lockout) in `identity-service`. It is a map, not a lesson —
> read it when the flow has slipped out of memory.
> **Code it describes:** `User`, `LoginAttemptService`, `AuthenticationEventsListener`,
> `DbUserDetailsService`, `UserRepository`, `V2__login_lockout.sql`.

---

## 1. The one-line answer

Spring Security **announces** every authentication result as an event. A small listener
(`AuthenticationEventsListener`) hears the bad ones and passes the email to
`LoginAttemptService`. That service loads the `User` row, asks it to count the failure, and the
row is saved in **its own transaction** — so the counter survives even though the login request
itself failed.

The plainest way to hold it: the login request is not a transaction. It is a knock on the door.
The knock fails, and a separate note is written on the door.

Where that knock happens inside the request — which filter runs, in what order — is **§12**
(two pictures: the filter stack, and what the login door does with the header).

---

## 2. The cast — who touches what

| Class / method | File | Its one job |
|---|---|---|
| `AuthenticationEventsListener.onBadCredentials` | `infra/AuthenticationEventsListener.java` | hears "the password was wrong", passes the email on |
| `AuthenticationEventsListener.onSuccess` | same file | hears "the password was right", passes the email on |
| `LoginAttemptService.recordFailure` | `application/LoginAttemptService.java` | opens a transaction, loads the row, counts one failure |
| `LoginAttemptService.recordSuccess` | same file | opens a transaction, loads the row, zeroes the counter |
| `User.recordFailedLogin` | `domain/User.java` | the rule itself: count, and lock at the threshold |
| `User.recordSuccessfulLogin` | same file | clears the counter and the deadline |
| `User.isTemporarilyLocked` | same file | "is the deadline still in the future?" |
| `DbUserDetailsService.loadUserByUsername` | `infra/DbUserDetailsService.java` | turns a row into a `UserDetails`, including `accountLocked` |
| `UserRepository.findByEmail` | `infra/UserRepository.java` | `where lower(email) = lower(?)` — the index-friendly lookup |
| `RestAuthenticationEntryPoint.commence` | `infra/RestAuthenticationEntryPoint.java` | writes the 401 body the client actually sees |
| `DaoAuthenticationProvider` | Spring Security, not our code | reads the `UserDetails` flags and **throws the refusal** (`LockedException`, `DisabledException`) *before* comparing the password |

**Nobody else writes to `users` on a login.** `IdentityController` and `AuthController` are not in
this path at all.

---

## 3. The three columns that carry the story

| Column | Type and rule | Written by | Meaning |
|---|---|---|---|
| `failed_attempts` | `smallint NOT NULL DEFAULT 0`, `CHECK (failed_attempts >= 0)` | `User.recordFailedLogin`, `User.recordSuccessfulLogin` | consecutive failures since the last success |
| `locked_until` | `timestamptz`, **NULL = not locked** | the same two methods | logins are refused until this instant |
| `status` | `varchar(16)`, `ENABLED` / `LOCKED` / `SUSPENDED` | registration and admins | account standing |

**The mistake almost everybody makes:** thinking the automatic lockout sets
`status = 'LOCKED'`. It does not. The lockout writes `locked_until` and leaves `status` alone.
`status = 'LOCKED'` is a state a human or another feature would set; the counter's lock is a
**deadline that expires by itself**, so no background job is needed to reopen the door.

A permanent flag would be a denial-of-service lever: any stranger could shut any account for ever
by typing wrong passwords. A deadline costs the attacker a wait and costs the user the same wait.

**The Java field is `short`, not `int`, and that is not a style choice.** The column is
`smallint`, and `ddl-auto: validate` compares the JDBC type **code**, not the column's name or
width. A Java `int` announces `INTEGER`, so the app refuses to start with
`found [int2 (Types#SMALLINT)], but expecting [integer (Types#INTEGER)]`. `short` maps to
`SMALLINT` with no extra hint. `getFailedAttempts()` still returns `int`, so nothing outside the
entity knows.

---

## 4. Flow — a wrong password

```
 1  client   GET /api/me   Authorization: Basic <wrong password>
                 |
                 v
 2  BasicAuthenticationFilter
      -> DbUserDetailsService.loadUserByUsername("you@example.com")
           -> UserRepository.findByEmail           [read users]
 3  DaoAuthenticationProvider
      -> account locked?  no  -> continue
      -> BCrypt compare       -> FALSE
      -> throw BadCredentialsException
 4  ProviderManager publishes
      AuthenticationFailureBadCredentialsEvent     (Spring's event, not ours)
 5  AuthenticationEventsListener.onBadCredentials(event)
      -> LoginAttemptService.recordFailure("you@example.com")
           -> @Transactional      [its OWN transaction opens here]
                -> findByEmail                    [read users again]
                -> User.recordFailedLogin(5, 15 min)
                     failed_attempts  n -> n+1
                     when n+1 reaches 5  ->  locked_until = now + 15 min
 6  COMMIT                                         [UPDATE users]
 7  RestAuthenticationEntryPoint writes 401 problem+json
```

Three details in that picture are worth keeping:

1. **The count is written at step 6, after the refusal.** A failed request never rolls the counter
   back, because the counter was never part of that request's transaction.
2. **The same email is looked up twice** (steps 2 and 5). That is not waste: step 2 answers "may
   this person log in", step 5 answers "should this failure be recorded", and they run in two
   different objects with two different owners.
3. **An unknown email changes nothing.** `recordFailure` uses `ifPresent`, so a typo'd address
   simply matches no row. The client still gets one 401, so it cannot learn which addresses exist.

---

## 5. Flow — the CORRECT password on a locked account

This is the case people find surprising: nothing is written. The password is never even checked.

```
 1  client   GET /api/me   Authorization: Basic <CORRECT password>
                 |
                 v
 2  DbUserDetailsService.loadUserByUsername(...)
      -> build UserDetails
           status              == SUSPENDED?  no
           status              == LOCKED?     no
           isTemporarilyLocked(now)?          YES
      -> accountLocked = true
 3  DaoAuthenticationProvider
      -> pre-authentication check sees accountLocked
      -> throw LockedException      (the BCrypt comparison never runs)
 4  AuthenticationFailureLockedEvent   -> nobody listens to it
      failed_attempts 5, locked_until unchanged
 5  401 problem+json
```

Why the missing write is deliberate: counting failures on an account that is **already** shut
would push the deadline forward on every attempt, and a stranger could keep somebody locked out
for ever by knocking once a minute. So `AuthenticationEventsListener` listens only for
`AuthenticationFailureBadCredentialsEvent`. Locked and disabled accounts produce different events,
and those are ignored on purpose.

The cost is honest: the account stays locked for the full window even if the real owner turns up
with the right password. That is the price of making the lock unpickable from outside.

---

## 6. How the counter goes back to zero

```
 PATH A — the lock expired, then a WRONG password

   locked_until is in the past
        -> recordFailedLogin notices the deadline has passed
        -> failed_attempts reset to 0 FIRST
        -> then +1                             => failed_attempts = 1

 PATH B — a CORRECT password on an account that is not locked

   authentication succeeds
        -> AuthenticationSuccessEvent
        -> LoginAttemptService.recordSuccess
        -> User.recordSuccessfulLogin
        -> failed_attempts = 0, locked_until = NULL
```

Without path A the counter would keep climbing across unrelated days, and a user who mistyped once
a week would be locked out for no reason. The expired lock is treated as "that attack is over", so
the next failure is failure number **one**, not failure number six.

---

## 7. The row, moment by moment

| Moment | `failed_attempts` | `locked_until` | What wrote it |
|---|---|---|---|
| account created | `0` (column default) | `NULL` | `UserAccountService.register` |
| 1st wrong password | `1` | `NULL` | `User.recordFailedLogin` |
| 2nd, 3rd, 4th wrong | `2`, `3`, `4` | `NULL` | same |
| **5th wrong password** | **`5`** | **`now + 15 min`** | same — the threshold and the deadline land in one UPDATE |
| correct password, still locked | `5`, unchanged | unchanged | nothing — `LockedException`, no listener |
| correct password, lock expired | `0` | `NULL` | `User.recordSuccessfulLogin` |
| wrong password, lock expired | `1` | `NULL` | `User.recordFailedLogin` (path A reset first) |

Two things the UPDATE also carries, for free: `version` goes up by one (`@Version`, optimistic
locking) and `updated_at` is restamped by `@PreUpdate`. You can watch both in the row.

---

## 8. Why it is done with an event, not with a line in the controller

Putting `loginAttempts.recordFailure(...)` in a controller sounds simpler. It stops working the
moment another door exists:

- **Today:** authentication happens in `BasicAuthenticationFilter`, before any controller method.
  A controller would never run for a failed Basic login, so the counter would never move.
- **Day 5:** the same `/api/me` is reached through a JWT filter. No controller sees a token that
  failed to parse.
- **Day 6:** a social-login callback and a future `POST /api/auth/login` are two more doors.

An `@EventListener` on Spring Security's own event bus sits downstream of **all** of them, so the
counting rule is written once. The rule itself still lives in `User` and `LoginAttemptService` — a
listener is plumbing, and plumbing should not hold policy.

**One detail that is easy to miss:** the `@Transactional` on `recordFailure` is what makes any of
this persist. `UserRepository.findByEmail` runs in its own short transaction, so once it returns
the `User` is **detached**. Changing a detached object writes nothing at all — no error, no UPDATE,
just a counter that never moves. The service-level transaction opens *before* the lookup, keeps the
object managed, and flushes the change at commit.

---

## 9. What this does NOT stop

- **Credential stuffing** — one guess per account across a million accounts. Every account records
  exactly one failure, nothing locks, and the attacker learns which pairs work. The answers are
  per-IP rate limiting at the gateway (Day 7.4) and a breached-password blocklist, not a
  per-account counter.
- **The lockout used as an attack** — deliberately locking a known user out in 15-minute stretches.
  Nothing in this code prevents it; only a shorter window or a CAPTCHA challenge does.
- **A wrong password sent to a public path.** `BasicAuthenticationFilter` authenticates before the
  authorization rule is consulted, so bad credentials on `/api/public/ping` *do* increment the
  counter. Those are real failed attempts and should count.

Say all three out loud in an interview. Lockout is one layer, not the answer.

---

## 10. Quick reference — method, lines, purpose

| Method | Lines | What it does |
|---|---|---|
| `User.recordFailedLogin(int maxAttempts, Duration lockFor)` | `User.java` 158–170 | resets an expired run, then counts; sets `locked_until` at the threshold |
| `User.recordSuccessfulLogin()` | `User.java` 173–176 | `failed_attempts = 0`, `locked_until = NULL` |
| `User.isTemporarilyLocked(Instant now)` | `User.java` 179–181 | `locked_until != null && locked_until.isAfter(now)` |
| `User.getFailedAttempts()` | `User.java` 183–185 | returns `int`, widened from the `short` field |
| `LoginAttemptService.MAX_ATTEMPTS` | `LoginAttemptService.java` 40 | `5` |
| `LoginAttemptService.LOCK_FOR` | `LoginAttemptService.java` 47 | `Duration.ofMinutes(15)` |
| `LoginAttemptService.recordFailure(String email)` | `LoginAttemptService.java` 56–60 | `@Transactional`, `findByEmail` + `ifPresent(recordFailedLogin)` |
| `LoginAttemptService.recordSuccess(String email)` | `LoginAttemptService.java` 63–66 | `@Transactional`, `findByEmail` + `ifPresent(recordSuccessfulLogin)` |
| `AuthenticationEventsListener.onBadCredentials(...)` | `AuthenticationEventsListener.java` 39–42 | the only failure type that is counted |
| `AuthenticationEventsListener.onSuccess(...)` | `AuthenticationEventsListener.java` 44–47 | zeroes the counter |
| `DbUserDetailsService.loadUserByUsername(String)` | `DbUserDetailsService.java` 58–79 | sets `disabled` and `accountLocked` on the `UserDetails` |
| `UserRepository.findByEmail(String)` | `UserRepository.java` 40–41 | `lower(email) = lower(:email)` |
| `V2__login_lockout.sql` | migration 24–30 | the two columns and the `CHECK` |

Paths above are relative to
`services/identity-service/src/main/java/com/kodegravity/exr/identity/`, and the migration is under
`services/identity-service/src/main/resources/db/migration/`. Line numbers were correct on
2026-11-17; if a method moves, the table is the thing to fix.

---

## 11. The 20-second interview version

> "Failed logins are counted on the user row: a `failed_attempts` counter and a `locked_until`
> timestamp. Spring Security publishes an authentication event; a listener turns the
> bad-credential ones into a service call; the service does the write in its own transaction, so
> the counter survives the failed request. The fifth failure sets a 15-minute deadline, and the
> deadline expires by itself, so a stranger cannot lock an account permanently. Attempts against an
> already-locked account are not counted, otherwise knocking would extend the lock. It stops a slow
> guesser, not credential stuffing — that needs per-IP rate limiting."

---

## 12. The request itself — where the control goes

Sections 1–11 describe **what** is written. This section shows **where** the code runs while the
request is alive. One sentence first: our correlation filter, then fifteen security filters, then
the controller — and the login happens in one of the fifteen.

### 12.1 The filter stack, before any controller exists

```
Postman  GET /api/me     Authorization: Basic base64(email:password)
   |
   v
Tomcat worker thread  ->  the bytes become an HttpServletRequest
   |
   v
CorrelationIdFilter           ours; order = HIGHEST_PRECEDENCE, so it runs FIRST
   |                          reads or mints X-Correlation-Id, puts it in the MDC
   v
FilterChainProxy              ONE servlet filter (order -100) that holds the 15 below
   |  SecurityContextHolderFilter .. the context starts EMPTY; stateless, nothing restored
   |  BasicAuthenticationFilter .... THE LOGIN DOOR — see 12.2
   |  ExceptionTranslationFilter .. turns a later "not allowed" into our 401 body
   |  AuthorizationFilter ......... the rule: /api/public/** and /api/auth/** open, the rest closed
   v
DispatcherServlet  ->  @GetMapping("/api/me")  IdentityController.me(Authentication)
   |
   v
Jackson  ->  200 {"name":"...","authorities":"[ROLE_FREE_USER]"}
```

### 12.2 Inside the door — what `BasicAuthenticationFilter` does

```
no Authorization header
   |
   +--> nothing happens here; the chain continues
        AuthorizationFilter asks the rule
        /api/me  needs identity -> AccessDenied -> entry point -> 401
        /api/public/ping is open -> reaches the controller

Authorization header present
   |
   decode "email:password", build a UsernamePasswordAuthenticationToken
   |
   v
ProviderManager  ->  DaoAuthenticationProvider
   |
   +-- 1. loadUserByUsername .... DbUserDetailsService
   |                             UserRepository.findByEmail
   |                             SELECT ... FROM users WHERE lower(email) = lower(?)
   +-- 2. account state ........ locked or suspended?  refused HERE, before the password
   +-- 3. password .............. BCryptPasswordEncoder.matches(typed, stored hash)
   |
   +-- accepted --> publish AuthenticationSuccessEvent
   |                -> listener -> recordSuccess   (UPDATE: counter 0, deadline NULL)
   |                SecurityContextHolder = that Authentication
   |                chain CONTINUES towards the controller
   |
   +-- rejected --> publish AuthenticationFailureBadCredentialsEvent
                    -> listener -> recordFailure   (UPDATE: counter +1, maybe the deadline)
                    entryPoint.commence -> 401 application/problem+json + correlationId
                    CHAIN STOPS. The controller is never reached.
```

### 12.3 Four consequences worth carrying away

1. **There is no `/login` endpoint.** Authentication happens in a filter, before Spring MVC is
   involved. The only controller in the auth story is `POST /api/auth/register`, and it is
   `permitAll()`.
2. **The database write also happens inside the filter.** The events are published synchronously,
   so `recordFailure` has committed before the 401 is written — which is why DBeaver already shows
   the new number when you look.
3. **A `permitAll()` path still authenticates.** A header sent to `/api/public/ping` is examined by
   the door: bad credentials there count as failures, and good ones identify the caller.
4. **Every request pays for BCrypt.** Nothing is remembered between requests, so the digest is
   compared again on each one. Tens of milliseconds is fine once at login and wasteful on every
   call — the reason Day 5 replaces Basic auth with a signed token.



