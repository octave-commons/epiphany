---
id: "f13ff9b4-9ee9-4cee-b979-f998559c1167"
title: "Select historical resources and preserve typed conversations"
status: incoming
type: planning-record
priority: P1
phase: 1
points: 5
labels: [resources, selection, conversations, provenance]
category: stories
design: "docs/designs/selected-historical-resources.md"
dependency: []
---

As a person recovering ideas across repositories, I want explicit source
selection and typed historical resources/conversations, so memory remains
inspectable without indiscriminate body reads or rewritten histories.

This local execution record follows explicit user authorization, not a claim of
board promotion or completed acceptance. Implementation is stacked on unmerged
PR18's existing Clio provider. Native warnings/review and remaining source-map/
composition acceptance are documented in the design, not silently waived.

Acceptance: shipped CLI exact-path scope before body reads; current/parent and
reused-store filtering; stable durable retry and changed-intent refusal; unchanged
source DAGs; lossless typed JSON/JSONC/JSONL/EDN/TOML/YAML projections; recognized
chat role/order/IDs/timestamps/provenance without invented facts; negative and
oversize inputs; exact-head unit/static/process/corpus evidence.

Dependency justification: SnakeYAML 2.3 provides restricted YAML construction and
Tomlj 1.1.1 preserves TOML types; existing Jackson handles JSON/JSONC. No new
observation backend or external service is introduced.
