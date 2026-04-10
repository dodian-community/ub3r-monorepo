package net.dodian.uber.game.model.entity.player;

import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.systems.interaction.PersonalPassageService;
import net.dodian.uber.game.systems.pathing.collision.CollisionManager;
import net.dodian.utilities.Utils;

final class PlayerMovementState {
    private final Player owner;
    private int currentX;
    private int currentY;
    private boolean didTeleport = false;
    private boolean mapRegionDidChange = false;
    private int primaryDirection = -1;
    private int secondaryDirection = -1;

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
    }

    void resetWalkingQueue() {
        owner.walkingBlock = true;
        owner.wQueueReadPtr = owner.wQueueWritePtr = 0;
        owner.newWalkCmdSteps = 0;
        for (int i = 0; i < Player.WALKING_QUEUE_SIZE; i++) {
            owner.walkingQueueX[i] = currentX;
            owner.walkingQueueY[i] = currentY;
        }
    }

    void addToWalkingQueue(int x, int y) {
        int next = (owner.wQueueWritePtr + 1) % Player.WALKING_QUEUE_SIZE;
        if (next == owner.wQueueReadPtr) {
            return;
        }
        owner.walkingQueueX[owner.wQueueWritePtr] = x;
        owner.walkingQueueY[owner.wQueueWritePtr] = y;
        owner.wQueueWritePtr = next;
    }

    int getNextWalkingDirection() {
        if (owner.wQueueReadPtr == owner.wQueueWritePtr) {
            return -1;
        }
        int dir;
        do {
            dir = Utils.direction(currentX, currentY, owner.walkingQueueX[owner.wQueueReadPtr], owner.walkingQueueY[owner.wQueueReadPtr]);
            if (dir == -1) {
                owner.wQueueReadPtr = (owner.wQueueReadPtr + 1) % Player.WALKING_QUEUE_SIZE;
            } else {
                dir /= 2;
                if (dir < 0 || dir > 7) {
                    owner.println_debug("Invalid direction calculated: " + dir);
                    resetWalkingQueue();
                    return -1;
                }
            }
        } while (dir == -1 && owner.wQueueReadPtr != owner.wQueueWritePtr);

        if (dir == -1) {
            return -1;
        }

        int deltaX = Utils.directionDeltaX[dir];
        int deltaY = Utils.directionDeltaY[dir];

        // Per-step collision validation: stop if the next tile is blocked or
        // a wall prevents movement in this direction.
        int absX = owner.getPosition().getX();
        int absY = owner.getPosition().getY();
        int z = owner.getPosition().getZ();
        if (!CollisionManager.global().traversable(absX + deltaX, absY + deltaY, z, deltaX, deltaY) &&
            !PersonalPassageService.canTraverse(owner, absX, absY, absX + deltaX, absY + deltaY, z)) {
            resetWalkingQueue();
            return -1;
        }

        Position newPos = new Position(absX, absY, z);
        currentX += deltaX;
        currentY += deltaY;
        newPos.move(deltaX, deltaY);
        owner.getPosition().moveTo(newPos.getX(), newPos.getY());
        owner.setLastWalkDelta(deltaX, deltaY);
        owner.setPersistedFaceCoord(absX + deltaX, absY + deltaY);
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

        primaryDirection = getNextWalkingDirection();
        if (primaryDirection == -1) {
            return;
        }
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
            for (int i = 0; i < Player.WALKING_QUEUE_SIZE; i++) {
                owner.walkingQueueX[i] += deltaX;
                owner.walkingQueueY[i] += deltaY;
            }
        }
    }

    void postProcessing() {
        if (owner.walkingBlock) {
            owner.walkingBlock = false;
            return;
        }
        if (owner.newWalkCmdSteps > 0) {
            int firstX = owner.newWalkCmdX[0];
            int firstY = owner.newWalkCmdY[0];
            int lastDir;
            boolean found = false;
            owner.numTravelBackSteps = 0;
            int ptr = owner.wQueueReadPtr;
            int dir = Utils.direction(currentX, currentY, firstX, firstY);
            if (dir != -1 && (dir & 1) != 0) {
                do {
                    lastDir = dir;
                    if (--ptr < 0) {
                        ptr = Player.WALKING_QUEUE_SIZE - 1;
                    }
                    owner.travelBackX[owner.numTravelBackSteps] = owner.walkingQueueX[ptr];
                    owner.travelBackY[owner.numTravelBackSteps++] = owner.walkingQueueY[ptr];
                    dir = Utils.direction(owner.walkingQueueX[ptr], owner.walkingQueueY[ptr], firstX, firstY);
                    if (lastDir != dir) {
                        found = true;
                        break;
                    }
                } while (ptr != owner.wQueueWritePtr);
            } else {
                found = true;
            }

            owner.wQueueWritePtr = owner.wQueueReadPtr;
            addToWalkingQueue(currentX, currentY);

            if (dir != -1 && (dir & 1) != 0) {
                for (int i = 0; i < owner.numTravelBackSteps - 1; i++) {
                    addToWalkingQueue(owner.travelBackX[i], owner.travelBackY[i]);
                }
                int wayPointX2 = owner.travelBackX[owner.numTravelBackSteps - 1];
                int wayPointY2 = owner.travelBackY[owner.numTravelBackSteps - 1];
                int wayPointX1;
                int wayPointY1;
                if (owner.numTravelBackSteps == 1) {
                    wayPointX1 = currentX;
                    wayPointY1 = currentY;
                } else {
                    wayPointX1 = owner.travelBackX[owner.numTravelBackSteps - 2];
                    wayPointY1 = owner.travelBackY[owner.numTravelBackSteps - 2];
                }
                dir = Utils.direction(wayPointX1, wayPointY1, wayPointX2, wayPointY2);
                if (!(dir == -1 || (dir & 1) != 0)) {
                    dir >>= 1;
                    int x = wayPointX1;
                    int y = wayPointY1;
                    while (x != wayPointX2 || y != wayPointY2) {
                        x += Utils.directionDeltaX[dir];
                        y += Utils.directionDeltaY[dir];
                        if ((Utils.direction(x, y, firstX, firstY) & 1) == 0) {
                            found = true;
                            break;
                        }
                    }
                    if (found) {
                        addToWalkingQueue(wayPointX1, wayPointY1);
                    }
                }
            } else {
                for (int i = 0; i < owner.numTravelBackSteps; i++) {
                    addToWalkingQueue(owner.travelBackX[i], owner.travelBackY[i]);
                }
            }
            for (int i = 0; i < owner.newWalkCmdSteps; i++) {
                addToWalkingQueue(owner.newWalkCmdX[i], owner.newWalkCmdY[i]);
            }
        }
        owner.isRunning = owner.UsingAgility && System.currentTimeMillis() < owner.walkBlock ? owner.newWalkCmdIsRunning : owner.buttonOnRun;
        owner.newWalkCmdSteps = 0;
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
