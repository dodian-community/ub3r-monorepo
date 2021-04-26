package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.Config;
import net.dodian.cache.object.GameObjectData;
import net.dodian.cache.object.GameObjectDef;
import net.dodian.uber.game.Constants;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.event.Event;
import net.dodian.uber.game.event.EventManager;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.UpdateFlag;
import net.dodian.uber.game.model.WalkToTask;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.uber.game.model.item.Equipment;
import net.dodian.uber.game.model.object.DoorHandler;
import net.dodian.uber.game.model.object.GlobalObject;
import net.dodian.uber.game.model.object.Object;
import net.dodian.uber.game.model.object.RS2Object;
import net.dodian.uber.game.model.player.packets.Packet;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.uber.game.model.player.packets.outgoing.Sound;
import net.dodian.uber.game.model.player.skills.Agility;
import net.dodian.uber.game.model.player.skills.Skill;
import net.dodian.uber.game.model.player.skills.Thieving;
import net.dodian.uber.game.party.Balloons;
import net.dodian.utilities.Misc;
import net.dodian.utilities.Utils;

public class ClickObject implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        int objectX = client.getInputStream().readSignedWordBigEndianA();
        int objectID = client.getInputStream().readUnsignedWord();
        int objectY = client.getInputStream().readUnsignedWordA();

        final WalkToTask task = new WalkToTask(WalkToTask.Action.OBJECT_FIRST_CLICK, objectID,
                new Position(objectX, objectY));
        GameObjectDef def = Misc.getObject(objectID, objectX, objectY, client.getPosition().getZ());
        GameObjectData object = GameObjectData.forId(task.getWalkToId());
        client.setWalkToTask(task);
        if (Config.getWorldId() > 1 && object != null)
            client.send(new SendMessage("Obj click1: " + object.getId() + ", " + object.getName() + ", Coord: " + objectX + ", " + objectY + ", " + (def == null ? "Def is null!" : def.getFace())));
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
                Object o = new Object(objectID, task.getWalkToPosition().getX(), task.getWalkToPosition().getY(), task.getWalkToPosition().getZ(), 10);
                if (def != null && !GlobalObject.hasGlobalObject(o)) {
                    objectPosition = Misc.goodDistanceObject(task.getWalkToPosition().getX(), task.getWalkToPosition().getY(), client.getPosition().getX(), client.getPosition().getY(), object.getSizeX(def.getFace()), object.getSizeY(def.getFace()), client.getPosition().getZ());
                } else {
                    if (GlobalObject.hasGlobalObject(o)) {
                        objectPosition = Misc.goodDistanceObject(task.getWalkToPosition().getX(), task.getWalkToPosition().getY(), client.getPosition().getX(), client.getPosition().getY(), object.getSizeX(o.face), object.getSizeY(o.type), o.z);
                    } else if (object != null) {
                        objectPosition = Misc.goodDistanceObject(task.getWalkToPosition().getX(), task.getWalkToPosition().getY(), client.getPosition().getX(), client.getPosition().getY(), object.getSizeX(), object.getSizeY(), client.getPosition().getZ());
                    }
                }
                if (objectID == 23131)
                    objectPosition = Misc.goodDistanceObject(task.getWalkToPosition().getX(), 3552, client.getPosition().getX(), client.getPosition().getY(), object.getSizeX(), object.getSizeY(), client.getPosition().getZ());
                if (objectPosition == null)
                    return;
                atObject(client, task.getWalkToId(), task.getWalkToPosition(), object);
                client.setWalkToTask(null);
                this.stop();
            }
        });
    }

    public void atObject(Client client, int objectID, Position objectPosition, GameObjectData obj) {
        String objectName = obj == null ? "" : obj.getName().toLowerCase();
        if (!client.validClient || client.randomed) {
            return;
        }
        int xDiff = Math.abs(client.getPosition().getX() - objectPosition.getX());
        int yDiff = Math.abs(client.getPosition().getY() - objectPosition.getY());
        if (client.adding) {
            client.objects.add(new RS2Object(objectID, objectPosition.getX(), objectPosition.getY(), 1));
        }
        if (System.currentTimeMillis() < client.walkBlock) {
            return;
        }
        client.resetAction(false);
        client.setFocus(objectPosition.getX(), objectPosition.getY());
        client.getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
        if (xDiff > 5 || yDiff > 5) {
            return;
        }
        if (Balloons.lootBalloon(client, objectPosition.copy()) && objectID >= 115 && objectID <= 122) {
            return;
        }
        if (objectID == 26193) {
            Balloons.openInterface(client);
            //client.resetItems(5064);
            //Balloons.displayItems(client);
            return;
        }
        if (objectID == 26194 && client.playerRights > 1) {
            Balloons.triggerPartyEvent(client);
            return;
        }
        if (objectID == 23271) {
            client.teleportToX = objectPosition.getX();
            client.teleportToY = objectPosition.getY() + (client.getPosition().getY() == 3523 ? -1 : 2);
        }
        if ((objectID == 6451 && client.getPosition().getY() == 9375) || (objectID == 6452 && client.getPosition().getY() == 9376)) {
            if (client.getPosition().getX() == 3305) {
                Agility agi = new Agility(client);
                agi.kbdEntrance();
            } else
                client.NpcDialogue = 536;
            return;
        }
        if (objectID == 20873) {
            Thieving.attemptSteal(client, objectID, objectPosition);
        }
        if (objectID == 2391 || objectID == 2392) {
            if (client.premium) {
                client.ReplaceObject(2728, 3349, 2391, 0, 0);
                client.ReplaceObject(2729, 3349, 2392, -2, 0);
            }
            return;
        }
        if (objectID == 2097) {
            int type = -1;
            int[] possibleBars = {2349, 2351, 2353, 2359, 2361, 2363};
            for (int i = 0; i < possibleBars.length; i++)
                if (client.contains(possibleBars[i]))
                    type = possibleBars[i];
            if (type != -1 && client.CheckSmithing(type) != -1) {
                client.skillX = objectPosition.getX();
                client.setSkillY(objectPosition.getY());
                client.OpenSmithingFrame(client.CheckSmithing(type));
            } else if (type == -1)
                client.send(new SendMessage("You do not have any bars to smith!"));
        }
        if (objectID == 1294) {
            client.teleportToX = 2485;
            client.teleportToY = 9912;
            client.newheightLevel = 0;
        }
        if (objectID == 17384 && objectPosition.getX() == 2892 && objectPosition.getY() == 3507) {
            client.teleportToX = 2893;
            client.teleportToY = 3507 + 6400;
            client.newheightLevel = 0;
        }
        if (objectID == 17384 && objectPosition.getX() == 2677 && objectPosition.getY() == 3405) {
            client.teleportToX = 2677;
            client.teleportToY = 9806;
            client.newheightLevel = 0;
        }
        if (objectID == 17385 && objectPosition.getX() == 2677 && objectPosition.getY() == 9805) {
            client.teleportToX = 2677;
            client.teleportToY = 3404;
            client.newheightLevel = 0;
        }
        if (objectID == 17387 && objectPosition.getX() == 2892 && objectPosition.getY() == 9907) {
            client.teleportToX = 2893;
            client.teleportToY = 3507;
            client.newheightLevel = 0;
        }
        if (objectID == 882 && objectPosition.getX() == 2899 && objectPosition.getY() == 9728) {
            if (client.getLevel(Skill.AGILITY) < 85) {
                client.send(new SendMessage("You need level 85 agility to use this shortcut!"));
                return;
            }
            client.teleportToX = 2885;
            client.teleportToY = 9795;
            client.newheightLevel = 0;
        }
        if (objectID == 882 && objectPosition.getX() == 2885 && objectPosition.getY() == 9794) {
            if (client.getLevel(Skill.AGILITY) < 85) {
                client.send(new SendMessage("You need level 85 agility to use this shortcut!"));
                return;
            }
            client.teleportToX = 2899;
            client.teleportToY = 9729;
            client.newheightLevel = 0;
        }
        if (objectID == 16509) {
            if (!client.checkItem(989) || client.getLevel(Skill.AGILITY) < 70) {
                client.send(new SendMessage("You need a crystal key and 70 agility to use this shortcut!"));
                return;
            }
            if (client.getPosition().getX() == 2886 && client.getPosition().getY() == 9799) {
                client.teleportToX = 2892;
                client.teleportToY = 9799;
            } else if (client.getPosition().getX() == 2892 && client.getPosition().getY() == 9799) {
                client.teleportToX = 2886;
                client.teleportToY = 9799;
            }
        }
        if (objectID == 16510) {
            if (!client.checkItem(989) || client.getLevel(Skill.AGILITY) < 70) {
                client.send(new SendMessage("You need a crystal key and 70 agility to use this shortcut!"));
                return;
            }
            if (client.getPosition().getX() == 2880 && client.getPosition().getY() == 9813) {
                client.teleportToX = 2878;
                client.teleportToY = 9813;
            } else if (client.getPosition().getX() == 2878 && client.getPosition().getY() == 9813) {
                client.teleportToX = 2880;
                client.teleportToY = 9813;
            }
        }
        if (objectID == 6847) {
            Thieving.attemptSteal(client, objectID, objectPosition);
            // 	client.addItem(4084, 1);
        }
        if (objectID == 133) { // new dragon teleport?
            client.teleportToX = 3235;
            client.teleportToY = 9366;
            client.send(new SendMessage("Welcome to the dragon lair!"));
        }
        if (objectID == 3994 || objectID == 11666) {
            for (int fi = 0; fi < Utils.smelt_frame.length; fi++) {
                client.sendFrame246(Utils.smelt_frame[fi], 150, Utils.smelt_bars[fi][0]);
            }
            client.sendFrame164(2400);
        }
        if (objectID == 2309 && objectPosition.getX() == 2998 && objectPosition.getY() == 3917) {
            if (client.getLevel(Skill.AGILITY) < 75) {
                client.send(new SendMessage("You need at least 75 agility to enter!"));
                return;
            }
            client.ReplaceObject(2998, 3917, 2309, 2, 0);
            return;
        }
        if (objectID == 2624 || objectID == 2625) { //Heroes dungeon for runite rock.
            client.ReplaceObject(2901, 3510, 2624, -1, 0);
            client.ReplaceObject(2901, 3511, 2625, -3, 0);
            client.ReplaceObject(2902, 3510, -1, -1, 0);
            client.ReplaceObject(2902, 3511, -1, -3, 0);
            return;
        }
        if (objectID == 1516 && objectPosition.getX() == 2908 && objectPosition.getY() == 9698) {
            if (!client.checkItem(989)) {
                client.send(new SendMessage("You need a crystal key to open this door."));
                return;
            }
            if (client.getLevel(Skill.SLAYER) < 90) {
                client.send(new SendMessage("You need at least 90 slayer to enter!"));
                return;
            }
            client.ReplaceObject(2908, 9698, -1, 0, 0);
            client.ReplaceObject(2907, 9698, -1, 0, 0);
            client.ReplaceObject(2908, 9697, 1516, 2, 0);
            client.ReplaceObject(2907, 9697, 1519, 0, 0);
            return;
        }
        if (objectID == 1519 && objectPosition.getX() == 2907 && objectPosition.getY() == 9698) {
            if (!client.checkItem(989)) {
                client.send(new SendMessage("You need a crystal key to open this door."));
                return;
            }
            if (client.getLevel(Skill.SLAYER) < 90) {
                client.send(new SendMessage("You need at least 90 slayer to enter!"));
                return;
            }
            client.ReplaceObject(2908, 9698, -1, 0, 0);
            client.ReplaceObject(2907, 9698, -1, 0, 0);
            client.ReplaceObject(2908, 9697, 1516, 2, 0);
            client.ReplaceObject(2907, 9697, 1519, 0, 0);
            return;
        }
        if (objectID == 2623) {
            if (client.checkItem(989)) {
                client.ReplaceObject(2924, 9803, 2623, -3, 0);
            } else {
                client.send(new SendMessage("You need the crystal key to enter"));
                client.send(new SendMessage("The crystal key is made from 2 crystal pieces"));
            }
        }
        if (objectID == 16680 && objectPosition.getX() == 2884 && objectPosition.getY() == 3397) {
            if (client.getLevel(Skill.SLAYER) >= 50) {
                client.teleportToX = 2884;
                client.teleportToY = 9798;
            } else {
                client.send(new SendMessage("You need 50 slayer to enter the Taverly Dungeon"));
            }
        }
        if (objectID == 17385 && objectPosition.getX() == 2884 && objectPosition.getY() == 9797) {
            client.teleportToX = 2884;
            client.teleportToY = 3398;
        }
        if (objectID == 25939 && objectPosition.getX() == 2715 && objectPosition.getY() == 3470) {
            client.teleportToX = 2715;
            client.teleportToY = 3471;
            client.getPosition().setZ(0);
        }
        if (objectID == 25938 && objectPosition.getX() == 2715 && objectPosition.getY() == 3470) {
            client.teleportToX = 2714;
            client.teleportToY = 3470;
            client.getPosition().setZ(1);
        }
        if (objectID == 16683 && objectPosition.getX() == 2597 && objectPosition.getY() == 3107) {
            client.teleportToX = 2597;
            client.teleportToY = 3106;
            client.getPosition().setZ(1);
        }
        if (objectID == 16681 && objectPosition.getX() == 2597 && objectPosition.getY() == 3107) {
            client.teleportToX = 2597;
            client.teleportToY = 3106;
            client.getPosition().setZ(0);
        }
        if (objectID == 410 && objectPosition.getX() == 2925 && objectPosition.getY() == 3483) { //Guthix altar to cosmic
            client.requestAnim(645, 0);
            client.triggerTele(2162, 4833, 0, false);
            return;
        }
        if (objectID == 14847) {
            client.requestAnim(645, 0);
            client.triggerTele(2924, 3483, 0, false);
            return;
        }
        if (objectID == 1725) {
            client.stairs = "legendsUp".hashCode();
            client.skillX = objectPosition.getX();
            client.setSkillY(objectPosition.getY());
            client.stairDistance = 1;
        }
        if (objectID == 1725 && objectPosition.getX() == 2732 && objectPosition.getY() == 3377) {
            if (Utils.getDistance(client.getPosition().getX(), client.getPosition().getY(), objectPosition.getX(),
                    objectPosition.getY()) > 2) {
                return;
            }
            if (client.premium) {
                client.teleportToX = 2732;
                client.teleportToY = 3380;
                client.getPosition().setZ(1);
            }
        }
        if (objectID == 1726 && objectPosition.getX() == 2732 && objectPosition.getY() == 3378) {
            if (Utils.getDistance(client.getPosition().getX(), client.getPosition().getY(), objectPosition.getX(),
                    objectPosition.getY()) > 2) {
                return;
            }
            if (client.premium) {
                client.teleportToX = 2732;
                client.teleportToY = 3376;
                client.getPosition().setZ(0);
            }
        }
        if (objectID == 1726) {
            client.stairs = "legendsDown".hashCode();
            client.skillX = objectPosition.getX();
            client.setSkillY(objectPosition.getY());
            client.stairDistance = 1;
        }
        /* Agility */
        Agility agility = new Agility(client);
        if (objectID == 23145) {
            agility.GnomeLog();
            return;
        } else if (objectID == 23134 && client.distanceToPoint(objectPosition.getX(), objectPosition.getY()) < 2) {
            agility.GnomeNet1();
            return;
        } else if (objectID == 23559) {
            agility.GnomeTree1();
            return;
        } else if (objectID == 23557) {
            agility.GnomeRope();
            return;
        } else if (objectID == 23560) {
            agility.GnomeTreebranch2();
            return;
        } else if (objectID == 23135 && client.distanceToPoint(objectPosition.getX(), objectPosition.getY()) < 3) {
            agility.GnomeNet2();
            return;
        } else if (objectID == 23138 && client.getPosition().getX() == 2484 && client.getPosition().getY() == 3430 && client.distanceToPoint(objectPosition.getX(), objectPosition.getY()) < 2) {
            agility.GnomePipe();
            return;
        } else if (objectID == 23139 && client.getPosition().getX() == 2487 && client.getPosition().getY() == 3430 && client.distanceToPoint(objectPosition.getX(), objectPosition.getY()) < 2) {
            agility.GnomePipe();
            return;
        } else if (objectID == 23137) {
            agility.WildyPipe();
            return;
        } else if (objectID == 23132) {
            agility.WildyRope();
            return;
        } else if (objectID == 23556) {
            agility.WildyStones();
            return;
        } else if (objectID == 23542) {
            agility.WildyLog();
            return;
        } else if (objectID == 23640) {
            agility.WildyClimb();
            return;
        } else if (objectID == 23131) {
            agility.BarbRope();
            return;
        } else if (objectID == 23144) {
            agility.BarbLog();
            return;
        } else if (objectID == 20211) {
            agility.BarbNet();
            return;
        } else if (objectID == 23547) {
            agility.BarbLedge();
            return;
        } else if (objectID == 16682) {
            agility.BarbStairs();
            return;
        } else if (objectID == 1948 && objectPosition.getX() == 2536 && objectPosition.getY() == 3553) {
            agility.BarbFirstWall();
            return;
        } else if (objectID == 1948 && objectPosition.getX() == 2539 && objectPosition.getY() == 3553) {
            agility.BarbSecondWall();
            return;
        } else if (objectID == 1948 && objectPosition.getX() == 2542 && objectPosition.getY() == 3553) {
            agility.BarbFinishWall();
            return;
        } else if (objectID == 23567) {
            agility.orangeBar();
        } else if (objectID == 23548) {
            agility.yellowLedge();
        } else
            agility = null;
        if (objectID == 1558 || objectID == 1557 && client.distanceToPoint(2758, 3482) < 5 && client.playerRights > 0) {
            client.ReplaceObject(2758, 3482, 1558, -2, 0);
            client.ReplaceObject(2757, 3482, 1557, 0, 0);
            client.send(new SendMessage("Welcome to the Castle"));
        }
        if (objectID == 2104) {
            objectID = 2105;
        }
        if (objectID == 2102) {
            objectID = 2103;
        }
        if (objectID == 14859 && objectID == 14860) {
            return;
        }
        for (int r = 0; r < Utils.rocks.length; r++) {
            if (objectID == Utils.rocks[r]) {
                if (client.getLevel(Skill.MINING) < Utils.rockLevels[r]) {
                    client.send(new SendMessage("You need a mining level of " + Utils.rockLevels[r] + " to mine this rock"));
                    return;
                }
                boolean hasPick = false;
                int pickaxe = -1;
                pickaxe = client.findPick();
                if (pickaxe < 0) {
                    client.minePick = -1;
                    client.resetAction();
                    client.send(new SendMessage("You do not have an pickaxe that you can use."));
                    return;
                } else {
                    client.minePick = pickaxe;
                    hasPick = true;
                }
                if (hasPick) {
                    client.mineIndex = r;
                    client.mining = true;
                    client.lastAction = System.currentTimeMillis() + client.getMiningSpeed();
                    client.lastPickAction = System.currentTimeMillis() + 1200;
                    client.requestAnim(client.getMiningEmote(Utils.picks[pickaxe]), 0);
                    client.send(new SendMessage("You swing your pick at the rock..."));
                } else {
                    client.resetAction();
                    client.send(new SendMessage("You need a pickaxe to mine this rock"));
                }
                return;
            }
        }
        if (client.mining) {
            return;
        }
        if (objectID == 2634 && objectPosition.getX() == 2838 && objectPosition.getY() == 3517) { //2838, 3517
            client.teleportToX = 2840;
            client.teleportToY = 3517;
            client.send(new SendMessage("You jump to the other side of the rubble"));
//      if (client.getLevel(Skill.MINING) < 40) {
//        client.send(new SendMessage("You need 40 mining to clear this rubble"));
//        return;
//      }
//      client.requestAnim(client.getMiningEmote(624), 0);
//      client.animationReset = System.currentTimeMillis() + 2000;
//      client.ReplaceObject2(2838, 3517, -1, -1, 11);
//      client.send(new SendMessage("You clear the rubble"));
        }
        if (objectID == 16680) {
            int[] x = {2845, 2848, 2848};
            int[] y = {3516, 3513, 3519};
            for (int c = 0; c < x.length; c++) {
                if (objectPosition.getX() == x[c] && objectPosition.getY() == y[c]) {
                    client.teleportToX = 2868;
                    client.teleportToY = 9945;
                }
            }
        }
        if (objectID == 2107) {
            if (System.currentTimeMillis() - Server.lastRunite < 60000) {
                client.println("invalid timer");
                return;
            }
        }
        if (objectID == 2492) {
            client.teleportToX = 2591;
            client.teleportToY = 3087;
            client.getPosition().setZ(0);
            return;
        }
        if (objectID == 14905) {
            client.runecraft(561, 1, 60);
            return;
        }
        if (objectID == 27978) {
            client.runecraft(565, 50, 85);
            return;
        }
        if (objectID == 14903) {
            client.runecraft(564, 75, 120);
            return;
        }
        if (objectID == 2158 || objectID == 2156) {
            client.triggerTele(2921, 4844, 0, false);
            return;
        }
        if (objectID == 733) {
            int chance = Misc.chance(100);
            if (chance <= 50) {
                client.send(new SendMessage("You failed to cut the web!"));
                return;
            }
            if (System.currentTimeMillis() - client.lastAction < 2000) {
                client.lastAction = System.currentTimeMillis();
                return;
            }
            final Object emptyObj = new Object(734, objectPosition.getX(), objectPosition.getY(), client.getPosition().getZ(), 10, 1, objectID);
            if (!GlobalObject.addGlobalObject(emptyObj, 30000)) {
                return;
            }
            client.lastAction = System.currentTimeMillis();
            return;
        }
        if (objectID == 16520 || objectID == 16519) {
            if (client.getLevel(Skill.AGILITY) < 50) {
                client.send(new SendMessage("You need level 50 agility to use this shortcut!"));
                return;
            }
            if (objectPosition.getX() == 2575 && objectPosition.getY() == 3108) {
                client.teleportToX = 2575;
                client.teleportToY = 3112;
                client.getPosition().setZ(0);
            } else if (objectPosition.getX() == 2575 && objectPosition.getY() == 3111) {
                client.teleportToX = 2575;
                client.teleportToY = 3107;
                client.getPosition().setZ(0);
            }
            return;
        }
        if (objectID == 16664 && objectPosition.getX() == 2724 && objectPosition.getY() == 3374) {
            if (!client.premium) {
                client.resetPos();
            }
            client.teleportToX = 2727;
            client.teleportToY = 9774;
            client.getPosition().setZ(0);
            return;
        }
        if (objectID == 2833) {
            if (objectPosition.getX() == 2544 && objectPosition.getY() == 3111) {
                client.teleportToX = 2544;
                client.teleportToY = 3112;
                client.getPosition().setZ(1);
            }
            return;
        }
        if (objectID == 12260) {
            if (client.playerRights < 1) {
                return;
            }
            if (objectPosition.getX() == 2542 && objectPosition.getY() == 3097) {
                client.teleportToX = 2462;
                client.teleportToY = 4359;
                client.getPosition().setZ(0);
            } else if (objectPosition.getX() == 2459 && objectPosition.getY() == 4354) {
                client.teleportToX = 2544;
                client.teleportToY = 3096;
                client.getPosition().setZ(0);
            }
            return;
        }
        if (objectID == 17122) {
            if (objectPosition.getX() == 2544 && objectPosition.getY() == 3111) {
                client.teleportToX = 2544;
                client.teleportToY = 3112;
                client.getPosition().setZ(0);
            }
            return;
        }
        if (objectID == 2796) {
            if (objectPosition.getX() == 2549 && objectPosition.getY() == 3111) {
                client.teleportToX = 2549;
                client.teleportToY = 3112;
                client.getPosition().setZ(2);
            }
            return;
        }
        if (objectID == 2797) {
            if (objectPosition.getX() == 2549 && objectPosition.getY() == 3111) {
                client.teleportToX = 2549;
                client.teleportToY = 3112;
                client.getPosition().setZ(1);
            }
            return;
        }
        if (objectID == 17384) {
            if (objectPosition.getX() == 2594 && objectPosition.getY() == 3085) {
                client.teleportToX = 2594;
                client.teleportToY = 9486;
                client.getPosition().setZ(0);
            }
            return;
        }
        if (objectID == 17385) {
            if (objectPosition.getX() == 2594 && objectPosition.getY() == 9485) {
                client.teleportToX = 2594;
                client.teleportToY = 3086;
                client.getPosition().setZ(0);
            }
            return;
        }
        if (objectID == 16665) {
            if (objectPosition.getX() == 2724 && objectPosition.getY() == 9774) {
                if (!client.premium) {
                    client.resetPos();
                }
                client.teleportToX = 2723;
                client.teleportToY = 3375;
                client.getPosition().setZ(0);
            } else if (objectPosition.getX() == 2603 && objectPosition.getY() == 9478) {
                client.teleportToX = 2606;
                client.teleportToY = 3079;
                client.getPosition().setZ(0);
            } else if (objectPosition.getX() == 2569 && objectPosition.getY() == 9522) {
                client.teleportToX = 2570;
                client.teleportToY = 3121;
                client.getPosition().setZ(0);
            }
            return;
        }

//    if (objectID == 377 && client.playerHasItem(605)) {
//      {
//
//        double roll = Math.random() * 100;
//        if (roll < 100) {
//          int[] items = { 4708, 4710, 4712, 4714, 4716, 4718, 4720, 4722, 4724, 4726, 4728, 4730, 4732, 4734, 4736,
//              4738, 4745, 4747, 4749, 4751, 4753, 4755, 4757, 4759 };
//          int r = (int) (Math.random() * items.length);
//          client.send(new SendMessage("You have recieved a " + client.GetItemName(items[r]) + "!"));
//          client.addItem(items[r], 1);
//          client.deleteItem(605, 1);
//          client.yell("[Server] - " + client.getPlayerName() + " has just received from the BKT chest a  "
//              + client.GetItemName(items[r]));
//        } else {
//          int coins = Utils.random(7000);
//          client.send(new SendMessage("You find " + coins + " coins inside the BKT chest"));
//          client.addItem(995, coins);
//        }
//
//      }
//      for (int p = 0; p < Constants.maxPlayers; p++) {
//        Client player = (Client) PlayerHandler.players[p];
//        if (player == null) {
//          continue;
//        }
//        if (player.getPlayerName() != null && player.getPosition().getZ() == client.getPosition().getZ()
//            && !player.disconnected && Math.abs(player.getPosition().getY() - client.getPosition().getY()) < 30
//            && Math.abs(player.getPosition().getX() - client.getPosition().getX()) < 30 && player.dbId > 0) {
//          player.stillgfx(444, objectPosition.getY(), objectPosition.getX());
//        }
//      }
//    }
        if (objectID == 375 && objectPosition.getX() == 2593 && objectPosition.getY() == 3108 && client.getPosition().getZ() == 1) {
            if (client.getLevel(Skill.THIEVING) < 70) {
                client.send(new SendMessage("You must be level 70 thieving to open this chest"));
                return;
            }
            if (client.freeSlots() < 1) {
                client.send(new SendMessage("You need atleast one free inventory slot!"));
                return;
            }
            if (System.currentTimeMillis() - client.lastAction < 2000) {
                client.lastAction = System.currentTimeMillis();
                return;
            }
            final Object emptyObj = new Object(378, objectPosition.getX(), objectPosition.getY(), client.getPosition().getZ(), 10, 2, objectID);
            if (!GlobalObject.addGlobalObject(emptyObj, 20000)) {
                return;
            }
            client.lastAction = System.currentTimeMillis();
            double roll = Math.random() * 100;
            if (roll <= 0.3) {
                int[] items = {2577, 2579, 2631};
                int r = (int) (Math.random() * items.length);
                client.send(new SendMessage("You have recieved a " + client.GetItemName(items[r]) + "!"));
                client.addItem(items[r], 1);
                client.yell("[Server] - " + client.getPlayerName() + " has just received from the chest a  "
                        + client.GetItemName(items[r]));
            } else {
                int coins = 1200 + Utils.random(5000);
                client.send(new SendMessage("You find " + coins + " coins inside the chest"));
                client.addItem(995, coins);
            }
            if (client.getEquipment()[Equipment.Slot.HEAD.getId()] == 2631)
                client.giveExperience(150, Skill.THIEVING);
            client.stillgfx(444, objectPosition.getY(), objectPosition.getX());
            client.triggerRandom(10000);
        }
        if (objectID == 6420 && objectPosition.getX() == 2733 && objectPosition.getY() == 3374) {
            if (!client.premium) {
                client.resetPos();
                return;
            }
            if (client.getLevel(Skill.THIEVING) < 85) {
                client.send(new SendMessage("You must be level 85 thieving to open this chest"));
                return;
            }
            if (client.freeSlots() < 1) {
                client.send(new SendMessage("You need atleast one free inventory slot!"));
                return;
            }
            if (System.currentTimeMillis() - client.lastAction < 2000) {
                client.lastAction = System.currentTimeMillis();
                return;
            }
            final Object o = new Object(6421, objectPosition.getX(), objectPosition.getY(), objectPosition.getZ(), 11, -1, objectID);
            if (!GlobalObject.addGlobalObject(o, 20000)) {
                return;
            }
            client.lastAction = System.currentTimeMillis();
            double roll = Math.random() * 100;
            if (roll <= 0.3) {
                int[] items = {1050, 2581, 2631};
                int r = (int) (Math.random() * items.length);
                client.send(new SendMessage("You have recieved a " + client.GetItemName(items[r]) + "!"));
                client.addItem(items[r], 1);
                client.yell("[Server] - " + client.getPlayerName() + " has just received from the premium chest a  "
                        + client.GetItemName(items[r]));
            } else {
                int coins = 3000 + Utils.random(9000);
                client.send(new SendMessage("You find " + coins + " coins inside the chest"));
                client.addItem(995, coins);
            }
            if (client.getEquipment()[Equipment.Slot.HEAD.getId()] == 2631)
                client.giveExperience(300, Skill.THIEVING);
            client.stillgfx(444, objectPosition.getY(), objectPosition.getX());
            client.triggerRandom(15000);
        }
        if (System.currentTimeMillis() - client.lastDoor > 1000) {
            client.lastDoor = System.currentTimeMillis();
            for (int d = 0; d < DoorHandler.doorX.length; d++) {
                if (objectID == DoorHandler.doorId[d] && objectPosition.getX() == DoorHandler.doorX[d]
                        && objectPosition.getY() == DoorHandler.doorY[d]) {
                    int newFace = -3;
                    if (DoorHandler.doorState[d] == 0) { // closed
                        newFace = DoorHandler.doorFaceOpen[d];
                        DoorHandler.doorState[d] = 1;
                        DoorHandler.doorFace[d] = newFace;
                    } else {
                        newFace = DoorHandler.doorFaceClosed[d];
                        DoorHandler.doorState[d] = 0;
                        DoorHandler.doorFace[d] = newFace;
                    }
                    client.send(new Sound(326));
                    for (int p = 0; p < Constants.maxPlayers; p++) {
                        Client player = (Client) PlayerHandler.players[p];
                        if (player == null) {
                            continue;
                        }
                        if (player.getPlayerName() != null && player.getPosition().getZ() == client.getPosition().getZ()
                                && !player.disconnected && player.getPosition().getY() > 0 && player.getPosition().getX() > 0
                                && player.dbId > 0) {
                            player.ReplaceObject(DoorHandler.doorX[d], DoorHandler.doorY[d], DoorHandler.doorId[d], newFace, 0);
                        }
                    }
                }
            }
        }
        if (objectID == 23140) {
            if (!client.checkItem(1544)) {
                client.send(new SendMessage("You need a orange key to use this pipe!"));
                return;
            }
            if (objectPosition.getX() == 2576 && objectPosition.getY() == 9506) {
                client.teleportToX = 2572;
                client.teleportToY = 9506;
            } else if (objectPosition.getX() == 2573 && objectPosition.getY() == 9506) {
                client.teleportToX = 2578;
                client.teleportToY = 9506;
            }
        }
        if (objectID == 23564) {
            client.teleportToX = 2621;
            client.teleportToY = 9496;
        }
        if (objectID == 15656) {
            client.teleportToX = 2614;
            client.teleportToY = 9505;
        }
        //if (objectID == 6836) {
        //  client.skillX = objectPosition.getX();
        //  client.setSkillY(objectPosition.getY());
        //  client.WanneThieve = 6836;
        // }
        if (objectID == 881) {
            client.getPosition().setZ(client.getPosition().getZ() - 1);
        }
        if (objectID == 1591 && objectPosition.getX() == 3268 && objectPosition.getY() == 3435) {
            if (client.determineCombatLevel() >= 80) {
                client.teleportToX = 2540;
                client.teleportToY = 4716;
            } else {
                client.send(new SendMessage("You need to be level 80 or above to enter the mage arena."));
                client.send(new SendMessage("The skeletons at the varrock castle are a good place until then."));
            }
        }
        if (objectID == 5960 && objectPosition.getX() == 2539 && objectPosition.getY() == 4712) {
            client.teleportToX = 3105;
            client.teleportToY = 3933;
        }

        // Wo0t Tzhaar Objects

        if (objectID == 9369 && (objectPosition.getX() == 2399) && (objectPosition.getY() == 5176)) {
            if (client.getPosition().getY() == 5177) {
                client.teleportToX = 2399;
                client.teleportToY = 5175;

            }
        }
        if (objectID == 9369 && (objectPosition.getX() == 2399) && (objectPosition.getY() == 5176)) {
            if (client.getPosition().getY() == 5175) {
                client.teleportToX = 2399;
                client.teleportToY = 5177;

            }
        }

        if (objectID == 9368 && (objectPosition.getX() == 2399) && (objectPosition.getY() == 5168)) {
            if (client.getPosition().getY() == 5169) {
                client.teleportToX = 2399;
                client.teleportToY = 5167;

            }
        }
        if (objectID == 9368 && (objectPosition.getX() == 2399) && (objectPosition.getY() == 5168)) {
            if (client.getPosition().getY() == 5167) {
                client.teleportToX = 2399;
                client.teleportToY = 5169;

            }
        }
        if (objectID == 9391 && (objectPosition.getX() == 2399) && (objectPosition.getY() == 5172)) // Tzhaar
        // Fight
        // bank
        {
            client.openUpBank();
        }
        if (objectName.contains("bank booth"))
            client.openUpBank();
        if (objectID == 9356 && (objectPosition.getX() == 2437) && (objectPosition.getY() == 5166)) // Tzhaar
        // Jad
        // Cave
        // Enterance
        {
            client.teleportToX = 2413;
            client.teleportToY = 5117;
            client.send(new SendMessage("You have entered the Jad Cave."));
        }
        if (objectID == 9357 && (objectPosition.getX() == 2412) && (objectPosition.getY() == 5118)) // Tzhaar
        // Jad
        // Cave
        // Exit
        {
            client.teleportToX = 2438;
            client.teleportToY = 5168;
            client.send(new SendMessage("You have left the Jad Cave."));
        }

        // End of Tzhaar Objects

        if ((objectID == 2213) || (objectID == 2214) || (objectID == 3045) || (objectID == 5276)
                || (objectID == 6084)) {
            //System.out.println("Banking..");
            client.skillX = objectPosition.getX();
            client.setSkillY(objectPosition.getY());
            client.WanneBank = 1;
            client.WanneShop = -1;
        }
        // woodCutting
        // mining
        // if (actionTimer == 0) {
        if (client.CheckObjectSkill(objectID, objectName) == true) {
            client.skillX = objectPosition.getX();
            client.setSkillY(objectPosition.getY());
        }
        // }
        // go upstairs
        if (true) {
            if (objectID == 1747) {
                client.stairs = 1;
                client.skillX = objectPosition.getX();
                client.setSkillY(objectPosition.getY());
                client.stairDistance = 1;
            } else if (objectID == 1738) {
                client.stairs = 1;
                client.skillX = objectPosition.getX();
                client.setSkillY(objectPosition.getY());
                client.stairDistance = 2;
            } else if (objectID == 1734) {
                client.stairs = 10;
                client.skillX = objectPosition.getX();
                client.setSkillY(objectPosition.getY());
                client.stairDistance = 3;
                client.stairDistanceAdd = 1;
            } else if (objectID == 55) {
                client.stairs = 15;
                client.skillX = objectPosition.getX();
                client.setSkillY(objectPosition.getY());
                client.stairDistance = 3;
                client.stairDistanceAdd = 1;
            } else if (objectID == 57) {
                client.stairs = 15;
                client.skillX = objectPosition.getX();
                client.setSkillY(objectPosition.getY());
                client.stairDistance = 3;
            } else if (objectID == 1755 || objectID == 5946 || objectID == 1757) {
                client.stairs = 4;
                client.skillX = objectPosition.getX();
                client.setSkillY(objectPosition.getY());
                client.stairDistance = 2;
            } else if (objectID == 1764) {
                client.stairs = 12;
                client.skillX = objectPosition.getX();
                client.setSkillY(objectPosition.getY());
                client.stairDistance = 1;
            } else if (objectID == 2148) {
                client.stairs = 8;
                client.skillX = objectPosition.getX();
                client.setSkillY(objectPosition.getY());
                client.stairDistance = 1;
            } else if (objectID == 3608) {
                client.stairs = 13;
                client.skillX = objectPosition.getX();
                client.setSkillY(objectPosition.getY());
                client.stairDistance = 1;
            } else if (objectID == 2408) {
                client.stairs = 16;
                client.skillX = objectPosition.getX();
                client.setSkillY(objectPosition.getY());
                client.stairDistance = 1;
            } else if (objectID == 5055) {
                client.stairs = 18;
                client.skillX = objectPosition.getX();
                client.setSkillY(objectPosition.getY());
                client.stairDistance = 1;
            } else if (objectID == 5131) {
                client.stairs = 20;
                client.skillX = objectPosition.getX();
                client.setSkillY(objectPosition.getY());
                client.stairDistance = 1;
            } else if (objectID == 9359) {
                client.stairs = 24;
                client.skillX = objectPosition.getX();
                client.setSkillY(objectPosition.getY());
                client.stairDistance = 1;
                client.stairDistance = 1;
            } else if (objectID == 2406) { /* Lost City Door */
                if (client.getEquipment()[Equipment.Slot.WEAPON.getId()] == 772) { // Dramen
                    // Staff
                    client.stairs = 27;
                    client.skillX = objectPosition.getX();
                    client.setSkillY(objectPosition.getY());
                    client.stairDistance = 1;
                } else {// Open Door
                }
            }
            // go downstairs
            if (objectID == 1746 || objectID == 1749) {
                client.stairs = 2;
                client.skillX = objectPosition.getX();
                client.setSkillY(objectPosition.getY());
                client.stairDistance = 1;
            } else if (objectID == 1740) {
                client.stairs = 2;
                client.skillX = objectPosition.getX();
                client.setSkillY(objectPosition.getY());
                client.stairDistance = 1;
            } else if (objectID == 1723) {
                client.stairs = 22;
                client.skillX = objectPosition.getX();
                client.setSkillY(objectPosition.getY());
                client.stairDistance = 2;
                client.stairDistanceAdd = 2;
            } else if (objectID == 16664) {
                if (objectPosition.getX() == 2603 && objectPosition.getY() == 3078) {
                    if (!client.checkItem(1543)) {
                        client.send(new SendMessage("You need a red key to go down these stairs!"));
                        return;
                    }
                    client.teleportToX = 2602;
                    client.teleportToY = 9479;
                    client.getPosition().setZ(0);
                } else if (objectPosition.getX() == 2569 && objectPosition.getY() == 3122) {
                    if (!client.checkItem(1545)) {
                        client.send(new SendMessage("You need a yellow key to use this staircase!"));
                        return;
                    }
                    client.teleportToX = 2570;
                    client.teleportToY = 9525;
                    client.getPosition().setZ(0);
                }
                return;
            } else if (objectID == 1992 && objectPosition.getX() == 2558 && objectPosition.getY() == 3444) {
//        if (client.playerHasItem(537, 50)) {
//          client.deleteItem(537, client.getItemSlot(537), 50);
//          client.send(new SendMessage("The gods accept your offer!"));
                client.teleportToX = 2717;
                client.teleportToY = 9820;
                client.getPosition().setZ(0);
//        } else {
//          client.send(new SendMessage("The gods require 50 dragon bones as a sacrifice!"));
//          return;
//        }
            } else if (objectID == 69) { //Stuff??
            } else if (objectID == 54) {
                client.stairs = 14;
                client.skillX = objectPosition.getX();
                client.setSkillY(objectPosition.getY());
                client.stairDistance = 3;
                client.stairDistanceAdd = 1;
            } else if (objectID == 56) {
                client.stairs = 14;
                client.skillX = objectPosition.getX();
                client.setSkillY(objectPosition.getY());
                client.stairDistance = 3;
            } else if (objectID == 1568 || objectID == 5947 || objectID == 6434
                    || /* objectID == 1759 || */objectID == 1570) {
                if (objectPosition.getX() == 2594 && objectPosition.getY() == 3085)
                    return;
                client.stairs = 3;
                client.skillX = objectPosition.getX();
                client.setSkillY(objectPosition.getY());
                client.stairDistance = 1;
            } else if (objectID == 2113) { // Mining guild stairs
                if (client.getLevel(Skill.MINING) >= 60) {
                    client.stairs = 3;
                    client.skillX = objectPosition.getX();
                    client.setSkillY(objectPosition.getY());
                    client.stairDistance = 1;
                } else {
                    client.send(new SendMessage("You need 60 mining to enter the mining guild."));
                }
            } else if (objectID == 492) {
                client.stairs = 11;
                client.skillX = objectPosition.getX();
                client.setSkillY(objectPosition.getY());
                client.stairDistance = 2;
            } else if (objectID == 2147) {
                client.stairs = 7;
                client.skillX = objectPosition.getX();
                client.setSkillY(objectPosition.getY());
                client.stairDistance = 1;
            } else if (objectID == 5054) {
                client.stairs = 17;
                client.skillX = objectPosition.getX();
                client.setSkillY(objectPosition.getY());
                client.stairDistance = 1;
            } else if (objectID == 5130) {
                client.stairs = 19;
                client.skillX = objectPosition.getX();
                client.setSkillY(objectPosition.getY());
                client.stairDistance = 1;
            } else if (objectID == 9358) {
                client.stairs = 23;
                client.skillX = objectPosition.getX();
                client.setSkillY(objectPosition.getY());
                client.stairDistance = 1;
            } else if (objectID == 5488) {
                client.stairs = 28;
                client.setSkillX(objectPosition.getX());
                client.setSkillY(objectPosition.getY());
                client.stairDistance = 1;
            } else if (objectID == 9294) {
                if (objectPosition.getX() == 2879 && objectPosition.getY() == 9813) {
                    client.stairs = "trap".hashCode();
                    client.stairDistance = 1;
                    client.setSkillX(objectPosition.getX());
                    client.setSkillY(objectPosition.getY());
                }
            }
        }
    }

}