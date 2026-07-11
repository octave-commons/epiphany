---
name: deep-research
description: "Deep academic research for Gates of Truth. Investigates arxiv, cites sources, runs for extended periods, generates docs/research/ notebooks with LaTeX, Clojure pseudocode, charts, graphs, visualizations, and toy models. Covers cosmology, physics, geology, biology, atmosphere, sociology, anthropology — every domain that feeds the simulation."
triggers:
  - "deep research"
  - "research this topic"
  - "investigate the science"
  - "what does the literature say"
  - "find papers on"
  - "arxiv search"
  - "write a research notebook"
  - "research report"
license: GPL-3.0-or-later
compatibility: opencode
metadata:
  audience: agents
  workflow: academic-research
  version: 2
---

# Skill: Deep Research for Truth

## Goal

Produce rigorous, citation-backed, multi-domain research notebooks that feed
the Gates of Truth simulation with real physics, real biology, real geology,
real sociology. Every finding must be traceable to published work. Every model
must include enough implementation detail to promote into `domain/` code.

## Use This Skill When

- The user asks to research a scientific topic for the simulation.
- An actor is dispatched to investigate a domain (cosmology, geology, biology, etc.).
- A new simulation phase needs literature grounding.
- A physics model needs validation against published values.
- The user says "deep research", "research notebook", or "investigate the science".

## Do Not Use This Skill When

- The user wants a quick factual answer (use web search).
- The task is pure code implementation with no research component.
- The topic has no connection to the simulation domains.

## Core Principles

1. **The physics is real until the Gates open.** Every simulation model must be
   recognizably descended from a published method, even at toy resolution.
2. **Cite everything.** No claim without a source. arxiv DOIs, textbook
   chapters, dataset references — every assertion has a receipt.
3. **Run long.** Deep research may take hours. The agent must be comfortable
   reading multiple papers, cross-referencing, running calculations, and
   iterating. Do not rush to conclusions.
4. **Output is permanent.** Research notebooks live in `docs/research/` and are
   version-controlled. They are the knowledge base of the project.
5. **Promotion path is explicit.** Every notebook ends with a clear path from
   "this is what the literature says" to "this is how it becomes Clojure code".

## Output Structure

Every research notebook is written to `docs/research/<domain>/<slug>.md`:

```
docs/research/
  cosmology/
    primordial-nucleosynthesis-yields.md
    cmb-anisotropy-power-spectrum.md
  geology/
    mantle-convection-rayleigh-number.md
    plate-boundary-force-balance.md
  biology/
    lotka-volterra-stochastic-extensions.md
    abiogenesis-threshold-models.md
  atmosphere/
    hadley-cell-scaling-laws.md
    clausius-clapeyron-cloud-feedback.md
  physics/
    sph-viscosity-artifacts-balsara-switch.md
    barnes-hut-opening-angle-convergence.md
  culture/
    agent-based-settlement-patterns.md
    mythogenesis-computational-models.md
  cross-domain/
    star-planet-magnetic-coupling.md
    impact-delivery-volatile-fractionation.md
```

### Notebook Format (Markdown with LaTeX)

Every notebook MUST follow this structure:

```markdown
# <Title>

**Domain:** <domain> | **Phase:** <0-6 or "cross-phase">
**Date:** <ISO date> | **Author:** <agent-id>
**Status:** draft | validated | promoted
**Primary sources:** <DOI or arxiv ID list>

---

## 1. Research Question

<What are we trying to model? Why does it matter for the simulation?>

## 2. Literature Survey

### 2.1 <Subtopic>

<Paper-by-paper summary with inline citations>

> **Key finding:** <blockquote the most important result>

**Citation:** <Authors> (<Year>). <Title>. <Journal>. DOI/arXiv:<id>

### 2.2 <Subtopic>
...

## 3. Governing Equations

The core physics expressed in LaTeX notation:

$$
\frac{\partial \rho}{\partial t} + \nabla \cdot (\rho \mathbf{v}) = 0
$$

<Explain each term. Give typical values. Note regimes where simplifications hold.>

## 4. Implementation Sketch (Clojure Pseudocode)

```clojure
(defn governing-equation-step
  "One tick of the model. Returns delta-map of state changes."
  [state params]
  (let [rho   (:density state)
        v     (:velocity state)
        grad  (compute-gradient rho)]
    {:density-delta  (- (divergence (* rho v)))
     :velocity-delta (pressure-gradient-force state params)}))
```

<Explain design choices. Note where ECS components would attach.>

## 5. Toy Model / Numerical Experiment

### 5.1 Setup

<Define the test case: initial conditions, boundary conditions, parameters>

### 5.2 Results

<Tables of output values. Comparison to published benchmarks.>

| Parameter | Published value | Our model | Error |
|-----------|----------------|-----------|-------|
| ...       | ...            | ...       | ...   |

### 5.3 Charts

<Embedded chart images or Jupyter notebook references>

![Chart title](relative/path/to/chart.png)

## 6. Validation

- [ ] Matches published analytic solutions for limiting cases
- [ ] Reproduces benchmark values from literature (within X%)
- [ ] Behaves correctly at boundary regimes
- [ ] Conserves relevant quantities (energy, mass, momentum)
- [ ] Performance characteristics documented

## 7. Promotion Path to Domain

### 7.1 ECS Components

```clojure
(defrecord NewComponent [field1 field2])
```

### 7.2 Malli Schema (law/)

```clojure
(def new-schema
  {:field1 number?
   :field2 [:vector number?]})
```

### 7.3 System Function (domain/)

```clojure
(defn new-system
  "Docstring describing what this system does each tick."
  [world]
  ;; implementation
  )
```

### 7.4 Test (test/)

```clojure
(deftest new-system-validates-against-published-values
  (is (≈ expected actual 1e-6) "Matches published benchmark"))
```

## 8. Open Questions

- <What remains uncertain?>
- <What would change the model significantly?>
- <What experiments should run next?>

## 9. References

1. <Full citation with DOI/arXiv link>
2. ...
```

## Research Methodology

### Phase 1: Discovery

1. **Define the research question** from the simulation need. What system or
   phase needs grounding? What specific physics/biology/etc. is missing?
2. **Search arxiv** using targeted queries:
   - `<topic> review` for survey papers
   - `<topic> simulation method` for implementation details
   - `<topic> benchmark` for validation data
   - `<topic> Clojure` or `<topic> implementation` for code references
3. **Search Google Scholar** for highly-cited foundational papers.
4. **Search existing docs/research/** to avoid duplication and find cross-links.
5. **Record all candidates** before reading any. Build a bibliography first.

### Phase 2: Deep Reading

1. Read the abstract, introduction, and conclusion of each paper first.
2. For papers that matter: read methods section completely.
3. Extract governing equations into LaTeX.
4. Note parameter ranges, boundary conditions, and limiting cases.
5. Record numerical benchmark values for validation.
6. Cross-reference with other papers for consensus.

### Phase 3: Synthesis

1. Organize findings by subtopic, not by paper.
2. Identify the consensus model (what most papers agree on).
3. Identify open debates (where papers disagree).
4. Choose the model best suited to our resolution and ECS architecture.
5. Write the governing equations section with full LaTeX.

### Phase 4: Implementation Sketch

1. Write Clojure pseudocode that maps to our ECS patterns:
   - Pure functions in `domain/`
   - `defrecord` for components
   - System functions that take and return world
2. Note which existing systems this couples to.
3. Design the Malli schema.
4. Specify the test cases.

### Phase 5: Toy Model

1. Implement a minimal numerical experiment (Python or Clojure).
2. Run against published benchmark cases.
3. Generate charts comparing our results to literature.
4. Document error margins and convergence behavior.

### Phase 6: Documentation

1. Write the full notebook following the format above.
2. Include all charts as embedded images.
3. Ensure every claim has a citation.
4. Write the promotion path with concrete code stubs.
5. List open questions for future research.

## Tools and Commands

### arxiv Search

```bash
# Use web search with site:arxiv.org
# Example: "site:arxiv.org SPH viscosity switch Balsara"
```

### Chart Generation

Charts are generated as Python scripts or Jupyter notebooks:

```python
import numpy as np
import matplotlib.pyplot as plt

# Example: Blackbody SED comparison
def planck(wavelength_m, T_K):
    h = 6.626e-34  # Planck constant
    c = 3e8        # Speed of light
    k = 1.381e-23  # Boltzmann constant
    return (2*h*c**2 / wavelength_m**5) / (np.exp(h*c/(wavelength_m*k*T_K)) - 1)

wl = np.linspace(1e-9, 3e-6, 1000)
for T, label in [(40000, 'O5'), (5800, 'G2'), (3000, 'M5')]:
    plt.semilogy(wl*1e9, planck(wl, T), label=f'{label} ({T}K)')
plt.xlabel('Wavelength (nm)')
plt.ylabel('Spectral radiance')
plt.legend()
plt.savefig('docs/research/cosmology/sed_comparison.png', dpi=150)
```

### Clojure Validation

```bash
# Run against the live nREPL
clj -M:dev -e "(require '[nrepl.core :as nrepl]) ..."
```

## Long-Running Sessions

Deep research sessions may run for hours. The agent MUST:

1. **Commit intermediate work** every 30-60 minutes so nothing is lost.
2. **Log progress** to the actor's `outbox/` or `sessions/` directory.
3. **Take breaks between papers** — synthesize before moving on.
4. **Validate incrementally** — don't wait until the end to check numbers.
5. **Report status** if dispatched by another actor or the user.

## Non-Destructive Contract

Research actors and sessions MUST:

- **Only write to `docs/research/`** — never modify `src/`, `test/`, or `law/`.
- **Only create new files** — never overwrite existing research without explicit approval.
- **Append to indexes** — if maintaining a research index, append new entries.
- **Respect existing work** — cross-reference and build on, never replace.

The promotion from research to code is a **separate step** done by a human or
a code-focused actor. Research only produces the evidence and the path.

## Cross-Domain Linking

Many simulation systems span multiple domains. When a finding connects to
another domain, add a cross-reference:

```markdown
## Cross-references

- See `docs/research/atmosphere/hadley-cell-scaling-laws.md` for the
  atmospheric circulation model this couples to.
- See `docs/research/physics/sph-viscosity-artifacts-balsara-switch.md` for
  the viscosity treatment that affects shock heating here.
```

Maintain a `docs/research/INDEX.md` that catalogs all research notebooks
by domain, status, and cross-references.

## Integration with ημ Actors

This skill is the methodology behind the `truth-research-*` actor family.
Each domain actor uses this skill as its core protocol. The coordinator actor
(`truth-research-coordinator`) uses this skill to:

- Assign research topics to domain actors.
- Track progress across all domains.
- Identify cross-domain gaps.
- Maintain the master research index.

See `.eta-mu/actors/` for actor definitions and dispatch instructions.

## References

- arxiv.org — primary source for physics/astro/CS papers
- NASA ADS — astronomy paper search and citation tracking
- Google Scholar — cross-discipline citation graph
- Wolfram Alpha — physical constants and unit conversions
- NIST Reference Data — authoritative physical constants
