package net.dodian.uber.game.skills

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser


class FarmingJson() {
    private var farmingCompostValues = JsonObject()
    private var farmingPatchValues = JsonObject()

    fun farmingSave() : String {
        var jsonString = "[" + farmingCompostValues.toString() + ",\n" + farmingPatchValues.toString() + "]"
        //return jsonString
        return "[]"
    }
    fun farmingShow() : String {
        var jsonString = ""
        /* Compost */
        jsonString += farmingCompostValues
        jsonString += "\n"
        /* Farm Patches */
        jsonString += farmingPatchValues
        return jsonString
    }

    fun farmingLoad(farmString : String) {
        val new = farmString.isEmpty()
        if(new) {
            for(compost in FarmingData.compostBin.values()) { /* Compost default values */
                val farmCompost = JsonArray()
                farmCompost.add(FarmingData.compost.NONE.toString())
                farmCompost.add(FarmingData.compostState.EMPTY.toString())
                farmCompost.add(0)
                farmCompost.add(-1)
                farmingCompostValues.add(compost.name, farmCompost)
            }
            for (patch in FarmingData.patches.values()) { /* Patches default values */
                val farmPatch = JsonArray()
                (0..patch.farmData.size-1).forEach { slot ->
                    farmPatch.add("NONE")
                    farmPatch.add(FarmingData.patchState.WEED.toString())
                    farmPatch.add(FarmingData.compost.NONE.toString()) //Protection or compost!
                    farmPatch.add(0) //stages
                    farmPatch.add(0) //timer
                    farmPatch.add(-1) //planted at date
                }
                farmingPatchValues.add(patch.name, farmPatch)
            }
        } else { /* Values from save! */
            val farmJsonShiet = JsonParser().parse(farmString)
            farmingCompostValues = farmJsonShiet.asJsonArray.get(0) as JsonObject
            for(compost in FarmingData.compostBin.values()) { /* Compost default values */
                if(farmingCompostValues.get(compost.name) == null) {
                    for(compost in FarmingData.compostBin.values()) { /* Compost default values */
                        val farmCompost = JsonArray()
                        farmCompost.add(FarmingData.compost.NONE.toString())
                        farmCompost.add(FarmingData.compostState.EMPTY.toString())
                        farmCompost.add(0)
                        farmCompost.add(-1)
                        farmingCompostValues.add(compost.name, farmCompost)
                    }
                }
            }
            farmingPatchValues = farmJsonShiet.asJsonArray.get(1) as JsonObject
            for (patch in FarmingData.patches.values()) { /* Patches default values */
                if (farmingPatchValues.get(patch.name) == null) {
                    val farmPatch = JsonArray()
                    (0..farmPatch.size()-1).forEach { slot ->
                        farmPatch.add("NONE")
                        farmPatch.add(FarmingData.patchState.WEED.toString())
                        farmPatch.add(FarmingData.compost.NONE.toString()) //Protection or compost!
                        farmPatch.add(0) //stages
                        farmPatch.add(0) //timer
                        farmPatch.add(-1) //planted at date
                    }
                    farmingPatchValues.add(patch.name, farmPatch)
                }
            }
        }
        //System.out.println(FarmingSave())
    }

    fun getCompostData() : JsonObject {
        return farmingCompostValues
    }
    fun getPatchData() : JsonObject {
        return farmingPatchValues
    }

    val PATCHAMOUNT = 6

}