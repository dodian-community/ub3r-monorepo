package net.dodian.uber.game.content.objects.impl.travel

import net.dodian.cache.`object`.GameObjectData
import net.dodian.uber.game.content.objects.ObjectContent
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.utilities.Misc
import java.util.Random

object TeleportObjects : ObjectContent {
    override val objectIds: IntArray = intArrayOf(
        823,
        133,
        410,
        1294,
        1591,
        17122,
        2352,
        2492,
        2796, 2797,
        2833,
        5960,
        9368, 9369,
        11833, 11834,
        11635,
        12260,
        14914,
        14847,
        16519, 16520,
        16665,
        16675, 16677,
        16680, 16681, 16683,
        17384, 17385, 17387,
        2156, 2158,
        20877,
        5553, 6702,
        25938, 25939,
    )

    override fun onFirstClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        return when (objectId) {
            1294 -> {
                client.transport(Position(2485, 9912, 0))
                true
            }
            133 -> {
                client.send(SendMessage("Welcome to the dragon lair!"))
                client.transport(Position(3235, 9366, 0))
                true
            }
            11635 -> {
                client.transport(Position(3543, 3463, 0))
                true
            }
            16680 -> {
                if (position.x == 2884 && position.y == 3397) {
                    if (client.getLevel(Skill.SLAYER) < 50) {
                        client.send(SendMessage("You need at least level 50 slayer to enter the Taverly Dungeon."))
                    } else {
                        client.transport(Position(2884, 9798, 0))
                    }
                    true
                } else {
                    false
                }
            }
            17384 -> {
                when {
                    position.x == 2892 && position.y == 3507 -> client.transport(Position(2893, 9907, 0))
                    position.x == 2677 && position.y == 3405 -> client.transport(Position(2677, 9806, 0))
                    position.x == 2594 && position.y == 3085 -> client.transport(Position(2594, 9486, 0))
                    else -> return false
                }
                true
            }
            17385 -> {
                when {
                    position.x == 2677 && position.y == 9805 -> client.transport(Position(2677, 3404, 0))
                    position.x == 2884 && position.y == 9797 -> client.transport(Position(2884, 3398, 0))
                    position.x == 2594 && position.y == 9485 -> client.transport(Position(2594, 3086, 0))
                    else -> return false
                }
                true
            }
            17387 -> {
                if (position.x == 2892 && position.y == 9907) {
                    client.transport(Position(2893, 3507, 0))
                    true
                } else {
                    false
                }
            }
            25939 -> {
                if (position.x == 2715 && position.y == 3470) {
                    client.transport(Position(2715, 3471, 0))
                    true
                } else {
                    false
                }
            }
            25938 -> {
                if (position.x == 2715 && position.y == 3470) {
                    client.transport(Position(2714, 3470, 1))
                    true
                } else {
                    false
                }
            }
            16675 -> {
                when {
                    position.x == 2488 && position.y == 3407 -> client.transport(Position(2489, 3409, 1))
                    position.x == 2485 && position.y == 3402 -> client.transport(Position(2485, 3401, 1))
                    position.x == 2445 && position.y == 3434 -> client.transport(Position(2445, 3433, 1))
                    position.x == 2444 && position.y == 3414 -> client.transport(Position(2445, 3416, 1))
                    else -> return false
                }
                true
            }
            16677 -> {
                when {
                    position.x == 2489 && position.y == 3408 -> client.transport(Position(2488, 3406, 0))
                    position.x == 2485 && position.y == 3402 -> client.transport(Position(2485, 3404, 0))
                    position.x == 2445 && position.y == 3434 -> client.transport(Position(2446, 3436, 0))
                    position.x == 2445 && position.y == 3415 -> client.transport(Position(2444, 3413, 0))
                    else -> return false
                }
                true
            }
            16665 -> {
                when {
                    position.x == 2724 && position.y == 9774 -> {
                        if (!client.premium) {
                            client.resetPos()
                        }
                        client.transport(Position(2723, 3375, 0))
                    }
                    position.x == 2603 && position.y == 9478 -> client.transport(Position(2606, 3079, 0))
                    position.x == 2569 && position.y == 9522 -> client.transport(Position(2570, 3121, 0))
                    else -> return false
                }
                true
            }
            16683 -> {
                if (position.x == 2597 && position.y == 3107) {
                    client.transport(Position(2597, 3106, 1))
                    true
                } else {
                    false
                }
            }
            16681 -> {
                if (position.x == 2597 && position.y == 3107) {
                    client.transport(Position(2597, 3106, 0))
                    true
                } else {
                    false
                }
            }
            1591 -> {
                if (position.x == 3268 && position.y == 3435) {
                    if (client.determineCombatLevel() >= 80) {
                        client.transport(Position(2540, 4716, 0))
                    } else {
                        client.send(SendMessage("You need to be level 80 or above to enter the mage arena."))
                        client.send(SendMessage("The skeletons at the varrock castle are a good place until then."))
                    }
                    true
                } else {
                    false
                }
            }
            5960 -> {
                if (position.x == 2539 && position.y == 4712) {
                    client.transport(Position(3105, 3933, 0))
                    true
                } else {
                    false
                }
            }
            9369 -> {
                if (position.x == 2399 && position.y == 5176) {
                    if (client.position.y == 5177) {
                        client.transport(Position(2399, 5175, 0))
                    } else if (client.position.y == 5175) {
                        client.transport(Position(2399, 5177, 0))
                    }
                    true
                } else {
                    false
                }
            }
            9368 -> {
                if (position.x == 2399 && position.y == 5168) {
                    if (client.position.y == 5169) {
                        client.transport(Position(2399, 5167, 0))
                    }
                    true
                } else {
                    false
                }
            }
            11833 -> {
                if (position.x == 2437 && position.y == 5166) {
                    client.send(SendMessage("You have entered the Jad Cave."))
                    client.transport(Position(2413, 5117, 0))
                    true
                } else {
                    false
                }
            }
            11834 -> {
                if (position.x == 2412 && position.y == 5118) {
                    client.send(SendMessage("You have left the Jad Cave."))
                    client.transport(Position(2438, 5168, 0))
                    true
                } else {
                    false
                }
            }
            20877 -> {
                if (position.x == 2743 && position.y == 3153) {
                    if (!client.checkUnlock(0) && client.checkUnlockPaid(0) != 1) {
                        client.showNPCChat(2345, 596, arrayOf("You have not paid yet to enter my dungeon."))
                        true
                    } else {
                        client.addUnlocks(0, "0", if (client.checkUnlock(0)) "1" else "0")
                        client.showNPCChat(2345, 592, arrayOf("Welcome to my dungeon."))
                        client.transport(Position(3748, 9373 + Misc.random(1), 0))
                        true
                    }
                } else {
                    false
                }
            }
            5553 -> {
                if (position.x == 3749 && position.y == 9373) {
                    client.showNPCChat(2345, 593, arrayOf("Welcome back out from my dungeon."))
                    client.transport(Position(2744 + Misc.random(1), 3153, 0))
                    true
                } else {
                    false
                }
            }
            6702 -> {
                if (position.x == 3749 && position.y == 9374) {
                    client.showNPCChat(2345, 593, arrayOf("Welcome back out from my dungeon."))
                    client.transport(Position(2744 + Misc.random(1), 3153, 0))
                    true
                } else {
                    false
                }
            }
            14914 -> {
                if (!client.checkUnlock(1) && client.checkUnlockPaid(1) != 1) {
                    client.showNPCChat(2180, 596, arrayOf("You have not paid yet to enter my cave."))
                    true
                } else {
                    client.addUnlocks(1, "0", if (client.checkUnlock(1)) "1" else "0")
                    client.showNPCChat(2180, 592, arrayOf("Welcome to my cave."))
                    client.transport(Position(2444, 5169, 0))
                    client.GetBonus(true)
                    true
                }
            }
            2352 -> {
                if (position.x == 2443 && position.y == 5169) {
                    client.showNPCChat(2180, 593, arrayOf("Welcome back out from my cave."))
                    client.transport(Position(2848, 2991, 0))
                    client.GetBonus(true)
                    true
                } else {
                    false
                }
            }
            2492 -> {
                client.transport(Position(2591, 3087, 0))
                true
            }
            2158, 2156 -> {
                client.triggerTele(2921, 4844, 0, false)
                true
            }
            16520, 16519 -> {
                if (client.getLevel(Skill.AGILITY) < 50) {
                    client.send(SendMessage("You need level 50 agility to use this shortcut!"))
                    true
                } else {
                    if (position.x == 2575 && position.y == 3108) {
                        client.transport(Position(2575, 3112, 0))
                    } else if (position.x == 2575 && position.y == 3111) {
                        client.transport(Position(2575, 3107, 0))
                    }
                    true
                }
            }
            2833 -> {
                if (position.x == 2544 && position.y == 3111) {
                    client.transport(Position(2544, 3112, 1))
                }
                true
            }
            12260 -> {
                if (position.x == 2459 && position.y == 4354) {
                    client.transport(Position(2941, 4691, 0))
                } else {
                    client.transport(Position(2462, 4359, 0))
                }
                true
            }
            17122 -> {
                if (position.x == 2544 && position.y == 3111) {
                    client.transport(Position(2544, 3112, 0))
                }
                true
            }
            2796 -> {
                if (position.x == 2549 && position.y == 3111) {
                    client.transport(Position(2549, 3112, 2))
                }
                true
            }
            2797 -> {
                if (position.x == 2549 && position.y == 3111) {
                    client.transport(Position(2549, 3112, 1))
                }
                true
            }
            410 -> {
                if (position.x == 2925 && position.y == 3483) {
                    client.requestAnim(645, 0)
                    when (Misc.random(3)) {
                        1 -> client.transport(Position(2162 + Misc.random(2), 4831 + Misc.random(4), 0))
                        2 -> client.transport(Position(2140 + Misc.random(4), 4811 + Misc.random(2), 0))
                        3 -> client.transport(Position(2120 + Misc.random(2), 4831 + Misc.random(4), 0))
                        else -> client.transport(Position(2140 + Misc.random(4), 4853 + Misc.random(2), 0))
                    }
                    true
                } else {
                    false
                }
            }
            14847 -> {
                client.requestAnim(645, 0)
                client.transport(Position(2924, 3483, 0))
                true
            }
            else -> false
        }
    }

    override fun onSecondClick(client: Client, objectId: Int, position: Position, obj: GameObjectData?): Boolean {
        if (objectId == 823) {
            val random = Random()
            client.moveTo(2602 + random.nextInt(5), 3162 + random.nextInt(5), client.position.z)
            return true
        }
        return false
    }
}
