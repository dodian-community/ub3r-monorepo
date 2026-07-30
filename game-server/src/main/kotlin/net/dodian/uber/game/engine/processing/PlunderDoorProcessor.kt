package net.dodian.uber.game.engine.processing

import net.dodian.uber.skills.thieving.ThievingModule

class PlunderDoorProcessor : Runnable {
    private var hourTick = 4

    override fun run() {
        hourTick--
        val rotate = hourTick == 0
        if (rotate) {
            hourTick = 4
        }
        ThievingModule.advanceDoorCycle(rotate)
    }
}
