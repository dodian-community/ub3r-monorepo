package net.dodian.uber.game.model.music

import java.awt.Rectangle
import net.dodian.uber.game.model.Position

enum class RegionSong(
    private val songId: Int,
    private val area: Rectangle,
) {
    VICTORIOUS_DAYS(0, Rectangle(2700, 3340, 50, 50)),
    IN_REVERENCE(1, Rectangle(2691, 9727, 60, 60)),
    THE_TOWN_OF_WITCHWOODE(2, Rectangle(2870, 3391, 65, 116)),
    UNDER_THE_BARDS_TREE(3, Rectangle(2670, 3391, 120, 125)),
    IN_THE_CAVE(4, Rectangle(2624, 9783, 66, 137)),
    THE_HEROES_RETURN(5, Rectangle(2500, 3050, 200, 125)),
    PROUD_WARRIORS(6, Rectangle(2559, 9471, 73, 64)),
    MEDIEVAL_BANQUET(7, Rectangle(2559, 3259, 125, 77)),
    THE_LONG_JOURNEY_HOME(8, Rectangle(0, 0, 0, 0)),
    A_JETTY_ON_THE_LAKE(9, Rectangle(2791, 3412, 78, 120)),
    VALLEY_OF_THE_CLOUDS(10, Rectangle(2574, 3393, 51, 36)),
    LOTTYR_LADY_OF_THE_HELLS(11, Rectangle(3199, 9345, 128, 64)),
    THE_FAIRY_WOODS(12, Rectangle(2375, 3388, 130, 140)),
    TWELTH_WARRIOR(13, Rectangle(2816, 9920, 90, 60)),
    THE_CHAOS_WARRIOR(14, Rectangle(2461, 3264, 98, 73)),
    ;

    fun getSongId(): Int = songId

    fun getArea(): Rectangle = area

    fun isInArea(position: Position): Boolean =
        area.minX <= position.getX() &&
            position.getX() <= area.maxX &&
            area.minY <= position.getY() &&
            position.getY() <= area.maxY

    companion object {
        @JvmStatic
        fun getRegionSong(position: Position): RegionSong {
            for (song in values()) {
                if (song.isInArea(position)) {
                    return song
                }
            }
            return THE_LONG_JOURNEY_HOME
        }
    }
}
