# Getting Started Guide

Welcome! This guide will walk you through setting up and running Dodian Ub3r on **Windows, Mac, or Linux** in under 5 minutes.

---

## Prerequisites

1. **JDK 21 or greater**: Gradle auto-provisions JDK 21 per machine, but having Java 21 installed locally is recommended.
2. **Git & IDE**: [IntelliJ IDEA](https://www.jetbrains.com/idea/) (Recommended).
3. **Database**: MariaDB 10.11+ or MySQL 8.0+ instance.

---

## Step-by-Step Setup

### Step 1: Clone the Repository
```bash
git clone https://github.com/dodian-community/ub3r-monorepo.git
cd ub3r-monorepo
```

### Step 2: Configure Environment Variables
Copy `game-server/example.env` to `game-server/.env`:
```bash
cp game-server/example.env game-server/.env
```
Ensure your `.env` contains valid database credentials:
```env
SERVER_DATABASE_HOST=localhost
SERVER_DATABASE_PORT=3306
SERVER_DATABASE_NAME=dodiannet
SERVER_DATABASE_USERNAME=dodian
SERVER_DATABASE_PASSWORD=dodian_local_development_123
SERVER_DATABASE_INITIALIZE=true
```

### Step 3: Set Up Database
Start your local MariaDB/MySQL instance (or run `docker-compose up -d` in project root).  
With `SERVER_DATABASE_INITIALIZE=true`, the server will automatically import all SQL scripts from `game-server/database/` (`1` through `7`) on first startup.

### Step 4: Download & Extract Revision 218 Cache (317 Format)
1. Download the **Revision 218 Cache (OSRS Data packed into 317 Format)**: [cache-tarnish-218.zip](https://files.jire.org/cache-tarnish-218.zip)
2. Extract the ZIP contents into:
   ```
   game-server/data/cache
   ```
3. **Important — Rename Folder**:
   If the extracted folder is named `cache-tarnish-218`, rename it to `cache` so that the cache index files reside directly inside `game-server/data/cache/`.

#### Required Server Cache Files List
Ensure `game-server/data/cache` contains the following files:
* `main_file_cache.dat`
* `main_file_cache.idx0`
* `main_file_cache.idx1`
* `main_file_cache.idx2`
* `main_file_cache.idx3`
* `main_file_cache.idx4`
* `main_file_cache.idx5`

### Step 5: Launch Game Server
Run the Gradle application task from terminal:
```bash
./gradlew :game-server:run
```
*(Or in IntelliJ: Gradle panel -> `ub3r-monorepo` -> `game-server` -> `Tasks` -> `application` -> `run`)*

When boot completes, you will see the structured Service Table in your terminal:
```
┌─────────────────────────────────────────────────────────────┐
│                  DODIAN 3.0 ENGINE ONLINE                   │
├──────────────────────────────┬──────────────────────────────┤
│ Game Engine Port             │ 43594                        │
│ Ktor Web API Port            │ 8080                         │
│ Generational ZGC             │ Enabled (-XX:+UseZGC)        │
│ Item Definitions             │ 27,513                       │
│ NPC Spawns / Definitions     │ 1,842 / 4,120                │
└──────────────────────────────┴──────────────────────────────┘
```

---

## Default Development Login Accounts

If `4_dummy_development_data.sql` was imported, use:
- **Username**: `Admin`
- **Password**: `abc123`
