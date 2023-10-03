package net.dodian.uber.cache.definition

@Suppress("MemberVisibilityCanBePrivate", "unused")
class GameDefinitions(
    private val definitions: MutableMap<Class<GameDefinition>, MutableMap<Int, GameDefinition>> = mutableMapOf()
) : MutableMap<Class<GameDefinition>, MutableMap<Int, GameDefinition>> by definitions {

    fun definitions(type: Class<out GameDefinition>) = definitions[type]
        ?: error("No definitions for type: ${type.simpleName}")

    @Suppress("UNCHECKED_CAST")
    fun <T : GameDefinition> typeById(type: Class<T>, id: Int) = definitions(type)[id] as T?
}