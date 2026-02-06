package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.content.items.ItemDispatcher;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.entity.Entity;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.out.SendMessage;
import net.dodian.uber.game.model.player.skills.Skill;
import net.dodian.uber.game.model.player.skills.Skills;
import net.dodian.uber.game.model.player.skills.prayer.Prayer;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketHandler;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import net.dodian.utilities.DbTables;
import net.dodian.utilities.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Objects;

import static net.dodian.utilities.DatabaseKt.getDbConnection;

/**
 * A Netty-based PacketListener that handles all incoming first-click item actions (opcode 122).
 * This class is a complete and faithful replacement for the legacy ClickItem.java file,
 * with corrected packet decoding logic and preserving the original code layout.
 */
@PacketHandler(opcode = 122)
public class ClickItemListener implements PacketListener {

    private static final Logger logger = LoggerFactory.getLogger(ClickItemListener.class);

    static {
        PacketListenerManager.register(122, new ClickItemListener());
    }

    private int readSignedWordBigEndianA(ByteBuf buf) {
        int high = buf.readByte();
        int low = (buf.readByte() - 128) & 0xFF;
        return (high << 8) | low;
    }

    private int readUnsignedWordA(ByteBuf buf) {
        int high = buf.readUnsignedByte();
        int low = (buf.readUnsignedByte() - 128) & 0xFF;
        return (high << 8) | low;
    }

    /**
     * CORRECTED: Reads a little-endian short. This is the fix for the item ID mismatch.
     */
    private int readLEShort(ByteBuf buf) {
        int low = buf.readUnsignedByte();
        int high = buf.readUnsignedByte();
        return (high << 8) | low;
    }

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.getPayload();

        // Faithful replication of legacy decoding order with the corrected item ID read.
        int interfaceId = buf.readUnsignedShort();
        int itemId = buf.readUnsignedShort();
        int itemSlot = buf.readUnsignedShort();

        // Debug line to confirm packet data is now being read correctly.
        logger.debug("ClickItem: [interface={}, slot={}, id={}] for player {}", interfaceId, itemSlot, itemId, client.getPlayerName());

        if (client.fillEssencePouch(itemId)) {
            return;
        }

        if (client.randomed || client.UsingAgility) {
            return;
        }

        if (itemSlot < 0 || itemSlot >= client.playerItems.length) {
            client.disconnected = true;
            return;
        }

        if (client.playerItems[itemSlot] - 1 != itemId) {
            logger.warn("ClickItem Mismatch: Player {} tried to use item {} from slot {}, but found {}",
                    client.getPlayerName(), itemId, itemSlot, client.playerItems[itemSlot] - 1);
            return;
        }

        if (itemId == 5733) {
            handleStaffTool(client);
            return;
        }
        if (itemId == 2528) {
            client.openGenie();
            return;
        }
        if (itemId == 6543) {
            client.openAntique();
            return;
        }

        boolean isHerb = (itemId >= 199 && itemId <= 219) || itemId == 3049 || itemId == 3051;
        if (isHerb) {
            processItemClick(client, itemId, itemSlot, interfaceId);
        } else if (System.currentTimeMillis() - client.lastAction > 100) {
            processItemClick(client, itemId, itemSlot, interfaceId);
            client.lastAction = System.currentTimeMillis();
        }
    }

    private void handleStaffTool(Client client) {
        if (client.playerRights < 2) {
            client.send(new SendMessage("You need to be an administrator to use this tool."));
            return;
        }
        try {
            if (client.getPlayerNpc() < 0) {
                client.send(new SendMessage("Please select an NPC first using ::pnpc id"));
                return;
            }
            if (Server.npcManager.getData(client.getPlayerNpc()) == null) {
                Server.npcManager.reloadNpcConfig(client, client.getPlayerNpc(), "New Npc", "-1");
                client.send(new SendMessage("NPC data not found, creating a new entry. Please try again."));
                return;
            }

            try(Connection conn = getDbConnection(); Statement statement = conn.createStatement()) {
                ResultSet rs = statement.executeQuery("SELECT 1 FROM " + DbTables.GAME_NPC_SPAWNS + " WHERE id='" + client.getPlayerNpc() + "' AND x='" + client.getPosition().getX() + "' AND y='" + client.getPosition().getY() + "' AND height='" + client.getPosition().getZ() + "'");
                if (rs.next()) {
                    client.send(new SendMessage("An NPC spawn already exists at this position."));
                    return;
                }

                int health = Server.npcManager.getData(client.getPlayerNpc()).getHP();
                statement.executeUpdate("INSERT INTO " + DbTables.GAME_NPC_SPAWNS + " SET id = " + client.getPlayerNpc() + ", x=" + client.getPosition().getX()
                        + ", y=" + client.getPosition().getY() + ", height=" + client.getPosition().getZ() + ", hitpoints="
                        + health + ", live=1, face=0, rx=0,ry=0,rx2=0,ry2=0,movechance=0");

                Server.npcManager.createNpc(client.getPlayerNpc(), new Position(client.getPosition().getX(), client.getPosition().getY(), client.getPosition().getZ()), 0);
                client.send(new SendMessage("Npc added = " + client.getPlayerNpc() + ", at x = " + client.getPosition().getX()
                        + " y = " + client.getPosition().getY() + "."));
            }
        } catch (Exception e) {
            logger.error("Staff Tool (Potato) SQL error for {}: {}", client.getPlayerName(), e.getMessage(), e);
            client.send(new SendMessage("An error occurred while spawning the NPC."));
        }
    }

    public void processItemClick(Client client, int id, int slot, int interfaceId) {
        int item = client.playerItems[slot] - 1;
        if (item != id) {
            return; // might prevent stuff
        }
        if (client.duelRule[7] && client.inDuel && client.duelFight) {
            client.send(new SendMessage("Food has been disabled for this duel"));
            return;
        }
        boolean used = true;
        int nextId = -1;
        if (client.inDuel || client.duelFight || client.duelConfirmed || client.duelConfirmed2) {
            if (item != 4155) { //Slayer gem check
                client.send(new SendMessage("This item cannot be used in a duel!"));
                return;
            }
        }
        if (Prayer.buryBones(client, item, slot)) {
            return;
        }
        if (ItemDispatcher.tryHandle(client, 1, item, slot, interfaceId)) {
            client.checkItemUpdate();
            return;
        }
        if (client.playerHasItem(item)) {
            switch (item) {
                case 121: // regular attack potion
                case 123:
                case 125:
                case 2428:
                    if (client.deathStage > 0 || client.deathTimer > 0 || client.inDuel) {
                        return;
                    }
                    client.requestAnim(1327, 0);
                    client.boost(3 + (int)(Skills.getLevelForExperience(client.getExperience(Skill.ATTACK)) * 0.1), Skill.ATTACK);
                    for(int i = 0; i < Utils.pot_4_dose.length && nextId == -1; i++)
                        nextId = Utils.pot_4_dose[i] == item ? Utils.pot_3_dose[i] :
                                Utils.pot_3_dose[i] == item ? Utils.pot_2_dose[i] :
                                        Utils.pot_2_dose[i] == item ? Utils.pot_1_dose[i] :
                                                Utils.pot_1_dose[i] == item ? 229 : -1;
                    client.send(new SendMessage(nextId == 229 ? "You empty the attack potion." : "You drink the attack potion."));
                    break;
                case 113:
                case 115: // regular str
                case 117:
                case 119:
                    if (client.deathStage > 0 || client.deathTimer > 0 || client.inDuel) {
                        return;
                    }
                    client.requestAnim(1327, 0);
                    client.boost(3 + (int)(Skills.getLevelForExperience(client.getExperience(Skill.STRENGTH)) * 0.1), Skill.STRENGTH);
                    for(int i = 0; i < Utils.pot_4_dose.length && nextId == -1; i++)
                        nextId = Utils.pot_4_dose[i] == item ? Utils.pot_3_dose[i] :
                                Utils.pot_3_dose[i] == item ? Utils.pot_2_dose[i] :
                                        Utils.pot_2_dose[i] == item ? Utils.pot_1_dose[i] :
                                                Utils.pot_1_dose[i] == item ? 229 : -1;
                    client.send(new SendMessage(nextId == 229 ? "You empty the strength potion." : "You drink the strength potion."));
                    break;
                case 2432:
                case 133: // regular def
                case 135:
                case 137:
                    if (client.deathStage > 0 || client.deathTimer > 0 || client.inDuel) {
                        return;
                    }
                    client.requestAnim(1327, 0);
                    client.boost(3 + (int)(Skills.getLevelForExperience(client.getExperience(Skill.DEFENCE)) * 0.1), Skill.DEFENCE);
                    for(int i = 0; i < Utils.pot_4_dose.length && nextId == -1; i++)
                        nextId = Utils.pot_4_dose[i] == item ? Utils.pot_3_dose[i] :
                                Utils.pot_3_dose[i] == item ? Utils.pot_2_dose[i] :
                                        Utils.pot_2_dose[i] == item ? Utils.pot_1_dose[i] :
                                                Utils.pot_1_dose[i] == item ? 229 : -1;
                    client.send(new SendMessage(nextId == 229 ? "You empty the defense potion." : "You drink the defense potion."));
                    break;
                case 2436:
                case 145:
                case 147:
                case 149:
                    if (client.deathStage > 0 || client.deathTimer > 0 || client.inDuel) {
                        return;
                    }
                    client.requestAnim(1327, 0);
                    client.boost(5 + (int)(Skills.getLevelForExperience(client.getExperience(Skill.ATTACK)) * 0.15), Skill.ATTACK);
                    for(int i = 0; i < Utils.pot_4_dose.length && nextId == -1; i++)
                        nextId = Utils.pot_4_dose[i] == item ? Utils.pot_3_dose[i] :
                                Utils.pot_3_dose[i] == item ? Utils.pot_2_dose[i] :
                                        Utils.pot_2_dose[i] == item ? Utils.pot_1_dose[i] :
                                                Utils.pot_1_dose[i] == item ? 229 : -1;
                    client.send(new SendMessage(nextId == 229 ? "You empty the super attack potion." : "You drink the super attack potion."));
                    break;
                case 2440:
                case 157:
                case 159:
                case 161:
                    if (client.deathStage > 0 || client.deathTimer > 0 || client.inDuel) {
                        return;
                    }
                    client.requestAnim(1327, 0);
                    client.boost(5 + (int)(Skills.getLevelForExperience(client.getExperience(Skill.STRENGTH)) * 0.15), Skill.STRENGTH);
                    for(int i = 0; i < Utils.pot_4_dose.length && nextId == -1; i++)
                        nextId = Utils.pot_4_dose[i] == item ? Utils.pot_3_dose[i] :
                                Utils.pot_3_dose[i] == item ? Utils.pot_2_dose[i] :
                                        Utils.pot_2_dose[i] == item ? Utils.pot_1_dose[i] :
                                                Utils.pot_1_dose[i] == item ? 229 : -1;
                    client.send(new SendMessage(nextId == 229 ? "You empty the super strength potion." : "You drink the super strength potion."));
                    break;
                case 2442:
                case 163:
                case 165:
                case 167:
                    if (client.deathStage > 0 || client.deathTimer > 0 || client.inDuel) {
                        return;
                    }
                    client.requestAnim(1327, 0);
                    client.boost(5 + (int)(Skills.getLevelForExperience(client.getExperience(Skill.DEFENCE)) * 0.15), Skill.DEFENCE);
                    client.refreshSkill(Skill.DEFENCE);
                    for(int i = 0; i < Utils.pot_4_dose.length && nextId == -1; i++)
                        nextId = Utils.pot_4_dose[i] == item ? Utils.pot_3_dose[i] :
                                Utils.pot_3_dose[i] == item ? Utils.pot_2_dose[i] :
                                        Utils.pot_2_dose[i] == item ? Utils.pot_1_dose[i] :
                                                Utils.pot_1_dose[i] == item ? 229 : -1;
                    client.send(new SendMessage(nextId == 229 ? "You empty the super defense potion." : "You drink the super defense potion."));
                    break;
                case 2444: //4 dose
                case 169://ranging potion
                case 171:
                case 173:
                    if (client.deathStage > 0 || client.deathTimer > 0 || client.inDuel) {
                        return;
                    }
                    client.requestAnim(1327, 0);
                    client.boost(4 + (int)(Skills.getLevelForExperience(client.getExperience(Skill.RANGED)) * 0.12), Skill.RANGED);
                    for(int i = 0; i < Utils.pot_4_dose.length && nextId == -1; i++)
                        nextId = Utils.pot_4_dose[i] == item ? Utils.pot_3_dose[i] :
                                Utils.pot_3_dose[i] == item ? Utils.pot_2_dose[i] :
                                        Utils.pot_2_dose[i] == item ? Utils.pot_1_dose[i] :
                                                Utils.pot_1_dose[i] == item ? 229 : -1;
                    client.send(new SendMessage(nextId == 229 ? "You empty the ranging potion." : "You drink the ranging potion."));
                    break;
                case 139://prayer potion
                case 141:
                case 143:
                case 2434: //4dose
                    if (client.deathStage > 0 || client.deathTimer > 0 || client.inDuel) {
                        return;
                    }
                    client.requestAnim(1327, 0);
                    client.pray(8 + (int)(client.getMaxPrayer() * 0.25));
                    client.refreshSkill(Skill.PRAYER);
                    for(int i = 0; i < Utils.pot_4_dose.length && nextId == -1; i++)
                        nextId = Utils.pot_4_dose[i] == item ? Utils.pot_3_dose[i] :
                                Utils.pot_3_dose[i] == item ? Utils.pot_2_dose[i] :
                                        Utils.pot_2_dose[i] == item ? Utils.pot_1_dose[i] :
                                                Utils.pot_1_dose[i] == item ? 229 : -1;
                    client.send(new SendMessage(nextId == 229 ? "You empty the prayer potion." : "You drink the prayer potion."));
                    break;
                case 3026://Super restore potion
                case 3028:
                case 3030:
                case 3024: //4dose
                    if (client.deathStage > 0 || client.deathTimer > 0 || client.inDuel) {
                        return;
                    }
                    client.requestAnim(1327, 0);
                    client.pray(10 + (int)(client.getMaxPrayer() * 0.28));
                    client.refreshSkill(Skill.PRAYER);
                    for(int i = 0; i < Utils.pot_4_dose.length && nextId == -1; i++)
                        nextId = Utils.pot_4_dose[i] == item ? Utils.pot_3_dose[i] :
                                Utils.pot_3_dose[i] == item ? Utils.pot_2_dose[i] :
                                        Utils.pot_2_dose[i] == item ? Utils.pot_1_dose[i] :
                                                Utils.pot_1_dose[i] == item ? 229 : -1;
                    client.send(new SendMessage(nextId == 229 ? "You empty the restore potion." : "You drink the restore potion."));
                    break;
                case 12695: //Super combat potion
                case 12697:
                case 12699:
                case 12701:
                    if (client.deathStage > 0 || client.deathTimer > 0 || client.inDuel) {
                        return;
                    }
                    client.requestAnim(1327, 0);
                    for(int skill = 0; skill < 3; skill++)
                        client.boost(5 + (int)(Skills.getLevelForExperience(client.getExperience(Skill.getSkill(skill))) * 0.15), Skill.getSkill(skill));
                    for(int i = 0; i < Utils.pot_4_dose.length && nextId == -1; i++)
                        nextId = Utils.pot_4_dose[i] == item ? Utils.pot_3_dose[i] :
                                Utils.pot_3_dose[i] == item ? Utils.pot_2_dose[i] :
                                        Utils.pot_2_dose[i] == item ? Utils.pot_1_dose[i] :
                                                Utils.pot_1_dose[i] == item ? 229 : -1;
                    client.send(new SendMessage(nextId == 229 ? "You empty the super combat potion." : "You drink the super combat potion."));
                    break;
                case 11730: //Overload
                case 11731:
                case 11732:
                case 11733:
                    if (client.deathStage > 0 || client.deathTimer > 0 || client.inDuel || client.getCurrentHealth() < 11) {
                        return;
                    }
                    client.requestAnim(1327, 0);
                    client.dealDamage(null, 10, Entity.hitType.CRIT);
                    for(int skill = 0; skill < 4; skill++) {
                        skill = skill == 3 ? 4 : skill;
                        client.boost(5 + (int) (Skills.getLevelForExperience(client.getExperience(Objects.requireNonNull(Skill.getSkill(skill)))) * 0.15), Skill.getSkill(skill));
                    }
                    int ticks = (1 + Skills.getLevelForExperience(client.getExperience(Skill.HERBLORE))) * 2;
                    client.addEffectTime(2, 200 + ticks);
                    for(int i = 0; i < Utils.pot_4_dose.length && nextId == -1; i++)
                        nextId = Utils.pot_4_dose[i] == item ? Utils.pot_3_dose[i] :
                                Utils.pot_3_dose[i] == item ? Utils.pot_2_dose[i] :
                                        Utils.pot_2_dose[i] == item ? Utils.pot_1_dose[i] :
                                                Utils.pot_1_dose[i] == item ? 229 : -1;
                    client.send(new SendMessage(nextId == 229 ? "You empty the overload potion." : "You drink the overload potion."));
                    break;
                case 2452:
                case 2454:
                case 2456:
                case 2458:
                    if((client.effects.size() > 1 && client.effects.get(1) > 50) || client.deathStage > 0 || client.deathTimer > 0 || client.inDuel) {
                        return;
                    }
                    client.requestAnim(1327, 0);
                    client.addEffectTime(1, 500);
                    for(int i = 0; i < Utils.pot_4_dose.length && nextId == -1; i++)
                        nextId = Utils.pot_4_dose[i] == item ? Utils.pot_3_dose[i] :
                                Utils.pot_3_dose[i] == item ? Utils.pot_2_dose[i] :
                                        Utils.pot_2_dose[i] == item ? Utils.pot_1_dose[i] :
                                                Utils.pot_1_dose[i] == item ? 229 : -1;
                    client.send(new SendMessage(nextId == 229 ? "You empty the anti-fire potion." : "You drink the anti-fire potion."));
                    break;
                case 4155:
                    if (client.inTrade || client.inDuel)
                        break;
                    client.NpcDialogue = 15;
                    client.NpcDialogueSend = false;
                    client.nextDiag = -1;
                    used = false;
                    break;
                default:
                    used = false;
                    break;
            }
        }
        if (used) {
            client.deleteItem(item, slot, 1);
        }
        if (nextId > 0) {
            client.addItemSlot(nextId, 1, slot);
        }
        client.checkItemUpdate();
    }
}
