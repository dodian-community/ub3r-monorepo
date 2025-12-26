package net.dodian.uber.game.skills

import net.dodian.uber.game.model.Position

class FarmingData() {
    /* Chance should be x:1024,
    where x is the chance! Do note chance should also be reduced by a % based on farming level.
    Compost will be reduce the chance / (farmLvl - farmLevelReq / 3) = y,
    y/compostValue rounded up! */

    enum class allotmentPatch(val level : Int, val seed: Int, val plantXp : Int, val disease : Int, val config : Int, val stages : Int, val ticks : Int, val harvestXp : Int, val harvestItem : Int) {
        POTATO(1, 5318, 8, 100, 5, 5, 3, 22, 1942),
        ONION(9, 5319, 12, 100, 12, 6, 3, 32, 1957),
        CABBAGE(1, 5324, 12, 100, 12, 6, 3, 32, -1),
        TOMATO(1, 5322, 12, 100, 12, 6, 3, 32, -1),
        SWEETCORN(1, 5320, 12, 100, 12, 6, 3, 32, -1),
        STRAWBERRY(1, 5323, 12, 100, 12, 6, 3, 32, -1),
        WATERMELON(1, 5321, 12, 100, 12, 6, 3, 32, -1);

        companion object{
            fun find(id: Int): allotmentPatch? = allotmentPatch.values().find { it.seed == id }
            fun findSeed(id: Int): Boolean {
                for(values in allotmentPatch.values())
                    if(values.seed == id) return true
                return false
            }
        }
    }

    enum class flowerPatch(val level : Int, val seed: Int, val plantXp : Int, val disease : Int, val config : Int, val stages : Int, val ticks : Int, val harvestXp : Int, val harvestItem : Int) {
        MARIGOLD(1, 5096, 8, 100, 7, 5, 3, 22, 6010),
        ROSEMARY(12, 5097, 8, 100, 15, 5, 3, 22, 6014),
        NASTURTIUM(18, 5098, 8, 100, 15, 5, 3, 22, 6012),
        WOAD(24, 5099, 8, 100, 15, 5, 3, 22, 1793),
        LIMPWURT(32, 5100, 8, 100, 15, 5, 3, 22, 225);

        companion object{
            fun find(id: Int): flowerPatch? = flowerPatch.values().find { it.seed == id }
            fun findSeed(id: Int): Boolean {
                for(values in flowerPatch.values())
                    if(values.seed == id) return true
                return false
            }
        }
    }

    enum class herbPatch(val level : Int, val seed: Int, val plantXp : Int, val disease : Int, val config : Int, val stages : Int, val ticks : Int, val harvestXp : Int, val harvestItem : Int) {
        GUAM(1, 5291, 12, 100, 3, 5, 3, 32, -1),
        MARRENTILL(1, 0, 12, 100, 10, 5, 3, 32, -1),
        TARROMIN(1, 0, 12, 100, 17, 5, 3, 32, -1),
        HARRALANDER(1, 0, 12, 100, 24, 5, 3, 32, -1),
        RANARR(1, 0, 12, 100, 31, 5, 3, 32, -1),
        TOADFLAX(1, 0, 12, 100, 38, 5, 3, 32, -1),
        IRIT(1, 0, 12, 100, 45, 5, 3, 32, -1),
        AVANTOE(1, 0, 12, 100, 52, 5, 3, 32, -1),
        KWUARM(1, 0, 12, 100, 67, 5, 3, 32, -1),
        SNAPDRAGON(1, 0, 12, 100, 74, 5, 3, 32, -1),
        CADANTINE(1, 0, 12, 100, 81, 5, 3, 32, -1),
        LANTADYME(1, 0, 12, 100, 88, 5, 3, 32, -1),
        DWARF_WEED(1, 0, 12, 100, 95, 5, 3, 32, -1),
        TORSTOL(1, 0, 12, 100, 102, 5, 3, 32, -1);

        companion object{
            fun find(id: Int): herbPatch? = herbPatch.values().find { it.seed == id }
            fun findSeed(id: Int): Boolean {
                for(values in herbPatch.values())
                    if(values.seed == id) return true
                return false
            }
        }
    }

    enum class bushPatch(val level : Int, val seed: Int, val plantXp : Int, val disease : Int, val config : Int, val stages : Int, val ticks : Int, val harvestXp : Int, val harvestItem : Int) {
        REDBERRY(20, 5101, 8, 100, 4, 5, 3, 22, 1951),
        CADAVABERRY(1, 0, 8, 100, 14, 6, 3, 22, -1),
        DWELLBERRY(1, 0, 8, 100, 25, 6, 3, 22, -1),
        JANGERBERRY(1, 0, 8, 100, 37, 6, 3, 22, -1),
        WHITEBERRY(1, 0, 8, 100, 50, 6, 3, 22, -1);

        companion object{
            fun find(id: Int): bushPatch? = bushPatch.values().find { it.seed == id }
            fun findSeed(id: Int): Boolean {
                for(values in bushPatch.values())
                    if(values.seed == id) return true
                return false
            }
        }
    }

    enum class fruitTreePatch(val level : Int, val seed: Int, val plantXp : Int, val disease : Int, val config : Int, val stages : Int, val ticks : Int, val harvestXp : Int, val harvestItem : Int) {
        APPLE(25, 0, 8, 55, 7, 8, 30, 22, -1),
        BANANA(25, 0, 8, 55, 34, 8, 30, 22, -1),
        ORANGE(25, 0, 8, 55, 71, 8, 30, 22, -1),
        CURRY(25, 0, 8, 55, 98, 8, 30, 22, -1),
        PINEAPPLE(25, 0, 8, 55, 135, 8, 30, 22, -1),
        PAPAYA(25, 0, 8, 55, 162, 8, 30, 22, -1),
        PALM(25, 0, 8, 55, 199, 8, 30, 22, -1);

        companion object{
            fun find(id: Int): fruitTreePatch? = fruitTreePatch.values().find { it.seed == id }
            fun findSeed(id: Int): Boolean {
                for(values in fruitTreePatch.values())
                    if(values.seed == id) return true
                return false
            }
        }
    }

    enum class treePatch(val level : Int, val seed: Int, val plantXp : Int, val disease : Int, val config : Int, val stages : Int, val ticks : Int, val harvestXp : Int, val harvestItem : Int) {
        ACORN(15, 0, 8, 55, 7, 4, 30, 22, -1),
        WILLOW(30, 0, 8, 55, 14, 4, 30, 22, -1),
        MAPLE(45, 0, 8, 55, 23, 4, 30, 22, -1),
        YEW(60, 0, 8, 55, 34, 4, 30, 22, -1),
        MAGIC(75, 0, 8, 55, 47, 4, 30, 22, -1);

        companion object{
            fun find(id: Int): treePatch? = treePatch.values().find { it.seed == id }
            fun findSeed(id: Int): Boolean {
                for(values in treePatch.values())
                    if(values.seed == id) return true
                return false
            }
        }
    }

    fun getDiseaseChance(id : Int) : Int {
        return allotmentPatch.find(id)?.disease ?: flowerPatch.find(id)?.disease ?:
        herbPatch.find(id)?.disease ?: bushPatch.find(id)?.disease ?:
        fruitTreePatch.find(id)?.disease ?: treePatch.find(id)?.disease ?: 0
    }
    fun getPatchConfig(id : Int) : Int {
        return allotmentPatch.find(id)?.config ?: flowerPatch.find(id)?.config ?:
        herbPatch.find(id)?.config ?: bushPatch.find(id)?.config ?:
        fruitTreePatch.find(id)?.config ?: treePatch.find(id)?.config ?: 0
    }
    fun getEndStage(id : Int) : Int {
        return allotmentPatch.find(id)?.stages ?: flowerPatch.find(id)?.stages ?:
        herbPatch.find(id)?.stages ?: bushPatch.find(id)?.stages ?:
        fruitTreePatch.find(id)?.stages ?: treePatch.find(id)?.stages ?: 0
    }
    fun getGrowTick(id : Int) : Int {
        return allotmentPatch.find(id)?.ticks ?: flowerPatch.find(id)?.ticks ?:
        herbPatch.find(id)?.ticks ?: bushPatch.find(id)?.ticks ?:
        fruitTreePatch.find(id)?.ticks ?: treePatch.find(id)?.ticks ?: 0
    }
    fun getPlantLevel(id : Int) : Int {
        return allotmentPatch.find(id)?.level ?: flowerPatch.find(id)?.level ?:
        herbPatch.find(id)?.level ?: bushPatch.find(id)?.level ?:
        fruitTreePatch.find(id)?.level ?: treePatch.find(id)?.level ?: 0
    }
    fun getPlantedXp(id : Int) : Int {
        return allotmentPatch.find(id)?.plantXp ?: flowerPatch.find(id)?.plantXp ?:
        herbPatch.find(id)?.plantXp ?: bushPatch.find(id)?.plantXp ?:
        fruitTreePatch.find(id)?.plantXp ?: treePatch.find(id)?.plantXp ?: 0
    }
    fun getHarvestXp(id : Int) : Int {
        return allotmentPatch.find(id)?.harvestXp ?: flowerPatch.find(id)?.harvestXp ?:
        herbPatch.find(id)?.harvestXp ?: bushPatch.find(id)?.harvestXp ?:
        fruitTreePatch.find(id)?.harvestXp ?: treePatch.find(id)?.harvestXp ?: 0
    }

    enum class sapling(val farmLevel : Int, val treeSeed : Int, val plantedId : Int, val waterId : Int, val saplingId : Int) {
        OAK(15, 5312, 5358, 5364, 5370),
        WILLOW(30, 5313, 5359, 5365, 5371),
        MAPLE(45, 5314, 5360, 5366, 5372),
        YEW(60, 5315, 5361, 5367, 5373),
        MAGIC(75, 5316, 5362, 5368, 5374),
        APPLE(27, 5283, 5480, 5488, 5496),
        BANANA(33, 5284, 5481, 5489, 5497),
        ORANGE(39, 5285, 5482, 5490, 5498),
        CURRY(45, 5286, 5483, 5491, 5499),
        PINEAPPLE(51, 5287, 5484, 5492, 5500),
        PAPAYA(57, 5288, 5485, 5493, 5501),
        PALM(63, 5289, 5486, 5494, 5502)
    }

    enum class patches(val updatePos: Position, val objectId: Array<Int>, val farmData: List<Array<out Enum<*>>>) {
        CATHERBY_WEST(Position(2809,3463,0), arrayOf(8552, 8553, 7848, 8151),
            listOf(allotmentPatch.values(), allotmentPatch.values(), flowerPatch.values(), herbPatch.values())),
        CATHERBY_EAST(Position(2860,3433,0), arrayOf(7965), listOf(fruitTreePatch.values())),
        ARDOUGNE_EAST(Position(2666,3374,0), arrayOf(8554, 8555, 7849, 8152),
            listOf(allotmentPatch.values(), allotmentPatch.values(), flowerPatch.values(), herbPatch.values())),
        ARDOUGNE_SOUTH(Position(2617,3225,0), arrayOf(7580), listOf(bushPatch.values()))
    }
    enum class patchState {
        WEED, GROWING, PROTECTED, DISEASE, DEAD, HARVEST, WATER, TREE, STUMP; //Two last are not common for all patches!
    }
    enum class compost(val itemId : Int, val divideValue: Int) { //chance/divideValue
        NONE(-1, 1), COMPOST(6032, 4), SUPER(6034, 7), ULTRA(21483, 10);
        companion object {
            fun find(name: String): compost? = compost.values().find { it.name == name }
        }
    }

    enum class compostBin(val updatePos: Position, val objectId: Int, val ticks: Int) {
        CATHERBY(Position(2804,3464,0), 7837, 4),
        ARDOUGNE(Position(2661,3375,0), 7839, 60),
        CANAFIS(Position(3333,3333,0), 1002, 60),
        FALADOR(Position(3333,3333,0), 1003, 60)
    }
    enum class compostState {
        EMPTY, FILLED, CLOSED, DONE, OPEN
    }

    val regularCompostItems = listOf(6055, 6010, 6014, 1793, 5986, 5504, 1955, 1963, 2108, 5970,
        1957, 1942, 1965, 1951, 2126, 753, 1779, 401, 249, 199, 251, 201, 253, 203, 255, 205, 257, 207)
    val superCompostItems = listOf(2114, 5982, 5972, 5974, 5978, 5976, 231, 247, 239, 6018, 2998, 3049,
        261, 211, 263, 213, 3000, 3051, 265, 215, 2481, 2485, 267, 217, 269, 219, 259, 209)
    val BUCKET = 1925
    val SPADE = 952
    val RAKE = 5341
    val SEED_DIBBER = 5343
    val TROWEL = 5325
    val PLANT_POT = 5356
    val SECATEURS = 5329
    val MAGIC_SECATEURS = 7409
    val PLANT_CURE = 6036

    val RAKE_ANIM = 2273
    val SPADE_ANIM = 830
    val WATERCAN_ANIM = 2293
    val PLANTSEED_ANIM = 2291
    val HARVEST_ANIM = 2282
    val COMPOST_PATCH_ANIM = 2283
    val CURING_ANIM = 2288
    val FILL_PLANTPOT_ANIM = 2287

    val farmPatchConfig = 4771
    val compostBinConfig = 4775

}