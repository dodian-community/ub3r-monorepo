# Internal Tools & Validation (Skill Doctor)

## Overview
As the server transitions from legacy Java to modern Kotlin, it's easy for data to fall out of sync. The **Skill Doctor** is a system designed to maintain "Data Parity."

## 1. `SkillDoctorBootstrap`
This is a `ContentBootstrap` object discovered by the KSP processor.
- **Purpose**: At server startup, it performs a "health check" on the skill system.
- **Function**: It verifies that every entry in the `Skill` enum has a corresponding progression handler and that the experience table loaded from the database is mathematically consistent (no gaps or overlaps).

## 2. Collision Build Service
Located in `net.dodian.uber.game.systems.cache`, this service is responsible for building the initial 3D collision map.
- It parses the client's `loc.dat` and `map` files.
- It calculates "clipping" for 20,000+ objects simultaneously.
- If the cache is missing or corrupted, the server logs a critical error here, as players would be able to walk through all walls.

## 3. Python Automation Scripts
Located in `game-server/scripts/`, these are used by developers to maintain the database:
- `generate_npc_modules.py`: Scans the NPC database and generates boilerplate Kotlin `NpcModule` files.
- `scan_loc_opcodes.py`: Scans the client cache to find which objects have "Open", "Close", or "Pick-up" options, helping to identify missing interaction logic.
- `find_violations.py`: A linter that checks the Kotlin source code for "Forbidden Patterns" (like calling `Thread.sleep()` inside a coroutine).
