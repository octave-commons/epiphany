# Epiphany Documentation Workflow

- `inbox/`
- `notes/`
- `adrs/` (Architectural Decision Records)
- `archive/`
- `designs/`
- `research/`
- `kanban/`

## architecture

Architectural decision records

## Tasks


## Common Frontmatter Fields

```yml
slug: name-with-hyphens
uuid: 3d4f5d0c-8b64-4e3e-9f7f-5f7a1b2c6d90
kind: note | decision | design | story | epic | roadmap | report | research | etc
status: enum-valid-status-for-kind
description: "A short sentance or two describing the document"
labels: [frontend, backend, devops, etc]
```


Many document types have fields which refer to other documents.
A document may be always be referenced to by filename, slug or uuid.
