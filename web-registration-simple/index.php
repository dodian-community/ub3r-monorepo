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

$errors = [];
$successMessage = null;
$username = '';
$email = '';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    if ($configMissing) {
        $errors[] = 'Server is not configured yet. Copy config.example.php to config.php and add database credentials.';
    }

    $username = trim((string)($_POST['username'] ?? ''));
    $email = trim((string)($_POST['email'] ?? ''));
    $password = (string)($_POST['password'] ?? '');
    $confirmPassword = (string)($_POST['confirm_password'] ?? '');

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

    if (!$configMissing && empty($errors)) {
        try {
            $pdo = pdoFromConfig($config);

            $stmt = $pdo->prepare('SELECT 1 FROM user WHERE username = :username LIMIT 1');
            $stmt->execute(['username' => $username]);
            if ($stmt->fetch()) {
                $errors[] = 'This username already exists.';
            } else {
                $salt = randomSalt(30);
                $storedPassword = dodianPasswordHash($password, $salt);

                $insert = $pdo->prepare(
                    'INSERT INTO user (username, password, salt, email, passworddate, birthday_search, joindate)
                     VALUES (:username, :password, :salt, :email, :passworddate, :birthday_search, :joindate)'
                );

                $insert->execute([
                    'username' => $username,
                    'password' => $storedPassword,
                    'salt' => $salt,
                    'email' => $email,
                    'passworddate' => date('Y-m-d H:i:s'),
                    'birthday_search' => '',
                    'joindate' => time(),
                ]);

                $successMessage = 'Account created! You can now log in in-game.';
                $username = '';
                $email = '';
            }
        } catch (Throwable $e) {
            $errors[] = 'Registration failed due to a server/database error.';
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

        <button type="submit">Create account</button>
    </form>
</div>
</body>
</html>
