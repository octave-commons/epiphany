---
id: 01900d7c-7f3a-7e8b-9c4d-000000000031
title: "Epic 28: Distributed Experiment Scheduling"
status: icebox
type: epic
priority: low
phase: 4
design: docs/notes/design/phase-4-simulation-laboratory.md
size: 8
labels: [scheduling, distributed, resources, cluster]
---

# Epic 28: Distributed Experiment Scheduling

Use the four machines as a resource-aware research cluster, without treating weak nodes as failed versions of the strong ones.

## User outcome

“Approved simulations, data preparation, model inference, and visualization jobs run on the right machines with visible resource budgets, recoverable failures, and no accidental starvation of interactive work.”

## Scope

Extend the event/job infrastructure with resource-aware scheduling: CPU cores, RAM, GPU/VRAM, NPU if usable, disk space, network budget, job priority, expected duration, retry policy.

Workload classes: `:interactive-query`, `:ingestion`, `:batch-embedding`, `:simulation-small`, `:simulation-sweep`, `:gpu-inference`, `:archive`, `:visualization-precompute`.

Hardware roles:

- **Ultra 9/4070 Ti:** GPU inference, embedding/reranking, high-value simulation batches.
- **Ryzen 7:** primary database/index projections, CPU experiments, orchestration.
- **Ryzen 3/i5:** object storage, crawling, archive, metrics/logging, low-priority or embarrassingly parallel preparation work.

Add quotas and preemption: interactive queries outrank background sweeps; no simulation can consume unbounded disk; GPU jobs have VRAM and runtime ceilings; low-priority work pauses under resource pressure.

## Acceptance criteria

- Every job declares a resource class and maximum budget.
- The scheduler can explain why a job is pending, running, paused, retried, or failed.
- Interactive retrieval remains available under batch/simulation load.
- No weak node is scheduled for an out-of-memory-prone service by default.
- Job queues survive worker restart and retain idempotency keys.
- Resource telemetry is visible by node, workload class, experiment, and user/project.
- A run can be resumed or cleanly restarted from checkpoints where the model supports it.

## Next step

Design the job scheduler data model and resource-class taxonomy.
