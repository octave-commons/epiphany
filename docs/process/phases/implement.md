# Implement a task from the kanban board


1. **Implement Slice**

Do the smallest cohesive change that can clear gates defined in agent docs
When the scope is larger than the available session, carve off a reviewable subset and explicitly document what remains (e.g.,
inventory lingering files, capture blockers, link references).

2. **Review → Done**

Move to _In Review_; when the reviewer approves **and** the global [Definition of Done](#definition-of-done) is satisfied, advance to _Done_, recording evidence and summaries on the card. Testing and documentation are DoD gates, not their own columns.


## Definition of Ready

- Must have committed to a kanban task
- The status of the task has been set to `in_progress`
- The agent committing to implementation of the task appended a comment to the task with it's reasoning for accepting the task, and the approach it will be taking.

## Definition of Done

A reviewer advances a card to _Done_ only when all apply:

- **Tests pass.** The change ships with tests that exercise it, and the relevant
  suite is green (`clojure -M:test`, plus `test/architecture_test.clj` for
  structural work). Record the command and pass/fail counts on the card.
- **Static gates clean.** `bin/analyze` (or `bin/analyze --strict` for
  structural work) introduces no new warning classes in the touched files.
- **Documented.** Public vars have docstrings; the card records a short summary
  of what changed plus evidence (test output, benchmark numbers, screenshots).
  Update `docs/designs`/`docs/notes` when behavior or architecture changed.
- **Invariants intact.**  no `domain/` → `infra/` import — see `CLAUDE.md`

A card that fails any gate goes back to **In Progress** (or **Todo**), not to a
Testing/Document column — those no longer exist.
