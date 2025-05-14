package net.dodian.uber.game.skills

import com.google.gson.JsonPrimitive
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.skills.FarmingData.patches

class Farming () {
    val farmData = FarmingData()
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
        /* Compost Bin */
        for(compost in FarmingData.compostBin.values()) { /* Compost default values */
            if (farmingJson.getCompostData().get(compost.name) != null) {
                val farmCompost = farmingJson.getCompostData().get(compost.name).asJsonArray
                if(compostGrow(farmCompost.get(1).asString)) {
                    farmCompost.set(3, JsonPrimitive(farmCompost.get(3).asInt + 1))
                    if(farmCompost.get(3).asInt == compost.ticks) {
                        farmCompost.set(1, JsonPrimitive(FarmingData.compostState.DONE.toString()))
                        updateCompost(farmCompost.get(0).asString,farmCompost.get(1).asString, farmCompost.get(2).asInt)
                    }
                    //System.out.println(farmingJson.FarmingShow())
                }
            }
        }
        /* Farming Patches */
    }

    fun compostGrow(status: String) : Boolean {
        return status.equals(FarmingData.compostState.CLOSED.toString(), true)
    }
    fun Client.updateCompost(compost : String, status: String, amount : Int) {
        if(FarmingData.compostState.CLOSED.toString().equals(status))
            varbit(1057, if(FarmingData.compost.REGULAR.toString().equals(status)) 32 else 65)
        else if(FarmingData.compostState.DONE.toString().equals(status))
            varbit(1057, if(FarmingData.compost.REGULAR.toString().equals(status)) 31 else 64)
        else if(FarmingData.compostState.OPEN.toString().equals(status))
            varbit(1057, if(FarmingData.compost.REGULAR.toString().equals(status)) 15 + amount else 47 + amount)
        else if(FarmingData.compostState.FILLED.toString().equals(status))
            varbit(1057, if(FarmingData.compost.REGULAR.toString().equals(status)) 0 + amount else 32 + amount)
        else varbit(1057, 0)
    }
    fun Client.updateCompost() {
        //TODO: Loop all compost bins and update the ones that are needed!
        varbit(1057, 0)
        for(compost in FarmingData.compostBin.values()) { /* Compost default values */
            if (farmingJson.getCompostData().get(compost.name) != null) {
                val farmCompost = farmingJson.getCompostData().get(compost.name).asJsonArray
                if(distanceToPoint(compost.updatePos, position) <= 32)
                    updateCompost(farmCompost.get(0).asString,farmCompost.get(1).asString, farmCompost.get(2).asInt)
            }
        }
    }
    fun Client.interactItemBin(objectId : Int, itemId: Int) : Boolean {
        for(compost in FarmingData.compostBin.values()) {
            if(objectId == compost.objectId) {
                val farmCompost = farmingJson.getCompostData().get(compost.name).asJsonArray
                if (itemId == farmData.BUCKET && farmCompost.get(2).asInt > 0 && FarmingData.compostState.OPEN.toString().equals(farmCompost.get(1).asString)) {
                    deleteItem(itemId, 1)
                    addItem(if(farmCompost.get(0).asString.equals(FarmingData.compost.SUPER.toString())) 6034 else 6032, 1)
                    farmCompost.set(2, JsonPrimitive(farmCompost.get(2).asInt - 1))
                    if(farmCompost.get(2).asInt == 0) {
                        farmCompost.set(0, JsonPrimitive(FarmingData.compost.NONE.toString()))
                        farmCompost.set(1, JsonPrimitive(FarmingData.compostState.EMPTY.toString()))
                        farmCompost.set(3, JsonPrimitive(0))
                    }
                    checkItemUpdate()
                    updateCompost(farmCompost.get(0).asString,farmCompost.get(1).asString, farmCompost.get(2).asInt)
                    return true
                }
                if(!FarmingData.compostState.EMPTY.toString().equals(farmCompost.get(1).asString) && !(FarmingData.compostState.FILLED.toString().equals(farmCompost.get(1).asString) && farmCompost.get(2).asInt < 15)) { //If not empty, or filled to the brim...
                        send(SendMessage(if(FarmingData.compostState.CLOSED.toString().equals(farmCompost.get(1).asString)) "The bin is currently in the process of rotting the containment."
                        else if (FarmingData.compostState.FILLED.toString().equals(farmCompost.get(1).asString) && farmCompost.get(2).asInt == 15) "The bin is currently full!"
                        else if (FarmingData.compostState.OPEN.toString().equals(farmCompost.get(1).asString) && farmCompost.get(2).asInt > 0) "Empty the bin before you try and fill it!"
                        else "The bin is done rotting the containment; Perhaps you should open it?"))
                    return false
                }
                if(farmCompost != null && (farmData.regularCompostItems.indexOf(itemId) >= 0 || farmData.superCompostItems.indexOf(itemId) >= 0)) {
                    if((FarmingData.compostState.FILLED.toString().equals(farmCompost.get(1).asString) || FarmingData.compostState.EMPTY.toString().equals(farmCompost.get(1).asString)) && farmCompost.get(2).asInt < 15) {
                        //TODO: Fix a loop of inputting items to the bin!
                        deleteItem(itemId, 1)
                        farmCompost.set(2, JsonPrimitive(farmCompost.get(2).asInt + 1))
                        farmCompost.set(1, JsonPrimitive(FarmingData.compostState.FILLED.toString()))
                        farmCompost.set(0, JsonPrimitive(if(farmData.regularCompostItems.indexOf(itemId) >= 0) FarmingData.compost.REGULAR.toString() else FarmingData.compost.SUPER.toString()))
                        checkItemUpdate()
                        updateCompost(farmCompost.get(0).asString,farmCompost.get(1).asString, farmCompost.get(2).asInt)
                        return true
                    } else if (farmCompost.get(2).asInt == 15) send(SendMessage("The bin is currently full!"))
                } else send(SendMessage("This item has no use to be put into the bin."))
            }
        }
        return false
    }
    fun Client.interactBin(objectId : Int, option : Int) {
        for(compost in FarmingData.compostBin.values()) {
            if (objectId == compost.objectId && option == 1) {
                val farmCompost = farmingJson.getCompostData().get(compost.name).asJsonArray
                if(farmCompost.get(2).asInt == 15 && FarmingData.compostState.FILLED.toString().equals(farmCompost.get(1).asString)) {
                    farmCompost.set(1, JsonPrimitive(FarmingData.compostState.CLOSED.toString()))
                    updateCompost(farmCompost.get(0).asString,farmCompost.get(1).asString, farmCompost.get(2).asInt)
                } else if (FarmingData.compostState.DONE.toString().equals(farmCompost.get(1).asString)) {
                    farmCompost.set(1, JsonPrimitive(FarmingData.compostState.OPEN.toString()))
                    updateCompost(farmCompost.get(0).asString,farmCompost.get(1).asString, farmCompost.get(2).asInt)
                }
            } else if (objectId == compost.objectId && option == 5) {
                //TODO: Fix a compost dump message!
            }
        }
    }
    fun Client.examineBin(pos : Position) {
        for(compost in FarmingData.compostBin.values()) {
            if (compost.updatePos.x == pos.x && compost.updatePos.y == pos.y) {
                val farmCompost = farmingJson.getCompostData().get(compost.name).asJsonArray
                send(SendMessage(if(FarmingData.compostState.CLOSED.toString().equals(farmCompost.get(1).asString)) "The bin is currently in the process of rotting the containment."
                else if(FarmingData.compostState.DONE.toString().equals(farmCompost.get(1).asString)) "The bin is done rotting the containment; Perhaps you should open it?"
                else if(FarmingData.compostState.EMPTY.toString().equals(farmCompost.get(1).asString)) "The bin is currently empty."
                else if (FarmingData.compostState.OPEN.toString().equals(farmCompost.get(1).asString)) "There is currently " + farmCompost.get(2).asString + "/15 of " + farmCompost.get(0).asString.lowercase() + " compost remaining."
                else "There is currently " + farmCompost.get(2).asString + "/15 of " + farmCompost.get(0).asString.lowercase() + " compost filled."))
            }
        }
    }

    fun addFarmValue(farmEnum : String, slot : Int, value : String) { //Going to make it easier to change a value!
        
    }

    fun Client.clickPatch(objectId : Int) {
        val findData = findPatch(objectId)
        if(findData != "null") { //hehe
            send(SendMessage("Value is fine..." + findData))
        }
    }
    fun Client.inspectPatch(objectId : Int) {

    }

    fun Client.loadPatch() {

    }

    fun Client.savePatch() {

    }
    fun findPatch(objectId : Int) : String {
        for (patch in patches.values())
            for (slot in patch.objectId.indices)
                if(patch.objectId[slot] == objectId)
                    return patch.farmData[slot].toString()
        return "null"
    }
    fun Client.updateFarmPatch(compost : String, status: String, amount : Int) {

    }
    fun Client.updateFarmPatch() {

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