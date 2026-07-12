---
id: "01900d7c-7f3a-7e8b-9c4d-000000001602"
title: "ENG-006B: Ship the workbench: search + evidence drawer"
status: "done"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000006"
design: "docs/kanban/epics/epic-06-temporal-research-workbench.md"
points: 5
labels: ["phase-1", "workbench", "ui", "search", "evidence"]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001601", "01900d7c-7f3a-7e8b-9c4d-000000001402"]
---

# ENG-006B: Ship the workbench: search + evidence drawer

First local web screens: search workspace with mode/filter controls, and the evidence drawer.

## Acceptance criteria

- Search results open the exact source span with provenance in one interaction.
- Retrieval mode and per-signal scores are visible per result.
- Epistemic status (observed/derived/provisional/accepted) is visible wherever a record renders.
- Local-only: no SaaS dependency; Unicode paths render untransliterated.

---
AUDIT 2026-07-12: status=done graded C-. Search workspace is genuinely wired to the hybrid-search service (workbench.clj requires epiphany.domain.hybrid-search; htmx search form posts to /htmx/search) and 25 workbench tests exist. Not verified: 'epistemic status visible wherever a record renders' and per-signal score display — no test names them, no completion evidence recorded. Status left at done; treat unverified criteria as open verification debt for ENG-017I. Lesson: this card is the boundary case — plausible claim, thin evidence. --tasks-dir docs/kanban
---
