package net.dodian.uber.game.content.skills.agility

import net.dodian.uber.game.event.GameEventScheduler
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.item.Equipment
import net.dodian.uber.game.model.item.Ground
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.content.skills.core.progression.SkillProgressionService
import net.dodian.uber.game.content.skills.core.runtime.SkillingRandomEventService
import net.dodian.uber.game.systems.world.npc.NpcSpawnLocator
import net.dodian.utilities.Misc

class WerewolfCourseService(private val c: Client) {
    fun giveEndExperience(xp: Int, ringCheck: Boolean) {
        var awardedXp = xp
        if (ringCheck && c.equipment[Equipment.Slot.RING.id] == 4202) {
            awardedXp = (awardedXp * 1.1).toInt()
        }
        SkillProgressionService.gainXp(c, awardedXp, Skill.AGILITY)
        SkillingRandomEventService.trigger(c, awardedXp)
    }

    private fun runLater(delayMs: Int, action: () -> Unit) {
        GameEventScheduler.runLaterMs(delayMs, action)
    }

    private fun runRepeating(delayMs: Int, action: () -> Boolean) {
        GameEventScheduler.runRepeatingMs(delayMs, action)
    }

    private fun requireAgilityLevel(level: Int): Boolean {
        if (c.getLevel(Skill.AGILITY) >= level) {
            return true
        }
        c.send(SendMessage("You need level $level agility to use this!"))
        return false
    }

    fun StepStone(pos: Position) {
        if (c.UsingAgility) {
            return
        }
        if (!requireAgilityLevel(AgilityDefinitions.WEREWOLF_COURSE_LEVEL)) {
            return
        }
        if (pos.x == 3538 && pos.y == 9875 && c.position.x == 3538 && c.position.y == 9873) {
            c.UsingAgility = true
            val npc = NpcSpawnLocator.werewolfCourseNpc(0)
            c.requestAnim(769, 0)
            c.walkBlock = System.currentTimeMillis() + 600
            val x = 3533 + Misc.random(4)
            val y = 9910 + Misc.random(2)
            val originY = npc?.position?.y ?: c.position.y
            val originX = npc?.position?.x ?: c.position.x
            val offsetX = x - originY
            val offsetY = y - originX
            val distance = c.distanceToPoint(x, y)
            npc?.requestAnim(6547, 0)
            npc?.text = "Go fetch!"
            c.createProjectile(
                originY,
                originX,
                offsetY,
                offsetX,
                50,
                50 + (distance * 5),
                338,
                0,
                35,
                0,
                51,
                16,
                64,
            )
            runLater(600) {
                if (c.disconnected) {
                    return@runLater
                }
                c.transport(Position(pos.x, pos.y, 0))
                giveEndExperience(160, true)
                Ground.addFloorItem(c, Position(x, y, 0), 4179, 1, 100)
                c.UsingAgility = false
            }
        } else if (pos.x == 3538 && pos.y == 9877 && c.position.x == 3538 && c.position.y == 9875) {
            c.UsingAgility = true
            c.requestAnim(769, 0)
            c.walkBlock = System.currentTimeMillis() + 600
            runLater(600) {
                if (c.disconnected) {
                    return@runLater
                }
                c.transport(Position(pos.x, pos.y, 0))
                giveEndExperience(160, true)
                c.UsingAgility = false
            }
        } else if (pos.x == 3540 && pos.y == 9877 && c.position.x == 3538 && c.position.y == 9877) {
            c.UsingAgility = true
            c.requestAnim(769, 0)
            c.walkBlock = System.currentTimeMillis() + 600
            runLater(600) {
                if (c.disconnected) {
                    return@runLater
                }
                c.transport(Position(pos.x, pos.y, 0))
                giveEndExperience(160, true)
                c.UsingAgility = false
            }
        } else if (pos.x == 3540 && pos.y == 9879 && c.position.x == 3540 && c.position.y == 9877) {
            c.UsingAgility = true
            c.requestAnim(769, 0)
            c.walkBlock = System.currentTimeMillis() + 600
            runLater(600) {
                if (c.disconnected) {
                    return@runLater
                }
                c.transport(Position(pos.x, pos.y, 0))
                giveEndExperience(160, true)
                c.UsingAgility = false
            }
        } else if (pos.x == 3540 && pos.y == 9881 && c.position.x == 3540 && c.position.y == 9879) {
            c.UsingAgility = true
            c.requestAnim(769, 0)
            c.walkBlock = System.currentTimeMillis() + 600
            runLater(600) {
                if (c.disconnected) {
                    return@runLater
                }
                c.transport(Position(pos.x, pos.y, 0))
                giveEndExperience(160, true)
                c.UsingAgility = false
            }
        }
    }

    fun hurdle(pos: Position) {
        if (c.UsingAgility) {
            return
        }
        if (!requireAgilityLevel(AgilityDefinitions.WEREWOLF_COURSE_LEVEL)) {
            return
        }
        if (pos.x in 3537..3543 && pos.y == 9893 && c.position.x in 3537..3543 && c.position.y == 9892) {
            c.UsingAgility = true
            val npc = NpcSpawnLocator.werewolfCourseNpc(1)
            npc?.text = "GO GO GO!"
            c.setFocus(pos.x, pos.y)
            c.requestAnim(2750, 0)
            c.walkBlock = System.currentTimeMillis() + 600
            runLater(600) {
                if (c.disconnected) {
                    return@runLater
                }
                c.transport(Position(pos.x, pos.y + 1, 0))
                giveEndExperience(160, true)
                c.UsingAgility = false
            }
        } else if (pos.x in 3537..3543 && pos.y == 9896 && c.position.x in 3537..3543 && c.position.y == 9895) {
            c.UsingAgility = true
            c.setFocus(pos.x, pos.y)
            c.requestAnim(2750, 0)
            c.walkBlock = System.currentTimeMillis() + 600
            runLater(600) {
                if (c.disconnected) {
                    return@runLater
                }
                c.transport(Position(pos.x, pos.y + 1, 0))
                giveEndExperience(160, true)
                c.UsingAgility = false
            }
        } else if (pos.x in 3537..3543 && pos.y == 9899 && c.position.x in 3537..3543 && c.position.y == 9898) {
            c.UsingAgility = true
            c.setFocus(pos.x, pos.y)
            c.requestAnim(2750, 0)
            c.walkBlock = System.currentTimeMillis() + 600
            runLater(600) {
                if (c.disconnected) {
                    return@runLater
                }
                c.transport(Position(pos.x, pos.y + 1, 0))
                giveEndExperience(160, true)
                c.UsingAgility = false
            }
        }
    }

    fun pipe(pos: Position) {
        if (c.UsingAgility) {
            return
        }
        if (!requireAgilityLevel(AgilityDefinitions.WEREWOLF_COURSE_LEVEL)) {
            return
        }
        if ((pos.x == 3538 || pos.x == 3541 || pos.x == 3544) && pos.y == 9905 && c.position.x == pos.x && c.position.y == pos.y - 1) {
            c.UsingAgility = true
            val npc = NpcSpawnLocator.werewolfCourseNpc(2)
            npc?.text = "You smell good!!"
            c.setWalkAnim(746)
            c.AddToWalkCords(0, 6, 3600)
            var part = 0
            runRepeating(600) {
                if (c.disconnected) {
                    return@runRepeating false
                }
                part++
                when {
                    part == 6 -> {
                        c.requestWeaponAnims()
                        c.requestAnim(748, 0)
                        giveEndExperience(600, true)
                        c.UsingAgility = false
                        false
                    }
                    part == 1 -> {
                        c.setWalkAnim(747)
                        true
                    }
                    else -> true
                }
            }
        }
    }

    fun slope(pos: Position) {
        if (c.UsingAgility) {
            return
        }
        if (!requireAgilityLevel(60)) {
            return
        }
        if (pos.x == 3532 && pos.y in 9909..9911 && c.position.x == pos.x + 1 && c.position.y in 9909..9911) {
            c.UsingAgility = true
            val npc = NpcSpawnLocator.werewolfCourseNpc(3)
            npc?.text = "You fetch good... Now bleed!"
            c.setWalkAnim(737)
            c.AddToWalkCords(-3, 0, 1800)
            runLater(1800) {
                if (c.disconnected) {
                    return@runLater
                }
                c.requestWeaponAnims()
                giveEndExperience(400, true)
                c.UsingAgility = false
            }
        }
    }

    fun zipLine(pos: Position) {
        if (c.UsingAgility) {
            return
        }
        if (!requireAgilityLevel(60)) {
            return
        }
        if (pos.x in 3527..3529 && pos.y in 9909..9911 && c.position.x == 3528 && (c.position.y == 9910 || c.position.y == 9909)) {
            c.UsingAgility = true
            c.setFocus(3528, 9908)
            val npc = NpcSpawnLocator.werewolfCourseNpc(4)
            val lastNpc = NpcSpawnLocator.werewolfCourseNpc(5)
            npc?.text = "Run adventurer.. RUN!!!"
            c.setRunAnim(904)
            val time = 22800 / 2
            c.AddToRunCords(0, -38, time.toLong())
            runLater(time) {
                if (c.disconnected) {
                    return@runLater
                }
                c.requestWeaponAnims()
                giveEndExperience(1500, true)
                lastNpc?.text = "Hand in that juicy stick to me!"
                c.UsingAgility = false
            }
        }
    }

    fun handStick() {
        if (c.playerHasItem(4179)) {
            c.showNPCChat(5927, 616, arrayOf("Thank you for that juicy stick.", "Have some agility knowledge!"))
            c.deleteItem(4179, 1)
            c.checkItemUpdate()
            giveEndExperience(4000, false)
        } else {
            c.showNPCChat(5927, 616, arrayOf("You do not have a stick to give me!"))
        }
    }
}
