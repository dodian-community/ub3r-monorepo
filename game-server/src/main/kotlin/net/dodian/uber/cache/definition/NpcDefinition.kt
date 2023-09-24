package net.dodian.uber.cache.definition

class NpcDefinition(
    override val id: Int,
    override val name: String,
    override val examine: String = "I don't know anything about this NPC...",
    val size: Int = 1
) : GameDefinition