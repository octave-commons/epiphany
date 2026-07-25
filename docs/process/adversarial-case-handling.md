---
slug: adversarial-case-handling
title: "Protected and Rotated Adversarial-Case Handling (ADR-004 §8)"
kind: process
status: active
description: "Specification for where adversarial test cases live, how they rotate, and how they stay out of reach of untrusted PR contexts. Specification only — private-suite build-out is follow-up."
created: "2026-07-25"
---

# Protected and Rotated Adversarial-Case Handling

ADR-004 §8 requires that adversarial cases (corruption fixtures, boundary
attacks, sabotage mutants) not be readable by the same untrusted context
that submits code for review — otherwise the defense is enumerable and
gameable. This document specifies the handling; it does not build the
private suite.

## Classes of adversarial case

| Class | Examples | Trust requirement |
|---|---|---|
| Public structural | closed-map violations, type flips, version bumps | none — enumerable by reading the schemas anyway |
| Protected corruption | specific byte-level backup tampering, BSON decode attacks | must not be enumerable from the PR diff alone |
| Rotated cases | any case whose VALUE (not shape) an attacker could pre-compute a bypass for | values rotate per window |

## Rules

1. **Public structural cases stay in-repo** (`test/`). Their strength is
   that the schema is the wall, not the secrecy of the attack.
2. **Protected corruption cases live outside the repo's untrusted surface.**
   Location: a private companion repo (`epiphany-assurance-cases`, same org,
   restricted access) consumed as a git dep with a pinned SHA. CI for the
   main repo consumes it; untrusted PR builds receive only the public subset.
3. **Rotation**: protected values (magic bytes, specific corruption offsets,
   nonce-bearing fixtures) rotate on a weekly window. Rotation is
   deterministic from a secret seed held in CI (`ADVERSARIAL_SEED`),
   so any run is reproducible by anyone holding the seed and meaningless
   without it.
4. **No fixture secrets in the main repo.** The main repo holds only the
   HARNESS for protected cases (load path, rotation function); the cases
   and the seed live in the companion repo / CI secret store.
5. **Untrusted PR context**: PRs from forks run the public suite only.
   The protected suite runs on `push` to protected branches and on
   `pull_request` from same-org branches, via the `assurance-cases` job,
   which requires the `ADVERSARIAL_SEED` secret (forks do not receive it).

## Current state

- Public structural adversarial cases: in-repo (ENG-017D law suite,
  ENG-017F corruption fixtures, ENG-017I generative laws).
- Protected companion repo: NOT YET CREATED (follow-up).
- Rotation mechanism: NOT YET IMPLEMENTED (follow-up).

This file is the specification those follow-ups must implement against.
