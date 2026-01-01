package net.dodian.uber.game.skills

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser


class FarmingJson {
    private var farmingCompostValues = JsonObject()
    private var farmingPatchValues = JsonObject()

    fun farmingSave() : String {
        val jsonString = "[$farmingCompostValues,\n$farmingPatchValues]"
        return jsonString
        //return "[]" //Default value for test!
    }

    fun farmingLoad(farmString : String) {
        val new = farmString.isEmpty()
        val farmJson = JsonParser().parse(farmString)
        var farmPatch: JsonArray
        if(new) {
            for(compost in FarmingData.compostBin.values()) { /* Compost default values */
                val farmCompost = JsonArray()
                farmCompost.add(FarmingData.compost.NONE.toString()) //Compost!
                farmCompost.add(FarmingData.compostState.EMPTY.toString()) //State
                farmCompost.add(0) //Timer/Amount
                farmCompost.add(-1) //Date
                farmingCompostValues.add(compost.name, farmCompost)
            }
            for (patch in FarmingData.patches.values()) { /* Patches default values */
                farmPatch = JsonArray()
                repeat((0 until patch.objectId.size).count()) {
                    farmPatch.add(-1) //ItemId of planted seed :D
                    farmPatch.add(FarmingData.patchState.WEED.toString()) //State (Included Protection!)
                    farmPatch.add(FarmingData.compost.NONE.toString()) //Compost
                    farmPatch.add(0) //Stages
                    farmPatch.add(0) //Timer
                    farmPatch.add(-1) //Planted at Date
                }
                farmingPatchValues.add(patch.name, farmPatch)
            }
        } else { /* Values from save! */
            farmingCompostValues = farmJson.asJsonArray.get(0) as JsonObject
            for(compost in FarmingData.compostBin.values()) { /* Compost default values */
                if(farmingCompostValues.get(compost.name) == null) {
                    for(compost in FarmingData.compostBin.values()) { /* Compost default values */
                        val farmCompost = JsonArray()
                        farmCompost.add(FarmingData.compost.NONE.toString()) //Amount
                        farmCompost.add(FarmingData.compostState.EMPTY.toString()) //State
                        farmCompost.add(0) //Timer
                        farmCompost.add(-1) //Date
                        farmingCompostValues.add(compost.name, farmCompost)
                    }
                }
            }
            farmingPatchValues = farmJson.asJsonArray.get(1) as JsonObject
            for (patch in FarmingData.patches.values()) { /* Patches default values */
                if (farmingPatchValues.get(patch.name) == null) {
                    farmPatch = JsonArray()
                    repeat((0 until patch.objectId.size).count()) {
                        farmPatch.add(-1)
                        farmPatch.add(FarmingData.patchState.WEED.toString()) //State (Included Protection!)
                        farmPatch.add(FarmingData.compost.NONE.toString()) //Compost
                        farmPatch.add(0) //Stages
                        farmPatch.add(0) //Timer
                        farmPatch.add(-1) //Planted at Date
                    }
                    farmingPatchValues.add(patch.name, farmPatch)
                }
            }
        }
    }

    fun getCompostData() : JsonObject {
        return farmingCompostValues
    }
    fun getPatchData() : JsonObject {
        return farmingPatchValues
    }

    val PATCHAMOUNT = 6

}