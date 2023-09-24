package net.dodian.uber.cache.definition

import net.dodian.definitions

@Suppress("MemberVisibilityCanBePrivate")
data class ItemDefinition(
    override val id: Int,
    override val name: String,
    override val examine: String = "Not sure what this item is...",
    val actionsInventory: List<String> = listOf(),
    val actionsGround: List<String> = listOf(),
    val members: Boolean = false,
    val noteGraphicId: Int = -1,
    val noteInfoId: Int = -1,
    val canStack: Boolean = false,
    val team: Int = -1
) : GameDefinition {
    val isNote: Boolean get() = noteGraphicId != -1 && noteInfoId != -1

    private fun toNote() {
        if (!isNote) return
        if (examine.startsWith("Swap this note at any bank for")) return
    }

    companion object {
        fun byId(id: Int) = definitions.typeById(ItemDefinition::class, id)
            ?: error("No item definition for ID: $id")
    }
}