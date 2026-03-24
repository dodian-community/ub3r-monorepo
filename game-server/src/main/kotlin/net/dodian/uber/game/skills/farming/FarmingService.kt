package net.dodian.uber.game.skills.farming

import com.google.gson.JsonArray
import com.google.gson.JsonPrimitive
import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.skills.farming.FarmingDefinitions.patches
import net.dodian.uber.game.runtime.world.farming.FarmingScheduler
import net.dodian.utilities.Misc

class FarmingService {
    val farmData = FarmingDefinitions()

    fun Client.updateFarming() {
        /* Saplings in inventory + bank! */
        for (sapling in FarmingDefinitions.sapling.values()) {
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
        for(compost in FarmingDefinitions.compostBin.values()) { /* Compost default values */
            if (farmingJson.getCompostData().get(compost.name) != null) {
                val farmCompost = farmingJson.getCompostData().get(compost.name).asJsonArray
                if(compostGrow(farmCompost.get(1).asString)) {
                    farmCompost.set(3, JsonPrimitive(farmCompost.get(3).asInt + 1))
                    if(farmCompost.get(3).asInt >= compost.ticks) {
                        farmCompost.set(1, JsonPrimitive(FarmingDefinitions.compostState.DONE.toString()))
                        updateCompost(farmCompost.get(0).asString,farmCompost.get(1).asString, farmCompost.get(2).asInt)
                        markFarmingDirty()
                    }
                }
            }
        }
        /* Farming Patches */
        for(patch in patches.values()) { /* Patches default values */
            if (farmingJson.getPatchData().get(patch.name) != null) {
                val farmPatches = farmingJson.getPatchData().get(patch.name).asJsonArray //Not sure if we need just yet!
                (0 until patch.objectId.size).forEach { slot ->
                    val objectId = patch.objectId[slot]
                    val itemId = findPatch(objectId, 0).toInt()
                    val checkPos = slot * farmingJson.PATCHAMOUNT
                    /* Weed generation */
                    if(findPatch(objectId, 1) == FarmingDefinitions.patchState.WEED.toString() && findPatch(objectId, 3).toInt() > 0) {
                        farmPatches.set(checkPos + 4, JsonPrimitive(farmPatches.get(checkPos + 4).asInt + 1))
                        if(farmPatches.get(checkPos + 4).asInt == 3) {
                            farmPatches.set(checkPos + 4, JsonPrimitive(0))
                            farmPatches.set(checkPos + 3, JsonPrimitive(farmPatches.get(checkPos + 3).asInt - 1))
                            updateFarmPatch()
                            markFarmingDirty()
                        }
                    } else if(itemId != -1 && plantGrow(findPatch(objectId, 1)) && farmPatches.get(checkPos + 3).asInt < farmData.getEndStage(itemId)) { //Growing of plant!
                            farmPatches.set(checkPos + 4, JsonPrimitive(farmPatches.get(checkPos + 4).asInt + 1))
                            if(farmPatches.get(checkPos + 4).asInt >= farmData.getGrowTick(itemId)) {
                                var diseaseChance = if(findPatch(objectId, 1) == "WATER") farmData.getDiseaseChance(itemId) * 0.9 else farmData.getDiseaseChance(itemId)
                                /* Change status on patch */
                                if(findPatch(objectId, 1) == "WATER") //Change status due to water!
                                    farmPatches.set(checkPos + 1, JsonPrimitive(FarmingDefinitions.patchState.GROWING.toString()))
                                farmPatches.set(checkPos + 4, JsonPrimitive(0))
                                farmPatches.set(checkPos + 3, JsonPrimitive(farmPatches.get(checkPos + 3).asInt + 1))
                                /* Disease checks */
                                val chance = Misc.chance(1024)
                                val divideValue = FarmingDefinitions.compost.find(farmPatches.get(checkPos + 2).asString)?.divideValue ?: 1
                                diseaseChance = diseaseChance.toDouble() * (1.0 - ((getLevel(Skill.FARMING).toDouble() / 2.85) / 100))
                                diseaseChance /= divideValue //Compost value decrease :D
                                //System.out.println("Test.." + farmData.getDiseaseChance(itemId) + ", " + chance + ", " + diseaseChance)
                                if (farmPatches.get(checkPos + 3).asInt == farmData.getEndStage(itemId)) { //full grown
                                    farmPatches.set(checkPos + 1, JsonPrimitive(FarmingDefinitions.patchState.HARVEST.toString()))
                                    val life = farmData.getLife(itemId, FarmingDefinitions.compost.find(farmPatches.get(checkPos + 2).asString)?.ordinal ?: 0)
                                    farmPatches.set(checkPos + 3, JsonPrimitive(life))
                                } else if(chance <= diseaseChance && findPatch(objectId, 1) != "PROTECTED") //Disease here?
                                    farmPatches.set(checkPos + 1, JsonPrimitive(FarmingDefinitions.patchState.DISEASE.toString()))
                                updateFarmPatch()
                                markFarmingDirty()
                        }
                    } else if(itemId != -1 && findPatch(objectId, 1) == FarmingDefinitions.patchState.STUMP.toString()) { //Growing of production of a patch!
                        farmPatches.set(checkPos + 4, JsonPrimitive(farmPatches.get(checkPos + 4).asInt + 1))
                        if(farmPatches.get(checkPos + 4).asInt >= farmData.getGrowTick(itemId) / 3) {
                            farmPatches.set(checkPos + 4, JsonPrimitive(0))
                            farmPatches.set(checkPos + 1, JsonPrimitive(FarmingDefinitions.patchState.PRODUCTION.toString()))
                            updateFarmPatch()
                            markFarmingDirty()
                        }
                    } else if(itemId != -1 && findPatch(objectId, 1) == FarmingDefinitions.patchState.PRODUCTION.toString()) { //Growing of production of a patch!
                        val life = if(FarmingDefinitions.treePatch.find(itemId) != null) 1 else if (FarmingDefinitions.bushPatch.find(itemId) != null) 4 else 6
                        if(farmPatches.get(checkPos + 3).asInt < life) {
                            farmPatches.set(checkPos + 4, JsonPrimitive(farmPatches.get(checkPos + 4).asInt + 1))
                            if(farmPatches.get(checkPos + 4).asInt >= farmData.getGrowTick(itemId) / 3) {
                                farmPatches.set(checkPos + 4, JsonPrimitive(0))
                                farmPatches.set(checkPos + 3, JsonPrimitive(farmPatches.get(checkPos + 3).asInt + 1))
                                updateFarmPatch()
                                markFarmingDirty()
                            }
                        }
                    } else if(itemId != -1 && findPatch(objectId, 1) == "DISEASE") {
                        farmPatches.set(checkPos + 4, JsonPrimitive(farmPatches.get(checkPos + 4).asInt + 1))
                        if(farmPatches.get(checkPos + 4).asInt == farmData.getGrowTick(itemId) * 2) { //Dead patch!
                            farmPatches.set(checkPos + 4, JsonPrimitive(0))
                            farmPatches.set(checkPos + 1, JsonPrimitive(FarmingDefinitions.patchState.DEAD.toString()))
                            updateFarmPatch()
                            markFarmingDirty()
                        }
                    }
                }
            }
        }
    }

    fun compostGrow(status: String) : Boolean {
        return status.equals(FarmingDefinitions.compostState.CLOSED.toString(), true)
    }
    fun Client.updateCompost(compost : String, status: String, amount : Int) {
        when (status) {
            FarmingDefinitions.compostState.CLOSED.toString() -> varbit(
                farmData.compostBinConfig,
                if (compost == FarmingDefinitions.compost.COMPOST.toString()) 32 else 65
            )

            FarmingDefinitions.compostState.DONE.toString() -> varbit(
                farmData.compostBinConfig,
                if (compost == FarmingDefinitions.compost.COMPOST.toString()) 31 else 64
            )

            FarmingDefinitions.compostState.OPEN.toString() -> varbit(
                farmData.compostBinConfig,
                if (compost == FarmingDefinitions.compost.COMPOST.toString()) 15 + amount else 47 + amount
            )

            FarmingDefinitions.compostState.FILLED.toString() -> varbit(
                farmData.compostBinConfig,
                if (compost == FarmingDefinitions.compost.COMPOST.toString()) 0 + amount else 32 + amount
            )

            else -> varbit(farmData.compostBinConfig, 0)
        }
    }
    fun Client.updateCompost() {
        for(compost in FarmingDefinitions.compostBin.values()) { /* Compost default values */
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

        for(compost in FarmingDefinitions.compostBin.values()) {
            if(objectId == compost.objectId) {
                val farmCompost = farmingJson.getCompostData().get(compost.name).asJsonArray
                if (itemId == farmData.BUCKET && farmCompost.get(2).asInt > 0 && FarmingDefinitions.compostState.OPEN.toString() == farmCompost.get(1).asString) {
                    interactBin(objectId, 1)
                    return true
                } else if (itemId == farmData.VOLCANIC_ASH && farmCompost.get(2).asInt == 15 && FarmingDefinitions.compostState.OPEN.toString() == farmCompost.get(1).asString) {
                    if(playerHasItem(farmData.VOLCANIC_ASH, 25)) {
                        deleteItem(farmData.VOLCANIC_ASH, 25)
                        farmCompost.set(0, JsonPrimitive(FarmingDefinitions.compost.ULTRACOMPOST.toString()))
                        checkItemUpdate()
                        markFarmingDirty()
                    } else send(SendMessage("You need 25 "+GetItemName(itemId).lowercase()+" in order to convert into ultra compost."))
                    return true
                }
                if(farmCompost.get(1).asString != FarmingDefinitions.compostState.EMPTY.toString() && !(farmCompost.get(1).asString == FarmingDefinitions.compostState.FILLED.toString() && farmCompost.get(2).asInt < 15)) { //If not empty, or filled to the brim...
                    send(
                        SendMessage(
                            if (FarmingDefinitions.compostState.CLOSED.toString() == farmCompost.get(1).asString)
                            "The bin is currently in the process of rotting the containment."
                            else if (FarmingDefinitions.compostState.FILLED.toString() == farmCompost.get(1).asString && farmCompost.get(2).asInt == 15)
                            "The bin is currently full!"
                            else if (FarmingDefinitions.compostState.OPEN.toString() == farmCompost.get(1).asString && farmCompost.get(2).asInt > 0)
                            "Empty the bin before you try and fill it!"
                            else "The bin is done rotting the containment; Perhaps you should open it?"
                        )
                    )
                    return false
                }
                if(farmCompost != null && (farmData.checkSuperCompost(itemId) || farmData.checkRegularCompost(itemId))) {
                    if((FarmingDefinitions.compostState.FILLED.toString() == farmCompost.get(1).asString || FarmingDefinitions.compostState.EMPTY.toString() == farmCompost.get(1).asString) && farmCompost.get(2).asInt < 15) {
                        //TODO: Fix a loop of inputting items to the bin!
                        if(FarmingDefinitions.compostState.EMPTY.toString() != farmCompost.get(1).asString && farmData.checkSuperCompost(itemId) && farmCompost.get(0).asString != FarmingDefinitions.compost.SUPERCOMPOST.toString()) {
                            send(SendMessage("This will be redundant if you use super compost item on a regular compost."))
                            return true
                        }  else if(farmData.checkRegularCompost(itemId) && farmCompost.get(0).asString == FarmingDefinitions.compost.SUPERCOMPOST.toString()) {
                            send(SendMessage("You cant do this as it will convert to regular compost!"))
                            return true
                        }
                        deleteItem(itemId, 1)
                        farmCompost.set(2, JsonPrimitive(farmCompost.get(2).asInt + 1))
                        farmCompost.set(1, JsonPrimitive(FarmingDefinitions.compostState.FILLED.toString()))
                        farmCompost.set(0, JsonPrimitive(if(farmData.regularCompostItems.indexOf(itemId) >= 0) FarmingDefinitions.compost.COMPOST.toString() else FarmingDefinitions.compost.SUPERCOMPOST.toString()))
                        checkItemUpdate()
                        updateCompost(farmCompost.get(0).asString,farmCompost.get(1).asString, farmCompost.get(2).asInt)
                        markFarmingDirty()
                        FarmingScheduler.INSTANCE.noteActivity(this)
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
                (0 until patch.objectId.size).forEach { slot ->
                    val checkPos = slot * farmingJson.PATCHAMOUNT
                    if (patch.objectId[slot] == objectId) { //We got the correct objectId!
                        if(findPatch(objectId, 1) == FarmingDefinitions.patchState.WEED.toString() && findPatch(objectId, 3).toInt() < 3) {
                            if (itemId == farmData.RAKE) clickPatch(objectId)
                            else send(SendMessage("You need to use a rake in order to clear the weed."))
                            return true
                        } else if (findPatch(objectId, 1) == FarmingDefinitions.patchState.DEAD.toString()) { //Do dead stuff!
                            if (itemId == farmData.SPADE) clickPatch(objectId)
                            else send(SendMessage("You need to use a spade in order to clear the dead plant."))
                            return true
                        } else if (findPatch(objectId, 1) == FarmingDefinitions.patchState.STUMP.toString()) { //Do dead stuff!
                            if (itemId == farmData.SPADE) clickPatch(objectId)
                            else send(SendMessage("You need to use a spade in order to clear the stump from the plant."))
                            return true
                        } else if (findPatch(objectId, 1) == FarmingDefinitions.patchState.HARVEST.toString()) { //Do dead stuff!
                            if (itemId == farmData.SPADE) clickPatch(objectId)
                            else send(SendMessage("You need to use a spade in order to harvest the patch."))
                            return true
                        } else if (findPatch(objectId, 1) == FarmingDefinitions.patchState.DISEASE.toString() && !objName.contains("tree") && !objName.contains("bush")) {
                            if (itemId == farmData.PLANT_CURE) clickPatch(objectId)
                            else send(SendMessage("You need to use a plant cure in order to cure the patch."))
                            return true
                        } else if (findPatch(objectId, 1) == FarmingDefinitions.patchState.DISEASE.toString() && (objName.contains("tree") || objName.contains("bush"))) {
                            if (itemId == farmData.SECATEURS || itemId == farmData.MAGIC_SECATEURS) clickPatch(objectId)
                            else send(SendMessage("You need to use a pair of secateurs in order to prune the patch."))
                            return true
                        } else if((objName == "allotment" || objName == "flower patch") && itemName.startsWith("watering can(")) {
                            if (findPatch(objectId, 1) == FarmingDefinitions.patchState.GROWING.toString()) {
                                sendAnimation(farmData.WATERCAN_ANIM)
                                deleteItem(itemId, 1)
                                addItem(if (itemName.endsWith("1)")) 5331 else itemId - 1, 1)
                                farmPatches.set(checkPos + 1, JsonPrimitive(FarmingDefinitions.patchState.WATER.toString()))
                                checkItemUpdate()
                                updateFarmPatch()
                                markFarmingDirty()
                                FarmingScheduler.INSTANCE.noteActivity(this)
                            } else send(SendMessage(if(findPatch(objectId, 1) == FarmingDefinitions.patchState.PROTECTED.toString()) "The plant is already protected so no need to water the plant." else "The plant do not need watering."))
                        }
                        else if (itemName.endsWith("compost")) {
                            val currentCompost = FarmingDefinitions.compost.find(findPatch(objectId, 2))?.ordinal ?: 0
                            /* Loop values! */
                            for(compost in FarmingDefinitions.compost.values()) {
                                if(itemId == compost.itemId && currentCompost < compost.ordinal) {
                                    sendAnimation(farmData.COMPOST_PATCH_ANIM)
                                    deleteItem(itemId, 1)
                                    addItem(1925, 1)
                                    farmPatches.set(checkPos + 2, JsonPrimitive(compost.name))
                                    checkItemUpdate()
                                    markFarmingDirty()
                                    FarmingScheduler.INSTANCE.noteActivity(this)
                                } else if (itemId == compost.itemId) {
                                    send(SendMessage("There is no point in using " + itemName + " when the patch already got " + findPatch(objectId, 2).lowercase() + ""+if(findPatch(objectId, 2) != "COMPOST") "compost." else "."))
                                }
                            }
                        } else if (findPatch(objectId, 0).toInt() == -1) {
                            val checkName = objName.replace(" patch", "")
                            var foundStage = 0
                            if(FarmingDefinitions.allotmentPatch.findSeed(itemId)) { //First check is always true!
                                foundStage = if (checkName == "allotment") 1 else 2
                            } else if(FarmingDefinitions.flowerPatch.findSeed(itemId)) {
                                foundStage = if (checkName == "flower") 1 else 2
                            } else if(FarmingDefinitions.herbPatch.findSeed(itemId)) {
                                foundStage = if (checkName == "herb") 1 else 2
                            } else if(FarmingDefinitions.bushPatch.findSeed(itemId)) {
                                foundStage = if (checkName == "bush") 1 else 2
                            } else if(FarmingDefinitions.fruitTreePatch.findSeed(itemId)) {
                                foundStage = if (checkName == "fruit tree") 1 else 2
                            } else if(FarmingDefinitions.treePatch.findSeed(itemId)) {
                                foundStage = if (checkName == "tree") 1 else 2
                            }
                            val value = if(FarmingDefinitions.allotmentPatch.findSeed(itemId)) "allotment"
                            else if (FarmingDefinitions.flowerPatch.findSeed(itemId)) "flower"
                            else if (FarmingDefinitions.herbPatch.findSeed(itemId)) "herb"
                            else if (FarmingDefinitions.bushPatch.findSeed(itemId)) "bush"
                            else if (FarmingDefinitions.fruitTreePatch.findSeed(itemId)) "fruit tree"
                            else if (FarmingDefinitions.treePatch.findSeed(itemId)) "tree" else ""

                            if(foundStage == 2)
                                send(SendMessage(GetItemName(itemId) + " can only be planted at a " + value + " patch."))
                            else if(foundStage == 1) { //Found seed for right patch!
                                val tool = if(checkName.contains("tree")) farmData.SPADE else farmData.SEED_DIBBER
                                if(playerHasItem(tool)) {
                                    if (getLevel(Skill.FARMING) >= farmData.getPlantLevel(itemId)) {
                                        val amount = if (checkName == "allotment") 3 else 1
                                        if (playerHasItem(itemId, amount)) {
                                            sendAnimation(if(checkName.contains("tree")) farmData.SPADE_ANIM else farmData.PLANTSEED_ANIM)
                                            deleteItem(itemId, amount)
                                            addExperience(farmData.getPlantedXp(itemId), Skill.FARMING)
                                            if(checkName.contains("tree"))
                                                addItem(farmData.EMPTY_PLANT_POT, 1)
                                            farmPatches.set(checkPos + 0, JsonPrimitive(itemId))
                                            farmPatches.set(checkPos + 1, JsonPrimitive(FarmingDefinitions.patchState.GROWING.toString()))
                                            farmPatches.set(checkPos + 3, JsonPrimitive(1))
                                            farmPatches.set(checkPos + 4, JsonPrimitive(0))
                                            farmPatches.set(checkPos + 5, JsonPrimitive(System.currentTimeMillis()))
                                            checkItemUpdate()
                                            updateFarmPatch()
                                            markFarmingDirty()
                                            FarmingScheduler.INSTANCE.noteActivity(this)
                                        } else send(SendMessage("You need " + amount + " " + GetItemName(itemId).lowercase() + " to plant here."))
                                    } else send(SendMessage("You need level " + farmData.getPlantLevel(itemId) + " farming to plant " + GetItemName(itemId).lowercase() + "."))
                                } else send(SendMessage("You are missing the "+GetItemName(tool).lowercase()+" tool."))
                            }
                        } else clickPatch(objectId)
                    }
                }
            }
        }
        return false
    }
    fun Client.interactBin(objectId : Int, option : Int) {
        for(compost in FarmingDefinitions.compostBin.values()) {
            if (objectId == compost.objectId && option == 1) {
                val farmCompost = farmingJson.getCompostData().get(compost.name).asJsonArray
                if(farmCompost.get(2).asInt == 15 && FarmingDefinitions.compostState.FILLED.toString() == farmCompost.get(1).asString) {
                    farmCompost.set(1, JsonPrimitive(FarmingDefinitions.compostState.CLOSED.toString()))
                    updateCompost(farmCompost.get(0).asString,farmCompost.get(1).asString, farmCompost.get(2).asInt)
                    markFarmingDirty()
                    FarmingScheduler.INSTANCE.noteActivity(this)
                } else if (FarmingDefinitions.compostState.DONE.toString() == farmCompost.get(1).asString) {
                    farmCompost.set(1, JsonPrimitive(FarmingDefinitions.compostState.OPEN.toString()))
                    updateCompost(farmCompost.get(0).asString,farmCompost.get(1).asString, farmCompost.get(2).asInt)
                    markFarmingDirty()
                    FarmingScheduler.INSTANCE.noteActivity(this)
                } else if (FarmingDefinitions.compostState.OPEN.toString() == farmCompost.get(1).asString) {
                    if(playerHasItem(farmData.BUCKET)) {
                        deleteItem(farmData.BUCKET, 1)
                        addItem(FarmingDefinitions.compost.find(farmCompost.get(0).asString)?.itemId ?: 6032, 1)
                        farmCompost.set(2, JsonPrimitive(farmCompost.get(2).asInt - 1))
                        if(farmCompost.get(2).asInt == 0) {
                            farmCompost.set(0, JsonPrimitive(FarmingDefinitions.compost.NONE.toString()))
                            farmCompost.set(1, JsonPrimitive(FarmingDefinitions.compostState.EMPTY.toString()))
                            farmCompost.set(3, JsonPrimitive(0))
                        }
                        checkItemUpdate()
                        updateCompost(farmCompost.get(0).asString,farmCompost.get(1).asString, farmCompost.get(2).asInt)
                        markFarmingDirty()
                        FarmingScheduler.INSTANCE.noteActivity(this)
                    } else send(SendMessage("You are missing a bucket to be filled with compost."))
                }
            } else if (objectId == compost.objectId && option == 5) {
                //TODO: Fix a compost dump message!
                send(SendMessage("You dump all the content inside the bin!"))
            }
        }
    }
    fun Client.examineBin(pos : Position) {
        for(compost in FarmingDefinitions.compostBin.values()) {
            if (compost.updatePos.x == pos.x && compost.updatePos.y == pos.y) {
                val farmCompost = farmingJson.getCompostData().get(compost.name).asJsonArray
                send(
                    SendMessage(
                        when (farmCompost.get(1).asString) {
                            FarmingDefinitions.compostState.CLOSED.toString() -> "The bin is currently in the process of rotting the containment."
                            FarmingDefinitions.compostState.DONE.toString() -> "The " + farmCompost.get(0).asString.lowercase() + " is ready." //The *name* is ready.
                            FarmingDefinitions.compostState.EMPTY.toString() -> "The bin is currently empty."
                            FarmingDefinitions.compostState.OPEN.toString() -> "There is currently " + farmCompost.get(2).asString + "/15 " + farmCompost.get(0).asString.lowercase() + " remaining."
                            else -> "There is currently " + farmCompost.get(2).asString + "/15 " + farmCompost.get(0).asString.lowercase() + " filled."
                        }
                    )
                )
            }
        }
    }

    fun plantGrow(status: String) : Boolean {
    return status.equals(FarmingDefinitions.patchState.GROWING.toString(), true)
        || status.equals(FarmingDefinitions.patchState.WATER.toString(), true)
        || status.equals(FarmingDefinitions.patchState.PROTECTED.toString(), true)
    }
    fun clearPatch(farmPatches : JsonArray, checkPos : Int) {
        farmPatches.set(checkPos + 0, JsonPrimitive(-1))
        farmPatches.set(checkPos + 1, JsonPrimitive(FarmingDefinitions.patchState.WEED.toString()))
        farmPatches.set(checkPos + 2, JsonPrimitive(FarmingDefinitions.compost.NONE.toString()))
        farmPatches.set(checkPos + 3, JsonPrimitive(3))
        farmPatches.set(checkPos + 4, JsonPrimitive(0))
        farmPatches.set(checkPos + 5, JsonPrimitive(-1))
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

    fun Client.clickPatch(objectId : Int) : Boolean {
        if(findPatch(objectId, 0).isEmpty()) //Not go through if object is not here :D!
            return false
        val objName = GameObjectData.forId(objectId).name.lowercase()
        for(patch in patches.values()) {
            if (farmingJson.getPatchData().get(patch.name) != null) {
                val farmPatches = farmingJson.getPatchData().get(patch.name).asJsonArray //Not sure if we need just yet!
                (0 until patch.objectId.size).forEach { slot ->
                    val checkPos = slot * farmingJson.PATCHAMOUNT
                    val checkItem = findPatch(objectId, 0).toInt()
                    if (patch.objectId[slot] == objectId) { //We got the correct objectId!
                        if(findPatch(objectId, 1) == FarmingDefinitions.patchState.WEED.toString() && findPatch(objectId, 3).toInt() < 3) {
                            if(playerHasItem(farmData.RAKE)) {
                                farmPatches.set(checkPos + 3, JsonPrimitive(farmPatches.get(checkPos + 3).asInt + 1))
                                sendAnimation(farmData.RAKE_ANIM)
                                addItem(farmData.regularCompostItems[0], 1)
                                checkItemUpdate()
                                updateFarmPatch()
                                markFarmingDirty()
                                FarmingScheduler.INSTANCE.noteActivity(this)
                            } else send(SendMessage("You need a rake in order to clear the weed."))
                        } else if (findPatch(objectId, 1) == FarmingDefinitions.patchState.DEAD.toString()) {
                            if(playerHasItem(farmData.SPADE)) {
                                sendAnimation(farmData.SPADE_ANIM)
                                clearPatch(farmPatches, checkPos)
                                updateFarmPatch()
                                markFarmingDirty()
                                FarmingScheduler.INSTANCE.noteActivity(this)
                            } else send(SendMessage("You need to have a spade in order to clear the dead plant."))
                        } else if (findPatch(objectId, 1) == FarmingDefinitions.patchState.STUMP.toString()) {
                            if(playerHasItem(farmData.SPADE)) {
                                sendAnimation(farmData.SPADE_ANIM)
                                clearPatch(farmPatches, checkPos)
                                updateFarmPatch()
                                markFarmingDirty()
                                FarmingScheduler.INSTANCE.noteActivity(this)
                            } else send(SendMessage("You need to have a spade in order to clear the stump from the patch."))
                        } else if (findPatch(objectId, 1) == FarmingDefinitions.patchState.DISEASE.toString() && farmData.getCheckHealthXp(checkItem) > 0) {
                            if(playerHasItem(farmData.SECATEURS) || playerHasItem(farmData.MAGIC_SECATEURS)) {
                                sendAnimation(if(objName == "bush") farmData.PRUNE_SECATEURS_ANIM else farmData.PRUNE_SECATEURS_ANIM - 1)
                                addItem(6020, 1)
                                checkItemUpdate()
                                if(Misc.chance(4) <= 3) { //25% chance to fail prune! Not sure if we keep :P
                                    farmPatches.set(checkPos + 1, JsonPrimitive(FarmingDefinitions.patchState.GROWING.toString()))
                                    farmPatches.set(checkPos + 4, JsonPrimitive(0))
                                } else send(SendMessage("You failed to cure the tree."))
                                updateFarmPatch()
                                markFarmingDirty()
                                FarmingScheduler.INSTANCE.noteActivity(this)
                            } else send(SendMessage("You need to use a pair of secateurs to prune the tree."))
                        } else if (findPatch(objectId, 1) == FarmingDefinitions.patchState.DISEASE.toString()) {
                            if(playerHasItem(farmData.PLANT_CURE)) {
                                sendAnimation(farmData.CURING_ANIM)
                                deleteItem(farmData.PLANT_CURE, 1)
                                farmPatches.set(checkPos + 1, JsonPrimitive(FarmingDefinitions.patchState.GROWING.toString()))
                                farmPatches.set(checkPos + 4, JsonPrimitive(0))
                                updateFarmPatch()
                                markFarmingDirty()
                                checkItemUpdate()
                                FarmingScheduler.INSTANCE.noteActivity(this)
                            } else send(SendMessage("You need to use a spade in order to clear the dead plant."))
                        } else if (findPatch(objectId, 1) == FarmingDefinitions.patchState.HARVEST.toString() && farmData.getCheckHealthXp(checkItem) < 1) {
                            if(playerHasItem(farmData.SPADE) && hasSpace()) {
                                sendAnimation(if(objName == "allotment") farmData.SPADE_ANIM else farmData.HARVEST_ANIM)
                                val life = farmPatches.get(checkPos + 3).asInt - 1
                                addItem(farmData.getHarvestItem(checkItem), 1)
                                addExperience(farmData.getHarvestXp(checkItem), Skill.FARMING)
                                farmPatches.set(checkPos + 3, JsonPrimitive(life))
                                if(life == 0)
                                    clearPatch(farmPatches, checkPos)
                                updateFarmPatch()
                                markFarmingDirty()
                                checkItemUpdate()
                                FarmingScheduler.INSTANCE.noteActivity(this)
                            } else if(!hasSpace()) send(SendMessage("You do not have enough inventory space!"))
                            else send(SendMessage("You need to use a spade in order to clear the dead plant."))
                        } else if (findPatch(objectId, 1) == FarmingDefinitions.patchState.HARVEST.toString() && farmData.getCheckHealthXp(checkItem) > 0) {
                                val life = if(objName == "tree") 1 else if(objName == "bush") 4 else 6
                                farmPatches.set(checkPos + 1, JsonPrimitive(FarmingDefinitions.patchState.PRODUCTION.toString()))
                                farmPatches.set(checkPos + 3, JsonPrimitive(life))
                                farmPatches.set(checkPos + 4, JsonPrimitive(0))
                                addExperience(farmData.getCheckHealthXp(checkItem), Skill.FARMING)
                                send(SendMessage("You check the health of the "+farmData.getPlantName(checkItem).lowercase().replace("_", " ")+" and it is in perfect condition."))
                                updateFarmPatch()
                                markFarmingDirty()
                                FarmingScheduler.INSTANCE.noteActivity(this)
                        } else if (findPatch(objectId, 1) == FarmingDefinitions.patchState.PRODUCTION.toString() && farmData.getCheckHealthXp(checkItem) > 0) {
                            if(objName.startsWith("tree")) { //TODO: Custom code to cut down tree! 1:8 chance for stump
                                //TODO: Fix woodcutting check for cutting to a stump!
                                farmPatches.set(checkPos + 1, JsonPrimitive(FarmingDefinitions.patchState.STUMP.toString()))
                                updateFarmPatch()
                                markFarmingDirty()
                                FarmingScheduler.INSTANCE.noteActivity(this)
                            } else if(hasSpace() && farmPatches.get(checkPos + 3).asInt > 0) {
                                sendAnimation(if(objName.contains("tree")) farmData.HARVEST_FRUIT_ANIM else farmData.HARVEST_BUSH_ANIM)
                                farmPatches.set(checkPos + 3, JsonPrimitive(farmPatches.get(checkPos + 3).asInt - 1))
                                addItem(farmData.getHarvestItem(checkItem), 1)
                                addExperience(farmData.getHarvestXp(checkItem), Skill.FARMING)
                                updateFarmPatch()
                                markFarmingDirty()
                                checkItemUpdate()
                                FarmingScheduler.INSTANCE.noteActivity(this)
                            } else if (farmPatches.get(checkPos + 3).asInt == 0 && objName.startsWith("fruit tree")) {
                                //TODO: Fix woodcutting check for cutting to a stump!
                                farmPatches.set(checkPos + 1, JsonPrimitive(FarmingDefinitions.patchState.STUMP.toString()))
                                updateFarmPatch()
                                markFarmingDirty()
                                FarmingScheduler.INSTANCE.noteActivity(this)
                            } else if (farmPatches.get(checkPos + 3).asInt == 0 && objName.startsWith("bush")) {
                                clearPatch(farmPatches, checkPos)
                                updateFarmPatch()
                                markFarmingDirty()
                                FarmingScheduler.INSTANCE.noteActivity(this)
                            } else send(SendMessage("You do not have enough inventory space!"))
                        }
                        return true
                    }
                }
            }
        }
        return false
    }
    fun Client.inspectPatch(objectId : Int) {
        val findData = findPatch(objectId, 0).toIntOrNull() ?: return
        var stage: Int
        var endStage: Int
        var weeding: Boolean
        var growing: Boolean
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
                weeding = farmPatch.get((slot * farmingJson.PATCHAMOUNT) + 1).asString.equals(FarmingDefinitions.patchState.WEED.toString())
                growing = farmPatch.get((slot * farmingJson.PATCHAMOUNT) + 1).asString.equals(FarmingDefinitions.patchState.GROWING.toString())
                disease = farmPatch.get((slot * farmingJson.PATCHAMOUNT) + 1).asString.equals(FarmingDefinitions.patchState.DISEASE.toString())
                dead = farmPatch.get((slot * farmingJson.PATCHAMOUNT) + 1).asString.equals(FarmingDefinitions.patchState.DEAD.toString())
                compost = farmPatch.get((slot * farmingJson.PATCHAMOUNT) + 2).asString

                val messageOne = "This is a " + GameObjectData.forId(objectId).name.lowercase() +"."
                val messageTwo = if(compost == "NONE") "The soil has not been treated." else if(compost == "PROTECTED") "This patch is protected." else "The soil has been treated with "+compost.lowercase()+"."
                val messageThree = if(stage < 3 && weeding) "The patch needs weeding." else if(findData == -1) "The patch is empty." else if (dead) "Patch is dead!"
                else if (disease) "Currently diseased, you should try and cure it." else if (growing) "The patch has "+ farmData.getPlantName(findData).lowercase().replace("_", " ") +" growing in it and is at state " + stage + "/" + endStage + "" else "The patch ("+farmData.getPlantName(findData).lowercase().replace("_", " ")+") is fully grown."
                send(SendMessage("$messageOne $messageTwo $messageThree"))
            }
        }
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
                var extraSlot = 0
                if(objectId == 7962) extraSlot = 1 //Gnome special treatment?!

                if(findPatch(objectId, 1) == FarmingDefinitions.patchState.WATER.toString() || (findPatch(objectId, 1) == FarmingDefinitions.patchState.DISEASE.toString() && farmData.getCheckHealthXp(itemId) > 0))
                    config = config or (1 shl 6)
                else if(findPatch(objectId, 1) == FarmingDefinitions.patchState.DISEASE.toString() || (findPatch(objectId, 1) == FarmingDefinitions.patchState.DEAD.toString() && farmData.getCheckHealthXp(itemId) > 0))
                    config = config or (2 shl 6)
                else if(findPatch(objectId, 1) == FarmingDefinitions.patchState.DEAD.toString() && farmData.getCheckHealthXp(itemId) < 1)
                    config = config or (3 shl 6)
                else if(findPatch(objectId, 1) == FarmingDefinitions.patchState.HARVEST.toString())
                    config = startConfig + farmData.getEndStage(itemId)
                else if(findPatch(objectId, 1) == FarmingDefinitions.patchState.PRODUCTION.toString())
                    config = startConfig + farmData.getEndStage(itemId) + 1
                else if(findPatch(objectId, 1) == FarmingDefinitions.patchState.STUMP.toString())
                    config = startConfig + farmData.getEndStage(itemId) + 2
                /* Special dead for herb! */
                if(findPatch(objectId, 1) == FarmingDefinitions.patchState.DEAD.toString() && FarmingDefinitions.herbPatch.find(itemId) != null) //Herb!
                    config = 168 + stage
                else if(findPatch(objectId, 1) == FarmingDefinitions.patchState.DISEASE.toString() && FarmingDefinitions.herbPatch.find(itemId) != null) { //Herb!
                    val position = FarmingDefinitions.herbPatch.find(itemId)?.ordinal ?: -1
                    config -= if (position > 7) 5 + position * 4 + 8 else 5 + position * 4
                }
                /* Bush logic */
                if(findPatch(objectId, 1) == FarmingDefinitions.patchState.HARVEST.toString() && FarmingDefinitions.bushPatch.find(itemId) != null)
                    config = 254 - startConfig
                else if(findPatch(objectId, 1) == FarmingDefinitions.patchState.PRODUCTION.toString() && FarmingDefinitions.bushPatch.find(itemId) != null)
                    config = startConfig + farmData.getEndStage(itemId) + stage
                /* Fruit tree logic xD */
                if(findPatch(objectId, 1) == FarmingDefinitions.patchState.DISEASE.toString() && FarmingDefinitions.fruitTreePatch.find(itemId) != null)
                    config = startConfig + stage + 12
                else if(findPatch(objectId, 1) == FarmingDefinitions.patchState.DEAD.toString() && FarmingDefinitions.fruitTreePatch.find(itemId) != null)
                    config = startConfig + stage + 18
                else if(findPatch(objectId, 1) == FarmingDefinitions.patchState.PRODUCTION.toString() && FarmingDefinitions.fruitTreePatch.find(itemId) != null)
                    config = startConfig + farmData.getEndStage(itemId) + stage
                else if(findPatch(objectId, 1) == FarmingDefinitions.patchState.HARVEST.toString() && FarmingDefinitions.fruitTreePatch.find(itemId) != null)
                    config = startConfig + farmData.getEndStage(itemId) + 20
                else if(findPatch(objectId, 1) == FarmingDefinitions.patchState.STUMP.toString() && FarmingDefinitions.fruitTreePatch.find(itemId) != null)
                    config = startConfig + farmData.getEndStage(itemId) + 19
                varbit(farmData.farmPatchConfig + slot + extraSlot, config)
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
        for (sapling in FarmingDefinitions.sapling.values()) {
            if((itemOne == sapling.treeSeed || itemTwo == sapling.treeSeed) && (itemOne == farmData.FILLED_PLANT_POT || itemTwo == farmData.FILLED_PLANT_POT)) {
                if(!playerHasItem(farmData.TROWEL)) {
                    send(SendMessage("You are missing your "+GetItemName(farmData.TROWEL).lowercase()+"."))
                    return
                }
                if(getSkillLevel(Skill.FARMING) < sapling.farmLevel) {
                    send(SendMessage( "You need level " + sapling.farmLevel + " " +
                    "farming to plant the " + GetItemName(sapling.treeSeed).lowercase() + "."))
                    return
                }
                deleteItem(itemOne, if(itemOne == farmData.FILLED_PLANT_POT) itemOneSlot else itemTwoSlot,  1)
                deleteItem(itemTwo, if(itemOne == farmData.FILLED_PLANT_POT) itemTwoSlot else itemOneSlot, 1)
                addItemSlot(sapling.plantedId, 1, if(itemOne == farmData.FILLED_PLANT_POT) itemOneSlot else itemTwoSlot)
                checkItemUpdate()
                FarmingScheduler.INSTANCE.noteActivity(this)
            } else if ((itemOne == sapling.plantedId || itemTwo == sapling.plantedId) && (GetItemName(itemOne).startsWith("Watering can(") || GetItemName(itemTwo).startsWith("Watering can("))) {
                deleteItem(if(itemOne == sapling.plantedId) itemTwo else itemOne, if(itemOne == sapling.plantedId) itemTwoSlot else itemOneSlot,1)
                if((itemOne == sapling.plantedId && !GetItemName(itemTwo).endsWith("1)")) || (itemTwo == sapling.plantedId && !GetItemName(itemOne).endsWith("1)")))
                    addItemSlot(if(itemOne == sapling.plantedId) itemTwo-1 else itemOne-1, 1, if(itemOne == sapling.plantedId) itemTwoSlot else itemOneSlot)
                else addItemSlot(5331, 1, if(itemOne == sapling.plantedId) itemTwoSlot else itemOneSlot)
                deleteItem(sapling.plantedId, if(itemOne == sapling.plantedId) itemOneSlot else itemTwoSlot, 1)
                addItem(sapling.waterId, 1)
                checkItemUpdate()
                FarmingScheduler.INSTANCE.noteActivity(this)
            }
        }
        if((itemOne == FarmingDefinitions.compost.COMPOST.itemId || itemTwo == FarmingDefinitions.compost.COMPOST.itemId) && (itemOne == farmData.EMPTY_PLANT_POT || itemTwo == farmData.EMPTY_PLANT_POT)) {
            sendAnimation(farmData.FILL_PLANTPOT_ANIM)
            deleteItem(itemOne, if(itemOne == farmData.EMPTY_PLANT_POT) itemOneSlot else itemTwoSlot,  1)
            deleteItem(itemTwo, if(itemOne == farmData.EMPTY_PLANT_POT) itemTwoSlot else itemOneSlot, 1)
            addItemSlot(farmData.FILLED_PLANT_POT, 1, if(itemOne == farmData.EMPTY_PLANT_POT) itemOneSlot else itemTwoSlot)
            addItemSlot(farmData.BUCKET, 1, if(itemOne == FarmingDefinitions.compost.COMPOST.itemId) itemOneSlot else itemTwoSlot)
            checkItemUpdate()
            FarmingScheduler.INSTANCE.noteActivity(this)
        }
    }
}
