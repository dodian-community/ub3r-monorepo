package net.dodian.uber.game.model.chunk

data class Chunk(
    val x: Int,
    val y: Int,
) {
    fun getAbsX(): Int = SIZE * (x + 6)

    fun getAbsY(): Int = SIZE * (y + 6)

    fun translate(dx: Int, dy: Int): Chunk = Chunk(x + dx, y + dy)

    companion object {
        const val SIZE: Int = 8
    }
}
