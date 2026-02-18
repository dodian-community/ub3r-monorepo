# Account Registration Website Guide

This guide explains what you need to build a simple website that lets players register accounts for this Ub3r game server.

## What you already have in this repository

- The server uses a MySQL database.
- Login validation checks the `user` table.
- The login flow uses a salted MD5 hash (`MD5(MD5(password) + salt)`).

## Minimum requirements for your registration website

1. **Web app runtime**
   - Use something like Node.js (Express/Nest), PHP (Laravel/plain PHP), Python (Django/FastAPI), or Java (Spring Boot).

2. **Database connection to the same MySQL instance**
   - Connect to the same DB as the game server (host, port, name, user, password).

3. **Registration form**
   - Required fields: username, email, password, password confirmation.
   - Server-side validation:
     - username: 3-12 chars, letters/numbers/underscore only.
     - email: valid email format.
     - password: minimum length (for example 8+).

4. **Password hashing compatible with the game server**
   - Generate a random `salt` (30 chars, matching the DB schema).
   - Build hash as:
     - `passM = MD5(plainPassword)`
     - `password = MD5(passM + salt)`
   - Store **both** values: `password` and `salt`.

5. **Database insert into `user` table**
   - Minimum fields to populate:
     - `username`
     - `password`
     - `salt`
     - `email`
     - `passworddate`
     - `birthday_search`
   - Let table defaults handle the rest when possible.
   - Always use prepared statements (no SQL string concatenation).

6. **Error handling and UX**
   - Show clear errors for:
     - existing username
     - weak/invalid password
     - database error
   - Success message example: account created, you can now log in in-game.

7. **Security (important)**
   - HTTPS required.
   - Rate limiting on registration endpoint.
   - CAPTCHA / bot protection.
   - Input sanitization + prepared statements.
   - Audit logging for registrations (IP, timestamp, username).

## Important note on hashing

Salted MD5 is outdated for modern web security. You need it for compatibility with this server today. The best long-term improvement is migrating to a stronger hash (for example Argon2id or bcrypt) in both the game server and registration flow.

## Recommended API contract (example)

`POST /api/register`

Request:

```json
{
  "username": "Player123",
  "email": "player@example.com",
  "password": "SuperSecret123",
  "confirmPassword": "SuperSecret123"
}
```

Success response:

```json
{
  "ok": true,
  "message": "Account created"
}
```

Error response:

```json
{
  "ok": false,
  "message": "Username already exists"
}
```

## Go-live checklist

- [ ] DNS + domain
- [ ] HTTPS certificate
- [ ] Production DB credentials stored in a secrets manager
- [ ] Daily database backups
- [ ] Monitoring + alerting for error rates
- [ ] Basic abuse protection (rate limit + CAPTCHA)
