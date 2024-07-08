package net.dodian.uber.game.network.packets.incoming;

import net.dodian.cache.object.GameObjectData;
import net.dodian.cache.object.GameObjectDef;
import net.dodian.uber.game.event.Event;
import net.dodian.uber.game.event.EventManager;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.WalkToTask;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.object.GlobalObject;
import net.dodian.uber.game.model.object.Object;
import net.dodian.uber.game.network.packets.Packet;
import net.dodian.uber.game.network.packets.outgoing.SendMessage;
import net.dodian.uber.game.model.player.skills.Skill;
import net.dodian.utilities.Misc;

import java.util.Objects;

import static net.dodian.utilities.DotEnvKt.getGameWorldId;

public class MagicOnObject implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        int objectX = client.getInputStream().readSignedWordBigEndian();
        int magicID = client.getInputStream().readSignedWordA();
        int objectY = client.getInputStream().readSignedWordA();
        int objectID = client.getInputStream().readUnsignedWordBigEndianA() - 128;
        final WalkToTask task = new WalkToTask(WalkToTask.Action.OBJECT_FIRST_CLICK, objectID,
                new Position(objectX, objectY));
        GameObjectDef def = Misc.getObject(objectID, objectX, objectY, client.getPosition().getZ());
        GameObjectData object = GameObjectData.forId(task.getWalkToId());
        client.setWalkToTask(task);
        if (getGameWorldId() > 1 && object != null)
            client.send(new SendMessage("Magic: id: "+magicID+", obj id:" + object.getId() + ", " + object.getName() + ", Coord: " + objectX + ", " + objectY + ", " + (def == null ? "Def is null!" : def.getFace())));
        if (client.randomed || client.UsingAgility) {
            return;
        }
        EventManager.getInstance().registerEvent(new Event(600) {

            @Override
            public void execute() {

                if (client.disconnected) {
                    this.stop();
                    return;
                }

                if (client.getWalkToTask() != task) {
                    this.stop();
                    return;
                }
                Position objectPosition = null;
                Object o = new Object(objectID, task.getWalkToPosition().getX(), task.getWalkToPosition().getY(), task.getWalkToPosition().getZ(), 10);
                if (def != null && !GlobalObject.hasGlobalObject(o)) {
                    objectPosition = Misc.goodDistanceObject(task.getWalkToPosition().getX(), task.getWalkToPosition().getY(), client.getPosition().getX(), client.getPosition().getY(), Objects.requireNonNull(object).getSizeX(def.getFace()), object.getSizeY(def.getFace()), client.getPosition().getZ());
                } else {
                    if (GlobalObject.hasGlobalObject(o)) {
                        objectPosition = Misc.goodDistanceObject(task.getWalkToPosition().getX(), task.getWalkToPosition().getY(), client.getPosition().getX(), client.getPosition().getY(), Objects.requireNonNull(object).getSizeX(o.face), object.getSizeY(o.type), o.z);
                    } else if (object != null) {
                        objectPosition = Misc.goodDistanceObject(task.getWalkToPosition().getX(), task.getWalkToPosition().getY(), client.getPosition().getX(), client.getPosition().getY(), object.getSizeX(), object.getSizeY(), client.getPosition().getZ());
                    }
                }
                if (objectID == 23131)
                    objectPosition = Misc.goodDistanceObject(task.getWalkToPosition().getX(), 3552, client.getPosition().getX(), client.getPosition().getY(), Objects.requireNonNull(object).getSizeX(), object.getSizeY(), client.getPosition().getZ());
                if(objectID == 16466)
                    objectPosition = Misc.goodDistanceObject(task.getWalkToPosition().getX(), 2972, client.getPosition().getX(), client.getPosition().getY(), 1, 1, client.getPosition().getZ());
                if(objectID == 11643) {
                    objectPosition = Misc.goodDistanceObject(task.getWalkToPosition().getX(), task.getWalkToPosition().getY(), client.getPosition().getX(), client.getPosition().getY(), 2, client.getPosition().getZ());
                }
                if (objectPosition == null)
                    this.stop();
                atObject(client, magicID, task.getWalkToId(), task.getWalkToPosition(), object);
                client.setWalkToTask(null);
                this.stop();
            }
        });
    }

    public void atObject(Client client, int magicID, int objectID, Position objectPosition, GameObjectData obj) {
        //Should we reset actions here?!
        client.setFocus(objectPosition.getX(), objectPosition.getY());
        if(objectID == 2151 && magicID == 1179) { //Water Obelisk
            if(client.getSkillLevel(Skill.MAGIC) >= 55) {
                if(!client.playerHasItem(567) || !client.playerHasItem(564, 3)) {
                    client.send(new SendMessage("You need one unpowered orb and 3 cosmic runes to cast on this obelisk."));
                    return;
                }
                client.setSkillAction(Skill.MAGIC.getId(), 571, 1, 567, 564, 725, 726, 5);
                client.skillMessage = "You charge the orb with the power of water.";
            } else client.send(new SendMessage("You need level 55 magic in order to cast this spell!"));
        }
        else if(objectID == 2150 && magicID == 1182) { //Earth Obelisk
            if(client.getSkillLevel(Skill.MAGIC) >= 60) {
                if(!client.playerHasItem(567) || !client.playerHasItem(564, 3)) {
                    client.send(new SendMessage("You need one unpowered orb and 3 cosmic runes to cast on this obelisk."));
                    return;
                }
                client.setSkillAction(Skill.MAGIC.getId(), 575, 1, 567, 564, 800, 726, 5);
                client.skillMessage = "You charge the orb with the power of earth.";
            } else client.send(new SendMessage("You need level 60 magic in order to cast this spell!"));
        }
        else if(objectID == 2153 && magicID == 1184) { //Fire Obelisk
            if(client.getSkillLevel(Skill.MAGIC) >= 65) {
                if(!client.playerHasItem(567) || !client.playerHasItem(564, 3)) {
                    client.send(new SendMessage("You need one unpowered orb and 3 cosmic runes to cast on this obelisk."));
                    return;
                }
                client.setSkillAction(Skill.MAGIC.getId(), 569, 1, 567, 564, 875, 726, 5);
                client.skillMessage = "You charge the orb with the power of fire.";
            } else client.send(new SendMessage("You need level 65 magic in order to cast this spell!"));
        }
        else if(objectID == 2152 && magicID == 1186) { //Air Obelisk
            if(client.getSkillLevel(Skill.MAGIC) >= 70) {
                if(!client.playerHasItem(567) || !client.playerHasItem(564, 3)) {
                    client.send(new SendMessage("You need one unpowered orb and 3 cosmic runes to cast on this obelisk."));
                    return;
                }
                client.setSkillAction(Skill.MAGIC.getId(), 573, 1, 567, 564, 950, 726, 5);
                client.skillMessage = "You charge the orb with the power of air.";
            } else client.send(new SendMessage("You need level 70 magic in order to cast this spell!"));
        }
    }

}