package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.cache.object.GameObjectData;
import net.dodian.cache.object.GameObjectDef;
import net.dodian.uber.game.event.Event;
import net.dodian.uber.game.event.EventManager;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.UpdateFlag;
import net.dodian.uber.game.model.WalkToTask;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.object.GlobalObject;
import net.dodian.uber.game.model.object.Object;
import net.dodian.uber.game.model.object.RS2Object;
import net.dodian.uber.game.model.player.packets.Packet;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.uber.game.model.player.skills.Thieving;
import net.dodian.utilities.Misc;
import net.dodian.utilities.Utils;

import java.util.Random;

import static net.dodian.utilities.DotEnvKt.getGameWorldId;

public class ClickObject2 implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        int objectID = client.getInputStream().readUnsignedWordBigEndianA(); // 5292
        int objectY = client.getInputStream().readSignedWordBigEndian();
        int objectX = client.getInputStream().readUnsignedWordA();

        final WalkToTask task = new WalkToTask(WalkToTask.Action.OBJECT_SECOND_CLICK, objectID,
                new Position(objectX, objectY));
        GameObjectDef def = Misc.getObject(objectID, objectX, objectY, client.getPosition().getZ());
        GameObjectData object = GameObjectData.forId(task.getWalkToId());
        client.setWalkToTask(task);
        if (getGameWorldId() > 1 && object != null)
            client.send(new SendMessage("Obj click2: " + object.getId() + ", " + object.getName() + ", Coord: " + objectX + ", " + objectY + ", " + (def == null ? "Def is null!" : def.getFace())));
        if (objectID == 14896 || objectID == 14909) {
            client.addItem(1779, 1);
        }
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
                clickObject2(client, task.getWalkToId(), task.getWalkToPosition(), object);
                client.setWalkToTask(null);
                this.stop();
            }

        });
    }

    public void clickObject2(Client client, int objectID, Position position, GameObjectData obj) {
        //System.out.println("atObject2: " + position.getX() + "," + position.getY() + " objectID: " + objectID);
        if (client.adding) {
            client.objects.add(new RS2Object(objectID, position.getX(), position.getY(), 2));
        }
        if (getGameWorldId() > 0) {
            client.println_debug("atObject2: " + position.getX() + "," + position.getY() + " objectID: " + objectID);
        }
        if (System.currentTimeMillis() < client.walkBlock) {
            return;
        }
        client.setFocus(position.getX(), position.getY());
        String objectName = obj == null ? "" : obj.getName().toLowerCase();
        switch (objectID) {
            case 20873: //Cage
            case 11729:
            case 11730:
            case 11731:
            case 11732:
            case 11733:
            case 11734:
                Thieving.attemptSteal(client, objectID, position);
                break;
            case 378: //Empty chest
                client.send(new SendMessage("this chest is empty!"));
                break;
        }
		/*if (objectID == 2564) {
			client.skillX = position.getX();
			client.setSkillY(position.getY());
			client.WanneThieve = 2564;
		}
		if (objectID == 2563) {
			client.skillX = position.getX();
			client.setSkillY(position.getY());
			client.WanneThieve = 2563;
		}
		if (objectID == 2565) {
			client.skillX = position.getX();
			client.setSkillY(position.getY());
			client.WanneThieve = 2565;
		}
		//if (objectID == 2561) {
		//	client.skillX = position.getX();
		//	client.setSkillY(position.getY());
		//	client.WanneThieve = 2561;
		//}
		if (objectID == 2560) {
			client.skillX = position.getX();
			client.setSkillY(position.getY());
			client.WanneThieve = 2560;
		}*/
        if (objectID == 4877) {
            client.skillX = position.getX();
            client.setSkillY(position.getY());
            client.WanneThieve = 4877;
        }
        if (objectID == 3994 || objectID == 11666 || objectID == 16469 || objectID == 29662) { //Gold craft
            client.showItemsGold();
            client.showInterface(4161);
        }
		/*if (objectID == 2562) {
			client.skillX = position.getX();
			client.setSkillY(position.getY());
			client.WanneThieve = 2562;
		}*/
        if (objectID == 25824 || objectID == 14889) {
            client.spinning = true;
            client.getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
        }
        if (objectID == 823) {
            Random r = new Random();
            client.moveTo(2602 + r.nextInt(5), 3162 + r.nextInt(5), client.getPosition().getZ());
        }
        if ((objectID == 2213) || (objectID == 2214) || (objectID == 3045) || (objectID == 5276)
                || (objectID == 6084) || objectName.contains("bank booth")) {
            //System.out.println("Banking..");
            client.skillX = position.getX();
            client.setSkillY(position.getY());
            client.WanneBank = 1;
            client.WanneShop = -1;
        }
    }

}
