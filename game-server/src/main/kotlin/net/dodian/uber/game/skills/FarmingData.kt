package net.dodian.uber.game.skills

import net.dodian.uber.game.model.Position

class FarmingData {
    enum class allotmentPatch(val level : Int, val seed: Int, val plantXp : Int, val disease : Int, val config : Int, val stages : Int, val ticks : Int, val harvestXp : Int, val harvestItem : Int) {
        POTATO(1, 5318, 32, 180, 5, 5, 8, 36, 1942),
        ONION(5, 5319, 38, 190, 12, 5, 8, 42, 1957),
        CABBAGE(9, 5324, 40, 200, 19, 5, 8, 46, 1965),
        TOMATO(16, 5322, 50, 210, 26, 5, 8, 56, 1982),
        SWEETCORN(25, 5320, 68, 220, 33, 7, 8, 76, 5986),
        STRAWBERRY(31, 5323, 100, 230, 42, 7, 8, 116, 5504),
        WATERMELONS(47, 5321, 180, 240, 51, 9, 8, 208, 5982),
        SNAPE_GRASS(59, 22879, 300, 250, 19, 8, 8, 312, -1);

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
        MARIGOLD(3, 5096, 36, 185, 7, 5, 4, 180, 6010),
        ROSEMARY(13, 5097, 48, 190, 12, 5, 4, 260, 6014),
        NASTURTIUM(22, 5098, 80, 195, 17, 5, 4, 440, 6012),
        WOAD(26, 5099, 88, 200, 22, 5, 4, 480, 1793),
        LIMPWURT(34, 5100, 96, 210, 27, 5, 4, 500, 225);

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
        GUAM(8, 5291, 44, 210, 3, 5, 15, 50, 199),
        MARRENTILL(12, 5292, 54, 220, 10, 5, 15, 60, 201),
        TARROMIN(19, 5293, 64, 230, 17, 5, 15, 72, 203),
        HARRALANDER(999, 5294, 86, 240, 24, 5, 15, 96, -1),
        RANARR(34, 5295, 108, 250, 31, 5, 15, 122, 207),
        TOADFLAX(999, 5296, 154, 260, 38, 5, 15, 154, -1),
        IRIT(44, 5297, 172, 270, 45, 5, 15, 194, 209),
        AVANTOE(999, 5298, 218, 280, 52, 5, 15, 246, -1),
        KWUARM(56, 5299, 276, 290, 67, 5, 15, 312, 213),
        SNAPDRAGON(63, 5300, 350, 300, 74, 5, 15, 394, 3051),
        CADANTINE(70, 5301, 426, 310, 81, 5, 15, 480, 215),
        LANTADYME(999, 5302, 538, 320, 88, 5, 15, 606, -1),
        DWARF_WEED(79, 5303, 682, 330, 95, 5, 15, 768, 217),
        TORSTOL(85, 5304, 798, 340, 102, 5, 15, 898, 219);

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
        REDBERRY_BUSH(22, 5101, 44, 230, 4, 6, 16, 256, 20, 1951),
        CADAVABERRY_BUSH(68, 5102, 400, 230, 14, 7, 16, 2280, 136, 1987), //Might be grapes?!
        DWELLBERRY_BUSH(36, 5103, 120, 230, 25, 8, 16, 700, 48, 2126),
        JANGERBERRY_BUSH(48, 5104, 200, 230, 37, 9, 16, 1120, 76, 247),
        WHITEBERRY_BUSH(59, 5105, 312, 230, 50, 9, 16, 1720, 116, 239);

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
        APPLE_TREE(sapling.APPLE.farmLevel, sapling.APPLE.saplingId, 88, 240, 7, 7, 120, 5090, 36, 1955),
        BANANA_TREE(sapling.BANANA.farmLevel, sapling.BANANA.saplingId, 112, 240, 34, 7, 120, 7366, 44, 1963),
        ORANGE_TREE(sapling.ORANGE.farmLevel, sapling.ORANGE.saplingId, 140, 240, 71, 7, 120, 10340, 52, 2108),
        CURRY_TREE(sapling.CURRY.farmLevel, sapling.CURRY.saplingId, 160, 240, 98, 7, 120, 12140, 64, 5970),
        PINEAPPLE_TREE(sapling.PINEAPPLE.farmLevel, sapling.PINEAPPLE.saplingId, 220, 240, 135, 7, 120, 19160, 84, 2114),
        PAPAYA_TREE(sapling.PAPAYA.farmLevel, sapling.PAPAYA.saplingId, 280, 240, 162, 7, 120, 25520, 108, 5972),
        PALM_TREE(sapling.PALM.farmLevel, sapling.PALM.saplingId, 440, 240, 199, 7, 120, 42000, 140, 5974);

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
        OAK_TREE(sapling.OAK.farmLevel, sapling.OAK.saplingId, 60, 190, 7, 5, 30, 1860, 0,1521),
        WILLOW_TREE(sapling.WILLOW.farmLevel, sapling.WILLOW.saplingId, 100, 200, 14, 7, 30, 5800, 0, 1519),
        MAPLE_TREE(sapling.MAPLE.farmLevel, sapling.MAPLE.saplingId, 180, 210, 23, 9, 30, 13600, 0, 1517),
        YEW_TREE(sapling.YEW.farmLevel, sapling.YEW.saplingId, 320, 220, 34, 11, 30, 28000, 0, 1515),
        MAGIC_TREE(sapling.MAGIC.farmLevel, sapling.MAGIC.saplingId, 580, 230, 47, 13, 30, 54800, 0, 1513);

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
    fun canNote(id : Int, itemName : String) : Boolean {
        val checkName = itemName.replace("grimy ", "") //Herb special check!
        for(values in herbPatch.values())
            if(values.harvestItem == id || values.name.replace("_", " ").lowercase() == checkName) return true
        for(values in allotmentPatch.values())
            if(values.harvestItem == id) return true
        for(values in flowerPatch.values())
            if(values.harvestItem == id) return true
        for(values in bushPatch.values())
            if(values.harvestItem == id) return true
        for(values in fruitTreePatch.values())
            if(values.harvestItem == id) return true
        for(values in treePatch.values())
            if(values.harvestItem == id) return true
        return false
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
        //ARDOUGNE_WEST(Position(2489,3179,0), arrayOf(7963)), //Fruit tree patch, but need to update map so we got 3 fruit and 3 regular tree patches!
        CANIFIS_NORTH(Position(3601,3525,0), arrayOf(8556, 8557, 7850, 8153)),
        GNOME_STRONGHOLD_SOUTH(Position(2435,3414,0), arrayOf(19147)),
        GNOME_STRONGHOLD_EAST(Position(2475,3445,0), arrayOf(7962)),
        TAVERLY_SOUTH(Position(2935,3437,0), arrayOf(8388))
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
    fun checkSuperCompost(id : Int) : Boolean {
        return superCompostItems.indexOf(id) >= 0
    }
    fun checkRegularCompost(id : Int) : Boolean {
        return regularCompostItems.indexOf(id) >= 0
    }

    enum class compostBin(val updatePos: Position, val objectId: Int, val ticks: Int) {
        CATHERBY(Position(2804,3464,0), 7837, 1),
        ARDOUGNE(Position(2661,3375,0), 7839, 1),
        CANAFIS(Position(3610,3522,0), 7838, 1),
        FALADOR(Position(3333,3333,0), 1003, 60)
        ;
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
    val VOLCANIC_ASH = 21622

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