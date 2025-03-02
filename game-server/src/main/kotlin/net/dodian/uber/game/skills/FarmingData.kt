package net.dodian.uber.game.skills

import net.dodian.uber.game.SkillWIP

class FarmingData() {

    enum class flowerPatch(val level : Int, val startConfig : Int, val stages : Int, val plantExp : Int, val harvestExp : Int, val harvestItem: Int) {
        MARIGOLD(2,3, 4, 12, 25, 444);

        companion object {
            fun getLevel(level: Int): flowerPatch? = flowerPatch.values().singleOrNull { it.level == level }
        }
    }

    enum class allotmentPatch(val level : Int, val startConfig : Int, val stages : Int, val plantExp : Int, val harvestExp : Int, val harvestItem: Int) {
        POTATO(1,3, 5, 12, 25, 444);

        companion object {
            fun getLevel(level: Int): flowerPatch? = flowerPatch.values().singleOrNull { it.level == level }
        }
    }

    enum class herbPatch(val level : Int, val startConfig : Int, val stages : Int, val plantExp : Int, val harvestExp : Int, val harvestItem: Int) {
        GUAM(10,3, 5, 12, 25, 199);

        companion object {
            fun getLevel(level: Int): flowerPatch? = flowerPatch.values().singleOrNull { it.level == level }
        }
    }

    enum class bushPatch(val level : Int, val startConfig : Int, val stages : Int, val plantExp : Int, val healthId : Int, val clearId : Int, val checkExp : Int, val harvestExp : Int, val harvestItem: Int) {
        REDBERRY(20,3, 5, 12, 12, 8, 350, 12, 1951);

        companion object {
            fun getLevel(level: Int): flowerPatch? = flowerPatch.values().singleOrNull { it.level == level }
        }
    }

    enum class fruitTreePatch(val level : Int, val startConfig : Int, val stages : Int, val plantExp : Int, val healthId : Int, val clearId : Int, val checkExp : Int, val harvestExp : Int, val harvestItem: Int) {
        APPLE(25,3, 5, 12, 12, 8, 350, 12, 1951);

        companion object {
            fun getLevel(level: Int): flowerPatch? = flowerPatch.values().singleOrNull { it.level == level }
        }
    }

    enum class treePatch(val level : Int, val startConfig : Int, val stages : Int, val plantExp : Int, val healthId : Int, val treeId : Int, val stumpId : Int, val checkExp : Int, val harvestItem: Int) {
        ACORN(15,3, 5, 12, 12, 8, 9,350,1951);

        companion object {
            fun getLevel(level: Int): flowerPatch? = flowerPatch.values().singleOrNull { it.level == level }
        }
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

}