package net.dodian.uber.game.content.objects.impl.legacy

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectBinding
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.`in`.ClickObject2Listener
import net.dodian.uber.game.netty.listener.`in`.ClickObject3Listener
import net.dodian.uber.game.netty.listener.`in`.ClickObject4Listener
import net.dodian.uber.game.netty.listener.`in`.ClickObject5Listener
import net.dodian.uber.game.netty.listener.`in`.ClickObjectListener
import net.dodian.uber.game.netty.listener.`in`.ItemOnObjectListener
import net.dodian.uber.game.netty.listener.`in`.MagicOnObjectListener

/**
 * Low-priority residual fallback for behavior not yet ported to typed object modules.
 * Runs through content-dispatch, not listener-local branch execution.
 */
object LegacyResidualObjects : ObjectContent {
    private val click1 = ClickObjectListener()
    private val click2 = ClickObject2Listener()
    private val click3 = ClickObject3Listener()
    private val click4 = ClickObject4Listener()
    private val click5 = ClickObject5Listener()
    private val itemOnObject = ItemOnObjectListener()
    private val magicOnObject = MagicOnObjectListener()

    override val objectIds: IntArray = intArrayOf(
        1,2,3,54,55,56,57,133,375,378,409,410,492,733,823,873,874,878,879,881,882,884,1003,1294,1521,1524,1557,1558,1568,1570,1591,1723,1725,1726,1734,1738,1739,1740,1746,1747,1749,1755,1757,1764,1948,1992,2097,2102,2104,2113,2147,2148,2150,2151,2152,2153,2156,2158,2213,2214,2309,2352,2391,2392,2406,2408,2492,2623,2624,2625,2634,2728,2781,2783,2796,2797,2833,3045,3608,3994,4877,5054,5055,5130,5131,5276,5488,5553,5946,5947,5960,6084,6232,6249,6434,6451,6452,6702,6847,7837,7838,7839,7962,8689,9294,9358,9359,9368,9369,9391,11635,11636,11638,11641,11643,11657,11666,11729,11730,11731,11732,11733,11734,11833,11834,12260,12279,14847,14868,14889,14890,14896,14903,14905,14909,14914,15656,16466,16469,16509,16510,16519,16520,16664,16665,16675,16677,16680,16681,16682,16683,17122,17384,17385,17387,19147,20211,20275,20277,20358,20377,20873,20877,20931,20932,23131,23132,23134,23135,23137,23138,23139,23140,23144,23145,23271,23542,23547,23548,23556,23557,23559,23560,23561,23564,23567,23640,25824,25929,25938,25939,26181,26193,26194,26579,26580,26616,26626,27111,27113,27114,27115,27978,29662
    )

    override fun bindings(): List<ObjectBinding> {
        return objectIds
            .distinct()
            .sorted()
            .map { ObjectBinding(objectId = it, priority = -10_000) }
    }

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        click1.atObject(client, objectId, position, obj)
        return true
    }

    override fun onSecondClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        click2.clickObject2(client, objectId, position, obj)
        return true
    }

    override fun onThirdClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        click3.clickObject3(client, objectId, position, obj)
        return true
    }

    override fun onFourthClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        click4.handleObjectClick(client, objectId, position, obj)
        return true
    }

    override fun onFifthClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        click5.handleObjectClick(client, objectId, position, obj)
        return true
    }

    override fun onUseItem(
        client: Client,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
        itemId: Int,
        itemSlot: Int,
        interfaceId: Int,
    ): Boolean {
        itemOnObject.preformObject(client, objectId, itemId, itemSlot, position, obj)
        return true
    }

    override fun onMagic(
        client: Client,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
        spellId: Int,
    ): Boolean {
        magicOnObject.atObject(client, spellId, objectId, position, obj)
        return true
    }
}
