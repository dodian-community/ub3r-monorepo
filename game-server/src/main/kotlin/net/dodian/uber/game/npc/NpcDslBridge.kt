package net.dodian.uber.game.npc

@Deprecated("Use net.dodian.uber.game.npc.npcPlugin as canonical import root")
fun npcPlugin(
    name: String,
    init: net.dodian.uber.game.content.npcs.NpcPluginBuilder.() -> Unit,
): net.dodian.uber.game.content.npcs.NpcPluginDefinition =
    net.dodian.uber.game.content.npcs.npcPlugin(name, init)

@Deprecated("Use net.dodian.uber.game.npc.simpleNpcDefinition as canonical import root")
fun simpleNpcDefinition(
    name: String,
    entries: List<NpcSpawnDef>,
): NpcContentDefinition = net.dodian.uber.game.content.npcs.simpleNpcDefinition(name, entries)

@Deprecated("Use net.dodian.uber.game.npc.legacyNpcDefinition as canonical import root")
fun legacyNpcDefinition(
    name: String,
    entries: List<NpcSpawnDef>,
    onFirstClick: NpcClickHandler = NO_CLICK_HANDLER,
    onSecondClick: NpcClickHandler = NO_CLICK_HANDLER,
    onThirdClick: NpcClickHandler = NO_CLICK_HANDLER,
    onFourthClick: NpcClickHandler = NO_CLICK_HANDLER,
    onAttack: NpcClickHandler = NO_CLICK_HANDLER,
): NpcContentDefinition =
    net.dodian.uber.game.content.npcs.legacyNpcDefinition(
        name = name,
        entries = entries,
        onFirstClick = onFirstClick,
        onSecondClick = onSecondClick,
        onThirdClick = onThirdClick,
        onFourthClick = onFourthClick,
        onAttack = onAttack,
    )
