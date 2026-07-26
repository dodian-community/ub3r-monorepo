-- Records the Discord identity captured during Discord-gated account creation.
-- discord_id is the stable key (Discord usernames can change); discord_username is
-- the display value, set once at creation. See AccountLoginService.kt / DiscordApiService.kt.
ALTER TABLE user
    ADD COLUMN discord_id varchar(32) NOT NULL DEFAULT '',
    ADD COLUMN discord_username varchar(128) NOT NULL DEFAULT '';
