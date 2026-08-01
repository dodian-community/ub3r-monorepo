package net.dodian.uber.game.model.entity.player;

import java.util.Collection;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.engine.systems.interaction.ClipProbeService;
import net.dodian.uber.game.engine.systems.interaction.PersonalPassageService;
import net.dodian.uber.game.engine.routing.WorldRouteService;
import net.dodian.utilities.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class PlayerMovementState {
    private static final Logger logger = LoggerFactory.getLogger(PlayerMovementState.class);
    private static final long BLOCKED_LOG_INTERVAL_MS = 2000L;
    private final Player owner;
    private int currentX;
    private int currentY;
    private boolean didTeleport = false;
    private boolean mapRegionDidChange = false;
    private int primaryDirection = -1;
    private int secondaryDirection = -1;
    private long lastBlockedLogMs = 0L;
    private int lastBlockedLogX = Integer.MIN_VALUE;
    private int lastBlockedLogY = Integer.MIN_VALUE;
    private int lastBlockedLogZ = Integer.MIN_VALUE;

    PlayerMovementState(Player owner) {
        this.owner = owner;
    }

    void initializeRegionState() {
        owner.teleportToX = -1;
        owner.teleportToY = -1;
        owner.mapRegionX = -1;
        owner.mapRegionY = -1;
        currentX = 0;
        currentY = 0;
        owner.teleportToZ = 0;
    }

    void resetTransientState() {
        owner.mapRegionX = -1;
        owner.mapRegionY = -1;
        currentX = 0;
        currentY = 0;
        owner.routeDestination().clear();
    }

    void resetWalkingQueue() {
        owner.routeDestination().clear();
        owner.isRunning = false;
    }

    void replaceRoute(Collection<MovementPoint> waypoints, boolean running) {
        owner.routeDestination().replace(waypoints, running);
    }

    int getNextWalkingDirection() {
        RouteDestination destination = owner.routeDestination();
        MovementPoint waypoint = destination.peekFirst();
        while (waypoint != null &&
                waypoint.getX() == owner.getPosition().getX() &&
                waypoint.getY() == owner.getPosition().getY() &&
                waypoint.getZ() == owner.getPosition().getZ()) {
            destination.pollFirst();
            waypoint = destination.peekFirst();
        }
        if (waypoint == null) return -1;
        if (waypoint.getZ() != owner.getPosition().getZ()) {
            resetWalkingQueue();
            return -1;
        }

        int dir = Utils.direction(
                owner.getPosition().getX(),
                owner.getPosition().getY(),
                waypoint.getX(),
                waypoint.getY()
        );
        if (dir == -1) return -1;
        dir /= 2;
        if (dir < 0 || dir > 7) {
            owner.println_debug("Invalid direction calculated: " + dir);
            resetWalkingQueue();
            return -1;
        }

        int deltaX = Utils.directionDeltaX[dir];
        int deltaY = Utils.directionDeltaY[dir];

        // a wall prevents movement in this direction.
        int absX = owner.getPosition().getX();
        int absY = owner.getPosition().getY();
        int z = owner.getPosition().getZ();

        net.dodian.uber.game.engine.systems.interaction.InteractionIntent pending = owner.getPendingInteraction();
        if (pending != null) {
            int targetX = -1;
            int targetY = -1;
            int targetSize = 1;
            switch (pending) {
                case net.dodian.uber.game.engine.systems.interaction.NpcInteractionIntent npcIntent -> {
                    net.dodian.uber.game.model.entity.npc.Npc npc = net.dodian.uber.game.Server.npcManager.getNpcMap().get(npcIntent.getNpcIndex());
                    if (npc != null) {
                        targetX = npc.getPosition().getX();
                        targetY = npc.getPosition().getY();
                        targetSize = npc.getSize();
                    }
                }
                case net.dodian.uber.game.engine.systems.interaction.ItemOnNpcIntent itemOnNpcIntent -> {
                    net.dodian.uber.game.model.entity.npc.Npc npc = net.dodian.uber.game.Server.npcManager.getNpcMap().get(itemOnNpcIntent.getNpcIndex());
                    if (npc != null) {
                        targetX = npc.getPosition().getX();
                        targetY = npc.getPosition().getY();
                        targetSize = npc.getSize();
                    }
                }
                case net.dodian.uber.game.engine.systems.interaction.MagicOnNpcIntent magicOnNpcIntent -> {
                    net.dodian.uber.game.model.entity.npc.Npc npc = net.dodian.uber.game.Server.npcManager.getNpcMap().get(magicOnNpcIntent.getNpcIndex());
                    if (npc != null) {
                        targetX = npc.getPosition().getX();
                        targetY = npc.getPosition().getY();
                        targetSize = npc.getSize();
                    }
                }
                case net.dodian.uber.game.engine.systems.interaction.PlayerInteractionIntent playerInteractionIntent -> {
                    Client targetPlr = net.dodian.uber.game.engine.systems.world.player.PlayerRegistry.getClient(playerInteractionIntent.getPlayerIndex());
                    if (targetPlr != null) {
                        targetX = targetPlr.getPosition().getX();
                        targetY = targetPlr.getPosition().getY();
                        targetSize = targetPlr.getSize();
                    }
                }
                case net.dodian.uber.game.engine.systems.interaction.MagicOnPlayerIntent magicOnPlayerIntent -> {
                    Client targetPlr = net.dodian.uber.game.engine.systems.world.player.PlayerRegistry.getClient(magicOnPlayerIntent.getVictimIndex());
                    if (targetPlr != null) {
                        targetX = targetPlr.getPosition().getX();
                        targetY = targetPlr.getPosition().getY();
                        targetSize = targetPlr.getSize();
                    }
                }
                case net.dodian.uber.game.engine.systems.interaction.AttackPlayerIntent attackPlayerIntent -> {
                    Client targetPlr = net.dodian.uber.game.engine.systems.world.player.PlayerRegistry.getClient(attackPlayerIntent.getVictimIndex());
                    if (targetPlr != null) {
                        targetX = targetPlr.getPosition().getX();
                        targetY = targetPlr.getPosition().getY();
                        targetSize = targetPlr.getSize();
                    }
                }
                default -> { }
            }

            if (targetX != -1 && targetY != -1) {
                int nextX = absX + deltaX;
                int nextY = absY + deltaY;
                if (nextX >= targetX && nextX < targetX + targetSize &&
                    nextY >= targetY && nextY < targetY + targetSize) {
                    resetWalkingQueue();
                    return -1;
                }
            }
        }

        if (!WorldRouteService.INSTANCE.traversable(absX + deltaX, absY + deltaY, z, deltaX, deltaY, owner.getSize()) &&
            !PersonalPassageService.canTraverse(owner, absX, absY, absX + deltaX, absY + deltaY, z)) {
            logBlockedStep(absX + deltaX, absY + deltaY, z);
            resetWalkingQueue();
            return -1;
        }

        Position newPos = new Position(absX, absY, z);
        currentX += deltaX;
        currentY += deltaY;
        newPos.move(deltaX, deltaY);
        owner.getPosition().moveTo(newPos.getX(), newPos.getY());
        owner.setLastWalkDelta(deltaX, deltaY);
        if (waypoint.getX() == newPos.getX() && waypoint.getY() == newPos.getY()) {
            destination.pollFirst();
        }
        return dir;
    }

    void getNextPlayerMovement() {
        Client temp = (Client) owner;
        mapRegionDidChange = false;
        didTeleport = false;
        primaryDirection = -1;
        secondaryDirection = -1;

        if (owner.teleportToX != -1 && owner.teleportToY != -1) {
            mapRegionDidChange = true;
            if (owner.mapRegionX != -1 && owner.mapRegionY != -1) {
                int relX = owner.teleportToX - owner.mapRegionX * 8;
                int relY = owner.teleportToY - owner.mapRegionY * 8;
                if (relX >= 16 && relX < 88 && relY >= 16 && relY < 88) {
                    mapRegionDidChange = false;
                }
            }
            if (mapRegionDidChange) {
                if (owner.firstSend) {
                    temp.pLoaded = false;
                } else {
                    owner.firstSend = true;
                }
                owner.mapRegionX = (owner.teleportToX >> 3) - 6;
                owner.mapRegionY = (owner.teleportToY >> 3) - 6;
            }
            currentX = owner.teleportToX - 8 * owner.mapRegionX;
            currentY = owner.teleportToY - 8 * owner.mapRegionY;
            Position newPos = new Position(owner.teleportToX, owner.teleportToY, owner.teleportToZ);
            resetWalkingQueue();
            owner.teleportToX = owner.teleportToY = -1;
            owner.teleportToZ = 0;
            didTeleport = true;
            temp.getPosition().moveTo(newPos.getX(), newPos.getY(), newPos.getZ());
            return;
        }

        if (temp.getInDuel() && temp.getDuelFight() && temp.getDuelRule()[9]) {
            resetWalkingQueue();
        }

        primaryDirection = getNextWalkingDirection();
        if (primaryDirection == -1) {
            return;
        }
        owner.isRunning = (owner.UsingAgility || owner.isWalkBlocked())
                ? owner.routeDestination().isRunning()
                : owner.buttonOnRun;
        if (owner.isRunning) {
            secondaryDirection = getNextWalkingDirection();
        }

        int deltaX = 0;
        int deltaY = 0;
        if (currentX < 16) {
            deltaX = 32;
            owner.mapRegionX -= 4;
            mapRegionDidChange = true;
        } else if (currentX >= 88) {
            deltaX = -32;
            owner.mapRegionX += 4;
            mapRegionDidChange = true;
        }
        if (currentY < 16) {
            deltaY = 32;
            owner.mapRegionY -= 4;
            mapRegionDidChange = true;
        } else if (currentY >= 88) {
            deltaY = -32;
            owner.mapRegionY += 4;
            mapRegionDidChange = true;
        }

        if (mapRegionDidChange) {
            if (owner.firstSend) {
                temp.pLoaded = false;
            } else {
                owner.firstSend = true;
            }
            currentX += deltaX;
            currentY += deltaY;
        }
    }

    private void logBlockedStep(int blockedX, int blockedY, int z) {
        long now = System.currentTimeMillis();
        if (now - lastBlockedLogMs < BLOCKED_LOG_INTERVAL_MS &&
            blockedX == lastBlockedLogX &&
            blockedY == lastBlockedLogY &&
            z == lastBlockedLogZ) {
            return;
        }
        lastBlockedLogMs = now;
        lastBlockedLogX = blockedX;
        lastBlockedLogY = blockedY;
        lastBlockedLogZ = z;

        int[] destination = resolveQueuedDestinationAbs();
        ClipProbeService.TileProbe probe = ClipProbeService.probeTile(blockedX, blockedY, z);
        int fromX = owner.getPosition().getX();
        int fromY = owner.getPosition().getY();
        boolean passageForward = PersonalPassageService.canTraverse(owner, fromX, fromY, blockedX, blockedY, z);
        boolean passageReverse = PersonalPassageService.canTraverse(owner, blockedX, blockedY, fromX, fromY, z);
        logger.info(
            "Blocked movement player={} from=({}, {}, {}) blocked=({}, {}, {}) clickDest=({}, {}, {}) flags=0x{} [{}] fullBlocked={} " +
                "objCurrent={} likelyTerrainOrUnknown={} staticOverride={} runtimeOverlay={} objectOverlaps={} overlapDetails={}",
            owner.getPlayerName(),
            fromX,
            fromY,
            owner.getPosition().getZ(),
            blockedX,
            blockedY,
            z,
            destination[0],
            destination[1],
            destination[2],
            Integer.toHexString(probe.getRawFlags()),
            ClipProbeService.formatFlags(probe.getRawFlags()),
            probe.getFullMobBlocked(),
            probe.getBlockedByCurrentObjects(),
            probe.getLikelyTerrainOrUnknownSource(),
            probe.getStaticOverridePresent(),
            probe.getRuntimeOverlayPresent(),
            probe.getObjectMatches().size(),
            ClipProbeService.formatOverlapSummary(probe, 10)

        );
        logger.info(
            "Blocked movement passageProbe player={} edge={}:{}->{}:{}:{} forward={} reverse={}",
            owner.getPlayerName(),
            fromX,
            fromY,
            blockedX,
            blockedY,
            z,
            passageForward,
            passageReverse
        );
    }

    private int[] resolveQueuedDestinationAbs() {
        MovementPoint destination = owner.routeDestination().destination();
        if (destination == null) {
            return new int[] { owner.getPosition().getX(), owner.getPosition().getY(), owner.getPosition().getZ() };
        }
        return new int[] { destination.getX(), destination.getY(), destination.getZ() };
    }

    boolean didTeleport() {
        return didTeleport;
    }

    boolean didMapRegionChange() {
        return mapRegionDidChange;
    }

    int getPrimaryDirection() {
        return primaryDirection;
    }

    int getSecondaryDirection() {
        return secondaryDirection;
    }

    int getCurrentX() {
        return currentX;
    }

    int getCurrentY() {
        return currentY;
    }
}
