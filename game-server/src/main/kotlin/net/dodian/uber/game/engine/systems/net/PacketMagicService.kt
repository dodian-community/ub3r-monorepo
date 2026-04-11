package net.dodian.uber.game.engine.systems.net

import net.dodian.uber.game.Server
import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.content.skills.smithing.Smithing
import net.dodian.uber.game.model.entity.UpdateFlag
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.netty.listener.out.SendSideTab
import net.dodian.uber.game.engine.systems.interaction.MagicOnNpcIntent
import net.dodian.uber.game.engine.systems.interaction.MagicOnObjectIntent
import net.dodian.uber.game.engine.systems.interaction.MagicOnPlayerIntent
import net.dodian.uber.game.engine.systems.interaction.scheduler.InteractionTaskScheduler
import net.dodian.uber.game.engine.systems.interaction.scheduler.NpcInteractionTask
import net.dodian.uber.game.engine.systems.interaction.scheduler.ObjectInteractionTask
import net.dodian.uber.game.engine.systems.interaction.scheduler.PlayerInteractionTask
import net.dodian.uber.game.engine.systems.skills.ProgressionService
import net.dodian.uber.game.engine.systems.skills.RuneCostService
import net.dodian.uber.game.engine.util.Misc
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry

/**
 * Kotlin service for magic-packet side-effects.
 *
 * Covers:
 * - MagicOnPlayer (opcode 249)
 * - MagicOnNpc (opcode 131)
 * - NpcAttack magicId reset in NpcInteractionListener
 * - MagicOnItems (opcode 237) — enchant, alchemy, superheat
 */
object PacketMagicService {

    // -------------------------------------------------------------------------
    // MagicOnPlayer
    // -------------------------------------------------------------------------

    /**
     * Stores the selected spell and schedules a player-attack interaction.
     * Replaces the `client.magicId = magicId` mutation + interaction scheduling
     * previously done inline in MagicOnPlayerListener.
     */
    @JvmStatic
    fun handleMagicOnPlayer(client: Client, opcode: Int, victimIndex: Int, magicId: Int) {
        client.magicId = magicId
        if (client.deathStage >= 1) return
        PlayerRegistry.getClient(victimIndex) ?: return
        if (client.randomed || client.UsingAgility) return
        val intent = MagicOnPlayerIntent(opcode, PlayerRegistry.cycle.toLong(), magicId, victimIndex)
        InteractionTaskScheduler.schedule(client, intent, PlayerInteractionTask(client, intent))
    }

    // -------------------------------------------------------------------------
    // MagicOnNpc
    // -------------------------------------------------------------------------

    /**
     * Stores the selected spell and schedules an NPC-attack interaction.
     * Replaces the `client.magicId = magicId` mutation + interaction scheduling
     * previously done inline in MagicOnNpcListener.
     */
    @JvmStatic
    fun handleMagicOnNpc(client: Client, opcode: Int, npcIndex: Int, magicId: Int) {
        client.magicId = magicId
        if (client.deathStage >= 1) return
        Server.npcManager.npcMap[npcIndex] ?: return
        if (client.randomed || client.UsingAgility) return
        val intent = MagicOnNpcIntent(opcode, PlayerRegistry.cycle.toLong(), magicId, npcIndex)
        InteractionTaskScheduler.schedule(client, intent, NpcInteractionTask(client, intent))
    }

    // -------------------------------------------------------------------------
    // MagicOnObject
    // -------------------------------------------------------------------------

    /**
     * Schedules a magic-on-object interaction after the listener has decoded
     * the world coordinates, spell id, and object id.
     */
    @JvmStatic
    fun handleMagicOnObject(client: Client, opcode: Int, objectX: Int, objectY: Int, objectId: Int, spellId: Int) {
        if (client.randomed || client.UsingAgility) return

        val targetPosition = Position(objectX, objectY, client.position.z)
        val objectDef = Misc.getObject(objectId, objectX, objectY, client.position.z)
        val obj = GameObjectData.forId(objectId)
        val intent = MagicOnObjectIntent(
            opcode = opcode,
            createdCycle = PlayerRegistry.cycle.toLong(),
            spellId = spellId,
            objectId = objectId,
            objectPosition = targetPosition,
            objectData = obj,
            objectDef = objectDef,
        )
        InteractionTaskScheduler.schedule(client, intent, ObjectInteractionTask(client, intent))
    }

    // -------------------------------------------------------------------------
    // NpcAttack magicId reset
    // -------------------------------------------------------------------------

    /**
     * Clears any previously selected magic spell when the player initiates a
     * melee attack on an NPC (NpcInteractionListener opcode 72).
     */
    @JvmStatic
    fun clearMagicIdIfSet(client: Client) {
        if (client.magicId >= 0) {
            client.magicId = -1
        }
    }

    // -------------------------------------------------------------------------
    // MagicOnItems — superheat, enchant, alchemy (opcode 237)
    // -------------------------------------------------------------------------

    /**
     * Full handler for the magic-on-inventory-item packet.
     * The listener decodes the slot, item id, and spell id then delegates here.
     *
     * @param castOnSlot slot index in the player's inventory (already bounds-checked by listener)
     * @param castOnItem item id the spell is cast on
     * @param castSpell  spell id
     */
    @JvmStatic
    fun handleMagicOnItem(client: Client, castOnSlot: Int, castOnItem: Int, castSpell: Int) {
        // Validate slot bounds — disconnect on tampered packet
        if (castOnSlot < 0 || castOnSlot > 28) {
            client.disconnected = true
            return
        }

        val value = Server.itemManager.getAlchemy(castOnItem)

        if (System.currentTimeMillis() - client.lastMagic < 1800 ||
            !client.playerHasItem(castOnItem) ||
            client.playerItems[castOnSlot] != castOnItem + 1
        ) {
            client.send(SendSideTab(6))
            return
        }
        if (client.randomed || client.randomed2) return

        // Superheat
        if (castSpell == 1173) {
            if (!checkLevel(client, 43)) return
            Smithing.castSuperheat(client, castOnItem)
            return
        }

        // Enchant gems
        if (handleEnchant(client, castSpell, castOnItem)) return

        // Low or High alchemy
        if (castSpell == 1162 || castSpell == 1178) {
            handleAlchemy(client, castOnSlot, castOnItem, value)
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun checkLevel(client: Client, lvl: Int): Boolean {
        if (client.getLevel(Skill.MAGIC) < lvl) {
            client.send(SendMessage("You need a magic level of $lvl to cast this spell."))
            return false
        }
        return true
    }

    private fun handleEnchant(client: Client, spell: Int, itemId: Int): Boolean {
        val reqLevel: Int
        val runeCost: Int
        val exp: Int
        var resultItem = 0

        when (spell) {
            1155 -> { // sapphire
                reqLevel = 7; runeCost = 2; exp = 175
                if (itemId == 1637) resultItem = 2550 else if (itemId == 1694) resultItem = 1727
            }
            1165 -> { // emerald
                reqLevel = 27; runeCost = 4; exp = 370
                if (itemId == 1696) resultItem = 1729
            }
            1176 -> { // ruby
                reqLevel = 49; runeCost = 6; exp = 590
                if (itemId == 1641) resultItem = 2568 else if (itemId == 1698) resultItem = 1725
            }
            1180 -> { // diamond
                reqLevel = 57; runeCost = 8; exp = 670
                if (itemId == 1643) resultItem = 2570 else if (itemId == 1700) resultItem = 1731
            }
            1187 -> { // dragonstone
                reqLevel = 68; runeCost = 10; exp = 780
                if (itemId == 1645) resultItem = 2572 else if (itemId == 1702) resultItem = 1704
            }
            6003 -> { // onyx
                reqLevel = 87; runeCost = 10; exp = 1150
                when (itemId) {
                    6575 -> resultItem = 6583
                    6577 -> resultItem = 11128
                    6581 -> resultItem = 6585
                }
            }
            else -> return false
        }

        if (!checkLevel(client, reqLevel)) return true
        if (RuneCostService.isMissingAny(client, intArrayOf(564), intArrayOf(runeCost))) {
            client.send(SendMessage("You need $runeCost cosmic runes to cast this spell!"))
            return true
        }
        if (resultItem == 0) {
            client.send(SendMessage("Cant enchant this item!"))
            return true
        }
        client.lastMagic = System.currentTimeMillis()
        client.deleteItem(itemId, 1)
        RuneCostService.consume(client, intArrayOf(564), intArrayOf(runeCost))
        client.addItem(resultItem, 1)
        client.checkItemUpdate()
        client.performAnimation(720, 0)
        client.callGfxMask(115, 100)
        client.send(SendSideTab(6))
        ProgressionService.addXp(client, exp, Skill.MAGIC)
        return true
    }

    private fun handleAlchemy(client: Client, slot: Int, itemId: Int, value: Int) {
        if (!client.playerHasItem(561) || (itemId == 561 && !client.playerHasItem(561, 2))) {
            client.send(SendMessage("Requires nature rune!"))
            return
        }
        if (itemId == 995 || client.premiumItem(itemId) ||
            (itemId in 2415..2417) || value < 1
        ) {
            client.send(SendMessage("This item can't be alched"))
            return
        }
        client.lastMagic = System.currentTimeMillis()
        client.deleteItem(itemId, slot, 1)
        client.deleteItem(561, 1)
        client.addItem(995, value)
        client.checkItemUpdate()
        ProgressionService.addXp(client, 600, Skill.MAGIC)
        client.performAnimation(713, 0)
        client.callGfxMask(113, 100)
        client.send(SendSideTab(6))
        client.getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true)
    }
}

