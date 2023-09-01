package net.dodian.uber.game.process

interface GameProcess {

    fun startUp()

    fun shutDown()

    fun cycle()
}
