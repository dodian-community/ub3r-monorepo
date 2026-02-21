# Environment Variables
The game server is configured via environment variables. These values can either be set in the system environment variables, or by creating a `.env`-file at the root of the game server.

---

<details>
<summary>Navigation Menu</summary>

<ul>
    <li><a href="/docs/contribution">Guides</a>
        <ul>
            <li><a href="/docs/guides/getting_started.md">Getting Started</a></li>
            <li><a href="/docs/guides/installing_mysql.md">Installing MySQL Database</a></li>
            <li style="margin-top: 5px"><a href="/docs/guides/glossary.md">Glossary</a></li>
        </ul>
    </li>
    <li><a href="/docs/contribution">Contribution</a>
        <ul>
            <li><a href="/docs/contribution/guidelines.md">Contribution Guidelines</a></li>
            <li><a href="/docs/contribution/issue_definitions.md">Issue Definitions</a></li>
        </ul>
    </li>
    <li><a href="/docs/development">Development</a>
        <ul>
            <li><a href="/docs/development/database.md">Ub3r Database</a></li>
        </ul>
    </li>
    <li><a href="/docs/other">Other</a>
        <ul>
            <li><a href="/docs/other/environment_variables.md">Environment Variables</a></li>
        </ul>
    </li>
</ul>

</details>

---

Use your existing `game-server/.env` file and add or adjust only the values you need.

**Example entries for your existing `.env` file:**
```dotenv
SERVER_NAME=Dodian
SERVER_PORT=43594
SERVER_DEBUG_MODE=false
SERVER_ENVIRONMENT=prod

DATABASE_HOST=localhost
DATABASE_PORT=3306
DATABASE_NAME=dodiannet
DATABASE_TABLE_PREFIX=
DATABASE_USERNAME=dodian_game
DATABASE_PASSWORD=abcd1234
DATABASE_INITIALIZE=false

CLIENT_CUSTOM_VERSION=dodian_client

GAME_WORLD_ID=1
GAME_MULTIPLIER_GLOBAL_XP=1
GAME_CONNECTIONS_PER_IP=2
```
The above values can be added to your existing `.env`. Core database credentials (`DATABASE_HOST`, `DATABASE_NAME`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`) are required. The new async/pool settings are optional because `DotEnv.kt` provides defaults.

- `SERVER_NAME` = The name of the server, used in the game chat etc
- `SERVER_PORT` = The port the game client will need to connect to _(server's listening port)_
- `SERVER_DEBUG_MODE` = `true`/`false` - enables some debug logging
  - Also, if server's `SERVER_ENVIRONMENT` is set to `dev`, you can log in on any account with any password _(useful for local development environment)_
- `SERVER_ENVIRONMENT` = `dev`/`stage`/`prod` - is used to define where the server is hosted, additionally it will enable/disable some features, depending on its environment
  - It's not ideal to have development environment functionality enabled in staging or production at any time, for any reason, for example
- `DATABASE_*` = The details used to connect to the database
- `DATABASE_TABLE_PREFIX` = The prefix for table names, blank if there is no prefix
  - Nothing in this project requires you to specify a prefix, you may however do it if you see fit to, and know what you're doing
- `DATABASE_INITIALIZE` = Provided you have valid database details for connecting to the database, it will try to execute all the files inside [database](/database)-folder, then it will create a `.initialized_database`-file to avoid doing this job again
  - All the default scripts we have included in this repository should be enough to run the game server, or even launch your own Dodian remake
