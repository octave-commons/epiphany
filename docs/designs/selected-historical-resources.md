---
slug: selected-historical-resources
uuid: f13ff9b4-9ee9-4cee-b979-f998559c1166
kind: design
status: implemented
description: "Bound historical body reads and retain typed resources and recognized conversations."
labels: [selection, resources, provenance, conversations]
requires-decisions: ["ADR-000", "ADR-001"]
dependencies: [clio-edn-development]
dependendents: []
epics: []
---

# Selected historical resources

As a person recovering idea lineage across repositories, select the historical
resources that may be read, retain their source structure, and compare them
without rewriting the original histories. This slice is explicitly stacked on
PR18's existing Clio `edn` provider; it does not introduce another persistence
backend or claim that PR18 is merged or warning-free.

## Selection contract

`ingest --selection-policy FILE` reads one bounded, non-evaluating EDN value:

```clojure
{:selection/version 1
 :selection/repository "/absolute/path/to/isolated-copy"
 :selection/paths ["notes.md" "design.jsonc" "history.jsonl"]}
```

The repository string must equal the explicit repository argument. Paths are
literal Git-relative strings; case, Unicode, commas and wildcard characters
are preserved. Traversal/absolute/empty paths, unknown keys, extra values,
unsupported versions and custom reader tags are refused before adapter startup.
Policy files are limited to 1 MiB and 4096 paths. No policy means the documented
legacy Markdown selection; there is no hidden switch to broad structured/chat
ingestion. Use an explicit policy for bounded archaeology.

The policy and resolved reachable commit OIDs are durable command intent.
Changing selection, repository, refs or the resolved history under an accepted
request UUID is refused. Use a new UUID for a new selection or a moved ref. The run freezes its initial
metadata walk for projection; missing refs are refused before body reads.
Regular-file selection applies equally to current and parent tree metadata.
Extraction and embedding filter persisted revisions by both path and the current
run's commits, so a reused store cannot rehydrate previously admitted resources
outside the new scope. Old facts/index projections are retained; narrowing is
not deletion or a permanent prohibition on chats. Deletions remain revision
observations, not invented child-file bodies. Resume uses admitted extraction
identities instead of treating processed-count checkpoints as a safe list offset.

Explicitly selected ingest/inspect bodies are limited to 1 MiB at the Git loader
before materialization. Oversized objects produce `blob-too-large`. Existing
source Git DAGs remain authoritative; perform registration and ingestion in
isolated copies when original worktrees/metadata must remain unchanged.

## Typed resources and first-class conversations

JSON, JSONC, JSONL, EDN, TOML and YAML/YML have versioned derived projections in
the existing extraction observation port. `inspect --path PATH --commit REF` handles literal path delimiters. New union contracts admit these and
scoped runs without changing historical v1 schemas. Clio persists/replays them;
Mongo codecs retain the same portable payloads without a new backend. These are
historical memory observations, not accepted lineage interpretations.

Each projection retains original bytes as base64, original-byte SHA-256,
format, parser identity/version, parser status and source spans. Its enclosing observation/provenance
binds resource ID, commit, exact path and blob. Strings, booleans, null, integers,
decimals, floats, collection kinds, EDN keywords/symbols/sets/lists/ratios/UUIDs/
instants, and TOML local temporal kinds remain typed. Decimal/integer parsing
does not pass through lossy JavaScript numbers. Source formatting/comments are
not reconstructed from parsed values; the original bytes are retained.

JSONC supports Java-style comments and trailing commas. JSON is strict about
both and rejects duplicate keys/trailing values. JSONL retains nonblank record
order and individual malformed-record statuses/spans. EDN is data-only: reader
evaluation and custom reader tags are refused; built-in EDN UUID/instant values
remain supported. YAML uses SafeConstructor, rejects duplicate keys, multiple
documents, unsafe tags and collection aliases, and bounds depth/input size.
TOML errors and malformed UTF-8 are explicit. Unsupported extensions remain
explicit unsupported data, never silently treated as Markdown.

Recognized schemas are `epiphany.conversation.v1` (declared message array),
ChatGPT `mapping` + `current_node` exports (including arrays of conversations),
and eta-mu session/message JSONL. Messages retain source role/speaker when
present, supplied IDs/timestamps, source paths/spans, and array/line order or
explicit branch parentage. Missing facts remain absent; no ingestion-time
timestamp or guessed author is substituted. Generic objects with a `messages`
key are not automatically chats. Malformed known schema claims produce a
conversation diagnostic while retaining valid typed source data. Embedded
instructions are inert text. Lexical sections are rebuildable views over the
source; typed/chat observations are not replaced by flattened search text.

Dependency justification: SnakeYAML 2.3 supplies a restricted safe constructor
and Tomlj 1.1.1 supplies TOML semantics/temporal types; reuse the existing Jackson
dependency for strict JSON/JSONC rather than write ad hoc format parsers.

## Existing ownership and reuse

Read-only discovery inspected Osmos's `kms-ingestion.drivers.eta-mu-sessions`
at `0c33018e27f861536822086afcdab666a60add44`, Knoxx's corresponding composition
at `fb08a10a8aa32a594cc97ae11b820113de4cf386`, and OpenPlanner's
`src/lib/importers/chatgpt.ts` at `5cbc8f200181114750e7f6e824309ed82e938c65`.
They already recognize session/message and ChatGPT source shapes. Their
filesystem/ZIP scanning and event sinks are not copied or invoked here.
Epiphany's JVM projection retains those source shapes plus historical Git
context; unlike ingestion conveniences, it must not truncate, skip malformed
records silently, invent a session ID/author or substitute the current time.

Osmos owns acquisition of general resources; Epiphany owns historical context
and these source-bound interpretations. OpenPlanner owns transferable graph
contracts/operations; Knoxx composes existing products. This change neither
modifies OpenPlanner nor pushes events to its service. A resource's usefulness
does not depend universally on Git or on a Mongo-free provider; this initial
history adapter uses Git, and the existing providers remain independently useful.

## Connected remaining acceptance

- **Span precision:** typed nodes/conversations currently disclose whole-document
  spans; JSONL records disclose whole-line spans with UTF-8 byte offsets. Per-value
  token spans, YAML anchors/comments and TOML key provenance need parser-specific
  source maps. Never advertise current spans as token-exact. Original bytes and
  source paths allow precise re-inspection meanwhile.
- **Formats:** YAML multi-document streams/collection-alias graphs and arbitrary
  EDN tagged literals are explicit refusals. Add versioned resource forms and
  executable fixtures before supporting them; never enable custom constructors.
- **Chats:** arbitrary Markdown transcripts and additional export schemas need
  named recognizers with role/order/absence fixtures. Non-text attachments/tool
  payloads remain in lossless typed source; message text is a derived view.
- **Composition:** export historical resource/message nodes and source references
  through existing OpenPlanner shapes; do not fabricate timestamps to satisfy an
  event contract. Qualify source-to-memory linkage across multiple repos before
  claiming a complete connected lineage workflow.
- **Existing product gaps:** UI rendering, trace candidate-store integration,
  repository-qualified search/evidence endpoints and human review remain separate.
  No parsed chat, lexical hit or similarity candidate is automatically accepted.
- **Provider readiness:** PR18's native warning gate and outstanding review
  issues remain explicit dependencies; this draft does not merge or waive them.

Synthetic tests exercise all listed format families, schema-negative inputs,
lossless bytes/types, chat provenance, parent/deletion selection, durable retry,
reused-store narrowing and BSON round-trip codecs. Real chat bodies are not
fixtures or patch content. Separate-process and bounded corpus evidence must be
reported for the exact current source, separately from earlier unit passes.

## Local verification

The implemented source passed 827 unit tests / 2681 assertions, configured lint
with zero errors/warnings, formatting, layer boundaries, interop ratchet and JVM
build. These used a task-local Maven cache and PR18's pinned Clio
`ff48f090965be5c17cca1e3662d7402b383510c1`, without changing global packages.

The stock launcher ingested four original commits / five selected file revisions
across isolated Discord Chatter, Duck and Promethean copies, with zero extraction
failures. A new process repeated the same Discord request with zero new revisions
or extractions and byte-identical canonical ledger bytes. A later launcher process
searched the Lucene projection successfully. No runtime selection override or
private-chat fixture was used. Shallow coverage was deliberately bounded; no
decade-wide or accepted conceptual-lineage result is claimed.

The strict native warning gate remains red on the existing JDK restricted-access
and Lucene vector-module diagnostics. This is separate from functional passes;
no warning suppression, service operation or PR18 readiness claim is made.
New implementation code follows the task's GPL-3.0-or-later application contract;
third-party components retain their own licenses.

## Draft review regression pass

On published draft head `2c6c710aca81ec0f1a8bbc36c352a128a8ace702`,
four synthetic regressions produced 16 failures / 39 assertions. They reproduced
legacy no-policy embedding/retry reopening previously selected chat bodies,
TOML quoted literal keys being read as dotted expressions, and two chat-boundary
cases losing parsed data or silently dropping a malformed sibling claim.

The repair applies the existing Markdown policy to persisted revisions, with
missing path metadata refused. TOML retrieves each immediate key as one literal
path component. Chat projection validates claims independently from structured
parsing: malformed claims retain the typed source and receive source-path/span
diagnostics, while valid siblings remain available. JSONL message errors identify
the original line and field. This does not rewrite existing durable observations
or automatically re-extract previously stored derived records.

The repaired source passes 831 unit tests / 2721 assertions, zero failures,
including real JGit/Clio reused-store regressions for both extracted and failed
chat revisions. Only the embedding provider is substituted; historical body reads
are audited. Lint has zero errors/warnings; formatting, boundaries, interop ratchet
and JVM build pass. The prior functional and warning-gate results above retain
their original scope; no inherited PR18 blocker is claimed resolved.
