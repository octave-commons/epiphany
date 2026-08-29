---
slug: knoxx-auth-fail-closed-triage-2026-08-01
uuid: 50f6d8d9-bc27-4214-905d-3ce9cf024eb2
title: "Knoxx authentication and MCP readiness triage — 2026-08-01"
kind: note
status: draft
description: "Revision-scoped findings from Knoxx policy initialization, MCP OAuth, transport, schema, and tool-semantics failures, separating repaired defects from remaining readiness obligations."
created: "2026-08-01"
updated: "2026-08-29"
labels: [triage, knoxx, authentication, authorization, oauth, mcp, fail-closed, security, readiness, provenance]
sources:
  - "open-hax/knoxx@24ae9b7c1dac2ca9ed26a3aa74e33a6812bfc1b2"
  - "open-hax/knoxx@7a954505dc97fd804eb9a427bc95803294226563:backend/src/cljs/knoxx/backend/bootstrap.cljs"
  - "open-hax/knoxx@7a954505dc97fd804eb9a427bc95803294226563:backend/src/cljs/knoxx/backend/infra/db/policy.cljs"
  - "open-hax/knoxx#212@3689b77c4786b8373ded44037e60c645e44aba9d"
  - "open-hax/knoxx#213@b24c68b1a1951a478316682d6adcac84d0145a48"
  - "open-hax/knoxx#214@41b15af5d24d1e26a2daad36fa6172dc4fed2220"
  - "open-hax/knoxx#215@e08946b92dc79f281983dc3f9311471e38f7afe3"
  - "open-hax/knoxx#216@d509565a36cc912d17f1ac2d9a746c4716a237f2"
  - "open-hax/knoxx#217@9d7ff08999710c45e33383ec637f100fb4eac5bf"
  - "open-hax/knoxx#218@1f79fc21fd36330b5f40e47f11c1840b4ee50896"
informs: []
---

# Knoxx authentication and MCP readiness triage — 2026-08-01

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

The required operational invariant is stronger than a process-manager readiness
signal: bootstrap must reject a nil or failed policy context before composing
protected routes or starting the HTTP listener. Readiness may report that
invariant, but it cannot enforce it after an unprotected listener exists.

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
  -> protected routes are not composed
  -> HTTP listener does not start
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

### Merged implementation evidence and reported deployment observation

Knoxx PRs #212 through #215 repaired four sequential blockers discovered by real
MCP connector attempts:

1. Public OAuth discovery documents returned `500` because their route mode
   invoked a missing request-context dependency. Clients could not enter the
   flow. PR #212 moved these intentionally unauthenticated endpoints onto the
   supported route mode and added tests against real route registration.
2. Dynamic client registration persisted the registration under `client_data`,
   while lookup returned the surrounding storage envelope. Redirect allow-list
   validation therefore saw no `redirect_uris` and rejected every registered
   client. PR #213 restored the registration/storage boundary and exposed OAuth
   client errors with their intended HTTP status.
3. The first authenticated consent request treated a ClojureScript auth-context
   map as a nested JavaScript object. The consent page failed before code issue,
   and token list/revoke routes carried the same latent access pattern. PR #214
   moved those reads to shared authorization accessors.
4. Credential writers stored `:expiresAt`, while authorization-code, token, and
   token-list readers asked for `:expires-at`. Every credential therefore
   appeared expired. PR #215 centralized fail-closed instant decoding and
   credential liveness over the field writers actually persist.

PR #214 also made token revocation membership-scoped and refused blank identity
when issuing an authorization code. PR #215 made authorization-code claiming
atomic with `findOneAndDelete`, while validating client, redirect, and PKCE
bindings before the destructive claim so an invalid request cannot spend a
legitimate client's code.

PR #216 contains an author-reported deployment log excerpt:

```text
POST /api/mcp/oauth/token   200
POST /mcp  (Bearer)         200 text/event-stream
```

That excerpt is durable evidence that the PR author reported a successful token
exchange and bearer-authenticated request. It is not an independently reproduced,
revision-bound conformance record: the deployed revision, configuration, and
complete client journey are not bound together by the cited excerpt.

### Classification

- **Fact:** discovery, registered-client lookup, authenticated consent, and
  credential expiry drift each independently prevented completion of OAuth.
- **Fact:** PRs #212 through #215 are merged at the cited revisions.
- **Reported observation:** PR #216 contains a log excerpt claiming successful
  PKCE token exchange and bearer-authenticated `POST /mcp`.
- **Fact:** membership-scoped revocation, nonblank code identity, fail-closed
  expiry decoding, binding checks, and atomic single-use code claiming are now
  executable invariants.
- **Unresolved verification:** no independently reproducible, revision-bound
  conformance record yet proves the downstream OAuth flow through bearer
  authentication and usable MCP capability.
- **Interpretation:** a security protocol is verified by a successful stateful
  journey across its trust boundaries, not by isolated endpoint reachability.
- **Interpretation:** writer/reader agreement and single-use atomicity are part
  of the credential contract, not storage implementation trivia.
- **Proposal:** retain a deployed registered-client OAuth journey as a release or
  readiness invariant, including negative checks for redirect, PKCE, replay,
  expiry, blank identity, and cross-membership revocation.
- **Not decided here:** which connector is the canonical conformance client or
  whether the journey belongs in Knoxx, deployment services, or both.

## Finding 3: authenticated transport is not usable MCP capability

### Sequential merged evidence and reported production symptoms

The reported bearer-authenticated request exposed three further layers that
earlier failures made unreachable.

#### Tool-schema composition

PR #216 records that ChatGPT's first authenticated discovery request failed while
registering tools:

```text
TypeError: fschema.optional is not a function
```

The TypeBox-to-Zod converter returned a plain field-shape object for nested
object properties rather than a Zod object schema. One nested object parameter
therefore prevented registration of the whole tool set. The repair wraps nested
shapes with `z.object`, tests the conversion against real Zod, and serves OAuth
protected-resource metadata at the path-inserted RFC 9728 location used for the
`/mcp` resource as well as compatibility locations.

#### Transport-state selection

PR #217 records that connectors then authorized and initialized but advertised
no tools. ClojureScript conversion changed
`{:sessionIdGenerator js/undefined}` into JavaScript
`{sessionIdGenerator: null}`. The SDK selects stateless mode only when the value
is absent or `undefined`; `null` selected stateful mode. Because Knoxx creates a
fresh `McpServer` for each request after removing the stateful session writer,
`initialize` succeeded on one instance and `tools/list` reached a different,
uninitialized instance. The repair omits the key with `#js {}` and pins the
interop trap in tests.

This is a regression in the attempted migration from stateful to stateless
transport, not an SDK protocol change. PR #217 also identifies two unresolved
implementation facts: production dependency resolution can drift from the
lockfile because the Docker build uses `--no-frozen-lockfile`, and GET/DELETE
session routes still read `mcp-sessions*` even though nothing writes it.

#### Tool capability semantics

PR #218 records that tools finally appeared but unannotated reads were presented
by clients as public, destructive, open-world writes. MCP's conservative defaults
are appropriate for unreviewed tools, but false for reviewed read-only tools.
The merged repair adds implementation-verified annotations for nine OpenPlanner
boundary tools, lets tool-local annotations override the table, and deliberately
leaves unreviewed tools undeclared rather than guessing.

Review corrected two initially false assertions: `save_translation` can overwrite
an existing segment and `create_new_file` truncates an existing path, so both are
destructive and idempotent rather than append-only. The annotation lookup also
uses the canonical pre-sanitization tool name when registered names have been
rewritten, preventing silent metadata loss for `web.read` -> `web_read`.

### Classification

- **Fact:** PRs #216 through #218 repaired schema-registration, transport-mode,
  and tool-metadata defects that earlier failures made unreachable.
- **Fact:** PRs #216, #217, and #218 are merged at the cited revisions.
- **Fact:** a single malformed nested parameter schema could prevent the complete
  tool catalog from registering.
- **Fact:** JavaScript `null` and absent/`undefined` selected different SDK
  transport modes, and CLJS conversion erased that distinction.
- **Fact:** only nine reviewed tools currently receive declared annotations;
  undeclared tools retain conservative client defaults.
- **Fact:** the review process itself caught incorrect safety claims for two
  write tools before merge.
- **Interpretation:** MCP readiness is a layered composition property:

  ```text
  policy readiness
    -> OAuth discovery and authorization
    -> credential issue and bearer verification
    -> schema conversion and tool registration
    -> transport lifecycle coherence
    -> accurate per-tool capability semantics
  ```

- **Interpretation:** boundary metadata is executable safety behavior. Incorrect
  tool annotations can cause clients or humans to authorize the wrong action;
  missing annotations are safer than unverified optimistic claims.
- **Interpretation:** host-language interop distinctions such as absent,
  `undefined`, `null`, CLJS map, and native object require explicit boundary
  tests whenever an SDK branches on exact JavaScript representation.
- **Proposal:** define an MCP readiness journey that continues beyond bearer
  authentication through `initialize`, `tools/list`, invocation of one reviewed
  read-only tool, and invocation-policy display for one destructive tool.
- **Proposal:** pin production SDK resolution to the tested lockfile and either
  implement stateful session storage or remove vestigial GET/DELETE session
  routes.
- **Proposal:** review remaining Discord, Bluesky, sandbox, voice, and event tools
  individually before annotating them.
- **Proposal:** introduce `structuredContent` and `outputSchema` per tool only
  when the implementation emits the structure being declared; blanket schemas
  would convert a warning into runtime validation failures.
- **Not decided here:** whether Knoxx should remain stateless permanently, what
  subset of tools constitutes readiness, or which annotations should affect
  policy enforcement rather than client presentation alone.

## Remaining verification and design gaps

PR #216's reported logs suggest that the OAuth identity path crossed its previous
success boundary, but independently reproducible evidence remains incomplete.
One revision-bound end-to-end record still needs to prove the complete usable
capability journey:

```text
OAuth discovery
  -> login and consent
  -> PKCE exchange
  -> bearer verification
  -> MCP initialize
  -> tools/list with the expected catalog
  -> reviewed read-only tool invocation
  -> destructive tool represented as destructive
  -> token listing and membership-scoped revocation
```

The cited PRs contain production observations for several consecutive stages,
but not one durable conformance record asserting all of them under one revision
and deployed configuration.

## Recommended verification

1. Change `create-policy-db` to reject on unavailable policy storage and remove
   `| nil` from its documented contract.
2. Add a bootstrap test proving nil and rejected policy initialization both
   abort protected-route composition and listener startup before readiness.
3. Keep the deployed unauthenticated `/api/config` health assertion as an
   end-to-end security invariant.
4. Add one revision-bound deployed MCP conformance journey covering discovery,
   OAuth, bearer verification, initialization, tool listing, one read, one
   destructive-write presentation, token listing, and scoped revocation.
5. Include negative journey checks: altered redirect URI, wrong PKCE verifier,
   reused code, unreadable expiry, blank identity, cross-membership revocation,
   malformed nested tool schema, and mismatched transport mode.
6. Pin the MCP SDK used in production to the tested lockfile or use a frozen
   install.
7. Resolve the stateful/stateless contradiction by implementing session writes
   or removing routes whose session map cannot be populated.
8. Audit the remaining tool implementations before assigning annotations.
9. Add structured output one tool at a time, beginning with genuinely structured
   graph and semantic-query results.
10. Audit other ESM CLJS namespaces for raw `js/require`; PR #209 found an
    identical latent defect in OpenUTAU tooling, although that path was not an
    authorization bypass.

## Disposition

`finding` / `proposal input` with merged implementation updates. The observed
policy bypass and the sequential OAuth, schema, transport, and annotation defects
are repaired at their cited revisions. PR #216 reports successful bearer
authentication, but this note does not promote that report into independently
verified production fact. The fail-closed policy-context contract, production
dependency pinning, session-route coherence, remaining tool annotations,
structured outputs, and a single revision-bound end-to-end MCP conformance
record remain bounded Knoxx follow-up work until accepted and implemented.
