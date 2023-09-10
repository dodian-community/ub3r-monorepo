package net.dodian.uber.game.sync.block

class SynchronizationBlockSet(
    val blocks: MutableMap<Class<out SynchronizationBlock>, SynchronizationBlock> = HashMap(8)
) : Cloneable {

    fun add(block: SynchronizationBlock) {
        val clazz = block.javaClass
        blocks[clazz] = block
    }

    fun clear() {
        blocks.clear()
    }

    fun contains(clazz: Class<out SynchronizationBlock>) = blocks.containsKey(clazz)

    operator fun get(clazz: Class<out SynchronizationBlock>) = blocks[clazz]

    fun remove(clazz: Class<out SynchronizationBlock>) {
        blocks.remove(clazz)
    }

    val size get() = blocks.size
}