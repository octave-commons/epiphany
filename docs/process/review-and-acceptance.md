---
slug: review-and-acceptance
uuid: 01900d7c-7f3a-7e8b-9c4d-000000000221
title: "Review and Acceptance Policy"
kind: process-policy
status: draft
implements: ["PROCESS.md#make-acceptance-explicit-and-scoped", "PROCESS.md#make-claims-proportionate-to-evidence", "PROCESS.md#preserve-provenance-and-reproducibility"]
description: "A revisable practice for independent evaluation, explicit disposition, and scoped acceptance of artifacts and outcomes."
labels: [process, review, acceptance, verification, governance]
created: "2026-07-12"
---

# Review and Acceptance Policy

## Purpose

This policy separates verification, review, and acceptance. Verification records
what a procedure observed; review evaluates an artifact or outcome against
criteria; acceptance is an authorized disposition for a declared scope.

No status field, merge, silence, successful command, or generated report is an
acceptance event by itself.

## Acceptance form

```clojure
(α target
   criteria
   evidence
   reviewer
   authority
   -> disposition)
```

`α` is valid design data before every review record or workflow is automated.
Its result is one of `accepted`, `changes-requested`, `rejected`,
`needs-investigation`, or `not-applicable`, always bounded by target and scope.

## Review contract

A review identifies the target/version, declared criteria, evidence inspected,
reviewer, authority or escalation path, actual disposition, limitations, and
follow-up. Criteria come from the applicable Charter, policy, ADR, design,
story, or explicit review brief; reviewers do not silently invent material
acceptance rules after work is presented.

Independence is proportional to consequence. A self-review may be sufficient
for a low-risk note; a consequential decision, public contract, or irreversible
migration requires an appropriately independent/authorized reviewer.

## Verification evidence

Verification records procedure, inputs/environment where material, artifact or
version context, actual result, and limitations. It says what was checked, not
more. A passing unit suite cannot establish production operability, and a
successful integration run cannot by itself accept an architectural decision.

Failures, unavailable environments, and inconclusive results are first-class
outcomes. They are not replaced with empty results or omitted from the record.

## Disposition and completion

Acceptance may accept the result, request changes, reject it, request further
inquiry, or accept it conditionally with named limitation/follow-up where the
applicable authority permits. A completion claim references the acceptance event
and verification evidence it relies on.

A card moves to `done` only under the Kanban policy after the authorized
acceptance for its declared scope is recorded. Follow-up uncertainty becomes a
new bounded item or explicit limitation, not a hidden condition of done.

## Exceptions and evolution

Expedited review or temporary acceptance follows the Charter exception rules
and states the reduced evidence, risk, authority, expiry/review date, and
required follow-up. Repeated exceptions are evidence to improve criteria,
tooling, capacity, or policy.

## Mechanical floor

This policy is not self-enforcing from prose alone — a 2026-07-12/13 audit
found six kanban cards moved to `done` on the implementer's own completion
comment, with no independent review ever recorded. `bin/kanban-done-gate
<slug>` is a mechanical pre-check for the `review → done` kanban transition:
it verifies any CLI command a story names is actually wired, that a real test
run (not prose) is attached, and that some comment records an explicit
review disposition distinct from the implementation comment. It is a floor,
not the acceptance form above — it cannot verify a reviewer was independent
or authorized, only that *a* disposition was recorded by *someone* other than
whoever wrote the "done" claim. See `docs/kanban/AGENTS.md` hard rule 7.

## Operational references

- Process Charter: `PROCESS.md`
- Kanban workflow: `docs/process/kanban.md`
- Document governance λ: `docs/process/document-governance.md`
- Design practice δ: `docs/process/design.md`
- Decision practice: `docs/process/decision.md`
- Mechanical done-gate: `bin/kanban-done-gate`
