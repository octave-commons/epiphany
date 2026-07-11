# Planning



The planning phase of development operates on the kanban. 


```
1. **Intake & Associate**
```

Find or create the task file in `docs/tasks/stories` or `docs/tasks/chores`; never work off-board;
If the matched or created task is a story, also locate the epic it belongs to.

Chores do not need to be affiliated with an epic, but stories do.

If a story is not being tracked by an epic, search `docs/epics` for one who's status is `incoming`, `accepted`, or `breakdown`. You should not expand the scope of an epic who's `ready`, `todo`, `in_progress`, or `review`.

If no matching un-scoped epic
A new epic, will need to be created,

```
2. **Clarify & Scope**
```

Anchor on the kanban card as the single source of truth and, before advancing, do the solo pass:

- Confirm the desired outcomes so the card reflects the slice you intend to deliver.
- Capture acceptance criteria or explicit exit signals on the task so "done" is unambiguous.
- Note any uncertainties, risks, or open questions directly on the task to surface follow-ups early.
- For story work, link the backing **design** in the epic tracking the story and confirm that design cites grounding research. If no design exists yet, the epic is bounced to `breakdown` and the the first slice is to *write the design*, not the feature — see [Grounding](#grounding-research--design--task).
- Record the scoped plan and supporting notes on the linked task before moving to step 3.

```
3. **Breakdown & Size**
```

Break into small, testable slices; assess **complexity, scope, and Level of Effort (LoE)** and assign a Fibonacci score from **1, 2, 3, 5, 8, 13** on the task card. Scores of **13+ ⇒ must split**; **8 ⇒ continue refinement before implementation**; **≤5 ⇒ eligible to implement**. Any score **>5** must cycle back through clarification/breakdown until the slice is small enough to implement, capturing the updated score on the task card.&#x20;

4. **Ready Gate** _(hard stop before code)_
   Only proceed if:

   - A matching task is **In Progress** (or you move it there), and WIP rules aren’t violated.&#x20;
   - The slice is scored **≤5** and fits capacity after planning; otherwise continue refinement/splitting.&#x20;
   - **Feature/spec tasks link a design that cites grounding research** (Definition of Ready). Hygiene, research, and design tasks are exempt — see [Grounding](#grounding-research--design--task).

```
