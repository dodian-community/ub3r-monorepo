package net.dodian.uber.game.runtime.task.suspension

abstract class TaskCondition {
    abstract fun resume(): Boolean
}

class WaitCondition(cycles: Int) : TaskCondition() {
    private var cyclesLeft = cycles

    override fun resume(): Boolean {
        cyclesLeft--
        return cyclesLeft <= 0
    }
}

class PredicateCondition(
    private val predicate: () -> Boolean,
) : TaskCondition() {
    override fun resume(): Boolean = predicate()
}
