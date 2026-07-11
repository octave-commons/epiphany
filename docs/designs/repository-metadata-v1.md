---
slug: repository-metadata-v1
uuid: 01900d7c-7f3a-7e8b-9c4d-000000000208
title: "Repository Metadata v1"
kind: design
description: "Defines the minimal v1 Git-local repository metadata file containing only a stable platform resource ID, plus cursor policy and family discovery timing."
labels: [architecture, phase-1, identity, metadata]
created: "2026-07-11"
source: "docs/inbox/2026.07.11.06.03.02.md"
---

Yes. This keeps `.git` metadata intentionally tiny, preserves exact path identity, and treats continuity/epoch boundaries as a typed gradient rather than a brittle binary classification.

## Metadata v1

`repository.edn` should contain only the durable local identity needed to recognize the repository after its directory moves:

```clojure
{:resource-id #uuid "7a6b0d26-..."
 :cursor-commit nil}
```

But I would make `:cursor-commit` **not part of the initial contract**. It is processing/projection state, not repository identity, and it will vary by operation: commit scan, Markdown extraction, embedding, indexing, lineage analysis, and so on.

So the actual minimal v1 file is:

```clojure
{:resource-id #uuid "7a6b0d26-..."}
```

That is enough to satisfy the intended purpose:

```text
repository moves on disk
  -> indexer finds resolved .git directory
  -> reads corpus-archaeology/repository.edn
  -> recognizes the existing platform resource ID
  -> records the new location as a new observation
```

Everything else belongs in MongoDB:

- first/last observed filesystem location
- Git common directory and worktree locations
- current refs and HEAD
- object format
- commits seen
- ingestion job status
- cursor/checkpoint per projector
- cache placement
- user/source configuration
- repository-family membership
- history-rewrite observations.

This is a good separation: **Git-local metadata survives a move; MongoDB records the platform’s evolving knowledge about that repository.**

### File location

```text
<resolved Git common directory>/
  corpus-archaeology/
    repository.edn
```

For a standard repository that becomes:

```text
my-notes/
  .git/
    corpus-archaeology/
      repository.edn
```

For linked worktrees, resolve the common Git directory first; the working tree’s `.git` can be a pointer file rather than a directory. [manpages.ubuntu](https://manpages.ubuntu.com/manpages/focal/man1/git-worktree.1.html)

### Small schema rule

The file should be valid EDN and satisfy one narrow schema:

```clojure
[:map
 [:resource-id uuid?]]
```

No system configuration, cache hints, credentials, paths, arbitrary notes, or runtime state. That keeps it portable, legible, and hard to corrupt semantically.

## Cursor policy

You are right that a single repository-level cursor is probably the wrong abstraction.

A cursor is not “where the repository is.” It is:

> Where a particular projection has successfully processed a particular observed history.

For example:

```clojure
{:projection/id :git-observation-v1
 :resource-id ...
 :cursor {:last-processed-commit "..."}

 :status :healthy}
```

```clojure
{:projection/id :markdown-extraction-v1
 :resource-id ...
 :cursor {:last-processed-revision-at-path-id ...}

 :status :behind}
```

```clojure
{:projection/id :embedding-bge-m3-v1
 :resource-id ...
 :cursor {:last-processed-section-expression-id ...}

 :status :healthy}
```

Those records should live in MongoDB alongside the projections they describe. Putting a generic `:cursor-commit` in `.git` would imply one linear processing position even though the repository has branches, merges, rewrites, multiple processing stages, and independently replayable projections.

## Family discovery timing

We can simplify this too:

> Compare shared commit OIDs once when a repository instance is first registered. Do not repeatedly recompute family identity during ordinary indexing.

This is reasonable because ordinary commits add descendants to an existing commit graph; they do not cause two previously unrelated histories to acquire a shared preexisting commit. Git treats histories with no common ancestor as unrelated by default, which reflects the underlying commit-parent graph relationship. [git-scm](https://git-scm.com/docs/git-merge)

### Exception: detected rewrite or replacement

The exception you identified is correct: if the system detects a potential history rewrite—such as familiar refs suddenly resolving to unrelated commit histories, or an instance’s known tips disappearing and being replaced—then it should schedule a **family-reassessment check**.

That check is not a normal polling loop. It is a consequence of an unusual observation:

```clojure
{:event/type :repository/history-rewrite-suspected
 :resource-id ...
 :old-observed-tip "..."
 :new-observed-tip "..."
 :reason :no-common-ancestor-at-known-ref}
```

Then:

```text
1. Preserve prior observations.
2. Compare the newly reachable commit set against known families.
3. If overlap exists, keep the repository instance in its family.
4. If no overlap exists, represent a history-replacement/rewrite event.
5. Do not silently merge or split families.
```

For a leaked secret, a filtering tool can rewrite Git history, changing commit IDs even if much of the apparent content remains. That is precisely a case where the original shared-commit test may no longer prove continuity, so reassessment and user review are appropriate. [github](https://github.com/newren/git-filter-repo/blob/main/Documentation/git-filter-repo.txt)

## Exact path policy

The identity-level rule is settled:

> The exact string returned by the source traversal is the canonical path spelling.

No normalization means we do **not** transform:

```text
.ημ/              -> .eta-mu/
MyFile.md         -> myfile.md
café.md           -> café.md
a\b.md            -> a/b.md
```

The system records the string exactly as supplied by Git tree traversal for Git facts, and by filesystem enumeration only where dealing with uncommitted/local material in the future.

For Git-backed Phase 1 content, prefer the Git tree entry path over the operating system’s filesystem enumeration whenever possible. This gives the platform a path in the same coordinate system as the commit/tree/blob evidence, without relying on local filesystem casing or Unicode behavior.

```clojure
{:path/raw ".ημ/identity.md"
 :path/source :git-tree-entry
 :path/comparison :exact}
```

A later search alias may let an ASCII query find `.ημ`, but that is an optional retrieval feature and never alters the identity record.

## Continuity signals

For Markdown Phase 1, use exactly the five signal classes you named:

| Signal | Measures | Initial use |
|---|---|---|
| Similarity | Textual/structural continuity between adjacent revisions or candidate sections | Main continuity feature |
| Front matter | Schema/type, stable IDs, project/category fields, title changes | Strong structural clue when present |
| Links | Explicit outbound/inbound links, target overlap, moved references | Strong authored evidence |
| Time gaps | Time between commits/revisions | Weak contextual evidence, never decisive alone |
| Named entities | People, projects, systems, places, technical terms | Topic/purpose continuity and change |

Do not add embeddings, LLM judgments, or global taxonomy labels to the **first deterministic continuity model**. They can later become additional, versioned candidate-generation signals, but your initial model should be auditable from source text and simple extraction output.

### Signal directions

Not every signal means the same thing:

```text
High text similarity          -> supports continuity
Stable front matter identity  -> strongly supports continuity
Changed front matter type     -> supports epoch boundary
Stable/overlapping links      -> supports continuity
Abrupt link-graph replacement -> supports epoch boundary
Short time gap                -> weakly supports continuity
Long time gap                 -> weakly supports boundary review
Entity overlap                -> supports continuity
Entity replacement            -> supports boundary review
```

The system should retain **raw measurements**, not only the final score:

```clojure
{:continuity/model :markdown-continuity-v1
 :from/revision-at-path-id ...
 :to/revision-at-path-id ...

 :signals
 {:text/similarity 0.43
  :frontmatter/stability 0.10
  :links/jaccard 0.04
  :time/gap-days 231
  :entities/overlap 0.13}

 :continuity/score 0.28
 :continuity/class :weak
 :computed-at ...}
```

That makes later reweighting possible without pretending earlier judgments were made by a different model.

## Separate defaults from the outset

Agreed. The system should have an explicit artifact-kind policy map from version one, even though Phase 1 initially executes only Markdown policy.

```clojure
{:continuity/policies
 {:markdown
  {:model :markdown-continuity-v1
   :signals [:text/similarity
             :frontmatter/stability
             :links/overlap
             :time/gap
             :entities/overlap]
   :classes {:strong [0.90 1.00]
             :substantial [0.70 0.90]
             :partial [0.40 0.70]
             :weak [0.15 0.40]
             :apparent-discontinuity [0.00 0.15]}
   :epoch-boundary-threshold 0.90}

  :code
  {:model :code-continuity-v1
   :signals [:ast/shape-overlap
             :symbols/public-overlap
             :dependencies/overlap
             :tests/overlap
             :comments/overlap
             :time/gap]
   :epoch-boundary-threshold 0.95}}}
```

The exact initial weights and thresholds remain empirical. The important decision is that Markdown and code do not share an accidental universal definition of “continuity.”

For Markdown, a sustained topic/structure replacement may be meaningful. For code, large changes can be routine while namespace role, public API, imports, and tests retain long-lived continuity.

## ADR 001 is now ready

The actual decision is concise:

> **Each registered local Git repository receives an automatically created, Git-local EDN file containing only a stable platform resource ID. Repository instances join a shared repository family only when they share commit OIDs at registration; reassessment occurs only after a suspected history rewrite. Git/tree paths are stored and compared exactly as observed. Persistent paths supply default document-role continuity, while Markdown and code apply separate weighted continuity models and propose hard epoch boundaries only above artifact-specific thresholds.**

All remaining details—exact MongoDB collections, event envelopes, projection checkpoints, section extraction schema, and cache key/value implementation—belong to later ADRs rather than this identity decision.
