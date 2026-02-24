<?php

declare(strict_types=1);

return [
    'db' => [
        'host' => '127.0.0.1',
        'port' => 3306,
        'name' => 'dodiannet',
        'user' => 'dodian_game',
        'password' => 'abcd1234',
        'charset' => 'utf8mb4',
    ],
    'app' => [
        'base_url' => 'http://localhost:8080',
        'client_jar_url' => 'https://example.com/downloads/dodian-client.jar',
        'java_download_url' => 'https://www.java.com/download/',
        'discord_url' => 'https://discord.gg/your-server',
        'blocked_email_domains' => [
            'mailinator.com',
            '10minutemail.com',
        ],
    ],
    'brevo' => [
        'api_key' => 'xkeysib-REPLACE_ME',
        'sender_email' => 'noreply@example.com',
        'sender_name' => 'Dodian',
    ],
    'discord' => [
        'client_id' => '123456789012345678',
        'client_secret' => 'REPLACE_ME',
        'redirect_uri' => 'http://localhost:8080/?page=discord-link',
        'guild_id' => '123456789012345678',
        'bot_token' => 'REPLACE_ME',
        'verified_role_id' => '123456789012345678',
        'premium_role_id' => '123456789012345678',
        'moderator_role_id' => '123456789012345678',
        'trial_moderator_role_id' => '123456789012345678',
        'administrator_role_id' => '123456789012345678',
        'developer_role_id' => '123456789012345678',
        'banned_role_id' => '123456789012345678',
    ],
    'turnstile' => [
        'site_key' => '0x4AAAAA-REPLACE_ME',
        'secret_key' => '0x4AAAAA-REPLACE_ME',
    ],
];
