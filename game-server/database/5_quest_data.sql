-- Persisted quest engine progress: JSON blob of { stages: {questKey: stage}, attributes: {"questKey.name": value} }.
-- See PlayerQuestProgressState.kt / PlayerSaveSnapshot.kt / AccountLoginMapper.kt.
ALTER TABLE characters
    ADD COLUMN quest_data text NULL;
