package net.dodian.uber.game.skill.agility

import net.dodian.cache.objects.GameObjectData
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.model.entity.UpdateFlag
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.skill.agility.runtime.AgilityPassageOverlayService
import net.dodian.uber.game.skill.agility.runtime.AgilityTraversalProfiles
import net.dodian.uber.game.skill.agility.runtime.AgilityTraversalService
import net.dodian.uber.game.skill.runtime.SkillActionContext
import net.dodian.uber.game.skill.runtime.SkillTraversalMovement
import net.dodian.uber.game.skill.runtime.SkillTraversalPlan
import net.dodian.uber.game.api.content.ContentInteraction
import net.dodian.uber.game.api.content.ContentObjectInteractionPolicy
import net.dodian.uber.game.api.content.ContentTiming
import net.dodian.uber.game.engine.systems.interaction.FirstClickDslObjectContent
import net.dodian.uber.game.engine.systems.interaction.firstClickObjectActions
import net.dodian.uber.game.engine.systems.skills.ProgressionService
import net.dodian.uber.game.api.plugin.skills.SkillPlugin
import net.dodian.uber.game.skill.runtime.action.SkillingRandomEventService
import net.dodian.uber.game.api.plugin.skills.skillPlugin
import net.dodian.uber.game.engine.systems.action.PolicyPreset
import net.dodian.uber.game.engine.systems.action.VerticalTransitionService
import net.dodian.uber.game.engine.systems.world.npc.NpcSpawnLocator
import net.dodian.uber.game.engine.systems.dialogue.DialogueService
import net.dodian.uber.game.engine.util.Misc

class Agility(private val c: Client) {
    private fun defaultActionContext(
        objectId: Int,
        option: Int = 1,
        objectPosition: Position = c.position.copy(),
    ): SkillActionContext = SkillActionContext(player = c, objectId = objectId, option = option, objectPosition = objectPosition)

    fun giveEndExperience(xp: Int) {
        ProgressionService.addXp(c, xp, Skill.AGILITY)
        SkillingRandomEventService.trigger(c, xp)
    }

    private fun runLater(delayMs: Int, action: () -> Unit) {
        ContentTiming.runLaterMs(delayMs) {
            action()
        }
    }

    private fun runRepeating(delayMs: Int, action: () -> Boolean) {
        ContentTiming.runRepeatingMs(delayMs) {
            action()
        }
    }

    private fun queueAgilityWalk(deltaX: Int, deltaY: Int, durationMs: Long) {
        AgilityPassageOverlayService.grantForDelta(c, deltaX, deltaY, durationMs)
        c.AddToWalkCords(deltaX, deltaY, durationMs)
    }

    private fun isBusy(): Boolean = c.isMovementLocked

    private fun scheduleVerticalMove(
        destination: Position,
        delayMs: Long = 600L,
        onComplete: () -> Unit,
    ) {
        val token = VerticalTransitionService.begin(c, delayMs)
        ContentTiming.scheduleGameThread(
            "agility-vertical",
            delayMs,
            "player=${c.playerName} destination=$destination pos=${c.position}",
        ) {
            if (c.disconnected) {
                VerticalTransitionService.clear(c)
                c.setMovementLocked(false)
                return@scheduleGameThread
            }
            VerticalTransitionService.finish(c, token, destination)
            onComplete()
        }
    }

    private fun requireAgilityLevel(level: Int): Boolean {
        if (c.getLevel(Skill.AGILITY) >= level) {
            return true
        }
        c.sendMessage("You need level $level agility to use this!")
        return false
    }

    fun GnomeLog(context: SkillActionContext = defaultActionContext(GnomeCourseObjectComponents.LOG_BALANCE)): Boolean {
        val start = Position(2474, 3436, 0)
        if (!c.position.equals(start)) {
            return false
        }
        val plan =
            SkillTraversalPlan(
                name = "agility.gnome.log_balance",
                movement = SkillTraversalMovement(deltaX = 0, deltaY = -7, durationMs = 4800, movementAnimationId = 762),
                passageEdges = { AgilityTraversalService.straightPathEdges(start = c.position.copy(), deltaX = 0, deltaY = -7) },
                onComplete = {
                    giveEndExperience(280)
                    c.agilitySessionStage = if (c.agilitySessionStage >= 0) 1 else c.agilitySessionStage
                },
            )
        return AgilityTraversalService.execute(context, plan)
    }

    fun GnomeNet1() {
        val npc = NpcSpawnLocator.gnomeCourseNpc(0)
        if (isBusy()) {
            return
        }
        if ((c.position.x < 2471 && c.position.x > 2476) || c.position.y != 3426) {
            return
        }
        c.setMovementLocked(true)
        npc?.text = "My mom is faster than you!"
        c.performAnimation(828, 0)
        scheduleVerticalMove(Position(2473, 3424, 1)) {
            giveEndExperience(150)
            c.agilitySessionStage = if (c.agilitySessionStage >= 1) 2 else c.agilitySessionStage
            c.setMovementLocked(false)
        }
    }

    fun GnomeTree1() {
        val npc = NpcSpawnLocator.gnomeCourseNpc(1)
        if (isBusy()) {
            return
        }
        c.setMovementLocked(true)
        npc?.text = "Haha you suck at this simple obstacle!"
        c.performAnimation(828, 0)
        c.agilitySessionStage = if (c.agilitySessionStage >= 2) 3 else c.agilitySessionStage
        scheduleVerticalMove(Position(2473, 3420, 2)) {
            giveEndExperience(50)
            c.agilitySessionStage = if (c.agilitySessionStage >= 2) 3 else c.agilitySessionStage
            c.setMovementLocked(false)
        }
    }

    fun GnomeRope(context: SkillActionContext = defaultActionContext(GnomeCourseObjectComponents.ROPE_SWING)): Boolean {
        val npc = NpcSpawnLocator.gnomeCourseNpc(2)
        if (c.position.x != 2477 || c.position.y != 3420) {
            return false
        }
        val start = c.position.copy()
        val plan =
            SkillTraversalPlan(
                name = "agility.gnome.balancing_rope",
                movement = SkillTraversalMovement(deltaX = 6, deltaY = 0, durationMs = 4800, movementAnimationId = 762),
                passageEdges = { AgilityTraversalService.straightPathEdges(start = start, deltaX = 6, deltaY = 0) },
                onStart = { npc?.text = "I do not know why you bother. HAHA!" },
                onComplete = {
                    giveEndExperience(250)
                    c.agilitySessionStage = if (c.agilitySessionStage >= 3) 4 else c.agilitySessionStage
                },
            )
        return AgilityTraversalService.execute(context, plan)
    }

    fun GnomeTreebranch2() {
        val npc = NpcSpawnLocator.gnomeCourseNpc(3)
        if (isBusy()) {
            return
        }
        c.setMovementLocked(true)
        npc?.text = "To darn easy."
        c.performAnimation(828, 0)
        scheduleVerticalMove(Position(2485, 3421, 0)) {
            giveEndExperience(50)
            c.agilitySessionStage = if (c.agilitySessionStage >= 4) 5 else c.agilitySessionStage
            c.setMovementLocked(false)
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
        c.setMovementLocked(true)
        npc?.text = "net profit of zero effort."
        c.performAnimation(828, 0)
        c.applyWalkBlockMs(600)
        runLater(600) {
            c.teleportTo(c.position.x, c.position.y + 2, 0)
            giveEndExperience(150)
            c.agilitySessionStage = if (c.agilitySessionStage >= 5) 6 else c.agilitySessionStage
            c.setMovementLocked(false)
        }
    }

    fun GnomePipe(
        context: SkillActionContext = defaultActionContext(GnomeCourseObjectComponents.PIPE_ENTRY_ONE),
    ): Boolean {
        val npc = NpcSpawnLocator.gnomeCourseNpc(5)
        if (isBusy()) {
            return false
        }
        if ((c.position.x != 2484 && c.position.x != 2487) || c.position.y != 3430) {
            return false
        }

        val profile = AgilityTraversalProfiles.profileForObjectId(context.objectId) ?: return false
        val plan =
            SkillTraversalPlan(
                name = "agility.gnome.pipe",
                movement =
                    SkillTraversalMovement(
                        deltaX = profile.deltaX,
                        deltaY = profile.deltaY,
                        durationMs = profile.durationMs,
                        movementAnimationId = profile.movementAnimationId,
                    ),
                onStart = {
                    npc?.text = "Pipe it down...You are nothing special!"
                    profile.startWalkAnimationId?.let { walkAnim ->
                        c.walkAnim = walkAnim
                        c.updateFlags.setRequired(UpdateFlag.APPEARANCE, true)
                    }
                },
                onComplete = {
                    c.requestWeaponAnims()
                    profile.completionAnimationId?.let { c.performAnimation(it, 0) }
                    c.updateFlags.setRequired(UpdateFlag.APPEARANCE, true)
                    if (c.agilitySessionStage == 6) {
                        c.addItem(2996, 1 + Misc.random(c.getLevel(Skill.AGILITY) / 11))
                        c.checkItemUpdate()
                        c.agilitySessionStage = 0
                        c.sendMessage("You finished a gnome lap!")
                        giveEndExperience(1050)
                    } else {
                        giveEndExperience(250)
                    }
                },
                passageEdges = {
                    AgilityTraversalService.straightPathEdges(
                        start = c.position.copy(),
                        deltaX = profile.deltaX,
                        deltaY = profile.deltaY,
                    )
                },
            )
        return AgilityTraversalService.execute(context, plan)
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
            c.setMovementLocked(true)
            val distance = 3549 - c.position.y
            c.setAgilityEmote(1501, 1501)
            queueAgilityWalk(0, distance, distance * -1L * 600L)
            runLater(distance * -1 * 600) {
                c.requestWeaponAnims()
                giveEndExperience(400)
                c.agilitySessionStage = if (c.agilitySessionStage >= 0) 1 else c.agilitySessionStage
                c.setMovementLocked(false)
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
        c.setMovementLocked(true)
        val time = 7200
        if (c.position.y == 3547 || c.position.y == 3545) {
            queueAgilityWalk(1, 0, time.toLong())
            var stage = 0
            runRepeating(600) {
                stage++
                when {
                    stage == 1 -> {
                        queueAgilityWalk(0, if (c.position.y == 3547) -1 else 1, time.toLong())
                        stage++
                        true
                    }
                    stage > 3 -> {
                        c.walkAnim = 762
                        queueAgilityWalk(-10, 0, time.toLong())
                        runLater(time) {
                            if (c.disconnected) {
                                return@runLater
                            }
                            c.requestWeaponAnims()
                            giveEndExperience(600)
                            c.agilitySessionStage = if (c.agilitySessionStage >= 1) 2 else c.agilitySessionStage
                            c.setMovementLocked(false)
                        }
                        false
                    }
                    else -> true
                }
            }
        } else if (c.position.x == 2551 && c.position.y == 3546) {
            c.walkAnim = 762
            queueAgilityWalk(-10, 0, time.toLong())
            runLater(time) {
                c.requestWeaponAnims()
                giveEndExperience(600)
                c.agilitySessionStage = if (c.agilitySessionStage >= 1) 2 else c.agilitySessionStage
                c.setMovementLocked(false)
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
        c.setMovementLocked(true)
        c.performAnimation(828, 0)
        c.applyWalkBlockMs(600)
        runLater(600) {
            c.teleportTo(c.position.x - 2, c.position.y, 1)
            giveEndExperience(250)
            c.agilitySessionStage = if (c.agilitySessionStage >= 2) 3 else c.agilitySessionStage
            c.setMovementLocked(false)
        }
    }

    fun BarbLedge() {
        if (isBusy()) {
            return
        }
        if (!requireAgilityLevel(40)) {
            return
        }
        c.setMovementLocked(true)
        c.walkAnim = 756
        val time = 2400
        queueAgilityWalk(-4, 0, time.toLong())
        runLater(time) {
            c.requestWeaponAnims()
            giveEndExperience(350)
            c.agilitySessionStage = if (c.agilitySessionStage >= 3) 4 else c.agilitySessionStage
            c.setMovementLocked(false)
        }
    }

    fun BarbStairs() {
        c.performAnimation(828, 0)
        c.teleportTo(c.position.x, c.position.y, 0)
        c.applyWalkBlockMs(600)
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
        c.setMovementLocked(true)
        c.runAnim = 840
        c.AddToRunCords(2, 0, 1200)
        runLater(600) {
            c.requestWeaponAnims()
            giveEndExperience(100)
            c.agilitySessionStage = if (c.agilitySessionStage >= 4) 5 else c.agilitySessionStage
            c.setMovementLocked(false)
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
        c.setMovementLocked(true)
        c.runAnim = 840
        c.AddToRunCords(2, 0, 1200)
        runLater(600) {
            c.requestWeaponAnims()
            giveEndExperience(100)
            c.agilitySessionStage = if (c.agilitySessionStage >= 5) 6 else c.agilitySessionStage
            c.setMovementLocked(false)
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
        c.setMovementLocked(true)
        c.runAnim = 840
        c.AddToRunCords(2, 0, 1200)
        runLater(600) {
            c.requestWeaponAnims()
            if (c.agilitySessionStage == 6) {
                c.addItem(2996, 2 + Misc.random(c.getLevel(Skill.AGILITY) / 22))
                c.checkItemUpdate()
                giveEndExperience(1300)
                c.sendMessage("You finished a barbarian lap!")
                c.agilitySessionStage = 0
            } else {
                giveEndExperience(100)
            }
            c.setMovementLocked(false)
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
                c.setMovementLocked(true)
                val distance = 13
                c.performAnimation(746, 0)
                c.walkAnim = 747
                queueAgilityWalk(0, distance, (distance * 600).toLong())
                var part = 0
                runRepeating(600) {
                    part++
                    if (part == distance - 1) {
                        c.requestWeaponAnims()
                        c.performAnimation(748, 1)
                        giveEndExperience(1000)
                        c.agilitySessionStage = if (c.agilitySessionStage >= 0) 1 else c.agilitySessionStage
                        c.setMovementLocked(false)
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
            c.setMovementLocked(true)
            val distance = 3958 - c.position.y
            c.walkAnim = 1501
            queueAgilityWalk(0, distance, distance * 600L)
            runLater(distance * 600) {
                c.requestWeaponAnims()
                giveEndExperience(500)
                c.agilitySessionStage = if (c.agilitySessionStage >= 1) 2 else c.agilitySessionStage
                c.setMovementLocked(false)
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
            c.setMovementLocked(true)
            c.setFocus(2996, 3960)
            c.walkAnim = 769
            queueAgilityWalk(-1, 0, 4200L)
            var parts = 0
            runRepeating(600) {
                parts++
                when {
                    parts > 1 && parts < 6 -> {
                        c.performAnimation(769, 0)
                        c.transport(Position(c.position.x - 1, c.position.y, 0))
                        true
                    }
                    parts > 1 -> {
                        c.requestWeaponAnims()
                        c.applyWalkBlockMs(600)
                        c.transport(Position(c.position.x - 1, c.position.y, 0))
                        giveEndExperience(650)
                        c.agilitySessionStage = if (c.agilitySessionStage >= 2) 3 else c.agilitySessionStage
                        c.setMovementLocked(false)
                        false
                    }
                    else -> true
                }
            }
        }
    }

    fun WildyLog() {
        val time = 5600
        c.setMovementLocked(true)
        if (c.position.y == 3944 || c.position.y == 3946) {
            queueAgilityWalk(1, 0, time.toLong())
            var stage = 0
            runRepeating(600) {
                stage++
                when {
                    stage == 1 -> {
                        queueAgilityWalk(0, if (c.position.y == 3944) 1 else -1, time.toLong())
                        stage++
                        true
                    }
                    stage > 3 -> {
                        c.walkAnim = 762
                        queueAgilityWalk(-8, 0, time.toLong())
                        runLater(time) {
                            if (c.disconnected) {
                                return@runLater
                            }
                            c.requestWeaponAnims()
                            giveEndExperience(650)
                            c.agilitySessionStage = if (c.agilitySessionStage >= 3) 4 else c.agilitySessionStage
                            c.setMovementLocked(false)
                        }
                        false
                    }
                    else -> true
                }
            }
        } else if (c.position.x == 3002 && c.position.y == 3945) {
            c.walkAnim = 762
            queueAgilityWalk(-8, 0, time.toLong())
            runLater(time) {
                c.requestWeaponAnims()
                giveEndExperience(650)
                c.agilitySessionStage = if (c.agilitySessionStage >= 3) 4 else c.agilitySessionStage
                c.setMovementLocked(false)
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
            c.setMovementLocked(true)
            c.walkAnim = 737
            queueAgilityWalk(0, -4, 2400L)
            runLater(2400) {
                c.requestWeaponAnims()
                if (c.agilitySessionStage == 4) {
                    c.addItem(2996, 3 + Misc.random(c.getLevel(Skill.AGILITY) / 33))
                    c.checkItemUpdate()
                    c.sendMessage("You finished a wilderness lap!")
                    giveEndExperience(2700)
                    c.agilitySessionStage = 0
                } else {
                    giveEndExperience(750)
                }
                c.setMovementLocked(false)
            }
        }
    }

    fun orangeBar() {
        if (isBusy()) {
            return
        }
        if (!c.checkItem(1544)) {
            c.sendMessage("You need a orange key to use these bars!")
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
        queueAgilityWalk(
            if (c.position.x == 2597) 1 else if (c.position.x == 2600) -1 else 0,
            if (c.position.y == 9488) 1 else if (c.position.y == 9495) -1 else 0,
            time.toLong(),
        )
        var stage = 0
        runRepeating(if (time > 0) 600 else 0) {
            stage++
            if (time == 1200 && stage < 2) {
                queueAgilityWalk(0, 0, time.toLong())
                stage++
                return@runRepeating true
            }
            c.walkAnim = 744
            c.setMovementLocked(true)
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
            queueAgilityWalk(0, distance, duration.toLong())
            runLater(duration) {
                if (c.disconnected) {
                    return@runLater
                }
                c.requestWeaponAnims()
                c.performAnimation(743, 0)
                c.setMovementLocked(false)
            }
            false
        }
    }

    fun yellowLedge() {
        if (isBusy() || c.position.x != 2580) {
            return
        }
        if (!c.checkItem(1545)) {
            c.sendMessage("You need a yellow key to use this ledge!")
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
        c.setMovementLocked(true)
        c.walkAnim = if (distance == 8) 756 else 754
        val time = 5400
        queueAgilityWalk(0, distance, time.toLong())
        runLater(time) {
            c.requestWeaponAnims()
            c.setMovementLocked(false)
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
        c.setMovementLocked(true)
        c.ReplaceObject(3305, 9376, 6452, -3, 0)
        c.ReplaceObject(3305, 9375, 6451, -1, 0)
        val time = 600
        queueAgilityWalk(distance, 0, time.toLong())
        runLater(time) {
            c.ReplaceObject(3305, 9376, 6452, 0, 0)
            c.ReplaceObject(3305, 9375, 6451, 0, 0)
            c.setMovementLocked(false)
        }
    }
}

object GnomeCourseObjectComponents {
    const val LOG_BALANCE = 23145
    const val NET_ONE = 23134
    const val TREE_BRANCH_UP = 23559
    const val ROPE_SWING = 23557
    val TREE_BRANCH_DOWN = intArrayOf(23560, 23561)
    const val NET_TWO = 23135
    const val PIPE_ENTRY_ONE = 23138
    const val PIPE_ENTRY_TWO = 23139
}

object GnomeCourseObjectBindings : FirstClickDslObjectContent(
    firstClickObjectActions {
        objectAction(GnomeCourseObjectComponents.LOG_BALANCE) { client, _, _, _ ->
            Agility(client).GnomeLog()
        }
        objectAction(GnomeCourseObjectComponents.NET_ONE) { client, _, position, _ ->
            if (client.distanceToPoint(position.x, position.y) >= 2) {
                return@objectAction false
            }
            Agility(client).GnomeNet1()
            true
        }
        objectAction(GnomeCourseObjectComponents.TREE_BRANCH_UP) { client, _, _, _ ->
            Agility(client).GnomeTree1()
            true
        }
        objectAction(GnomeCourseObjectComponents.ROPE_SWING) { client, _, _, _ ->
            Agility(client).GnomeRope()
        }
        objectAction(*GnomeCourseObjectComponents.TREE_BRANCH_DOWN) { client, _, _, _ ->
            Agility(client).GnomeTreebranch2()
            true
        }
        objectAction(GnomeCourseObjectComponents.NET_TWO) { client, _, position, _ ->
            if (client.distanceToPoint(position.x, position.y) >= 3) {
                return@objectAction false
            }
            Agility(client).GnomeNet2()
            true
        }
        objectAction(GnomeCourseObjectComponents.PIPE_ENTRY_ONE) { client, _, position, _ ->
            if (client.position.x != 2484 || client.position.y != 3430 || client.distanceToPoint(position.x, position.y) >= 2) {
                return@objectAction false
            }
            Agility(client).GnomePipe(
                SkillActionContext(
                    player = client,
                    objectId = GnomeCourseObjectComponents.PIPE_ENTRY_ONE,
                    option = 1,
                    objectPosition = position,
                ),
            )
            true
        }
        objectAction(GnomeCourseObjectComponents.PIPE_ENTRY_TWO) { client, _, position, _ ->
            if (client.position.x != 2487 || client.position.y != 3430 || client.distanceToPoint(position.x, position.y) >= 2) {
                return@objectAction false
            }
            Agility(client).GnomePipe(
                SkillActionContext(
                    player = client,
                    objectId = GnomeCourseObjectComponents.PIPE_ENTRY_TWO,
                    option = 1,
                    objectPosition = position,
                ),
            )
            true
        }
    },
) {
    override fun clickInteractionPolicy(
        option: Int,
        objectId: Int,
        position: Position,
        obj: GameObjectData?,
    ): ContentObjectInteractionPolicy? {
        if (option != 1 || objectId !in objectIds) {
            return null
        }
        return ContentInteraction.nearestBoundaryCardinalPolicy()
    }
}

object BarbarianCourseObjectComponents {
    const val ROPE_SWING = 23131
    const val LOG_BALANCE = 23144
    const val NET = 20211
    const val LEDGE = 23547
    const val STAIRS = 16682
    const val CRUMBLING_WALL = 1948
    const val ORANGE_BAR = 23567
    const val YELLOW_LEDGE = 23548
}

object BarbarianCourseObjectBindings : FirstClickDslObjectContent(
    firstClickObjectActions {
        objectAction(BarbarianCourseObjectComponents.ROPE_SWING) { client, _, _, _ ->
            Agility(client).BarbRope()
            true
        }
        objectAction(BarbarianCourseObjectComponents.LOG_BALANCE) { client, _, _, _ ->
            Agility(client).BarbLog()
            true
        }
        objectAction(BarbarianCourseObjectComponents.NET) { client, _, _, _ ->
            Agility(client).BarbNet()
            true
        }
        objectAction(BarbarianCourseObjectComponents.LEDGE) { client, _, _, _ ->
            Agility(client).BarbLedge()
            true
        }
        objectAction(BarbarianCourseObjectComponents.STAIRS) { client, _, _, _ ->
            Agility(client).BarbStairs()
            true
        }
        objectAction(BarbarianCourseObjectComponents.CRUMBLING_WALL) { client, _, position, _ ->
            val agility = Agility(client)
            when {
                position.x == 2536 && position.y == 3553 -> agility.BarbFirstWall()
                position.x == 2539 && position.y == 3553 -> agility.BarbSecondWall()
                position.x == 2542 && position.y == 3553 -> agility.BarbFinishWall()
                else -> return@objectAction false
            }
            true
        }
        objectAction(BarbarianCourseObjectComponents.ORANGE_BAR) { client, _, _, _ ->
            Agility(client).orangeBar()
            true
        }
        objectAction(BarbarianCourseObjectComponents.YELLOW_LEDGE) { client, _, _, _ ->
            Agility(client).yellowLedge()
            true
        }
    },
)

object WildernessCourseObjectComponents {
    const val PIPE = 23137
    const val ROPE = 23132
    const val STONES = 23556
    const val LOG_BALANCE = 23542
    const val CLIFF = 23640
}

object WildernessCourseObjectBindings : FirstClickDslObjectContent(
    firstClickObjectActions {
        objectAction(WildernessCourseObjectComponents.PIPE) { client, _, _, _ ->
            Agility(client).WildyPipe()
            true
        }
        objectAction(WildernessCourseObjectComponents.ROPE) { client, _, _, _ ->
            Agility(client).WildyRope()
            true
        }
        objectAction(WildernessCourseObjectComponents.STONES) { client, _, _, _ ->
            Agility(client).WildyStones()
            true
        }
        objectAction(WildernessCourseObjectComponents.LOG_BALANCE) { client, _, _, _ ->
            Agility(client).WildyLog()
            true
        }
        objectAction(WildernessCourseObjectComponents.CLIFF) { client, _, _, _ ->
            Agility(client).WildyClimb()
            true
        }
    },
)

object WerewolfCourseObjectComponents {
    const val STEPPING_STONE = 11643
    const val HURDLE = 11638
    const val PIPE = 11657
    const val SLOPE = 11641
    val ZIPLINE = intArrayOf(11644, 11645, 11646)
    const val ENTRY_GATE = 11636
}

object WerewolfCourseObjectBindings : FirstClickDslObjectContent(
    firstClickObjectActions {
        objectAction(WerewolfCourseObjectComponents.STEPPING_STONE) { client, _, position, _ ->
            AgilityWerewolf(client).StepStone(position)
            true
        }
        objectAction(WerewolfCourseObjectComponents.HURDLE) { client, _, position, _ ->
            AgilityWerewolf(client).hurdle(position)
            true
        }
        objectAction(WerewolfCourseObjectComponents.PIPE) { client, _, position, _ ->
            AgilityWerewolf(client).pipe(position)
            true
        }
        objectAction(WerewolfCourseObjectComponents.SLOPE) { client, _, position, _ ->
            AgilityWerewolf(client).slope(position)
            true
        }
        objectAction(*WerewolfCourseObjectComponents.ZIPLINE) { client, _, position, _ ->
            AgilityWerewolf(client).zipLine(position)
            true
        }
        objectAction(WerewolfCourseObjectComponents.ENTRY_GATE) { client, _, position, _ ->
            if (client.getLevel(Skill.AGILITY) >= 60) {
                client.ReplaceObject(position.x, position.y, WerewolfCourseObjectComponents.ENTRY_GATE, 2, 10)
                DialogueService.showNpcChat(client, 5928, 601, arrayOf("Welcome to the werewolf agility course!"))
                client.transport(Position(3549, 9865, 0))
            } else {
                DialogueService.showNpcChat(client, 5928, 616, arrayOf("Go and train your agility!"))
            }
            true
        }
    },
)
