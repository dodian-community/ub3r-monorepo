package net.dodian.uber.cache.definition

import kotlin.reflect.KClass

@Suppress("MemberVisibilityCanBePrivate", "unused")
class GameDefinitions(
    private val definitions: MutableMap<KClass<GameDefinition>, MutableMap<Int, GameDefinition>> = mutableMapOf()
) : MutableMap<KClass<GameDefinition>, MutableMap<Int, GameDefinition>> by definitions {

    fun definitions(type: KClass<out GameDefinition>) = definitions[type]
        ?: error("No definitions for type: ${type.simpleName}")

    @Suppress("UNCHECKED_CAST")
    fun <T : GameDefinition> typeById(type: KClass<T>, id: Int) = definitions(type)[id] as T?
}