# Git & GitHub recap — a planned session for a large team

> **Status:** planned, not started. This file is the plan and the coverage checklist for the agent.
> **Why it exists (user request, rev 34):** git is the one tool we use every single session, and it was learned in pieces — a revert here, a squash there. This session puts the pieces in order, on a **throwaway sandbox** with a few plain text files, and walks the workflow a company with a large team actually runs.
> **The user's own words:** the ASCII commit diagram *"is easier to understand git this way"*. So the diagram is not decoration here — it is the teaching method.

---

## 1. How every step in this session runs

1. **One step per turn.** The agent states the step's goal in one sentence, hands over the command(s) as **one physical line each**, and waits.
2. **The user runs it** (rule 10, rule 18). The agent never commits, pushes or clones on the user's behalf.
3. **The user pastes the output.** The agent reads only the 2–3 lines that matter.
4. **The agent draws the tree after the step** — always, even when nothing moved, because "nothing moved" is the lesson in half the steps.
5. **Two or three questions follow**, then the agent stops (rule 19). A wrong answer is fixed before the next step, never waved through.
6. **No new stage until the previous diagram can be explained back** in the user's own words.
7. **Plain words first, the term in brackets** (rule 14) — *"the shelf where unfinished work sits (a **branch**)"*.

---

## 2. The diagram style, and the legend

Every step ends with a picture in a plain fence — never mermaid, so it renders the same in chat, in the terminal and on GitHub. The user's own names are used, never a generic `A → B → C`.

```
legend
  ●────●──●       commits, oldest on the left
  main            the long-lived branch, written on the left of its line
  up/main         the same branch as it exists on GitHub (the remote)
  (work)          the working tree: edits not yet committed
  └ ...           a note about a position, drawn under the point it describes
```

A worked example — the `git status` picture the user has already seen, in the same style this session uses:

```
now:      main ●──●──● 648775a
                 └ working tree: 5 modified, 3 new — nothing committed

after:    main ●──●──● 648775a ─────────────────● squash-merged
                    \                          /
                     feature/lab ●──● ●
```

Two pictures whenever something **moved** (a push rejected then fixed, a rebase, a force-push, a squash). One picture can never show a move.

---

## 3. The sandbox

A **separate** playground repository, so the real project is never at risk:

- one GitHub repo, `git-recap-lab`, created empty;
- **two clones on disk**, `dev-a` and `dev-b`, treated as **two engineers** — this is how a conflict becomes real instead of theoretical;
- four plain files, deliberately tiny so a conflict fits on one screen: `notes.txt`, `todo.txt`, `menu.txt`, `README.txt`;
- no build tool, no code, nothing to compile — the only thing that changes is text, so the only thing being taught is git.

**Why two clones and not two branches:** in one clone, `git switch` hides the most important fact of team work — *the other person's branch only exists on the remote until you fetch it*.

---

## 4. Stage 1 — one engineer, a line of commits

**Goal:** a commit is a snapshot the user *chooses*, and there are three different "undos".

| Step | Goal in one sentence | Command (one line each) | The diagram must show |
|---|---|---|---|
| 1.1 | An empty repo has an empty history | `git clone <url> dev-a` then `git status` | `main` with no commits, `(work)` clean |
| 1.2 | A new file is **untracked** — git has not been told to watch it | create `notes.txt`, `git status` | the file listed under `(work)`, not under `main` |
| 1.3 | `add` moves a change into the **stage** (the shelf of what the next commit will contain) | `git add notes.txt`, `git status` | the file moved from `(work)` to `(stage)` — the picture has three boxes |
| 1.4 | `commit` turns the shelf into a point on the line | `git commit -m "add notes"`, `git log --oneline` | `main ● add notes` — the first dot |
| 1.5 | `diff` and `diff --staged` answer different questions | edit the file, run both | one change visible in `(work)`, none in `(stage)` |
| 1.6 | **A commit is a selection, not a copy of the folder** — two unrelated edits can become two commits | edit two lines, `git add -p`, commit, commit again | two dots, each carrying one change |
| 1.7 | The three undos are not interchangeable | `git restore <file>` · `git restore --staged <file>` · `git revert <hash>` | three pictures: work discarded, shelf emptied, a **new** dot that reverses an old one |

**Questions for 1.7, and why they are the right questions:** *"After a `revert`, how many dots are on the line?"* (memory + consequence) and *"Which of the three undos would you use on a commit that other people already pulled?"* (judgement).

---

## 5. Stage 2 — branches, merge, and the shape of history

**Goal:** a branch is a **label on a commit**, and the label moves forward as commits land.

| Step | Goal in one sentence | Command | The diagram must show |
|---|---|---|---|
| 2.1 | Creating a branch copies nothing — it writes a new label on the **same** commit | `git switch -c feature/menu` | two labels pointing at one dot |
| 2.2 | Commits move only the label you are on | two commits on `feature/menu` | `main` still at the old dot, `feature/menu` two dots ahead |
| 2.3 | A merge **fast-forwards** when `main` has not moved | `git switch main`, `git merge feature/menu` | `main` jumps forward, no new dot |
| 2.4 | A merge makes a **merge commit** when both sides moved | commit on `main` first, then merge | two lines joining in one new dot |
| 2.5 | Deleting a branch deletes a **label**, not the work | `git branch -d feature/menu` | the label gone, the dots still on `main` |

**The distinction to check:** *"We merged and the branch label vanished. Did we lose the commits?"* — the answer is the whole of Stage 2.

---

## 6. Stage 3 — two engineers, one file, one conflict

**Goal:** a conflict is not an error. It is git refusing to guess which of two edits to keep.

| Step | Goal in one sentence | Command | The diagram must show |
|---|---|---|---|
| 3.1 | A second clone is a second person | `git clone <url> dev-b` | `dev-a` and `dev-b` both at the same dot |
| 3.2 | Both people can work without seeing each other | edit `menu.txt` in each clone, one commit each (different branches) | two labels growing apart, `up/main` unchanged |
| 3.3 | `push` publishes a branch; `-u` remembers the pairing | `git push -u origin feature/menu` from `dev-a` | `origin/feature/menu` appears next to `up/main` |
| 3.4 | The first PR merges while the second person is still working | PR on GitHub, squash merge | `up/main` gets one new dot; the squash **hides** the branch's original dots |
| 3.5 | The second push is **rejected** — that is the remote protecting history | `git push` from `dev-b`, twice: `fetch` then `rebase`, and `fetch` then `merge` | before/after pair: the rejected line, then the repaired line |
| 3.6 | A conflict on the same line is resolved by editing the file | merge conflict markers, then `git add` and `git merge --continue` | the two lines joined by a merge dot, conflict resolved |
| 3.7 | The conflict nobody believes: **a file you never touched** | replay the squash-merge trap | `6cc7dc2`-style: a dot that is not an ancestor of the new branch |

**The mishap this stage exists for:** a squash merge hides ancestry, so a branch cut from a **stale** local tip conflicts on files it never edited, and git reports it as **add/add** rather than an edit. That is not a mystery — it is the diagram at 3.7.

---

## 7. Stage 4 — what a large team puts around the branch

**Goal:** the branch is the easy part. The rules around it are what stops 40 people from breaking `main` on a Friday.

| Step | Goal in one sentence | What the user does | The diagram must show |
|---|---|---|---|
| 4.1 | `fetch` moves the remote picture and **nothing else**; `pull` is fetch plus a merge | run `git fetch` then compare with `git pull`, reading `git log` between them | `up/main` moved forward, local `main` unchanged — then both on the same dot |
| 4.2 | A protected branch refuses a direct push | attempt `git push` to `main` | the arrow into `up/main` **blocked**, the PR path drawn instead |
| 4.3 | A required check gates the merge | open a PR with a deliberately broken file | PR box with `check ✗`, `master` untouched |
| 4.4 | A review is a comment on a line, not a rewrite | leave a review comment, push a fix, watch the check go green | same PR, two states side by side |
| 4.5 | Squash merge = one dot on `main`, the branch's history discarded | merge, then `git branch -d` — and watch it **refuse** | `up/main` one new dot, the branch label detached, **empty content** |
| 4.6 | Merge queue: several PRs, merged one at a time, each tested on the current `main` | line up two PRs touching the same file | two PRs queued, one landing, the other rebased and retested |
| 4.7 | `CODEOWNERS` routes the review to the owning team | add a `CODEOWNERS` line for `menu.txt` | PR box with a reviewer auto-assigned |
| 4.8 | A release is a **tag**, not a branch | `git tag -a v1.0.0`, push it | `v1.0.0` as a flag on a dot, `main` unchanged |

**The one rule of thumb for the whole stage:** a team works out of `main`; a branch lives hours, not weeks; and nothing lands without a green check.

---

## 8. Stage 5 — when it goes wrong (the useful half of git)

**Goal:** every one of these is a real Friday afternoon.

| Step | Situation | Command | The diagram must show |
|---|---|---|---|
| 5.1 | A commit went to the wrong branch | `git cherry-pick <hash>` | the same change appearing as a **new** dot on the right line |
| 5.2 | "I lost my branch." | `git reflog`, then `git switch -c rescue <hash>` | the recovered dot re-attached to a label |
| 5.3 | Wrong work is already on `main` | `git revert <hash>` vs a fix-forward PR | two pictures: the extra reversing dot, and the forward fix |
| 5.4 | "Which commit broke it?" | `git bisect start` … `git bisect good/bad` | the line halving each round — 3 pictures, cut, cut, found |
| 5.5 | Work that must wait | `git stash`, switch, `git stash pop` | a holding box outside the line |
| 5.6 | A branch lags and the PR is a mess | `git rebase -i` with squash and reword | before/after: 6 messy dots become 2 readable ones |
| 5.7 | A rebase changed a pushed branch | `git push --force-with-lease` — and why never plain `--force` | before/after: the remote line rewritten, the other person's fetch broken |
| 5.8 | A secret was committed | nothing on the command line — the lesson is that history keeps it, so the secret must be **rotated** | the dot still holding the secret after `revert` |

---

## 9. Coverage checklist (so nothing is skipped)

| Concept / command | Stage | Interview-grade yes/no |
|---|---|---|
| working tree vs stage vs history | 1 | can the user draw three boxes and say what `add` moves between them |
| `commit`, `log --oneline --graph`, `diff`, `diff --staged` | 1 | |
| `restore`, `restore --staged`, `revert`, `reset --hard` | 1 | can say which one rewrites history |
| `switch -c`, `merge` (fast-forward vs merge commit), `branch -d` | 2 | can say why the label disappears but the commits do not |
| `clone`, `push -u`, `fetch` vs `pull` | 3, 4 | |
| conflict resolution, add/add conflict from a stale tip | 3 | can explain why the squash caused it |
| pull request, review, required check, protected branch, merge queue | 4 | can describe the gate end to end |
| `CODEOWNERS`, `tag`, release off a tag | 4 | |
| `cherry-pick`, `reflog`, `bisect`, `stash`, `rebase -i`, `force-with-lease` | 5 | |
| trunk-based vs GitFlow | 5 | can justify the choice for a 40-person team and for a 4-person team |

---

## 10. Two incidents from our own project to replay (they are not hypothetical)

1. **The squash-merge ancestry trap (rev 30).** Three commits became `86c9f58`; the next branch was cut from the **old local tip**, so `86c9f58` was not its ancestor and the shared commit `6cc7dc2` had no `git-workflow.md` at all — git called it add/add. The fix is a habit, not a command: `git fetch` first, then branch from `origin/master`.
2. **`git branch -d` refused after a squash merge (rev 32).** The content is on `master`; the commits are not. `-d` is a safety check, `-D` is the override — and knowing *why* is the point.

Both are redrawn in Stage 3.7 and Stage 4.5 with the sandbox's own hashes.

---

## 11. What the agent must not do in this session

- Never hand over a **multi-line** command block — one physical line each (rule 10).
- Never run the git commands; the user does (rule 10).
- Never draw a diagram with `A → B → C`; use the user's real hashes, branch and file names.
- Never skip the picture to save space, and never put the picture **instead of** the sentence that explains it.
- Never leave a step without 2–3 questions, and never answer them in the same turn (rule 19).
- Never move to the next stage while an earlier diagram is still unexplained.


