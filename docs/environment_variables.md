# Environment Variables
The game server is configured via environment variables. These values can either be set in the system environment variables, or by creating a `.env`-file at the root of the game server.

---

We're going to refer to the [example.env](/game-server/example.env)-file, so you can see how it could look after it's configured properly.

**Example .env file:**
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
The above example contains all the possible environment variables that can configure the server. If no values are provided, it will use the values you see above.

- `SERVER_NAME` = The name of the server, used in the game chat etc
- `SERVER_PORT` = The port the game client will need to connect to _(server's listening port)_
- `SERVER_DEBUG_MODE` = `true`/`false` - enables some debug logging
  - Also, if server's `SERVER_ENVIRONMENT` is set to `dev`, you can log in on any account with any password _(useful for local development environment)_
- `SERVER_ENVIRONMENT` = `dev`/`stage`/`prod` - is used to define where the server is hosted, additionally it will enable/disable some features, depending on its environment
  - It's not ideal to have development environment functionality enabled in staging or production at any time, for any reason, for example
- `DATABASE_*` = The details used to connect to the database
- `DATABASE_TABLE_PREFIX` = The prefix for table names, blank if there is no prefix
  - Nothing in this project requires you to specify a prefix, you may however do it if you see fit to, and know what you're doing
- `DATABASE_INITIALIZE` = Provided you have valid database details for connecting to the database, it will try to execute all the files inside [database](/game-server/database)-folder, then it will create a `.initialized_database`-file to avoid doing this job again
  - All the default scripts we have included in this repository should be enough to run the game server, or even launch your own Dodian remake
- `GAME_WORLD_ID` = The ID of the world to use, depending on which ID you use, some stuff on the server will behave a little differently
  - World ID of 2 or above will among other things disable player saving
  - Traditionally in the past we've developed the server using a connection to the live database, and as such we've used another world ID than 1, to avoid conflicts with the live database
- `GAME_MULTIPLIER_GLOBAL_XP` = The global xp rate multiplier to apply
- `GAME_CONNECTIONS_PER_IP` = The maximum amounts of clients to be connected per IP address
  - As of right now, this is not in use in the server