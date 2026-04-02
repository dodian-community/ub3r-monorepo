package net.dodian.uber.game.content.items

import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.item.Ground

data class GlobalGroundSpawn(
    val position: Position,
    val itemId: Int,
    val amount: Int,
    val displayTime: Int,
)

class GlobalGroundSpawnBuilder {
    private val spawns = ArrayList<GlobalGroundSpawn>()

    fun group(name: String, block: GlobalGroundSpawnGroupBuilder.() -> Unit) {
        GlobalGroundSpawnGroupBuilder(name, spawns).apply(block)
    }

    fun build(): List<GlobalGroundSpawn> = spawns.toList()
}

class GlobalGroundSpawnGroupBuilder internal constructor(
    @Suppress("unused")
    private val name: String,
    private val spawns: MutableList<GlobalGroundSpawn>,
) {
    fun spawn(x: Int, y: Int, z: Int, itemId: Int, amount: Int, displayTime: Int) {
        spawns += GlobalGroundSpawn(Position(x, y, z), itemId, amount, displayTime)
    }
}

private fun globalGroundSpawns(block: GlobalGroundSpawnBuilder.() -> Unit): List<GlobalGroundSpawn> {
    val builder = GlobalGroundSpawnBuilder()
    builder.block()
    return builder.build()
}

object GlobalGroundSpawnContent {
    @JvmField
    val spawns: List<GlobalGroundSpawn> =
        globalGroundSpawns {
            group("Troll Items") {
                spawn(2611, 3096, 0, 11862, 1, 100)
                spawn(2612, 3096, 0, 11863, 1, 100)
                spawn(2563, 9511, 0, 1631, 1, 100)
                spawn(2564, 9511, 0, 6571, 1, 100)
            }
            group("Yanille Starter Items") {
                spawn(2605, 3104, 0, 1277, 1, 33)
                spawn(2607, 3104, 0, 1171, 1, 33)
            }
            group("Snape Grass") {
                spawn(2810, 3203, 0, 231, 1, 100)
                spawn(2807, 3204, 0, 231, 1, 100)
                spawn(2804, 3207, 0, 231, 1, 100)
                spawn(2801, 3210, 0, 231, 1, 100)
            }
            group("Limpwurt") {
                spawn(2874, 3475, 0, 225, 1, 100)
                spawn(2876, 3001, 0, 225, 1, 100)
            }
            group("White Berries") {
                spawn(2935, 3489, 0, 239, 1, 100)
                spawn(2877, 3000, 0, 239, 1, 100)
            }
            group("Red Spider Eggs") {
                spawn(3595, 3479, 0, 223, 1, 100)
                spawn(3597, 3479, 0, 223, 1, 100)
            }
            group("Seaweed Brimhaven") {
                spawn(2797, 3211, 0, 401, 1, 25)
                spawn(2795, 3212, 0, 401, 1, 25)
                spawn(2792, 3213, 0, 401, 1, 25)
                spawn(2789, 3214, 0, 401, 1, 25)
                spawn(2786, 3215, 0, 401, 1, 25)
                spawn(2784, 3217, 0, 401, 1, 25)
                spawn(2782, 3219, 0, 401, 1, 25)
                spawn(2779, 3219, 0, 401, 1, 25)
            }
            group("Seaweed Bandit Camp") {
                spawn(3143, 2991, 0, 401, 1, 25)
                spawn(3143, 2988, 0, 401, 1, 25)
                spawn(3145, 2985, 0, 401, 1, 25)
                spawn(3147, 2983, 0, 401, 1, 25)
                spawn(3149, 2980, 0, 401, 1, 25)
                spawn(3147, 2977, 0, 401, 1, 25)
                spawn(3145, 2975, 0, 401, 1, 25)
                spawn(3143, 2973, 0, 401, 1, 25)
            }
            group("Seaweed Catherby") {
                spawn(2860, 3427, 0, 401, 1, 25)
                spawn(2858, 3427, 0, 401, 1, 25)
                spawn(2856, 3426, 0, 401, 1, 25)
                spawn(2855, 3425, 0, 401, 1, 25)
                spawn(2852, 3425, 0, 401, 1, 25)
                spawn(2850, 3427, 0, 401, 1, 25)
                spawn(2848, 3429, 0, 401, 1, 25)
                spawn(2846, 3431, 0, 401, 1, 25)
            }
            group("Seaweed Ardougne") {
                spawn(2641, 3255, 0, 401, 1, 25)
                spawn(2644, 3254, 0, 401, 1, 25)
                spawn(2645, 3252, 0, 401, 1, 25)
                spawn(2645, 3250, 0, 401, 1, 25)
                spawn(2643, 3248, 0, 401, 1, 25)
                spawn(2641, 3246, 0, 401, 1, 25)
                spawn(2641, 3243, 0, 401, 1, 25)
                spawn(2642, 3240, 0, 401, 1, 25)
            }
        }

    @JvmStatic
    fun spawnAll() {
        for (spawn in spawns) {
            Ground.addGroundItem(spawn.position, spawn.itemId, spawn.amount, spawn.displayTime)
        }
    }
}
