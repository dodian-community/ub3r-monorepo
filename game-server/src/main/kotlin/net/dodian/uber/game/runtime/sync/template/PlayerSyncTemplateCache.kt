package net.dodian.uber.game.runtime.sync.template

import java.util.HashMap

class PlayerSyncTemplateCache {
    private val templates = HashMap<PlayerSyncTemplateKey, PlayerSyncTemplate>()

    fun get(key: PlayerSyncTemplateKey): PlayerSyncTemplate? = templates[key]

    fun put(key: PlayerSyncTemplateKey, template: PlayerSyncTemplate) {
        templates[key] = template
    }
}
