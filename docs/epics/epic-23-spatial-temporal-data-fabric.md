---
id: 01900d7c-7f3a-7e8b-9c4d-000000000026
title: "Epic 23: Spatial-Temporal Data Fabric"
status: incoming
type: epic
priority: low
phase: 4
design: docs/notes/design/phase-4-simulation-laboratory.md
size: 8
labels: [spatial, temporal, data, stac, geospatial]
---

# Epic 23: Spatial-Temporal Data Fabric

Ingest, normalize, index, and query spatial-temporal observations and reference data as evidence-linked assets.

## User outcome

“I can ask what happened in a place and time, what data supports it, what geometry/time resolution it has, and whether it is suitable for a particular simulation.”

## Scope

Support spatial primitives (points, lines, polygons, raster footprints, bounding boxes, CRS metadata, boundaries, named places) and temporal primitives (instant, interval, observation time, acquisition/publication time, valid vs. transaction time).

Use STAC-compatible metadata where practical: collection, item, spatial extent, temporal extent, asset URLs, media type, license, provider, version.

Ingest external sources only through Phase 3 source governance: weather observations/forecasts, climate/environmental reference data, public geospatial layers, and historical incident/simulation datasets where legally and ethically appropriate.

Build spatial-temporal joins between external observations, simulation inputs/outputs, local notes/code/experiments, and derived geographic entities.

## Acceptance criteria

- Every spatial-temporal observation retains source, acquisition time, geometry, temporal validity, resolution, license, and transformation history.
- The system distinguishes observed data, forecasts, synthesized scenarios, and simulation outputs.
- Queries support spatial containment/intersection/proximity plus time-window filtering.
- Dataset suitability checks surface coverage, resolution, missingness, coordinate system, license, and known limitations.
- Transformations between coordinate systems/resolutions are versioned and replayable.
- The user can inspect a map result and pivot to the artifact/source that produced every layer.
- Storage policy prevents accidental materialization of massive raster/forecast archives without an approved quota.

## Next step

Design the spatial-temporal entity schema and STAC-compatible catalog model.
