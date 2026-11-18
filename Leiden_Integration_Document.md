# Leiden Algorithm Integration in ServiceCutter

This document describes how to replace the temporary Leiden stub (which currently delegates to Louvain) with the actual Leiden implementation and how to finish the integration.

## Summary of changes made
- Added `LeidenAlgorithm` (stub) under `Engine/src/main/java/ch/hsr/servicecutter/solver/leiden`.
- Added `LeidenClusteringSolver` wrapper under `Engine/src/main/java/ch/hsr/servicecutter/solver`.
- Registered the algorithm in `SolverEndpoint` as `MODE_LEIDEN = "Leiden"` and added it to the UI dropdown.
- Added `LeidenSolverTest` to exercise the integration (temporary; uses stubbed Leiden).

## Steps to complete full, real Leiden integration
1. Clone the CWTSLeiden/networkanalysis repository locally:
   - `git clone https://github.com/CWTSLeiden/networkanalysis.git`
2. Identify the Java Leiden implementation classes and any helper classes they use (graph data structures, partitioning, quality functions).
3. Copy the required classes into package `ch.hsr.servicecutter.solver.leiden` (or add as a Maven module/dependency).
   - Keep package structure clean to avoid conflicts.
   - Prefer adding as a module/dependency if the copied codebase is large and has its own dependencies.
4. Add any required Maven dependencies to `Engine/pom.xml`.
   - Example: if the Leiden code uses `org.jgrapht` or other libs, add appropriate `<dependency>` entries.
5. Replace the body of `LeidenAlgorithm.run()` with a call into the real Leiden implementation and adapt the input mapping if necessary.
6. Ensure node ids map one-to-one between ServiceCutter nodes and Leiden graph nodes.
7. Run `mvn test` and verify `LeidenSolverTest` and other solver tests pass.
8. Compare results against Louvain, Girvan-Newman, and Chinese Whispers for validation (see Step 9 in To-Do list).

## Tips and notes
- The repository currently ships a temporary stub that delegates to Louvain to make CI and development seamless — replacing the stub with real Leiden code is straightforward once the classes and dependencies are available.
- If you prefer not to copy code, wrap the Leiden classes as a separate Maven module and add it as a dependency in `Engine/pom.xml`.

## Files to review
- `Engine/src/main/java/ch/hsr/servicecutter/solver/leiden/LeidenAlgorithm.java`
- `Engine/src/main/java/ch/hsr/servicecutter/solver/LeidenClusteringSolver.java`
- `Engine/src/main/java/ch/hsr/servicecutter/solver/SolverEndpoint.java` (registered `MODE_LEIDEN`)
- `Editor/src/main/webapp/scripts/app/solver/solver.controller.js` (dropdown updated)

---

If you'd like, I can:
- Help fetch or inspect the CWTSLeiden code and prepare a patch to add the required classes into this repository.
- Add more comprehensive tests comparing Leiden vs. Louvain outputs on the sample data.
