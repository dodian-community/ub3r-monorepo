# Ub3r v3: Production-Ready Restructure Plan

## Mission Statement

Make the `game-server` Kotlin directory **production-ready for live players** by establishing:

1. A **uniform plugin system** for skills, NPCs, objects, items, and all content
2. **Developer-friendly packaging** with RSPS-familiar naming conventions
3. **Content-facing DDD architecture** that is intuitive without being over-engineered
4. **Minimal boilerplate** for adding new content
5. **Robust debugging** and traceability
6. **Stable, performant runtime** ready for hundreds of concurrent players
7. Comprehensive `package-info.java` documentation at every package boundary

---

## Plan Overview

This plan is broken into **8 phases**, each with its own detailed document. Phases are ordered by dependency — later phases build on earlier ones. Within each phase, steps are atomic and can be committed independently.

### Phase Documents

| # | Document | Summary |
|---|----------|---------|
| 0 | [00_audit_and_inventory.md](00_audit_and_inventory.md) | Full inventory of current packages, interfaces, dead code, and pain points |
| 1 | [01_package_restructure.md](01_package_restructure.md) | DDD package rename: `content/` → domain packages, `engine/` → `runtime/`, naming conventions |
| 2 | [02_unified_plugin_system.md](02_unified_plugin_system.md) | Single `ContentPlugin` contract replacing `ObjectContent`, `NpcModule`, `SkillPlugin`, `ItemContent`, etc. |
| 3 | [03_skill_system_overhaul.md](03_skill_system_overhaul.md) | Uniform skill plugin architecture with zero-boilerplate gathering/production/combat skill templates |
| 4 | [04_ksp_processor_v2.md](04_ksp_processor_v2.md) | KSP processor rewrite to support the unified plugin system and annotation-driven discovery |
| 5 | [05_content_api_surface.md](05_content_api_surface.md) | The stable content-developer API: scheduling, timing, events, inventory, dialogue, movement |
| 6 | [06_debugging_observability.md](06_debugging_observability.md) | Structured logging, tick profiling, content tracing, admin tooling |
| 7 | [07_production_hardening.md](07_production_hardening.md) | Security, performance, graceful shutdown, save integrity, load testing |
| 8 | [08_package_info_javadoc.md](08_package_info_javadoc.md) | Every `package-info.java` file with purpose, boundaries, and examples |

### Supporting Documents

| Document | Summary |
|----------|---------|
| [09_migration_guide.md](09_migration_guide.md) | Step-by-step migration instructions for converting existing content to v3 |
| [10_naming_conventions.md](10_naming_conventions.md) | Comprehensive naming rules for classes, packages, files, and RSPS-specific terms |
| [11_architecture_tests.md](11_architecture_tests.md) | New and updated architecture boundary tests for v3 contracts |
| [12_content_developer_cookbook.md](12_content_developer_cookbook.md) | Recipes and examples for common content tasks |
| [13_risk_register.md](13_risk_register.md) | Known risks, mitigations, and rollback strategies |

---

## Guiding Principles

### 1. Content Developers Shouldn't Need to Understand the Engine
A content developer adding a new tree to Woodcutting should never touch `engine/`, `persistence/`, or `systems/`. They write a data definition and a handler. Done.

### 2. RSPS-Familiar Naming
Classes and packages use names that any RSPS developer would recognize:
- `Player` not `Client` (eventually)
- `skill.woodcutting` not `systems.skills.plugin`
- `npc.banker` not `content.npcs.Banker`
- `combat.melee` not `content.combat.MeleeCombat`

### 3. Discover by Convention, Override by Configuration
KSP discovers plugins by marker interface + `object` declaration. No manual registration. No reflection. No XML.

### 4. One Way to Do Things
There should be exactly one canonical way to:
- Start a skilling action
- Register an NPC interaction
- Award experience
- Open a dialogue
- Schedule a delayed task

### 5. Fail Fast, Fail Loud
Compile-time errors over runtime errors. Architecture tests over code reviews. Structured logs over `println`.

### 6. Don't Over-Abstract
No `AbstractBaseFactoryProvider<T>`. Keep the inheritance tree shallow. Prefer composition and DSL builders over deep class hierarchies.

---

## Current State vs Target State (High-Level)

```
CURRENT (v2)                              TARGET (v3)
─────────────────────────                 ─────────────────────────
net.dodian.uber.game                      net.dodian.uber.game
├── content/                              ├── skill/
│   ├── combat/                           │   ├── woodcutting/
│   ├── commands/                         │   ├── mining/
│   ├── skills/                           │   ├── fishing/
│   │   ├── woodcutting/                  │   └── ... (one per skill)
│   │   ├── mining/                       ├── npc/
│   │   └── ...                           │   ├── banker/
│   ├── npcs/                             │   ├── shopkeeper/
│   │   ├── Banker.kt                     │   └── ... (grouped by role)
│   │   ├── Man.kt                        ├── object/
│   │   └── (200+ files flat)             │   ├── bank/
│   ├── objects/                          │   ├── door/
│   ├── items/                            │   └── ladder/
│   ├── shop/                             ├── item/
│   └── dialogue/                         │   ├── food/
├── engine/                               │   ├── potion/
│   ├── loop/                             │   └── equipment/
│   ├── tasking/                          ├── combat/
│   ├── phases/                           │   ├── melee/
│   └── ...                               │   ├── ranged/
├── systems/                              │   ├── magic/
│   ├── api/content/                      │   └── special/
│   ├── skills/plugin/                    ├── social/
│   ├── interaction/                      │   ├── trade/
│   ├── dispatch/                         │   ├── duel/
│   └── ...                               │   └── chat/
├── model/                                ├── activity/
│   ├── entity/                           │   ├── quest/
│   ├── item/                             │   └── minigame/
│   └── ...                               ├── world/
├── persistence/                          │   ├── region/
│   ├── db/                               │   ├── grounditem/
│   └── ...                               │   └── spawn/
└── events/                               ├── model/           (pure domain)
                                          │   ├── entity/
                                          │   ├── item/
                                          │   └── position/
                                          ├── runtime/         (engine guts)
                                          │   ├── loop/
                                          │   ├── task/
                                          │   ├── sync/
                                          │   ├── net/
                                          │   └── phase/
                                          ├── persistence/     (DB adapters)
                                          │   ├── account/
                                          │   ├── world/
                                          │   └── audit/
                                          ├── event/           (event bus)
                                          │   ├── combat/
                                          │   ├── skill/
                                          │   └── player/
                                          └── api/             (content surface)
                                              ├── plugin/
                                              ├── schedule/
                                              ├── dialogue/
                                              └── action/
```

---

## How to Read This Plan

1. **Start with Phase 0** to understand the current inventory
2. **Read Phases 1–4** for the structural changes (packages, plugins, KSP)
3. **Read Phases 5–7** for the developer experience and production concerns
4. **Phase 8** is the documentation layer (package-info.java files)
5. **Supporting docs 9–13** are reference material for during and after migration

Each phase document includes:
- **Goal**: What this phase achieves
- **Prerequisites**: What must be done first
- **Detailed Steps**: Numbered, atomic steps
- **File-by-File Changes**: Exact files to create/move/modify
- **Verification**: How to confirm the phase is complete
- **Rollback**: How to undo if something goes wrong

---

*This plan targets the Kotlin source tree only. Java legacy code in `game-server/src/main/java` is out of scope except where it must be touched for interop (e.g., `Server.java` startup wiring).*

