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
