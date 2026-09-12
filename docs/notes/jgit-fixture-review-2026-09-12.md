---
slug: jgit-fixture-review-2026-09-12
uuid: ac7d6c72-af97-4d7c-9562-c37b7f7ce8ef
kind: report
status: open
description: "Actual JGit fixture repair after a review was resolved without the source change."
labels: [clio, review, verification, fixtures]
---

# JGit ingestion fixture review

[CodeRabbit finding 3997056207](https://github.com/octave-commons/epiphany/pull/18#discussion_r3997056207)
was marked resolved remotely, but source inspection at clean local
`e8513f80053636fe6c95ff55775431680b0dcb90` (published as
`7973f47f7961a3d5e03a0bc5696c955ed7d76992`) confirmed the fixture still called
`clojure.java.shell/sh` with `git`. That resolution did not establish that the
requested JGit change existed. AGENTS.md requires the existing JGit dependency
for repository operations.

A disposable executable at the front of only the test command's PATH refused
external `git` calls with exit 97. The unchanged focused namespace then reported
**4 tests, 17 assertions, 3 errors**. Each affected native ingestion case failed
while constructing its repository. The result-contract case still passed.
No installed Git binary, application source, dependency pin or global PATH changed.

The fixture now initializes the real repository, adds the same literal `notes.md`
bytes, and commits with the same author, committer and message through pinned
JGit 7.3. The repository handle closes before the existing concurrent Clio readers
run. The barriers, independent adapter handles, durable history, acknowledgement
counts, index identity assertions and index-failure case remain unchanged.

Under the identical refusing Git guard, the focused namespace passed
**4 tests, 27 assertions, 0 failures or errors**, with no warnings. Configured
full lint reported **0 errors and 0 warnings**; formatting and layer-boundary
checks passed. Their output is preserved without warning filtering. The test
selected the same clean combined Clio revision
`2b7bfbefa580d512262ca18f9163ecba43e54cc5` through an explicit alias override.
The independently reviewed fixture diff had no confirmed correctness issue.

[Exact command, fixture hash and raw receipts](evidence/jgit-fixture-review.json)
retain both the failure and passing results. This bounded test-only correction
has its own focused evidence. The prior full 803/2489 and actual service 22/108
receipts remain attached to their original source; they are not relabeled as a
new full-suite run. No production build was needed, and the known native Lucene
zero-warning acceptance blocker remains unchanged.
