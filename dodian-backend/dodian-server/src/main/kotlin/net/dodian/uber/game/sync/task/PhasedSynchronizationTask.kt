package net.dodian.uber.game.sync.task

import java.util.concurrent.Phaser

class PhasedSynchronizationTask(
    private val phaser: Phaser,
    private val task: SynchronizationTask
) : SynchronizationTask() {

    override fun run() {
        try {
            task.run()
        } catch (exception: Exception) {
            exception.printStackTrace()
        } finally {
            phaser.arriveAndDeregister()
        }
    }
}