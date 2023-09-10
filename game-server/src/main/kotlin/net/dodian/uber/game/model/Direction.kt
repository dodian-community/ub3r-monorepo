package net.dodian.uber.game.model

enum class Direction(val intValue: Int) {
    NONE(-1),
    NORTH_WEST(0),
	NORTH(1),
	NORTH_EAST(2),
	WEST(3),
	EAST(4),
	SOUTH_WEST(5),
	SOUTH(6),
	SOUTH_EAST(7);
}