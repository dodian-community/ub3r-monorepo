package net.dodian.uber.game.model.music;

import net.dodian.uber.game.model.Position;

import java.awt.*;

public enum RegionSong {

    VICTORIOUS_DAYS(0, new Rectangle(2700, 3340, 50, 50)), IN_REVERENCE(1, new Rectangle(2691, 9727, 60,
            60)), THE_TOWN_OF_WITCHWOODE(2, new Rectangle(2870, 3391, 65, 116)), UNDER_THE_BARDS_TREE(3,
            new Rectangle(2670, 3391, 120, 125)), IN_THE_CAVE(4, new Rectangle(2624, 9783, 66, 137)), THE_HEROES_RETURN(5,
            new Rectangle(2500, 3050, 200, 125)), PROUD_WARRIORS(6,
            new Rectangle(2559, 9471, 73, 64)), MEDIEVAL_BANQUET(7,
            new Rectangle(2559, 3259, 125, 77)), THE_LONG_JOURNEY_HOME(8,
            new Rectangle(0, 0, 0, 0)), A_JETTY_ON_THE_LAKE(9,
            new Rectangle(2791, 3412, 78, 120)), VALLEY_OF_THE_CLOUDS(10,
            new Rectangle(2574, 3393, 51, 36)), LOTTYR_LADY_OF_THE_HELLS(11,
            new Rectangle(3199, 9345, 128, 64)), THE_FAIRY_WOODS(12,
            new Rectangle(2375, 3388, 130, 140)), TWELTH_WARRIOR(13,
            new Rectangle(2816, 9920, 90, 60)), THE_CHAOS_WARRIOR(14,
            new Rectangle(2461, 3264, 98, 73));

    private int songId;
    private Rectangle area;

    RegionSong(int songId, Rectangle area) {
        this.songId = songId;
        this.area = area;
    }

    public int getSongId() {
        return this.songId;
    }

    public Rectangle getArea() {
        return this.area;
    }

    public boolean isInArea(Position position) {
        return area.getMinX() <= position.getX() && position.getX() <= area.getMaxX() && area.getMinY() <= position.getY()
                && position.getY() <= area.getMaxY();
    }

    public static RegionSong getRegionSong(Position position) {
        for (RegionSong song : values()) {
            if (song.isInArea(position))
                return song;
        }
        return THE_LONG_JOURNEY_HOME;
    }

}
