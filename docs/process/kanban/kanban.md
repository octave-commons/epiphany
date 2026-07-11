# Kanban Workflow

- **States (C)**: the board’s columns.
- **Initial state (S)**: **Incoming** (new tasks land here).
- **Transitions (T)**: moves between columns.
- **Rules R(Tₙ, t)**: predicates over task `t` that permit or block transition `Tₙ`.
- **Single source of status**: each task has exactly one column/status at a time.
- **Board is law**: never edit the board file directly; tasks drive board generation.
- **WIP**: a transition fails if the target state’s WIP cap is full.

### Workflow State Transition Diagram

When updating a tasks status, you must respect the transition rules.
Tasks may only transition from their current status, to a status it has ana
arrow to in following workflow diagram.

In addition to tasks being limited to what statuses they can transition to from their
current state, each transition has conditions that must be met to be allowed.

```mermaid
flowchart TD

  %% ====== Lanes ======
  subgraph Brainstorm
    IceBox["🧊 Ice Box"]
    Incoming["💭 Incoming"]
  end

  subgraph Planning
    Accepted["✅ Accepted"]
    Breakdown["🧩 Breakdown"]
    Blocked["🚧 Blocked"]
  end

  subgraph Execution
    Ready["🛠 Ready"]
    Todo["🟢 To Do"]
    InProgress["🟡 In Progress"]
    InReview["🔍 In Review"]
    Done["✅ Done"]
  end

  subgraph Abandoned
    Rejected["❌ Rejected"]
  end

  %% ====== Forward flow ======
  IceBox --> Incoming
  Incoming --> Accepted
  Incoming --> Rejected
  Incoming --> IceBox
  Accepted --> Breakdown
  Breakdown --> Ready
  Ready --> Todo
  Todo --> InProgress
  InProgress --> InReview
  InReview --> Done

  %% ====== Cycles back to Planning / queue ======
  Ready --> Breakdown
  Todo --> Breakdown
  InProgress --> Breakdown
  InReview --> Breakdown

  %% ====== Session-end, no-PR handoff ======
  InProgress --> Todo

  %% ====== Review crossroads (re-open work) ======
  InReview --> InProgress
  InReview --> Todo

  %% ====== Defer / archive loops ======
  Accepted --> IceBox
  Breakdown --> IceBox
  Rejected --> IceBox

  %% ====== Blocked (narrow, explicit dependency) ======
  Breakdown --> Blocked
  Blocked --> Breakdown
```
