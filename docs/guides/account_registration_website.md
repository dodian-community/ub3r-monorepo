# Account Registration Website Guide

Deze gids beschrijft wat je nodig hebt om een simpele website te maken waarmee spelers accounts kunnen registreren voor deze Ub3r game-server.

## Wat je al hebt in deze repo

- De server gebruikt een MySQL-database.
- Login valideert tegen de `user`-tabel.
- De login-flow gebruikt een salted MD5-hash (`MD5(MD5(password) + salt)`).

## Minimale eisen voor je registratie-website

1. **Web-app runtime**
   - Kies bijvoorbeeld Node.js (Express/Nest), PHP (Laravel), Python (Django/FastAPI) of Java (Spring Boot).

2. **Database-connectie naar dezelfde MySQL**
   - Verbind met dezelfde DB als de game-server (host, poort, naam, user, password).

3. **Registratieformulier**
   - Vereiste velden: username, email, password, password confirmation.
   - Server-side validatie:
     - username: 3-12 tekens, alleen letters/cijfers/underscore.
     - email: geldig formaat.
     - password: minimale lengte (bijv. 8+).

4. **Password hashing compatibel met game-server**
   - Genereer een random `salt` (30 chars, zoals DB-schema verwacht).
   - Maak hash als:
     - `passM = MD5(plainPassword)`
     - `password = MD5(passM + salt)`
   - Sla **beide** op: `password` en `salt`.

5. **Database insert in `user`-tabel**
   - Minimaal invullen:
     - `username`
     - `password`
     - `salt`
     - `email`
     - `passworddate`
     - `birthday_search`
   - Laat waar mogelijk defaults van de tabel het werk doen.
   - Gebruik altijd prepared statements (geen string-concatenatie).

6. **Foutafhandeling en UX**
   - Toon duidelijke errors bij:
     - bestaande username
     - zwak/ongeldig wachtwoord
     - databasefout
   - Succesmelding: account aangemaakt, je kunt nu inloggen in-game.

7. **Security (belangrijk)**
   - HTTPS verplicht.
   - Rate limiting op registratie endpoint.
   - CAPTCHA / bot bescherming.
   - Input sanitization + prepared statements.
   - Audit logging op registraties (ip, timestamp, username).

## Let op over hashing

Salted MD5 is verouderd voor moderne web-security. Voor compatibiliteit met deze server moet je dit voorlopig volgen. De beste lange-termijnstap is migreren naar een sterkere hash (bijv. Argon2id of bcrypt) in zowel game-server als webregistratie-flow.

## Aanbevolen API-contract (voorbeeld)

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

Response success:

```json
{
  "ok": true,
  "message": "Account created"
}
```

Response error:

```json
{
  "ok": false,
  "message": "Username already exists"
}
```

## Checklist om live te gaan

- [ ] DNS + domein
- [ ] HTTPS-certificaat
- [ ] Productie database credentials in secrets manager
- [ ] Dagelijkse database backups
- [ ] Monitoring + alerting op foutpercentages
- [ ] Basis abuse protectie (rate-limit + captcha)
