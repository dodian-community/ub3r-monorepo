package net.dodian.uber.skills.thieving

import net.dodian.uber.game.api.content.ContentAttributeKey
import net.dodian.uber.game.api.plugin.ContentMaturity
import net.dodian.uber.game.api.plugin.ContentModuleManifest
import net.dodian.uber.game.api.plugin.skills.SkillEquipmentSlot
import net.dodian.uber.game.api.plugin.skills.SkillNpcInteraction
import net.dodian.uber.game.api.plugin.skills.SkillObjectInteraction
import net.dodian.uber.game.api.plugin.skills.SkillPlayer
import net.dodian.uber.game.api.plugin.skills.SkillPlugin
import net.dodian.uber.game.api.plugin.skills.SkillPluginDefinition
import net.dodian.uber.game.api.plugin.skills.SkillPosition
import net.dodian.uber.game.api.plugin.skills.manifest
import net.dodian.uber.game.api.plugin.skills.skillPlugin
import net.dodian.uber.game.engine.systems.action.PolicyPreset
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.skills.api.SkillModuleDescriptor
import net.dodian.uber.game.api.plugin.runtime.TomlRecordReader

data class ThievingTargetDef(
    val name: String,
    val targetType: String,
    val id: Int,
    val level: Int,
    val experience: Int,
    /** Parallel weighted-drop table: at [lootChances][i] (cumulative, out of 100), grant [lootItems][i]. */
    val lootItems: IntArray,
    val lootChances: IntArray,
    val lootMinAmounts: IntArray,
    val lootMaxAmounts: IntArray,
    /** Ticks the object stays swapped to the empty-stall id after a successful "stall" steal; 0 = no swap (npc/chest/market_stall targets). */
    val respawnTicks: Int,
)

object ThievingModule : SkillPlugin {
    val descriptor = SkillModuleDescriptor("skill.thieving", "Thieving")
    val targets: List<ThievingTargetDef> by lazy { loadTargets() }

    // Pyramid Plunder's 6 tomb obstacles (2 special-cased: gold chest, sarcophagus) + 15 urn
    // obstacles - the confirmed, fully-understood parts of the room state machine (legacy
    // PyramidPlunder.toggleObstacle/openDoor). 4 of the 6 tomb ids double as the room's exit
    // door once solved (nextRoomDoorFor = nextRoom[room] + 26618, landing on 26618-26621).
    private val tombObjectIds = intArrayOf(26616, 26626, 26618, 26619, 26620, 26621)
    private val urnObjectIds = intArrayOf(
        26580, 26600, 26601, 26602, 26603, 26604, 26605, 26606, 26607, 26608, 26609, 26610, 26611, 26612, 26613,
    )
    // Remaining plunder-room objects (20275, 20277, 20932, 26622-26625) have no confirmed
    // real-world behavior recovered from the dead legacy code (Thieving.kt never dispatched to
    // them either) - left as an honest placeholder rather than fabricated behavior, same as the
    // rest of this module's documented approach. 20931 (the exit-confirmation dialogue id) used
    // to be listed here too, but it's not an object id at all - it's implemented below via
    // confirmDialogue, ported from the legacy PyramidPlunderDialogueModule/OptionHandler.
    private val plunderPlaceholderObjectIds = intArrayOf(20275, 20277, 20932, 26622, 26623, 26624, 26625)
    private const val PYRAMID_EXIT_DIALOGUE_ID = 20931
    private val plunderObjectIds = tombObjectIds + urnObjectIds + plunderPlaceholderObjectIds

    private val npcIds by lazy { targets.filter { it.targetType == "npc" }.map { it.id }.toIntArray() }

    private val stallIds by lazy { targets.filter { it.targetType == "stall" }.map { it.id }.toIntArray() }
    private val cageIds = intArrayOf(20873)

    // Object 375 is reused at two unrelated real-world locations for two distinct one-off reward
    // chests (legacy content.objects.impl.thieving.ChestObjects); 378 is the empty/looted state
    // it swaps to afterwards, and just reports the chest as empty (matches legacy exactly).
    private const val SPECIAL_CHEST_OBJECT_ID = 375
    private const val EMPTY_CHEST_OBJECT_ID = 378
    private const val YANILLE_CHEST_X = 2593
    private const val YANILLE_CHEST_Y = 3108
    private const val LEGENDS_CHEST_X = 2733
    private const val LEGENDS_CHEST_Y = 3374
    private const val THIEVING_XP_BONUS_HEAD_ITEM = 2631

    // Legacy Thieving.EMPTY_STALL_ID - what a "stall" target_type object swaps to after a
    // successful steal, for respawnTicks ticks, before reverting to the real stall object.
    private const val EMPTY_STALL_ID = 634
    private const val THIEVING_THROTTLE_KEY = "thieving.generic"

    override val definition: SkillPluginDefinition = skillPlugin("Thieving", Skill.THIEVING) {
        if (npcIds.isNotEmpty()) {
            npcClick(preset = PolicyPreset.GATHERING, option = 3, *npcIds) { interaction ->
                pickpocketNpc(interaction)
            }
        }
        // Revision-218 cache options: the cage is first-click Unlock, while stalls are
        // second-click Steal-from. Object 20885 is a Buy-option crate in this cache.
        objectClick(preset = PolicyPreset.GATHERING, option = 1, *cageIds) { stealFromTarget(it) }
        objectClick(preset = PolicyPreset.GATHERING, option = 2, *stallIds) { stealFromTarget(it) }
        objectClick(preset = PolicyPreset.GATHERING, option = 1, 6847) { true }
        objectClick(preset = PolicyPreset.GATHERING, option = 2, 11729, 4877) { true }
        objectClick(preset = PolicyPreset.GATHERING, option = 1, SPECIAL_CHEST_OBJECT_ID) { interaction ->
            openSpecialChest(interaction)
        }
        objectClick(preset = PolicyPreset.GATHERING, option = 2, EMPTY_CHEST_OBJECT_ID) { interaction ->
            interaction.player.ui.message("This chest is empty!")
            true
        }
        // Assumption (flagged for live-client verification): option 1 is the primary action slot;
        // toggleObstacle() is state-driven internally and ignores which option was clicked, same
        // reasoning as the farming patch click-option assumption.
        objectClick(preset = PolicyPreset.GATHERING, option = 1, *tombObjectIds) { interaction ->
            toggleObstacle(interaction.player, interaction.objectId)
        }
        objectClick(preset = PolicyPreset.GATHERING, option = 1, *urnObjectIds) { interaction ->
            toggleObstacle(interaction.player, interaction.objectId)
        }
        // Ported from the legacy PyramidPlunderDialogueModule/PyramidPlunderDialogueOptionHandler
        // (game-server) - the client opens this dialogue id itself (e.g. leaving the pyramid),
        // this only renders the Yes/No text and handles the response.
        confirmDialogue(
            preset = PolicyPreset.GATHERING,
            dialogueId = PYRAMID_EXIT_DIALOGUE_ID,
            message = "Exit pyramid plunder?",
        ) { player -> resetPlunder(player) }
    }

    override val contentManifest: ContentModuleManifest = definition.manifest(
        id = descriptor.id,
        owner = "gameplay",
        version = descriptor.version,
        maturity = ContentMaturity.STABLE,
    )

    /**
     * Rolls the weighted drop table (legacy Thieving.attempt's `if (rollChance < data.itemChance[i])`
     * scan) and picks an amount within that entry's range.
     */
    private fun rollLoot(target: ThievingTargetDef, player: SkillPlayer): Pair<Int, Int> {
        val roll = player.random.between(0, 99)
        val index = target.lootItems.indices.firstOrNull { roll < target.lootChances[it] } ?: target.lootItems.lastIndex
        return target.lootItems[index] to player.random.between(target.lootMinAmounts[index], target.lootMaxAmounts[index])
    }

    // Legacy per-tile stall face lookup (Thieving.attempt) - stalls have no orientation data of
    // their own, so the face the empty-stall replacement should render at is hardcoded per known
    // world tile, matching each stall's real placement.
    private fun stallFace(x: Int, y: Int): Int = when {
        (x == 2658 && y == 3297) || (x == 2663 && y == 3296) -> 0
        (x == 2655 && y == 3311) || (x == 2656 && y == 3302) -> 1
        (x == 2662 && y == 3314) || (x == 2657 && y == 3314) -> 2
        (x == 2667 && y == 3303) || (x == 2667 && y == 3310) -> 3
        else -> -1
    }

    private fun pickpocketNpc(interaction: SkillNpcInteraction): Boolean {
        val player = interaction.player
        val target = targets.firstOrNull { it.id == interaction.npc.id } ?: return false
        if (player.skills.current(Skill.THIEVING) < target.level) {
            player.ui.message("You need a thieving level of ${target.level} to steal from ${target.name.replace('_', ' ')}s.")
            return true
        }
        if (!player.actions.tryThrottle(THIEVING_THROTTLE_KEY, 2000L)) {
            return true
        }
        player.actions.animate(881)
        val (itemId, amount) = rollLoot(target, player)
        if (!player.inventory.transaction { add(itemId, amount) }) {
            player.ui.message("You don't have enough inventory space!")
            return true
        }
        player.actions.logGathering(itemId, amount, "Thieving")
        player.skills.gainXp(target.experience, Skill.THIEVING)
        player.actions.triggerRandomEvent(target.experience)
        player.ui.message("You receive ${article(player.inventory.itemName(itemId))} ${player.inventory.itemName(itemId).lowercase()}")
        return true
    }

    private fun stealFromTarget(interaction: SkillObjectInteraction): Boolean {
        val player = interaction.player
        val target = targets.firstOrNull { it.id == interaction.target.id } ?: return false
        if (player.skills.current(Skill.THIEVING) < target.level) {
            player.ui.message("You need a thieving level of ${target.level} to steal from ${target.name.replace('_', ' ')}s.")
            return true
        }
        if (!player.actions.tryThrottle(THIEVING_THROTTLE_KEY, 2000L)) {
            return true
        }
        val isStall = target.targetType == "stall"
        // Legacy: PICKPOCKETING/OTHER targets (npcs, chests, cages) use the pickpocket emote;
        // only STALL_THIEVING targets use the stall-steal emote.
        player.actions.animate(if (isStall) 832 else 881)
        val (itemId, amount) = rollLoot(target, player)
        if (!player.inventory.transaction { add(itemId, amount) }) {
            player.ui.message("You don't have enough inventory space!")
            return true
        }
        player.actions.logGathering(itemId, amount, "Thieving")
        player.skills.gainXp(target.experience, Skill.THIEVING)
        player.actions.triggerRandomEvent(target.experience)
        player.ui.message("You receive ${article(player.inventory.itemName(itemId))} ${player.inventory.itemName(itemId).lowercase()}")
        if (isStall && target.respawnTicks > 0) {
            val face = stallFace(interaction.position.x, interaction.position.y)
            if (face >= 0) {
                player.world.replaceObject(interaction.target, EMPTY_STALL_ID, target.respawnTicks, face = face)
            }
        }
        return true
    }

    // Legacy behavior (ChestObjects.onFirstClick, object 375 at two specific world tiles): each
    // location is a distinct one-off reward chest that swaps to the empty object (378) for a
    // while after being looted, with an orientation specific to that chest (the replacement has
    // no orientation of its own to infer it from). Restored to full legacy parity: loot messages,
    // an object-tile gfx, a server yell on the rare drop, a 1200ms per-chest throttle, the
    // level/premium/free-slot gates, the head-slot xp bonus, and the random-event trigger.
    private fun openSpecialChest(interaction: SkillObjectInteraction): Boolean {
        val player = interaction.player
        val position = interaction.position
        return when {
            // Legacy only gates the Yanille chest on plane 1; Legends has no plane check.
            position.x == YANILLE_CHEST_X && position.y == YANILLE_CHEST_Y && position.z == 1 -> {
                lootSpecialChest(
                    interaction = interaction,
                    chestName = "Yanille",
                    requiredLevel = 70,
                    rareItems = intArrayOf(2577, 2579, 2631),
                    minCoins = 300,
                    coinsRange = 1200,
                    xpBonus = 300,
                    respawnTicks = 20,
                    replacementType = 10,
                    replacementFace = 2,
                    throttleKey = "thieving.yanille_chest",
                    randomEventExperience = 900,
                )
            }
            position.x == LEGENDS_CHEST_X && position.y == LEGENDS_CHEST_Y -> {
                if (!player.profile.premium) {
                    player.world.resetPosition()
                    return true
                }
                lootSpecialChest(
                    interaction = interaction,
                    chestName = "Legends",
                    requiredLevel = 85,
                    rareItems = intArrayOf(1050, 2581, 2631),
                    minCoins = 500,
                    coinsRange = 2000,
                    xpBonus = 500,
                    respawnTicks = 25,
                    replacementType = 11,
                    replacementFace = -1,
                    throttleKey = "thieving.legends_chest",
                    randomEventExperience = 1500,
                )
            }
            else -> false
        }
    }

    private fun lootSpecialChest(
        interaction: SkillObjectInteraction,
        chestName: String,
        requiredLevel: Int,
        rareItems: IntArray,
        minCoins: Int,
        coinsRange: Int,
        xpBonus: Int,
        respawnTicks: Int,
        replacementType: Int,
        replacementFace: Int,
        throttleKey: String,
        randomEventExperience: Int,
    ): Boolean {
        val player = interaction.player
        if (player.skills.current(Skill.THIEVING) < requiredLevel) {
            player.ui.message("You must be level $requiredLevel thieving to open this chest")
            return true
        }
        if (player.inventory.freeSlots() < 1) {
            player.ui.message("You need atleast one free inventory slot!")
            return true
        }
        if (!player.actions.tryThrottle(throttleKey, 1200L)) {
            return true
        }
        val rolledRare = player.random.chance(3, 1000)
        var grantedItemId = -1
        var grantedAmount = 0
        val committed = player.inventory.transaction {
            if (rolledRare) {
                grantedItemId = rareItems[player.random.between(0, rareItems.size - 1)]
                grantedAmount = 1
            } else {
                grantedItemId = 995
                grantedAmount = player.random.between(minCoins, minCoins + coinsRange - 1)
            }
            add(grantedItemId, grantedAmount)
        }
        if (!committed) return true
        player.actions.logGathering(grantedItemId, grantedAmount, "Thieving")
        if (rolledRare) {
            val itemName = player.inventory.itemName(grantedItemId)
            player.ui.message("You have recieved a $itemName!")
            player.actions.yell("[Server] - ${player.profile.name} has just received from the $chestName chest a  $itemName")
        } else {
            player.ui.message("You find $grantedAmount coins inside the chest")
        }
        player.world.graphic(444, interaction.position)
        player.world.replaceObject(interaction.target, EMPTY_CHEST_OBJECT_ID, respawnTicks, type = replacementType, face = replacementFace)
        if (player.equipment.item(SkillEquipmentSlot.HEAD) == THIEVING_XP_BONUS_HEAD_ITEM) {
            player.skills.gainXp(xpBonus, Skill.THIEVING)
        }
        player.actions.triggerRandomEvent(randomEventExperience)
        return true
    }

    private fun article(itemName: String): String = when {
        itemName.startsWith("a", ignoreCase = true) || itemName.startsWith("e", ignoreCase = true) ||
            itemName.startsWith("i", ignoreCase = true) || itemName.startsWith("o", ignoreCase = true) ||
            itemName.startsWith("u", ignoreCase = true) -> if (itemName.endsWith("s", ignoreCase = true)) "some" else "an"
        itemName.endsWith("s", ignoreCase = true) -> "some"
        else -> "a"
    }

    private fun parseIntArray(value: String): IntArray = value.split(",").map { it.trim().toInt() }.toIntArray()

    private fun loadTargets(): List<ThievingTargetDef> =
        TomlRecordReader.readRecords("thieving/tables.toml", "target").map { row ->
            ThievingTargetDef(
                name = row.getValue("name"),
                targetType = row.getValue("target_type"),
                id = row.getValue("id").toInt(),
                level = row.getValue("level").toInt(),
                experience = row.getValue("experience").toInt(),
                lootItems = parseIntArray(row.getValue("loot_items")),
                lootChances = parseIntArray(row.getValue("loot_chances")),
                lootMinAmounts = parseIntArray(row.getValue("loot_min_amounts")),
                lootMaxAmounts = parseIntArray(row.getValue("loot_max_amounts")),
                respawnTicks = row["respawn_ticks"]?.toInt() ?: 0,
            )
        }

    // --- Pyramid Plunder ---
    // Legacy PyramidPlunder never persisted its state across logout, so it's modeled as ordinary
    // plugin-owned session state (ContentAttributes) rather than needing a save-data bridge like
    // Farming/Agility. The global door-shuffle state is a plain object-singleton field, exactly
    // matching how the legacy PyramidPlunder object held it (this module is itself a singleton).

    data class PyramidPlunderState(
        var ticksRemaining: Int = -1,
        var roomNumber: Int = 0,
        var looting: Boolean = false,
        val obstacles: IntArray = intArrayOf(
            26616, 0, 26626, 0, 26618, 0, 26619, 0, 26620, 0, 26621, 0,
            26580, 0, 26600, 0, 26601, 0, 26602, 0, 26603, 0, 26604, 0,
            26605, 0, 26606, 0, 26607, 0, 26608, 0, 26609, 0, 26610, 0,
            26611, 0, 26612, 0, 26613, 0,
        ),
    )

    private val PLUNDER_STATE_KEY = ContentAttributeKey<PyramidPlunderState>("skill.thieving", "pyramidPlunder")
    private const val TOMB_COUNT = 6
    private const val URN_COUNT = 15
    private val URN_CONFIG = intArrayOf(0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28)
    private val TOMB_CONFIG = intArrayOf(2, 0, 9, 10, 11, 12)
    private val ALL_DOORS = arrayOf(SkillPosition(3288, 2799), SkillPosition(3293, 2794), SkillPosition(3288, 2789), SkillPosition(3283, 2794))
    private val ROOM_ENTRANCES = arrayOf(
        SkillPosition(1954, 4477, 0), SkillPosition(1977, 4471, 0), SkillPosition(1927, 4453, 0),
        SkillPosition(1965, 4444, 0), SkillPosition(1927, 4424, 0), SkillPosition(1943, 4421, 0),
        SkillPosition(1974, 4420, 0),
    )
    private val PLUNDER_START = SkillPosition(1927, 4477, 0)
    private val PLUNDER_END = SkillPosition(3289, 2801, 0)
    private var currentDoorState: SkillPosition? = null
    private val nextRoomDoorOffset = IntArray(7)

    private fun plunderState(player: SkillPlayer): PyramidPlunderState? = player.attributes.get(PLUNDER_STATE_KEY)

    private fun plunderStateOrNew(player: SkillPlayer): PyramidPlunderState =
        plunderState(player) ?: PyramidPlunderState().also { player.attributes.put(PLUNDER_STATE_KEY, it) }

    fun tick(player: SkillPlayer) {
        val state = plunderState(player) ?: return
        if (state.ticksRemaining == -1) return
        state.ticksRemaining--
        if (state.ticksRemaining == 0) {
            resetPlunder(player, timedOut = true)
        } else if (state.ticksRemaining % 100 == 0) {
            val minutes = state.ticksRemaining / 100
            player.ui.message("You got $minutes minute${if (minutes == 1) "" else "s"} left.")
        }
    }

    @JvmStatic
    fun hindersTeleport(player: SkillPlayer): Boolean = (plunderState(player)?.ticksRemaining ?: -1) != -1

    fun isLooting(player: SkillPlayer): Boolean = plunderState(player)?.looting == true

    fun roomNumber(player: SkillPlayer): Int = plunderState(player)?.roomNumber ?: 0

    fun startPosition(): SkillPosition = PLUNDER_START

    fun currentDoor(): SkillPosition? = currentDoorState

    fun startPlunder(player: SkillPlayer) {
        val state = plunderStateOrNew(player)
        state.ticksRemaining = 500
        state.roomNumber = 0
        state.looting = false
        resetObstacleVarbits(player, state)
        player.world.teleport(PLUNDER_START)
        player.ui.message("Starting plunder test...")
    }

    @JvmStatic
    @JvmOverloads
    fun resetPlunder(player: SkillPlayer, timedOut: Boolean = false) {
        val state = plunderState(player) ?: return
        if (timedOut) player.ui.message("You have run out time!")
        state.ticksRemaining = -1
        state.roomNumber = 0
        state.looting = false
        resetObstacleVarbits(player, state)
        player.world.teleport(PLUNDER_END)
    }

    fun advanceRoom(player: SkillPlayer) {
        val state = plunderState(player) ?: return
        if (state.roomNumber + 1 == 8) return
        val level = 31 + state.roomNumber * 10
        if (player.skills.current(Skill.THIEVING) < level) {
            player.ui.message("You need level $level thieving to enter next room!")
            return
        }
        player.world.teleport(ROOM_ENTRANCES[state.roomNumber])
        resetObstacleVarbits(player, state)
        state.roomNumber++
    }

    /**
     * Rotates the shared door cycle (legacy PlunderDoorProcessor, run every game tick). Mirrors
     * the legacy sequencing exactly, including its apparent quirk: even when [rotateCurrentDoor]
     * picks a deliberately-different door first, the unconditional resetGlobalCycleState() call
     * right after immediately re-randomizes currentDoor again from the full set anyway. Preserved
     * as-is rather than "fixed", since this is pre-existing behavior, not something introduced here.
     */
    fun advanceDoorCycle(rotateCurrentDoor: Boolean) {
        if (rotateCurrentDoor) {
            val candidates = ALL_DOORS.filter { it != currentDoorState }
            if (candidates.isNotEmpty()) {
                currentDoorState = candidates[kotlin.random.Random.nextInt(candidates.size)]
            }
        }
        resetGlobalCycleState()
    }

    private fun resetGlobalCycleState() {
        currentDoorState = ALL_DOORS[kotlin.random.Random.nextInt(ALL_DOORS.size)]
        for (i in nextRoomDoorOffset.indices) {
            nextRoomDoorOffset[i] = kotlin.random.Random.nextInt(4)
        }
    }

    init {
        resetGlobalCycleState()
    }

    private fun checkSlot(state: PyramidPlunderState) = state.obstacles.size / (TOMB_COUNT + URN_COUNT)

    private fun openDoor(state: PyramidPlunderState, objectId: Int): Boolean {
        val slot = checkSlot(state)
        for (i in 2 until TOMB_COUNT) {
            if (state.obstacles[i * slot] == objectId && state.obstacles[i * slot + 1] == 1) return true
        }
        return false
    }

    /** legacy nextRoomDoorFor: only one of the 4 tomb-door ids (26618-26621) is "the" exit for the current room. */
    private fun nextRoomDoorFor(state: PyramidPlunderState): Int = nextRoomDoorOffset[state.roomNumber] + 26618

    private fun canOpenNextRoomDoor(state: PyramidPlunderState, objectId: Int): Boolean =
        nextRoomDoorFor(state) == objectId && openDoor(state, objectId)

    private fun resetObstacleVarbits(player: SkillPlayer, state: PyramidPlunderState) {
        player.ui.varbit(820, 0)
        player.ui.varbit(821, 0)
        val slot = checkSlot(state)
        for (i in 0 until TOMB_COUNT + URN_COUNT) {
            state.obstacles[i * slot + 1] = 0
        }
    }

    private fun displayUrns(player: SkillPlayer, state: PyramidPlunderState) {
        var config = 0
        val slot = checkSlot(state)
        for (i in TOMB_COUNT until TOMB_COUNT + URN_COUNT) {
            val urnSlot = i - TOMB_COUNT
            when (state.obstacles[i * slot + 1]) {
                1 -> config = config or (1 shl URN_CONFIG[urnSlot])
                2 -> config = config or (1 shl (URN_CONFIG[urnSlot] + 1))
            }
        }
        player.ui.varbit(820, config)
    }

    private fun displayTombs(player: SkillPlayer, state: PyramidPlunderState) {
        var config = 0
        val slot = checkSlot(state)
        for (i in 0 until TOMB_COUNT) {
            if (state.obstacles[i * slot + 1] == 1) {
                config = config or (1 shl TOMB_CONFIG[i])
            }
        }
        player.ui.varbit(821, config)
    }

    /** Legacy PyramidPlunder.toggleObstacle - state-driven, ignores which click option fired. */
    private fun toggleObstacle(player: SkillPlayer, objectId: Int): Boolean {
        val state = plunderState(player) ?: return false
        if (state.looting) return true
        val slot = checkSlot(state)
        var found = false
        for (i in TOMB_COUNT until TOMB_COUNT + URN_COUNT) {
            if (state.obstacles[i * slot] == objectId && state.obstacles[i * slot + 1] == 0) {
                state.obstacles[i * slot + 1] = player.random.between(1, 2)
                displayUrns(player, state)
                found = true
                break
            }
        }
        if (!found && state.obstacles[0] == objectId && state.obstacles[1] == 0) {
            state.obstacles[1] = 1
            displayTombs(player, state)
            player.ui.message("Room: ${state.roomNumber} trying to do gold chest!")
            found = true
        }
        if (!found && state.obstacles[2] == objectId && state.obstacles[3] == 0) {
            state.obstacles[3] = 1
            displayTombs(player, state)
            player.ui.message("Sarcophagus!")
            found = true
        }
        if (!found) {
            for (i in 2 until TOMB_COUNT) {
                if (state.obstacles[i * slot] == objectId && state.obstacles[i * slot + 1] == 0) {
                    state.obstacles[i * slot + 1] = 1
                    displayTombs(player, state)
                    found = true
                    break
                }
            }
        }
        // Assumption (not wired anywhere in the dead legacy code either): auto-advance the room
        // the moment the room's assigned exit tomb-door is solved, since canOpenNextRoomDoor's
        // only purpose is exactly this check and nothing else ever consumed its result.
        if (found && objectId in 26618..26621 && canOpenNextRoomDoor(state, objectId)) {
            advanceRoom(player)
        }
        return found
    }
}
