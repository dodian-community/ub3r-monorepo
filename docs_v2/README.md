# Ub3r RSPS: Comprehensive Documentation

Welcome to the comprehensive documentation for the Ub3r RSPS game server. This documentation has been meticulously verified against the source code and covers the architecture, engine, content systems, networking, persistence, and social interactions in deep technical detail.

## Table of Contents

### 1. Architecture
*   [System Overview](1_architecture/overview.md)
*   [KSP Plugin Processor](1_architecture/ksp_processor.md)
*   [Threading Model & Tick Execution](1_architecture/threading_model.md)
*   [Client Protocol & Compatibility](1_architecture/client_protocol.md)
*   [Internal Tools & Validation](1_architecture/internal_tools.md)
*   [Technical Debt & Modernization](1_architecture/technical_debt.md)
*   [Production Readiness Checklist](1_architecture/production_checklist.md)

### 2. Core Engine
*   [Game Loop & Scheduler](2_engine/game_loop.md)
*   [Coroutines & Game Tasks](2_engine/task_system.md)
*   [Game Event Bus](2_engine/event_bus.md)
*   [Entity Updating & Masks](2_engine/entity_updating.md)
*   [Pathfinding & Collision](2_engine/pathfinding.md)
*   [Spatial Partitioning (Chunks)](2_engine/chunk_system.md)
*   [Performance "Easy Wins"](2_engine/performance_wins.md)

### 3. Content Systems
*   [Content Plugins & Dispatch](3_content/plugins.md)
*   [Skill Architecture](3_content/skills.md)
*   [Combat System](3_content/combat.md)
*   [Special Attacks & Weapons](3_content/special_attacks.md)
*   [Dialogue System (DSL)](3_content/dialogues.md)
*   [Objects, NPCs, and Items](3_content/objects_npcs_items.md)
*   [Quests, Minigames, and Boss Logs](3_content/quests_minigames.md)

### 4. Networking & Persistence
*   [Login & Handshake Flow](4_networking_persistence/login_flow.md)
*   [Packet Processing Pipeline](4_networking_persistence/packet_processing.md)
*   [Database & Save/Load Flows](4_networking_persistence/database_saving.md)
*   [Web API](4_networking_persistence/web_api.md)
*   [Auditing & Monitoring](4_networking_persistence/auditing.md)

### 5. Social Interactions
*   [Trading & Dueling State Machines](5_social_interactions/trading_dueling.md)
*   [Chat & Private Messaging](5_social_interactions/chat_system.md)

### 6. Confirmed Bugs & Known Issues
*   [Known Issues](6_confirmed_bugs/known_issues.md)

---
**Note**: All claims in this documentation have been verified strictly against the source code of the `game-server` and `mystic-updatedclient`.
