package net.dodian.uber.game.persistence;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public final class WorldPollResult {

    public static final WorldPollResult EMPTY = new WorldPollResult(null, Set.of(), Map.of(), Set.of());

    private final Integer latestNewsId;
    private final Set<Integer> playersWithRefunds;
    private final Map<Integer, Long> muteTimes;
    private final Set<Integer> bannedPlayerIds;

    public WorldPollResult(Integer latestNewsId,
                           Set<Integer> playersWithRefunds,
                           Map<Integer, Long> muteTimes,
                           Set<Integer> bannedPlayerIds) {
        this.latestNewsId = latestNewsId;
        this.playersWithRefunds = playersWithRefunds == null ? Set.of() : Set.copyOf(playersWithRefunds);
        this.muteTimes = muteTimes == null ? Map.of() : Map.copyOf(muteTimes);
        this.bannedPlayerIds = bannedPlayerIds == null ? Set.of() : Set.copyOf(bannedPlayerIds);
    }

    public Integer getLatestNewsId() {
        return latestNewsId;
    }

    public Set<Integer> getPlayersWithRefunds() {
        return Collections.unmodifiableSet(playersWithRefunds);
    }

    public Map<Integer, Long> getMuteTimes() {
        return Collections.unmodifiableMap(muteTimes);
    }

    public Set<Integer> getBannedPlayerIds() {
        return Collections.unmodifiableSet(bannedPlayerIds);
    }
}
