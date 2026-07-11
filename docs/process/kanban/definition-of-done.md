## Definition of Done (global gates)

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
