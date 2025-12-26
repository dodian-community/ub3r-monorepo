package net.dodian.uber.game.skills

import com.google.gson.JsonPrimitive
import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.skills.FarmingData.patches
import net.dodian.utilities.Misc

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
                }
            }
        }
        /* Farming Patches */
        for(patch in patches.values()) { /* Patches default values */
            if (farmingJson.getPatchData().get(patch.name) != null) {
                val farmPatches = farmingJson.getPatchData().get(patch.name).asJsonArray //Not sure if we need just yet!
                (0..patch.objectId.size-1).forEach { slot ->
                    val objectId = patch.objectId[slot]
                    val itemId = findPatch(objectId, 0).toInt()
                    val checkPos = slot * farmingJson.PATCHAMOUNT
                    /* Weed generation */
                    if(findPatch(objectId, 1) == FarmingData.patchState.WEED.toString() && findPatch(objectId, 3).toInt() > 0) {
                        farmPatches.set(checkPos + 4, JsonPrimitive(farmPatches.get(checkPos + 4).asInt + 1))
                        if(farmPatches.get(checkPos + 4).asInt == 3) {
                            farmPatches.set(checkPos + 4, JsonPrimitive(0))
                            farmPatches.set(checkPos + 3, JsonPrimitive(farmPatches.get(checkPos + 3).asInt - 1))
                            updateFarmPatch()
                        }
                    } else if(itemId != -1 && plantGrow(findPatch(objectId, 1)) && farmPatches.get(checkPos + 3).asInt < farmData.getEndStage(itemId)) { //Growing of plant!
                            farmPatches.set(checkPos + 4, JsonPrimitive(farmPatches.get(checkPos + 4).asInt + 1))
                            if(farmPatches.get(checkPos + 4).asInt == farmData.getGrowTick(itemId)) {
                                if(findPatch(objectId, 1) == "WATER") //If we watering the plant, set to growing again!
                                    farmPatches.set(checkPos + 1, JsonPrimitive(FarmingData.patchState.GROWING.toString()))
                                farmPatches.set(checkPos + 4, JsonPrimitive(0))
                                farmPatches.set(checkPos + 3, JsonPrimitive(farmPatches.get(checkPos + 3).asInt + 1))
                                /* TODO: Fix death + disease chance! */
                                val chance = Misc.chance(1024)
                                if (farmPatches.get(checkPos + 3).asInt == farmData.getEndStage(itemId)) { //full grown
                                    farmPatches.set(checkPos + 1, JsonPrimitive(FarmingData.patchState.HARVEST.toString()))
                                    farmPatches.set(checkPos + 3, JsonPrimitive(farmData.getEndStage(itemId) + 2)) //3 lifes, but we can change it in the future :D
                                } else if(chance > farmData.getDiseaseChance(itemId) && findPatch(objectId, 1) != "PROTECTED") //Disease here?
                                    farmPatches.set(checkPos + 1, JsonPrimitive(FarmingData.patchState.DISEASE.toString()))
                                updateFarmPatch()
                        }
                    } else if(itemId != -1 && findPatch(objectId, 1) == "DISEASE") {
                        farmPatches.set(checkPos + 4, JsonPrimitive(farmPatches.get(checkPos + 4).asInt + 1))
                        if(farmPatches.get(checkPos + 4).asInt == farmData.getGrowTick(itemId) * 2) { //Dead patch!
                            farmPatches.set(checkPos + 4, JsonPrimitive(0))
                            farmPatches.set(checkPos + 1, JsonPrimitive(FarmingData.patchState.DEAD.toString()))
                            updateFarmPatch()
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
        if(FarmingData.compostState.CLOSED.toString() == status)
            varbit(farmData.compostBinConfig, if(FarmingData.compost.COMPOST.toString().equals(compost)) 32 else 65)
        else if(FarmingData.compostState.DONE.toString() == status)
            varbit(farmData.compostBinConfig, if(FarmingData.compost.COMPOST.toString().equals(compost)) 31 else 64)
        else if(FarmingData.compostState.OPEN.toString() == status)
            varbit(farmData.compostBinConfig, if(FarmingData.compost.COMPOST.toString().equals(compost)) 15 + amount else 47 + amount)
        else if(FarmingData.compostState.FILLED.toString() == status)
            varbit(farmData.compostBinConfig, if(FarmingData.compost.COMPOST.toString().equals(compost)) 0 + amount else 32 + amount)
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
        val objName = GameObjectData.forId(objectId).name.lowercase()
        val itemName = GetItemName(itemId).lowercase()

        for(compost in FarmingData.compostBin.values()) {
            if(objectId == compost.objectId) {
                val farmCompost = farmingJson.getCompostData().get(compost.name).asJsonArray
                if (itemId == farmData.BUCKET && farmCompost.get(2).asInt > 0 && FarmingData.compostState.OPEN.toString().equals(farmCompost.get(1).asString)) {
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
                        farmCompost.set(0, JsonPrimitive(if(farmData.regularCompostItems.indexOf(itemId) >= 0) FarmingData.compost.COMPOST.toString() else FarmingData.compost.SUPER.toString()))
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
                            if (itemId == farmData.RAKE) clickPatch(objectId)
                            else send(SendMessage("You need to use a rake in order to clear the weed."))
                            return true
                        } else if (findPatch(objectId, 1) == FarmingData.patchState.DEAD.toString()) { //Do dead stuff!
                            if (itemId == farmData.SPADE) clickPatch(objectId)
                            else send(SendMessage("You need to use a spade in order to clear the dead plant."))
                            return true
                        } else if((objName == "allotment" || objName == "flower patch") && itemName.startsWith("watering can(")) {
                            if (findPatch(objectId, 1) == FarmingData.patchState.GROWING.toString()) {
                                sendAnimation(farmData.WATERCAN_ANIM)
                                deleteItem(itemId, 1)
                                addItem(if (itemName.endsWith("1)")) 5331 else itemId - 1, 1)
                                farmPatches.set(checkPos + 1, JsonPrimitive(FarmingData.patchState.WATER.toString()))
                                checkItemUpdate()
                                updateFarmPatch()
                            } else send(SendMessage(if(findPatch(objectId, 1) == FarmingData.patchState.PROTECTED.toString()) "The plant is already protected so no need to water the plant." else "The plant do not need watering."))
                        }
                        else if (itemName.endsWith("compost")) {
                            val currentCompost = FarmingData.compost.find(findPatch(objectId, 2))?.ordinal ?: 0
                            /* Loop values! */
                            for(compost in FarmingData.compost.values()) {
                                if(itemId == compost.itemId && currentCompost < compost.ordinal) {
                                    sendAnimation(farmData.COMPOST_PATCH_ANIM)
                                    deleteItem(itemId, 1)
                                    addItem(1925, 1)
                                    farmPatches.set(checkPos + 2, JsonPrimitive(compost.name))
                                    checkItemUpdate()
                                } else if (itemId == compost.itemId) {
                                    send(SendMessage("There is no point in using " + itemName + " when the patch already got " + findPatch(objectId, 2).lowercase() + ""+if(findPatch(objectId, 2) != "COMPOST") "compost." else "."))
                                }
                            }
                        } else if (findPatch(objectId, 0).toInt() == -1) {
                            val checkName = objName.replace(" patch", "")
                            var foundStage = 0
                            if(FarmingData.allotmentPatch.findSeed(itemId)) { //First check is always true!
                                foundStage = if (checkName == "allotment") 1 else 2
                            } else if(FarmingData.flowerPatch.findSeed(itemId)) {
                                foundStage = if (checkName == "flower") 1 else 2
                            } else if(FarmingData.herbPatch.findSeed(itemId)) {
                                foundStage = if (checkName == "herb") 1 else 2
                            }
                            val value = if(FarmingData.allotmentPatch.findSeed(itemId)) "allotment"
                            else if (FarmingData.flowerPatch.findSeed(itemId)) "flower"
                            else if (FarmingData.herbPatch.findSeed(itemId)) "herb" else ""
                            if(foundStage == 2)
                                send(SendMessage(GetItemName(itemId) + " can only be planted at a " + value + " patch."))
                            else if(foundStage == 1) { //Found seed on right patch, eh?
                                if (getLevel(Skill.FARMING) >= farmData.getPlantLevel(itemId)) {
                                    val amount = if (checkName == "allotment") 3 else 1
                                    if (playerHasItem(itemId, amount)) {
                                        sendAnimation(farmData.PLANTSEED_ANIM)
                                        deleteItem(itemId, amount)
                                        farmPatches.set(checkPos + 0, JsonPrimitive(itemId))
                                        farmPatches.set(checkPos + 1, JsonPrimitive(FarmingData.patchState.GROWING.toString()))
                                        farmPatches.set(checkPos + 3, JsonPrimitive(1))
                                        farmPatches.set(checkPos + 4, JsonPrimitive(0))
                                        farmPatches.set(checkPos + 5, JsonPrimitive(System.currentTimeMillis()))
                                        checkItemUpdate()
                                        updateFarmPatch()
                                    } else send(SendMessage("You need " + amount + " " + GetItemName(itemId).lowercase() + " to plant here."))
                                } else send(SendMessage("You need level " + farmData.getPlantLevel(itemId) + " farming to plant " + GetItemName(itemId).lowercase() + "."))
                            }
                        } else clickPatch(objectId)
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

    fun plantGrow(status: String) : Boolean {
    return status.equals(FarmingData.patchState.GROWING.toString(), true)
        || status.equals(FarmingData.patchState.WATER.toString(), true)
        || status.equals(FarmingData.patchState.PROTECTED.toString(), true)
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
                                updateFarmPatch()
                            } else send(SendMessage("You need a rake in order to clear the weed."))
                        } else if (findPatch(objectId, 1) == FarmingData.patchState.DEAD.toString()) {
                            if(playerHasItem(farmData.SPADE)) {
                                sendAnimation(farmData.SPADE_ANIM)
                                farmPatches.set(checkPos + 0, JsonPrimitive(-1))
                                farmPatches.set(checkPos + 1, JsonPrimitive(FarmingData.patchState.WEED.toString()))
                                farmPatches.set(checkPos + 2, JsonPrimitive(FarmingData.compost.NONE.toString()))
                                farmPatches.set(checkPos + 3, JsonPrimitive(3))
                                farmPatches.set(checkPos + 4, JsonPrimitive(0))
                                updateFarmPatch()
                            } else send(SendMessage("You need to use a spade in order to clear the dead plant."))
                        } else if (findPatch(objectId, 1) == FarmingData.patchState.DISEASE.toString()) {
                            if(playerHasItem(farmData.PLANT_CURE)) {
                                sendAnimation(farmData.CURING_ANIM)
                                deleteItem(farmData.PLANT_CURE, 1)
                                farmPatches.set(checkPos + 1, JsonPrimitive(FarmingData.patchState.GROWING.toString()))
                                farmPatches.set(checkPos + 4, JsonPrimitive(0))
                                updateFarmPatch()
                                checkItemUpdate()
                            } else send(SendMessage("You need to use a spade in order to clear the dead plant."))
                        }
                    }
                }
            }
        }
        return false
    }
    fun Client.inspectPatch(objectId : Int) {
        val findData = findPatch(objectId, 0).toInt()
        var stage: Int
        var endStage: Int
        var weeding: Boolean
        var disease: Boolean
        var dead: Boolean
        var compost: String
        /* Patch check */
        for (patch in patches.values()) {
            val slot = patch.objectId.indexOf(objectId)
            if(slot != -1) {
                val farmPatch = farmingJson.getPatchData().get(patch.name).asJsonArray
                stage = farmPatch.get((slot * farmingJson.PATCHAMOUNT) + 3).asInt
                endStage = farmData.getEndStage(findData)
                weeding = farmPatch.get((slot * farmingJson.PATCHAMOUNT) + 1).asString.equals(FarmingData.patchState.WEED.toString())
                disease = farmPatch.get((slot * farmingJson.PATCHAMOUNT) + 1).asString.equals(FarmingData.patchState.DISEASE.toString())
                dead = farmPatch.get((slot * farmingJson.PATCHAMOUNT) + 1).asString.equals(FarmingData.patchState.DEAD.toString())
                compost = farmPatch.get((slot * farmingJson.PATCHAMOUNT) + 2).asString

                val messageOne = "This is a " + GameObjectData.forId(objectId).name.lowercase() +"."
                val messageTwo = if(compost == "NONE") "The soil has not been treated." else if(compost == "PROTECTED") "This patch is protected." else "The soil has been treated with "+compost.lowercase()+"."
                val messageThree = if(stage < 3 && weeding) "The patch needs weeding." else if(findData == -1) "The patch is empty." else if (dead) "Patch is dead!"
                else if (disease) "Currently diseased, you should try and cure it." else "Currently growning (" + stage + "/" + endStage + ")"
                send(SendMessage(messageOne + " " + messageTwo + " " + messageThree))
            }
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
    fun Client.updateFarmPatch(patch : patches) {
        if (farmingJson.getPatchData().get(patch.name) != null) {
            (0 until patch.objectId.size).forEach { slot ->
                val checkPos = slot * farmingJson.PATCHAMOUNT
                val objectId = patch.objectId[slot]
                val farmPatch = farmingJson.getPatchData().get(patch.name).asJsonArray
                val itemId = farmPatch.get(checkPos).asInt
                val startConfig = farmData.getPatchConfig(itemId)
                val stage = farmPatch.get(checkPos + 3).asInt
                var config = startConfig + stage

                if(findPatch(objectId, 1) == FarmingData.patchState.WATER.toString())
                    config = config or (1 shl 6)
                else if(findPatch(objectId, 1) == FarmingData.patchState.DISEASE.toString())
                    config = config or (2 shl 6)
                else if(findPatch(objectId, 1) == FarmingData.patchState.DEAD.toString())
                    config = config or (3 shl 6)
                /* Special dead for herb! */
                if(findPatch(objectId, 1) == FarmingData.patchState.DEAD.toString() && FarmingData.herbPatch.find(itemId) != null) //Herb!
                    config = 168 + stage
                if(findPatch(objectId, 1) == FarmingData.patchState.DISEASE.toString() && FarmingData.herbPatch.find(itemId) != null) { //Herb!
                    val position = FarmingData.herbPatch.find(itemId)?.ordinal ?: -1
                    config -= if (position > 7) 5 + position * 4 + 8 else 5 + position * 4
                }
                varbit(farmData.farmPatchConfig + slot, config)
            }
        }
    }
    fun Client.updateFarmPatch() {
        for(patch in patches.values()) {
            if (farmingJson.getPatchData().get(patch.name) != null) {
                if(distanceToPoint(patch.updatePos, position) <= 16)
                    updateFarmPatch(patch)
            }
        }
    }

    fun Client.saplingMaking(itemOne : Int, itemOneSlot : Int, itemTwo : Int, itemTwoSlot : Int) {
        for (sapling in FarmingData.sapling.values()) {
            if((itemOne == sapling.treeSeed || itemTwo == sapling.treeSeed) && (itemOne == 5356 || itemTwo == 5356)) {
                if(!playerHasItem(farmData.TROWEL)) {
                    send(SendMessage("You are missing your "+GetItemName(farmData.TROWEL).lowercase()+"."))
                    return;
                }
                if(getSkillLevel(Skill.FARMING) < sapling.farmLevel) {
                    send(SendMessage( "You need level " + sapling.farmLevel + " " +
                    "farming to plant the " + GetItemName(sapling.treeSeed).lowercase() + "."))
                    return;
                }
                deleteItem(itemOne, if(itemOne == farmData.PLANT_POT) itemOneSlot else itemTwoSlot,  1)
                deleteItem(itemTwo, if(itemOne == farmData.PLANT_POT) itemTwoSlot else itemOneSlot, 1)
                addItemSlot(sapling.plantedId, 1, if(itemOne == farmData.PLANT_POT) itemOneSlot else itemTwoSlot)
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