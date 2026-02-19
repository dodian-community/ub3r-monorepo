<?php

declare(strict_types=1);

session_start();

const INACTIVE_USERGROUP_ID = 3;
const ACTIVE_USERGROUP_ID = 40;
const BANNED_USERGROUP_ID = 8;
const PREMIUM_USERGROUP_ID = 11;
const MODERATOR_USERGROUP_ID = 5;
const TRIAL_MODERATOR_USERGROUP_ID = 9;
const ADMINISTRATOR_USERGROUP_ID = 6;
const DEVELOPER_USERGROUP_ID = 10;
const WEB_MANAGEMENT_USERGROUP_ID = 18;
const ADMIN_PANEL_ALLOWED_GROUPS = [ADMINISTRATOR_USERGROUP_ID, DEVELOPER_USERGROUP_ID, WEB_MANAGEMENT_USERGROUP_ID];
const DISCORD_ROLE_SYNC_INTERVAL_SECONDS = 1800;
const DISCORD_API_BASE_URL = 'https://discord.com/api/v10';

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

function ensurePasswordResetTable(PDO $pdo): void
{
    $pdo->exec(
        'CREATE TABLE IF NOT EXISTS user_password_reset_tokens (
            id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
            user_id INT UNSIGNED NOT NULL,
            token_hash CHAR(64) NOT NULL,
            expires_at DATETIME NOT NULL,
            created_at DATETIME NOT NULL,
            consumed_at DATETIME NULL,
            UNIQUE KEY unique_token_hash (token_hash),
            KEY idx_user_id (user_id)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4'
    );
}

function buildDiscordOAuthUrl(array $config, string $state): string
{
    $discord = $config['discord'] ?? [];
    $clientId = trim((string)($discord['client_id'] ?? ''));
    $redirectUri = trim((string)($discord['redirect_uri'] ?? ''));

    if ($clientId === '' || $redirectUri === '') {
        throw new RuntimeException('Discord OAuth is not configured. Add discord.client_id and discord.redirect_uri in config.php.');
    }

    $query = http_build_query([
        'client_id' => $clientId,
        'redirect_uri' => $redirectUri,
        'response_type' => 'code',
        'scope' => 'identify',
        'state' => $state,
        'prompt' => 'consent',
    ]);

    return 'https://discord.com/oauth2/authorize?' . $query;
}

function discordApiRequest(string $method, string $endpoint, array $headers = [], ?array $formBody = null, ?array $jsonBody = null): array
{
    $ch = curl_init(DISCORD_API_BASE_URL . $endpoint);
    if ($ch === false) {
        throw new RuntimeException('Failed to initialize Discord request.');
    }

    $requestHeaders = array_merge(['accept: application/json'], $headers);
    $payload = null;

    if ($formBody !== null) {
        $requestHeaders[] = 'content-type: application/x-www-form-urlencoded';
        $payload = http_build_query($formBody);
    } elseif ($jsonBody !== null) {
        $requestHeaders[] = 'content-type: application/json';
        $payload = json_encode($jsonBody, JSON_THROW_ON_ERROR);
    }

    curl_setopt_array($ch, [
        CURLOPT_CUSTOMREQUEST => $method,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_HTTPHEADER => $requestHeaders,
        CURLOPT_TIMEOUT => 15,
    ]);

    if ($payload !== null) {
        curl_setopt($ch, CURLOPT_POSTFIELDS, $payload);
    }

    $response = curl_exec($ch);
    $status = curl_getinfo($ch, CURLINFO_RESPONSE_CODE);
    $error = curl_error($ch);
    curl_close($ch);

    if ($response === false) {
        throw new RuntimeException('Discord request failed: ' . $error);
    }

    $decoded = json_decode($response, true);
    if (!is_array($decoded) && $response !== '') {
        throw new RuntimeException('Discord API returned invalid JSON.');
    }

    if ($status < 200 || $status >= 300) {
        $message = is_array($decoded) ? (string)($decoded['message'] ?? 'Unknown error') : $response;
        throw new RuntimeException('Discord API returned HTTP ' . $status . ': ' . $message);
    }

    return is_array($decoded) ? $decoded : [];
}

function exchangeDiscordCode(array $config, string $code): array
{
    $discord = $config['discord'] ?? [];
    $clientId = trim((string)($discord['client_id'] ?? ''));
    $clientSecret = trim((string)($discord['client_secret'] ?? ''));
    $redirectUri = trim((string)($discord['redirect_uri'] ?? ''));

    if ($clientId === '' || $clientSecret === '' || $redirectUri === '') {
        throw new RuntimeException('Discord OAuth is not configured. Add discord.client_id, discord.client_secret, and discord.redirect_uri in config.php.');
    }

    return discordApiRequest(
        'POST',
        '/oauth2/token',
        [],
        [
            'client_id' => $clientId,
            'client_secret' => $clientSecret,
            'grant_type' => 'authorization_code',
            'code' => $code,
            'redirect_uri' => $redirectUri,
        ]
    );
}

function syncDiscordNickname(array $config, string $discordUserId, string $gameUsername): void
{
    $discord = $config['discord'] ?? [];
    $botToken = trim((string)($discord['bot_token'] ?? ''));
    $guildId = trim((string)($discord['guild_id'] ?? ''));

    if ($botToken === '' || $guildId === '') {
        throw new RuntimeException('Discord bot is not configured. Add discord.bot_token and discord.guild_id in config.php.');
    }

    discordApiRequest(
        'PATCH',
        '/guilds/' . rawurlencode($guildId) . '/members/' . rawurlencode($discordUserId),
        ['authorization: Bot ' . $botToken],
        null,
        ['nick' => $gameUsername]
    );
}


function syncDiscordVerifiedRole(array $config, string $discordUserId): void
{
    $discord = $config['discord'] ?? [];
    $botToken = trim((string)($discord['bot_token'] ?? ''));
    $guildId = trim((string)($discord['guild_id'] ?? ''));
    $verifiedRoleId = trim((string)($discord['verified_role_id'] ?? ''));

    if ($botToken === '' || $guildId === '' || $verifiedRoleId === '') {
        throw new RuntimeException('Discord verified role sync is not configured. Add discord.bot_token, discord.guild_id, and discord.verified_role_id in config.php.');
    }

    discordApiRequest(
        'PUT',
        '/guilds/' . rawurlencode($guildId) . '/members/' . rawurlencode($discordUserId) . '/roles/' . rawurlencode($verifiedRoleId),
        ['authorization: Bot ' . $botToken]
    );
}


function getSupportedWebRoles(): array
{
    return [
        'verified' => ['label' => 'Verified', 'usergroupid' => ACTIVE_USERGROUP_ID, 'ban' => false],
        'premium' => ['label' => 'Premium', 'usergroupid' => PREMIUM_USERGROUP_ID, 'ban' => false],
        'moderator' => ['label' => 'Moderator', 'usergroupid' => MODERATOR_USERGROUP_ID, 'ban' => false],
        'trial_moderator' => ['label' => 'Trial moderator', 'usergroupid' => TRIAL_MODERATOR_USERGROUP_ID, 'ban' => false],
        'administrator' => ['label' => 'Administrator', 'usergroupid' => ADMINISTRATOR_USERGROUP_ID, 'ban' => false],
        'developer' => ['label' => 'Developer', 'usergroupid' => DEVELOPER_USERGROUP_ID, 'ban' => false],
        'banned' => ['label' => 'Banned', 'usergroupid' => BANNED_USERGROUP_ID, 'ban' => false],
    ];
}

function canAccessAdminPanel(?int $userGroupId): bool
{
    return $userGroupId !== null && in_array($userGroupId, ADMIN_PANEL_ALLOWED_GROUPS, true);
}


function resolveCurrentUserGroupId(PDO $pdo, ?int $sessionUserId, ?int $sessionUserGroupId): ?int
{
    if ($sessionUserId === null) {
        return $sessionUserGroupId;
    }

    try {
        $lookup = $pdo->prepare('SELECT usergroupid FROM user WHERE userid = :userid LIMIT 1');
        $lookup->execute(['userid' => $sessionUserId]);
        $row = $lookup->fetch();

        if ($row) {
            return (int)$row['usergroupid'];
        }
    } catch (Throwable $e) {
        error_log('Unable to resolve current usergroup: ' . $e->getMessage());
    }

    return $sessionUserGroupId;
}


function resolveUserGroupIdByUsername(PDO $pdo, ?string $username): ?int
{
    if ($username === null || trim($username) === '') {
        return null;
    }

    try {
        $lookup = $pdo->prepare('SELECT usergroupid FROM user WHERE username = :username LIMIT 1');
        $lookup->execute(['username' => trim($username)]);
        $row = $lookup->fetch();

        if ($row) {
            return (int)$row['usergroupid'];
        }
    } catch (Throwable $e) {
        error_log('Unable to resolve usergroup by username: ' . $e->getMessage());
    }

    return null;
}

function findRoleKeyByUserGroupId(int $userGroupId): ?string
{
    foreach (getSupportedWebRoles() as $roleKey => $roleDefinition) {
        if ((int)$roleDefinition['usergroupid'] === $userGroupId) {
            return $roleKey;
        }
    }

    return null;
}

function resolveRoleByUserData(int $userGroupId, int $unbanTime): string
{
    if ($unbanTime > time()) {
        return 'banned';
    }

    return findRoleKeyByUserGroupId($userGroupId) ?? 'verified';
}


function isAccountCurrentlyBanned(PDO $pdo, int $userId): bool
{
    try {
        $lookup = $pdo->prepare('SELECT unbantime FROM game_characters WHERE id = :id LIMIT 1');
        $lookup->execute(['id' => $userId]);
        $row = $lookup->fetch();

        if (!$row) {
            return false;
        }

        return (int)($row['unbantime'] ?? 0) > time();
    } catch (Throwable $e) {
        error_log('Ban status lookup failed for user ' . $userId . ': ' . $e->getMessage());
        return false;
    }
}

function ensureDiscordRoleSyncTable(PDO $pdo): void
{
    $pdo->exec(
        'CREATE TABLE IF NOT EXISTS user_discord_links (
            user_id INT UNSIGNED NOT NULL PRIMARY KEY,
            discord_user_id VARCHAR(32) NOT NULL,
            discord_username VARCHAR(128) NOT NULL,
            access_token TEXT NULL,
            refresh_token TEXT NULL,
            token_expires_at DATETIME NULL,
            roles_last_synced_at DATETIME NULL,
            created_at DATETIME NOT NULL,
            updated_at DATETIME NOT NULL,
            UNIQUE KEY unique_discord_user_id (discord_user_id)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4'
    );
}

function upsertDiscordLink(PDO $pdo, int $userId, string $discordUserId, string $discordUsername, ?string $accessToken, ?string $refreshToken, ?int $expiresInSeconds): void
{
    $expiresAt = null;
    if ($expiresInSeconds !== null && $expiresInSeconds > 0) {
        $expiresAt = date('Y-m-d H:i:s', time() + $expiresInSeconds);
    }

    $statement = $pdo->prepare(
        'INSERT INTO user_discord_links (user_id, discord_user_id, discord_username, access_token, refresh_token, token_expires_at, roles_last_synced_at, created_at, updated_at)
         VALUES (:user_id, :discord_user_id, :discord_username, :access_token, :refresh_token, :token_expires_at, NULL, NOW(), NOW())
         ON DUPLICATE KEY UPDATE
            discord_user_id = VALUES(discord_user_id),
            discord_username = VALUES(discord_username),
            access_token = VALUES(access_token),
            refresh_token = VALUES(refresh_token),
            token_expires_at = VALUES(token_expires_at),
            updated_at = NOW()'
    );

    $statement->execute([
        'user_id' => $userId,
        'discord_user_id' => $discordUserId,
        'discord_username' => $discordUsername,
        'access_token' => $accessToken,
        'refresh_token' => $refreshToken,
        'token_expires_at' => $expiresAt,
    ]);
}

function getDiscordRoleMap(array $config): array
{
    $discord = $config['discord'] ?? [];

    return [
        'verified' => trim((string)($discord['verified_role_id'] ?? '')),
        'premium' => trim((string)($discord['premium_role_id'] ?? '')),
        'moderator' => trim((string)($discord['moderator_role_id'] ?? '')),
        'trial_moderator' => trim((string)($discord['trial_moderator_role_id'] ?? '')),
        'administrator' => trim((string)($discord['administrator_role_id'] ?? '')),
        'developer' => trim((string)($discord['developer_role_id'] ?? '')),
        'banned' => trim((string)($discord['banned_role_id'] ?? '')),
    ];
}

function syncDiscordRolesForLinkedUser(array $config, string $discordUserId, string $roleKey): void
{
    $discord = $config['discord'] ?? [];
    $botToken = trim((string)($discord['bot_token'] ?? ''));
    $guildId = trim((string)($discord['guild_id'] ?? ''));

    if ($botToken === '' || $guildId === '') {
        throw new RuntimeException('Discord role sync is not configured. Add discord.bot_token and discord.guild_id in config.php.');
    }

    $roleMap = getDiscordRoleMap($config);
    $managedRoleKeys = ['verified', 'premium', 'moderator', 'trial_moderator', 'administrator', 'developer', 'banned'];

    foreach ($managedRoleKeys as $managedRoleKey) {
        $discordRoleId = $roleMap[$managedRoleKey] ?? '';
        if ($discordRoleId === '') {
            continue;
        }

        $method = $managedRoleKey === $roleKey ? 'PUT' : 'DELETE';
        discordApiRequest(
            $method,
            '/guilds/' . rawurlencode($guildId) . '/members/' . rawurlencode($discordUserId) . '/roles/' . rawurlencode($discordRoleId),
            ['authorization: Bot ' . $botToken]
        );
    }
}

function syncDiscordRolesForUser(PDO $pdo, array $config, int $userId, int $userGroupId, int $unbanTime): void
{
    ensureDiscordRoleSyncTable($pdo);

    $lookup = $pdo->prepare('SELECT discord_user_id FROM user_discord_links WHERE user_id = :user_id LIMIT 1');
    $lookup->execute(['user_id' => $userId]);
    $row = $lookup->fetch();

    if (!$row || !isset($row['discord_user_id'])) {
        return;
    }

    $roleKey = resolveRoleByUserData($userGroupId, $unbanTime);
    syncDiscordRolesForLinkedUser($config, (string)$row['discord_user_id'], $roleKey);

    $update = $pdo->prepare('UPDATE user_discord_links SET roles_last_synced_at = NOW(), updated_at = NOW() WHERE user_id = :user_id');
    $update->execute(['user_id' => $userId]);
}

function syncPendingDiscordRoles(PDO $pdo, array $config): int
{
    ensureDiscordRoleSyncTable($pdo);

    $lookup = $pdo->prepare(
        'SELECT l.user_id, l.discord_user_id, u.usergroupid, c.unbantime
         FROM user_discord_links l
         INNER JOIN user u ON u.userid = l.user_id
         INNER JOIN game_characters c ON c.id = l.user_id
         WHERE l.roles_last_synced_at IS NULL OR l.roles_last_synced_at <= DATE_SUB(NOW(), INTERVAL :interval_seconds SECOND)
         ORDER BY l.user_id ASC'
    );
    $lookup->bindValue(':interval_seconds', DISCORD_ROLE_SYNC_INTERVAL_SECONDS, PDO::PARAM_INT);
    $lookup->execute();

    $synced = 0;

    while ($row = $lookup->fetch()) {
        try {
            $roleKey = resolveRoleByUserData((int)$row['usergroupid'], (int)$row['unbantime']);
            syncDiscordRolesForLinkedUser($config, (string)$row['discord_user_id'], $roleKey);

            $update = $pdo->prepare('UPDATE user_discord_links SET roles_last_synced_at = NOW(), updated_at = NOW() WHERE user_id = :user_id');
            $update->execute(['user_id' => (int)$row['user_id']]);
            $synced++;
        } catch (Throwable $e) {
            error_log('Discord periodic role sync error for user ' . (int)$row['user_id'] . ': ' . $e->getMessage());
        }
    }

    return $synced;
}

function applyWebRoleToAccount(PDO $pdo, int $userId, string $roleKey): void
{
    $roles = getSupportedWebRoles();
    if (!isset($roles[$roleKey])) {
        throw new RuntimeException('Unknown role selected.');
    }

    $roleDefinition = $roles[$roleKey];
    $userGroupId = (int)$roleDefinition['usergroupid'];

    $updateUser = $pdo->prepare('UPDATE user SET usergroupid = :usergroupid WHERE userid = :userid LIMIT 1');
    $updateUser->execute([
        'usergroupid' => $userGroupId,
        'userid' => $userId,
    ]);
}

function buildDiscordRoleSyncHelpMessage(Throwable $error): string
{
    $message = $error->getMessage();

    if (str_contains($message, 'HTTP 403')) {
        return 'Discord account linked, but assigning Discord roles failed. Ensure the bot can Manage Roles and that the bot role is above all managed roles.';
    }

    if (str_contains($message, 'HTTP 404')) {
        return 'Discord account linked, but assigning Discord roles failed. Verify configured discord role IDs and confirm the linked user is in the configured guild.';
    }

    return 'Discord account linked, but assigning Discord roles failed. Ask an admin to verify role permissions and configuration.';
}

function describeHighestRole(array $member, array $rolesById): array
{
    $highest = ['name' => '@everyone', 'position' => 0, 'managed' => false];
    $memberRoleIds = isset($member['roles']) && is_array($member['roles']) ? $member['roles'] : [];

    foreach ($memberRoleIds as $roleId) {
        if (!is_string($roleId) || !isset($rolesById[$roleId])) {
            continue;
        }

        $role = $rolesById[$roleId];
        $position = (int)($role['position'] ?? 0);
        if ($position > $highest['position']) {
            $highest = [
                'name' => (string)($role['name'] ?? '@unknown-role'),
                'position' => $position,
                'managed' => (bool)($role['managed'] ?? false),
            ];
        }
    }

    return $highest;
}

function detectDiscordHierarchyHint(array $config, string $discordUserId): ?string
{
    try {
        $discord = $config['discord'] ?? [];
        $botToken = trim((string)($discord['bot_token'] ?? ''));
        $guildId = trim((string)($discord['guild_id'] ?? ''));
        if ($botToken === '' || $guildId === '') {
            return null;
        }

        $headers = ['authorization: Bot ' . $botToken];
        $roles = discordApiRequest('GET', '/guilds/' . rawurlencode($guildId) . '/roles', $headers);
        $botMember = discordApiRequest('GET', '/guilds/' . rawurlencode($guildId) . '/members/@me', $headers);
        $targetMember = discordApiRequest('GET', '/guilds/' . rawurlencode($guildId) . '/members/' . rawurlencode($discordUserId), $headers);

        $rolesById = [];
        foreach ($roles as $role) {
            if (!is_array($role) || !isset($role['id'])) {
                continue;
            }
            $rolesById[(string)$role['id']] = $role;
        }

        $botHighest = describeHighestRole($botMember, $rolesById);
        $targetHighest = describeHighestRole($targetMember, $rolesById);

        if ($botHighest['position'] <= $targetHighest['position']) {
            $targetRoleType = $targetHighest['managed'] ? ' (integration/managed role)' : '';
            return 'Bot role hierarchy issue: bot highest role is "' . $botHighest['name'] . '" (position ' . $botHighest['position'] . ') while target user highest role is "' . $targetHighest['name'] . '"' . $targetRoleType . ' (position ' . $targetHighest['position'] . '). Even with the same permissions, nickname edits only work when the bot role position is strictly higher. Move the bot role above that role and keep Manage Nicknames enabled.';
        }
    } catch (Throwable $e) {
        return null;
    }

    return null;
}


function detectDiscord404Hint(array $config, string $discordUserId, string $baseMessage): ?string
{
    $messageLower = strtolower($baseMessage);
    if (str_contains($messageLower, 'unknown guild')) {
        return 'Discord account linked, but nickname sync failed because the configured guild_id is invalid for this bot. Verify discord.guild_id and ensure the bot is in that server.';
    }

    if (str_contains($messageLower, 'unknown member')) {
        return 'Discord account linked, but nickname sync failed because this Discord account is not a member of the configured server. Join the server and run linking again.';
    }

    try {
        $discord = $config['discord'] ?? [];
        $botToken = trim((string)($discord['bot_token'] ?? ''));
        $guildId = trim((string)($discord['guild_id'] ?? ''));
        if ($botToken === '' || $guildId === '') {
            return null;
        }

        $headers = ['authorization: Bot ' . $botToken];

        try {
            discordApiRequest('GET', '/guilds/' . rawurlencode($guildId), $headers);
        } catch (Throwable $guildError) {
            return 'Discord account linked, but nickname sync failed because the configured guild_id cannot be accessed by the bot. Verify discord.guild_id and bot membership in that server.';
        }

        try {
            discordApiRequest('GET', '/guilds/' . rawurlencode($guildId) . '/members/' . rawurlencode($discordUserId), $headers);
        } catch (Throwable $memberError) {
            return 'Discord account linked, but nickname sync failed because this Discord account is not in the configured server. Join that server and run linking again.';
        }
    } catch (Throwable $e) {
        return null;
    }

    return null;
}

function buildDiscordNicknameSyncHelpMessage(array $config, string $discordUserId, Throwable $error): string
{
    $message = $error->getMessage();

    if (str_contains($message, 'HTTP 403')) {
        $hierarchyHint = detectDiscordHierarchyHint($config, $discordUserId);
        if ($hierarchyHint !== null) {
            return 'Discord account linked, but nickname sync failed. ' . $hierarchyHint;
        }

        return "Discord account linked, but nickname sync failed because Discord enforces role hierarchy. Matching permissions alone are not enough: the bot top role must be strictly above the member's top role, and guild owners cannot be renamed.";
    }

    if (str_contains($message, 'HTTP 404')) {
        $membershipHint = detectDiscord404Hint($config, $discordUserId, $message);
        if ($membershipHint !== null) {
            return $membershipHint;
        }

        return 'Discord account linked, but nickname sync failed with Discord 404. Check discord.guild_id, bot guild membership, and whether the linked Discord account is in that server.';
    }

    return 'Discord account linked, but nickname sync failed. Please ask an admin to verify bot permissions and guild configuration.';
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

function sendBrevoEmail(array $config, string $toEmail, string $toName, string $subject, string $htmlContent): void
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
            'name' => $toName,
        ]],
        'subject' => $subject,
        'htmlContent' => $htmlContent,
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

function buildAppUrl(array $config, string $pathAndQuery): string
{
    $baseUrl = rtrim((string)($config['app']['base_url'] ?? ''), '/');
    if ($baseUrl === '') {
        throw new RuntimeException('Missing app.base_url in config.php for account links.');
    }

    return $baseUrl . '/' . ltrim($pathAndQuery, '/');
}

function requireConfiguredOrFail(bool $configMissing, array &$errors): bool
{
    if ($configMissing) {
        $errors[] = 'Server is not configured yet. Copy config.example.php to config.php and add database credentials.';
        return false;
    }

    return true;
}

function redirectTo(string $url): void
{
    header('Location: ' . $url);
    exit;
}

$errors = [];
$successMessage = null;
$infoMessage = null;
$turnstileSiteKey = $configMissing ? '' : trim((string)($config['turnstile']['site_key'] ?? ''));
$clientJarUrl = $configMissing ? '#' : trim((string)($config['app']['client_jar_url'] ?? '#'));
$javaDownloadUrl = $configMissing ? 'https://www.java.com/download/' : trim((string)($config['app']['java_download_url'] ?? 'https://www.java.com/download/'));
$discordUrl = $configMissing ? 'https://discord.gg/' : trim((string)($config['app']['discord_url'] ?? 'https://discord.gg/'));

$page = isset($_GET['page']) && is_string($_GET['page']) ? strtolower(trim($_GET['page'])) : '';
$allowedPages = ['login', 'register', 'forgot-password', 'download', 'reset-password', 'change-password', 'activate', 'discord-link', 'admin-users'];
if (!in_array($page, $allowedPages, true)) {
    $page = isset($_SESSION['user_id']) ? 'download' : 'login';
}

if ($page === 'activate' && isset($_GET['token']) && is_string($_GET['token']) && $_GET['token'] !== '') {
    $token = trim($_GET['token']);

    if (!preg_match('/^[a-f0-9]{64}$/', $token)) {
        $errors[] = 'Activation link is invalid.';
    } elseif (requireConfiguredOrFail($configMissing, $errors)) {
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
                $successMessage = 'Your account has been activated. You can now sign in.';
                $page = 'login';
            }
        } catch (Throwable $e) {
            if (isset($pdo) && $pdo instanceof PDO && $pdo->inTransaction()) {
                $pdo->rollBack();
            }
            $errors[] = 'Could not activate account right now. Please try again later.';
            error_log('Activation error: ' . $e->getMessage());
            $page = 'login';
        }
    }
}



if ($page === 'discord-link') {
    if (!isset($_SESSION['user_id'])) {
        $infoMessage = 'Please sign in first.';
        $page = 'login';
    } elseif (requireConfiguredOrFail($configMissing, $errors)) {
        try {
            $pdo = pdoFromConfig($config);
            $hasCode = isset($_GET['code']) && is_string($_GET['code']) && $_GET['code'] !== '';
            if (!$hasCode) {
                $state = bin2hex(random_bytes(16));
                $_SESSION['discord_oauth_state'] = $state;
                redirectTo(buildDiscordOAuthUrl($config, $state));
            }

            $code = trim((string)$_GET['code']);
            $state = trim((string)($_GET['state'] ?? ''));
            $expectedState = (string)($_SESSION['discord_oauth_state'] ?? '');
            unset($_SESSION['discord_oauth_state']);

            if ($state === '' || $expectedState === '' || !hash_equals($expectedState, $state)) {
                throw new RuntimeException('Invalid Discord OAuth state. Please try linking again.');
            }

            $tokenResponse = exchangeDiscordCode($config, $code);
            $accessToken = trim((string)($tokenResponse['access_token'] ?? ''));
            $refreshToken = trim((string)($tokenResponse['refresh_token'] ?? ''));
            $expiresIn = isset($tokenResponse['expires_in']) ? (int)$tokenResponse['expires_in'] : null;
            if ($accessToken === '') {
                throw new RuntimeException('Discord OAuth did not return an access token.');
            }

            $discordUser = discordApiRequest('GET', '/users/@me', ['authorization: Bearer ' . $accessToken]);
            $discordUserId = trim((string)($discordUser['id'] ?? ''));
            $discordUsername = trim((string)($discordUser['global_name'] ?? $discordUser['username'] ?? ''));

            if ($discordUserId === '' || $discordUsername === '') {
                throw new RuntimeException('Could not read Discord account identity.');
            }

            $userId = (int)$_SESSION['user_id'];
            $gameUsername = (string)($_SESSION['username'] ?? '');
            if ($gameUsername === '') {
                $lookupName = $pdo->prepare('SELECT username FROM user WHERE userid = :userid LIMIT 1');
                $lookupName->execute(['userid' => $userId]);
                $nameRow = $lookupName->fetch();
                if (!$nameRow) {
                    throw new RuntimeException('Game account was not found.');
                }
                $gameUsername = (string)$nameRow['username'];
                $_SESSION['username'] = $gameUsername;
            }

            ensureDiscordRoleSyncTable($pdo);
            upsertDiscordLink($pdo, $userId, $discordUserId, $discordUsername, $accessToken, $refreshToken !== '' ? $refreshToken : null, $expiresIn);

            $syncWarnings = [];
            try {
                syncDiscordNickname($config, $discordUserId, $gameUsername);
            } catch (Throwable $nicknameError) {
                $syncWarnings[] = buildDiscordNicknameSyncHelpMessage($config, $discordUserId, $nicknameError);
                error_log('Discord nickname sync error: ' . $nicknameError->getMessage());
            }

            try {
                $currentUserGroup = isset($_SESSION['usergroupid']) ? (int)$_SESSION['usergroupid'] : ACTIVE_USERGROUP_ID;
                syncDiscordRolesForUser($pdo, $config, $userId, $currentUserGroup, 0);
            } catch (Throwable $roleError) {
                $syncWarnings[] = buildDiscordRoleSyncHelpMessage($roleError);
                error_log('Discord role sync error: ' . $roleError->getMessage());
            }

            $_SESSION['discord_link_username'] = $discordUsername;
            $_SESSION['discord_link_synced_at'] = date('Y-m-d H:i:s');

            if (empty($syncWarnings)) {
                $successMessage = 'Discord linked successfully. Nickname and Discord roles were synced.';
            } else {
                $infoMessage = implode(' ', $syncWarnings);
                $successMessage = 'Discord account linked successfully.';
            }
            $page = 'download';
        } catch (Throwable $e) {
            $errors[] = 'Discord linking failed: ' . $e->getMessage();
            error_log('Discord link error: ' . $e->getMessage());
            $page = 'download';
        }
    }
}

if (isset($_GET['logout'])) {
    $_SESSION = [];
    session_destroy();
    session_start();
    $successMessage = 'You are now signed out.';
    $page = 'login';
}

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $action = isset($_POST['action']) && is_string($_POST['action']) ? trim($_POST['action']) : '';

    if ($action === 'register') {
        $page = 'register';
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

        if (requireConfiguredOrFail($configMissing, $errors) && $turnstileToken !== '' && empty($errors)) {
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

        if (requireConfiguredOrFail($configMissing, $errors) && empty($errors)) {
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

                    $activationUrl = buildAppUrl($config, '?page=activate&token=' . urlencode($activationToken));
                    sendBrevoEmail(
                        $config,
                        $email,
                        $username,
                        'Activate your Dodian account',
                        '<p>Hi ' . htmlspecialchars($username, ENT_QUOTES, 'UTF-8') . ',</p>'
                        . '<p>Thanks for registering. Click the link below to activate your account:</p>'
                        . '<p><a href="' . htmlspecialchars($activationUrl, ENT_QUOTES, 'UTF-8') . '">Activate account</a></p>'
                        . '<p>If you did not request this account, you can ignore this email.</p>'
                    );

                    $pdo->commit();

                    $successMessage = 'Account created! Check your email to activate your account. This link expires in 2 hours.';
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

    if ($action === 'login') {
        $page = 'login';
        $username = trim((string)($_POST['username'] ?? ''));
        $password = (string)($_POST['password'] ?? '');

        if ($username === '' || $password === '') {
            $errors[] = 'Enter both username and password.';
        }

        if (requireConfiguredOrFail($configMissing, $errors) && empty($errors)) {
            try {
                $pdo = pdoFromConfig($config);
                ensureActivationTable($pdo);
                banExpiredPendingAccounts($pdo);

                $stmt = $pdo->prepare(
                    'SELECT userid, username, password, salt, usergroupid
                     FROM user
                     WHERE username = :username
                     LIMIT 1'
                );
                $stmt->execute(['username' => $username]);
                $user = $stmt->fetch();

                if (!$user) {
                    $errors[] = 'Invalid username or password.';
                } else {
                    $expectedPassword = dodianPasswordHash($password, (string)$user['salt']);
                    if (!hash_equals((string)$user['password'], $expectedPassword)) {
                        $errors[] = 'Invalid username or password.';
                    } elseif ((int)$user['usergroupid'] === INACTIVE_USERGROUP_ID) {
                        $errors[] = 'Your account is not activated yet. Please check your email first.';
                    } elseif ((int)$user['usergroupid'] === BANNED_USERGROUP_ID || isAccountCurrentlyBanned($pdo, (int)$user['userid'])) {
                        $errors[] = 'This account is banned.';
                    } else {
                        $_SESSION['user_id'] = (int)$user['userid'];
                        $_SESSION['username'] = (string)$user['username'];
                        $_SESSION['usergroupid'] = (int)$user['usergroupid'];
                        redirectTo('?page=download');
                    }
                }
            } catch (Throwable $e) {
                $errors[] = 'Sign in failed. Please try again later.';
                error_log('Login error: ' . $e->getMessage());
            }
        }
    }

    if ($action === 'forgot-password') {
        $page = 'forgot-password';
        $email = trim((string)($_POST['email'] ?? ''));

        if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
            $errors[] = 'Enter a valid email address.';
        }

        if (requireConfiguredOrFail($configMissing, $errors) && empty($errors)) {
            try {
                $pdo = pdoFromConfig($config);
                ensurePasswordResetTable($pdo);

                $stmt = $pdo->prepare(
                    'SELECT userid, username
                     FROM user
                     WHERE email = :email
                       AND usergroupid = :active_group
                     LIMIT 1'
                );
                $stmt->execute([
                    'email' => $email,
                    'active_group' => ACTIVE_USERGROUP_ID,
                ]);
                $user = $stmt->fetch();

                if ($user) {
                    $resetToken = bin2hex(random_bytes(32));
                    $resetTokenHash = hash('sha256', $resetToken);

                    $tokenInsert = $pdo->prepare(
                        'INSERT INTO user_password_reset_tokens (user_id, token_hash, expires_at, created_at)
                         VALUES (:user_id, :token_hash, DATE_ADD(NOW(), INTERVAL 1 HOUR), NOW())'
                    );
                    $tokenInsert->execute([
                        'user_id' => (int)$user['userid'],
                        'token_hash' => $resetTokenHash,
                    ]);

                    $resetUrl = buildAppUrl($config, '?page=reset-password&token=' . urlencode($resetToken));
                    sendBrevoEmail(
                        $config,
                        $email,
                        (string)$user['username'],
                        'Reset your Dodian password',
                        '<p>Hi ' . htmlspecialchars((string)$user['username'], ENT_QUOTES, 'UTF-8') . ',</p>'
                        . '<p>We received a request to reset your password.</p>'
                        . '<p><a href="' . htmlspecialchars($resetUrl, ENT_QUOTES, 'UTF-8') . '">Reset password</a></p>'
                        . '<p>This link is valid for 1 hour. If this was not you, you can ignore this email.</p>'
                    );
                }

                $successMessage = 'If this email belongs to an active account, a reset link has been sent.';
            } catch (Throwable $e) {
                $errors[] = 'Could not send reset email right now. Please try again later.';
                error_log('Forgot password error: ' . $e->getMessage());
            }
        }
    }

    if ($action === 'reset-password') {
        $page = 'reset-password';
        $token = trim((string)($_POST['token'] ?? ''));
        $password = (string)($_POST['password'] ?? '');
        $confirmPassword = (string)($_POST['confirm_password'] ?? '');

        if (!preg_match('/^[a-f0-9]{64}$/', $token)) {
            $errors[] = 'Reset link is invalid.';
        }

        if (strlen($password) < 8) {
            $errors[] = 'Password must be at least 8 characters long.';
        }

        if ($password !== $confirmPassword) {
            $errors[] = 'Passwords do not match.';
        }

        if (requireConfiguredOrFail($configMissing, $errors) && empty($errors)) {
            try {
                $pdo = pdoFromConfig($config);
                ensurePasswordResetTable($pdo);

                $tokenHash = hash('sha256', $token);
                $lookup = $pdo->prepare(
                    'SELECT t.id, t.user_id
                     FROM user_password_reset_tokens t
                     INNER JOIN user u ON u.userid = t.user_id
                     WHERE t.token_hash = :token_hash
                       AND t.consumed_at IS NULL
                       AND t.expires_at >= NOW()
                       AND u.usergroupid = :active_group
                     LIMIT 1'
                );
                $lookup->execute([
                    'token_hash' => $tokenHash,
                    'active_group' => ACTIVE_USERGROUP_ID,
                ]);
                $row = $lookup->fetch();

                if (!$row) {
                    $errors[] = 'Reset link is invalid or expired.';
                } else {
                    $salt = randomSalt(30);
                    $storedPassword = dodianPasswordHash($password, $salt);

                    $pdo->beginTransaction();

                    $updatePassword = $pdo->prepare(
                        'UPDATE user
                         SET password = :password,
                             salt = :salt,
                             passworddate = :passworddate
                         WHERE userid = :userid'
                    );
                    $updatePassword->execute([
                        'password' => $storedPassword,
                        'salt' => $salt,
                        'passworddate' => date('Y-m-d H:i:s'),
                        'userid' => (int)$row['user_id'],
                    ]);

                    $consumeToken = $pdo->prepare(
                        'UPDATE user_password_reset_tokens
                         SET consumed_at = NOW()
                         WHERE id = :id'
                    );
                    $consumeToken->execute(['id' => (int)$row['id']]);

                    $pdo->commit();
                    $successMessage = 'Password updated. You can now sign in.';
                    $page = 'login';
                }
            } catch (Throwable $e) {
                if (isset($pdo) && $pdo instanceof PDO && $pdo->inTransaction()) {
                    $pdo->rollBack();
                }
                $errors[] = 'Could not reset password right now. Please try again later.';
                error_log('Reset password error: ' . $e->getMessage());
            }
        }
    }
    if ($action === 'change-password') {
        $page = 'change-password';

        if (!isset($_SESSION['user_id'])) {
            $errors[] = 'Please sign in first.';
        }

        $currentPassword = (string)($_POST['current_password'] ?? '');
        $newPassword = (string)($_POST['new_password'] ?? '');
        $confirmPassword = (string)($_POST['confirm_password'] ?? '');

        if ($currentPassword === '' || $newPassword === '' || $confirmPassword === '') {
            $errors[] = 'Fill in all password fields.';
        }

        if (strlen($newPassword) < 8) {
            $errors[] = 'New password must be at least 8 characters long.';
        }

        if ($newPassword !== $confirmPassword) {
            $errors[] = 'New passwords do not match.';
        }

        if (requireConfiguredOrFail($configMissing, $errors) && empty($errors)) {
            try {
                $pdo = pdoFromConfig($config);

                $lookup = $pdo->prepare(
                    'SELECT userid, password, salt, usergroupid
                     FROM user
                     WHERE userid = :userid
                     LIMIT 1'
                );
                $lookup->execute(['userid' => (int)$_SESSION['user_id']]);
                $user = $lookup->fetch();

                if (!$user) {
                    $errors[] = 'Account not found.';
                } elseif ((int)$user['usergroupid'] !== ACTIVE_USERGROUP_ID) {
                    $errors[] = 'Only active accounts can change passwords.';
                } else {
                    $expectedPassword = dodianPasswordHash($currentPassword, (string)$user['salt']);
                    if (!hash_equals((string)$user['password'], $expectedPassword)) {
                        $errors[] = 'Current password is incorrect.';
                    }
                }

                if (empty($errors)) {
                    $salt = randomSalt(30);
                    $storedPassword = dodianPasswordHash($newPassword, $salt);

                    $update = $pdo->prepare(
                        'UPDATE user
                         SET password = :password,
                             salt = :salt,
                             passworddate = :passworddate
                         WHERE userid = :userid'
                    );
                    $update->execute([
                        'password' => $storedPassword,
                        'salt' => $salt,
                        'passworddate' => date('Y-m-d H:i:s'),
                        'userid' => (int)$user['userid'],
                    ]);

                    $successMessage = 'Password changed successfully.';
                    $page = 'download';
                }
            } catch (Throwable $e) {
                $errors[] = 'Could not change password right now. Please try again later.';
                error_log('Change password error: ' . $e->getMessage());
            }
        }
    }

    if ($action === 'admin-update-role') {
        $page = 'admin-users';

        if (!isset($_SESSION['user_id'])) {
            $errors[] = 'Please sign in first.';
            $page = 'login';
        } elseif (requireConfiguredOrFail($configMissing, $errors)) {
            $userId = isset($_POST['user_id']) ? (int)$_POST['user_id'] : 0;
            $roleKey = isset($_POST['role_key']) ? trim((string)$_POST['role_key']) : '';

            if ($userId <= 0 || $roleKey === '') {
                $errors[] = 'Select a user and role first.';
            }

            try {
                $pdo = pdoFromConfig($config);
                $actorGroupId = resolveCurrentUserGroupId($pdo, (int)$_SESSION['user_id'], isset($_SESSION['usergroupid']) ? (int)$_SESSION['usergroupid'] : null);
                if (!canAccessAdminPanel($actorGroupId)) {
                    $fallbackGroupId = resolveUserGroupIdByUsername($pdo, isset($_SESSION['username']) ? (string)$_SESSION['username'] : null);
                    if ($fallbackGroupId !== null) {
                        $actorGroupId = $fallbackGroupId;
                    }
                }

                if ($actorGroupId !== null) {
                    $_SESSION['usergroupid'] = $actorGroupId;
                    $currentSessionUserGroupId = $actorGroupId;
                }

                if (!canAccessAdminPanel($actorGroupId)) {
                    $errors[] = 'You do not have access to user management. Current detected usergroup: ' . (string)($actorGroupId ?? 'unknown') . '.';
                }

                if (empty($errors)) {
                    applyWebRoleToAccount($pdo, $userId, $roleKey);

                    try {
                        $targetLookup = $pdo->prepare('SELECT u.usergroupid, COALESCE(c.unbantime, 0) AS unbantime FROM user u LEFT JOIN game_characters c ON c.id = u.userid WHERE u.userid = :userid LIMIT 1');
                        $targetLookup->execute(['userid' => $userId]);
                        $targetRow = $targetLookup->fetch();
                    } catch (Throwable $targetJoinError) {
                        error_log('Target lookup fallback without game_characters: ' . $targetJoinError->getMessage());
                        $targetLookup = $pdo->prepare('SELECT u.usergroupid, 0 AS unbantime FROM user u WHERE u.userid = :userid LIMIT 1');
                        $targetLookup->execute(['userid' => $userId]);
                        $targetRow = $targetLookup->fetch();
                    }
                    if ($targetRow) {
                        syncDiscordRolesForUser($pdo, $config, $userId, (int)$targetRow['usergroupid'], (int)$targetRow['unbantime']);
                    }

                    $successMessage = 'Role updated for user #' . $userId . '.';
                }
            } catch (Throwable $e) {
                $errors[] = 'Could not update role right now. Please try again later.';
                error_log('Admin role update error: ' . $e->getMessage());
            }
        }
    }

}

if (($page === 'download' || $page === 'change-password' || $page === 'admin-users') && !isset($_SESSION['user_id'])) {
    $infoMessage = 'Please sign in first.';
    $page = 'login';
}

$currentSessionUserGroupId = isset($_SESSION['usergroupid']) ? (int)$_SESSION['usergroupid'] : null;
if (isset($_SESSION['user_id']) && requireConfiguredOrFail($configMissing, $errors)) {
    try {
        $pdo = pdoFromConfig($config);
        $currentSessionUserGroupId = resolveCurrentUserGroupId($pdo, (int)$_SESSION['user_id'], $currentSessionUserGroupId);
        if (!canAccessAdminPanel($currentSessionUserGroupId)) {
            $fallbackGroupId = resolveUserGroupIdByUsername($pdo, isset($_SESSION['username']) ? (string)$_SESSION['username'] : null);
            if ($fallbackGroupId !== null) {
                $currentSessionUserGroupId = $fallbackGroupId;
            }
        }

        if ($currentSessionUserGroupId !== null) {
            $_SESSION['usergroupid'] = $currentSessionUserGroupId;
        }

        syncPendingDiscordRoles($pdo, $config);
    } catch (Throwable $e) {
        error_log('Session refresh error: ' . $e->getMessage());
    }
}

$hasAdminPanelAccess = canAccessAdminPanel($currentSessionUserGroupId);
if (!$hasAdminPanelAccess && isset($_SESSION['usergroupid'])) {
    $hasAdminPanelAccess = canAccessAdminPanel((int)$_SESSION['usergroupid']);
    if ($hasAdminPanelAccess) {
        $currentSessionUserGroupId = (int)$_SESSION['usergroupid'];
    }
}

if ($page === 'admin-users' && !$hasAdminPanelAccess) {
    if (isset($_SESSION['user_id']) && requireConfiguredOrFail($configMissing, $errors)) {
        try {
            $pdo = isset($pdo) && $pdo instanceof PDO ? $pdo : pdoFromConfig($config);
            $currentSessionUserGroupId = resolveCurrentUserGroupId($pdo, (int)$_SESSION['user_id'], $currentSessionUserGroupId);
            if (!canAccessAdminPanel($currentSessionUserGroupId)) {
                $fallbackGroupId = resolveUserGroupIdByUsername($pdo, isset($_SESSION['username']) ? (string)$_SESSION['username'] : null);
                if ($fallbackGroupId !== null) {
                    $currentSessionUserGroupId = $fallbackGroupId;
                }
            }

            if ($currentSessionUserGroupId !== null) {
                $_SESSION['usergroupid'] = $currentSessionUserGroupId;
            }
        } catch (Throwable $e) {
            error_log('Admin access recheck error: ' . $e->getMessage());
        }
    }

    $hasAdminPanelAccess = canAccessAdminPanel($currentSessionUserGroupId);
    if (!$hasAdminPanelAccess) {
        $errors[] = 'You do not have access to user management.';
        $page = 'download';
    }
}

$resetTokenFromQuery = isset($_GET['token']) && is_string($_GET['token']) ? trim($_GET['token']) : '';

$discordLinkStatus = null;
if (isset($_SESSION['discord_link_username'])) {
    $discordLinkStatus = [
        'discord_username' => (string)$_SESSION['discord_link_username'],
        'last_synced_at' => (string)($_SESSION['discord_link_synced_at'] ?? ''),
    ];
}

$currentUserRoleDebug = null;
if (isset($_SESSION['user_id']) && requireConfiguredOrFail($configMissing, $errors)) {
    try {
        $pdo = isset($pdo) && $pdo instanceof PDO ? $pdo : pdoFromConfig($config);
        $roleLookup = $pdo->prepare('SELECT u.usergroupid, COALESCE(c.unbantime, 0) AS unbantime FROM user u LEFT JOIN game_characters c ON c.id = u.userid WHERE u.userid = :userid LIMIT 1');
        $roleLookup->execute(['userid' => (int)$_SESSION['user_id']]);
        $roleRow = $roleLookup->fetch();

        if ($roleRow) {
            $resolvedRoleKey = resolveRoleByUserData((int)$roleRow['usergroupid'], (int)$roleRow['unbantime']);
            $supportedRoles = getSupportedWebRoles();
            $resolvedRoleLabel = isset($supportedRoles[$resolvedRoleKey]) ? (string)$supportedRoles[$resolvedRoleKey]['label'] : ucfirst(str_replace('_', ' ', $resolvedRoleKey));

            $currentUserRoleDebug = [
                'usergroupid' => (int)$roleRow['usergroupid'],
                'role_key' => $resolvedRoleKey,
                'role_label' => $resolvedRoleLabel,
                'unbantime' => (int)$roleRow['unbantime'],
                'source' => 'database',
            ];
        }
    } catch (Throwable $e) {
        error_log('Current role debug lookup error: ' . $e->getMessage());
    }
}

if ($currentUserRoleDebug === null && isset($_SESSION['usergroupid'])) {
    $fallbackGroupId = (int)$_SESSION['usergroupid'];
    $fallbackRoleKey = findRoleKeyByUserGroupId($fallbackGroupId) ?? 'unknown';
    $supportedRoles = getSupportedWebRoles();
    $fallbackRoleLabel = isset($supportedRoles[$fallbackRoleKey]) ? (string)$supportedRoles[$fallbackRoleKey]['label'] : ucfirst(str_replace('_', ' ', $fallbackRoleKey));

    $currentUserRoleDebug = [
        'usergroupid' => $fallbackGroupId,
        'role_key' => $fallbackRoleKey,
        'role_label' => $fallbackRoleLabel,
        'unbantime' => 0,
        'source' => 'session',
    ];
}

$adminManageableUsers = [];
if ($page === 'admin-users' && $hasAdminPanelAccess && requireConfiguredOrFail($configMissing, $errors)) {
    try {
        $pdo = pdoFromConfig($config);

        try {
            $listUsers = $pdo->query(
                'SELECT u.userid, u.username, u.usergroupid, COALESCE(c.unbantime, 0) AS unbantime
                 FROM user u
                 LEFT JOIN game_characters c ON c.id = u.userid
                 ORDER BY u.userid DESC
                 LIMIT 200'
            );
        } catch (Throwable $joinError) {
            error_log('Admin users listing fallback without game_characters: ' . $joinError->getMessage());
            $listUsers = $pdo->query(
                'SELECT u.userid, u.username, u.usergroupid, 0 AS unbantime
                 FROM user u
                 ORDER BY u.userid DESC
                 LIMIT 200'
            );
        }

        while ($row = $listUsers->fetch()) {
            $adminManageableUsers[] = [
                'userid' => (int)$row['userid'],
                'username' => (string)$row['username'],
                'role_key' => resolveRoleByUserData((int)$row['usergroupid'], (int)$row['unbantime']),
            ];
        }
    } catch (Throwable $e) {
        $errors[] = 'Could not load users for the admin panel.';
        error_log('Admin users listing error: ' . $e->getMessage());
    }
}

?>
<!doctype html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dodian Account Portal</title>
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
        .card.admin-wide {
            max-width: 980px;
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
        button, .btn-link {
            margin-top: 18px;
            width: 100%;
            padding: 11px 14px;
            border: 0;
            border-radius: 8px;
            background: #22c55e;
            color: #06220f;
            font-weight: 700;
            cursor: pointer;
            text-decoration: none;
            display: inline-block;
            text-align: center;
            box-sizing: border-box;
        }
        .btn-link.secondary {
            background: #1e293b;
            color: #e2e8f0;
            margin-top: 10px;
        }
        .btn-link.discord {
            background: #5865f2;
            color: #ffffff;
            margin-top: 10px;
        }
        .errors, .success, .info {
            margin: 12px 0;
            padding: 10px 12px;
            border-radius: 8px;
            font-size: 0.95rem;
        }
        .errors { background: #7f1d1d; color: #fecaca; }
        .success { background: #14532d; color: #bbf7d0; }
        .info { background: #172554; color: #bfdbfe; }
        ul { margin: 0; padding-left: 20px; }
        .links {
            margin-top: 14px;
            display: grid;
            gap: 10px;
        }
        .downloads {
            margin-top: 14px;
            display: grid;
            gap: 10px;
        }

        .admin-list {
            margin-top: 14px;
            display: grid;
            gap: 10px;
            max-height: 560px;
            overflow-y: auto;
            padding-right: 4px;
        }
        .admin-user-card {
            border: 1px solid #334155;
            border-radius: 10px;
            padding: 14px;
            background: #0b1220;
            display: grid;
            grid-template-columns: minmax(220px, 1fr) minmax(240px, 1fr) 160px;
            gap: 10px;
            align-items: end;
        }
        .admin-user-row {
            font-size: 0.95rem;
            margin-bottom: 6px;
            color: #cbd5e1;
            font-weight: 600;
        }
        .admin-role-label {
            font-size: 0.8rem;
            color: #94a3b8;
            margin-bottom: 6px;
            display: block;
        }
        .admin-actions {
            display: flex;
            align-items: end;
        }
        .admin-actions button {
            margin-top: 0;
        }
        @media (max-width: 860px) {
            .card.admin-wide {
                max-width: 100%;
            }
            .admin-user-card {
                grid-template-columns: 1fr;
            }
        }
        select {
            width: 100%;
            padding: 10px 12px;
            border-radius: 8px;
            border: 1px solid #334155;
            background: #0b1220;
            color: #e2e8f0;
            box-sizing: border-box;
        }
    </style>
</head>
<body>
<div class="card<?= $page === 'admin-users' ? ' admin-wide' : '' ?>">
    <h1>Dodian account portal</h1>

    <?php if ($configMissing): ?>
        <div class="errors">
            Config is missing: copy <code>config.example.php</code> to <code>config.php</code> before account actions can be used.
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

    <?php if ($infoMessage !== null): ?>
        <div class="info"><?= htmlspecialchars($infoMessage, ENT_QUOTES, 'UTF-8') ?></div>
    <?php endif; ?>

    <?php if ($successMessage !== null): ?>
        <div class="success"><?= htmlspecialchars($successMessage, ENT_QUOTES, 'UTF-8') ?></div>
    <?php endif; ?>

    <?php if ($page === 'login'): ?>
        <p class="meta">Sign in with your active account. New here? Create an account first.</p>
        <form method="post" action="">
            <input type="hidden" name="action" value="login">
            <label for="login_username">Username</label>
            <input id="login_username" name="username" maxlength="12" required>

            <label for="login_password">Password</label>
            <input id="login_password" name="password" type="password" minlength="8" required>

            <button type="submit">Sign in</button>
        </form>
        <div class="links">
            <a class="btn-link secondary" href="?page=register">Create account</a>
            <a class="btn-link secondary" href="?page=forgot-password">Forgot password</a>
        </div>
    <?php endif; ?>

    <?php if ($page === 'register'): ?>
        <p class="meta">Create your account to access downloads after activation.</p>
        <form method="post" action="">
            <input type="hidden" name="action" value="register">
            <label for="register_username">Username</label>
            <input id="register_username" name="username" maxlength="12" required value="<?= htmlspecialchars((string)($_POST['username'] ?? ''), ENT_QUOTES, 'UTF-8') ?>">

            <label for="register_email">Email</label>
            <input id="register_email" name="email" type="email" required value="<?= htmlspecialchars((string)($_POST['email'] ?? ''), ENT_QUOTES, 'UTF-8') ?>">

            <label for="register_password">Password</label>
            <input id="register_password" name="password" type="password" minlength="8" required>

            <label for="register_confirm_password">Repeat password</label>
            <input id="register_confirm_password" name="confirm_password" type="password" minlength="8" required>

            <?php if ($turnstileSiteKey !== ''): ?>
                <div class="turnstile-wrap">
                    <div class="cf-turnstile" data-sitekey="<?= htmlspecialchars($turnstileSiteKey, ENT_QUOTES, 'UTF-8') ?>"></div>
                </div>
            <?php else: ?>
                <p class="turnstile-note">Turnstile is not configured yet. Add <code>turnstile.site_key</code> to config.php.</p>
            <?php endif; ?>

            <button type="submit">Create account</button>
        </form>
        <div class="links">
            <a class="btn-link secondary" href="?page=login">Back to sign in</a>
        </div>
    <?php endif; ?>

    <?php if ($page === 'forgot-password'): ?>
        <p class="meta">Enter your email address and we will send a password reset link.</p>
        <form method="post" action="">
            <input type="hidden" name="action" value="forgot-password">
            <label for="forgot_email">Email</label>
            <input id="forgot_email" name="email" type="email" required value="<?= htmlspecialchars((string)($_POST['email'] ?? ''), ENT_QUOTES, 'UTF-8') ?>">
            <button type="submit">Send reset link</button>
        </form>
        <div class="links">
            <a class="btn-link secondary" href="?page=login">Back to sign in</a>
        </div>
    <?php endif; ?>

    <?php if ($page === 'reset-password'): ?>
        <p class="meta">Choose a new password for your account.</p>
        <form method="post" action="">
            <input type="hidden" name="action" value="reset-password">
            <input type="hidden" name="token" value="<?= htmlspecialchars($resetTokenFromQuery, ENT_QUOTES, 'UTF-8') ?>">

            <label for="reset_password">New password</label>
            <input id="reset_password" name="password" type="password" minlength="8" required>

            <label for="reset_confirm_password">Repeat new password</label>
            <input id="reset_confirm_password" name="confirm_password" type="password" minlength="8" required>

            <button type="submit">Update password</button>
        </form>
        <div class="links">
            <a class="btn-link secondary" href="?page=login">Back to sign in</a>
        </div>
    <?php endif; ?>

    <?php if ($page === 'download'): ?>
        <p class="meta">Welcome, <?= htmlspecialchars((string)($_SESSION['username'] ?? 'Player'), ENT_QUOTES, 'UTF-8') ?>. You are signed in.</p>
        <?php if (is_array($currentUserRoleDebug)): ?>
            <p class="meta">Current role: <?= htmlspecialchars((string)$currentUserRoleDebug['role_label'], ENT_QUOTES, 'UTF-8') ?> (key: <?= htmlspecialchars((string)$currentUserRoleDebug['role_key'], ENT_QUOTES, 'UTF-8') ?>, usergroup: <?= (int)$currentUserRoleDebug['usergroupid'] ?>, source: <?= htmlspecialchars((string)$currentUserRoleDebug['source'], ENT_QUOTES, 'UTF-8') ?>)</p>
        <?php endif; ?>
        <div class="downloads">
            <a class="btn-link" href="<?= htmlspecialchars($clientJarUrl, ENT_QUOTES, 'UTF-8') ?>">Download game client</a>
            <a class="btn-link secondary" href="<?= htmlspecialchars($javaDownloadUrl, ENT_QUOTES, 'UTF-8') ?>" target="_blank" rel="noopener noreferrer">Download Java</a>
            <a class="btn-link discord" href="<?= htmlspecialchars($discordUrl, ENT_QUOTES, 'UTF-8') ?>" target="_blank" rel="noopener noreferrer">Join Discord</a>
            <a class="btn-link discord" href="?page=discord-link">Link Discord account</a>
            <?php if (is_array($discordLinkStatus)): ?>
                <p class="meta">Linked Discord: <?= htmlspecialchars((string)$discordLinkStatus['discord_username'], ENT_QUOTES, 'UTF-8') ?> (last sync <?= htmlspecialchars((string)$discordLinkStatus['last_synced_at'], ENT_QUOTES, 'UTF-8') ?>)</p>
            <?php endif; ?>
            <a class="btn-link secondary" href="?page=change-password">Change password</a>
            <?php if ($hasAdminPanelAccess): ?>
                <a class="btn-link secondary" href="?page=admin-users">User management</a>
            <?php endif; ?>
            <a class="btn-link secondary" href="?logout=1">Sign out</a>
        </div>
    <?php endif; ?>

    <?php if ($page === 'change-password'): ?>
        <p class="meta">Update your account password while signed in.</p>
        <form method="post" action="">
            <input type="hidden" name="action" value="change-password">

            <label for="current_password">Current password</label>
            <input id="current_password" name="current_password" type="password" minlength="8" required>

            <label for="new_password">New password</label>
            <input id="new_password" name="new_password" type="password" minlength="8" required>

            <label for="confirm_new_password">Repeat new password</label>
            <input id="confirm_new_password" name="confirm_password" type="password" minlength="8" required>

            <button type="submit">Change password</button>
        </form>
        <div class="links">
            <a class="btn-link secondary" href="?page=download">Back to downloads</a>
        </div>
    <?php endif; ?>

    <?php if ($page === 'admin-users'): ?>
        <p class="meta">User management: only visible for usergroups 6, 10, and 18.</p>
        <?php if (empty($adminManageableUsers)): ?>
            <p class="meta">No users found.</p>
        <?php endif; ?>
        <div class="admin-list">
            <?php foreach ($adminManageableUsers as $userRow): ?>
                <form method="post" action="" class="admin-user-card">
                    <input type="hidden" name="action" value="admin-update-role">
                    <input type="hidden" name="user_id" value="<?= (int)$userRow['userid'] ?>">

                    <div>
                        <div class="admin-user-row">#<?= (int)$userRow['userid'] ?> - <?= htmlspecialchars((string)$userRow['username'], ENT_QUOTES, 'UTF-8') ?></div>
                        <span class="admin-role-label">Current role: <?= htmlspecialchars((string)(getSupportedWebRoles()[$userRow['role_key']]['label'] ?? ucfirst(str_replace('_', ' ', (string)$userRow['role_key']))), ENT_QUOTES, 'UTF-8') ?></span>
                    </div>

                    <div>
                        <label class="admin-role-label" for="role_<?= (int)$userRow['userid'] ?>">Assign role</label>
                        <select id="role_<?= (int)$userRow['userid'] ?>" name="role_key" required>
                            <?php foreach (getSupportedWebRoles() as $roleKey => $roleDefinition): ?>
                                <option value="<?= htmlspecialchars((string)$roleKey, ENT_QUOTES, 'UTF-8') ?>" <?= $userRow['role_key'] === $roleKey ? 'selected' : '' ?>>
                                    <?= htmlspecialchars((string)$roleDefinition['label'], ENT_QUOTES, 'UTF-8') ?>
                                </option>
                            <?php endforeach; ?>
                        </select>
                    </div>

                    <div class="admin-actions">
                        <button type="submit">Update role</button>
                    </div>
                </form>
            <?php endforeach; ?>
        </div>
        <div class="links">
            <a class="btn-link secondary" href="?page=download">Back to downloads</a>
        </div>
    <?php endif; ?>
</div>
<?php if ($turnstileSiteKey !== '' && $page === 'register'): ?>
    <script src="https://challenges.cloudflare.com/turnstile/v0/api.js" async defer></script>
<?php endif; ?>
</body>
</html>
