package net.dodian.uber.game.skills

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser


class FarmingJson() {
    private var farmingCompostValues = JsonObject()
    private var farmingPatchValues = JsonObject()

    fun FarmingSave() : String {
        var jsonString = "[" + farmingCompostValues.toString() + ",\n" + farmingPatchValues.toString() + "]"
        /* Compost */
        /*System.out.println("test...")
        System.out.println(jsonString)
        System.out.println("-------------------------") */
        //System.out.println("json string?")
        //System.out.println(jsonString)
        return jsonString
    }
    fun FarmingShow() : String {
        var jsonString = ""
        /* Compost */
        jsonString += farmingCompostValues
        jsonString += "\n"
        /* Farm Patches */
        jsonString += farmingPatchValues
        return jsonString
    }

    fun FarmingLoad(farmString : String) {
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
                    farmPatch.add(FarmingData.compost.NONE.toString())
                    farmPatch.add(FarmingData.patchState.WEED.toString())
                    farmPatch.add("false")
                    farmPatch.add(0)
                    farmPatch.add(0)
                    farmPatch.add(-1)
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
                        farmPatch.add(FarmingData.compost.NONE.toString())
                        farmPatch.add(FarmingData.patchState.WEED.toString())
                        farmPatch.add("false")
                        farmPatch.add(0)
                        farmPatch.add(0)
                        farmPatch.add(-1)
                    }
                    farmingPatchValues.add(patch.name, farmPatch)
                }
            }
        }
        System.out.println(FarmingSave())
    }

    fun getCompostData() : JsonObject {
        return farmingCompostValues
    }
    fun getPatchData() : JsonObject {
        return farmingPatchValues
    }

}