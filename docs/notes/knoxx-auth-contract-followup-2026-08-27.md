---
slug: knoxx-auth-contract-followup-2026-08-27
uuid: 6e28d70a-1ef4-4dca-84ac-9f898b9caa70
title: "Knoxx MCP authentication contract follow-up — 2026-08-27"
kind: note
status: draft
description: "Revision-scoped follow-up recording Knoxx's merged contract-declared MCP authentication methods and the remaining deployment/e2e obligations."
created: "2026-08-27"
updated: "2026-08-27"
labels: [triage, knoxx, authentication, authorization, mcp, contracts, fail-closed, provenance]
sources:
  - "open-hax/knoxx#256@b36383923735e8cdfa15cbbcf3a76f8492dd40f5"
  - "octave-commons/epiphany#9@28160f84ff256f5c9358412b4ec6aea70b560b32"
---

# Knoxx MCP authentication contract follow-up — 2026-08-27

## Scope

This note supplements, rather than rewrites, the earlier authentication/MCP triage in Epiphany PR #9. It records current merged implementation evidence from Knoxx PR #256 and keeps implementation fact, interpretation, and promotion status separate.

## New merged evidence

Knoxx PR #256 merged as `b36383923735e8cdfa15cbbcf3a76f8492dd40f5` on 2026-08-27.

The accepted request methods for `POST /mcp` are now declared by the `authentication` contract rather than inferred from environment configuration alone. The pure `law.auth-methods` layer decides which declared methods are admissible; `infra.auth.method-config` supplies runtime facts such as environment values and peer socket address.

The boundary fails closed. A method is refused when the authentication contract is absent, the method is not declared, it is not enabled, no grant is produced, or a declared guard is unsatisfied. The `:trusted-loopback` method may be declared while remaining inert unless its configured secret satisfies the declared minimum length and the request satisfies the loopback guard.

The merged change also teaches the resource loader the `authentication` contract class. This matters outside `/mcp`: before #256, deploying an authentication contract that the running image could not parse produced an invalid resource with no usable kind, and publication resource validation correctly refused publication surfaces because the invalid resource could not be proven irrelevant.

## Classification

- **Merged implementation fact:** MCP authentication-method selection is now contract-declared and decided by pure law over declared policy plus runtime facts.
- **Merged implementation fact:** trusted-loopback authentication is opt-in and fail-closed when its runtime guard or secret requirements are unsatisfied.
- **Merged implementation fact:** the backend can parse the deployed `authentication` contract class, removing the cross-domain invalid-resource failure observed before #256.
- **Interpretation:** authentication method policy and runtime enablement are distinct concerns. Declaring that a method may exist is not equivalent to proving that its runtime prerequisites are present.
- **Interpretation:** resource loaders that fail closed across heterogeneous contract classes make contract-class compatibility a deployment-wide readiness concern, not merely a local parser concern.
- **Promotion status:** repository-local evidence only. This does not promote Knoxx's authentication contract shape or method vocabulary into Epiphany or Foresight common law.

## Remaining obligations

The earlier PR #9 readiness model remains useful but is now incomplete without this contract layer. A current MCP readiness journey is better described as:

```text
contract parseability
  -> declared authentication method admissibility
  -> runtime guard satisfaction
  -> OAuth / loopback credential verification
  -> MCP initialize
  -> tools/list
  -> invocation semantics
```

Two concrete follow-ups remain outside #256:

1. Deployment must re-ship the `contracts/knoxx/authentication/mcp_http.edn` contract only with an image that contains #256 or later. `open-hax/services#56` currently carries that deployment-side reconciliation as an open change, so it is not yet current services `main` behavior.
2. A full revision-bound end-to-end MCP conformance record is still not supplied by #256. Its law tests cover method selection, but route wiring and the complete connector journey remain separate verification obligations.

## Cross-repository note

The open Knoxx #257 / services #56 pair adds another useful distinction but is not promoted here because both changes remain open: a capability contract can exist, resolve, and still be operationally inert when no deployed actor/trigger/producer composition executes it. If that shape survives merge and appears independently elsewhere, it is a candidate for Foresight crucible comparison rather than an authentication-law conclusion.

## Disposition

`merged implementation update` / `proposal input`.

Knoxx #256 closes a real gap in the authentication-policy boundary and supersedes the earlier assumption that MCP method availability is primarily environment-shaped. It does not close the complete MCP readiness journey, does not make services #56 current fact, and does not establish a cross-repository accepted law.
