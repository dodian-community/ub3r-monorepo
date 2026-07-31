package net.dodian.uber.game.engine.systems.skills.farming

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.engine.systems.skills.farming.FarmingData.patches
import net.dodian.uber.game.persistence.player.PlayerSaveSegment
import net.dodian.uber.game.api.content.ContentRuntimeApi

class Farming {
    val farmData = FarmingData()

    // The compost-bin CLOSED->DONE tick and the allotment/flower/herb WEED/GROWING/DISEASE
    // progression loop that used to live here moved to FarmingModule.applyGrowthPulse (plugin-
    // owned, called once per due wall-clock pulse by FarmingRuntimeService). STUMP/PRODUCTION
    // (bush/fruit-tree/tree) were confirmed dead - no live object ids exist anywhere - and were
    // deleted, not ported. Only the sapling bank/inventory auto-conversion loop remains here,
    // since bush/fruit-tree/tree/saplings are still out of scope (Farming part 3, undecided).
    fun Client.updateSaplings() {
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

    fun Client.updateCompost(compost : String, status: String, amount : Int) {
        when (status) {
            FarmingData.compostState.CLOSED.toString() -> varbit(
                farmData.compostBinConfig,
                if (compost == FarmingData.compost.COMPOST.toString()) 32 else 65
            )

            FarmingData.compostState.DONE.toString() -> varbit(
                farmData.compostBinConfig,
                if (compost == FarmingData.compost.COMPOST.toString()) 31 else 64
            )

            FarmingData.compostState.OPEN.toString() -> varbit(
                farmData.compostBinConfig,
                if (compost == FarmingData.compost.COMPOST.toString()) 15 + amount else 47 + amount
            )

            FarmingData.compostState.FILLED.toString() -> varbit(
                farmData.compostBinConfig,
                if (compost == FarmingData.compost.COMPOST.toString()) 0 + amount else 32 + amount
            )

            else -> varbit(farmData.compostBinConfig, 0)
        }
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
    // Client.interactItemBin/.interactBin/.examineBin (compost-bin logic) and .clickPatch/
    // .inspectPatch/.clearPatch (patch weed/plant/harvest logic) were confirmed dead - all
    // ported to plugins/skills/farming/.../FarmingModule.kt (interactBin/useItemOnBin/
    // clickPatch/useItem/inspectPatch/examineBin), which reads/writes the same underlying
    // client.farmingJson storage via SkillFarmingState - see skills-plugin-migration-audit
    // memory, Chunks 8-9, for the full port record.

    fun plantGrow(status: String) : Boolean {
    return status.equals(FarmingData.patchState.GROWING.toString(), true)
        || status.equals(FarmingData.patchState.WATER.toString(), true)
        || status.equals(FarmingData.patchState.PROTECTED.toString(), true)
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
                var extraSlot = 0
                if(objectId == 7962) extraSlot = 1 //Gnome special treatment?!

                if(findPatch(objectId, 1) == FarmingData.patchState.WATER.toString() || (findPatch(objectId, 1) == FarmingData.patchState.DISEASE.toString() && farmData.getCheckHealthXp(itemId) > 0))
                    config = config or (1 shl 6)
                else if(findPatch(objectId, 1) == FarmingData.patchState.DISEASE.toString() || (findPatch(objectId, 1) == FarmingData.patchState.DEAD.toString() && farmData.getCheckHealthXp(itemId) > 0))
                    config = config or (2 shl 6)
                else if(findPatch(objectId, 1) == FarmingData.patchState.DEAD.toString() && farmData.getCheckHealthXp(itemId) < 1)
                    config = config or (3 shl 6)
                else if(findPatch(objectId, 1) == FarmingData.patchState.HARVEST.toString())
                    config = startConfig + farmData.getEndStage(itemId)
                else if(findPatch(objectId, 1) == FarmingData.patchState.PRODUCTION.toString())
                    config = startConfig + farmData.getEndStage(itemId) + 1
                else if(findPatch(objectId, 1) == FarmingData.patchState.STUMP.toString())
                    config = startConfig + farmData.getEndStage(itemId) + 2
                /* Special dead for herb! */
                if(findPatch(objectId, 1) == FarmingData.patchState.DEAD.toString() && FarmingData.herbPatch.find(itemId) != null) //Herb!
                    config = 168 + stage
                else if(findPatch(objectId, 1) == FarmingData.patchState.DISEASE.toString() && FarmingData.herbPatch.find(itemId) != null) { //Herb!
                    val position = FarmingData.herbPatch.find(itemId)?.ordinal ?: -1
                    config -= if (position > 7) 5 + position * 4 + 8 else 5 + position * 4
                }
                /* Bush logic */
                if(findPatch(objectId, 1) == FarmingData.patchState.HARVEST.toString() && FarmingData.bushPatch.find(itemId) != null)
                    config = 254 - startConfig
                else if(findPatch(objectId, 1) == FarmingData.patchState.PRODUCTION.toString() && FarmingData.bushPatch.find(itemId) != null)
                    config = startConfig + farmData.getEndStage(itemId) + stage
                /* Fruit tree logic xD */
                if(findPatch(objectId, 1) == FarmingData.patchState.DISEASE.toString() && FarmingData.fruitTreePatch.find(itemId) != null)
                    config = startConfig + stage + 12
                else if(findPatch(objectId, 1) == FarmingData.patchState.DEAD.toString() && FarmingData.fruitTreePatch.find(itemId) != null)
                    config = startConfig + stage + 18
                else if(findPatch(objectId, 1) == FarmingData.patchState.PRODUCTION.toString() && FarmingData.fruitTreePatch.find(itemId) != null)
                    config = startConfig + farmData.getEndStage(itemId) + stage
                else if(findPatch(objectId, 1) == FarmingData.patchState.HARVEST.toString() && FarmingData.fruitTreePatch.find(itemId) != null)
                    config = startConfig + farmData.getEndStage(itemId) + 20
                else if(findPatch(objectId, 1) == FarmingData.patchState.STUMP.toString() && FarmingData.fruitTreePatch.find(itemId) != null)
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
        for (sapling in FarmingData.sapling.values()) {
            if((itemOne == sapling.treeSeed || itemTwo == sapling.treeSeed) && (itemOne == farmData.FILLED_PLANT_POT || itemTwo == farmData.FILLED_PLANT_POT)) {
                if(!playerHasItem(farmData.TROWEL)) {
                    send(SendMessage("You are missing your "+getItemName(farmData.TROWEL).lowercase()+"."))
                    return
                }
                if(getSkillLevel(Skill.FARMING) < sapling.farmLevel) {
                    send(SendMessage( "You need level " + sapling.farmLevel + " " +
                    "farming to plant the " + getItemName(sapling.treeSeed).lowercase() + "."))
                    return
                }
                deleteItem(itemOne, if(itemOne == farmData.FILLED_PLANT_POT) itemOneSlot else itemTwoSlot,  1)
                deleteItem(itemTwo, if(itemOne == farmData.FILLED_PLANT_POT) itemTwoSlot else itemOneSlot, 1)
                addItemSlot(sapling.plantedId, 1, if(itemOne == farmData.FILLED_PLANT_POT) itemOneSlot else itemTwoSlot)
                checkItemUpdate()
                ContentRuntimeApi.onFarmingSaplingInventoryChange(this)
            } else if ((itemOne == sapling.plantedId || itemTwo == sapling.plantedId) && (getItemName(itemOne).startsWith("Watering can(") || getItemName(itemTwo).startsWith("Watering can("))) {
                deleteItem(if(itemOne == sapling.plantedId) itemTwo else itemOne, if(itemOne == sapling.plantedId) itemTwoSlot else itemOneSlot,1)
                if((itemOne == sapling.plantedId && !getItemName(itemTwo).endsWith("1)")) || (itemTwo == sapling.plantedId && !getItemName(itemOne).endsWith("1)")))
                    addItemSlot(if(itemOne == sapling.plantedId) itemTwo-1 else itemOne-1, 1, if(itemOne == sapling.plantedId) itemTwoSlot else itemOneSlot)
                else addItemSlot(5331, 1, if(itemOne == sapling.plantedId) itemTwoSlot else itemOneSlot)
                deleteItem(sapling.plantedId, if(itemOne == sapling.plantedId) itemOneSlot else itemTwoSlot, 1)
                addItem(sapling.waterId, 1)
                checkItemUpdate()
                ContentRuntimeApi.onFarmingSaplingInventoryChange(this)
            }
        }
        if((itemOne == FarmingData.compost.COMPOST.itemId || itemTwo == FarmingData.compost.COMPOST.itemId) && (itemOne == farmData.EMPTY_PLANT_POT || itemTwo == farmData.EMPTY_PLANT_POT)) {
            sendAnimation(farmData.FILL_PLANTPOT_ANIM)
            deleteItem(itemOne, if(itemOne == farmData.EMPTY_PLANT_POT) itemOneSlot else itemTwoSlot,  1)
            deleteItem(itemTwo, if(itemOne == farmData.EMPTY_PLANT_POT) itemTwoSlot else itemOneSlot, 1)
            addItemSlot(farmData.FILLED_PLANT_POT, 1, if(itemOne == farmData.EMPTY_PLANT_POT) itemOneSlot else itemTwoSlot)
            addItemSlot(farmData.BUCKET, 1, if(itemOne == FarmingData.compost.COMPOST.itemId) itemOneSlot else itemTwoSlot)
            checkItemUpdate()
            ContentRuntimeApi.onFarmingSaplingInventoryChange(this)
        }
    }
}

object FarmingObjectComponents {
    val patchObjects: IntArray = patches.values()
        .flatMap { it.objectId.toList() }
        .distinct()
        .sorted()
        .toIntArray()
}

fun Client.markFarmingDirty() {
    farmingJson.refreshSaveSnapshot()
    markSaveDirty(PlayerSaveSegment.FARMING.mask)
    ContentRuntimeApi.onFarmingStateDirty(this)
}
