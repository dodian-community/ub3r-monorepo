package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.cache.object.GameObjectData;
import net.dodian.cache.object.GameObjectDef;
import net.dodian.uber.game.event.Event;
import net.dodian.uber.game.event.EventManager;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.WalkToTask;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.object.GlobalObject;
import net.dodian.uber.game.model.object.Object;
import net.dodian.uber.game.model.player.packets.Packet;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.uber.game.model.player.skills.Skill;
import net.dodian.utilities.Misc;

import static net.dodian.uber.game.model.player.skills.Skill.FARMING;
import static net.dodian.utilities.DotEnvKt.getGameWorldId;

public class ClickObject4 implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        int objectX = client.getInputStream().readUnsignedWordBigEndianA();
        int objectID = client.getInputStream().readUnsignedWordA();
        int objectY = client.getInputStream().readUnsignedWordBigEndianA();
        final WalkToTask task = new WalkToTask(WalkToTask.Action.OBJECT_FOURTH_CLICK, objectID,
                new Position(objectX, objectY));
        GameObjectDef def = Misc.getObject(objectID, objectX, objectY, client.getPosition().getZ());
        GameObjectData object = GameObjectData.forId(task.getWalkToId());
        client.setWalkToTask(task);
        if (getGameWorldId() > 1 && object != null)
            client.send(new SendMessage("Obj click4: " + object.getId() + ", " + object.getName() + ", Coord: " + objectX + ", " + objectY + ", " + (def == null ? "Def is null!" : def.getFace())));
        if (client.randomed) {
            return;
        }
        EventManager.getInstance().registerEvent(new Event(600) {

            @Override
            public void execute() {

                if (client == null || client.disconnected) {
                    this.stop();
                    return;
                }

                if (client.getWalkToTask() != task) {
                    this.stop();
                    return;
                }
                Position objectPosition = null;
                if (def != null)
                    objectPosition = Misc.goodDistanceObject(task.getWalkToPosition().getX(), task.getWalkToPosition().getY(), client.getPosition().getX(), client.getPosition().getY(), object.getSizeX(def.getFace()), object.getSizeY(def.getFace()), client.getPosition().getZ());
                else {
                    Object o = new Object(objectID, task.getWalkToPosition().getX(), task.getWalkToPosition().getY(), task.getWalkToPosition().getZ(), 10);
                    if (GlobalObject.hasGlobalObject(o)) {
                        objectPosition = Misc.goodDistanceObject(task.getWalkToPosition().getX(), task.getWalkToPosition().getY(), client.getPosition().getX(), client.getPosition().getY(), object.getSizeX(o.face), object.getSizeY(o.type), client.getPosition().getZ());
                    } else if (object != null)
                        objectPosition = Misc.goodDistanceObject(task.getWalkToPosition().getX(), task.getWalkToPosition().getY(), client.getPosition().getX(), client.getPosition().getY(), object.getSizeX(), object.getSizeY(), client.getPosition().getZ());
                }
                if (objectPosition == null)
                    return;

                clickObject3(client, task.getWalkToId(), task.getWalkToPosition(), object);
                client.setWalkToTask(null);
                this.stop();
            }

        });
    }

    public void clickObject3(Client client, int objectID, Position position, GameObjectData obj) {
        //String objectName = obj == null ? "" : obj.getName().toLowerCase();
        client.setFocus(position.getX(), position.getY());
        if(objectID >= 8550 && objectID <= 8557 || (objectID == 27114 || objectID == 27113)) { //Allotment guide
            client.showSkillMenu(FARMING.getId(), 0);
        }
        if(objectID >= 7847 && objectID <= 7850 || objectID == 27111) { //Flowers guide
            client.showSkillMenu(FARMING.getId(), 1);
        }
        if(objectID >= 7577 && objectID <= 7580) { //Bushes guide
            client.showSkillMenu(FARMING.getId(), 2);
        }
        if(objectID >= 8150 && objectID <= 8153 || objectID == 27115) { //Herbs guide
            client.showSkillMenu(FARMING.getId(), 3);
        }
        if(objectID >= 8389 && objectID <= 8391 || objectID == 19147) { //Tree guide
            client.showSkillMenu(FARMING.getId(), 4);
        }
        if(objectID >= 7962 && objectID <= 7965 || objectID == 26579) { //Fruit tree guide
            client.showSkillMenu(FARMING.getId(), 5);
        }
    }

}
