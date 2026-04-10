package net.dodian.uber.game.model.item

import net.dodian.uber.game.Server
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.systems.world.player.PlayerRegistry
import net.dodian.uber.game.netty.listener.out.CreateGroundItem
import net.dodian.uber.game.netty.listener.out.RemoveGroundItem

class GroundItem {
    @JvmField var x: Int
    @JvmField var y: Int
    @JvmField var z: Int
    @JvmField var id: Int
    @JvmField var amount: Int
    @JvmField var dropper: Int
    @JvmField var playerId: Int = -1
    @JvmField var npcId: Int = -1
    @JvmField var type: Int = 2
    @JvmField var taken: Boolean = false
    @JvmField var visible: Boolean = false
    @JvmField var npc: Boolean = false
    @JvmField var timeToShow: Int = 100
    @JvmField var timeToDespawn: Int = 200
    @JvmField var display: Int = 0

    constructor(pos: Position, id: Int, amount: Int, dropper: Int, npcId: Int) {
        x = pos.x
        y = pos.y
        z = pos.z
        this.id = id
        this.amount = amount
        this.dropper = dropper
        npc = npcId >= 0
        if (npc) {
            this.npcId = npcId
        }
        if (!Server.itemManager.isTradable(this.id)) {
            timeToDespawn += timeToShow
            timeToShow = -1
            type = 1
        }
        if (dropper > 0 && PlayerRegistry.validClient(dropper)) {
            val owner = PlayerRegistry.getClient(dropper) ?: return
            playerId = owner.dbId
            sendOwnerCreate(owner)
        }
    }

    constructor(pos: Position, id: Int, amount: Int, display: Int, visible: Boolean) {
        x = pos.x
        y = pos.y
        z = pos.z
        this.id = id
        this.amount = amount
        dropper = 0
        timeToShow = -1
        timeToDespawn = display
        this.display = display
        type = 0
        this.visible = visible
    }

    constructor(pos: Position, drop: IntArray) {
        require(drop.size >= 4)
        x = pos.x
        y = pos.y
        z = pos.z
        id = drop[1]
        amount = drop[2]
        dropper = drop[0]
        if (!Server.itemManager.isTradable(id) && drop.size == 4) {
            timeToDespawn = drop[3]
            timeToShow = -1
            type = 1
        }
        if (drop[0] >= 0 && PlayerRegistry.validClient(drop[0])) {
            val owner = PlayerRegistry.getClient(drop[0]) ?: return
            playerId = owner.dbId
            sendOwnerCreate(owner)
        }
    }

    fun setTaken(value: Boolean) {
        taken = value
    }

    fun isTaken(): Boolean = taken

    fun isVisible(): Boolean = visible

    fun reduceTime() {
        if (type == 0 && isVisible() && getDespawnTime() > 0) {
            timeToDespawn--
        } else if (timeToShow < 1 && getDespawnTime() > 0) {
            timeToDespawn--
        } else if (getDespawnTime() > 0) {
            timeToShow--
        }
    }

    fun getDespawnTime(): Int = if (timeToShow < 1) timeToDespawn else timeToShow + timeToDespawn

    fun removeItemDisplay() {
        PlayerRegistry.forEachActivePlayer { c ->
            if (c.isWithinDistance(c.position.x, c.position.y, x, y, 104)) {
                c.send(RemoveGroundItem(GameItem(id, amount), Position(x, y, z)))
            }
        }
    }

    fun itemDisplay() {
        PlayerRegistry.forEachActivePlayer { c ->
            if (type == 1 && playerId != c.dbId) {
                return@forEachActivePlayer
            }
            if (c.isWithinDistance(c.position.x, c.position.y, x, y, 104) && c.dbId != playerId && isVisible()) {
                c.send(CreateGroundItem(GameItem(id, amount), Position(x, y, z)))
            }
        }
    }

    private fun sendOwnerCreate(owner: Client) {
        owner.send(CreateGroundItem(GameItem(id, amount), Position(x, y, z)))
    }
}
