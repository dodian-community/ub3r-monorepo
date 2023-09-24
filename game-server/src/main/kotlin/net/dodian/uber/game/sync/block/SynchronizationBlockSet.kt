package net.dodian.uber.game.sync.block

import kotlin.reflect.KClass

class SynchronizationBlockSet(
    private val blocks: MutableMap<KClass<out SynchronizationBlock>, SynchronizationBlock> = HashMap(8)
) : Cloneable {

    val size: Int get() = blocks.size

    fun add(block: SynchronizationBlock) {
        blocks[block::class] = block
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : SynchronizationBlock> remove(clazz: KClass<out SynchronizationBlock>) = blocks.remove(clazz) as T?

    @Suppress("UNCHECKED_CAST")
    operator fun <T : SynchronizationBlock> get(clazz: KClass<out SynchronizationBlock>) = blocks[clazz] as T?

    fun contains(clazz: KClass<out SynchronizationBlock>) = blocks.containsKey(clazz)

    fun clear() {
        blocks.clear()
    }
}