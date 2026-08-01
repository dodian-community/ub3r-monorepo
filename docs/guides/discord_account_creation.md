# Discord account creation

New accounts are gated behind Discord OAuth: a player must authorize with Discord
before the server will create a character for them. This doc covers what to set
up to make that work, and how the pieces fit together.

## Where each piece lives

| Piece | File | Notes |
|---|---|---|
| Client-side OAuth flow | `game-client/src/main/java/com/osroyale/DiscordOAuth.java` | Opens the browser, runs a tiny local HTTP server to catch the redirect. |
| Client-side config | `game-client/src/main/java/com/osroyale/Configuration.java` (`DiscordConfiguration`) | Public client id + redirect URL only. **No secret here.** |
| Login screen wiring | `game-client/.../login/impl/MainScreen.java`, `game-client/.../login/LoginModeState.java` | Captures the auth code, flags the login packet as account creation. |
| Server config | `game-server/.env` | Client id, client secret, redirect URL, enabled flag, per-Discord account cap. |
| Server-side token exchange | `game-server/src/main/kotlin/net/dodian/uber/game/persistence/account/discord/DiscordApiService.kt` | Trades the one-time code for the Discord user's id/username/email. |
| Account creation rules | `game-server/src/main/kotlin/net/dodian/uber/game/persistence/account/login/AccountLoginService.kt` | Verified-email requirement, per-Discord account limit, username rules. |
| Verified-email toggle | `game-server/Settings.toml` (`[discord] require_verified_email`) | |

## Why the client has no secret

OAuth "confidential clients" (anything that can keep a secret server-side) are
supposed to do the code→token exchange on the server, never in the app itself.
Until recently this client shipped `DISCORD_CLIENT_SECRET` hardcoded in
`Configuration.java` — readable by anyone who decompiled the jar, which
defeats the point of having a secret at all. It's been removed. The client
only ever sends the public `client_id` and `redirect_uri` to Discord's
`/oauth2/authorize` endpoint (this step never needs a secret); the server is
the only thing that ever presents the client secret, in the token-exchange
POST inside `DiscordApiService.kt`.

**If your `.env`'s `DISCORD_CLIENT_SECRET` was ever the same value that used
to be in `Configuration.java`, treat it as compromised** (it was committed to
git history) — rotate it in the Discord Developer Portal and update `.env`.

## Setting it up

### 1. Create/find the Discord application

Go to <https://discord.com/developers/applications>, create an application
(or open the existing one), and under **OAuth2 → General** note:

- **Client ID** — public, goes in both places below.
- **Client Secret** — private, goes in `game-server/.env` only.

Under **OAuth2 → Redirects**, add a redirect URL. It must **exactly** match
what both the client and server send (scheme, host, port, path, no trailing
slash differences) — see the callback-port section below for what that value
actually is.

### 2. Server config (`game-server/.env`)

```
DISCORD_ENABLED=true
DISCORD_CLIENT_ID=<the client id from the Developer Portal>
DISCORD_CLIENT_SECRET=<the client secret from the Developer Portal>
DISCORD_REDIRECT_URL=http://localhost:8080
DISCORD_MAX_ACCOUNTS_PER_DISCORD=3
```

Read by `DotEnv.kt`; `DISCORD_ENABLED=false` or a blank `DISCORD_CLIENT_ID`
disables Discord-gated signup entirely (`DiscordApiService.fetchDiscordUserResult`
short-circuits to `Unavailable`).

`.env` is gitignored — copy `game-server/.env.example` to `game-server/.env`
and fill in real values. Never commit the real secret.

### 3. Client config (`game-client/.../Configuration.java`)

```java
public static class DiscordConfiguration {
    public static boolean ENABLE_DISCORD_OAUTH_LOGIN = true;
    public static String CLIENT_ID = "<the same client id as above>";
    public static int CALLBACK_PORT = 8080;
    public static String REDIRECT_URL = "http://localhost:8080";
}
```

### 4. The redirect URL must match in three places

This is the single most common way this breaks, and the thing that broke it
for this repo's own dev instance while writing this doc:

1. `DiscordConfiguration.REDIRECT_URL` (client, used to build the Discord
   authorize URL)
2. `DISCORD_REDIRECT_URL` in `game-server/.env` (server, used in the
   token-exchange POST)
3. A registered redirect in the Discord Developer Portal (**OAuth2 → Redirects**)

All three must be byte-for-byte identical. Discord's OAuth2 implementation
requires the `redirect_uri` sent at token-exchange time to match the one sent
at authorize time, and it also has to be one of the app's registered
redirects — if any of the three differs (even a trailing slash or an extra
`/discord/callback` path), the token exchange fails with `invalid_grant`.

`DiscordOAuth.java`'s local callback server listens on `CALLBACK_PORT` (8080
by default, falling back to up to 8089 if 8080 is busy) and answers on **both**
`/` and `/discord/callback`, so either a bare-origin or a `/discord/callback`
redirect URL works client-side — but whichever one you pick has to be the same
value in the server's `.env` and in the Developer Portal too.

### 5. Optional: require a verified Discord email

`game-server/Settings.toml`:

```toml
[discord]
require_verified_email = false
```

When `true`, `AccountLoginService` rejects account creation for Discord
identities whose email Discord reports as unverified.

## How the flow works end to end

1. Player clicks "Create Account". The client waits 5 seconds
   (`DiscordOAuth.BROWSER_OPEN_DELAY_MS`), then opens the system browser to
   `https://discord.com/api/oauth2/authorize?client_id=...&redirect_uri=...&response_type=code&scope=identify%20email`.
2. Player approves in the browser. Discord redirects to the local callback
   server with `?code=...`.
3. `DiscordOAuth.CallbackHandler` extracts `code`, hands it to the login
   screen, and shows a static "you can return to the client" page.
4. Player enters a username/password and submits. The client's login packet
   includes the auth code and a `CREATE_ACCOUNT` mode flag
   (`LoginModeState`, protocol byte `1`). The code has a 15-minute
   client-side TTL (`Client.DISCORD_AUTH_CODE_TTL_MILLIS`) after which the
   client discards it and asks the player to re-authorize.
5. Server-side, `LoginPreparationService` parses the login block and hands
   the code to `AccountPersistenceService.submitLoginLoad`, which calls
   `DiscordApiService.fetchDiscordUserResult(code)`.
6. `DiscordApiService` POSTs the code + client id/secret + redirect URI to
   Discord's token endpoint, then fetches `/users/@me` with the resulting
   access token to get the Discord user's id/username/email/verified flag.
7. `AccountLoginService.prepareGame` enforces the rules (username not taken,
   Discord user id present, verified-email requirement if enabled, per-Discord
   account cap) and creates the account.

## Troubleshooting

Every rejection point in this flow now logs a distinct line — check
`logs/console-audit.log` or the server console right after the failure:

| Log line | Where | Meaning |
|---|---|---|
| `Account creation rejected for X: missing Discord authorization` | `AccountPersistenceService.kt` | The server never received a code at all. Client-side issue — check the client's `[DiscordOAuth]` console output for whether the callback actually fired. |
| `Account creation rejected for X: invalid or expired Discord authorization` | `AccountPersistenceService.kt` | Discord's token exchange rejected the code — almost always a redirect-URL mismatch (see above) or a stale/reused code. |
| `Account creation unavailable for X: Discord service/configuration failure` | `AccountPersistenceService.kt` | `DISCORD_ENABLED=false`, blank client id/secret, or a non-4xx failure talking to Discord (network, 5xx, etc). |
| `Account creation rejected for X: Discord identity missing user id` | `AccountLoginService.kt` | Exchange succeeded but Discord didn't return a usable id — shouldn't normally happen. |
| `Account creation rejected for discord_id X: Discord email is not verified` | `AccountLoginService.kt` | Only fires if `require_verified_email = true`. |
| `Account creation rejected for discord_id X: limit of N accounts reached` | `AccountLoginService.kt` | `DISCORD_MAX_ACCOUNTS_PER_DISCORD` hit. |
| `Login rejected for X: account does not exist and creation was not requested` | `AccountLoginService.kt` | Player tried a normal login (not "Create Account") for a username that was never created. |

If you see nothing at all in the log when creation fails, you're on an older
build — these lines used to be `logger.debug` (invisible at the server's
default `INFO` level) instead of `warn`/`info`.

## Security notes

- Dev-mode bypasses (`SERVER_ENV=dev` account auto-create without Discord,
  and the debug password bypass) both require `debug=true` **and** a loopback
  connection (`127.0.0.1`/`::1`) — they do not work over the network, even in
  dev, even with `debug=true`.
- Account creation requires a real Discord user id from a successful token
  exchange; a response missing the id is rejected rather than silently
  falling through to unlimited account creation.
- The OAuth client secret must only ever exist in `game-server/.env` — never
  in client source, never committed.
