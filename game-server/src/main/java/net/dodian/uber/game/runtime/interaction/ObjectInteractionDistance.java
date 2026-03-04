package net.dodian.uber.game.runtime.interaction;

import net.dodian.cache.object.GameObjectData;
import net.dodian.cache.object.GameObjectDef;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.WalkToTask;
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
        ITEM_ON_OBJECT,
        MAGIC,
    }

    public static Position resolveDistancePosition(
            Client client,
            WalkToTask task,
            int objectId,
            GameObjectData objectData,
            GameObjectDef def,
            DistanceMode mode
    ) {
        Position walkTo = task.getWalkToPosition();
        Position objectPosition = null;

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
}

