package net.dodian.uber.game.npc

fun simpleNpcDefinition(
    name: String,
    entries: List<NpcSpawnDef>,
): NpcContentDefinition =
    npcPlugin(name) {
        ids(*npcIdsFromEntries(entries))
        spawns(entries)
    }.toContentDefinition(name, false)

fun legacyNpcDefinition(
    name: String,
    entries: List<NpcSpawnDef>,
    onFirstClick: NpcClickHandler = NO_CLICK_HANDLER,
    onSecondClick: NpcClickHandler = NO_CLICK_HANDLER,
    onThirdClick: NpcClickHandler = NO_CLICK_HANDLER,
    onFourthClick: NpcClickHandler = NO_CLICK_HANDLER,
    onAttack: NpcClickHandler = NO_CLICK_HANDLER,
): NpcContentDefinition {
    val plugin =
        npcPlugin(name) {
            ids(*npcIdsFromEntries(entries))
            spawns(entries)
            options {
                if (onFirstClick !== NO_CLICK_HANDLER) {
                    first("first", onFirstClick)
                }
                if (onSecondClick !== NO_CLICK_HANDLER) {
                    second("second", onSecondClick)
                }
                if (onThirdClick !== NO_CLICK_HANDLER) {
                    third("third", onThirdClick)
                }
                if (onFourthClick !== NO_CLICK_HANDLER) {
                    fourth("fourth", onFourthClick)
                }
                if (onAttack !== NO_CLICK_HANDLER) {
                    attack("attack", onAttack)
                }
            }
        }
    return plugin.toContentDefinition(name, false)
}
