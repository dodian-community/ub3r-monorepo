package net.dodian.uber.game.runtime.interaction;

import net.dodian.cache.object.GameObjectData;
import net.dodian.cache.object.GameObjectDef;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.object.GlobalObject;
import net.dodian.uber.game.model.object.Object;
import net.dodian.utilities.Misc;

/**
 * Shared distance resolution for object interactions.
 *
 * This was lifted from the legacy Event(600) object interaction handlers to preserve 317 behavior.
 */
public final class ObjectInteractionDistance {

    private ObjectInteractionDistance() {}

    public enum DistanceMode {
        CLICK,
        MINING,
        ITEM_ON_OBJECT,
        MAGIC,
        POLICY_NEAREST_BOUNDARY_CARDINAL,
        POLICY_NEAREST_BOUNDARY_ANY,
    }

    public static Position resolveDistancePosition(
            Client client,
            Position walkTo,
            int objectId,
            GameObjectData objectData,
            GameObjectDef def,
            DistanceMode mode
    ) {
        Position objectPosition = null;

        if (mode == DistanceMode.POLICY_NEAREST_BOUNDARY_CARDINAL) {
            return resolveNearestBoundaryDistancePosition(client, walkTo, objectData, def, objectId, true);
        }
        if (mode == DistanceMode.POLICY_NEAREST_BOUNDARY_ANY) {
            return resolveNearestBoundaryDistancePosition(client, walkTo, objectData, def, objectId, false);
        }

        Object objectAtTile = new Object(objectId, walkTo.getX(), walkTo.getY(), walkTo.getZ(), 10);
        if (def != null && !GlobalObject.hasGlobalObject(objectAtTile)) {
            if (objectData != null) {
                objectPosition = Misc.goodDistanceObject(
                        walkTo.getX(),
                        walkTo.getY(),
                        client.getPosition().getX(),
                        client.getPosition().getY(),
                        objectData.getSizeX(def.getFace()),
                        objectData.getSizeY(def.getFace()),
                        client.getPosition().getZ()
                );
            }
        } else {
            if (GlobalObject.hasGlobalObject(objectAtTile)) {
                if (objectData != null) {
                    objectPosition = Misc.goodDistanceObject(
                            walkTo.getX(),
                            walkTo.getY(),
                            client.getPosition().getX(),
                            client.getPosition().getY(),
                            objectData.getSizeX(objectAtTile.face),
                            objectData.getSizeY(objectAtTile.type),
                            objectAtTile.z
                    );
                }
            } else if (objectData != null) {
                objectPosition = Misc.goodDistanceObject(
                        walkTo.getX(),
                        walkTo.getY(),
                        client.getPosition().getX(),
                        client.getPosition().getY(),
                        objectData.getSizeX(),
                        objectData.getSizeY(),
                        client.getPosition().getZ()
                );
            }
        }

        if (mode == DistanceMode.CLICK || mode == DistanceMode.MAGIC) {
            if (objectId == 23131 && objectData != null) {
                objectPosition = Misc.goodDistanceObject(
                        walkTo.getX(),
                        3552,
                        client.getPosition().getX(),
                        client.getPosition().getY(),
                        objectData.getSizeX(),
                        objectData.getSizeY(),
                        client.getPosition().getZ()
                );
            }
            if (objectId == 16466) {
                objectPosition = Misc.goodDistanceObject(
                        walkTo.getX(),
                        2972,
                        client.getPosition().getX(),
                        client.getPosition().getY(),
                        1,
                        1,
                        client.getPosition().getZ()
                );
            }
            if (objectId == 11643) {
                objectPosition = Misc.goodDistanceObject(
                        walkTo.getX(),
                        walkTo.getY(),
                        client.getPosition().getX(),
                        client.getPosition().getY(),
                        2,
                        client.getPosition().getZ()
                );
            }
        }

        if (mode == DistanceMode.ITEM_ON_OBJECT) {
            if (objectId == 23131 && objectData != null) {
                objectPosition = Misc.goodDistanceObject(
                        walkTo.getX(),
                        3552,
                        client.getPosition().getX(),
                        client.getPosition().getY(),
                        objectData.getSizeX(),
                        objectData.getSizeY(),
                        client.getPosition().getZ()
                );
            }
            if (objectId == 16466) {
                objectPosition = Misc.goodDistanceObject(
                        walkTo.getX(),
                        2972,
                        client.getPosition().getX(),
                        client.getPosition().getY(),
                        1,
                        3,
                        client.getPosition().getZ()
                );
            }
        }

        return objectPosition;
    }

    private static Position resolveNearestBoundaryDistancePosition(
            Client client,
            Position walkTo,
            GameObjectData objectData,
            GameObjectDef def,
            int objectId,
            boolean cardinalOnly
    ) {
        if (client.getPosition().getZ() != walkTo.getZ()) {
            return null;
        }

        Footprint footprint = resolveObjectFootprint(walkTo, objectData, def, objectId);
        Position nearestBoundaryTile = resolveNearestBoundaryTile(client.getPosition(), footprint);
        if (nearestBoundaryTile == null) {
            return null;
        }

        if (cardinalOnly && isCardinalAdjacent(client.getPosition(), nearestBoundaryTile)) {
            return nearestBoundaryTile;
        }
        if (!cardinalOnly && isAdjacent(client.getPosition(), nearestBoundaryTile)) {
            return nearestBoundaryTile;
        }
        return null;
    }

    private static Footprint resolveObjectFootprint(
            Position walkTo,
            GameObjectData objectData,
            GameObjectDef def,
            int objectId
    ) {
        int minX = walkTo.getX();
        int minY = walkTo.getY();
        int maxX = walkTo.getX();
        int maxY = walkTo.getY();

        if (objectData != null) {
            int sizeX = objectData.getSizeX();
            int sizeY = objectData.getSizeY();

            Object globalObject = GlobalObject.getGlobalObject(walkTo.getX(), walkTo.getY());
            if (globalObject != null && globalObject.id == objectId) {
                int rotation = globalObject.face;
                sizeX = objectData.getSizeX(rotation);
                sizeY = objectData.getSizeY(rotation);
            } else if (def != null) {
                int rotation = def.getFace();
                sizeX = objectData.getSizeX(rotation);
                sizeY = objectData.getSizeY(rotation);
            }

            sizeX = Math.max(1, sizeX);
            sizeY = Math.max(1, sizeY);
            maxX = minX + sizeX - 1;
            maxY = minY + sizeY - 1;
        }

        return new Footprint(minX, minY, maxX, maxY, walkTo.getZ());
    }

    private static Position resolveNearestBoundaryTile(Position player, Footprint footprint) {
        int nearestX = clamp(player.getX(), footprint.minX, footprint.maxX);
        int nearestY = clamp(player.getY(), footprint.minY, footprint.maxY);

        if (nearestX > footprint.minX && nearestX < footprint.maxX
                && nearestY > footprint.minY && nearestY < footprint.maxY) {
            int toWest = nearestX - footprint.minX;
            int toEast = footprint.maxX - nearestX;
            int toSouth = nearestY - footprint.minY;
            int toNorth = footprint.maxY - nearestY;

            int minDelta = Math.min(Math.min(toWest, toEast), Math.min(toSouth, toNorth));
            if (minDelta == toWest) {
                nearestX = footprint.minX;
            } else if (minDelta == toEast) {
                nearestX = footprint.maxX;
            } else if (minDelta == toSouth) {
                nearestY = footprint.minY;
            } else {
                nearestY = footprint.maxY;
            }
        }

        return new Position(nearestX, nearestY, footprint.z);
    }

    private static boolean isCardinalAdjacent(Position player, Position tile) {
        if (player.getZ() != tile.getZ()) {
            return false;
        }
        int deltaX = Math.abs(player.getX() - tile.getX());
        int deltaY = Math.abs(player.getY() - tile.getY());
        return (deltaX + deltaY) == 1;
    }

    private static boolean isAdjacent(Position player, Position tile) {
        if (player.getZ() != tile.getZ()) {
            return false;
        }
        int deltaX = Math.abs(player.getX() - tile.getX());
        int deltaY = Math.abs(player.getY() - tile.getY());
        return Math.max(deltaX, deltaY) == 1;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class Footprint {
        private final int minX;
        private final int minY;
        private final int maxX;
        private final int maxY;
        private final int z;

        private Footprint(int minX, int minY, int maxX, int maxY, int z) {
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
            this.z = z;
        }
    }
}
