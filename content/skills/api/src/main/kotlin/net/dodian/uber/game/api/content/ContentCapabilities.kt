package net.dodian.uber.game.api.content

/**
 * Typed, plugin-owned session state. Values belong to the player's content
 * runtime state and are discarded when that player logs out.
 */
data class ContentAttributeKey<T : Any>(val owner: String, val name: String) {
    init {
        require(owner.isNotBlank()) { "Content attribute owner cannot be blank." }
        require(name.isNotBlank()) { "Content attribute name cannot be blank." }
    }

    val id: String get() = "$owner:$name"
}

interface ContentAttributes {
    fun <T : Any> get(key: ContentAttributeKey<T>): T?
    fun <T : Any> put(key: ContentAttributeKey<T>, value: T)
    fun remove(key: ContentAttributeKey<*>)
}

/** Engine-provided state variables for content that needs client configuration. */
interface ContentVariables {
    fun varbit(id: Int, value: Int)
}

/** Small dialogue surface; richer dialogue flows remain plugin-owned. */
interface ContentDialogue {
    fun message(text: String)
    fun npc(dialogueId: Int, npcId: Int)
    fun close()
}
