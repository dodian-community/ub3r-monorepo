package net.dodian.uber.game.activity.partyroom

import net.dodian.uber.game.model.Position

class PartyRoomRewardItem {
    private var itemId: Int = -1
    private var itemAmount: Int = 0
    private var itemPos: Position? = null

    constructor(id: Int) {
        this.itemId = id
        this.itemAmount = 1
    }

    constructor(id: Int, amount: Int) {
        this.itemId = id
        this.itemAmount = amount
    }

    constructor(id: Int, amount: Int, pos: Position?) {
        this.itemId = id
        this.itemAmount = amount
        this.itemPos = pos
    }

    fun getId(): Int = itemId

    fun getAmount(): Int = itemAmount

    fun setAmount(amountDelta: Int) {
        itemAmount += amountDelta
    }

    fun getPosition(): Position? = itemPos

    fun setPosition(pos: Position?) {
        this.itemPos = pos
    }
}

