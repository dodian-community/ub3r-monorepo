package net.dodian.uber.game.skills.thieving

import net.dodian.uber.game.event.GameEventScheduler
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.`object`.GlobalObject
import net.dodian.uber.game.model.`object`.Object as GameObject
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.persistence.audit.ItemLog
import net.dodian.uber.game.runtime.interaction.PlayerTickThrottleService
import net.dodian.uber.game.skills.core.progression.SkillProgressionService
import net.dodian.uber.game.skills.core.runtime.SkillingRandomEventService

object ThievingService {
    const val PICKPOCKET_EMOTE: Int = 881
    const val STALL_THIEVING_EMOTE: Int = 832
    const val EMPTY_STALL_ID: Int = 634

    @JvmStatic
    fun attempt(player: Client, entityId: Int, position: Position) {
        val data = ThievingDefinition.forId(entityId) ?: return
        if (player.chestEventOccur) return
        val failChance = 0
        val face =
            if ((position.x == 2658 && position.y == 3297) || (position.x == 2663 && position.y == 3296)) 0
            else if ((position.x == 2655 && position.y == 3311) || (position.x == 2656 && position.y == 3302)) 1
            else if ((position.x == 2662 && position.y == 3314) || (position.x == 2657 && position.y == 3314)) 2
            else if ((position.x == 2667 && position.y == 3303) || (position.x == 2667 && position.y == 3310)) 3
            else -1
        if (face == -1 && data.type == ThievingType.STALL_THIEVING) {
            player.send(SendMessage("Not added object!"))
            return
        }
        val emptyObject = GameObject(EMPTY_STALL_ID, position.x, position.y, position.z, 10, face, data.entityId)
        if (player.getLevel(Skill.THIEVING) < data.requiredLevel) {
            player.send(SendMessage("You need a thieving level of ${data.requiredLevel} to steal from ${data.name.lowercase().replace('_', ' ')}s."))
            return
        }
        if (!PlayerTickThrottleService.tryAcquireMs(player, PlayerTickThrottleService.THIEVING_GENERIC, 2000L)) {
            return
        }
        if (data.type == ThievingType.PICKPOCKETING || data.type == ThievingType.OTHER) {
            player.setFocus(position.x, position.y)
            player.requestAnim(PICKPOCKET_EMOTE, 0)
            player.send(SendMessage("You attempt to steal from the ${data.name.lowercase().replace('_', ' ')}..."))
        } else {
            if (GlobalObject.hasGlobalObject(emptyObject)) return
            player.requestAnim(STALL_THIEVING_EMOTE, 0)
        }

        GameEventScheduler.runLaterMs(600) {
            if (player.disconnected) return@runLaterMs
            if (failChance > 75) {
                player.send(SendMessage("You fail to thieve from the ${data.name.lowercase().replace('_', ' ')}"))
                return@runLaterMs
            }
            if (!player.hasSpace()) {
                player.send(SendMessage("You don't have enough inventory space!"))
                return@runLaterMs
            }
            SkillProgressionService.gainXp(player, data.receivedExperience, Skill.THIEVING)
            player.canPreformAction = false
            if (data.item.size > 1) {
                val rollChance = (Math.random() * 100).toInt()
                for (i in data.item.indices) {
                    if (rollChance < data.itemChance[i]) {
                        val id = data.item[i]
                        val amount = data.itemAmount[i].value
                        player.addItem(id, amount)
                        ItemLog.playerGathering(player, id, amount, player.position.copy(), "Thieving")
                        player.send(SendMessage("You receive ${article(player.GetItemName(id))} ${player.GetItemName(id).lowercase()}"))
                        break
                    }
                }
            } else {
                val id = data.item[0]
                val amount = data.itemAmount[0].value
                player.addItem(id, amount)
                ItemLog.playerGathering(player, id, amount, player.position.copy(), "Thieving")
                player.send(SendMessage("You receive ${article(player.GetItemName(id))} ${player.GetItemName(id).lowercase()}"))
            }
            if (data.type == ThievingType.STALL_THIEVING) {
                val stallObject = GameObject(EMPTY_STALL_ID, position.x, position.y, position.z, 10, face, data.entityId)
                GlobalObject.addGlobalObject(stallObject, data.respawnTime * 1000)
            }
            player.checkItemUpdate()
            SkillingRandomEventService.trigger(player, data.receivedExperience)
            player.chestEvent++
        }
    }

    private fun article(itemName: String): String =
        when {
            (itemName.startsWith("a") || itemName.startsWith("e") || itemName.startsWith("i") || itemName.startsWith("o") || itemName.startsWith("u")) && !itemName.endsWith("s") -> "an"
            itemName.endsWith("s") -> "some"
            else -> "a"
        }
}
