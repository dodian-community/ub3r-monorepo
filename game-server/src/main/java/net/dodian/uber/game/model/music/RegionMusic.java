package net.dodian.uber.game.model.music;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.network.packets.outgoing.SetRegionSong;
import net.dodian.uber.game.network.packets.outgoing.SongSetting;

/**
 * @author Dashboard
 */
public class RegionMusic {

    public static void handleRegionMusic(Client client) {
        int songId = RegionSong.getRegionSong(client.getPosition()).getSongId();
        client.send(new SetRegionSong(songId));
        if (!client.isSongUnlocked(songId)) {
            client.send(new SongSetting(songId, true, true));
            client.setSongUnlocked(songId, true);
            if (client.areAllSongsUnlocked()) {
                Client.publicyell(client.getPlayerName() + " has finished unlocking every music track!");
            }
        }
    }

    public static void sendSongSettings(Client client) {
        for (RegionSong song : RegionSong.values()) {
            client.send(new SongSetting(song.getSongId(), client.isSongUnlocked(song.getSongId()), false));
        }
    }

}
