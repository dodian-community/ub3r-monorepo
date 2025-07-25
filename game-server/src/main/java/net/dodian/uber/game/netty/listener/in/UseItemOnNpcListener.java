package net.dodian.uber.game.netty.listener.in;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.entity.npc.Npc;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.content.Skillcape;
import net.dodian.uber.game.netty.listener.out.SendMessage;
import net.dodian.uber.game.netty.game.GamePacket;
import net.dodian.uber.game.netty.listener.PacketListener;
import net.dodian.uber.game.netty.listener.PacketListenerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opcode 57 – Use item on NPC. Mirrors legacy {@code UseItemOnNpc} logic.
 */
public class UseItemOnNpcListener implements PacketListener {

    static { PacketListenerManager.register(57, new UseItemOnNpcListener()); }

    private static final Logger logger = LoggerFactory.getLogger(UseItemOnNpcListener.class);

    /* ---------------- Byte helpers (match legacy Stream variants) ---------------- */
    private static int readUnsignedWordA(ByteBuf buf) {
        int high = buf.readUnsignedByte();
        int low = buf.readUnsignedByte();
        return ((high << 8) | ((low - 128) & 0xFF)) & 0xFFFF;
    }

    private static int readUnsignedWordBigEndian(ByteBuf buf) {
        int low = buf.readUnsignedByte();
        int high = buf.readUnsignedByte();
        return ((high << 8) | low) & 0xFFFF;
    }

    @Override
    public void handle(Client client, GamePacket packet) {
        ByteBuf buf = packet.getPayload();

        int itemId = readUnsignedWordA(buf);
        int npcIndex = readUnsignedWordA(buf);
        int slot = readUnsignedWordBigEndian(buf);
        readUnsignedWordA(buf); // discarded value, matches legacy behaviour

        if (logger.isTraceEnabled()) {
            logger.trace("UseItemOnNpc item={} slot={} npcIndex={} player={}", itemId, slot, npcIndex, client.getPlayerName());
        }

        /* Validation */
        if (slot < 0 || slot > 27) {
            logger.warn("UseItemOnNpc invalid slot={} item={} npcIndex={} -> disconnect {}", slot, itemId, npcIndex, client.getPlayerName());
            client.disconnected = true;
            return;
        }
        if (itemId != client.playerItems[slot] - 1) return;
        if (client.randomed || client.UsingAgility) return;

        Npc npc = Server.npcManager.getNpc(npcIndex);
        if (npc == null) return;
        int npcId = npc.getId();
        client.faceNpc(npcIndex);

        /* Potato dev item – opens internal UI */
        if (itemId == 5733) {
            client.playerPotato.clear();
            client.playerPotato.add(0, 2);
            client.playerPotato.add(1, npcIndex);
            client.playerPotato.add(2, npcId);
            client.playerPotato.add(3, 1);
            client.showPlayerOption(new String[]{
                    "What do you wish to do?", "Remove spawn", "Check drops", "Reload drops", "Check config", "Reload config!"});
            client.NpcDialogueSend = true;
            return;
        }

        /* Slayer gem */
        if (itemId == 4155) { npc.showGemConfig(client); return; }

        /* Dragonfire shield assembly */
        if (npcId == 535 && (itemId == 1540 || itemId == 11286)) {
            if (itemId == 1540 && !client.playerHasItem(11286)) { client.showNPCChat(npcId, 596, new String[]{"You need a draconic visage!"}); return; }
            if (itemId == 11286 && !client.playerHasItem(1540)) { client.showNPCChat(npcId, 596, new String[]{"You need a anti-dragon shield!"}); return; }
            if (!client.playerHasItem(995, 1_500_000)) { client.showNPCChat(npcId, 596, new String[]{"You need 1.5 million coins!"}); return; }
            client.deleteItem(itemId, slot, 1);
            client.deleteItem(itemId == 1540 ? 11286 : 1540, 1);
            client.deleteItem(995, 1_500_000);
            client.addItemSlot(11284, 1, slot);
            client.checkItemUpdate();
            client.showNPCChat(npcId, 591, new String[]{"Here you go.", "Your shield is done."});
            return;
        }

        /* Sheep shearing */
        if (npcId == 2794) {
            if (itemId == 1735) {
                client.addItem(1737, 1);
                client.checkItemUpdate();
            } else {
                client.send(new SendMessage("You need some shears to shear this sheep!"));
            }
            return;
        }

        /* Skillcape hood handing */
        Skillcape skillcape = Skillcape.getSkillCape(itemId);
        if (skillcape != null && npcId == 6059) {
            if (client.hasSpace()) {
                client.addItem(skillcape.getTrimmedId() + 1, 1);
                client.checkItemUpdate();
                client.showNPCChat(6059, 588, new String[]{"Here, have a skillcape hood from me."});
            } else {
                client.send(new SendMessage("Not enough of space to get a skillcape hood."));
            }
            return;
        }

        /* Max cape hood */
        boolean gotMaxCape = client.GetItemName(itemId).contains("Max cape");
        if (gotMaxCape && npcId == 6481) {
            if (client.hasSpace()) {
                client.addItem(itemId + 1, 1);
                client.checkItemUpdate();
                client.showNPCChat(6481, 588, new String[]{"Here, have a skillcape hood from me."});
            } else {
                client.send(new SendMessage("Not enough of space to get a skillcape hood."));
            }
        }
    }
}
