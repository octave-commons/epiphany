---
id: "01900d7c-7f3a-7e8b-9c4d-000000001710"
title: "ENG-017J: Pilot mutation checks and CI assurance evidence"
status: "incoming"
type: "story"
priority: "P2"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
design: "docs/designs/verification-architecture.md"
points: 5
labels: [quality, mutation-testing, ci, evidence, adversarial-testing, phase-1]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001708", "01900d7c-7f3a-7e8b-9c4d-000000001709"]
---

# ENG-017J: Pilot mutation checks and CI assurance evidence

Evaluate a Clojure-CLI-compatible mutation workflow against boundary-critical
code and generate a CI-owned assurance evidence artifact.

## Scope

- Trial mutation tooling only on schema gateway/operation registry and selected
  contract laws.
- Require deliberately removed validation and wrong-schema-selection mutants to
  be detected before adopting the tool.
- Generate revision-bound evidence for executed checks, seeds, adapter suites,
  mutation results, and interop deltas.
- Specify protected/private or rotated case handling without exposing fixtures
  to untrusted pull-request contexts.

## Acceptance criteria

- The selected workflow runs through `clojure -M` or is rejected with a recorded decision.
- Meaningful surviving mutants are classified as test gap, equivalent,
  irrelevant, or tool limitation; they are not silently ignored.
- CI, not agent-authored text, generates the evidence artifact.
- The artifact identifies revision, command outcomes, replay seeds, and check versions.
