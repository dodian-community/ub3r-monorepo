# Simple PHP Account Website

A minimal PHP + HTML account portal with registration, login, downloads, activation, forgot-password, and in-session change-password flows compatible with the current Ub3r password-hash flow.

## Getting started

1. Copy `config.example.php` to `config.php`.
2. Fill in your database credentials.
3. Configure these required activation settings in `config.php`:
   - `app.base_url` (public URL of the registration page)
   - `app.client_jar_url` (direct link to your single game client `.jar`)
   - `app.java_download_url` (Java download page, defaults to java.com)
   - `app.discord_url` (invite link for your Discord community)
   - `brevo.api_key`
   - `brevo.sender_email`
   - `turnstile.site_key`
   - `turnstile.secret_key`
   - `discord.client_id`
   - `discord.client_secret`
   - `discord.redirect_uri` (set this to `.../?page=discord-link`)
   - `discord.guild_id`
   - `discord.bot_token`
   - `discord.verified_role_id` (role ID for the Verified role)
4. Start locally:

```bash
php -S 0.0.0.0:8080 -t web-registration-simple
```

5. Open `http://localhost:8080`.

## Compatible password hash

This demo uses the same flow as the game server:

- `passM = md5(password)`
- `stored = md5(passM + salt)`

## Account activation, login, and password recovery

- Registrations start as `usergroupid = 3` (inactive).
- An activation token is stored in `user_activation_tokens`.
- Brevo sends an activation email with a link (`/?page=activate&token=...`) that is valid for 2 hours.
- Clicking the link changes the account to `usergroupid = 40` (active), which is required for login.
- Expired activation links auto-ban the account (`usergroupid = 8`).
- Registration blocks duplicate usernames and duplicate email addresses.
- Successful login redirects users to `?page=download` with one game client `.jar` download button, one Java download button, and Discord actions (join + link).
- Discord linking starts from the signed-in website only (`?page=discord-link`) and uses OAuth `identify` scope to connect a Discord account.
- After linking, the configured bot updates the member nickname in the configured guild to the website/game username and assigns the configured Verified role during the linking flow.
- The Discord bot must have `Manage Nicknames` and `Manage Roles`. Its highest role position must be strictly above the member's highest role and above the Verified role (same permissions are not enough; hierarchy wins, and guild owners still cannot be renamed). If this is wrong, linking still succeeds for the current session but sync warnings will explain what failed.
- If nickname sync returns Discord 404, verify `discord.guild_id` first (wrong guild or bot not in that guild can also cause 404), then verify the linked Discord account is a member of that guild.
- Forgot password stores reset tokens in `user_password_reset_tokens` and emails `?page=reset-password&token=...` links for active accounts.
- Signed-in users can change their password from `?page=change-password` by confirming their current password.

## Security notes

- Use HTTPS in production.
- Cloudflare Turnstile is integrated and validated server-side for each registration.
- Add rate limiting as a second layer.
- Never commit `config.php` to git.
