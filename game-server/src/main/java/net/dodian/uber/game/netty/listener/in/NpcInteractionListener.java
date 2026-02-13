package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.Constants;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.combat.PlayerAttackCombatKt;
import net.dodian.uber.game.content.npcs.spawns.NpcContentDispatcher;
import net.dodian.uber.game.event.Event;
import net.dodian.uber.game.event.EventManager;
import net.dodian.uber.game.model.WalkToTask;
import net.dodian.uber.game.model.entity.npc.Npc;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.uber.game.model.player.skills.Skill;
import net.dodian.uber.game.model.player.skills.Skills;
import net.dodian.uber.game.model.player.skills.agility.Werewolf;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketHandler;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.uber.game.netty.listener.out.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Objects;

/**
 * Consolidated Netty handler for npc interaction opcodes:
 * 155 (click1), 17 (click2), 21 (click3), 18 (click4), 72 (attack).
 */
@PacketHandler(opcode = 155)
public class NpcInteractionListener implements PacketListener {

    private static final Logger logger = LoggerFactory.getLogger(NpcInteractionListener.class);

    static {
        NpcInteractionListener listener = new NpcInteractionListener();
        PacketListenerManager.register(155, listener);
        PacketListenerManager.register(17, listener);
        PacketListenerManager.register(21, listener);
        PacketListenerManager.register(18, listener);
        PacketListenerManager.register(72, listener);
    }

    @Override
    public void handle(Client client, GamePacket packet) {
        switch (packet.getOpcode()) {
            case 155:
                handleNpcClick1(client, packet);
                return;
            case 17:
                handleNpcClick2(client, packet);
                return;
            case 21:
                handleNpcClick3(client, packet);
                return;
            case 18:
                handleNpcClick4(client, packet);
                return;
            case 72:
                handleNpcAttack(client, packet);
                return;
            default:
                logger.warn("NpcInteractionListener got unexpected opcode={} for player={}", packet.getOpcode(), client.getPlayerName());
        }
    }

    private void handleNpcClick1(Client client, GamePacket packet) {
        ByteMessage message = ByteMessage.wrap(packet.getPayload());
        int npcIndex = message.getShort(true, ByteOrder.LITTLE, ValueType.NORMAL);
        Npc tempNpc = Server.npcManager.getNpc(npcIndex);
        if (tempNpc == null) {
            return;
        }

        int npcId = tempNpc.getId();
        WalkToTask task = new WalkToTask(WalkToTask.Action.NPC_FIRST_CLICK, npcId, tempNpc.getPosition());
        client.setWalkToTask(task);

        if (client.randomed || client.UsingAgility) {
            return;
        }
        if (!client.playerPotato.isEmpty()) {
            client.playerPotato.clear();
        }
        if (npcId == 2794) {
            if (client.playerHasItem(1735)) {
                client.addItem(1737, 1);
                client.checkItemUpdate();
            } else {
                client.send(new SendMessage("You need some shears to shear this sheep!"));
            }
            return;
        }

        EventManager.getInstance().registerEvent(new Event(600) {
            @Override
            public void execute() {
                if (client.disconnected || client.getWalkToTask() != task) {
                    stop();
                    return;
                }
                if (!client.goodDistanceEntity(tempNpc, 1) || tempNpc.getPosition().withinDistance(client.getPosition(), 0)) {
                    return;
                }
                performNpcClick1(client, tempNpc);
                client.setWalkToTask(null);
                stop();
            }
        });
    }

    private void performNpcClick1(Client client, Npc tempNpc) {
        if (!tempNpc.isAlive()) {
            client.send(new SendMessage("That monster has been killed!"));
            return;
        }

        int npcId = tempNpc.getId();
        client.resetAction();
        client.faceNpc(tempNpc.getSlot());
        client.skillX = tempNpc.getPosition().getX();
        client.setSkillY(tempNpc.getPosition().getY());

        if (npcId == 5809) {
            client.openTan();
        } else if (npcId == 5792) {
            client.triggerTele(3045, 3372, 0, false);
            client.send(new SendMessage("Welcome to the party room!"));
        } else if (npcId == 3306) {
            int peopleInEdge = 0;
            int peopleInWild = 0;
            for (int index = 0; index < Constants.maxPlayers; index++) {
                Client checkPlayer = (Client) PlayerHandler.players[index];
                if (checkPlayer != null) {
                    if (checkPlayer.inWildy()) {
                        peopleInWild++;
                    } else if (checkPlayer.inEdgeville()) {
                        peopleInEdge++;
                    }
                }
            }
            client.showNPCChat(
                    3306,
                    590,
                    new String[]{
                            "There is currently " + peopleInWild + " player" + (peopleInWild != 1 ? "s" : "") + " in the wild!",
                            "There is " + peopleInEdge + " player" + (peopleInEdge != 1 ? "s" : "") + " in Edgeville!"
                    }
            );
        }

        client.startFishing(npcId, 1);

        if (NpcContentDispatcher.tryHandleClick(client, 1, tempNpc)) {
            return;
        }

        if (npcId == 394 || npcId == 395 || npcId == 7677) {
            client.NpcWanneTalk = 1;
            client.convoId = 0;
        } else if (npcId == 5927) {
            Werewolf wolf = new Werewolf(client);
            wolf.handStick();
        } else if (npcId == 637) {
            client.NpcWanneTalk = 3;
            client.convoId = 3;
        } else if (npcId == 555) {
            client.send(new SendMessage(client.playerRights > 1
                    ? "Monk debug quest state (quests[0]): " + client.quests[0]
                    : "Suddenly the monk had an urge to dissapear!"));
        } else if (npcId == 683) {
            client.WanneShop = 11;
        } else if (npcId == 2053) {
            client.WanneShop = 32;
        } else if (npcId == 3951) {
            if (client.premium) {
                client.ReplaceObject(2728, 3349, 2391, 0, 0);
                client.ReplaceObject(2729, 3349, 2392, -2, 0);
                client.showNPCChat(npcId, 590, new String[]{"Welcome to the Guild of Legends", "Enjoy your stay."});
            } else {
                client.showNPCChat(npcId, 595, new String[]{"You must be a premium member to enter", "Visit Dodian.net to subscribe"});
            }
        } else if (npcId == 376 && client.playerRights == 2) {
            client.triggerTele(2772, 3234, 0, false);
        } else if (npcId == 8051) {
            client.NpcWanneTalk = 8051;
        } else if (npcId == 659) {
            client.NpcWanneTalk = 1000;
            client.convoId = 1001;
        } else if (npcId == 3640) {
            client.WanneShop = 17;
        } else if (npcId == 556) {
            client.WanneShop = 31;
        } else if (npcId == 557) {
            tempNpc.requestAnim(5643, 0);
            for (int skill = 0; skill < 4; skill++) {
                skill = (skill == 3 ? 4 : skill);
                client.boost(
                        5 + (int) (Skills.getLevelForExperience(client.getExperience(Objects.requireNonNull(Skill.getSkill(skill)))) * 0.15),
                        Skill.getSkill(skill)
                );
            }
            int ticks = (1 + Skills.getLevelForExperience(client.getExperience(Skill.HERBLORE))) * 2;
            client.addEffectTime(2, 200 + ticks);
            client.send(new SendMessage("The monk boost your stats!"));
        } else if (npcId == 4808) {
            client.WanneShop = 34;
        } else if (npcId == 3541) {
            client.WanneShop = 35;
        } else if (npcId == 520) {
            client.NpcWanneTalk = 19;
            client.convoId = 4;
        } else if (npcId == 5842) {
            boolean canClaim = new Date().before(new Date("06/1/2024")) && !client.checkItem(7927);
            if (canClaim) {
                client.showNPCChat(npcId, 595, new String[]{"Here take a easter ring for all your troubles.", "Enjoy your stay at Dodian."});
                client.addItem(7927, 1);
                client.checkItemUpdate();
            } else {
                client.showNPCChat(npcId, 595, new String[]{client.checkItem(7927) ? "You already got the ring." : "It is not May anymore."});
            }
        } else if (npcId == 1779) {
            client.showNPCChat(1779, 605, new String[]{"What are you even doing in here?!", "Begone from me!"});
        } else if (npcId == 943) {
            int num = 0;
            for (Player player : PlayerHandler.players) {
                if (player != null && player.wildyLevel > 0) {
                    num++;
                }
            }
            tempNpc.setText("There are currently " + num + " people in the wilderness");
        } else {
            logger.debug("Unhandled NPC first-click fallback npcId={} player={}", npcId, client.getPlayerName());
        }
    }

    private void handleNpcClick2(Client client, GamePacket packet) {
        ByteBuf payload = packet.getPayload();
        int npcIndex = readSignedWordBigEndianA(payload);
        Npc tempNpc = Server.npcManager.getNpc(npcIndex);
        if (tempNpc == null) {
            return;
        }

        if (logger.isTraceEnabled()) {
            logger.trace("Npc click2 opcode={} npcIndex={} npcId={} player={}", packet.getOpcode(), npcIndex, tempNpc.getId(), client.getPlayerName());
        }

        int npcId = tempNpc.getId();
        WalkToTask task = new WalkToTask(WalkToTask.Action.NPC_SECOND_CLICK, npcId, tempNpc.getPosition());
        client.setWalkToTask(task);

        if (client.randomed || client.UsingAgility) {
            return;
        }
        if (!client.playerPotato.isEmpty()) {
            client.playerPotato.clear();
        }

        EventManager.getInstance().registerEvent(new Event(600) {
            @Override
            public void execute() {
                if (client.disconnected || client.getWalkToTask() != task) {
                    stop();
                    return;
                }
                if (!client.goodDistanceEntity(tempNpc, 1) || tempNpc.getPosition().withinDistance(client.getPosition(), 0)) {
                    return;
                }
                performNpcClick2(client, tempNpc);
                client.setWalkToTask(null);
                stop();
            }
        });
    }

    private void performNpcClick2(Client client, Npc tempNpc) {
        if (!tempNpc.isAlive()) {
            client.send(new SendMessage("That monster has been killed!"));
            return;
        }

        int npcId = tempNpc.getId();
        client.resetAction();
        client.faceNpc(tempNpc.getSlot());
        client.skillX = tempNpc.getPosition().getX();
        client.setSkillY(tempNpc.getPosition().getY());
        client.startFishing(npcId, 2);

        if (NpcContentDispatcher.tryHandleClick(client, 2, tempNpc)) {
            return;
        }

        logger.debug("Unhandled NPC second-click fallback npcId={} player={}", npcId, client.getPlayerName());
    }

    private void handleNpcClick3(Client client, GamePacket packet) {
        ByteBuf payload = packet.getPayload();
        int npcIndex = readSignedWord(payload);
        Npc tempNpc = Server.npcManager.getNpc(npcIndex);
        if (tempNpc == null) {
            return;
        }

        if (logger.isTraceEnabled()) {
            logger.trace("Npc click3 opcode={} npcIndex={} npcId={} player={}", packet.getOpcode(), npcIndex, tempNpc.getId(), client.getPlayerName());
        }

        int npcId = tempNpc.getId();
        WalkToTask task = new WalkToTask(WalkToTask.Action.NPC_THIRD_CLICK, npcId, tempNpc.getPosition());
        client.setWalkToTask(task);

        if (client.randomed || client.UsingAgility) {
            return;
        }
        if (!client.playerPotato.isEmpty()) {
            client.playerPotato.clear();
        }

        EventManager.getInstance().registerEvent(new Event(600) {
            @Override
            public void execute() {
                if (client.disconnected || client.getWalkToTask() != task) {
                    stop();
                    return;
                }
                if (!client.goodDistanceEntity(tempNpc, 1) || tempNpc.getPosition().withinDistance(client.getPosition(), 0)) {
                    return;
                }
                performNpcClick3(client, tempNpc);
                client.setWalkToTask(null);
                stop();
            }
        });
    }

    private void performNpcClick3(Client client, Npc tempNpc) {
        if (client.isBusy()) {
            return;
        }

        int npcId = tempNpc.getId();
        client.resetAction();
        client.faceNpc(tempNpc.getSlot());
        client.skillX = tempNpc.getPosition().getX();
        client.setSkillY(tempNpc.getPosition().getY());

        if (NpcContentDispatcher.tryHandleClick(client, 3, tempNpc)) {
            return;
        }

        logger.debug("Unhandled NPC third-click fallback npcId={} player={}", npcId, client.getPlayerName());
    }

    private void handleNpcClick4(Client client, GamePacket packet) {
        ByteBuf payload = packet.getPayload();
        int npcIndex = readSignedWordBigEndian(payload);
        Npc tempNpc = Server.npcManager.getNpc(npcIndex);
        if (tempNpc == null) {
            return;
        }

        if (logger.isTraceEnabled()) {
            logger.trace("Npc click4 opcode={} npcIndex={} npcId={} player={}", packet.getOpcode(), npcIndex, tempNpc.getId(), client.getPlayerName());
        }

        int npcId = tempNpc.getId();
        WalkToTask task = new WalkToTask(WalkToTask.Action.NPC_FOURTH_CLICK, npcId, tempNpc.getPosition());
        client.setWalkToTask(task);

        if (client.randomed || client.UsingAgility) {
            return;
        }
        if (!client.playerPotato.isEmpty()) {
            client.playerPotato.clear();
        }

        EventManager.getInstance().registerEvent(new Event(600) {
            @Override
            public void execute() {
                if (client.disconnected || client.getWalkToTask() != task) {
                    stop();
                    return;
                }
                if (!client.goodDistanceEntity(tempNpc, 1) || tempNpc.getPosition().withinDistance(client.getPosition(), 0)) {
                    return;
                }
                performNpcClick4(client, tempNpc);
                client.setWalkToTask(null);
                stop();
            }
        });
    }

    private void performNpcClick4(Client client, Npc tempNpc) {
        if (client.isBusy()) {
            return;
        }

        int npcId = tempNpc.getId();
        client.skillX = tempNpc.getPosition().getX();
        client.setSkillY(tempNpc.getPosition().getY());

        if (NpcContentDispatcher.tryHandleClick(client, 4, tempNpc)) {
            return;
        }

        logger.debug("Unhandled NPC fourth-click fallback npcId={} player={}", npcId, client.getPlayerName());
    }

    private void handleNpcAttack(Client client, GamePacket packet) {
        ByteBuf payload = packet.getPayload();
        int npcIndex = readUnsignedWordA(payload);

        logger.debug("Npc attack opcode={} npcIndex={} player={}", packet.getOpcode(), npcIndex, client.getPlayerName());
        if (client.magicId >= 0) {
            client.magicId = -1;
        }
        if (client.deathStage >= 1) {
            return;
        }

        Npc npc = Server.npcManager.getNpc(npcIndex);
        if (npc == null) {
            return;
        }
        if (client.randomed || client.UsingAgility) {
            return;
        }
        if (NpcContentDispatcher.tryHandleAttack(client, npc)) {
            return;
        }

        boolean rangedAttack = PlayerAttackCombatKt.getAttackStyle(client) != 0;
        if ((rangedAttack && client.goodDistanceEntity(npc, 5)) || client.goodDistanceEntity(npc, 1)) {
            client.resetWalkingQueue();
            client.startAttack(npc);
            return;
        }

        WalkToTask task = new WalkToTask(WalkToTask.Action.ATTACK_NPC, npcIndex, npc.getPosition());
        client.setWalkToTask(task);
        EventManager.getInstance().registerEvent(new Event(600) {
            @Override
            public void execute() {
                if (client.disconnected || client.getWalkToTask() != task) {
                    stop();
                    return;
                }
                if ((PlayerAttackCombatKt.getAttackStyle(client) != 0 && client.goodDistanceEntity(npc, 5))
                        || client.goodDistanceEntity(npc, 1)) {
                    client.resetWalkingQueue();
                    client.startAttack(npc);
                    client.setWalkToTask(null);
                    stop();
                }
            }
        });
    }

    private static int readSignedWordBigEndianA(ByteBuf buf) {
        int low = (buf.readUnsignedByte() - 128) & 0xFF;
        int high = buf.readUnsignedByte();
        int value = (high << 8) | low;
        if (value > 32767) {
            value -= 65536;
        }
        return value;
    }

    private static int readSignedWord(ByteBuf buf) {
        int high = buf.readUnsignedByte();
        int low = buf.readUnsignedByte();
        int value = (high << 8) | low;
        if (value > 32767) {
            value -= 65536;
        }
        return value;
    }

    private static int readSignedWordBigEndian(ByteBuf buf) {
        int low = buf.readUnsignedByte();
        int high = buf.readUnsignedByte();
        int value = (high << 8) | low;
        if (value > 32767) {
            value -= 65536;
        }
        return value;
    }

    private static int readUnsignedWordA(ByteBuf buf) {
        int high = buf.readUnsignedByte();
        int low = (buf.readUnsignedByte() - 128) & 0xFF;
        return (high << 8) | low;
    }
}
