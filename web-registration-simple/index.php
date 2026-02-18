<?php

declare(strict_types=1);

$configPath = __DIR__ . '/config.php';
$configMissing = !file_exists($configPath);
$config = $configMissing ? null : require $configPath;

function randomSalt(int $length = 30): string
{
    $alphabet = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    $alphabetLen = strlen($alphabet);
    $salt = '';

    for ($i = 0; $i < $length; $i++) {
        $salt .= $alphabet[random_int(0, $alphabetLen - 1)];
    }

    return $salt;
}

function dodianPasswordHash(string $password, string $salt): string
{
    $passM = md5($password);
    return md5($passM . $salt);
}

function pdoFromConfig(array $config): PDO
{
    $db = $config['db'];
    $dsn = sprintf('mysql:host=%s;port=%d;dbname=%s;charset=%s', $db['host'], $db['port'], $db['name'], $db['charset']);

    return new PDO($dsn, $db['user'], $db['password'], [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
    ]);
}

function verifyCloudflareTurnstile(array $config, string $token, string $remoteIp): bool
{
    $turnstile = $config['turnstile'] ?? [];
    $secretKey = trim((string)($turnstile['secret_key'] ?? ''));

    if ($secretKey === '') {
        throw new RuntimeException('Cloudflare Turnstile is not configured. Add turnstile.secret_key in config.php.');
    }

    $payload = http_build_query([
        'secret' => $secretKey,
        'response' => $token,
        'remoteip' => $remoteIp,
    ]);

    $ch = curl_init('https://challenges.cloudflare.com/turnstile/v0/siteverify');
    if ($ch === false) {
        throw new RuntimeException('Failed to initialize Turnstile verification request.');
    }

    curl_setopt_array($ch, [
        CURLOPT_POST => true,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_HTTPHEADER => [
            'content-type: application/x-www-form-urlencoded',
        ],
        CURLOPT_POSTFIELDS => $payload,
        CURLOPT_TIMEOUT => 15,
    ]);

    $response = curl_exec($ch);
    $status = curl_getinfo($ch, CURLINFO_RESPONSE_CODE);
    $error = curl_error($ch);
    curl_close($ch);

    if ($response === false) {
        throw new RuntimeException('Turnstile verification failed: ' . $error);
    }

    if ($status < 200 || $status >= 300) {
        throw new RuntimeException('Turnstile API returned HTTP ' . $status . ': ' . $response);
    }

    $decoded = json_decode($response, true);
    if (!is_array($decoded)) {
        throw new RuntimeException('Turnstile API returned an invalid response payload.');
    }

    return isset($decoded['success']) && $decoded['success'] === true;
}

const INACTIVE_USERGROUP_ID = 3;
const ACTIVE_USERGROUP_ID = 40;
const BANNED_USERGROUP_ID = 8;

function ensureActivationTable(PDO $pdo): void
{
    $pdo->exec(
        'CREATE TABLE IF NOT EXISTS user_activation_tokens (
            id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
            user_id INT UNSIGNED NOT NULL,
            token_hash CHAR(64) NOT NULL,
            expires_at DATETIME NOT NULL,
            created_at DATETIME NOT NULL,
            consumed_at DATETIME NULL,
            UNIQUE KEY unique_user_id (user_id),
            UNIQUE KEY unique_token_hash (token_hash)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4'
    );
}

function banExpiredPendingAccounts(PDO $pdo): void
{
    $banExpired = $pdo->prepare(
        'UPDATE user u
         INNER JOIN user_activation_tokens t ON t.user_id = u.userid
         SET u.usergroupid = :banned_group,
             t.consumed_at = NOW()
         WHERE u.usergroupid = :inactive_group
           AND t.consumed_at IS NULL
           AND t.expires_at < NOW()'
    );

    $banExpired->execute([
        'banned_group' => BANNED_USERGROUP_ID,
        'inactive_group' => INACTIVE_USERGROUP_ID,
    ]);
}

function sendBrevoActivationEmail(array $config, string $toEmail, string $username, string $activationUrl): void
{
    $brevo = $config['brevo'] ?? [];
    $apiKey = trim((string)($brevo['api_key'] ?? ''));
    $senderEmail = trim((string)($brevo['sender_email'] ?? ''));
    $senderName = trim((string)($brevo['sender_name'] ?? 'Dodian'));

    if ($apiKey === '' || $senderEmail === '') {
        throw new RuntimeException('Brevo is not configured. Add brevo.api_key and brevo.sender_email in config.php.');
    }

    $payload = [
        'sender' => [
            'email' => $senderEmail,
            'name' => $senderName,
        ],
        'to' => [[
            'email' => $toEmail,
            'name' => $username,
        ]],
        'subject' => 'Activate your Dodian account',
        'htmlContent' => '<p>Hi ' . htmlspecialchars($username, ENT_QUOTES, 'UTF-8') . ',</p>'
            . '<p>Thanks for registering. Click the link below to activate your account:</p>'
            . '<p><a href="' . htmlspecialchars($activationUrl, ENT_QUOTES, 'UTF-8') . '">Activate account</a></p>'
            . '<p>If you did not request this account, you can ignore this email.</p>',
    ];

    $ch = curl_init('https://api.brevo.com/v3/smtp/email');
    if ($ch === false) {
        throw new RuntimeException('Failed to initialize Brevo request.');
    }

    curl_setopt_array($ch, [
        CURLOPT_POST => true,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_HTTPHEADER => [
            'accept: application/json',
            'content-type: application/json',
            'api-key: ' . $apiKey,
        ],
        CURLOPT_POSTFIELDS => json_encode($payload, JSON_THROW_ON_ERROR),
        CURLOPT_TIMEOUT => 15,
    ]);

    $response = curl_exec($ch);
    $status = curl_getinfo($ch, CURLINFO_RESPONSE_CODE);
    $error = curl_error($ch);
    curl_close($ch);

    if ($response === false) {
        throw new RuntimeException('Brevo request failed: ' . $error);
    }

    if ($status < 200 || $status >= 300) {
        throw new RuntimeException('Brevo API returned HTTP ' . $status . ': ' . $response);
    }
}

$errors = [];
$successMessage = null;
$activationMessage = null;
$username = '';
$email = '';
$turnstileSiteKey = $configMissing ? '' : trim((string)(($config['turnstile']['site_key'] ?? '')));

if (!$configMissing && isset($_GET['token']) && is_string($_GET['token']) && $_GET['token'] !== '') {
    $token = trim($_GET['token']);

    if (!preg_match('/^[a-f0-9]{64}$/', $token)) {
        $errors[] = 'Activation link is invalid.';
    } else {
        try {
            $pdo = pdoFromConfig($config);
            ensureActivationTable($pdo);
            banExpiredPendingAccounts($pdo);

            $tokenHash = hash('sha256', $token);
            $lookup = $pdo->prepare(
                'SELECT t.user_id
                 FROM user_activation_tokens t
                 WHERE t.token_hash = :token_hash
                   AND t.consumed_at IS NULL
                   AND t.expires_at >= NOW()
                 LIMIT 1'
            );
            $lookup->execute(['token_hash' => $tokenHash]);
            $row = $lookup->fetch();

            if (!$row) {
                $errors[] = 'Activation link is invalid or expired.';
            } else {
                $pdo->beginTransaction();

                $activateUser = $pdo->prepare('UPDATE user SET usergroupid = :active_group WHERE userid = :userid');
                $activateUser->execute([
                    'active_group' => ACTIVE_USERGROUP_ID,
                    'userid' => (int)$row['user_id'],
                ]);

                $consumeToken = $pdo->prepare('UPDATE user_activation_tokens SET consumed_at = NOW() WHERE token_hash = :token_hash');
                $consumeToken->execute(['token_hash' => $tokenHash]);

                $pdo->commit();
                $activationMessage = 'Your account is now activated. You can log in in-game.';
            }
        } catch (Throwable $e) {
            if (isset($pdo) && $pdo instanceof PDO && $pdo->inTransaction()) {
                $pdo->rollBack();
            }
            $errors[] = 'Could not activate account right now. Please try again later.';
            error_log('Activation error: ' . $e->getMessage());
        }
    }
}

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    if ($configMissing) {
        $errors[] = 'Server is not configured yet. Copy config.example.php to config.php and add database credentials.';
    }

    $username = trim((string)($_POST['username'] ?? ''));
    $email = trim((string)($_POST['email'] ?? ''));
    $password = (string)($_POST['password'] ?? '');
    $confirmPassword = (string)($_POST['confirm_password'] ?? '');
    $turnstileToken = trim((string)($_POST['cf-turnstile-response'] ?? ''));

    if (!preg_match('/^[A-Za-z0-9_]{3,12}$/', $username)) {
        $errors[] = 'Username must be 3-12 characters and contain only letters, numbers, or underscore.';
    }

    if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
        $errors[] = 'Enter a valid email address.';
    }

    if (strlen($password) < 8) {
        $errors[] = 'Password must be at least 8 characters long.';
    }

    if ($password !== $confirmPassword) {
        $errors[] = 'Passwords do not match.';
    }

    if ($turnstileToken === '') {
        $errors[] = 'Please complete the anti-bot check.';
    }

    if (!$configMissing && $turnstileToken !== '' && empty($errors)) {
        try {
            $remoteIp = $_SERVER['REMOTE_ADDR'] ?? '';
            if (!verifyCloudflareTurnstile($config, $turnstileToken, $remoteIp)) {
                $errors[] = 'Anti-bot verification failed. Please try again.';
            }
        } catch (Throwable $e) {
            $errors[] = 'Could not verify anti-bot check right now. Please try again.';
            error_log('Turnstile verification error: ' . $e->getMessage());
        }
    }

    if (!$configMissing && empty($errors)) {
        try {
            $pdo = pdoFromConfig($config);
            ensureActivationTable($pdo);
            banExpiredPendingAccounts($pdo);

            $stmt = $pdo->prepare('SELECT 1 FROM user WHERE username = :username LIMIT 1');
            $stmt->execute(['username' => $username]);
            if ($stmt->fetch()) {
                $errors[] = 'This username already exists.';
            }

            $emailStmt = $pdo->prepare('SELECT 1 FROM user WHERE email = :email LIMIT 1');
            $emailStmt->execute(['email' => $email]);
            if ($emailStmt->fetch()) {
                $errors[] = 'This email address is already in use.';
            }

            if (empty($errors)) {
                $salt = randomSalt(30);
                $storedPassword = dodianPasswordHash($password, $salt);

                $pdo->beginTransaction();

                $insert = $pdo->prepare(
                    'INSERT INTO user (usergroupid, username, password, salt, email, passworddate, birthday_search, joindate)
                     VALUES (:usergroupid, :username, :password, :salt, :email, :passworddate, :birthday_search, :joindate)'
                );

                $insert->execute([
                    'usergroupid' => INACTIVE_USERGROUP_ID,
                    'username' => $username,
                    'password' => $storedPassword,
                    'salt' => $salt,
                    'email' => $email,
                    'passworddate' => date('Y-m-d H:i:s'),
                    'birthday_search' => '',
                    'joindate' => time(),
                ]);

                $userId = (int)$pdo->lastInsertId();
                $activationToken = bin2hex(random_bytes(32));
                $activationTokenHash = hash('sha256', $activationToken);

                $tokenInsert = $pdo->prepare(
                    'INSERT INTO user_activation_tokens (user_id, token_hash, expires_at, created_at)
                     VALUES (:user_id, :token_hash, DATE_ADD(NOW(), INTERVAL 2 HOUR), NOW())
                     ON DUPLICATE KEY UPDATE
                        token_hash = VALUES(token_hash),
                        expires_at = VALUES(expires_at),
                        created_at = VALUES(created_at),
                        consumed_at = NULL'
                );
                $tokenInsert->execute([
                    'user_id' => $userId,
                    'token_hash' => $activationTokenHash,
                ]);

                $baseUrl = rtrim((string)($config['app']['base_url'] ?? ''), '/');
                if ($baseUrl === '') {
                    throw new RuntimeException('Missing app.base_url in config.php for activation links.');
                }

                $activationUrl = $baseUrl . '/?token=' . urlencode($activationToken);
                sendBrevoActivationEmail($config, $email, $username, $activationUrl);

                $pdo->commit();

                $successMessage = 'Account created! Check your email to activate your account. This expires in 2 hours!';
                $username = '';
                $email = '';
            }
        } catch (Throwable $e) {
            if (isset($pdo) && $pdo instanceof PDO && $pdo->inTransaction()) {
                $pdo->rollBack();
            }
            $errors[] = 'Registration failed. Please verify configuration and try again.';
            error_log('Registration error: ' . $e->getMessage());
        }
    }
}
?>
<!doctype html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dodian Account Registration</title>
    <style>
        :root { color-scheme: dark; }
        body {
            margin: 0;
            font-family: Inter, system-ui, Arial, sans-serif;
            background: #0f172a;
            color: #e2e8f0;
            min-height: 100vh;
            display: grid;
            place-items: center;
            padding: 24px;
        }
        .card {
            width: 100%;
            max-width: 460px;
            background: #111827;
            border: 1px solid #1f2937;
            border-radius: 12px;
            padding: 24px;
            box-shadow: 0 10px 30px rgba(0,0,0,.35);
        }
        h1 { margin-top: 0; font-size: 1.4rem; }
        p.meta { color: #94a3b8; font-size: 0.95rem; }
        label { display: block; margin: 14px 0 6px; font-weight: 600; }
        input {
            width: 100%;
            padding: 10px 12px;
            border-radius: 8px;
            border: 1px solid #334155;
            background: #0b1220;
            color: #e2e8f0;
            box-sizing: border-box;
        }
        .turnstile-wrap {
            margin-top: 18px;
            display: flex;
            justify-content: center;
        }
        .turnstile-note {
            margin-top: 12px;
            color: #94a3b8;
            font-size: 0.85rem;
            text-align: center;
        }
        button {
            margin-top: 18px;
            width: 100%;
            padding: 11px 14px;
            border: 0;
            border-radius: 8px;
            background: #22c55e;
            color: #06220f;
            font-weight: 700;
            cursor: pointer;
        }
        .errors, .success {
            margin: 12px 0;
            padding: 10px 12px;
            border-radius: 8px;
            font-size: 0.95rem;
        }
        .errors { background: #7f1d1d; color: #fecaca; }
        .success { background: #14532d; color: #bbf7d0; }
        ul { margin: 0; padding-left: 20px; }
    </style>
</head>
<body>
<div class="card">
    <h1>Dodian registration</h1>
    <p class="meta">Create your account to log in to the game server.</p>

    <?php if ($configMissing): ?>
        <div class="errors">
            Config is missing: copy <code>config.example.php</code> to <code>config.php</code> before registrations can be saved.
        </div>
    <?php endif; ?>

    <?php if (!empty($errors)): ?>
        <div class="errors">
            <ul>
                <?php foreach ($errors as $error): ?>
                    <li><?= htmlspecialchars($error, ENT_QUOTES, 'UTF-8') ?></li>
                <?php endforeach; ?>
            </ul>
        </div>
    <?php endif; ?>

    <?php if ($activationMessage !== null): ?>
        <div class="success"><?= htmlspecialchars($activationMessage, ENT_QUOTES, 'UTF-8') ?></div>
    <?php endif; ?>

    <?php if ($successMessage !== null): ?>
        <div class="success"><?= htmlspecialchars($successMessage, ENT_QUOTES, 'UTF-8') ?></div>
    <?php endif; ?>

    <form method="post" action="">
        <label for="username">Username</label>
        <input id="username" name="username" maxlength="12" required value="<?= htmlspecialchars($username, ENT_QUOTES, 'UTF-8') ?>">

        <label for="email">E-mail</label>
        <input id="email" name="email" type="email" required value="<?= htmlspecialchars($email, ENT_QUOTES, 'UTF-8') ?>">

        <label for="password">Password</label>
        <input id="password" name="password" type="password" minlength="8" required>

        <label for="confirm_password">Repeat password</label>
        <input id="confirm_password" name="confirm_password" type="password" minlength="8" required>

        <?php if ($turnstileSiteKey !== ''): ?>
            <div class="turnstile-wrap">
                <div class="cf-turnstile" data-sitekey="<?= htmlspecialchars($turnstileSiteKey, ENT_QUOTES, 'UTF-8') ?>"></div>
            </div>
        <?php else: ?>
            <p class="turnstile-note">Turnstile is not configured yet. Add <code>turnstile.site_key</code> to config.php.</p>
        <?php endif; ?>

        <button type="submit">Create account</button>
    </form>
</div>
<?php if ($turnstileSiteKey !== ''): ?>
    <script src="https://challenges.cloudflare.com/turnstile/v0/api.js" async defer></script>
<?php endif; ?>
</body>
</html>
