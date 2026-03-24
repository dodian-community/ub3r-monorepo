package net.dodian.uber.game.skills.agility

import net.dodian.uber.game.event.GameEventScheduler
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.netty.listener.out.SendMessage

class DesertCarpetService(private val c: Client) {
    private fun runLater(delayMs: Int, action: () -> Unit) {
        GameEventScheduler.runLaterMs(delayMs, action)
    }

    private fun runRepeating(delayMs: Int, action: () -> Boolean) {
        GameEventScheduler.runRepeatingMs(delayMs, action)
    }

    private fun startRide(initialCount: Int, beforeLoop: () -> Unit, tickHandler: (Int) -> Boolean) {
        runLater(600) {
            beforeLoop()
            var count = initialCount
            runRepeating(600) {
                count++
                tickHandler(count)
            }
        }
    }

    fun sophanem(pos: Int) {
        if (c.UsingAgility) {
            return
        }
        if (c.position.x != 3286 && c.position.x != 3287) {
            return
        }
        var x = -1
        var y = -1
        if (c.position.x == 3287 && c.position.y == 2813) {
            y = 0
        }
        val extraStep = y == 0
        c.send(RemoveInterfaces())
        c.faceNpc(-1)
        c.UsingAgility = true
        c.clearTabs()
        c.AddToWalkCords(x, y, 600 + (600L * 35))
        startRide(if (extraStep) -1 else 0, {
            if (extraStep) {
                c.AddToWalkCords(-1, 0, 600 + (600L * 35))
            }
        }) { count ->
            val countAbove = count > 0
            if (countAbove && count < 20) {
                c.setWalkAnim(6936)
                c.setRunAnim(6936)
                c.requestAnim(6936, 0)
                c.AddToRunCords(if (pos == 2) -2 else if (pos == 1) 2 else 0, 2, 600L * (35 - count))
            } else if (count == 20) {
                c.showInterface(18460)
                c.requestAnim(6936, 0)
            } else if (countAbove && count < 23) {
                c.requestAnim(6936, 0)
            }
            when (pos) {
                0 -> {
                    if (count == 23) {
                        c.requestAnim(-1, 0)
                        c.showInterface(18452)
                        c.transport(Position(3349, 3003, 0))
                    }
                    if (count == 25) {
                        landed(c)
                        c.AddToWalkCords(0, -1, 600)
                        c.send(SendMessage("Welcome to Pollnivneach."))
                        return@startRide false
                    }
                }
                1 -> {
                    if (count == 26) {
                        c.requestAnim(-1, 0)
                        c.showInterface(18452)
                        c.transport(Position(3401, 2916, 0))
                    }
                    if (count == 28) {
                        landed(c)
                        c.AddToWalkCords(0, 1, 600)
                        c.send(SendMessage("Welcome to Nardah."))
                        return@startRide false
                    }
                }
                2 -> {
                    if (count == 29) {
                        c.requestAnim(-1, 0)
                        c.showInterface(18452)
                        c.transport(Position(3180, 3045, 0))
                    }
                    if (count == 31) {
                        landed(c)
                        c.AddToWalkCords(-1, 0, 600)
                        c.send(SendMessage("Welcome to Bedabin Camp."))
                        return@startRide false
                    }
                }
                else -> {
                    c.transport(Position(3287, 2813, 0))
                    return@startRide false
                }
            }
            true
        }
    }

    fun pollnivneach(pos: Int) {
        if (c.UsingAgility) {
            return
        }
        if (c.position.x < 3347 && c.position.x > 3349 && c.position.y != 3002 && c.position.y != 3003) {
            return
        }
        var x = 0
        var y = 1
        if ((c.position.x == 3347 && c.position.y == 3002) || (c.position.x == 3348 && c.position.y == 3003)) {
            y = 0
            x = 1
        }
        val extraStep = c.position.x == 3347 && c.position.y == 3002
        c.send(RemoveInterfaces())
        c.faceNpc(-1)
        c.UsingAgility = true
        c.clearTabs()
        c.AddToWalkCords(x, y, 600 + (600L * 35))
        startRide(if (extraStep) -1 else 0, {
            if (extraStep) {
                c.AddToWalkCords(1, 1, 600 + (600L * 35))
            }
        }) { count ->
            val countAbove = count > 0
            if (countAbove && count < 20) {
                c.setWalkAnim(6936)
                c.setRunAnim(6936)
                c.requestAnim(6936, 0)
                when (pos) {
                    0 -> {
                        if (count - 1 <= 2) {
                            c.AddToRunCords(2, 2, 600L * (35 - count))
                        } else {
                            c.AddToRunCords(2, 0, 600L * (35 - count))
                        }
                    }
                    1 -> c.AddToRunCords(-1, 1, 600L * (35 - count))
                    2 -> {
                        when {
                            count - 1 <= 1 -> c.AddToRunCords(2, 0, 600L * (35 - count))
                            count - 1 <= 14 -> c.AddToRunCords(0, -2, 600L * (35 - count))
                            count - 1 <= 16 -> c.AddToRunCords(2, 0, 600L * (35 - count))
                            count - 1 <= 18 -> c.AddToRunCords(0, -2, 600L * (35 - count))
                            else -> c.AddToRunCords(2, 0, 600L * (35 - count))
                        }
                    }
                }
            } else if (count == 20) {
                c.showInterface(18460)
                c.requestAnim(6936, 0)
            } else if (countAbove && count < 23) {
                c.requestAnim(6936, 0)
            }
            when (pos) {
                0 -> {
                    if (count == 23) {
                        c.requestAnim(-1, 0)
                        c.showInterface(18452)
                        c.transport(Position(3401, 2916, 0))
                    }
                    if (count == 25) {
                        landed(c)
                        c.AddToWalkCords(0, 1, 600)
                        c.send(SendMessage("Welcome to Nardah."))
                        false
                    } else {
                        true
                    }
                }
                1 -> {
                    if (count == 26) {
                        c.requestAnim(-1, 0)
                        c.showInterface(18452)
                        c.transport(Position(3180, 3045, 0))
                    }
                    if (count == 28) {
                        landed(c)
                        c.AddToWalkCords(-1, 0, 600)
                        c.send(SendMessage("Welcome to Bedabin Camp."))
                        false
                    } else {
                        true
                    }
                }
                2 -> {
                    if (count == 29) {
                        c.requestAnim(-1, 0)
                        c.showInterface(18452)
                        c.transport(Position(3285, 2813, 0))
                    }
                    if (count == 31) {
                        landed(c)
                        c.AddToWalkCords(0, -1, 600)
                        c.send(SendMessage("Welcome to Sophanem."))
                        false
                    } else {
                        true
                    }
                }
                else -> {
                    c.transport(Position(3350, 3001, 0))
                    false
                }
            }
        }
    }

    fun nardah(pos: Int) {
        if (c.UsingAgility) {
            return
        }
        if (c.position.y != 2918 && c.position.x != 3401) {
            return
        }
        var x = 0
        var y = -1
        if (c.position.x == 3400 && c.position.y == 2918) {
            x = 1
        }
        val extraStep = x == 1
        c.send(RemoveInterfaces())
        c.faceNpc(-1)
        c.UsingAgility = true
        c.clearTabs()
        c.AddToWalkCords(x, y, 600 + (600L * 35))
        startRide(if (extraStep) -1 else 0, {
            if (extraStep) {
                c.AddToWalkCords(0, -1, 600 + (600L * 35))
            }
        }) { count ->
            val countAbove = count > 0
            if (countAbove && count < 20) {
                c.setWalkAnim(6936)
                c.setRunAnim(6936)
                c.requestAnim(6936, 0)
                if (pos != 1 && count >= 18) {
                    c.AddToRunCords(-2, 0, 600L * (35 - count))
                } else {
                    c.AddToRunCords(-2, if (pos == 1) -2 else 2, 600L * (35 - count))
                }
            } else if (count == 20) {
                c.showInterface(18460)
                c.requestAnim(6936, 0)
            } else if (countAbove && count < 23) {
                c.requestAnim(6936, 0)
            }
            when (pos) {
                0 -> {
                    if (count == 23) {
                        c.requestAnim(-1, 0)
                        c.showInterface(18452)
                        c.transport(Position(3349, 3003, 0))
                    }
                    if (count == 25) {
                        landed(c)
                        c.AddToWalkCords(0, -1, 600)
                        c.send(SendMessage("Welcome to Pollnivneach."))
                        false
                    } else {
                        true
                    }
                }
                1 -> {
                    if (count == 26) {
                        c.requestAnim(-1, 0)
                        c.showInterface(18452)
                        c.transport(Position(3285, 2813, 0))
                    }
                    if (count == 28) {
                        landed(c)
                        c.AddToWalkCords(0, -1, 600)
                        c.send(SendMessage("Welcome to Sophanem."))
                        false
                    } else {
                        true
                    }
                }
                2 -> {
                    if (count == 29) {
                        c.requestAnim(-1, 0)
                        c.showInterface(18452)
                        c.transport(Position(3180, 3045, 0))
                    }
                    if (count == 31) {
                        landed(c)
                        c.AddToWalkCords(-1, 0, 600)
                        c.send(SendMessage("Welcome to Bedabin Camp."))
                        false
                    } else {
                        true
                    }
                }
                else -> {
                    c.transport(Position(3400, 2918, 0))
                    false
                }
            }
        }
    }

    fun bedabinCamp(pos: Int) {
        if (c.UsingAgility) {
            return
        }
        if (c.position.y != 3044 && c.position.y != 3046) {
            return
        }
        val x = -1
        var y = -1
        if (c.position.x == 3181 && c.position.y == 3044) {
            y = 1
        }
        c.send(RemoveInterfaces())
        c.faceNpc(-1)
        c.UsingAgility = true
        c.clearTabs()
        c.AddToWalkCords(x, y, 600 + (600L * 35L))
        var count = 0
        runRepeating(600) {
            count++
            val countAbove = count > 0
            if (countAbove && count < 20) {
                c.setWalkAnim(6936)
                c.setRunAnim(6936)
                c.requestAnim(6936, 0)
                c.AddToRunCords(if (pos < 2) 2 else 0, -2, 600L * (35 - count))
            } else if (count == 20) {
                c.showInterface(18460)
                c.requestAnim(6936, 0)
            } else if (countAbove && count < 23) {
                c.requestAnim(6936, 0)
            }
            when (pos) {
                0 -> {
                    if (count == 23) {
                        c.requestAnim(-1, 0)
                        c.showInterface(18452)
                        c.transport(Position(3349, 3003, 0))
                    }
                    if (count == 25) {
                        landed(c)
                        c.AddToWalkCords(0, -1, 600)
                        c.send(SendMessage("Welcome to Pollnivneach."))
                        false
                    } else {
                        true
                    }
                }
                1 -> {
                    if (count == 26) {
                        c.requestAnim(-1, 0)
                        c.showInterface(18452)
                        c.transport(Position(3401, 2916, 0))
                    }
                    if (count == 28) {
                        landed(c)
                        c.AddToWalkCords(0, 1, 600)
                        c.send(SendMessage("Welcome to Nardah."))
                        false
                    } else {
                        true
                    }
                }
                2 -> {
                    if (count == 29) {
                        c.requestAnim(-1, 0)
                        c.showInterface(18452)
                        c.transport(Position(3285, 2813, 0))
                    }
                    if (count == 31) {
                        landed(c)
                        c.AddToWalkCords(0, -1, 600)
                        c.send(SendMessage("Welcome to Sophanem."))
                        false
                    } else {
                        true
                    }
                }
                else -> {
                    c.transport(Position(3182, 3044, 0))
                    false
                }
            }
        }
    }

    private fun landed(c: Client) {
        c.requestWeaponAnims()
        c.send(RemoveInterfaces())
        c.UsingAgility = false
        c.resetTabs()
    }
}
