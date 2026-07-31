package net.dodian.uber.game.integration.skills.support

import io.netty.channel.embedded.EmbeddedChannel
import net.dodian.cache.objects.GameObjectData
import net.dodian.uber.game.Server
import net.dodian.uber.game.api.plugin.PluginRegistry
import net.dodian.uber.game.engine.config.FeatureStateService
import net.dodian.uber.game.engine.config.SettingsLoader
import net.dodian.uber.game.engine.loop.GameLoopService
import net.dodian.uber.game.engine.loop.GameThreadContext
import net.dodian.uber.game.engine.systems.cache.CacheBootstrapService
import net.dodian.uber.game.engine.systems.interaction.items.ItemCombinationService
import net.dodian.uber.game.engine.systems.interaction.items.ItemDispatcher
import net.dodian.uber.game.engine.systems.dialogue.DialogueOptionService
import net.dodian.uber.game.engine.systems.net.PacketBankingService
import net.dodian.uber.game.engine.systems.net.PacketInteractionService
import net.dodian.uber.game.engine.systems.net.PacketMagicService
import net.dodian.uber.game.engine.systems.net.PacketObjectService
import net.dodian.uber.game.engine.systems.objectexamines.ObjectExamines
import net.dodian.uber.game.engine.systems.skills.SkillMultiButtonService
import net.dodian.uber.game.engine.systems.world.npc.NpcManager
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry
import net.dodian.uber.game.engine.tasking.GameTaskRuntime
import net.dodian.uber.game.item.ItemManager
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.chunk.ChunkManager
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.objects.DoorRegistry
import net.dodian.uber.game.model.objects.GlobalObject
import net.dodian.uber.game.model.objects.WorldObject
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.shop.ShopManager
import net.dodian.uber.game.testutil.RequiresCache
import net.dodian.utilities.Utils
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Boots a real, in-process game world (real cache-loaded map/collision/object defs, a
 * real [GameLoopService] tick pipeline, real skill-plugin wiring via [PluginRegistry])
 * so skill tests can drive the same packet-equivalent entry points a real client uses
 * instead of a fake double. See docs/development/skill_plugin_template.md for when to
 * add a test in this tier vs. the module-level FakeSkillPlayer tests.
 *
 * Requires the real OSRS cache under game-server/data/cache; callers must gate on
 * [assumeCacheAvailable] before touching [ensureWorldBooted] so CI (no cache) skips
 * before paying any boot cost.
 */
object RealWorldHarness {
    private val booted = AtomicBoolean(false)
    private val slotSequence = AtomicInteger(1)
    private val loop = GameLoopService()
    private val runTickMethod =
        GameLoopService::class.java.getDeclaredMethod("runTick").apply { isAccessible = true }

    fun assumeCacheAvailable() = RequiresCache.assume()

    @Synchronized
    fun ensureWorldBooted() {
        if (!booted.compareAndSet(false, true)) return
        SettingsLoader.load()
        FeatureStateService.initialize(SettingsLoader.settings)
        Server.npcManager = NpcManager()
        Server.itemManager = ItemManager()
        Server.chunkManager = ChunkManager()
        Server.shopManager = ShopManager()
        GameObjectData.init()
        ObjectExamines.load()
        // Real cache/map/collision/object-def load. Do NOT call WorldRouteService.clear()/
        // allocateZone() after this - that would stomp real cache-derived collision with a
        // synthetic clear zone, defeating the point of this "real world" tier.
        CacheBootstrapService().bootstrap()
        DoorRegistry()
        // Minimal skill-plugin bootstrap only - NOT EnginePluginBootstrap.bootstrap(), which
        // also starts a real Ktor WebApi server and the GameEventBus this tier doesn't need.
        PluginRegistry.bootstrap()
        GameThreadContext.bindCurrentThread()
    }

    fun createPlayer(name: String, x: Int, y: Int, z: Int = 0, slot: Int = slotSequence.getAndIncrement()): Client {
        val client = Client(EmbeddedChannel(), slot)
        client.playerName = name
        client.longName = Utils.playerNameToLong(name)
        client.moveTo(x, y, z)
        client.loaded = true
        client.pLoaded = true
        client.initialized = true
        client.isActive = true
        client.disconnected = false
        client.setSynchronizationReady(true)
        PlayerRegistry.players[slot] = client
        PlayerRegistry.playersOnline[client.longName] = client
        return client
    }

    fun despawnPlayer(client: Client, slot: Int) {
        GameTaskRuntime.terminatePlayerTasks(client)
        client.destruct()
        if (PlayerRegistry.players.getOrNull(slot) === client) {
            PlayerRegistry.players[slot] = null
        }
        PlayerRegistry.playersOnline.remove(client.longName)
    }

    /**
     * Spawns a real, present [WorldObject] the same way production skill content does
     * (see ClientSkillPlayerAdapter.SkillWorld.spawnTemporaryObject) - type=10 dynamic
     * objects are recognized by the real interaction pipeline as long as [GameObjectData]
     * has a definition for [objectId] (guaranteed once [ensureWorldBooted] has run).
     */
    fun spawnObject(objectId: Int, x: Int, y: Int, z: Int = 0, durationMs: Int = 60_000): WorldObject {
        val worldObject = WorldObject(objectId, x, y, z, 10, 0, 0)
        GlobalObject.addGlobalObject(worldObject, durationMs)
        return worldObject
    }

    /** Undoes [spawnObject] - GlobalObject is a JVM-wide singleton, so leaving test-spawned
     * objects registered would leak into other tests running later in the same fork. */
    fun despawnObject(worldObject: WorldObject) {
        GlobalObject.getGlobalObject().remove(worldObject)
    }

    private val npcIndexSequence = AtomicInteger(1000)

    /** Registers a real [Npc] into Server.npcManager.npcMap, the same map production
     * spawns look up (see NpcManager.addNpc). Uses a high starting index to avoid
     * colliding with any npcs a test itself adds via the manager's own auto-increment. */
    fun spawnNpc(id: Int, x: Int, y: Int, z: Int = 0, face: Int = 0): Npc {
        val index = npcIndexSequence.getAndIncrement()
        val npc = Npc(index, id, Position(x, y, z), face)
        Server.npcManager.npcMap[index] = npc
        return npc
    }

    fun despawnNpc(npc: Npc) {
        Server.npcManager.npcMap.remove(npc.slot)
    }

    /** Mirrors the real click-option -> packet-opcode mapping in NpcInteractionListener. */
    private fun opcodeForNpcOption(option: Int): Int = when (option) {
        1 -> 155
        3 -> 17
        4 -> 21
        else -> error("No known npc-click opcode for option $option")
    }

    fun npcOption(client: Client, npc: Npc, option: Int) {
        PacketInteractionService.handleNpcClick(client, opcodeForNpcOption(option), option, npc.slot)
    }

    /** [itemId] must already be present in [client]'s inventory; the slot is resolved for you.
     * Calls the same PacketInteractionService.handleUseItemOnNpc(...) a real "use item on npc"
     * packet reaches (see UseItemOnNpcListener, opcode 57). */
    fun itemOnNpc(client: Client, npc: Npc, itemId: Int) {
        val itemSlot = client.getItemSlot(itemId)
        require(itemSlot >= 0) { "Item $itemId is not in ${client.playerName}'s inventory" }
        PacketInteractionService.handleUseItemOnNpc(client, 57, itemId, itemSlot, npc.slot)
    }

    /** [itemId] and [otherItemId] must already be present in [client]'s inventory; slots
     * are resolved for you. Calls the same ItemCombinationService.handle(...) a real
     * client's "use item on item" packet reaches (see ItemOnItemListener) once the
     * generic GameEventBus content path (unused by skill item-on-item bindings, and
     * not bootstrapped by this harness) declines to handle it. */
    fun itemOnItem(client: Client, itemId: Int, otherItemId: Int) {
        val itemSlot = client.getItemSlot(itemId)
        val otherSlot = client.getItemSlot(otherItemId)
        require(itemSlot >= 0) { "Item $itemId is not in ${client.playerName}'s inventory" }
        require(otherSlot >= 0) { "Item $otherItemId is not in ${client.playerName}'s inventory" }
        ItemCombinationService.handle(client, otherSlot, itemSlot)
    }

    /**
     * Completes a pending "make X" production menu (opened by a skill's itemOnItem/
     * itemOnObject/objectClick handler via SkillProduction.open) the same way a real
     * button-click packet does - see SkillMultiButtonService.tryHandle, the real entry
     * point every make-x/make-all interface button reaches. [buttonId] must be one of
     * the well-known component ids in [SkillMultiButtonService]:
     * - single-entry interfaces (one recipe): 2799=make 1, 2798=make 5, 1747=make all
     * - two-entry interfaces (e.g. short/long bow): 34174/34173/34172/34171 = first
     *   entry at 1/5/10/all, 34170/34169/34168/34167 = second entry at 1/5/10/all
     */
    fun completeProduction(client: Client, buttonId: Int): Boolean =
        SkillMultiButtonService.tryHandle(client, buttonId)

    /** Clicks option [button] on whatever dialogue is currently open for [client] - the same
     * DialogueOptionService.triggerChat(...) a real dialogue-option-click packet reaches. */
    fun dialogueOption(client: Client, button: Int) {
        DialogueOptionService.triggerChat(client, button)
    }

    /** Submits [amount] on whatever pending "enter amount" prompt [client] has open - the
     * same PacketBankingService.handleXAmount(...) a real enter-amount packet reaches. */
    fun enterAmount(client: Client, amount: Int) {
        PacketBankingService.handleXAmount(client, amount)
    }

    /** Advances a pending SkillUi.dialogue npc-chat step past its "click to continue" wait -
     * the same DialogueService.onContinue(...) a real continue-click packet reaches. */
    fun continueDialogue(client: Client) {
        net.dodian.uber.game.engine.systems.dialogue.DialogueService.onContinue(client)
    }

    /** Clicks [rawButtonId] on whatever interface is currently open for [client] (tracked via
     * client.activeInterfaceId, set as a side effect of SkillUi.open/openInterface) - the same
     * InterfaceButtonService.tryHandle(...) a real interface-button-click packet reaches. */
    fun button(client: Client, rawButtonId: Int, opIndex: Int = -1): Boolean =
        net.dodian.uber.game.ui.buttons.InterfaceButtonService.tryHandle(client, rawButtonId, opIndex)

    /** Casts [spellId] on [itemId] (must already be in [client]'s inventory) - calls the same
     * PacketMagicService.handleMagicOnItem(...) a real magic-on-inventory-item packet reaches. */
    fun magicOnItem(client: Client, itemId: Int, spellId: Int) {
        val itemSlot = client.getItemSlot(itemId)
        require(itemSlot >= 0) { "Item $itemId is not in ${client.playerName}'s inventory" }
        PacketMagicService.handleMagicOnItem(client, itemSlot, itemId, spellId)
    }

    /** [itemId] must already be present in [client]'s inventory. Calls the same
     * ItemDispatcher.tryHandle(...) a real "click item option" packet reaches (see
     * ClickItem2Listener/ClickItem3Listener) once the generic GameEventBus content path
     * (not bootstrapped by this harness) declines to handle it - option 1 is a plain
     * item click, 2/3 are the second/third menu options. */
    fun itemClick(client: Client, itemId: Int, option: Int = 1): Boolean {
        val itemSlot = client.getItemSlot(itemId)
        require(itemSlot >= 0) { "Item $itemId is not in ${client.playerName}'s inventory" }
        return ItemDispatcher.tryHandle(client, option, itemId, itemSlot, 3214)
    }

    fun giveItem(client: Client, itemId: Int, amount: Int = 1) {
        client.addItem(itemId, amount)
    }

    fun setSkillLevel(client: Client, skill: Skill, level: Int) {
        client.setLevel(level, skill)
    }

    /** Mirrors the real click-option -> packet-opcode mapping in ObjectInteractionListener. */
    private fun opcodeForObjectOption(option: Int): Int = when (option) {
        1 -> 132
        2 -> 252
        3 -> 70
        4 -> 234
        5 -> 228
        else -> error("No known object-click opcode for option $option")
    }

    fun objectOption(client: Client, objectId: Int, option: Int, x: Int, y: Int) {
        PacketObjectService.handleObjectClick(client, opcodeForObjectOption(option), option, objectId, x, y)
    }

    /** [itemId] must already be present in [client]'s inventory; the slot is resolved for you. */
    fun itemOnObject(client: Client, objectId: Int, x: Int, y: Int, itemId: Int) {
        val itemSlot = client.getItemSlot(itemId)
        require(itemSlot >= 0) { "Item $itemId is not in ${client.playerName}'s inventory" }
        PacketObjectService.handleItemOnObject(client, 192, 3214, objectId, x, y, itemSlot, itemId)
    }

    /** Advances the real tick pipeline (GameLoopService.runTick()) [times] times. */
    fun tick(times: Int = 1) {
        repeat(times) { runTickMethod.invoke(loop) }
    }

    /**
     * Ticks up to [maxTicks] times until [condition] holds, returning whether it was
     * met - a bounded wait so a stuck/regressed interaction fails fast with a clear
     * assertion rather than hanging the test run.
     */
    fun tickUntil(maxTicks: Int = 50, condition: () -> Boolean): Boolean {
        if (condition()) return true
        repeat(maxTicks) {
            tick(1)
            if (condition()) return true
        }
        return false
    }
}
