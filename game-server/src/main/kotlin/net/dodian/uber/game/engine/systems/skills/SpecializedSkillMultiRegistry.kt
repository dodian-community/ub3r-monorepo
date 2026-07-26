package net.dodian.uber.game.engine.systems.skills

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.skills.api.SkillMultiConfig

/** Maps module-declared presentation keys to established 317 interfaces. */
object SpecializedSkillMultiRegistry {
    private val renderers = linkedMapOf<String, (Client, SkillMultiConfig) -> Unit>()

    init {
        register("makeall") { client, config ->
            val entries = config.entries
            client.sendString(config.title, 35101)
            val modelComponents = intArrayOf(35307, 35311, 35315)
            for (i in 0 until 3) {
                val entry = entries.getOrNull(i)
                val itemId = entry?.recipe?.outputItemId ?: -1
                client.sendInterfaceModel(modelComponents[i], 100, itemId)
            }
            client.sendChatboxInterface(35100)
        }
    }

    fun register(key: String, renderer: (Client, SkillMultiConfig) -> Unit) {
        require(key.isNotBlank())
        require(renderers.putIfAbsent(key, renderer) == null) { "Duplicate specialized skill presentation: $key" }
    }

    fun render(client: Client, config: SkillMultiConfig): Boolean {
        val renderer = config.presentationKey?.let(renderers::get) ?: return false
        renderer(client, config)
        return true
    }
}
