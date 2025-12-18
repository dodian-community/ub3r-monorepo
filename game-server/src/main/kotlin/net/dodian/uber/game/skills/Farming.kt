package net.dodian.uber.game.skills

import com.google.gson.JsonPrimitive
import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.skills.FarmingData.patches

class Farming () {
    val farmData = FarmingData()

    fun Client.updateFarming() {
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
        for(patch in FarmingData.patches.values()) { /* Patches default values */
            if (farmingJson.getPatchData().get(patch.name) != null) {
                val farmPatches = farmingJson.getPatchData().get(patch.name).asJsonArray //Not sure if we need just yet!
                (0..patch.objectId.size-1).forEach { slot ->
                    val objectId = patch.objectId[slot]
                    val checkPos = slot * farmingJson.PATCHAMOUNT
                    val growing = findPatch(objectId, 1) == FarmingData.patchState.GROWING.toString() || findPatch(objectId, 1) == FarmingData.patchState.WATER.toString()
                    /* Weed generation */
                    if(findPatch(objectId, 1) == FarmingData.patchState.WEED.toString() && findPatch(objectId, 3).toInt() > 0) {
                        farmPatches.set(checkPos + 4, JsonPrimitive(farmPatches.get(checkPos + 4).asInt + 1))
                        if(farmPatches.get(checkPos + 4).asInt == 3) {
                            farmPatches.set(checkPos + 4, JsonPrimitive(0))
                            farmPatches.set(checkPos + 3, JsonPrimitive(farmPatches.get(checkPos + 3).asInt - 1))
                            updateFarmPatch(false)
                            //System.out.println("I will regenerate weed!")
                        }
                    } else if(findPatch(objectId, 0) != "NONE" && growing && findPatch(objectId, 3).toInt() < 3) { //Growing of plant!
                        farmPatches.set(checkPos + 4, JsonPrimitive(farmPatches.get(checkPos + 4).asInt + 1))
                        if(farmPatches.get(checkPos + 4).asInt == 333) { //333 should be something else!
                            farmPatches.set(checkPos + 4, JsonPrimitive(0))
                            farmPatches.set(checkPos + 3, JsonPrimitive(farmPatches.get(checkPos + 3).asInt + 1))
                            updateFarmPatch(false)
                        }
                    }
                }
            }
        }
    }

    fun compostGrow(status: String) : Boolean {
        return status.equals(FarmingData.compostState.CLOSED.toString(), true)
    }
    fun Client.updateCompost(compost : String, status: String, amount : Int) {
        if(FarmingData.compostState.CLOSED.toString().equals(status))
            varbit(farmData.compostBinConfig, if(FarmingData.compost.REGULAR.toString().equals(compost)) 32 else 65)
        else if(FarmingData.compostState.DONE.toString().equals(status))
            varbit(farmData.compostBinConfig, if(FarmingData.compost.REGULAR.toString().equals(compost)) 31 else 64)
        else if(FarmingData.compostState.OPEN.toString().equals(status))
            varbit(farmData.compostBinConfig, if(FarmingData.compost.REGULAR.toString().equals(compost)) 15 + amount else 47 + amount)
        else if(FarmingData.compostState.FILLED.toString().equals(status))
            varbit(farmData.compostBinConfig, if(FarmingData.compost.REGULAR.toString().equals(compost)) 0 + amount else 32 + amount)
        else varbit(farmData.compostBinConfig, 0)
    }
    fun Client.updateCompost() {
        for(compost in FarmingData.compostBin.values()) { /* Compost default values */
            if (farmingJson.getCompostData().get(compost.name) != null) {
                val farmCompost = farmingJson.getCompostData().get(compost.name).asJsonArray
                if(distanceToPoint(compost.updatePos, position) <= 32) {
                    varbit(farmData.compostBinConfig, 0)
                    updateCompost(farmCompost.get(0).asString,farmCompost.get(1).asString, farmCompost.get(2).asInt)
                }
            }
        }
    }
    fun Client.interactItemBin(objectId : Int, itemId: Int) : Boolean {
        for(compost in FarmingData.compostBin.values()) {
            if(objectId == compost.objectId) {
                val farmCompost = farmingJson.getCompostData().get(compost.name).asJsonArray
                if (itemId == farmData.BUCKET && farmCompost.get(2).asInt > 0 && FarmingData.compostState.OPEN.toString().equals(farmCompost.get(1).asString)) {
                    /*deleteItem(itemId, 1)
                    addItem(if(farmCompost.get(0).asString.equals(FarmingData.compost.SUPER.toString())) 6034 else 6032, 1)
                    farmCompost.set(2, JsonPrimitive(farmCompost.get(2).asInt - 1))
                    if(farmCompost.get(2).asInt == 0) {
                        farmCompost.set(0, JsonPrimitive(FarmingData.compost.NONE.toString()))
                        farmCompost.set(1, JsonPrimitive(FarmingData.compostState.EMPTY.toString()))
                        farmCompost.set(3, JsonPrimitive(0))
                    }
                    checkItemUpdate()
                    updateCompost(farmCompost.get(0).asString,farmCompost.get(1).asString, farmCompost.get(2).asInt)*/
                    interactBin(objectId, 1)
                    return true
                }
                if(!FarmingData.compostState.EMPTY.toString().equals(farmCompost.get(1).asString) && !(FarmingData.compostState.FILLED.toString().equals(farmCompost.get(1).asString) && farmCompost.get(2).asInt < 15)) { //If not empty, or filled to the brim...
                    send(
                        SendMessage(
                            if (FarmingData.compostState.CLOSED.toString()
                                    .equals(farmCompost.get(1).asString)
                            ) "The bin is currently in the process of rotting the containment."
                            else if (FarmingData.compostState.FILLED.toString()
                                    .equals(farmCompost.get(1).asString) && farmCompost.get(2).asInt == 15
                            ) "The bin is currently full!"
                            else if (FarmingData.compostState.OPEN.toString()
                                    .equals(farmCompost.get(1).asString) && farmCompost.get(2).asInt > 0
                            ) "Empty the bin before you try and fill it!"
                            else "The bin is done rotting the containment; Perhaps you should open it?"
                        )
                    )
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
                    } else if (farmCompost.get(2).asInt == 15) send(
                        SendMessage(
                            "The bin is currently full!"
                        )
                    )
                } else send(SendMessage("This item has no use to be put into the bin."))
            }
        }
        /* Item on object for patches */
        for(patch in patches.values()) {
            if (farmingJson.getPatchData().get(patch.name) != null) {
                val farmPatches = farmingJson.getPatchData().get(patch.name).asJsonArray //Not sure if we need just yet!
                (0..patch.objectId.size - 1).forEach { slot ->
                    val checkPos = slot * farmingJson.PATCHAMOUNT
                    if (patch.objectId[slot] == objectId) { //We got the correct objectId!
                        if(findPatch(objectId, 1) == FarmingData.patchState.WEED.toString() && findPatch(objectId, 3).toInt() < 3) {
                            if(itemId == farmData.RAKE) clickPatch(objectId)
                            else send(SendMessage("You need to use a rake in order to clear the weed."))
                            return true
                        } else if (FarmingData.allotmentPatch.equals(findPatch(objectId, 0))) { //Can I water it?
                            clickPatch(objectId)
                        }
                    }
                }
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
                } else if (FarmingData.compostState.OPEN.toString().equals(farmCompost.get(1).asString)) {
                    if(playerHasItem(farmData.BUCKET)) {
                        deleteItem(farmData.BUCKET, 1)
                        addItem(if(farmCompost.get(0).asString.equals(FarmingData.compost.SUPER.toString())) 6034 else 6032, 1)
                        farmCompost.set(2, JsonPrimitive(farmCompost.get(2).asInt - 1))
                        if(farmCompost.get(2).asInt == 0) {
                            farmCompost.set(0, JsonPrimitive(FarmingData.compost.NONE.toString()))
                            farmCompost.set(1, JsonPrimitive(FarmingData.compostState.EMPTY.toString()))
                            farmCompost.set(3, JsonPrimitive(0))
                        }
                        checkItemUpdate()
                        updateCompost(farmCompost.get(0).asString,farmCompost.get(1).asString, farmCompost.get(2).asInt)
                    } else send(SendMessage("You are missing a bucket to be filled with compost."))
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
                send(
                    SendMessage(
                        if (FarmingData.compostState.CLOSED.toString()
                                .equals(farmCompost.get(1).asString)
                        ) "The bin is currently in the process of rotting the containment."
                        else if (FarmingData.compostState.DONE.toString()
                                .equals(farmCompost.get(1).asString)
                        ) "The bin is done rotting the containment; Perhaps you should open it?"
                        else if (FarmingData.compostState.EMPTY.toString()
                                .equals(farmCompost.get(1).asString)
                        ) "The bin is currently empty."
                        else if (FarmingData.compostState.OPEN.toString()
                                .equals(farmCompost.get(1).asString)
                        ) "There is currently " + farmCompost.get(2).asString + "/15 of " + farmCompost.get(0).asString.lowercase() + " compost remaining."
                        else "There is currently " + farmCompost.get(2).asString + "/15 of " + farmCompost.get(0).asString.lowercase() + " compost filled."
                    )
                )
            }
        }
    }

    fun Client.clickPatch(objectId : Int) : Boolean {
        for(patch in patches.values()) {
            if (farmingJson.getPatchData().get(patch.name) != null) {
                val farmPatches = farmingJson.getPatchData().get(patch.name).asJsonArray //Not sure if we need just yet!
                (0..patch.objectId.size - 1).forEach { slot ->
                    val checkPos = slot * farmingJson.PATCHAMOUNT
                    if (patch.objectId[slot] == objectId) { //We got the correct objectId!
                        if(findPatch(objectId, 1) == FarmingData.patchState.WEED.toString() && findPatch(objectId, 3).toInt() < 3) {
                            if(playerHasItem(farmData.RAKE)) {
                                farmPatches.set(checkPos + 3, JsonPrimitive(farmPatches.get(checkPos + 3).asInt + 1))
                                sendAnimation(farmData.RAKE_ANIM) //<- Confirm if the stuff works!
                                if(hasSpace()) {
                                    addItem(farmData.regularCompostItems[0], 1)
                                    checkItemUpdate()
                                }
                                updateFarmPatch(false)
                            } else send(SendMessage("You need a rake in order to clear the weed."))
                        } else if (FarmingData.allotmentPatch.equals(findPatch(objectId, 0))) { //Can I water it?
                            System.out.println("I can water the..." + findPatch(objectId, 0))
                        }
                    }
                }
            }
        }
        return false
    }
    fun Client.inspectPatch(objectId : Int) {
        val findData = findPatch(objectId, 0)
        var stage = 0
        var endStage = 0
        var growing = false
        var compost = "NONE"
        /* Patch check */
        for (patch in patches.values()) {
            val slot = patch.objectId.indexOf(objectId)
            if(slot != -1) {
                val farmPatch = farmingJson.getPatchData().get(patch.name).asJsonArray
                stage = farmPatch.get((slot * farmingJson.PATCHAMOUNT) + 3).asInt
                endStage = 0
                growing = farmPatch.get((slot * farmingJson.PATCHAMOUNT) + 1).asString.equals(FarmingData.patchState.GROWING.toString())
                compost = farmPatch.get((slot * farmingJson.PATCHAMOUNT) + 2).asString
                //return farmPatch.get((slot * farmingJson.PATCHAMOUNT) + value).asString
            }
        }

        if(!findData.isEmpty()) { //Value to check for a patch!
            val messageOne = "This is a " + GameObjectData.forId(objectId).name.lowercase() +"."
            val messageTwo = if(compost == "NONE") "The soil has not been treated." else "The soil has been treated."
            val messageThree = if(stage < 3 && !growing) "The patch needs weeding." else if(growing) "Currently (" + stage + "/(" + endStage + ")" else "The patch is empty."
            send(SendMessage(messageOne + " " + messageTwo + " " + messageThree))
        }
    }
    fun Client.findPatch(objectId : Int, value : Int) : String {
        if(value >= 6) return "" //Cant have a value beyond 6!
        for (patch in patches.values()) {
            val slot = patch.objectId.indexOf(objectId)
            if(slot != -1) {
                val farmPatch = farmingJson.getPatchData().get(patch.name).asJsonArray
                return farmPatch.get((slot * farmingJson.PATCHAMOUNT) + value).asString
            }
        }
        return ""
    }
    fun Client.updateFarmPatch(patch : FarmingData.patches) {
        if (farmingJson.getPatchData().get(patch.name) != null) {
            (0 until patch.objectId.size).forEach { slot ->
                val checkPos = slot * farmingJson.PATCHAMOUNT
                val farmPatch = farmingJson.getPatchData().get(patch.name).asJsonArray
                val enumText = farmPatch.get(checkPos).asString
                val startConfig = if(enumText == "NONE") 0 else 5
                //TODO: Fix this -> val change = if(farmPatch.get(checkPos + 1).asString == "WATER") 1 else if (farmPatch.get(checkPos + 1).asString == "DISEASE") 2
                //else if (farmPatch.get(checkPos + 1).asString == "DEAD") 3 else 0
                val stage = farmPatch.get(checkPos + 3).asInt
                varbit(farmData.farmPatchConfig + slot, startConfig + stage)
            }
        }
    }
    fun Client.updateFarmPatch(refresh : Boolean) {
        for(patch in FarmingData.patches.values()) {
            if (farmingJson.getPatchData().get(patch.name) != null) {
                if(distanceToPoint(patch.updatePos, position) <= 32) {
                    for(i in 0..3)
                        if(refresh) varbit(farmData.farmPatchConfig + i, 0)
                    updateFarmPatch(patch)
                }
            }
        }
    }
    fun findPatch(enum : String) : Nothing? {
        var check = null
        //check = FarmingData.fruitTreePatch.findEnum(enum) as Nothing?
        //check = FarmingData.treePatch.findEnum(enum) as Nothing?
        check = FarmingData.allotmentPatch.findEnum(enum) as Nothing?
        System.out.println("hmm..." + check)
        return check
    }

    fun Client.saplingMaking(itemOne : Int, itemOneSlot : Int, itemTwo : Int, itemTwoSlot : Int) {
        for (sapling in FarmingData.sapling.values()) {
            if((itemOne == sapling.treeSeed || itemTwo == sapling.treeSeed) && (itemOne == 5356 || itemTwo == 5356)) {
                if(!playerHasItem(5325)) {
                    send(SendMessage("You are missing your gardening trowel."))
                    return;
                }
                if(getSkillLevel(Skill.FARMING) < sapling.farmLevel) {
                    send(
                        SendMessage(
                            "You need level " + sapling.farmLevel + " farming to plant the " + GetItemName(
                                sapling.treeSeed
                            ).lowercase() + "."
                        )
                    )
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