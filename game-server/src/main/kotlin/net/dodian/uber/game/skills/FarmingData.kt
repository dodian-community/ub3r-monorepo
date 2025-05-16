package net.dodian.uber.game.skills

import net.dodian.uber.game.model.Position

class FarmingData() {
    /* Chance should be x:1024,
    where x is the chance! Do note chance should also be reduced by a % based on farming level.
    Compost will be reduce the chance / (farmLvl - farmLevelReq / 3) = y,
    y/compostValue rounded up! */

    enum class flowerPatch(val level : Int, val startConfig : Int, val stages : Int, val diseaseChance: Int, val plantExp : Int, val harvestExp : Int, val harvestItem: Int) {
        MARIGOLD(2,3, 4, 55, 12, 25, 444);
    }

    enum class allotmentPatch(val level : Int, val startConfig : Int, val stages : Int, val diseaseChance: Int, val plantExp : Int, val harvestExp : Int, val harvestItem: Int) {
        POTATO(1,3, 5, 55, 12, 25, 444),
        ONION(9,3, 5, 55,12, 25, 444);
        /*companion object {
            fun getLevel(level: Int): flowerPatch? = flowerPatch.values().singleOrNull { it.level == level }
        }*/
    }

    enum class herbPatch(val level : Int, val startConfig : Int, val stages : Int, val diseaseChance: Int, val plantExp : Int, val harvestExp : Int, val harvestItem: Int) {
        GUAM(10,3, 5,  55,12, 25, 199);
    }

    enum class bushPatch(val level : Int, val startConfig : Int, val stages : Int, val diseaseChance: Int, val plantExp : Int, val healthId : Int, val clearId : Int, val checkExp : Int, val harvestExp : Int, val harvestItem: Int) {
        REDBERRY(20,3, 5, 55,12, 12, 8, 350, 12, 1951);
    }

    enum class fruitTreePatch(val level : Int, val startConfig : Int, val stages : Int, val diseaseChance: Int, val plantExp : Int, val healthId : Int, val clearId : Int, val checkExp : Int, val harvestExp : Int, val harvestItem: Int) {
        APPLE(25,3, 5, 55, 12, 12, 8, 350, 12, 1951);
    }

    enum class treePatch(val level : Int, val startConfig : Int, val stages : Int, val diseaseChance: Int, val plantExp : Int, val healthId : Int, val treeId : Int, val stumpId : Int, val checkExp : Int, val harvestItem: Int) {
        ACORN(15,3, 5, 55,12, 12, 8, 9,350,1951);
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
        CATHERBY_WEST(Position(2809,3463,0), arrayOf(7848, 8552, 8553, 8151),
        arrayOf(flowerPatch.values(), allotmentPatch.values(), allotmentPatch.values(), herbPatch.values()).toList()),
        CATHERBY_EAST(Position(2860,3433,0), arrayOf(7965), arrayOf(fruitTreePatch.values()).toList()),
        ARDOUGNE_EAST(Position(2666,3374,0), arrayOf(7849, 8554, 8555, 8152),
        arrayOf(flowerPatch.values(), allotmentPatch.values(), allotmentPatch.values(), herbPatch.values()).toList()),
        ARDOUGNE_SOUTH(Position(2617,3225,0), arrayOf(7580), arrayOf(bushPatch.values()).toList())
    }
    enum class patchState {
        WEED, GROWING, DISEASE, DEAD, HARVEST, WATER, STUMP //Two last are not common for all patches!
    }
    enum class compost(val divideValue: Int) {
        NONE(1), REGULAR(4), SUPER(8) //chance/divideValue
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

}