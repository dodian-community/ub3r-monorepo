package net.dodian.uber.game.model

import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException
import net.dodian.utilities.Utils

class ShopHandler {
    init {
        for (i in 0 until MaxShops) {
            for (j in 0 until MaxShopItems) {
                ResetItem(i, j)
                ShopItemsSN[i][j] = 0
            }
            ShopItemsStandard[i] = 0
            ShopSModifier[i] = 0
            ShopBModifier[i] = 0
            ShopName[i] = ""
        }
        TotalShops = 0
        loadShops("./data/shops.cfg")
    }

    fun DiscountItem(ShopID: Int, ArrayID: Int) {
        ShopItemsN[ShopID][ArrayID] -= 1
        if (ShopItemsN[ShopID][ArrayID] <= 0 && ArrayID >= ShopItemsStandard[ShopID]) {
            ShopItemsN[ShopID][ArrayID] = 0
            ResetItem(ShopID, ArrayID)
        }
    }

    fun ResetItem(ShopID: Int, ArrayID: Int) {
        ShopItems[ShopID][ArrayID] = 0
        ShopItemsN[ShopID][ArrayID] = 0
        ShopItemsDelay[ShopID] = 0
    }

    @Suppress("resource")
    fun loadShops(FileName: String): Boolean {
        var line: String
        val token3 = arrayOfNulls<String>((MaxShopItems * 2) + 4)

        val characterfile: BufferedReader =
            try {
                BufferedReader(FileReader("./$FileName"))
            } catch (_: FileNotFoundException) {
                Utils.println("$FileName: file not found.")
                return false
            }

        try {
            line = characterfile.readLine() ?: return false
        } catch (_: IOException) {
            Utils.println("$FileName: error loading file.")
            return false
        }

        var endOfFile = false
        while (!endOfFile && line.isNotEmpty()) {
            val trimmed = line.trim()
            val spot = trimmed.indexOf('=')
            if (spot > -1) {
                val token = trimmed.substring(0, spot).trim()
                val token2 = trimmed.substring(spot + 1).trim()
                val token2_2 = token2.replace("\t\t", "\t").replace("\t\t", "\t").replace("\t\t", "\t").replace("\t\t", "\t").replace("\t\t", "\t")
                val parsed = token2_2.split("\t")
                for (i in parsed.indices) {
                    token3[i] = parsed[i]
                }
                if (token == "shop") {
                    val ShopID = token3[0]!!.toInt()
                    ShopName[ShopID] = token3[1]!!.replace("_", " ")
                    ShopSModifier[ShopID] = token3[2]!!.toInt()
                    ShopBModifier[ShopID] = token3[3]!!.toInt()
                    for (i in 0 until ((parsed.size - 4) / 2)) {
                        val itemValue = token3[4 + (i * 2)] ?: break
                        ShopItems[ShopID][i] = itemValue.toInt() + 1
                        ShopItemsN[ShopID][i] = token3[5 + (i * 2)]!!.toInt()
                        ShopItemsSN[ShopID][i] = token3[5 + (i * 2)]!!.toInt()
                        ShopItemsStandard[ShopID]++
                    }
                    TotalShops++
                }
            } else if (trimmed == "[ENDOFSHOPLIST]") {
                try {
                    characterfile.close()
                } catch (_: IOException) {
                }
                return true
            }

            line =
                try {
                    characterfile.readLine() ?: break
                } catch (_: IOException) {
                    endOfFile = true
                    ""
                }
        }

        try {
            characterfile.close()
        } catch (_: IOException) {
        }
        return false
    }

    companion object {
        @JvmField var MaxShops: Int = 101
        @JvmField var MaxShopItems: Int = 40
        @JvmField var MaxShowDelay: Int = 100
        @JvmField var TotalShops: Int = 0

        @JvmField var ShopItems: Array<IntArray> = Array(MaxShops) { IntArray(MaxShopItems) }
        @JvmField var ShopItemsN: Array<IntArray> = Array(MaxShops) { IntArray(MaxShopItems) }
        @JvmField var ShopItemsDelay: IntArray = IntArray(MaxShops)
        @JvmField var ShopItemsSN: Array<IntArray> = Array(MaxShops) { IntArray(MaxShopItems) }
        @JvmField var ShopItemsStandard: IntArray = IntArray(MaxShops)
        @JvmField var ShopName: Array<String> = Array(MaxShops) { "" }
        @JvmField var ShopSModifier: IntArray = IntArray(MaxShops)
        @JvmField var ShopBModifier: IntArray = IntArray(MaxShops)

        @JvmStatic
        fun resetAnItem(ShopID: Int, ArrayID: Int) {
            ShopItems[ShopID][ArrayID] = -1
            ShopItemsN[ShopID][ArrayID] = 0
            ShopItemsDelay[ShopID] = 0
        }

        @JvmStatic
        fun findDefaultItem(shopId: Int, id: Int): Boolean {
            for (i in 0 until ShopItemsStandard[shopId]) {
                if (ShopItems[shopId][i] - 1 == id) {
                    return true
                }
            }
            return false
        }
    }
}
