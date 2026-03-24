package net.dodian.uber.game.skills.thieving.plunder

import net.dodian.uber.game.model.Position

data class PyramidPlunderGlobalState(
    val allDoors: Array<Position> = arrayOf(Position(3288, 2799), Position(3293, 2794), Position(3288, 2789), Position(3283, 2794)),
    val nextRoom: IntArray = IntArray(7),
    val start: Position = Position(1927, 4477, 0),
    val end: Position = Position(3289, 2801, 0),
    var currentDoor: Position? = null,
) {
    val roomEntrances: Array<Position> = arrayOf(
        Position(1954, 4477, 0),
        Position(1977, 4471, 0),
        Position(1927, 4453, 0),
        Position(1965, 4444, 0),
        Position(1927, 4424, 0),
        Position(1943, 4421, 0),
        Position(1974, 4420, 0),
    )
}

data class PyramidPlunderPlayerState(
    var ticksRemaining: Int = -1,
    var roomNumber: Int = 0,
    var looting: Boolean = false,
    val obstacles: IntArray = intArrayOf(
        26616, 0, 26626, 0, 26618, 0, 26619, 0, 26620, 0, 26621, 0,
        26580, 0, 26600, 0, 26601, 0, 26602, 0, 26603, 0, 26604, 0,
        26605, 0, 26606, 0, 26607, 0, 26608, 0, 26609, 0, 26610, 0,
        26611, 0, 26612, 0, 26613, 0,
    ),
) {
    val urnConfig: IntArray = intArrayOf(0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28)
    val tombConfig: IntArray = intArrayOf(2, 0, 9, 10, 11, 12)
}
