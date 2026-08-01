# Installing Database (MariaDB / MySQL)

This document explains how to set up a database instance for local development or production hosting.

---

## Recommended Setup: Docker Compose (MariaDB 10.11)

Using Docker Compose is the fastest way to run a local database instance.

### `docker-compose.yml` Example
Save the following as `docker-compose.yml` in your project root:

```yaml
version: '3.8'

volumes:
  mariadb-data:

services:
  mariadb:
    image: mariadb:10.11
    container_name: dodian-mariadb
    restart: always
    environment:
      MYSQL_DATABASE: dodiannet
      MYSQL_USER: dodian
      MYSQL_PASSWORD: dodian_local_development_123
      MYSQL_ALLOW_EMPTY_PASSWORD: 'true'
    ports:
      - '3306:3306'
    volumes:
      - mariadb-data:/var/lib/mysql
```

### Starting the Container
Run the following command in your terminal:
```bash
docker-compose up -d
```

When running, configure your `game-server/.env` file with:
```env
SERVER_DATABASE_HOST=localhost
SERVER_DATABASE_PORT=3306
SERVER_DATABASE_NAME=dodiannet
SERVER_DATABASE_USERNAME=dodian
SERVER_DATABASE_PASSWORD=dodian_local_development_123
SERVER_DATABASE_INITIALIZE=true
```

---

## Database Initialization Scripts (`game-server/database/`)

If you want to manually import database tables using **MySQL Workbench**, **DataGrip**, or command line, run the SQL files from `game-server/database/` in sequential order:

1. `1_mandatory_tables.sql` — Mandatory table schema
2. `2_dodian_default_data.sql` / `2.1_dodian_default_data.sql` — Default spawns and game data
3. `3_convenient_data.sql` — Local development flags
4. `4_dummy_development_data.sql` — Creates `Admin` / `abc123` account
5. `5_quest_data.sql` — Quest progress tables
6. `6_discord_identity.sql` — Discord OAuth2 account integration
7. `7_shop_transaction_logs.sql` — Shop audit logs

---

## Standalone Installation
Alternatively, install MariaDB or MySQL natively on your OS:
- **Mac**: `brew install mariadb` -> `brew services start mariadb`
- **Linux (Ubuntu/Debian)**: `sudo apt install mariadb-server` -> `sudo systemctl start mariadb`
- **Windows**: Download MariaDB Community Server installer from [mariadb.org](https://mariadb.org/download/).