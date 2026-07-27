package net.dodian.uber.game.npc

import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client

import net.dodian.uber.game.api.interaction.GameContentDsl
import net.dodian.uber.game.api.interaction.NpcInteractionContext

@GameContentDsl
class NpcOptionsBuilder internal constructor() {
    private val bindings = ArrayList<NpcOptionBinding>()

    fun first(label: String = "first", handler: NpcClickHandler) = bind(1, label, handler)
    fun first(label: String = "first", handler: (NpcInteractionContext) -> Boolean) = bindContext(1, label, handler)
    fun second(label: String = "second", handler: NpcClickHandler) = bind(2, label, handler)
    fun second(label: String = "second", handler: (NpcInteractionContext) -> Boolean) = bindContext(2, label, handler)
    fun third(label: String = "third", handler: NpcClickHandler) = bind(3, label, handler)
    fun third(label: String = "third", handler: (NpcInteractionContext) -> Boolean) = bindContext(3, label, handler)
    fun fourth(label: String = "fourth", handler: NpcClickHandler) = bind(4, label, handler)
    fun fourth(label: String = "fourth", handler: (NpcInteractionContext) -> Boolean) = bindContext(4, label, handler)
    fun attack(label: String = "attack", handler: NpcClickHandler) = bind(5, label, handler)
    fun attack(label: String = "attack", handler: (NpcInteractionContext) -> Boolean) = bindContext(5, label, handler)

    fun talkTo(label: String = "talk-to", handler: NpcClickHandler) = first(label, handler)
    fun talkTo(label: String = "talk-to", handler: (NpcInteractionContext) -> Boolean) = first(label, handler)
    fun trade(label: String = "trade", handler: NpcClickHandler) = second(label, handler)
    fun trade(label: String = "trade", handler: (NpcInteractionContext) -> Boolean) = second(label, handler)

    internal fun build(): List<NpcOptionBinding> = bindings.toList()

    private fun bind(option: Int, label: String, handler: NpcClickHandler) {
        require(bindings.none { it.option == option }) { "Duplicate NPC option $option" }
        bindings += NpcOptionBinding(option, label, handler = handler)
    }

    private fun bindContext(option: Int, label: String, handler: NpcContextClickHandler) {
        require(bindings.none { it.option == option }) { "Duplicate NPC option $option" }
        bindings += NpcOptionBinding(option, label, contextHandler = handler)
    }
}

data class NpcOptionBinding(
    val option: Int,
    val label: String,
    val handler: NpcClickHandler = NO_CLICK_HANDLER,
    val contextHandler: NpcContextClickHandler = NO_CONTEXT_CLICK_HANDLER,
)

@GameContentDsl
class NpcSpawnsBuilder internal constructor(private val primaryId: Int) {
    private val values = ArrayList<NpcSpawnDef>()

    fun at(
        x: Int,
        y: Int,
        z: Int = 0,
        block: NpcSpawnOverrideBuilder.() -> Unit = {},
    ) {
        spawn(x, y, z = z, block = block)
    }

    fun atId(
        npcId: Int,
        x: Int,
        y: Int,
        z: Int = 0,
        block: NpcSpawnOverrideBuilder.() -> Unit = {},
    ) {
        spawnId(npcId, x, y, z = z, block = block)
    }

    fun spawn(
        x: Int,
        y: Int,
        z: Int = 0,
        face: Int = NORTH,
        walkRadius: Int = 0,
        leashDistance: Int = 15,
        profile: NpcProfile? = null,
        hitpoints: Int? = null,
        attack: Int? = null,
        defence: Int? = null,
        strength: Int? = null,
        ranged: Int? = null,
        magic: Int? = null,
        respawnTicks: Int? = null,
        attackAnimation: Int? = null,
        defenceAnimation: Int? = null,
        deathAnimation: Int? = null,
        headIcon: Int? = null,
        transformTo: Int? = null,
        attackRange: Int = 0,
        alwaysActive: Boolean = false,
        block: NpcSpawnOverrideBuilder.() -> Unit = {},
    ) {
        spawnId(
            primaryId,
            x,
            y,
            z,
            face,
            walkRadius,
            leashDistance,
            profile,
            hitpoints,
            attack,
            defence,
            strength,
            ranged,
            magic,
            respawnTicks,
            attackAnimation,
            defenceAnimation,
            deathAnimation,
            headIcon,
            transformTo,
            attackRange,
            alwaysActive,
            block,
        )
    }

    fun spawnId(
        npcId: Int,
        x: Int,
        y: Int,
        z: Int = 0,
        face: Int = NORTH,
        walkRadius: Int = 0,
        leashDistance: Int = 15,
        profile: NpcProfile? = null,
        hitpoints: Int? = null,
        attack: Int? = null,
        defence: Int? = null,
        strength: Int? = null,
        ranged: Int? = null,
        magic: Int? = null,
        respawnTicks: Int? = null,
        attackAnimation: Int? = null,
        defenceAnimation: Int? = null,
        deathAnimation: Int? = null,
        headIcon: Int? = null,
        transformTo: Int? = null,
        attackRange: Int = 0,
        alwaysActive: Boolean = false,
        block: NpcSpawnOverrideBuilder.() -> Unit = {},
    ) {
        val base =
            NpcSpawnDef(
                npcId = npcId,
                x = x,
                y = y,
                z = z,
                face = face,
                walkRadius = walkRadius,
                leashDistance = leashDistance,
                attackRange = attackRange,
                alwaysActive = alwaysActive,
                overrides = NpcServerPatch(
                    attackAnimation = attackAnimation,
                    defenceAnimation = defenceAnimation,
                    deathAnimation = deathAnimation,
                    respawnTicks = respawnTicks,
                    attack = attack,
                    defence = defence,
                    strength = strength,
                    hitpoints = hitpoints,
                    ranged = ranged,
                    magic = magic,
                    headIcon = headIcon,
                    transformTo = transformTo,
                ),
                profile = profile?.key,
            )
        values += NpcSpawnOverrideBuilder(base).apply(block).build()
    }

    internal fun build(): List<NpcSpawnDef> = values.toList()
}

@GameContentDsl
class NpcFamilyBuilder internal constructor(
    private val name: String,
    private val primaryId: Int,
) {
    private val ids = linkedSetOf(primaryId)
    private val profiles = linkedSetOf<String>()
    private val cacheOverrides = ArrayList<NpcCacheOverride>()
    private val serverDefinitions = ArrayList<NpcServerDefinition>()
    private var options = emptyList<NpcOptionBinding>()
    private var spawns = emptyList<NpcSpawnDef>()
    private var combatProfile: NpcCombatProfile? = null

    fun ids(vararg values: Int) {
        values.forEach { ids += it }
    }

    fun profiles(vararg values: String) {
        values.mapNotNullTo(profiles) { it.trim().takeIf(String::isNotEmpty) }
    }

    fun profile(value: String): NpcProfile =
        net.dodian.uber.game.npc.profile(value).also { profiles += it.key }

    fun options(block: NpcOptionsBuilder.() -> Unit) {
        options = NpcOptionsBuilder().apply(block).build()
    }

    fun definition(block: NpcDefinitionOverrideBuilder.() -> Unit) {
        definition(primaryId, block)
    }

    fun definition(id: Int, block: NpcDefinitionOverrideBuilder.() -> Unit) {
        ids += id
        cacheOverrides += NpcDefinitionOverrideBuilder(id).apply(block).build()
    }

    fun server(block: NpcServerDefinitionBuilder.() -> Unit) {
        server(primaryId, block)
    }

    fun server(id: Int, block: NpcServerDefinitionBuilder.() -> Unit) {
        ids += id
        serverDefinitions += NpcServerDefinitionBuilder(id).apply(block).buildDefinition()
    }

    fun combat(block: NpcCombatBuilder.() -> Unit) {
        combatProfile = NpcCombatBuilder().apply(block).build()
    }

    fun spawns(block: NpcSpawnsBuilder.() -> Unit) {
        spawns = NpcSpawnsBuilder(primaryId).apply(block).build()
        ids += spawns.map { it.npcId }
    }

    internal fun build(): NpcFamily {
        val finalIds = ids.toIntArray()
        combatProfile?.let { NpcCombatRegistry.registerFamily(name, finalIds, it) }
        val optionLabels = options.associate { it.option to it.label }
        fun handler(option: Int): NpcClickHandler =
            options.firstOrNull { it.option == option }?.handler ?: NO_CLICK_HANDLER
        fun contextHandler(option: Int): NpcContextClickHandler =
            options.firstOrNull { it.option == option }?.contextHandler ?: NO_CONTEXT_CLICK_HANDLER
        val content =
            NpcContentDefinition(
                name = name,
                npcIds = finalIds,
                profiles = profiles.toSet(),
                optionLabels = optionLabels,
                onFirstClick = handler(1),
                onSecondClick = handler(2),
                onThirdClick = handler(3),
                onFourthClick = handler(4),
                onAttack = handler(5),
                onFirstClickCtx = contextHandler(1),
                onSecondClickCtx = contextHandler(2),
                onThirdClickCtx = contextHandler(3),
                onFourthClickCtx = contextHandler(4),
                onAttackCtx = contextHandler(5),
                cacheOverrides = cacheOverrides.toList(),
                serverDefinitions = serverDefinitions.map { def ->
                    if (combatProfile != null) def.copy(bossAttackHandler = combatProfile?.attack) else def
                },
            )
        return DefaultNpcFamily(name, primaryId, finalIds, content, spawns, cacheOverrides, serverDefinitions)
    }
}

private data class DefaultNpcFamily(
    override val familyName: String,
    override val primaryId: Int,
    override val ids: IntArray,
    override val definition: NpcContentDefinition,
    override val spawns: List<NpcSpawnDef>,
    override val cacheOverrides: List<NpcCacheOverride>,
    override val serverDefinitions: List<NpcServerDefinition>,
) : NpcFamily

fun npcFamily(name: String, primaryId: Int, block: NpcFamilyBuilder.() -> Unit): NpcFamily =
    NpcFamilyBuilder(name, primaryId).apply(block).build()

fun noNpcClick(@Suppress("UNUSED_PARAMETER") client: Client, @Suppress("UNUSED_PARAMETER") npc: Npc): Boolean = false
