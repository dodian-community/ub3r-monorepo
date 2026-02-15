package net.dodian.uber.game.content.objects.impl.woodcutting

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.utilities.Utils

object WoodcuttingTreesObjects : ObjectContent {
    override val objectIds: IntArray = intArrayOf(
        1276, 1277, 1278, 1279, 1280,
        1281, 1282, 1283, 1284, 1285, 1286, 1287, 1289, 1290, 1291,
        1306, 1307, 1308, 1309,
        1315, 1316, 1318, 1319,
        1330, 1332,
        1365, 1383, 1384,
        1754, 1762,
        2102, 2104,
        2409,
        3033, 3034, 3035, 3036, 3037,
        3879, 3881, 3882, 3883,
        4674,
        5551, 5552,
        5902, 5903, 5904,
    )

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        var checkId = objectId
        if (checkId == 2104) {
            checkId = 2105
        }
        if (checkId == 2102) {
            checkId = 2103
        }

        val objectName = obj?.name?.lowercase() ?: ""
        if (!client.CheckObjectSkill(checkId, objectName)) {
            return false
        }
        if (client.fletchings || client.isFiremaking || client.shafting) {
            client.resetAction()
        }
        if (client.cuttingIndex < 0) {
            client.resetAction()
            return true
        }
        val wcAxe = client.findAxe()
        if (wcAxe < 0) {
            client.send(SendMessage("You need an axe in which you got the required woodcutting level for."))
            client.resetAction()
            return true
        }
        val level = Utils.woodcuttingLevels[client.cuttingIndex]
        if (level > client.getLevel(Skill.WOODCUTTING)) {
            client.send(SendMessage("You need a woodcutting level of $level to cut this tree."))
            client.resetAction()
            return true
        }
        if (client.freeSlots() < 1) {
            client.send(SendMessage("You got full inventory!"))
            client.resetAction()
            return true
        }

        client.resourcesGathered = 0
        client.lastAxeAction = System.currentTimeMillis() + 600
        client.lastAction = System.currentTimeMillis() - 600
        client.woodcutting = true
        client.requestAnim(client.getWoodcuttingEmote(Utils.axes[wcAxe]), 0)
        client.send(SendMessage("You swing your axe at the tree..."))
        return true
    }
}
