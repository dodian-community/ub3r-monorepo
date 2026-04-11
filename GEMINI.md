# GEMINI.md

## Mission
Reverse-engineer this legacy RSPS and produce evidence-backed documentation.
Do not summarize by naming convention or common RSPS assumptions.
Derive behavior from code paths, references, registration, and runtime evidence.

## Non-negotiable rules
1. No assumptions from class names, package names, or old RSPS conventions.
2. No claim without evidence.
3. Do not describe code as active unless it is reachable by references, registration, call path, or runtime evidence.
4. Trace before documenting:
   - entrypoint
   - dispatch chain
   - state mutations
   - dependencies
   - persistence/network side effects
5. Use only these statuses:
   - VERIFIED
   - PARTIAL
   - UNKNOWN
6. If uncertain, mark UNKNOWN, record attempted traces, and continue.

## Required outputs
- docs/MASTER_INDEX.md
- docs/ARCHITECTURE.md
- docs/RUNTIME_MODEL.md
- docs/domains/*.md
- docs/flows/*.md
- docs/evidence/*.json
- docs/CHECKPOINT_STATUS.md
- docs/TODO_REMAINING.md

## Required sections for every domain doc
- Purpose
- Entry Points
- Dispatch Chain
- State Mutations
- Dependencies
- Evidence
- Confidence

## Required sections for every flow doc
- Trigger
- Step-by-step path
- Side effects
- Evidence
- Confidence

## Working loop
1. Read docs/TODO_REMAINING.md and docs/CHECKPOINT_STATUS.md
2. Select exactly one unfinished target
3. Use MCP tools first
4. Verify the path with evidence
5. Write or update the artifact
6. Update checkpoint files
7. Continue until the requested checkpoint is complete

## Stop policy
Do not stop because a task is large.
Stop only when the requested checkpoint is complete.
Before stopping, update:
- docs/CHECKPOINT_STATUS.md
- docs/TODO_REMAINING.md
