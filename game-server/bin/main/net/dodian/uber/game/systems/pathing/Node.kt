package net.dodian.uber.game.systems.pathing

data class Node(
    val x: Int,
    val y: Int,
    val z: Int,
    val g: Int = 0,
    val h: Int = 0,
    val parent: Node? = null,
) {
    val f: Int
        get() = g + h
}
