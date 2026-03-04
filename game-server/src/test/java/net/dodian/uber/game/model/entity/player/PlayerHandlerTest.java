package net.dodian.uber.game.model.entity.player;

import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class PlayerHandlerTest {

    @AfterEach
    void tearDown() {
        PlayerHandler.playersOnline.clear();
    }

    @Test
    void snapshotActivePlayersFiltersDisconnectedInactiveAndClosedChannels() {
        Client active = new Client(new EmbeddedChannel(), 1);
        active.isActive = true;
        active.disconnected = false;
        active.dbId = 101;

        Client disconnected = new Client(new EmbeddedChannel(), 2);
        disconnected.isActive = true;
        disconnected.disconnected = true;
        disconnected.dbId = 102;

        Client inactive = new Client(new EmbeddedChannel(), 3);
        inactive.isActive = false;
        inactive.disconnected = false;
        inactive.dbId = 103;

        EmbeddedChannel closedChannel = new EmbeddedChannel();
        closedChannel.close();
        Client closed = new Client(closedChannel, 4);
        closed.isActive = true;
        closed.disconnected = false;
        closed.dbId = 104;

        PlayerHandler.playersOnline.put(1L, active);
        PlayerHandler.playersOnline.put(2L, disconnected);
        PlayerHandler.playersOnline.put(3L, inactive);
        PlayerHandler.playersOnline.put(4L, closed);

        List<Client> snapshot = PlayerHandler.snapshotActivePlayers();

        assertEquals(1, snapshot.size());
        assertSame(active, snapshot.get(0));
    }
}
