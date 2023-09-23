package net.dodian.uber.game.modelkt.entity

class MobRepository<T : Mob>(
    private val list: MutableList<T> = mutableListOf()
) : MutableList<T> by list