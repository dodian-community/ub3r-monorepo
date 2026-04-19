package net.dodian.uber.game.netty.listener.in;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.engine.systems.follow.FollowService;
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry;

/**
 * Player-click routing for the legacy follow packet.
 *
 * The packet listener decodes the target slot and delegates here so the
 * gameplay-side follow state can be updated without touching the listener
 * layer.
 */
public final class PlayerClickListener {

    private PlayerClickListener() {
    }

    public static void handleFollowPlayer(Client client, int followId) {
        if (client == null) {
            return;
        }
        Client target = PlayerRegistry.getClient(followId);
        if (target == null || client.getSlot() == followId) {
            FollowService.cancelFollow(client);
            return;
        }

        FollowService.requestFollow(client, target);
    }
}
