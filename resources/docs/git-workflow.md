# EXR — Git & Branching Strategy

> **Model: trunk-based development.** One long-lived branch. Every merge to it is
> releasable. CI is the gate, not a reviewer's opinion.

> **Every explanation of this material in chat carries a picture** (rule 23, `plan.md` §1): ASCII in
> a plain fence, commits as short labels, branch names on the left, `master` on the right, the
> reader's own hashes. The sentence says what happened; the diagram says where.

## 1. The model in one picture

```
feature/add-v2-tsvector ──┐
feature/fix-flyway-check ─┼── PR + green checks ── squash merge ──▶  master  ──▶  tag v0.1.0  ──▶ deploy
feature/gate-lab ─────────┘                                            │
                                                                       └── always releasable
```

Work is a **short-lived branch** (hours to a day or two), not a place to live. It
reaches `master` only through a pull request whose checks are green, and it is
deleted the moment it lands.

## 2. Branches

| Branch | Purpose | Cut from | Merges into | Lifetime |
|---|---|---|---|---|
| `master` | the product; always releasable | — | — | forever |
| `feature/<slug>` | one task, one concern | `master` | `master` via PR (squash) | hours – 2 days |
| `fix/<slug>` | a defect, including production hotfixes | `master` | `master` via PR (squash) | hours |
| `docs/<slug>` | documentation-only change | `master` | `master` via PR (squash) | hours |

Naming: `<type>/<kebab-slug>` — `feature/catalog-v2-tsvector`, `fix/null-release-date`.
The type matches the commit types in §7, so branch and commit tell the same story.

**There is no `development` branch and no `release/*` branch.** See §8 for why, and
for the conditions under which that decision would be revisited.

The conventional four-role model is `master` (production), `development` (staging),
`feature/*` and `hotfix/*`. We keep **three of the four roles and move the fourth
somewhere else**: the staging role is played by a **staging environment** (a second
deployment fed by `master`), not by a branch, and production is reached by a **tag**
plus an approval. The rule behind it: a **branch** holds work in progress, an
**environment** holds a running copy. Mixing the two roles into one branch is what
turns `development` into a second trunk that has to be merged both ways.

## 3. The rules, and the reason for each

| # | Rule | Why |
|---|---|---|
| 1 | `master` is always releasable | if it is, "when do we ship?" stops being a meeting |
| 2 | **No direct pushes to `master`** | bypassing the gate once teaches everyone that the gate is optional |
| 3 | A PR needs the check **`Build and test (Java 21)`** green | the check is the reviewer's evidence, not their gut |
| 4 | **Squash merge** (one commit per PR) | `git revert <sha>` undoes exactly one task; `git bisect` finds it fast |
| 5 | Delete the branch after merge | a merged branch is a lie — it suggests work in progress |
| 6 | Keep branches under ~2 days | the longer it lives, the bigger the merge and the review |
| 7 | **Flag, don't branch**, for unfinished work | a half-built feature hidden in a branch never integrates; behind a flag it does |
| 8 | Rebase on `master` before asking for review | your PR shows only your change, not the whole history |
| 9 | Releases are **tags on `master`** | a tag names an exact, tested commit; a branch does not |
| 10 | Red on `master` → **revert first**, investigate second | the trunk belongs to everyone; a fix can wait, a blocked team cannot |

**How rule 7 actually looks in this repository.** The value lives in configuration, not in
code: `exr.features.<name>: false` in `application.yml`, and the bean that belongs to the
feature carries `@ConditionalOnProperty(name = "exr.features.<name>", havingValue = "true")`.
Turning the feature on is therefore a configuration change and a restart, never a merge —
which is the whole reason it beats a branch. **Flag debt:** a flag that has been off for a
year is deleted together with the code behind it, or the switch quietly becomes permanent.

## 4. Branch protection — enabled on `master` (2026-11-13)

For `master` only: one **ruleset**, because there is one long-lived branch. A classic rule works
too, but a ruleset starts with an **empty bypass list**, which is the property that matters (§10).

- **Require a pull request before merging** — 0 approvals while solo, 1 when a second dev joins.
- **Require status checks to pass** — **`Build and test (Java 21)`**, exactly as the `name:` of the
  job in `ci.yml`. GitHub offers a check in that list **only after it has reported at least once**,
  so protection is enabled *after* the first CI run, never before.
- **Restrict deletions** · **Block force pushes** · **squash** merges only.
- **Bypass list: empty.** Put *Repository admin* there and the admin can merge red — the gate becomes
  a suggestion.
- Optional: require linear history (redundant with squash-only, harmless).

An **unrequired** check is decoration: GitHub shows the red X and leaves the merge button enabled.
That is not a theory — PR #3 merged exactly that way (§10).

## 5. When it goes red

| Where | Who acts | What they do |
|---|---|---|
| PR branch | the author, within minutes | fix on the **same** branch and push again; the PR stays open. Never merge red, never "just this once" |
| `master` after a merge | whoever notices — usually the next pusher | `git revert <merge-sha>` immediately, then fix on `fix/*`; no debugging on the trunk |
| A flaky test | the author of that test | quarantine or fix it **the same day** — a flaky suite destroys rule 3, because people start ignoring red |
| PR conflict | the author of the newer PR | `git fetch origin && git rebase origin/master`, resolve, `--force-with-lease` **the branch** only |

The failure path was proven once, deliberately (§10): PR #3 merged while its check was still
running, `master` went red, and a revert PR took it back. A gate nobody has seen fail is
decoration.

## 6. Releases

- SemVer tag on `master`: `v0.1.0`, `v0.1.1`, `v0.2.0`; annotated, not lightweight.
- The tag is the deploy unit — later the pipeline deploys a tag, never a branch.
- The changelog is derived from Conventional Commits (§7), so commit discipline pays for itself.
- A hotfix is a `fix/*` branch plus a new patch tag. **Nothing has to be back-merged**, which is the
  single biggest saving versus a two-branch model.

## 7. Commit messages — Conventional Commits

`<type>(<scope>): <imperative summary>` — subject under ~72 characters, no full stop; the *why*
goes in the body via a second `-m`.

| Type | Use for |
|---|---|
| `feat` | new behaviour a user can notice |
| `fix` | a defect repaired |
| `refactor` | same behaviour, better shape |
| `test` | tests only |
| `docs` | documentation only |
| `build` / `ci` | Maven, wrapper, workflows |
| `chore` | housekeeping with no product effect |

Real examples from this repo: `feat(catalog): map Source and Event; prove the foreign key with a test`
· `ci: drop the postgres service container; the tests start their own` · `docs(plan): record the verified V1 migration`.

## 8. What we deliberately did not choose

| Rejected | Why | When it becomes right |
|---|---|---|
| **GitFlow** (`develop` + `release/*`, long-lived) | two sources of truth drift; every hotfix must be applied twice; releases become one big unreviewable merge; integration feedback moves off the branch that actually ships | a staged environment with a manual QA gate, or a release that needs a code freeze for days |
| **Long-lived feature branches** | merge hell, and every integration surprise arrives at once, late | never — use a flag (§3 rule 7) |
| **Direct pushes to `master`** | removes the only automatic evidence that the change is safe | never |

Trunk-based is not "careless". It moves the discipline from *branch management* to
*test reliability and small changes*, which is where the modern payoff comes from.
The cost is real: a flaky test suite makes the whole gate unaffordable.

## 9. How CI maps to this model — `.github/workflows/ci.yml`

- Triggers on `push` to `master` (protects the trunk) **and** on `pull_request` targeting `master`.
  For a `pull_request` event the `branches` filter applies to the **base** branch, so every
  `feature/*` PR is built without listing feature branches.
- `concurrency` cancels the in-flight run for the same ref — nobody waits on a superseded commit.
- `./mvnw -B -ntp verify` is the gate: tests and packaging in one command.
- On failure, `surefire-reports` is uploaded as an artifact, so the report survives the scrolling log.
- No database service container — every persistence test extends `AbstractIntegrationTest` and
  starts its own `postgres:16` through Testcontainers.

## 10. The lab — the gate bit, and what it taught (done 2026-11-13)

1. `git switch -c feature/gate-lab`, add
   `src/test/java/com/kodegravity/exr/catalog/lab/DeliberateFailureTest.java` — a plain JUnit test
   asserting `3 == 4`, so only the pipeline is under test (no Spring, no Docker). Run it locally
   first: **red**, `expected: 4 but was: 3`.
2. Commit, push, open the PR into `master`. Run **#22** starts on the PR — a `pull_request` event
   against base `master` is enough; no feature branch has to be listed in `ci.yml`.
3. **The merge went through while #22 was still running.** Nothing stopped it, because no check was
   **required** yet. That merge wrote `914d4fb` to `master`, and its own `push` run **#23** failed:
   the trunk was red.
4. Enable the ruleset (§4). It has to come *after* a CI run, because GitHub cannot offer a check in
   the required list until that check has reported once.
5. Take the red out with a **revert**, not a fix: `git switch -c fix/revert-gate-lab`,
   `git revert --no-edit 914d4fb`, push, PR #4, squash-merged as `c315fc3` — trunk green again,
   −39 lines, and the bad commit still visible in history.
6. Delete both branches. `feature/gate-lab` must never be merged, so its remote copy goes too:
   `git push origin --delete feature/gate-lab`.

**What the lab proved, one line each.** A check that is not required cannot block anything. Branch
protection can only name a check that has already reported. A revert is a normal commit, so it passes
the same gate it repairs. And `git branch -d` cannot tidy up after a squash merge — the content is on
`master`, the commits are not — so the deletion needs `-D`.

## 11. The 30-second interview answer

> "Trunk-based. `master` is always releasable and protected — nothing lands without a green PR.
> Work lives on short-lived feature branches, a day at most, squash-merged so one commit is one
> task and one `revert`. Unfinished features hide behind flags instead of branches. Releases are
> tags, and we deploy the tag. The trade-off is that the test suite has to stay fast and
> non-flaky, because red must keep meaning something."

---

**Related:** `plan.md` §0 (resume point) · `plan.md` §11.1 (revision history, rev 27) ·
`index.md` (documentation entry point) · `.github/workflows/ci.yml` (the gate itself).
