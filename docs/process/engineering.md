---
slug: engineering-practice
uuid: 01900d7c-7f3a-7e8b-9c4d-000000000222
title: "Engineering Practice Policy"
kind: process-policy
status: draft
implements: ["PROCESS.md#make-claims-proportionate-to-evidence", "PROCESS.md#keep-work-accountable-and-bounded", "PROCESS.md#preserve-provenance-and-reproducibility"]
description: "A revisable practice for implementing, verifying, operating, and evidencing bounded Epiphany engineering changes."
labels: [process, engineering, verification, operations, clojure]
created: "2026-07-12"
---

# Engineering Practice Policy

## Purpose

This policy governs the engineering work that realizes accepted or provisional
shapes without misrepresenting capability as verified product behavior. It
delegates semantic layer construction to `STYLE.md` and current commands to
`AGENTS.md`; it governs how an implementation slice selects and records
proportionate checks, environment facts, and operational evidence.

## Engineering form

```clojure
(ε slice
   laws
   shapes
   boundaries
   verification-plan
   environment
   -> evidenced-change)
```

`ε` is valid design data before each repository capability is automated. An
evidenced change is not automatically accepted; it is a change with inspectable
basis, actual verification result, limitations, and handoff information.

## Implementation contract

A material implementation slice identifies its target outcome, applicable
policy/ADR/design constraints, changed contracts or schemas, relevant layer
boundaries, dependencies/environment requirements, verification plan, and
observable failure behavior. New foreign/JVM capability follows the Engineering
Kernel: law before shape, explicit extern decoding, pure domain meaning, and
infra orchestration.

Do not claim a feature exists because a namespace compiles, a stub returns a
value, or a happy-path demo works. State which contract, mode, boundary, and
failure paths have actually been verified.

## Verification selection

Select checks based on the changed risk surface, not a ritual fixed command
list. Where applicable, include contract/schema tests, pure behavior tests,
property/generative tests, foreign-boundary adapter tests, integration tests,
architecture/dependency checks, static analysis, mutation/adversarial checks,
and operational/manual evidence.

A change that affects persisted records, public ports, foreign decoding,
epistemic tiers, identity/continuity, or failure representation requires
verification at the affected boundary. A smaller pure change may require only
focused law/shape/domain evidence. Record commands/tool versions/environment
when they materially affect reproduction.

## Environment and operations

Environment is evidence when it affects a result. Record unavailable services,
versions, configuration profiles, fixtures, network/filesystem assumptions, and
external dependencies sufficiently to distinguish a product failure from an
unavailable verification environment.

Operational behavior—including startup, configuration, diagnostics, retry,
backup/recovery, migration, and observability—is part of the change whenever it
can alter the promised outcome. “Works locally” is an observation with context,
not an operations guarantee.

## Warnings, failures, and handoff

Warnings are contract findings, not harmless background. New warnings, skipped
checks, unavailable checks, and known failures are recorded and dispositioned;
they are not hidden by a passing unrelated suite.

A handoff records changed artifacts, verified and unverified claims, actual
results, environment limits, unresolved anomalies, and next action. This lets
an unblocked successor continue without reconstructing the prior actor's state.

## Release and acceptance

A release/process evidence record identifies the change set/version, selected
verification evidence, environment/operational checks, known limitations, and
acceptance authority. Review and acceptance follow `docs/process/review-and-acceptance.md`;
engineering evidence never self-accepts the outcome.

## Operational references

- Engineering kernel: `STYLE.md`
- Current commands and repository guidance: `AGENTS.md`
- Process Charter: `PROCESS.md`
- Review and acceptance: `docs/process/review-and-acceptance.md`
- Kanban workflow: `docs/process/kanban.md`
