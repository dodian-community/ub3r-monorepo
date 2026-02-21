package net.dodian.uber.game.persistence;

import java.util.Collections;
import java.util.List;

public final class WorldPollInput {

    private final int worldId;
    private final int playerCount;
    private final List<Integer> onlinePlayerDbIds;

    public WorldPollInput(int worldId, int playerCount, List<Integer> onlinePlayerDbIds) {
        this.worldId = worldId;
        this.playerCount = playerCount;
        this.onlinePlayerDbIds = onlinePlayerDbIds == null ? List.of() : List.copyOf(onlinePlayerDbIds);
    }

    public int getWorldId() {
        return worldId;
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public List<Integer> getOnlinePlayerDbIds() {
        return Collections.unmodifiableList(onlinePlayerDbIds);
    }
}
