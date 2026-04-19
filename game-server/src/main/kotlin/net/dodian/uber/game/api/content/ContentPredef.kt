package net.dodian.uber.game.api.content

import net.dodian.uber.game.engine.systems.dialogue.DialogueService
import net.dodian.uber.game.engine.tasking.TaskHandle
import net.dodian.uber.game.engine.tasking.TaskPriority
import net.dodian.uber.game.events.GameEvent
import net.dodian.uber.game.model.entity.npc.Npc
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.api.content.dialogue.DialogueFactory
import net.dodian.uber.game.shop.ShopId

/**
 * Single import surface for content modules.
 *
 * Import with:
 * `import net.dodian.uber.game.api.content.ContentPredef.*`
 */
object ContentPredef {
    @JvmStatic
    fun <E : GameEvent> on(
        clazz: Class<E>,
        condition: (E) -> Boolean = { true },
        otherwiseAction: (E) -> Unit = {},
        action: (E) -> Boolean,
    ) {
        ContentEvents.on(clazz, condition, otherwiseAction, action)
    }

    @JvmStatic
    fun <E : GameEvent> post(event: E) = ContentEvents.post(event)

    @JvmStatic
    fun <E : GameEvent> postWithResult(event: E): Boolean = ContentEvents.postWithResult(event)

    @JvmStatic
    fun world(
        priority: TaskPriority = TaskPriority.STANDARD,
        block: suspend ContentScheduleScope.() -> Unit,
    ): TaskHandle = ContentScheduling.world(priority, block)

    @JvmStatic
    fun player(
        player: Client,
        priority: TaskPriority = TaskPriority.STANDARD,
        block: suspend ContentScheduleScope.() -> Unit,
    ): TaskHandle = ContentScheduling.player(player, priority, block)

    @JvmStatic
    fun npc(
        npc: Npc,
        priority: TaskPriority = TaskPriority.STANDARD,
        block: suspend ContentScheduleScope.() -> Unit,
    ): TaskHandle = ContentScheduling.npc(npc, priority, block)

    @JvmStatic
    fun worldCountdown(totalTicks: Int, onTick: (Int) -> Unit = {}, onDone: () -> Unit): TaskHandle =
        ContentTaskRecipes.worldCountdown(totalTicks, onTick, onDone)

    @JvmStatic
    fun playerCountdown(player: Client, totalTicks: Int, onTick: (Int) -> Unit = {}, onDone: () -> Unit): TaskHandle =
        ContentTaskRecipes.playerCountdown(player, totalTicks, onTick, onDone)

    @JvmStatic
    fun message(player: Client, text: String) = ContentPlayerActions.message(player, text)

    @JvmStatic
    fun openShop(player: Client, shopId: Int) = ContentPlayerActions.openShop(player, shopId)

    @JvmStatic
    fun openShop(player: Client, shopId: ShopId) = ContentPlayerActions.openShop(player, shopId)

    @JvmStatic
    fun openBank(player: Client) = ContentPlayerActions.openBank(player)

    @JvmStatic
    fun startNpcDialogue(player: Client, dialogueId: Int, npcId: Int) =
        ContentPlayerActions.startNpcDialogue(player, dialogueId, npcId)

    @JvmStatic
    fun teleport(
        player: Client,
        x: Int,
        y: Int,
        z: Int,
        premiumOnly: Boolean = false,
        emote: Int? = null,
    ) = ContentPlayerActions.teleport(player, x, y, z, premiumOnly, emote)

    @JvmStatic
    fun dialogue(player: Client, block: DialogueFactory.() -> Unit) {
        DialogueService.start(player, block)
    }
}

