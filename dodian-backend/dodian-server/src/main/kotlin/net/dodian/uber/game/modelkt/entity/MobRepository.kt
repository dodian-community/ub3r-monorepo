package net.dodian.uber.game.modelkt.entity

@Suppress("MemberVisibilityCanBePrivate")
class MobRepository<T : Mob>(
    private val mobs: MutableList<T> = mutableListOf()
) : MutableList<T> by mobs {

    override fun add(element: T): Boolean {
        if (!mobs.add(element))
            return false

        element.updateIndex(mobs.indexOf(element))
        return true
    }

    override fun remove(element: T): Boolean {
        if (!mobs.remove(element))
            return false

        element.updateIndex(-1)
        return true
    }

    /*
    private var repoSize = 0
    val capacity get() = mobs.size
    val isFull get() = repoSize == capacity

    override fun add(element: T): Boolean {
        if (isFull) return false

        mobs.forEachIndexed { index, mob ->
            if (mobs.size >= index) return@forEachIndexed

            mobs.add(index, mob)
            mob.updateIndex(index + 1)
            repoSize++

            return true
        }

        return false
    }

    override fun remove(element: T) = remove(element.index)

    fun remove(index: Int): Boolean {
        val mob = mobs[index]
        if (mob.index != index)
            error("MobRepository index mismatch, cannot remove Mob!")

        mobs.removeAt(index - 1)
        mob.updateIndex(-1)
        repoSize--

        return true
    }*/
}