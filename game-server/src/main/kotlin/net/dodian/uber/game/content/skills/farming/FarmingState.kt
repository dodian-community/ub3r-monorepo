package net.dodian.uber.game.content.skills.farming

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser


class FarmingState {
    private var farmingCompostValues = JsonObject()
    private var farmingPatchValues = JsonObject()
    @Volatile
    private var cachedSaveSnapshot = "[{},\n{}]"

    fun farmingSave() : String {
        return buildSaveSnapshot()
    }

    fun farmingSaveSnapshot(): String = cachedSaveSnapshot

    fun refreshSaveSnapshot(): String {
        cachedSaveSnapshot = buildSaveSnapshot()
        return cachedSaveSnapshot
    }

    fun farmingLoad(farmString : String) {
        val new = farmString.isEmpty()
        val farmJson = JsonParser().parse(farmString)
        var farmPatch: JsonArray
        if(new) {
            for(compost in FarmingDefinitions.compostBin.values()) { /* Compost default values */
                val farmCompost = JsonArray()
                farmCompost.add(FarmingDefinitions.compost.NONE.toString()) //Compost!
                farmCompost.add(FarmingDefinitions.compostState.EMPTY.toString()) //State
                farmCompost.add(0) //Timer/Amount
                farmCompost.add(-1) //Date
                farmingCompostValues.add(compost.name, farmCompost)
            }
            for (patch in FarmingDefinitions.patches.values()) { /* Patches default values */
                farmPatch = JsonArray()
                repeat((0 until patch.objectId.size).count()) {
                    farmPatch.add(-1) //ItemId of planted seed :D
                    farmPatch.add(FarmingDefinitions.patchState.WEED.toString()) //State (Included Protection!)
                    farmPatch.add(FarmingDefinitions.compost.NONE.toString()) //Compost
                    farmPatch.add(0) //Stages
                    farmPatch.add(0) //Timer
                    farmPatch.add(-1) //Planted at Date
                }
                farmingPatchValues.add(patch.name, farmPatch)
            }
        } else { /* Values from save! */
            farmingCompostValues = farmJson.asJsonArray.get(0) as JsonObject
            for(compost in FarmingDefinitions.compostBin.values()) { /* Compost default values */
                if(farmingCompostValues.get(compost.name) == null) {
                    for(compostType in FarmingDefinitions.compostBin.values()) { /* Compost default values */
                        val farmCompost = JsonArray()
                        farmCompost.add(FarmingDefinitions.compost.NONE.toString()) //Amount
                        farmCompost.add(FarmingDefinitions.compostState.EMPTY.toString()) //State
                        farmCompost.add(0) //Timer
                        farmCompost.add(-1) //Date
                        farmingCompostValues.add(compostType.name, farmCompost)
                    }
                }
            }
            farmingPatchValues = farmJson.asJsonArray.get(1) as JsonObject
            for (patch in FarmingDefinitions.patches.values()) { /* Patches default values */
                if (farmingPatchValues.get(patch.name) == null) {
                    farmPatch = JsonArray()
                    repeat((0 until patch.objectId.size).count()) {
                        farmPatch.add(-1)
                        farmPatch.add(FarmingDefinitions.patchState.WEED.toString()) //State (Included Protection!)
                        farmPatch.add(FarmingDefinitions.compost.NONE.toString()) //Compost
                        farmPatch.add(0) //Stages
                        farmPatch.add(0) //Timer
                        farmPatch.add(-1) //Planted at Date
                    }
                    farmingPatchValues.add(patch.name, farmPatch)
                }
            }
        }
        refreshSaveSnapshot()
    }

    fun getCompostData() : JsonObject {
        return farmingCompostValues
    }
    fun getPatchData() : JsonObject {
        return farmingPatchValues
    }

    val PATCHAMOUNT = 6

    private fun buildSaveSnapshot(): String = "[$farmingCompostValues,\n$farmingPatchValues]"

}
