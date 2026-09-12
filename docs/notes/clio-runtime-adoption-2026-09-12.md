---
slug: clio-runtime-adoption-2026-09-12
uuid: 691be5a4-e7f1-445d-932b-5d0e70f4d20b
kind: report
status: open
description: "Native failure-first evidence for Clio schema snapshot ordering and reopen durability."
labels: [development, clio, verification]
---

# Clio runtime adoption

The observation adapter now captures locked ledger snapshots before loading their
schema revisions. It delegates this order to `runtime/canonicalize-files`.
Reopening first validates and replays history, then uses `runtime/ensure-durable!`
to validate and force the existing ledger under its owning lock before returning.
Missing historical schemas still refuse before `runtime/open` can materialize the
current catalog. The logical no-op path also uses the runtime durability helper,
which loads revisions after capturing the locked ledger.

This recovery slice started at Epiphany
`7c157fb3a824cf6ff23205486ccf8e285ea78fba` and used separately restored, clean Clio
`ff48f090965be5c17cca1e3662d7402b383510c1`. A classpath assertion resolved
`clio/infra/runtime.cljc` from that checkout, not the changing sibling workspace.
No dependency manifest or canonical event was rewritten.

The new native tests first produced **2 tests / 18 assertions / 8 failures**:

- A second real Epiphany adapter materialized an evolved catalog and appended an
  accepted record immediately before the first reader captured its ledger.
  The stale revision inventory rejected that valid event as an unknown schema.
  This is a deterministic interleaving in one JVM, not a separate-process claim.
- An actual append left visible bytes after an injected inode or parent-directory
  force failure. Reopening twice during the same persistent failure incorrectly
  succeeded. A later successful reopen did not force the existing inode either.

After the reader and reopen changes, the unchanged tests passed **2 tests / 18
assertions / 0 failures**. Recovery retained byte-identical canonical history and
the accepted record. Strict clj-kondo on the new test file reported **0 errors /
0 warnings**. These tests exercise native JVM files and the actual Clio append,
read, validation and force paths; they do not establish resilience to a real power
loss or all filesystems.

Run the focused test from Epiphany, supplying the immutable checkout path:

```bash
clojure -Sdeps '{:deps {eta-mu/clio {:local/root "/workspace/scratch/3655842e43cf/eta-clio-origin/packages/clio"}}}' \
  -M:test --focus epiphany.infra.adapters.clio-runtime-ordering-test
```

The first run needed to restore the project's Maven and Clojars dependencies in
the shared sandbox cache. Resolution succeeded through the restored JVM proxy
and trust configuration. The later wider adapter run, concurrent with the
explicit write-result contract migration, reported **26 tests / 177 assertions /
23 failures**: older fixtures still demanded `nil` for record writes. That run
is not presented as green coverage. The coordinated migration must require exact
`:accepted` and `:duplicate` results and rerun the broader gates.

| Evidence | SHA-256 |
| --- | --- |
| [Actual RED output](evidence/clio-runtime-ordering-red.txt) | `d8f31176e914632e1b503e3f7e65ab7f58c905965a0101050c8783460a9c681a` |
| [Actual GREEN output](evidence/clio-runtime-ordering-green.txt) | `e9a6244a2dbea91c92e008a986e33555af667064fabcbcdff0456bf9cd725f68` |
| [Classpath assertion](evidence/clio-runtime-ordering-classpath.txt) | `452c81621b030d710b4ce365873ec7083401efcb8c8d837050b15d9776daeb92` |
| Unchanged new regression source | `70c2d5d2910e58550c6ab8a90ed67f1c81f5bb5bbf7f346581fa23ef1f298320` |
| Immutable Clio runtime source | `f46b7731ee0bfb570497d62c4815b451625d9ce55bd4c1c89c919bf4c6da13e0` |

The focused green receipt precedes the coordinated public write-result change.
It is not a whole-project verification receipt for that later combined source.
