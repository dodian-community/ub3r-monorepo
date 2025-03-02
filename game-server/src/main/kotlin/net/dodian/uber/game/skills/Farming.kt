package net.dodian.uber.game.skills

import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage
import net.dodian.uber.game.model.player.skills.Skill

class Farming () {

    enum class patches(val updatePos: Position, val objectId: Array<Int>, val farmData: Array<Any>) {
        CATHERBY(Position(2809,3463,0), arrayOf(7848, 8552, 8553, 8151), arrayOf(FarmingData.flowerPatch, FarmingData.allotmentPatch, FarmingData.allotmentPatch, FarmingData.herbPatch))
    }
    //Update position, objId array!, FarmingDataArray

    /* TODO: Fix shiet */
    fun Client.updateFarming() {
        //send(SendMessage("test"))
        //System.out.println("Farming tick...")
        /* Saplings in inventory + bank! */
        for (sapling in FarmingData.sapling.values()) {
            for(slot in playerItems.indices)
                if(playerItems[slot] == sapling.waterId + 1) {
                    deleteItem(sapling.waterId, slot, 1)
                    addItemSlot(sapling.saplingId, 1, slot)
                }
            for(slot in bankItems.indices)
                if(bankItems[slot] == sapling.waterId + 1) {
                    val amount = bankItemsN[slot]
                    deleteItemBank(sapling.waterId, slot, amount)
                    if(getBankAmt(sapling.saplingId) > 0) {
                        bankItems[getBankSlot(sapling.saplingId)]
                        bankItemsN[getBankSlot(sapling.saplingId)] += amount
                    } else {
                        bankItems[slot] = sapling.saplingId + 1
                        bankItemsN[slot] = amount
                    }
                    checkItemUpdate()
                }
        }
    }

    fun findPatch(objectId : Int) : Any? {
        for (patch in patches.values())
            for (slot in patch.objectId.indices)
                if(patch.objectId[slot] == objectId)
                    return patch.farmData[slot]
        return null
    }

    fun Client.harvestPatch(objectId : Int) {

    }

    fun Client.loadPatch() {

    }

    fun Client.savePatch() {

    }

    fun Client.saplingMaking(itemOne : Int, itemOneSlot : Int, itemTwo : Int, itemTwoSlot : Int) {
        for (sapling in FarmingData.sapling.values()) {
            if((itemOne == sapling.treeSeed || itemTwo == sapling.treeSeed) && (itemOne == 5356 || itemTwo == 5356)) {
                if(!playerHasItem(5325)) {
                    send(SendMessage("You are missing your gardening trowel."))
                    return;
                }
                if(getSkillLevel(Skill.FARMING) < sapling.farmLevel) {
                    send(SendMessage("You need level "+sapling.farmLevel+" farming to plant the "+GetItemName(sapling.treeSeed).lowercase()+"."))
                    return;
                }
                deleteItem(itemOne, if(itemOne == 5356) itemOneSlot else itemTwoSlot,  1)
                deleteItem(itemTwo, if(itemOne == 5356) itemTwoSlot else itemOneSlot, 1)
                addItemSlot(sapling.plantedId, 1, if(itemOne == 5356) itemOneSlot else itemTwoSlot)
            } else if ((itemOne == sapling.plantedId || itemTwo == sapling.plantedId) && (GetItemName(itemOne).startsWith("Watering can(") || GetItemName(itemTwo).startsWith("Watering can("))) {
                deleteItem(if(itemOne == sapling.plantedId) itemTwo else itemOne, if(itemOne == sapling.plantedId) itemTwoSlot else itemOneSlot,1)
                if((itemOne == sapling.plantedId && !GetItemName(itemTwo).endsWith("1)")) || (itemTwo == sapling.plantedId && !GetItemName(itemOne).endsWith("1)")))
                    addItemSlot(if(itemOne == sapling.plantedId) itemTwo-1 else itemOne-1, 1, if(itemOne == sapling.plantedId) itemTwoSlot else itemOneSlot)
                else addItemSlot(5331, 1, if(itemOne == sapling.plantedId) itemTwoSlot else itemOneSlot)
                deleteItem(sapling.plantedId, if(itemOne == sapling.plantedId) itemOneSlot else itemTwoSlot, 1)
                addItem(sapling.waterId, 1)
            }
        }
    }
}