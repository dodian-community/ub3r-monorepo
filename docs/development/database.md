# Ub3r Database Documentation

This document details the database architecture, schema setup scripts, and driver configuration for Dodian Ub3r.

---

## Database Connection & Driver

- **Database Engine**: **MariaDB 10.11+** or **MySQL 8.0+**
- **JDBC Driver**: `org.mariadb.jdbc:mariadb-java-client`
- **Connection URL**: `jdbc:mariadb://<host>:<port>/<database>`
- **Connection Pool**: **HikariCP** managing connections with Virtual Thread async dispatchers (`DbDispatchers.kt`).

---

## Database Scripts (`game-server/database/`)

The server includes 7 structured SQL migration and initialization scripts located in `game-server/database/`:

| Script Name | Purpose & Contents |
| :--- | :--- |
| `1_mandatory_tables.sql` | **Core Schema**: Creates mandatory engine tables (`uber3_players`, `uber3_inventory`, `uber3_bank`, `uber3_staff_logs`, `uber3_highscores`, `uber3_settings`). |
| `2_dodian_default_data.sql` / `2.1_dodian_default_data.sql` | **World & Game Data**: Imports default NPC spawns, drop tables, teleports, and object definitions. |
| `3_convenient_data.sql` | **Dev Utilities**: Sets convenient server configuration flags for local development. |
| `4_dummy_development_data.sql` | **Development Admin Account**: Creates default administrator account (`Admin` / `abc123`). |
| `5_quest_data.sql` | **Quest Engine**: Creates quest progress tracking tables (`uber3_quests`). |
| `6_discord_identity.sql` | **Discord Integration**: Schema for Discord OAuth2 account authentication, identity mappings, and token links. |
| `7_shop_transaction_logs.sql` | **Shop Audit Logs**: Logs all shop purchases, sell transactions, and gold flow for economy monitoring. |

---

## Auto-Import & Setup

When `game-server` boots with `.env` setting `SERVER_DATABASE_INITIALIZE=true`:
1. The engine connects via HikariCP.
2. Checks if `1_mandatory_tables.sql` has been imported.
3. Automatically executes missing `.sql` scripts from `game-server/database/` in sequential order (`1` through `7`).