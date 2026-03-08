package net.dodian.uber.game.content.npc

class NpcContentBuilder(
    private val name: String,
) {
    var npcIds: IntArray = intArrayOf()
    var ownsSpawnDefinitions: Boolean = false
    var entries: List<NpcSpawnDef> = emptyList()
    var onFirstClick: NpcClickHandler = NO_CLICK_HANDLER
    var onSecondClick: NpcClickHandler = NO_CLICK_HANDLER
    var onThirdClick: NpcClickHandler = NO_CLICK_HANDLER
    var onFourthClick: NpcClickHandler = NO_CLICK_HANDLER
    var onAttack: NpcClickHandler = NO_CLICK_HANDLER

    fun build(): NpcContentDefinition {
        return NpcContentDefinition(
            name = name,
            npcIds = npcIds,
            ownsSpawnDefinitions = ownsSpawnDefinitions,
            entries = entries,
            onFirstClick = onFirstClick,
            onSecondClick = onSecondClick,
            onThirdClick = onThirdClick,
            onFourthClick = onFourthClick,
            onAttack = onAttack,
        )
    }
}

fun npcContent(name: String, init: NpcContentBuilder.() -> Unit): NpcContentDefinition {
    return NpcContentBuilder(name).apply(init).build()
}
