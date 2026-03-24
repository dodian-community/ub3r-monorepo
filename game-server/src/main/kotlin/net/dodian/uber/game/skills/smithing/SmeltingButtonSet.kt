package net.dodian.uber.game.skills.smithing

data class SmeltingButtonSet(
    val displayName: String,
    val barId: Int,
    val oneButtonId: Int,
    val fiveButtonId: Int,
    val tenButtonId: Int,
    val xButtonId: Int,
) {
    fun toMappings(): List<FurnaceButtonMapping> = listOf(
        FurnaceButtonMapping(oneButtonId, barId, 1),
        FurnaceButtonMapping(fiveButtonId, barId, 5),
        FurnaceButtonMapping(tenButtonId, barId, 10),
        FurnaceButtonMapping(xButtonId, barId, 0),
    ).filter { it.buttonId > 0 }
}

data class NamedSmeltingButtonSet(
    val bronze: SmeltingButtonSet,
    val iron: SmeltingButtonSet,
    val silver: SmeltingButtonSet,
    val steel: SmeltingButtonSet,
    val gold: SmeltingButtonSet,
    val mithril: SmeltingButtonSet,
    val adamant: SmeltingButtonSet,
    val rune: SmeltingButtonSet,
) {
    val all: List<SmeltingButtonSet> = listOf(
        bronze,
        iron,
        silver,
        steel,
        gold,
        mithril,
        adamant,
        rune,
    )
}
