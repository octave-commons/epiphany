---
id: "01900d7c-7f3a-7e8b-9c4d-000000001708"
title: "ENG-017H: Add static architecture and interop boundary gates"
status: "incoming"
type: "story"
priority: "P1"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
design: "docs/designs/verification-architecture.md"
points: 5
labels: [quality, static-analysis, interop, architecture, phase-1]
category: "stories"
dependency: []
---

# ENG-017H: Add static architecture and interop boundary gates

Establish a reproducible static-analysis baseline and enforce the architectural
layer boundaries that keep JVM-library assumptions inside infrastructure.

## Scope

- Add a pinned general Clojure linter and zero-warning CI alias.
- Generate a namespace-level Java interop inventory: imports, type hints,
  constructors/static calls, and dot calls.
- Fail unapproved `law`/`domain`/`application` dependencies on `infra`.
- Fail new direct Java interop in `law`, `domain`, and `application` absent a
  documented exception mechanism.

## Acceptance criteria

- CI emits the inventory and compares it to a reviewed baseline.
- Static checks are deterministic and runnable locally through `deps.edn`.
- Existing violations are either removed in scope or captured as explicit,
  dated exceptions; no silent baseline reset is allowed.
- Adapter interop is measured but not prohibited wholesale.
