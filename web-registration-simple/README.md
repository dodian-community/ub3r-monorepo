# Simple PHP Registration Website

A minimal PHP + HTML registration page compatible with the current Ub3r password-hash flow.

## Getting started

1. Copy `config.example.php` to `config.php`.
2. Fill in your database credentials.
3. Start locally:

```bash
php -S 0.0.0.0:8080 -t web-registration-simple
```

4. Open `http://localhost:8080`.

## Compatible password hash

This demo uses the same flow as the game server:

- `passM = md5(password)`
- `stored = md5(passM + salt)`

## Security notes

- Use HTTPS in production.
- Add rate limiting and CAPTCHA.
- Never commit `config.php` to git.
