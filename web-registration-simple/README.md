# Simple PHP Registration Website

A minimal PHP + HTML registration page compatible with the current Ub3r password-hash flow.

## Getting started

1. Copy `config.example.php` to `config.php`.
2. Fill in your database credentials.
3. Configure these required activation settings in `config.php`:
   - `app.base_url` (public URL of the registration page)
   - `brevo.api_key`
   - `brevo.sender_email`
   - `turnstile.site_key`
   - `turnstile.secret_key`
4. Start locally:

```bash
php -S 0.0.0.0:8080 -t web-registration-simple
```

5. Open `http://localhost:8080`.

## Compatible password hash

This demo uses the same flow as the game server:

- `passM = md5(password)`
- `stored = md5(passM + salt)`

## Account activation and email uniqueness

- Registrations start as `usergroupid = 3` (inactive).
- An activation token is stored in `user_activation_tokens`.
- Brevo sends an activation email with a link (`/?token=...`) that is valid for 2 hours.
- Clicking the link changes the account to `usergroupid = 40` (active).
- Expired activation links auto-ban the account (`usergroupid = 8`).
- Registration blocks duplicate usernames and duplicate email addresses.

## Security notes

- Use HTTPS in production.
- Cloudflare Turnstile is integrated and validated server-side for each registration.
- Add rate limiting as a second layer.
- Never commit `config.php` to git.
