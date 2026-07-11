# Epiphany

## Core idea

Epiphany treats your local Git repositories as a long-lived personal and technical archive.

It scans repository history—not only the files currently checked out—and extracts Markdown documents into structured, revision-scoped evidence units such as:

- documents;
- headings and sections;
- paragraphs, lists, tables, quotes, and code blocks;
- front matter;
- links;
- named entities;
- commit and path history.

It then makes those units searchable through several complementary methods:

- lexical/full-text search;
- semantic/vector retrieval;
- metadata and date filtering;
- path and repository filtering;
- Git history traversal;
- continuity signals;
- later, graph relationships and geospatial data.

The goal is fast retrieval, but with the ability to inspect exactly *why* a result or relationship exists.

## Evidence first

The project is designed around a strict distinction between four kinds of knowledge:

| Category | Meaning |
|---|---|
| Observed | Directly present in Git history, a file, a commit, a path, or another canonical source |
| Provisional | A model, heuristic, or similarity process proposed a possible relationship |
| Accepted | A human explicitly reviewed and accepted an interpretation |
| Rejected | A human explicitly rejected a candidate; it remains auditable but is hidden by default |

## Continuity model

Epiphany does not assume that a path equals an idea.

A file can stay at the same path while completely changing purpose. Conversely, a file can move or be copied while retaining its exact content. Git does not permanently store semantic “rename” or “same document” facts; it stores object history, while rename detection is a comparison-based inference.

So Epiphany keeps separate relationships for:

- **same path across adjacent commits** — observed path continuity;
- **same blob at another path** — exact content relocation or copy;
- **similar text or structure** — inferred similarity;
- **same concept or ongoing argument** — provisional or accepted semantic continuity;
- **epoch boundary** — a reviewable claim that a long-lived path was repurposed.

For Markdown, continuity can be estimated from signals such as:

- text similarity;
- front matter stability;
- overlap in explicit links;
- time gaps;
- named-entity overlap.

For code, later policies can use different evidence:

- AST-shape overlap;
- public-symbol overlap;
- dependency changes;
- test overlap;
- comments and documentation;
- code-specific structural changes.

Markdown and code are intentionally not forced into one generic continuity model.
