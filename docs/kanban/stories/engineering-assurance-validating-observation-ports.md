---
id: "01900d7c-7f3a-7e8b-9c4d-000000001702"
title: "ENG-017B: Enforce schemas through validating observation ports"
status: "incoming"
type: "story"
priority: "P0"
phase: 1
epic: "01900d7c-7f3a-7e8b-9c4d-000000000001"
design: "docs/designs/verification-architecture.md"
points: 3
labels: [quality, schemas, ports, validation, phase-1]
category: "stories"
dependency: ["01900d7c-7f3a-7e8b-9c4d-000000001701"]
---

# ENG-017B: Enforce schemas through validating observation ports

Add a composition-time observation-port decorator that validates every public
write using ENG-017A before delegation.

## Scope

- Wrap all public observation write functions at the application composition root.
- Validate before delegation and preserve stable domain-level validation errors.
- Prove a rejected write invokes no underlying mutation function.
- Ensure the local profile composes the validated reference port.

## Out of scope

- Mongo-local defense-in-depth validation and adapter-law parity.

## Acceptance criteria

- Each public `:record-*` call is validated exactly once at the wrapper boundary.
- Invalid inputs return `:schema-validation-failed` and leave wrapped state unchanged.
- Tests prove all registered write operations are wrapped.
- Application callers no longer need to remember ad hoc schema validation.
