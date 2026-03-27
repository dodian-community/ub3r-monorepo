package net.dodian.uber.game.content.skills.agility

import net.dodian.uber.game.event.GameEventScheduler
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.UpdateFlag
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.systems.api.content.ContentTiming
import net.dodian.uber.game.content.skills.core.progression.SkillProgressionService
import net.dodian.uber.game.content.skills.core.runtime.SkillingRandomEventService
import net.dodian.uber.game.systems.world.npc.NpcSpawnLocator
import net.dodian.utilities.Misc

class AgilityCourseService(private val c: Client) {
    fun giveEndExperience(xp: Int) {
        SkillProgressionService.gainXp(c, xp, Skill.AGILITY)
        SkillingRandomEventService.trigger(c, xp)
    }

    private fun runLater(delayMs: Int, action: () -> Unit) {
        GameEventScheduler.runLaterMs(delayMs, action)
    }

    private fun runRepeating(delayMs: Int, action: () -> Boolean) {
        GameEventScheduler.runRepeatingMs(delayMs, action)
    }

    private fun isBusy(): Boolean = c.UsingAgility

    private fun scheduleVerticalMove(
        destination: Position,
        delayMs: Long = 600L,
        onComplete: () -> Unit,
    ) {
        val token = c.beginVerticalTransition(delayMs)
        c.walkBlock = System.currentTimeMillis() + delayMs
        ContentTiming.scheduleGameThread(
            "agility-vertical",
            delayMs,
            "player=${c.playerName} destination=$destination pos=${c.position}",
        ) {
            if (c.disconnected) {
                c.clearVerticalTransition()
                c.UsingAgility = false
                return@scheduleGameThread
            }
            c.finishVerticalTransition(token, destination)
            onComplete()
        }
    }

    private fun requireAgilityLevel(level: Int): Boolean {
        if (c.getLevel(Skill.AGILITY) >= level) {
            return true
        }
        c.send(SendMessage("You need level $level agility to use this!"))
        return false
    }

    fun GnomeLog() {
        if (isBusy()) {
            return
        }
        if (c.position.x == 2474 && c.position.y == 3436) {
            c.UsingAgility = true
            c.setWalkAnim(762)
            val time = 4800
            c.AddToWalkCords(0, -7, time.toLong())
            runLater(time) {
                c.requestWeaponAnims()
                giveEndExperience(280)
                c.agilityCourseStage = if (c.agilityCourseStage >= 0) 1 else c.agilityCourseStage
                c.UsingAgility = false
            }
        }
    }

    fun GnomeNet1() {
        val npc = NpcSpawnLocator.gnomeCourseNpc(0)
        if (isBusy()) {
            return
        }
        if ((c.position.x < 2471 && c.position.x > 2476) || c.position.y != 3426) {
            return
        }
        c.UsingAgility = true
        npc?.text = "My mom is faster than you!"
        c.requestAnim(828, 0)
        scheduleVerticalMove(Position(2473, 3424, 1)) {
            giveEndExperience(150)
            c.agilityCourseStage = if (c.agilityCourseStage >= 1) 2 else c.agilityCourseStage
            c.UsingAgility = false
        }
    }

    fun GnomeTree1() {
        val npc = NpcSpawnLocator.gnomeCourseNpc(1)
        if (isBusy()) {
            return
        }
        c.UsingAgility = true
        npc?.text = "Haha you suck at this simple obstacle!"
        c.requestAnim(828, 0)
        c.agilityCourseStage = if (c.agilityCourseStage >= 2) 3 else c.agilityCourseStage
        scheduleVerticalMove(Position(2473, 3420, 2)) {
            giveEndExperience(50)
            c.agilityCourseStage = if (c.agilityCourseStage >= 2) 3 else c.agilityCourseStage
            c.UsingAgility = false
        }
    }

    fun GnomeRope() {
        val npc = NpcSpawnLocator.gnomeCourseNpc(2)
        if (isBusy()) {
            return
        }
        if (c.position.x != 2477 || c.position.y != 3420) {
            return
        }
        c.UsingAgility = true
        npc?.text = "I do not know why you bother. HAHA!"
        c.setWalkAnim(762)
        val time = 4800
        c.AddToWalkCords(6, 0, time.toLong())
        runLater(time) {
            c.requestWeaponAnims()
            giveEndExperience(250)
            c.agilityCourseStage = if (c.agilityCourseStage >= 3) 4 else c.agilityCourseStage
            c.UsingAgility = false
        }
    }

    fun GnomeTreebranch2() {
        val npc = NpcSpawnLocator.gnomeCourseNpc(3)
        if (isBusy()) {
            return
        }
        c.UsingAgility = true
        npc?.text = "To darn easy."
        c.requestAnim(828, 0)
        scheduleVerticalMove(Position(2485, 3421, 0)) {
            giveEndExperience(50)
            c.agilityCourseStage = if (c.agilityCourseStage >= 4) 5 else c.agilityCourseStage
            c.UsingAgility = false
        }
    }

    fun GnomeNet2() {
        val npc = NpcSpawnLocator.gnomeCourseNpc(4)
        if (isBusy()) {
            return
        }
        if ((c.position.x < 2483 && c.position.x > 2488) || c.position.y != 3425) {
            return
        }
        c.UsingAgility = true
        npc?.text = "net profit of zero effort."
        c.requestAnim(828, 0)
        c.walkBlock = System.currentTimeMillis() + 600
        runLater(600) {
            c.teleportTo(c.position.x, c.position.y + 2, 0)
            giveEndExperience(150)
            c.agilityCourseStage = if (c.agilityCourseStage >= 5) 6 else c.agilityCourseStage
            c.UsingAgility = false
        }
    }

    fun GnomePipe() {
        val npc = NpcSpawnLocator.gnomeCourseNpc(5)
        if (isBusy()) {
            return
        }
        c.resetWalkingQueue()
        c.UsingAgility = true
        npc?.text = "Pipe it down...You are nothing special!"
        c.setWalkAnim(746)
        c.AddToWalkCords(0, 7, 4200)
        var part = 0
        runRepeating(600) {
            part++
            when {
                part > 0 && isMovementSettled() -> {
                    c.requestWeaponAnims()
                    c.requestAnim(748, 0)
                    c.updateFlags.setRequired(UpdateFlag.APPEARANCE, true)
                    if (c.agilityCourseStage == 6) {
                        c.addItem(2996, 1 + Misc.random(c.getLevel(Skill.AGILITY) / 11))
                        c.checkItemUpdate()
                        c.agilityCourseStage = 0
                        c.send(SendMessage("You finished a gnome lap!"))
                        giveEndExperience(1050)
                    } else {
                        giveEndExperience(250)
                    }
                    c.UsingAgility = false
                    false
                }
                part == 1 -> {
                    c.setWalkAnim(747)
                    c.updateFlags.setRequired(UpdateFlag.APPEARANCE, true)
                    true
                }
                else -> true
            }
        }
    }

    private fun isMovementSettled(): Boolean {
        return c.primaryDirection == -1 &&
            c.secondaryDirection == -1 &&
            c.wQueueReadPtr == c.wQueueWritePtr
    }

    fun BarbRope() {
        if (isBusy()) {
            return
        }
        if ((c.position.x < 2550 && c.position.x > 2552) || c.position.y != 3554) {
            return
        }
        if (!requireAgilityLevel(40)) {
            return
        }
        if (c.position.y == 3554 && c.position.x >= 2550 && c.position.x <= 2552) {
            c.UsingAgility = true
            val distance = 3549 - c.position.y
            c.setAgilityEmote(1501, 1501)
            c.AddToWalkCords(0, distance, distance * -1L * 600L)
            runLater(distance * -1 * 600) {
                c.requestWeaponAnims()
                giveEndExperience(400)
                c.agilityCourseStage = if (c.agilityCourseStage >= 0) 1 else c.agilityCourseStage
                c.UsingAgility = false
            }
        }
    }

    fun BarbLog() {
        if (isBusy()) {
            return
        }
        if (!requireAgilityLevel(40)) {
            return
        }
        c.UsingAgility = true
        val time = 7200
        if (c.position.y == 3547 || c.position.y == 3545) {
            c.AddToWalkCords(1, 0, time.toLong())
            var stage = 0
            runRepeating(600) {
                stage++
                when {
                    stage == 1 -> {
                        c.AddToWalkCords(0, if (c.position.y == 3547) -1 else 1, time.toLong())
                        stage++
                        true
                    }
                    stage > 3 -> {
                        c.setWalkAnim(762)
                        c.AddToWalkCords(-10, 0, time.toLong())
                        runLater(time) {
                            if (c.disconnected) {
                                return@runLater
                            }
                            c.requestWeaponAnims()
                            giveEndExperience(600)
                            c.agilityCourseStage = if (c.agilityCourseStage >= 1) 2 else c.agilityCourseStage
                            c.UsingAgility = false
                        }
                        false
                    }
                    else -> true
                }
            }
        } else if (c.position.x == 2551 && c.position.y == 3546) {
            c.setWalkAnim(762)
            c.AddToWalkCords(-10, 0, time.toLong())
            runLater(time) {
                c.requestWeaponAnims()
                giveEndExperience(600)
                c.agilityCourseStage = if (c.agilityCourseStage >= 1) 2 else c.agilityCourseStage
                c.UsingAgility = false
            }
        }
    }

    fun BarbNet() {
        if (isBusy()) {
            return
        }
        if (c.position.x != 2539 && (c.position.y != 3546 || c.position.y != 3545)) {
            return
        }
        if (!requireAgilityLevel(40)) {
            return
        }
        c.UsingAgility = true
        c.requestAnim(828, 0)
        c.walkBlock = System.currentTimeMillis() + 600
        runLater(600) {
            c.teleportTo(c.position.x - 2, c.position.y, 1)
            giveEndExperience(250)
            c.agilityCourseStage = if (c.agilityCourseStage >= 2) 3 else c.agilityCourseStage
            c.UsingAgility = false
        }
    }

    fun BarbLedge() {
        if (isBusy()) {
            return
        }
        if (!requireAgilityLevel(40)) {
            return
        }
        c.UsingAgility = true
        c.setWalkAnim(756)
        val time = 2400
        c.AddToWalkCords(-4, 0, time.toLong())
        runLater(time) {
            c.requestWeaponAnims()
            giveEndExperience(350)
            c.agilityCourseStage = if (c.agilityCourseStage >= 3) 4 else c.agilityCourseStage
            c.UsingAgility = false
        }
    }

    fun BarbStairs() {
        c.requestAnim(828, 0)
        c.teleportTo(c.position.x, c.position.y, 0)
        c.walkBlock = System.currentTimeMillis() + 600
    }

    fun BarbFirstWall() {
        if (isBusy()) {
            return
        }
        if (c.position.x != 2535 && c.position.y != 3553) {
            return
        }
        if (!requireAgilityLevel(40)) {
            return
        }
        c.UsingAgility = true
        c.setRunAnim(840)
        c.AddToRunCords(2, 0, 1200)
        runLater(600) {
            c.requestWeaponAnims()
            giveEndExperience(100)
            c.agilityCourseStage = if (c.agilityCourseStage >= 4) 5 else c.agilityCourseStage
            c.UsingAgility = false
        }
    }

    fun BarbSecondWall() {
        if (isBusy()) {
            return
        }
        if (c.position.x != 2535 && c.position.y != 3553) {
            return
        }
        if (!requireAgilityLevel(40)) {
            return
        }
        c.UsingAgility = true
        c.setRunAnim(840)
        c.AddToRunCords(2, 0, 1200)
        runLater(600) {
            c.requestWeaponAnims()
            giveEndExperience(100)
            c.agilityCourseStage = if (c.agilityCourseStage >= 5) 6 else c.agilityCourseStage
            c.UsingAgility = false
        }
    }

    fun BarbFinishWall() {
        if (isBusy()) {
            return
        }
        if (c.position.x != 2535 && c.position.y != 3553) {
            return
        }
        if (!requireAgilityLevel(40)) {
            return
        }
        c.UsingAgility = true
        c.setRunAnim(840)
        c.AddToRunCords(2, 0, 1200)
        runLater(600) {
            c.requestWeaponAnims()
            if (c.agilityCourseStage == 6) {
                c.addItem(2996, 2 + Misc.random(c.getLevel(Skill.AGILITY) / 22))
                c.checkItemUpdate()
                giveEndExperience(1300)
                c.send(SendMessage("You finished a barbarian lap!"))
                c.agilityCourseStage = 0
            } else {
                giveEndExperience(100)
            }
            c.UsingAgility = false
        }
    }

    fun WildyPipe() {
        if (c.position.x == 3004 && c.position.y == 3937) {
            if (isBusy()) {
                return
            }
            if (!requireAgilityLevel(70)) {
                return
            }
            if (c.position.x == 3004 && c.position.y == 3937) {
                c.UsingAgility = true
                val distance = 13
                c.requestAnim(746, 0)
                c.setWalkAnim(747)
                c.AddToWalkCords(0, distance, (distance * 600).toLong())
                var part = 0
                runRepeating(600) {
                    part++
                    if (part == distance - 1) {
                        c.requestWeaponAnims()
                        c.requestAnim(748, 1)
                        giveEndExperience(1000)
                        c.agilityCourseStage = if (c.agilityCourseStage >= 0) 1 else c.agilityCourseStage
                        c.UsingAgility = false
                        false
                    } else {
                        true
                    }
                }
            }
        }
    }

    fun WildyRope() {
        if (isBusy()) {
            return
        }
        if (!requireAgilityLevel(70)) {
            return
        }
        if (c.position.y == 3953 && c.position.x in 3003..3006) {
            c.UsingAgility = true
            val distance = 3958 - c.position.y
            c.setWalkAnim(1501)
            c.AddToWalkCords(0, distance, distance * 600L)
            runLater(distance * 600) {
                c.requestWeaponAnims()
                giveEndExperience(500)
                c.agilityCourseStage = if (c.agilityCourseStage >= 1) 2 else c.agilityCourseStage
                c.UsingAgility = false
            }
        }
    }

    fun WildyStones() {
        val validStart = c.position.x == 3002 && c.position.y == 3960
        if (validStart) {
            if (isBusy()) {
                return
            }
            if (!requireAgilityLevel(70)) {
                return
            }
            c.UsingAgility = true
            c.setFocus(2996, 3960)
            c.setWalkAnim(769)
            c.AddToWalkCords(-1, 0, 4200L)
            var parts = 0
            runRepeating(600) {
                parts++
                when {
                    parts > 1 && parts < 6 -> {
                        c.requestAnim(769, 0)
                        c.transport(Position(c.position.x - 1, c.position.y, 0))
                        true
                    }
                    parts > 1 -> {
                        c.requestWeaponAnims()
                        c.walkBlock = System.currentTimeMillis() + 600
                        c.transport(Position(c.position.x - 1, c.position.y, 0))
                        giveEndExperience(650)
                        c.agilityCourseStage = if (c.agilityCourseStage >= 2) 3 else c.agilityCourseStage
                        c.UsingAgility = false
                        false
                    }
                    else -> true
                }
            }
        }
    }

    fun WildyLog() {
        val time = 5600
        c.UsingAgility = true
        if (c.position.y == 3944 || c.position.y == 3946) {
            c.AddToWalkCords(1, 0, time.toLong())
            var stage = 0
            runRepeating(600) {
                stage++
                when {
                    stage == 1 -> {
                        c.AddToWalkCords(0, if (c.position.y == 3944) 1 else -1, time.toLong())
                        stage++
                        true
                    }
                    stage > 3 -> {
                        c.setWalkAnim(762)
                        c.AddToWalkCords(-8, 0, time.toLong())
                        runLater(time) {
                            if (c.disconnected) {
                                return@runLater
                            }
                            c.requestWeaponAnims()
                            giveEndExperience(650)
                            c.agilityCourseStage = if (c.agilityCourseStage >= 3) 4 else c.agilityCourseStage
                            c.UsingAgility = false
                        }
                        false
                    }
                    else -> true
                }
            }
        } else if (c.position.x == 3002 && c.position.y == 3945) {
            c.setWalkAnim(762)
            c.AddToWalkCords(-8, 0, time.toLong())
            runLater(time) {
                c.requestWeaponAnims()
                giveEndExperience(650)
                c.agilityCourseStage = if (c.agilityCourseStage >= 3) 4 else c.agilityCourseStage
                c.UsingAgility = false
            }
        }
    }

    fun WildyClimb() {
        if (c.position.x in 2993..2996 && c.position.y == 3937) {
            if (isBusy()) {
                return
            }
            if (!requireAgilityLevel(70)) {
                return
            }
            c.UsingAgility = true
            c.setWalkAnim(737)
            c.AddToWalkCords(0, -4, 2400L)
            runLater(2400) {
                c.requestWeaponAnims()
                if (c.agilityCourseStage == 4) {
                    c.addItem(2996, 3 + Misc.random(c.getLevel(Skill.AGILITY) / 33))
                    c.checkItemUpdate()
                    c.send(SendMessage("You finished a wilderness lap!"))
                    giveEndExperience(2700)
                    c.agilityCourseStage = 0
                } else {
                    giveEndExperience(750)
                }
                c.UsingAgility = false
            }
        }
    }

    fun orangeBar() {
        if (isBusy()) {
            return
        }
        if (!c.checkItem(1544)) {
            c.send(SendMessage("You need a orange key to use these bars!"))
            return
        }
        val time =
            if (c.position.x == 2600 || c.position.x == 2597) {
                1200
            } else if (c.position.y == 9488 || c.position.y == 9495) {
                600
            } else {
                0
            }
        c.AddToWalkCords(
            if (c.position.x == 2597) 1 else if (c.position.x == 2600) -1 else 0,
            if (c.position.y == 9488) 1 else if (c.position.y == 9495) -1 else 0,
            time.toLong(),
        )
        var stage = 0
        runRepeating(if (time > 0) 600 else 0) {
            stage++
            if (time == 1200 && stage < 2) {
                c.AddToWalkCords(0, 0, time.toLong())
                stage++
                return@runRepeating true
            }
            c.setWalkAnim(744)
            c.UsingAgility = true
            val distance =
                when (c.position.y) {
                    9488 -> 6
                    9489 -> 5
                    9494 -> -5
                    9495 -> -6
                    else -> 0
                }
            val duration =
                if (distance == 6 || distance == -6 || distance == 5 || distance == -5) {
                    6 * 600
                } else {
                    0
                }
            c.AddToWalkCords(0, distance, duration.toLong())
            runLater(duration) {
                if (c.disconnected) {
                    return@runLater
                }
                c.requestWeaponAnims()
                c.requestAnim(743, 0)
                c.UsingAgility = false
            }
            false
        }
    }

    fun yellowLedge() {
        if (isBusy() || c.position.x != 2580) {
            return
        }
        if (!c.checkItem(1545)) {
            c.send(SendMessage("You need a yellow key to use this ledge!"))
            return
        }
        val distance =
            when (c.position.y) {
                9512 -> 8
                9520 -> -8
                else -> 0
            }
        if (distance == 0) {
            return
        }
        c.UsingAgility = true
        c.setWalkAnim(if (distance == 8) 756 else 754)
        val time = 5400
        c.AddToWalkCords(0, distance, time.toLong())
        runLater(time) {
            c.requestWeaponAnims()
            c.UsingAgility = false
        }
    }

    fun kbdEntrance() {
        if (isBusy()) {
            return
        }
        val distance =
            when (c.position.x) {
                3304 -> 1
                3305 -> -1
                else -> 0
            }
        if (distance == 0) {
            return
        }
        c.UsingAgility = true
        c.ReplaceObject(3305, 9376, 6452, -3, 0)
        c.ReplaceObject(3305, 9375, 6451, -1, 0)
        val time = 600
        c.AddToWalkCords(distance, 0, time.toLong())
        runLater(time) {
            c.ReplaceObject(3305, 9376, 6452, 0, 0)
            c.ReplaceObject(3305, 9375, 6451, 0, 0)
            c.UsingAgility = false
        }
    }
}
