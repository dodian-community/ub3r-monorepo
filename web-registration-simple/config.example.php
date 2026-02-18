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
    ],
    'brevo' => [
        'api_key' => 'xkeysib-REPLACE_ME',
        'sender_email' => 'noreply@example.com',
        'sender_name' => 'Dodian',
    ],
    'turnstile' => [
        'site_key' => '0x4AAAAA-REPLACE_ME',
        'secret_key' => '0x4AAAAA-REPLACE_ME',
    ],
];
