package net.dodian.uber.game.engine.processing

import java.util.ArrayList
import net.dodian.uber.game.model.Position
import net.dodian.uber.game.content.skills.thieving.PyramidPlunder
import net.dodian.utilities.Misc

class PlunderDoorProcessor : Runnable {
    private var hourTick = 4

    override fun run() {
        hourTick--
        if (hourTick == 0) {
            val state = PyramidPlunder.global()
            val cloneDoors = ArrayList<Position>(state.allDoors.toList())
            cloneDoors.remove(state.currentDoor)
            if (cloneDoors.isNotEmpty()) {
                val random = Misc.random(cloneDoors.size - 1)
                state.currentDoor = cloneDoors[random]
            }
            hourTick = 4
        }
        PyramidPlunder.resetGlobalCycleState()
    }
}
