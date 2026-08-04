---
slug: knoxx-auth-fail-closed-triage-2026-08-01
uuid: 50f6d8d9-bc27-4214-905d-3ce9cf024eb2
title: "Knoxx authentication fail-closed triage — 2026-08-01"
kind: note
status: draft
description: "Revision-scoped findings from Knoxx policy initialization and MCP OAuth failures, separating repaired defects from remaining fail-closed and end-to-end verification obligations."
created: "2026-08-01"
updated: "2026-08-04"
labels: [triage, knoxx, authentication, authorization, oauth, mcp, fail-closed, security, provenance]
sources:
  - "open-hax/knoxx@24ae9b7c1dac2ca9ed26a3aa74e33a6812bfc1b2"
  - "open-hax/knoxx@7a954505dc97fd804eb9a427bc95803294226563:backend/src/cljs/knoxx/backend/bootstrap.cljs"
  - "open-hax/knoxx@7a954505dc97fd804eb9a427bc95803294226563:backend/src/cljs/knoxx/backend/infra/db/policy.cljs"
  - "open-hax/knoxx#212@3689b77c4786b8373ded44037e60c645e44aba9d"
  - "open-hax/knoxx#213@b24c68b1a1951a478316682d6adcac84d0145a48"
  - "open-hax/knoxx#214@41b15af5d24d1e26a2daad36fa6172dc4fed2220"
  - "open-hax/knoxx#215@c005638986bd0ae8b5c0c25cb56b1c24f1ed3ed6 (open branch evidence)"
informs: []
---

# Knoxx authentication fail-closed triage — 2026-08-01

## Scope

This note records bounded cross-repository triage findings. It does not define
new accepted architecture. It preserves current implementation evidence and the
smallest design corrections that evidence supports.

## Finding 1: policy initialization must fail closed

### Observation

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

### Current code evidence

At revision `7a954505dc97fd804eb9a427bc95803294226563`, bootstrap wraps
`create-policy-db` in a `try`/`catch` and exits the process when initialization
throws. That is fail-closed for thrown failures.

The public policy namespace still documents `create-policy-db` as returning
`Promise<policy context | nil>`. A nil result is structurally different from a
rejected promise: unless bootstrap explicitly rejects nil before route
registration, the catch boundary does not protect it.

This creates an ambiguity between the declared public contract and the intended
startup invariant.

### Classification

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

### Bounded correction

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

## Finding 2: route reachability is not OAuth-flow verification

### New merged evidence

Knoxx PRs #212, #213, and #214 repaired three sequential blockers discovered by
a real ChatGPT MCP connector attempt:

1. The public OAuth discovery documents returned `500` because their route mode
   invoked a missing request-context dependency. Clients could not enter the
   flow at all. PR #212 moved these intentionally unauthenticated endpoints onto
   the route mode their dependency map supports and added tests against the real
   route registration path.
2. Dynamic client registration persisted the registration under
   `client_data`, while lookup returned the surrounding storage envelope.
   Redirect allow-list validation therefore saw no `redirect_uris` and rejected
   every registered client. PR #213 restored the registration/storage boundary
   and exposed OAuth client errors to Fastify with their intended HTTP status.
3. The first authenticated consent request treated a ClojureScript auth-context
   map as a nested JavaScript object. The consent page failed before code issue,
   and token list/revoke routes carried the same latent access pattern. PR #214
   moved those reads to the shared authorization accessors.

PR #214 also closed two authorization defects found during review:

- token revocation is now scoped to the authenticated membership rather than
  token value alone;
- authorization-code creation refuses blank membership or user identity rather
  than minting a bearer credential without an authorization principal.

The revocation persistence boundary now decodes the Mongo result through an
extern adapter, validates law-owned request/result contracts, and fails closed
when `deletedCount` is missing or non-numeric instead of fabricating a confident
"nothing deleted" result.

### New open-branch evidence

Knoxx PR #215 remains open at
`c005638986bd0ae8b5c0c25cb56b1c24f1ed3ed6`; it is branch evidence, not merged
implementation fact. It records the next production-confirmed blocker after
consent: code exchange rejected a freshly issued authorization code as
`Unknown or expired code`.

The writers store `:expiresAt`, while the code, token, and membership-token-list
readers asked for `:expires-at`. Because the missing field defaulted to `0`, all
persisted authorization codes and tokens were treated as expired. Even a
successful exchange would therefore have produced a bearer token that the next
authenticated request could not verify.

The branch changes all credential readers to one fail-closed liveness rule over
the written `:expiresAt` field. The Mongo boundary accepts BSON dates, finite
numeric epoch values, or parseable ISO strings and rejects unreadable expiry
values. It also changes code exchange from a separate read-then-delete sequence
to an atomic `findOneAndDelete` claim, preventing concurrent exchanges from
minting multiple tokens from one authorization code.

### Classification

- **Fact:** discovery, registered-client lookup, and authenticated consent each
  independently prevented any MCP client from completing OAuth.
- **Fact:** earlier unauthenticated probes could not exercise the authenticated
  consent and token-management paths because the browser-auth guard redirected
  before those handlers ran.
- **Fact:** the three merged fixes add route-level and store-level regression
  tests for the failures observed so far.
- **Fact:** membership-scoped revocation and nonblank code identity are now
  executable authorization invariants.
- **Open branch fact:** PR #215's cited branch consistently reads the expiry key
  its writers persist and atomically consumes an authorization code.
- **Production observation:** the deployed flow reached token exchange and then
  rejected a newly minted code as expired, according to PR #215's source record.
- **Proposal:** merge and deploy PR #215 only after review and validation; its
  behavior is not current `main` authority while the PR remains open.
- **Stale interpretation:** "the downstream OAuth flow is healthy because its
  routes respond or redirect" is disproved by the sequence of production
  failures.
- **Interpretation:** a security protocol is verified by a successful stateful
  journey across its trust boundaries, not by isolated endpoint reachability.
- **Interpretation:** writer/reader agreement and single-use atomicity are part
  of the credential contract, not storage implementation trivia.
- **Proposal:** treat a real registered-client OAuth journey through discovery,
  login, consent, code exchange, PKCE verification, authenticated MCP request,
  token listing, and scoped revocation as a release/readiness invariant.
- **Not decided here:** which connector implementation is the canonical
  conformance client or whether this belongs in Knoxx, deployment services, or
  both.

### Remaining verification gap

At the merged revision
`41b15af5d24d1e26a2daad36fa6172dc4fed2220`, the consent page is the furthest
production-confirmed step in `main`. PR #215 records branch and production
observation evidence through authorization-code issue and attempted token
exchange, but the following still lack one successful deployed journey:

```text
PKCE token exchange with the corrected expiry reader
  -> bearer-token verification
  -> first authenticated POST /mcp
  -> token listing
  -> membership-scoped revocation
```

Unit and route tests are necessary evidence, but they do not prove that deployed
configuration, browser session state, registered-client storage, redirect
handling, PKCE, bearer identity, and MCP transport compose correctly.

## Recommended verification

1. Change `create-policy-db` to reject on unavailable policy storage and remove
   `| nil` from its documented contract.
2. Add a bootstrap test proving nil and rejected policy initialization both
   prevent HTTP readiness.
3. Keep the deployed unauthenticated `/api/config` health assertion as an
   end-to-end security invariant.
4. Review PR #215's expiry normalization, query contract, and atomic
   authorization-code consumption before merge.
5. Add one deployed OAuth journey using a registered test client and PKCE. Assert
   the final MCP request resolves the same membership authorized at consent.
6. Include negative journey checks: altered redirect URI, wrong PKCE verifier,
   reused authorization code, unreadable expiry, blank identity,
   cross-membership revocation, and unreadable deletion result.
7. Audit other ESM CLJS namespaces for raw `js/require`; PR #209 found an
   identical latent defect in OpenUTAU tooling, although that path was not an
   authorization bypass.
8. Align OAuth error bodies with the protocol format separately from status-code
   correctness; PR #213 explicitly leaves that as follow-up.

## Disposition

`finding` / `proposal input` with merged implementation updates and one open
branch refinement. The observed policy bypass and merged MCP OAuth blockers are
repaired at their cited revisions. PR #215 contains a bounded candidate repair
for expiry-field drift and authorization-code replay, but remains proposal-stage
branch evidence until merged. The fail-closed policy-context contract and a
successful deployed end-to-end OAuth conformance journey remain bounded Knoxx
follow-up work until accepted and implemented.
