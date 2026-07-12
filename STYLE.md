# Epiphany Engineering Kernel

## Purpose and authority

This document defines the cross-project construction kernel used by Epiphany.
It is deliberately more durable than a tool-specific coding guide: it explains
how a Lisp system keeps meaning, admissibility, foreign capability, domain
decision, and effects distinct while still composing them into one product.

`PROCESS.md` governs claims, evidence, authority, and acceptance. Approved ADRs
govern architectural decisions. This kernel governs code construction and
namespace boundaries within those constraints. `AGENTS.md` and board guides
supply current commands and execution instructions; they do not weaken these
layer laws.

## The construction form

```clojure
(η Discovery (-> (-> Describe specify define) (-> μ shape extern domain infra)) Π)
```

This is Epiphany's executable notation for a construction cycle. It is a
Lisp-shaped process form: its symbols have the meanings declared here.

- **`η`** is the field of effects we have experienced without a settled account
  of their source. It includes user reports, surprising system behavior,
  existing code, failing checks, raw foreign responses, and discovered
  contradictions.
- **`Discovery`** observes, inventories, and orients to `η`. It records the
  encountered effect and its context; it does not silently turn an observation
  into an explanation.
- **`(-> Describe specify define)`** turns encountered effects into explicit
  intent, observable obligations, and `law.*` contracts.
- **`μ`** is the field of effects we can produce without yet having a sufficient
  account of their behavior, limits, or meaning. A prototype, library call,
  parser, query, or model output can be a μ-capability.
- **`(-> μ shape extern domain infra)`** turns raw capability into bounded
  system behavior: pure shaping, foreign-boundary decoding, domain decision,
  and infrastructure orchestration.
- **`Π`** is the currently integrated product of the cycle: code, declared
  contracts, verification evidence, and integration behavior considered
  together. Observed behavior of `Π` becomes input to future `η`.

The form is recursive, not a one-time waterfall. Discovery may occur at every
layer; a new anomaly can require a return to `Describe`, `specify`, or `define`
before construction continues.

## Categories and contracts

Every subsystem distinguishes **categories** from **contracts**.

A category describes the space of meaningful kinds and transformations: what
sort of event, value, relation, transition, or operation this is. A contract
decides whether this particular value or transition is admissible at this
boundary under current obligations.

```text
Category: a candidate continuity relation
Contract: this candidate has required endpoints, evidence basis, tier,
          and error/absence representation to count as a valid record
```

Categories make a system intelligible. Contracts make its claims enforceable.
Do not substitute one for the other:

- A keyword or class name can classify a value without making it valid.
- A schema can reject malformed data without deciding its domain meaning.
- A passing schema does not accept an interpretation or architectural decision.
- A useful external capability does not become an internal concept until it is
  decoded and shaped at a declared boundary.

## Layer laws

The standard semantic layers are `law`, `shape`, `extern`, `domain`, and
`infra`. Not every change creates every layer, but no layer may be skipped just
to reach a working demonstration faster. Omit a layer only when its
responsibility is genuinely absent from the change.

| Layer | Responsibility | May depend on | Must not do |
|---|---|---|---|
| `law.*` | Schemas, predicates, contracts, error shapes, operation declarations, and admissibility rules | Pure Clojure/data libraries | I/O, SDK calls, orchestration, domain policy |
| `shape.*` | Pure transformations among declared shapes: parsing, encoding, decoding, normalization, enrichment | `law.*` | I/O, hidden coercion, foreign objects, domain decisions |
| `extern.*` | Foreign/JVM/SDK/filesystem/network/process boundary invocation and immediate decoding | `law.*`, `shape.*`, foreign libraries | Leak raw foreign values upward, decide domain policy |
| `domain.*` | Pure Epiphany meaning, categories, transitions, and decisions over trusted shapes | `law.*`, `shape.*` | I/O, Java/SDK imports, raw host values, adapter selection |
| `infra.*` | Effect orchestration, adapter composition, configuration-selected implementations, and port fulfillment | All lower layers | Invent domain meaning, bypass contracts, conceal foreign values |

`application.*` is an application boundary: it coordinates named use cases over
domain services and declared ports. It must not become a second domain or an
unvalidated transport adapter.

## Dependency direction

Dependency direction is one way:

```text
law <- shape <- extern
law <- shape <- domain
law <- shape <- extern <- infra
law <- shape <- domain <- infra
```

Practical consequences:

- `law.*` must be usable without infrastructure startup.
- `shape.*` functions accept and return Clojure data, never JGit/Mongo/Lucene/
  HTTP-client/Ollama/Java SDK objects.
- `domain.*` uses values that have already crossed an `extern.*` contract.
- `infra.*` may call foreign systems only through `extern.*` where a foreign
  representation or API boundary exists.
- A namespace requiring a Java library directly is presumptively `extern.*`;
  a deliberate exception requires an explicit rationale and review.
- An adapter is not automatically `extern.*`: adapter composition and policy
  belong in `infra.*`; raw foreign interaction and decoding belong in `extern.*`.

Existing code may not yet satisfy every boundary. Treat that as explicit
migration work, not as proof that the layer law does not apply.

## Law first

For a new persisted record, public application-port input/output, external
boundary value, or epistemically material claim, define the contract before
writing its adapter or orchestration path.

At minimum, `law.*` owns:

- Data schemas and required/optional fields.
- Enumerated status/tier values and permitted transitions where appropriate.
- Versioned record shapes and operation registry declarations.
- Boundary error and absence shapes, including `unknown`, `unavailable`,
  `corrupt`, `stale`, `invalid`, and `not-implemented` where applicable.
- Port contracts and serialization/deserialization admissibility.
- The structural requirements for observed, derived, provisional, and accepted
  records whose distinction is material to Epiphany.

A schema is an executable contract, not documentation pasted beside an
unvalidated map. Validate at creation, foreign-boundary crossing, persistence,
and public port boundaries as appropriate to the record's risk and cost.

Validation does not license hidden normalization. In particular, observed Git
path representations retain their exact observed values under the identity
rules in approved ADRs.

## Shape without hidden meaning

A `shape.*` function makes value transformation legible and testable. It should
state, through name, contract, and tests, its input shape, output shape, and
failure behavior.

Shape may parse Markdown, encode/decode EDN/JSON, derive source spans, prepare
search documents, or transform a foreign boundary result already decoded into
Clojure data. It must not:

- Perform I/O or reach into global configuration.
- Quietly substitute missing/invalid values for valid ones.
- Assign an epistemic tier, accept a decision, or infer domain meaning unless
  that narrow transformation is itself explicitly specified and contracted.
- Leak an opaque foreign object for some higher layer to interpret later.

## Foreign boundaries

`extern.*` is where Epiphany encounters things it did not define: Java/JVM
libraries, JGit, Mongo drivers, Lucene/OpenSearch clients, Ollama/HTTP
responses, the filesystem, process output, environment values, and future SDKs.

An extern boundary does four things in this order:

1. Invoke or receive the foreign capability.
2. Capture meaningful boundary context and failure information.
3. Decode the foreign representation into declared Clojure data.
4. Validate/shape the decoded value or return an explicit contracted boundary
   failure.

Nothing above `extern.*` interprets a raw Java object, driver cursor, SDK
exception hierarchy, `Optional`, nullability convention, HTTP-client response,
or library-specific sentinel. Those are foreign facts, not Epiphany domain data.

Do not use exception swallowing, `nil`, an empty collection, or a generic
boolean as a substitute for an explicit unavailable/corrupt/invalid/not-
implemented result when that distinction affects a user, decision, retry, or
claim.

## Domain and infrastructure

`domain.*` is where Epiphany decides what shaped values mean. It owns pure
operations such as identity/continuity classification, evidence and status
handling, idempotency rules, selection, validation of domain transitions, and
interpretation of explicit failures.

`infra.*` fulfills ports and composes effects: profile-selected adapters,
transactions, scheduling, retries, command wiring, and operational lifecycle.
It may orchestrate domain decisions and extern calls, but it must not hide
business or epistemic policy inside a driver callback or adapter.

The question is not “is this code pure enough?” The question is “which layer
owns the meaning, admissibility, foreign representation, and effect being
introduced?” Put it there, then make the dependency visible.

## Construction practice

The construction form unfolds as follows:

1. **Discovery:** inventory existing behavior, laws, shapes, foreign boundaries,
   tests, documents, and anomalies relevant to the intended change.
2. **Describe:** state the projection's intent and the effect to be accounted
   for in prose understandable by a reviewer.
3. **Specify:** make success, failure, non-goals, and exit evidence observable.
4. **Define:** create or revise `law.*` contracts before dependent behavior.
5. **Shape:** create pure transformations that satisfy declared contracts.
6. **Extern:** isolate foreign capability and decode it at the boundary.
7. **Domain:** implement pure Epiphany decisions over trusted shapes.
8. **Infra:** compose effects, selected adapters, and domain decisions into a
   use case or port fulfillment.
9. **Integrate:** produce Π through the checks appropriate to the change and
   record what those checks establish and leave uncertain.

A small pure change may end at `domain`; a new JGit/Mongo/Lucene integration
will normally require `law`, `shape`, `extern`, `domain`, and `infra` work.

## The anomaly rule

Every construction step is also discovery. A surprise is not friction to be
silently absorbed; it is new η.

When discovery reveals an anomaly, record:

- The location and observed effect.
- Whether it is reusable existing shape, contradiction, foreign-boundary
  behavior, missing law, or unresolved domain question.
- Whether it invalidates the current description, specification, contract, or
  plan.
- The next action: continue with bounded reuse, revise an earlier form, split
  work, or escalate for a decision.

If an anomaly invalidates the target shape or contract, stop and return to
`Describe`/`specify`/`define`. Do not patch around it in `infra.*` merely to
preserve momentum.

## Verification and warnings

Warnings are failed contracts, not ambient noise. The applicable static checks,
test suites, and review procedure are selected by active engineering policy and
the work item's risk; their current commands are operational, not kernel law.

Verification must test the layer introduced or changed:

- `law.*`: valid and invalid instances, required fields, transition/error cases.
- `shape.*`: transformation properties, edge cases, and preservation/loss rules.
- `extern.*`: foreign decoding, translated failures, and adapter contract tests.
- `domain.*`: pure decision and state-transition behavior.
- `infra.*`: port fulfillment, integration behavior, configuration, and
  observable failure paths.

A green suite is evidence for the checks run. It does not erase an unmodeled
foreign boundary, missing schema, or unresolved anomaly.

## The form is valid before it compiles

The construction form is valid design data whether or not every symbol is
implemented as executable Clojure today. It declares the target shape against
which existing code can be read.

Existing source may partially instantiate the form, contain older shapes, or
contradict a current boundary law. Each difference is an architectural
observation:

- A `domain.*` namespace that imports an SDK identifies an unresolved `extern.*`
  boundary.
- A persisted map without a `law.*` contract identifies an unexpressed
  admissibility rule.
- A driver response escaping an adapter identifies an unshaped foreign value.
- An `infra.*` callback making a policy choice identifies domain meaning that
  has not yet been given a home.

These observations are not grounds to erase or weaken the form. They are
candidates for discovery, design, and bounded remediation. The form becomes
more executable over time because actors preserve and work the gap between its
declared shape and the current repository.

Do not perform a repository-wide namespace shuffle merely to make paths look
conformant. Make each change as a reviewable realization of a named part of the
form, with contracts and verification appropriate to the boundary.

## Relationship to process artifacts

- Use `PROCESS.md` for claim/acceptance/exception governance.
- Use `docs/process/research.md` when uncertainty requires bounded research.
- Use `docs/process/kanban.md` and `docs/kanban/AGENTS.md` for board workflow
  and current operational mechanics.
- Use ADRs for consequential architecture choices, including deviations from
  this kernel that are intended to persist.
- Use the applicable work item to state the concrete outcome and verification
  evidence for one slice.
