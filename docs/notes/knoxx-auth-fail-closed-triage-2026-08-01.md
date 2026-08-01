---
slug: knoxx-auth-fail-closed-triage-2026-08-01
uuid: 50f6d8d9-bc27-4214-905d-3ce9cf024eb2
title: "Knoxx authentication fail-closed triage — 2026-08-01"
kind: note
status: draft
description: "Revision-scoped finding that a Knoxx module-loading failure exposed a fail-open policy initialization path, plus the bounded design correction implied by current evidence."
created: "2026-08-01"
labels: [triage, knoxx, authentication, authorization, fail-closed, security, provenance]
sources:
  - "open-hax/knoxx@24ae9b7c1dac2ca9ed26a3aa74e33a6812bfc1b2"
  - "open-hax/knoxx@7a954505dc97fd804eb9a427bc95803294226563:backend/src/cljs/knoxx/backend/bootstrap.cljs"
  - "open-hax/knoxx@7a954505dc97fd804eb9a427bc95803294226563:backend/src/cljs/knoxx/backend/infra/db/policy.cljs"
informs: []
---

# Knoxx authentication fail-closed triage — 2026-08-01

## Scope

This note records one bounded cross-repository triage finding. It does not define
new accepted architecture. It preserves the current implementation evidence and
the smallest design correction that evidence supports.

## Observation

Knoxx commit `24ae9b7c1dac2ca9ed26a3aa74e33a6812bfc1b2` fixed ESM module imports for
MongoDB and Node built-ins. Before that fix, `js/require` remained in an ESM
server bundle where CommonJS `require` did not exist. Mongo policy-store
initialization failed, `create-policy-db` produced no usable policy context, and
the running backend answered protected `/api` routes without authentication.
The production health gate detected this because an unauthenticated request to
`/api/config` returned `200` instead of `401` or `403`.

The import defect is fixed. The incident nevertheless exposes a more durable
boundary problem: authentication and authorization availability were coupled to
policy-store initialization in a way that permitted a running HTTP service with
no effective policy context.

## Current code evidence

At revision `7a954505dc97fd804eb9a427bc95803294226563`, bootstrap wraps
`create-policy-db` in a `try`/`catch` and exits the process when initialization
throws. That is fail-closed for thrown failures.

The public policy namespace still documents `create-policy-db` as returning
`Promise<policy context | nil>`. A nil result is structurally different from a
rejected promise: unless bootstrap explicitly rejects nil before route
registration, the catch boundary does not protect it.

This creates an ambiguity between the declared public contract and the intended
startup invariant.

## Classification

- **Fact:** the ESM import failure caused the policy database path to fail and
  protected routes to become publicly reachable.
- **Fact:** the import defect was corrected in Knoxx PR #209.
- **Fact:** the current bootstrap exits when policy initialization throws.
- **Fact:** the policy API documentation still permits a nil initialization
  result.
- **Interpretation:** a security-critical policy context should be a startup
  precondition, not an optional capability.
- **Proposal:** make `create-policy-db` return a usable context or reject; remove
  nil from the public contract, and retain a production gate that proves
  unauthenticated protected routes return `401` or `403`.
- **Not decided here:** whether Knoxx should offer an explicit unauthenticated
  development mode. Such a mode would require an intentional configuration,
  visible startup state, and route-level constraints rather than accidental
  fallback.

## Bounded correction

The smallest warranted design refinement is:

```text
policy context unavailable
  -> HTTP application does not become ready
  -> process exits or remains unready

never:

policy context unavailable
  -> register protected routes without enforcement
  -> advertise readiness
```

This is narrower than requiring every non-security persistence subsystem to be
available at startup. Session recovery, indexes, caches, and optional enrichment
may remain degradable where their contracts explicitly permit it. The policy
context is different because its absence changes who may invoke the service.

## Recommended verification

1. Change `create-policy-db` to reject on unavailable policy storage and remove
   `| nil` from its documented contract.
2. Add a bootstrap test proving nil and rejected policy initialization both
   prevent HTTP readiness.
3. Keep the deployed unauthenticated `/api/config` health assertion as an
   end-to-end security invariant.
4. Audit other ESM CLJS namespaces for raw `js/require`; the same commit found
   an identical latent defect in OpenUTAU tooling, although that path was not an
   authorization bypass.

## Disposition

`finding` / `proposal input`. The implementation defect is repaired. The
fail-closed startup invariant and nil-contract cleanup remain bounded Knoxx
follow-up work until accepted and implemented there.
