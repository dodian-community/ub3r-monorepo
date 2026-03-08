package net.dodian.uber.game.model.entity.player;


import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.UpdateFlag;
import net.dodian.uber.game.model.chunk.ChunkPlayerComparator;
import net.dodian.uber.game.model.entity.Entity;
import net.dodian.uber.game.model.entity.EntityUpdating;
import net.dodian.uber.game.model.item.Equipment;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;
import net.dodian.uber.game.netty.codec.MessageType;
import net.dodian.uber.game.runtime.sync.SynchronizationContext;
import net.dodian.uber.game.runtime.sync.player.PlayerSyncDecision;
import net.dodian.uber.game.runtime.sync.player.ViewerPlayerSyncState;
import net.dodian.uber.game.runtime.sync.playerinfo.PlayerVisibilityRules;
import net.dodian.uber.game.runtime.sync.playerinfo.dispatch.RootPlayerInfoPlan;
import net.dodian.uber.game.runtime.sync.scratch.ThreadLocalSyncScratch;
import net.dodian.uber.game.runtime.sync.template.PlayerSyncTemplate;
import net.dodian.uber.game.runtime.sync.template.PlayerSyncTemplateKey;
import net.dodian.uber.game.runtime.sync.viewport.ViewportSnapshot;
import net.dodian.utilities.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.dodian.utilities.DotEnvKt.getSyncAppearanceCacheEnabled;
import static net.dodian.utilities.DotEnvKt.getSyncScratchBufferReuseEnabled;

/**
 * @author blakeman8192
 * @author lare96 <<a href="http://github.com/lare96">...</a>>
 * @author Dashboard
 */
public class PlayerUpdating extends EntityUpdating<Player> {

    private static final Logger logger = LoggerFactory.getLogger(PlayerUpdating.class);
    private static final boolean DEBUG_REGION_UPDATES = false;
    private static final boolean DEBUG_ADDED_LOCAL_PLAYERS = false;
    private static final int MAX_LOCAL_PLAYER_ADDS_PER_TICK = 15;
    private static final int MAX_LOCAL_PLAYER_CAP = 255;

    private static final PlayerUpdating instance = new PlayerUpdating();
    private static final java.util.concurrent.atomic.AtomicInteger DEBUG_ADDED_LOCAL_COUNTER = new java.util.concurrent.atomic.AtomicInteger();
    private static final PlayerUpdateBlockSet BLOCK_SET = new PlayerUpdateBlockSet();

    enum UpdatePhase {
        UPDATE_SELF,
        UPDATE_LOCAL,
        ADD_LOCAL
    }

    public static PlayerUpdating getInstance() {
        return instance;
    }

    @Override
    public void update(Player player, ByteMessage stream) {
        ByteMessage updateBlock = withScratchUpdateBlock();
        try {
            sendServerUpdateIfNeeded(player);

            // Ensure the player is registered in the chunk index before discovery.
            player.syncChunkMembership();

            boolean localPlayerUpdateRequired = hasUpdatesForPhase(player, UpdatePhase.UPDATE_SELF);
            updateLocalPlayerMovement(player, stream, localPlayerUpdateRequired);

            // Handle teleportation - clear player list but continue to local player discovery
            if (player.didTeleport()) {
                // Clear existing player list when teleporting (similar to Hyperion's approach)
                if (player.playerListSize > 0 || !player.playersUpdating.isEmpty()) {
                    player.bumpLocalPlayerMembershipRevision();
                }
                java.util.Arrays.fill(player.playerList, 0, player.playerListSize, null);
                player.playerListSize = 0;
                player.playersUpdating.clear();
                // Don't return early - allow local player discovery to happen
            }

            appendBlockUpdate(player, updateBlock, UpdatePhase.UPDATE_SELF);
            if (player.loaded) {
                pruneLocalsToProtocolCap(player);
                SynchronizationContext.recordPlayerPacketBuilt(player.playerListSize);
                stream.putBits(8, player.playerListSize);
                int size = player.playerListSize;
                int keep = 0;
                boolean localsChanged = false;
                for (int i = 0; i < size; i++) {
                    Player local = player.playerList[i];
                    if (local != null && player.loaded && !local.didTeleport() && !player.didTeleport()
                            && player.withinDistance(local)) {
                        local.updatePlayerMovement(stream);
                        appendBlockUpdate(local, updateBlock, UpdatePhase.UPDATE_LOCAL);
                        player.playerList[keep++] = local;
                    } else {
                        if (local != null) {
                            player.playersUpdating.remove(local);
                            localsChanged = true;
                        }
                        stream.putBits(1, 1);
                        stream.putBits(2, 3);
                    }
                }
                java.util.Arrays.fill(player.playerList, keep, size, null);
                if (keep != size || localsChanged) {
                    player.bumpLocalPlayerMembershipRevision();
                }
                player.playerListSize = keep;

                addLocalPlayers(player, stream, updateBlock);
            } else {
                stream.putBits(8, 0);
            }

            // Always write the magic termination value (2047) - required by protocol
            stream.putBits(11, 2047);
            stream.endBitAccess();

            if (updateBlock.getBuffer().writerIndex() > 0) {
                stream.putBytes(updateBlock);
            }
            // Note: endFrameVarSizeWord equivalent is handled by the outer packet wrapper

            if (DEBUG_REGION_UPDATES && logger.isTraceEnabled()) {
                int rx = player.getPosition().getX() >> 6;
                int ry = player.getPosition().getY() >> 6;
                logger.trace("[RegionUpdate] {} region({},{}) locals={}", player.getPlayerName(), rx, ry, player.playerListSize);
            }
        } finally {
            releaseScratch(updateBlock);
        }
    }

    public void writeSelfOnlyUpdate(Player viewer, ByteMessage stream, RootPlayerInfoPlan plan) {
        ByteMessage updateBlock = withScratchUpdateBlock();
        try {
            sendServerUpdateIfNeeded(viewer);
            boolean localPlayerUpdateRequired = hasUpdatesForPhase(viewer, UpdatePhase.UPDATE_SELF);
            updateLocalPlayerMovement(viewer, stream, localPlayerUpdateRequired);
            appendBlockUpdate(viewer, updateBlock, UpdatePhase.UPDATE_SELF);

            if (viewer.loaded) {
                stream.putBits(8, viewer.playerListSize);
                for (int i = 0; i < viewer.playerListSize; i++) {
                    stream.putBits(1, 0);
                }
            } else {
                stream.putBits(8, 0);
            }

            finishPlayerSync(stream, updateBlock);
        } finally {
            releaseScratch(updateBlock);
        }
    }

    public void writeIncrementalSteadyUpdate(Player viewer, ByteMessage stream, RootPlayerInfoPlan plan) {
        writeIncrementalUpdate(viewer, stream, plan, false);
    }

    public void writeIncrementalAdmissionUpdate(Player viewer, ByteMessage stream, RootPlayerInfoPlan plan) {
        writeIncrementalUpdate(viewer, stream, plan, true);
    }

    private void writeIncrementalUpdate(Player viewer, ByteMessage stream, RootPlayerInfoPlan plan, boolean includeAdditions) {
        ByteMessage updateBlock = withScratchUpdateBlock();
        try {
            sendServerUpdateIfNeeded(viewer);
            viewer.syncChunkMembership();
            boolean localPlayerUpdateRequired = hasUpdatesForPhase(viewer, UpdatePhase.UPDATE_SELF);
            updateLocalPlayerMovement(viewer, stream, localPlayerUpdateRequired);
            appendBlockUpdate(viewer, updateBlock, UpdatePhase.UPDATE_SELF);

            if (viewer.loaded) {
                pruneLocalsToProtocolCap(viewer);
                stream.putBits(8, viewer.playerListSize);
                java.util.BitSet removals = toBitSet(plan.getDiff().getRemovals(), plan.getDiff().getRemovalsCount());
                java.util.BitSet changedRetained = toBitSet(plan.getDiff().getChangedRetained(), plan.getDiff().getChangedRetainedCount());
                int originalSize = viewer.playerListSize;
                int keep = 0;
                boolean localsChanged = false;
                for (int i = 0; i < originalSize; i++) {
                    Player local = viewer.playerList[i];
                    if (local == null || removals.get(local.getSlot())) {
                        if (local != null) {
                            viewer.playersUpdating.remove(local);
                            localsChanged = true;
                        }
                        stream.putBits(1, 1);
                        stream.putBits(2, 3);
                        continue;
                    }

                    writeRetainedLocalUpdate(local, stream, updateBlock, changedRetained.get(local.getSlot()));
                    viewer.playerList[keep++] = local;
                }
                java.util.Arrays.fill(viewer.playerList, keep, originalSize, null);
                if (keep != originalSize || localsChanged) {
                    viewer.bumpLocalPlayerMembershipRevision();
                }
                viewer.playerListSize = keep;
                if (includeAdditions) {
                    writeLocalAdditions(viewer, stream, updateBlock, plan);
                }
            } else {
                stream.putBits(8, 0);
            }

            finishPlayerSync(stream, updateBlock);
        } finally {
            releaseScratch(updateBlock);
        }
    }

    public void writeFullRebuild(Player viewer, ByteMessage stream, RootPlayerInfoPlan plan) {
        update(viewer, stream);
    }

    private void addLocalPlayers(Player player, ByteMessage stream, ByteMessage updateBlock) {
        int remainingAdds = Math.min(MAX_LOCAL_PLAYER_ADDS_PER_TICK, MAX_LOCAL_PLAYER_CAP - player.playerListSize);
        if (remainingAdds <= 0) {
            return;
        }

        ViewportSnapshot snapshot = SynchronizationContext.getViewportSnapshot(player);
        if (snapshot != null) {
            java.util.Collection<Player> candidates = snapshot.getPlayers();
            if (candidates.isEmpty()) {
                return;
            }
            if (player.playerListSize > 50) {
                addPrioritizedLocalPlayersFromCollection(player, stream, updateBlock, candidates, remainingAdds);
                return;
            }
            addLocalPlayersFromCollection(player, stream, updateBlock, candidates, remainingAdds);
            return;
        }

        if (Server.chunkManager == null) {
            java.util.List<Player> candidates = PlayerHandler.getLocalPlayers(player);
            if (candidates.isEmpty()) {
                return;
            }
            if (player.playerListSize > 50) {
                candidates.sort(new ChunkPlayerComparator(player));
            }
            addLocalPlayersFromCollection(player, stream, updateBlock, candidates, remainingAdds);
            return;
        }

        if (player.playerListSize > 50) {
            addPrioritizedChunkLocalPlayers(player, stream, updateBlock, remainingAdds);
            return;
        }

        final int[] playersAdded = {0};
        Server.chunkManager.forEachUpdatePlayerCandidate(player, 16, other -> {
            if (playersAdded[0] >= remainingAdds || !shouldAddLocalPlayerCandidate(player, other)) {
                return;
            }
            player.addNewPlayer(other, stream, updateBlock);
            if (!player.playersUpdating.contains(other)) {
                return;
            }
            playersAdded[0]++;
            SynchronizationContext.recordPlayerAdd();
            if (DEBUG_ADDED_LOCAL_PLAYERS) {
                DEBUG_ADDED_LOCAL_COUNTER.incrementAndGet();
            }
        });
    }

    private void addLocalPlayersFromCollection(Player player,
                                               ByteMessage stream,
                                               ByteMessage updateBlock,
                                               java.util.Collection<Player> candidates,
                                               int remainingAdds) {
        int playersAdded = 0;
        for (Player other : candidates) {
            if (!shouldAddLocalPlayerCandidate(player, other)) {
                continue;
            }

            player.addNewPlayer(other, stream, updateBlock);
            if (!player.playersUpdating.contains(other)) {
                continue;
            }
            playersAdded++;
            SynchronizationContext.recordPlayerAdd();
            if (DEBUG_ADDED_LOCAL_PLAYERS) {
                DEBUG_ADDED_LOCAL_COUNTER.incrementAndGet();
            }

            if (playersAdded >= remainingAdds || player.playerListSize >= MAX_LOCAL_PLAYER_CAP) {
                break;
            }
        }
    }

    private void addPrioritizedChunkLocalPlayers(Player player,
                                                 ByteMessage stream,
                                                 ByteMessage updateBlock,
                                                 int remainingAdds) {
        Player[] prioritized = new Player[remainingAdds];
        int[] prioritizedDistances = new int[remainingAdds];
        int[] prioritizedCount = {0};

        Server.chunkManager.forEachUpdatePlayerCandidate(player, 16, other -> {
            if (!shouldAddLocalPlayerCandidate(player, other)) {
                return;
            }
            insertPrioritizedCandidate(player, prioritized, prioritizedDistances, prioritizedCount, other);
        });

        for (int i = 0; i < prioritizedCount[0]; i++) {
            Player other = prioritized[i];
            if (!shouldAddLocalPlayerCandidate(player, other)) {
                continue;
            }
            player.addNewPlayer(other, stream, updateBlock);
            if (player.playersUpdating.contains(other)) {
                SynchronizationContext.recordPlayerAdd();
                if (DEBUG_ADDED_LOCAL_PLAYERS) {
                    DEBUG_ADDED_LOCAL_COUNTER.incrementAndGet();
                }
            }
        }
    }

    private void addPrioritizedLocalPlayersFromCollection(Player player,
                                                          ByteMessage stream,
                                                          ByteMessage updateBlock,
                                                          java.util.Collection<Player> candidates,
                                                          int remainingAdds) {
        Player[] prioritized = new Player[remainingAdds];
        int[] prioritizedDistances = new int[remainingAdds];
        int[] prioritizedCount = {0};
        for (Player other : candidates) {
            if (!shouldAddLocalPlayerCandidate(player, other)) {
                continue;
            }
            insertPrioritizedCandidate(player, prioritized, prioritizedDistances, prioritizedCount, other);
        }

        for (int i = 0; i < prioritizedCount[0]; i++) {
            Player other = prioritized[i];
            if (!shouldAddLocalPlayerCandidate(player, other)) {
                continue;
            }
            player.addNewPlayer(other, stream, updateBlock);
            if (player.playersUpdating.contains(other)) {
                SynchronizationContext.recordPlayerAdd();
                if (DEBUG_ADDED_LOCAL_PLAYERS) {
                    DEBUG_ADDED_LOCAL_COUNTER.incrementAndGet();
                }
            }
        }
    }

    private void insertPrioritizedCandidate(Player viewer,
                                            Player[] prioritized,
                                            int[] prioritizedDistances,
                                            int[] prioritizedCount,
                                            Player candidate) {
        int distance = computeLongestDistance(viewer, candidate);
        int count = prioritizedCount[0];
        int limit = prioritized.length;

        if (count == limit) {
            Player worst = prioritized[count - 1];
            if (compareCandidate(candidate, distance, worst, prioritizedDistances[count - 1]) >= 0) {
                return;
            }
        } else {
            prioritizedCount[0] = count + 1;
        }

        int insert = Math.min(count, limit - 1);
        while (insert > 0 && compareCandidate(candidate, distance, prioritized[insert - 1], prioritizedDistances[insert - 1]) < 0) {
            prioritized[insert] = prioritized[insert - 1];
            prioritizedDistances[insert] = prioritizedDistances[insert - 1];
            insert--;
        }

        prioritized[insert] = candidate;
        prioritizedDistances[insert] = distance;
    }

    private int compareCandidate(Player left, int leftDistance, Player right, int rightDistance) {
        if (right == null) {
            return -1;
        }
        int distanceCompare = Integer.compare(leftDistance, rightDistance);
        if (distanceCompare != 0) {
            return distanceCompare;
        }
        return Integer.compare(left.getSlot(), right.getSlot());
    }

    private int computeLongestDistance(Player viewer, Player other) {
        int dx = Math.abs(viewer.getPosition().getX() - other.getPosition().getX());
        int dy = Math.abs(viewer.getPosition().getY() - other.getPosition().getY());
        return Math.max(dx, dy);
    }

    private boolean shouldAddLocalPlayerCandidate(Player player, Player other) {
        return PlayerVisibilityRules.canAddLocal(player, other);
    }

    private void pruneLocalsToProtocolCap(Player player) {
        if (player.playerListSize <= MAX_LOCAL_PLAYER_CAP) {
            return;
        }
        int originalSize = player.playerListSize;
        int keep = 0;
        for (int i = 0; i < originalSize && keep < MAX_LOCAL_PLAYER_CAP; i++) {
            Player local = player.playerList[i];
            if (local == null) {
                continue;
            }
            player.playerList[keep++] = local;
        }
        for (int i = keep; i < originalSize; i++) {
            Player local = player.playerList[i];
            if (local != null) {
                player.playersUpdating.remove(local);
                player.playerList[i] = null;
            }
        }
        if (keep != originalSize) {
            player.bumpLocalPlayerMembershipRevision();
        }
        player.playerListSize = keep;
    }


    public void updateLocalPlayerMovement(Player player, ByteMessage stream, boolean localPlayerUpdateRequired) {
        /* Noob! */
        if(player.didMapRegionChange()) {
            // Send map region change as separate packet (73)
            ((Client) player).send(new net.dodian.uber.game.netty.listener.out.MapRegionUpdate(player.mapRegionX, player.mapRegionY));
            ((Client) player).updateGroundItems();
        }
        // This should match the original: createFrameVarSizeWord(81) + initBitAccess()
        // But we're doing this in the packet wrapper instead
        stream.startBitAccess();
        if (player.didTeleport()) {
            stream.putBits(1, 1);
            stream.putBits(2, 3); // updateType
            stream.putBits(2, player.getPosition().getZ());
            stream.putBits(1, player.didTeleport() ? 1 : 0);
            stream.putBits(1, localPlayerUpdateRequired ? 1 : 0);
            stream.putBits(7, player.getCurrentY());
            stream.putBits(7, player.getCurrentX());
            return;
        }
        if (player.getPrimaryDirection() == -1) {
            if (localPlayerUpdateRequired) {
                stream.putBits(1, 1);
                stream.putBits(2, 0);
            } else {
                stream.putBits(1, 0);
            }
        } else
        if (player.getSecondaryDirection() == -1) {
            int primaryDirection = translateDirectionToClient(player.getPrimaryDirection(), player.getPlayerName(), "self-primary");
            if (primaryDirection == -1) {
                stream.putBits(1, localPlayerUpdateRequired ? 1 : 0);
                if (localPlayerUpdateRequired) {
                    stream.putBits(2, 0);
                }
                return;
            }
            stream.putBits(1, 1);
            stream.putBits(2, 1);
            stream.putBits(3, primaryDirection);
            stream.putBits(1, localPlayerUpdateRequired ? 1 : 0);
        } else {
            int primaryDirection = translateDirectionToClient(player.getPrimaryDirection(), player.getPlayerName(), "self-primary");
            int secondaryDirection = translateDirectionToClient(player.getSecondaryDirection(), player.getPlayerName(), "self-secondary");
            if (primaryDirection == -1) {
                stream.putBits(1, localPlayerUpdateRequired ? 1 : 0);
                if (localPlayerUpdateRequired) {
                    stream.putBits(2, 0);
                }
                return;
            }
            stream.putBits(1, 1);
            stream.putBits(2, secondaryDirection == -1 ? 1 : 2);
            stream.putBits(3, primaryDirection);
            if (secondaryDirection != -1) {
                stream.putBits(3, secondaryDirection);
            }
            stream.putBits(1, localPlayerUpdateRequired ? 1 : 0);
        }
    }

    private static int translateDirectionToClient(int direction, String playerName, String phase) {
        if (direction < 0 || direction >= Utils.xlateDirectionToClient.length) {
            logger.warn("Invalid player direction {} for {} during {}", direction, playerName, phase);
            return -1;
        }
        return Utils.xlateDirectionToClient[direction];
    }

    public void writeLocalRemovals(Player viewer, ByteMessage stream, java.util.BitSet removals) {
        int originalSize = viewer.playerListSize;
        int keep = 0;
        boolean localsChanged = false;
        for (int i = 0; i < originalSize; i++) {
            Player local = viewer.playerList[i];
            if (local == null || removals.get(local.getSlot())) {
                if (local != null) {
                    viewer.playersUpdating.remove(local);
                    localsChanged = true;
                }
                stream.putBits(1, 1);
                stream.putBits(2, 3);
                continue;
            }
            viewer.playerList[keep++] = local;
        }
        java.util.Arrays.fill(viewer.playerList, keep, originalSize, null);
        if (keep != originalSize || localsChanged) {
            viewer.bumpLocalPlayerMembershipRevision();
        }
        viewer.playerListSize = keep;
    }

    public void writeLocalAdditions(Player viewer, ByteMessage stream, ByteMessage updateBlock, RootPlayerInfoPlan plan) {
        int[] additions = plan.getActualAdditions();
        // Root player sync queues teleport reinserts ahead of ordinary admissions so
        // retained teleports are encoded as remove+readd in one packet when capacity allows.
        for (int slot : additions) {
            Player other = resolvePlayerSlot(slot);
            if (!shouldAddLocalPlayerCandidate(viewer, other)) {
                continue;
            }
            writeLocalAdd(viewer, other, stream, updateBlock);
            if (viewer.playersUpdating.contains(other)) {
                SynchronizationContext.recordPlayerAdd();
            }
        }
    }

    public void writeRetainedLocalUpdate(Player local, ByteMessage stream, ByteMessage updateBlock, boolean changed) {
        if (changed) {
            local.updatePlayerMovement(stream);
            appendBlockUpdate(local, updateBlock, UpdatePhase.UPDATE_LOCAL);
        } else {
            stream.putBits(1, 0);
        }
    }

    public void writeLocalRemoval(ByteMessage stream) {
        stream.putBits(1, 1);
        stream.putBits(2, 3);
    }

    public void writeLocalAdd(Player viewer, Player other, ByteMessage stream, ByteMessage updateBlock) {
        viewer.addNewPlayer(other, stream, updateBlock);
    }


    @Override
    public void appendBlockUpdate(Player player, ByteMessage buf) {
        appendBlockUpdate(player, buf, UpdatePhase.UPDATE_LOCAL);
    }

    void appendBlockUpdate(Player player, ByteMessage buf, UpdatePhase phase) {
        BLOCK_SET.encode(this, player, buf, phase);
    }

    public void appendAddLocalBlockUpdate(Player player, ByteMessage buf) {
        appendBlockUpdate(player, buf, UpdatePhase.ADD_LOCAL);
    }

    public PlayerSyncDecision shouldSkipPlayerSync(Player viewer) {
        ViewerPlayerSyncState viewerState = SynchronizationContext.getViewerPlayerSyncState(viewer);
        ViewportSnapshot viewportSnapshot = SynchronizationContext.getViewportSnapshot(viewer);
        if (viewerState == null || viewportSnapshot == null || !viewer.loaded) {
            return PlayerSyncDecision.BUILD;
        }

        long selfMovementRevision = SynchronizationContext.getPlayerMovementRevision(viewer);
        long selfBlockRevision = SynchronizationContext.getPlayerBlockRevision(viewer);
        boolean selfStable =
                selfMovementRevision == viewerState.getLastSelfMovementRevision()
                        && selfBlockRevision == viewerState.getLastSelfBlockRevision();
        boolean regionStable =
                viewer.mapRegionX == viewerState.getLastKnownMapRegionX()
                        && viewer.mapRegionY == viewerState.getLastKnownMapRegionY()
                        && viewer.getPosition().getZ() == viewerState.getLastKnownPlane();
        long localMembershipRevision = viewer.getLocalPlayerMembershipRevision();
        boolean localActivityStable = localMembershipRevision == viewerState.getLastLocalMembershipRevision();
        boolean noImmediateStateChange =
                !viewer.didTeleport()
                        && !viewer.didMapRegionChange()
                        && viewer.getPrimaryDirection() == -1
                        && viewer.getSecondaryDirection() == -1
                        && !viewer.getUpdateFlags().isUpdateRequired();

        if (!selfStable || !regionStable || !localActivityStable || !noImmediateStateChange) {
            return PlayerSyncDecision.BUILD;
        }

        PlayerVisibilitySignature visibleSignature = buildVisibleSignature(viewer, viewportSnapshot);
        PlayerVisibilitySignature localSignature = buildLocalSignature(viewer);
        if (visibleSignature.matches(localSignature)) {
            return PlayerSyncDecision.SKIP;
        }
        return PlayerSyncDecision.BUILD;
    }

    private PlayerVisibilitySignature buildVisibleSignature(Player viewer, ViewportSnapshot snapshot) {
        int count = 0;
        int hash = 1;
        for (Player other : snapshot.getPlayers()) {
            if (!isVisiblePlayerCandidate(viewer, other)) {
                continue;
            }
            count++;
            hash = 31 * hash + other.getSlot();
        }
        return new PlayerVisibilitySignature(count, hash);
    }

    private PlayerVisibilitySignature buildLocalSignature(Player viewer) {
        int count = 0;
        int hash = 1;
        for (int i = 0; i < viewer.playerListSize; i++) {
            Player local = viewer.playerList[i];
            if (!isVisiblePlayerCandidate(viewer, local)) {
                continue;
            }
            count++;
            hash = 31 * hash + local.getSlot();
        }
        return new PlayerVisibilitySignature(count, hash);
    }

    private boolean isVisiblePlayerCandidate(Player viewer, Player other) {
        return PlayerVisibilityRules.isVisibleTo(viewer, other);
    }

    private record PlayerVisibilitySignature(int count, int hash) {

        private boolean matches(PlayerVisibilitySignature other) {
                return other != null && count == other.count && hash == other.hash;
            }
        }

    public PlayerSyncTemplateKey buildPlayerSyncTemplateKey(Player viewer) {
        int[] localSlots = new int[viewer.playerListSize];
        for (int i = 0; i < viewer.playerListSize; i++) {
            Player local = viewer.playerList[i];
            localSlots[i] = local == null ? -1 : local.getSlot();
        }
        return new PlayerSyncTemplateKey(
                localSlots,
                viewer.playerListSize,
                movementMode(viewer),
                hasUpdatesForPhase(viewer, UpdatePhase.UPDATE_SELF),
                viewer.didTeleport(),
                viewer.didMapRegionChange()
        );
    }

    public PlayerSyncTemplate buildPlayerSyncTemplate(Player viewer) {
        ByteMessage stream = withSharedBlock();
        try {
            writePlayerSyncTemplate(viewer, stream, null);
            return new PlayerSyncTemplate(stream.toByteArray());
        } finally {
            releaseScratch(stream);
        }
    }

    public void writePlayerSyncTemplate(Player viewer, ByteMessage stream, PlayerSyncTemplate template) {
        if (template != null) {
            stream.putBytes(template.getPayload());
            return;
        }

        stream.startBitAccess();
        stream.putBits(1, 0);
        stream.putBits(8, viewer.playerListSize);
        for (int i = 0; i < viewer.playerListSize; i++) {
            stream.putBits(1, 0);
        }
        stream.putBits(11, 2047);
        stream.endBitAccess();
    }

    public byte[] buildSharedBlock(Player player, String phaseName) {
        UpdatePhase phase = UpdatePhase.valueOf(phaseName);
        ByteMessage block = withSharedBlock();
        try {
            appendBlockUpdate(player, block, phase);
            return block.toByteArray();
        } finally {
            releaseScratch(block);
        }
    }

    private boolean hasUpdatesForPhase(Player player, UpdatePhase phase) {
        if (phase == UpdatePhase.ADD_LOCAL) {
            return true;
        }
        if (phase == UpdatePhase.UPDATE_SELF) {
            return player.getUpdateFlags().isRequired(UpdateFlag.FORCED_MOVEMENT)
                    || player.getUpdateFlags().isRequired(UpdateFlag.GRAPHICS)
                    || player.getUpdateFlags().isRequired(UpdateFlag.ANIM)
                    || player.getUpdateFlags().isRequired(UpdateFlag.FORCED_CHAT)
                    || player.getUpdateFlags().isRequired(UpdateFlag.FACE_CHARACTER)
                    || player.getUpdateFlags().isRequired(UpdateFlag.APPEARANCE)
                    || player.getUpdateFlags().isRequired(UpdateFlag.FACE_COORDINATE)
                    || player.getUpdateFlags().isRequired(UpdateFlag.HIT)
                    || player.getUpdateFlags().isRequired(UpdateFlag.HIT2);
        }
        return player.getUpdateFlags().isUpdateRequired();
    }

    public static void resetDebugAddedLocalCounter() {
        DEBUG_ADDED_LOCAL_COUNTER.set(0);
    }

    public static int consumeDebugAddedLocalCounter() {
        return DEBUG_ADDED_LOCAL_COUNTER.getAndSet(0);
    }

    public void appendGraphic(Player player, ByteMessage buf) {
        buf.putShort(player.getGraphicId(), ByteOrder.LITTLE); // writeWordBigEndian  
        buf.putInt(player.getGraphicHeight()); // writeDWord
    }

    @Override
    public void appendAnimationRequest(Player player, ByteMessage buf) {
        buf.putShort(player.getAnimationId(), ByteOrder.LITTLE); // writeWordBigEndian is actually little-endian!
        buf.put(player.getAnimationDelay(), ValueType.NEGATE); // writeByteC = -value, not 128-value
    }

    public static void appendForcedChatText(Player player, ByteMessage buf) {
        buf.putString(player.getForcedChat());
    }

    public static void appendPlayerChatText(Player player, ByteMessage buf) {
        buf.putShort(((player.getChatTextColor() & 0xFF) << 8) + (player.getChatTextEffects() & 0xFF), ByteOrder.LITTLE); // writeWordBigEndian
        buf.put(player.playerRights);

        // Mystic client expects a null-terminated string for chat (not the packed 317 bytes).
        String chatMessage = player.getChatTextMessage();
        if (chatMessage == null) {
            chatMessage = "";
        }
        buf.putString(chatMessage);
    }

    @Override
    public void appendFaceCharacter(Player player, ByteMessage buf) {
        buf.putShort(player.getFaceTarget(), ByteOrder.LITTLE); // writeWordBigEndian
    }

    public static void appendPlayerAppearance(Player player, ByteMessage buf) {
        byte[] appearanceBytes = getInstance().getAppearanceBytes(player);
        buf.put(appearanceBytes.length, ValueType.NEGATE); // writeByteC = -value
        buf.putBytes(appearanceBytes);
    }

    public byte[] getAppearanceBytes(Player player) {
        if (getSyncAppearanceCacheEnabled()
                && !player.getUpdateFlags().isRequired(UpdateFlag.APPEARANCE)
                && player.isCachedAppearanceValid()) {
            SynchronizationContext.recordPlayerAppearanceCacheHit(true);
            return player.getCachedAppearanceBytes();
        }

        ByteMessage playerProps = withAppearanceScratch();
        try {
            playerProps.put(player.getGender());
            playerProps.put((byte) player.headIcon); // Head icon aka prayer over head
            playerProps.put((byte) player.skullIcon); // Skull icon
            if (!player.isNpc) {
                if (player.getEquipment()[Equipment.Slot.HEAD.getId()] > 1) {
                    playerProps.putShort(0x200 + player.getEquipment()[Equipment.Slot.HEAD.getId()]);
                } else {
                    playerProps.put(0);
                }
                if (player.getEquipment()[Equipment.Slot.CAPE.getId()] > 1) {
                    playerProps.putShort(0x200 + player.getEquipment()[Equipment.Slot.CAPE.getId()]);
                } else {
                    playerProps.put(0);
                }
                if (player.getEquipment()[Equipment.Slot.NECK.getId()] > 1) {
                    playerProps.putShort(0x200 + player.getEquipment()[Equipment.Slot.NECK.getId()]);
                } else {
                    playerProps.put(0);
                }
                if (player.getEquipment()[Equipment.Slot.WEAPON.getId()] > 1 && !player.UsingAgility) {
                    playerProps.putShort(0x200 + player.getEquipment()[Equipment.Slot.WEAPON.getId()]);
                } else {
                    playerProps.put(0);
                }
                if (player.getEquipment()[Equipment.Slot.CHEST.getId()] > 1) {
                    playerProps.putShort(0x200 + player.getEquipment()[Equipment.Slot.CHEST.getId()]);
                } else {
                    playerProps.putShort(0x100 + player.getTorso());
                }
                if (player.getEquipment()[Equipment.Slot.SHIELD.getId()] > 1 && !player.UsingAgility) {
                    playerProps.putShort(0x200 + player.getEquipment()[Equipment.Slot.SHIELD.getId()]);
                } else {
                    playerProps.put(0);
                }
                if (!Server.itemManager.isFullBody(player.getEquipment()[Equipment.Slot.CHEST.getId()])) {
                    playerProps.putShort(0x100 + player.getArms());
                } else {
                    playerProps.put(0);
                }
                if (player.getEquipment()[Equipment.Slot.LEGS.getId()] > 1) {
                    playerProps.putShort(0x200 + player.getEquipment()[Equipment.Slot.LEGS.getId()]);
                } else {
                    playerProps.putShort(0x100 + player.getLegs());
                }
                if (!Server.itemManager.isFullHelm(player.getEquipment()[Equipment.Slot.HEAD.getId()]) && !Server.itemManager.isMask(player.getEquipment()[Equipment.Slot.HEAD.getId()])) {
                    playerProps.putShort(0x100 + player.getHead()); // head
                } else {
                    playerProps.put(0);
                }
                if (player.getEquipment()[Equipment.Slot.HANDS.getId()] > 1) {
                    playerProps.putShort(0x200 + player.getEquipment()[Equipment.Slot.HANDS.getId()]);
                } else {
                    playerProps.putShort(0x100 + player.getHands());
                }
                if (player.getEquipment()[Equipment.Slot.FEET.getId()] > 1) {
                    playerProps.putShort(0x200 + player.getEquipment()[Equipment.Slot.FEET.getId()]);
                } else {
                    playerProps.putShort(0x100 + player.getFeet());
                }
                if (!Server.itemManager.isMask(player.getEquipment()[Equipment.Slot.HEAD.getId()]) && (player.playerLooks[0] != 1)) {
                    playerProps.putShort(0x100 + player.getBeard());
                } else {
                    playerProps.put(0); // 0 = nothing on and girl don't have beard
                    // so send 0. -bakatool
                }
            } else {
                playerProps.putShort(-1);
                playerProps.putShort(player.getPlayerNpc());
            }
            // array of 5 bytes defining the colors
            playerProps.put(player.playerLooks[8]); // hair color
            playerProps.put(player.playerLooks[9]); // torso color.
            playerProps.put(player.playerLooks[10]); // leg color
            playerProps.put(player.playerLooks[11]); // feet color
            playerProps.put(player.playerLooks[12]); // skin color (0-6)
            playerProps.putShort(player.getStandAnim()); // standAnimIndex
            playerProps.putShort(player.getWalkAnim()); // standTurnAnimIndex, 823 default
            playerProps.putShort(player.getWalkAnim()); // walkAnimIndex
            playerProps.putShort(player.getWalkAnim()); // turn180AnimIndex, 820 default
            playerProps.putShort(player.getWalkAnim()); // turn90CWAnimIndex, 821 default
            playerProps.putShort(player.getWalkAnim()); // turn90CCWAnimIndex, 822 default
            playerProps.putShort(player.getRunAnim()); // runAnimIndex

            playerProps.putLong(Utils.playerNameToInt64(player.getPlayerName()));
            playerProps.put(player.determineCombatLevel()); // combat level
            playerProps.putShort(0); // incase != 0, writes skill-%d
            byte[] appearanceBytes = playerProps.toByteArray();
            if (getSyncAppearanceCacheEnabled()) {
                player.cacheAppearanceBytes(appearanceBytes);
            }
            SynchronizationContext.recordPlayerAppearanceCacheHit(false);
            return appearanceBytes;
        } finally {
            releaseScratch(playerProps);
        }
    }

    public ByteMessage withScratchUpdateBlock() {
        if (getSyncScratchBufferReuseEnabled()) {
            SynchronizationContext.recordPlayerScratchReuse();
            return ThreadLocalSyncScratch.playerUpdateBlock();
        }
        return ByteMessage.raw(8192);
    }

    private ByteMessage withAppearanceScratch() {
        if (getSyncScratchBufferReuseEnabled()) {
            SynchronizationContext.recordPlayerScratchReuse();
            return ThreadLocalSyncScratch.appearanceBlock();
        }
        return ByteMessage.raw(256);
    }

    private ByteMessage withSharedBlock() {
        if (getSyncScratchBufferReuseEnabled()) {
            SynchronizationContext.recordPlayerScratchReuse();
            return ThreadLocalSyncScratch.sharedBlock();
        }
        return ByteMessage.raw(512);
    }

    private static void releaseScratch(ByteMessage message) {
        if (!getSyncScratchBufferReuseEnabled()) {
            message.releaseAll();
        }
    }

    private int movementMode(Player player) {
        if (player.didTeleport()) {
            return 3;
        }
        if (player.getPrimaryDirection() == -1) {
            return 0;
        }
        return player.getSecondaryDirection() == -1 ? 1 : 2;
    }

    public void sendServerUpdateIfNeeded(Player player) {
        if (Server.updateRunning) {
            int seconds = Server.updateSeconds + ((int) (Server.updateStartTime - System.currentTimeMillis()) / 1000);
            ((Client) player).send(new net.dodian.uber.game.netty.listener.out.SystemUpdateTimer(seconds * 50 / 30));
        }
    }

    private void finishPlayerSync(ByteMessage stream, ByteMessage updateBlock) {
        stream.putBits(11, 2047);
        stream.endBitAccess();
        if (updateBlock.getBuffer().writerIndex() > 0) {
            stream.putBytes(updateBlock);
        }
    }

    private java.util.BitSet toBitSet(int[] slots, int count) {
        java.util.BitSet set = new java.util.BitSet(PlayerHandler.players.length);
        int limit = Math.min(count, slots.length);
        for (int i = 0; i < limit; i++) {
            int slot = slots[i];
            if (slot >= 0) {
                set.set(slot);
            }
        }
        return set;
    }

    private Player resolvePlayerSlot(int slot) {
        if (slot < 0 || slot >= PlayerHandler.players.length) {
            return null;
        }
        return PlayerHandler.players[slot];
    }

    @Override
    public void appendFaceCoordinates(Player player, ByteMessage buf) {
        buf.putShort(player.getFaceCoordinateX(), ByteOrder.LITTLE, ValueType.ADD); // writeWordBigEndianA
        buf.putShort(player.getFaceCoordinateY(), ByteOrder.LITTLE); // writeWordBigEndian
    }

    @Override
    public void appendPrimaryHit(Player player, ByteMessage buf) {
        synchronized (this) {
            // Client appendPlayerUpdateMask (mask & 0x20) expects:
            // short damage, byte type, short currentHp, short maxHp
            int damage = player.getDamageDealt();
            if (damage < Short.MIN_VALUE) damage = Short.MIN_VALUE;
            if (damage > Short.MAX_VALUE) damage = Short.MAX_VALUE;
            buf.putShort(damage);

            int type;
            if (player.getDamageDealt() == 0) {
                type = 0; // miss
            } else if (player.getHitType() == Entity.hitType.BURN) {
                type = 4;
            } else if (player.getHitType() == Entity.hitType.CRIT) {
                type = 3;
            } else if (player.getHitType() == Entity.hitType.POISON) {
                type = 2;
            } else {
                type = 1; // normal
            }
            buf.put(type);

            int current = Math.max(0, player.getCurrentHealth());
            int max = Math.max(1, player.getMaxHealth());
            buf.putShort(current);
            buf.putShort(max);
        }
    }

    public void appendPrimaryHit2(Player player, ByteMessage buf) {
        synchronized(this) {
            // Client appendPlayerUpdateMask (mask & 0x200) uses same layout as primary hit
            int damage = player.getDamageDealt2();
            if (damage < Short.MIN_VALUE) damage = Short.MIN_VALUE;
            if (damage > Short.MAX_VALUE) damage = Short.MAX_VALUE;
            buf.putShort(damage);

            int type;
            if (player.getDamageDealt2() == 0) {
                type = 0; // miss
            } else if (player.getHitType2() == Entity.hitType.BURN) {
                type = 4;
            } else if (player.getHitType2() == Entity.hitType.CRIT) {
                type = 3;
            } else if (player.getHitType2() == Entity.hitType.POISON) {
                type = 2;
            } else {
                type = 1; // normal
            }
            buf.put(type);

            int current = Math.max(0, player.getCurrentHealth());
            int max = Math.max(1, player.getMaxHealth());
            buf.putShort(current);
            buf.putShort(max);
        }
    }

}
