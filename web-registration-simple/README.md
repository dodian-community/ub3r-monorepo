# Simple PHP Registration Website

Een minimale registratiepagina in PHP + HTML, compatibel met de huidige Ub3r password-hash flow.

## Starten

1. Kopieer `config.example.php` naar `config.php`.
2. Vul je databasegegevens in.
3. Start lokaal:

```bash
php -S 0.0.0.0:8080 -t web-registration-simple
```

4. Open `http://localhost:8080`.

## Compatibele password hash

Deze demo gebruikt dezelfde flow als de game-server:

- `passM = md5(password)`
- `stored = md5(passM + salt)`

## Veiligheidsnotities

- Gebruik HTTPS in productie.
- Voeg rate limiting en captcha toe.
- Sla `config.php` nooit in git op.
