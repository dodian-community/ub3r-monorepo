package net.dodian.uber.game.skills

import net.dodian.uber.game.model.Position

class FarmingData() {
    enum class allotmentPatch(val level : Int, val seed: Int, val plantXp : Int, val disease : Int, val config : Int, val stages : Int, val ticks : Int, val harvestXp : Int, val harvestItem : Int) {
        POTATO(1, 5318, 8, 220, 5, 5, 3, 22, 1942),
        ONION(9, 5319, 12, 220, 12, 6, 3, 32, 1957),
        CABBAGE(1, 5324, 12, 220, 12, 6, 3, 32, -1),
        TOMATO(1, 5322, 12, 220, 12, 6, 3, 32, -1),
        SWEETCORN(1, 5320, 12, 220, 12, 6, 3, 32, -1),
        STRAWBERRY(1, 5323, 12, 220, 12, 6, 3, 32, -1),
        WATERMELONS(1, 5321, 12, 220, 12, 6, 3, 32, -1);

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
        MARIGOLD(1, 5096, 8, 256, 7, 5, 4, 22, 6010),
        ROSEMARY(12, 5097, 8, 256, 15, 5, 4, 22, 6014),
        NASTURTIUM(18, 5098, 8, 256, 15, 5, 4, 22, 6012),
        WOAD(24, 5099, 8, 256, 15, 5, 4, 22, 1793),
        LIMPWURT(32, 5100, 8, 256, 15, 5, 4, 22, 225);

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
        GUAM(1, 5291, 12, 256, 3, 5, 3, 32, -1),
        MARRENTILL(1, 0, 12, 256, 10, 5, 3, 32, -1),
        TARROMIN(1, 0, 12, 256, 17, 5, 3, 32, -1),
        HARRALANDER(1, 0, 12, 256, 24, 5, 3, 32, -1),
        RANARR(1, 0, 12, 256, 31, 5, 3, 32, -1),
        TOADFLAX(1, 0, 12, 256, 38, 5, 3, 32, -1),
        IRIT(1, 0, 12, 256, 45, 5, 3, 32, -1),
        AVANTOE(1, 0, 12, 256, 52, 5, 3, 32, -1),
        KWUARM(1, 0, 12, 256, 67, 5, 3, 32, -1),
        SNAPDRAGON(1, 0, 12, 256, 74, 5, 3, 32, -1),
        CADANTINE(1, 0, 12, 256, 81, 5, 3, 32, -1),
        LANTADYME(1, 0, 12, 256, 88, 5, 3, 32, -1),
        DWARF_WEED(1, 0, 12, 256, 95, 5, 3, 32, -1),
        TORSTOL(1, 0, 12, 256, 102, 5, 3, 32, -1);

        companion object{
            fun find(id: Int): herbPatch? = herbPatch.values().find { it.seed == id }
            fun findSeed(id: Int): Boolean {
                for(values in herbPatch.values())
                    if(values.seed == id) return true
                return false
            }
        }
    }

    enum class bushPatch(val level : Int, val seed: Int, val plantXp : Int, val disease : Int, val config : Int, val stages : Int, val ticks : Int, val checkHealthXp : Int, val harvestXp : Int, val harvestItem : Int) {
        REDBERRY_BUSH(20, 5101, 8, 256, 4, 5, 3, 22, 22, 1951),
        CADAVABERRY_BUSH(1, 0, 8, 256, 14, 6, 3, 22, 22, -1),
        DWELLBERRY_BUSH(1, 0, 8, 256, 25, 6, 3, 22, 22, -1),
        JANGERBERRY_BUSH(1, 0, 8, 256, 37, 6, 3, 22, 22, -1),
        WHITEBERRY_BUSH(1, 0, 8, 256, 50, 6, 3, 22, 22, -1);

        companion object{
            fun find(id: Int): bushPatch? = bushPatch.values().find { it.seed == id }
            fun findSeed(id: Int): Boolean {
                for(values in bushPatch.values())
                    if(values.seed == id) return true
                return false
            }
        }
    }

    enum class fruitTreePatch(val level : Int, val seed: Int, val plantXp : Int, val disease : Int, val config : Int, val stages : Int, val ticks : Int, val checkHealthXp : Int, val harvestXp : Int, val harvestItem : Int) {
        APPLE_TREE(sapling.APPLE.farmLevel, sapling.APPLE.saplingId, 8, 256, 7, 7, 30, 22, 22, 1955),
        BANANA_TREE(sapling.BANANA.farmLevel, sapling.BANANA.saplingId, 12, 256, 34, 7, 30, 22, 22, 1963),
        ORANGE_TREE(sapling.ORANGE.farmLevel, sapling.ORANGE.saplingId, 14, 256, 71, 7, 30, 22, 22, 2108),
        CURRY_TREE(sapling.CURRY.farmLevel, sapling.CURRY.saplingId, 16, 256, 98, 7, 30, 22, 22, 5970),
        PINEAPPLE_TREE(sapling.PINEAPPLE.farmLevel, sapling.PINEAPPLE.saplingId, 18, 256, 135, 7, 30, 22, 22, 2114),
        PAPAYA_TREE(sapling.PAPAYA.farmLevel, sapling.PAPAYA.saplingId, 20, 256, 162, 7, 30, 22, 22, 5972),
        PALM_TREE(sapling.PALM.farmLevel, sapling.PALM.saplingId, 22, 256, 199, 7, 30, 22, 22, 5974);

        companion object{
            fun find(id: Int): fruitTreePatch? = fruitTreePatch.values().find { it.seed == id }
            fun findSeed(id: Int): Boolean {
                for(values in fruitTreePatch.values())
                    if(values.seed == id) return true
                return false
            }
        }
    }

    enum class treePatch(val level : Int, val seed: Int, val plantXp : Int, val disease : Int, val config : Int, val stages : Int, val ticks : Int, val checkHealthXp : Int, val harvestXp : Int, val harvestItem : Int) {
        OAK_TREE(sapling.OAK.farmLevel, sapling.OAK.saplingId, 8, 256, 7, 5, 30, 22, 22,1521),
        WILLOW_TREE(sapling.WILLOW.farmLevel, sapling.WILLOW.saplingId, 10, 256, 14, 7, 30, 22, 22, 1519),
        MAPLE_TREE(sapling.MAPLE.farmLevel, sapling.MAPLE.saplingId, 12, 256, 23, 9, 30, 22, 22, 1517),
        YEW_TREE(sapling.YEW.farmLevel, sapling.YEW.saplingId, 14, 256, 34, 11, 30, 22, 22, 1515),
        MAGIC_TREE(sapling.MAGIC.farmLevel, sapling.MAGIC.saplingId, 16, 256, 47, 13, 30, 22, 22, 1513);

        companion object{
            fun find(id: Int): treePatch? = treePatch.values().find { it.seed == id }
            fun findSeed(id: Int): Boolean {
                for(values in treePatch.values())
                    if(values.seed == id) return true
                return false
            }
        }
    }

    fun getPatchConfig(id : Int) : Int {
        return allotmentPatch.find(id)?.config ?: flowerPatch.find(id)?.config ?:
        herbPatch.find(id)?.config ?: bushPatch.find(id)?.config ?:
        fruitTreePatch.find(id)?.config ?: treePatch.find(id)?.config ?: 0
    }
    fun getDiseaseChance(id : Int) : Int {
        return allotmentPatch.find(id)?.disease ?: flowerPatch.find(id)?.disease ?:
        herbPatch.find(id)?.disease ?: bushPatch.find(id)?.disease ?:
        fruitTreePatch.find(id)?.disease ?: treePatch.find(id)?.disease ?: 0
    }
    fun getGrowTick(id : Int) : Int {
        return allotmentPatch.find(id)?.ticks ?: flowerPatch.find(id)?.ticks ?:
        herbPatch.find(id)?.ticks ?: bushPatch.find(id)?.ticks ?:
        fruitTreePatch.find(id)?.ticks ?: treePatch.find(id)?.ticks ?: 0
    }
    fun getEndStage(id : Int) : Int {
        return allotmentPatch.find(id)?.stages ?: flowerPatch.find(id)?.stages ?:
        herbPatch.find(id)?.stages ?: bushPatch.find(id)?.stages ?:
        fruitTreePatch.find(id)?.stages ?: treePatch.find(id)?.stages ?: 0
    }
    fun getLife(id : Int, compost : Int): Int {
        return if(allotmentPatch.find(id) != null || herbPatch.find(id) != null) 3 + compost else 1
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
    fun getCheckHealthXp(id : Int) : Int {
        return bushPatch.find(id)?.checkHealthXp ?: fruitTreePatch.find(id)?.checkHealthXp ?: treePatch.find(id)?.checkHealthXp ?: 0
    }
    fun getHarvestItem(id : Int) : Int {
        return allotmentPatch.find(id)?.harvestItem ?: flowerPatch.find(id)?.harvestItem ?:
        herbPatch.find(id)?.harvestItem ?: bushPatch.find(id)?.harvestItem ?:
        fruitTreePatch.find(id)?.harvestItem ?: treePatch.find(id)?.harvestItem ?: 0
    }
    fun getPlantName(id : Int) : String {
        return allotmentPatch.find(id)?.name ?: flowerPatch.find(id)?.name ?:
        herbPatch.find(id)?.name ?: bushPatch.find(id)?.name ?:
        fruitTreePatch.find(id)?.name ?: treePatch.find(id)?.name ?: ""
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
        PALM(63, 5289, 5486, 5494, 5502);
    }

    enum class patches(val updatePos: Position, val objectId: Array<Int>) {
        CATHERBY_WEST(Position(2809,3463,0), arrayOf(8552, 8553, 7848, 8151)),
        CATHERBY_EAST(Position(2860,3433,0), arrayOf(7965)),
        ARDOUGNE_EAST(Position(2666,3374,0), arrayOf(8554, 8555, 7849, 8152)),
        ARDOUGNE_SOUTH(Position(2617,3225,0), arrayOf(7580)),
        GNOME_STRONGHOLD_SOUTH(Position(2435,3414,0), arrayOf(19147)),
        GNOME_STRONGHOLD_EAST(Position(2475,3445,0), arrayOf(7962))
        ;
    }
    enum class patchState {
        WEED, GROWING, PROTECTED, DISEASE, DEAD, HARVEST, WATER, PRODUCTION, STUMP; //Two last are not common for all patches!
    }
    enum class compost(val itemId : Int, val divideValue: Int) { //chance/divideValue
        NONE(-1, 1), COMPOST(6032, 4), SUPERCOMPOST(6034, 8), ULTRACOMPOST(21483, 12);
        companion object {
            fun find(name: String): compost? = compost.values().find { it.name == name }
        }
    }

    enum class compostBin(val updatePos: Position, val objectId: Int, val ticks: Int) {
        CATHERBY(Position(2804,3464,0), 7837, 60),
        ARDOUGNE(Position(2661,3375,0), 7839, 60),
        CANAFIS(Position(3333,3333,0), 1002, 60),
        FALADOR(Position(3333,3333,0), 1003, 60)
    }
    enum class compostState {
        EMPTY, FILLED, CLOSED, DONE, OPEN
    }

    val regularCompostItems = listOf(6055, 6010, 6014, 6020, 1793, 5986, 5504, 1955, 1963, 2108, 5970,
        1957, 1942, 1965, 1951, 2126, 753, 1779, 401, 249, 199, 251, 201, 253, 203, 255, 205, 257, 207)
    val superCompostItems = listOf(2114, 5982, 5972, 5974, 5978, 5976, 231, 247, 239, 6018, 2998, 3049,
        261, 211, 263, 213, 3000, 3051, 265, 215, 2481, 2485, 267, 217, 269, 219, 259, 209)
    val BUCKET = 1925
    val SPADE = 952
    val RAKE = 5341
    val SEED_DIBBER = 5343
    val TROWEL = 5325
    val FILLED_PLANT_POT = 5354
    val EMPTY_PLANT_POT = 5350
    val SECATEURS = 5329
    val MAGIC_SECATEURS = 7409
    val PLANT_CURE = 6036

    val RAKE_ANIM = 2273
    val SPADE_ANIM = 830
    val WATERCAN_ANIM = 2293
    val PLANTSEED_ANIM = 2291
    val PRUNE_SECATEURS_ANIM = 2279
    val HARVEST_FRUIT_ANIM = 2280
    val HARVEST_BUSH_ANIM = 2281
    val HARVEST_ANIM = 2282
    val COMPOST_PATCH_ANIM = 2283
    val CURING_ANIM = 2288
    val FILL_PLANTPOT_ANIM = 2287

    val farmPatchConfig = 4771
    val compostBinConfig = 4775

}