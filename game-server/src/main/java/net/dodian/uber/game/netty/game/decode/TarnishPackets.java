package net.dodian.uber.game.netty.game.decode;

import io.netty.buffer.ByteBuf;
import net.dodian.uber.game.netty.codec.ByteBufReader;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;

/**
 * Typed inbound packet definitions for the Tarnish 317 client.
 *
 * This is the single place where an opcode's wire format lives. Every decode
 * mirrors the exact write sequence in the Tarnish client's
 * {@code com.osroyale.Client} ({@code outgoing.writeOpcode(N)} blocks), using the
 * encoding key from {@code com.osroyale.Buffer}:
 *
 * <pre>
 *   writeShort    = BIG    + NORMAL
 *   writeShortA   = BIG    + ADD
 *   writeLEShort  = LITTLE + NORMAL
 *   writeLEShortA = LITTLE + ADD
 * </pre>
 *
 * Listeners should call these instead of hand-reading fields so a format fix
 * lands in exactly one place. Each decoder validates payload length and returns
 * {@code null} for malformed payloads — the caller drops the packet (never
 * disconnects; see PacketItemActionService for the policy rationale).
 */
public final class TarnishPackets {

    private TarnishPackets() {}

    /** Opcode 122 — first item option (wield/bury/eat). Client: LEShortA iface, ShortA slot, LEShort item. */
    public static final class ItemOption1 {
        private final int interfaceId;
        private final int slot;
        private final int itemId;

        public ItemOption1(int interfaceId, int slot, int itemId) {
            this.interfaceId = interfaceId;
            this.slot = slot;
            this.itemId = itemId;
        }

        public int interfaceId() {
            return interfaceId;
        }

        public int slot() {
            return slot;
        }

        public int itemId() {
            return itemId;
        }
        public static final int OPCODE = 122;
        public static final int SIZE = 6;

        public static ItemOption1 decode(ByteBuf buf) {
            if (buf.readableBytes() != SIZE) return null;
            int interfaceId = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.ADD);
            int slot = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);
            int itemId = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.NORMAL);
            return new ItemOption1(interfaceId, slot, itemId);
        }
    }

    /** Opcode 16 — second item option. Client: ShortA item, LEShortA slot, LEShortA iface. */
    public static final class ItemOption2 {
        private final int itemId;
        private final int slot;
        private final int interfaceId;

        public ItemOption2(int itemId, int slot, int interfaceId) {
            this.itemId = itemId;
            this.slot = slot;
            this.interfaceId = interfaceId;
        }

        public int itemId() {
            return itemId;
        }

        public int slot() {
            return slot;
        }

        public int interfaceId() {
            return interfaceId;
        }
        public static final int OPCODE = 16;
        public static final int SIZE = 6;

        public static ItemOption2 decode(ByteBuf buf) {
            if (buf.readableBytes() != SIZE) return null;
            int itemId = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);
            int slot = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.ADD);
            int interfaceId = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.ADD);
            return new ItemOption2(itemId, slot, interfaceId);
        }
    }

    /** Opcode 75 — third item option. Client: LEShortA iface, LEShort slot, ShortA item. */
    public static final class ItemOption3 {
        private final int interfaceId;
        private final int slot;
        private final int itemId;

        public ItemOption3(int interfaceId, int slot, int itemId) {
            this.interfaceId = interfaceId;
            this.slot = slot;
            this.itemId = itemId;
        }

        public int interfaceId() {
            return interfaceId;
        }

        public int slot() {
            return slot;
        }

        public int itemId() {
            return itemId;
        }
        public static final int OPCODE = 75;
        public static final int SIZE = 6;

        public static ItemOption3 decode(ByteBuf buf) {
            if (buf.readableBytes() != SIZE) return null;
            int interfaceId = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.ADD);
            int slot = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.NORMAL);
            int itemId = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);
            return new ItemOption3(interfaceId, slot, itemId);
        }
    }

    /** Opcode 41 — wield/wear item. Client: Short item, ShortA slot, ShortA iface. */
    public static final class WearItem {
        private final int itemId;
        private final int slot;
        private final int interfaceId;

        public WearItem(int itemId, int slot, int interfaceId) {
            this.itemId = itemId;
            this.slot = slot;
            this.interfaceId = interfaceId;
        }

        public int itemId() {
            return itemId;
        }

        public int slot() {
            return slot;
        }

        public int interfaceId() {
            return interfaceId;
        }
        public static final int OPCODE = 41;
        public static final int SIZE = 6;

        public static WearItem decode(ByteBuf buf) {
            if (buf.readableBytes() != SIZE) return null;
            int itemId = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.NORMAL);
            int slot = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);
            int interfaceId = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);
            return new WearItem(itemId, slot, interfaceId);
        }
    }

    /**
     * Opcode 53 — item on item. Client writes 6 shorts; only the two leading slots
     * carry gameplay meaning (matches Tarnish server's own decoder, which ignores
     * the trailing fields).
     */
    public static final class ItemOnItem {
        private final int usedWithSlot;
        private final int itemUsedSlot;

        public ItemOnItem(int usedWithSlot, int itemUsedSlot) {
            this.usedWithSlot = usedWithSlot;
            this.itemUsedSlot = itemUsedSlot;
        }

        public int usedWithSlot() {
            return usedWithSlot;
        }

        public int itemUsedSlot() {
            return itemUsedSlot;
        }
        public static final int OPCODE = 53;
        public static final int MIN_SIZE = 4;

        public static ItemOnItem decode(ByteBuf buf) {
            if (buf.readableBytes() < MIN_SIZE) return null;
            int usedWithSlot = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.NORMAL);
            int itemUsedSlot = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);
            return new ItemOnItem(usedWithSlot, itemUsedSlot);
        }
    }

    /**
     * Opcode 192 — item on object. Client: Short ifaceType, LEShort objectId,
     * LEShortA y, LEShort slot, LEShortA x, Short itemId (mirrors Tarnish server's
     * UseItemPacketListener.handleItemOnObject).
     */
    public static final class ItemOnObject {
        private final int interfaceId;
        private final int objectId;
        private final int objectY;
        private final int slot;
        private final int objectX;
        private final int itemId;

        public ItemOnObject(int interfaceId, int objectId, int objectY, int slot, int objectX, int itemId) {
            this.interfaceId = interfaceId;
            this.objectId = objectId;
            this.objectY = objectY;
            this.slot = slot;
            this.objectX = objectX;
            this.itemId = itemId;
        }

        public int interfaceId() {
            return interfaceId;
        }

        public int objectId() {
            return objectId;
        }

        public int objectY() {
            return objectY;
        }

        public int slot() {
            return slot;
        }

        public int objectX() {
            return objectX;
        }

        public int itemId() {
            return itemId;
        }
        public static final int OPCODE = 192;
        public static final int SIZE = 12;

        public static ItemOnObject decode(ByteBuf buf) {
            if (buf.readableBytes() != SIZE) return null;
            int interfaceId = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.NORMAL);
            int objectId = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.NORMAL);
            int objectY = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.ADD);
            int slot = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.NORMAL);
            int objectX = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.ADD);
            int itemId = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.NORMAL);
            return new ItemOnObject(interfaceId, objectId, objectY, slot, objectX, itemId);
        }
    }

    /** Opcode 87 — drop item. Client: ShortA item, Short iface, ShortA slot. */
    public static final class DropItem {
        private final int itemId;
        private final int interfaceId;
        private final int slot;

        public DropItem(int itemId, int interfaceId, int slot) {
            this.itemId = itemId;
            this.interfaceId = interfaceId;
            this.slot = slot;
        }

        public int itemId() {
            return itemId;
        }

        public int interfaceId() {
            return interfaceId;
        }

        public int slot() {
            return slot;
        }
        public static final int OPCODE = 87;
        public static final int SIZE = 6;

        public static DropItem decode(ByteBuf buf) {
            if (buf.readableBytes() != SIZE) return null;
            int itemId = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);
            int interfaceId = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.NORMAL);
            int slot = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);
            return new DropItem(itemId, interfaceId, slot);
        }
    }

    /** Opcode 57 — use item on npc. Client: ShortA item, ShortA npcIndex, LEShort slot, ShortA iface. */
    public static final class ItemOnNpc {
        private final int itemId;
        private final int npcIndex;
        private final int slot;
        private final int interfaceId;

        public ItemOnNpc(int itemId, int npcIndex, int slot, int interfaceId) {
            this.itemId = itemId;
            this.npcIndex = npcIndex;
            this.slot = slot;
            this.interfaceId = interfaceId;
        }

        public int itemId() {
            return itemId;
        }

        public int npcIndex() {
            return npcIndex;
        }

        public int slot() {
            return slot;
        }

        public int interfaceId() {
            return interfaceId;
        }
        public static final int OPCODE = 57;
        public static final int SIZE = 8;

        public static ItemOnNpc decode(ByteBuf buf) {
            if (buf.readableBytes() != SIZE) return null;
            int itemId = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);
            int npcIndex = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);
            int slot = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.NORMAL);
            int interfaceId = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);
            return new ItemOnNpc(itemId, npcIndex, slot, interfaceId);
        }
    }

    /** Opcode 14 — use item on player. Client: ShortA iface, Short playerIndex, Short item, LEShort slot. */
    public static final class ItemOnPlayer {
        private final int interfaceId;
        private final int playerIndex;
        private final int itemId;
        private final int slot;

        public ItemOnPlayer(int interfaceId, int playerIndex, int itemId, int slot) {
            this.interfaceId = interfaceId;
            this.playerIndex = playerIndex;
            this.itemId = itemId;
            this.slot = slot;
        }

        public int interfaceId() {
            return interfaceId;
        }

        public int playerIndex() {
            return playerIndex;
        }

        public int itemId() {
            return itemId;
        }

        public int slot() {
            return slot;
        }
        public static final int OPCODE = 14;
        public static final int SIZE = 8;

        public static ItemOnPlayer decode(ByteBuf buf) {
            if (buf.readableBytes() != SIZE) return null;
            int interfaceId = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);
            int playerIndex = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.NORMAL);
            int itemId = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.NORMAL);
            int slot = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.NORMAL);
            return new ItemOnPlayer(interfaceId, playerIndex, itemId, slot);
        }
    }

    /** Opcode 237 — magic on inventory item. Client: Short slot, ShortA item, Short iface, ShortA spell. */
    public static final class MagicOnItem {
        private final int slot;
        private final int itemId;
        private final int interfaceId;
        private final int spellId;

        public MagicOnItem(int slot, int itemId, int interfaceId, int spellId) {
            this.slot = slot;
            this.itemId = itemId;
            this.interfaceId = interfaceId;
            this.spellId = spellId;
        }

        public int slot() {
            return slot;
        }

        public int itemId() {
            return itemId;
        }

        public int interfaceId() {
            return interfaceId;
        }

        public int spellId() {
            return spellId;
        }
        public static final int OPCODE = 237;
        public static final int SIZE = 8;

        public static MagicOnItem decode(ByteBuf buf) {
            if (buf.readableBytes() != SIZE) return null;
            int slot = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.NORMAL);
            int itemId = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);
            int interfaceId = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.NORMAL);
            int spellId = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);
            return new MagicOnItem(slot, itemId, interfaceId, spellId);
        }
    }

    /** Opcodes 248/164/98 — walk (game screen/minimap/command). Client: LEShort x, LEShortA y, negated byte run. */
    public static final class Walk {
        private final int targetX;
        private final int targetY;
        private final boolean running;

        public Walk(int targetX, int targetY, boolean running) {
            this.targetX = targetX;
            this.targetY = targetY;
            this.running = running;
        }

        public int targetX() {
            return targetX;
        }

        public int targetY() {
            return targetY;
        }

        public boolean running() {
            return running;
        }

        public static final int SIZE = 5;

        public static Walk decode(ByteBuf buf) {
            if (buf.readableBytes() != SIZE) return null;
            int targetX = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.NORMAL);
            int targetY = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.ADD);
            boolean running = ByteBufReader.readSignedByte(buf, ValueType.NEGATE) == 1;
            return new Walk(targetX, targetY, running);
        }
    }

    /** Opcode 185 — interface button click. Client: Short buttonId. (186 is never sent by the client.) */
    public static final class ButtonClick {
        private final int buttonId;

        public ButtonClick(int buttonId) {
            this.buttonId = buttonId;
        }

        public int buttonId() {
            return buttonId;
        }

        public static final int OPCODE = 185;
        public static final int SIZE = 2;

        public static ButtonClick decode(ByteBuf buf) {
            if (buf.readableBytes() != SIZE) return null;
            return new ButtonClick(ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.NORMAL));
        }
    }

    /**
     * Object click options 1-5. Per-opcode field order/encoding (verified against the client):
     * 132: LEShortA x, Short id, ShortA y | 252: LEShortA id, LEShort y, ShortA x |
     * 70: LEShort x, Short y, LEShortA id | 234: LEShortA x, ShortA id, LEShortA y |
     * 228: ShortA id, ShortA y, Short x.
     */
    public static final class ObjectClick {
        private final int objectId;
        private final int x;
        private final int y;

        public ObjectClick(int objectId, int x, int y) {
            this.objectId = objectId;
            this.x = x;
            this.y = y;
        }

        public int objectId() {
            return objectId;
        }

        public int x() {
            return x;
        }

        public int y() {
            return y;
        }

        public static final int SIZE = 6;

        public static ObjectClick decode(int opcode, ByteBuf buf) {
            if (buf.readableBytes() != SIZE) return null;
            int objectId;
            int x;
            int y;
            switch (opcode) {
                case 132:
                    x = ByteBufReader.readShortSigned(buf, ByteOrder.LITTLE, ValueType.ADD);
                    objectId = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.NORMAL);
                    y = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);
                    break;
                case 252:
                    objectId = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.ADD);
                    y = ByteBufReader.readShortSigned(buf, ByteOrder.LITTLE, ValueType.NORMAL);
                    x = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);
                    break;
                case 70:
                    x = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.NORMAL);
                    y = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.NORMAL);
                    objectId = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.ADD);
                    break;
                case 234:
                    x = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.ADD);
                    objectId = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);
                    y = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.ADD);
                    break;
                case 228:
                    objectId = ByteBufReader.readShortSigned(buf, ByteOrder.BIG, ValueType.ADD);
                    y = ByteBufReader.readShortSigned(buf, ByteOrder.BIG, ValueType.ADD);
                    x = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.NORMAL);
                    break;
                default:
                    return null;
            }
            return new ObjectClick(objectId, x, y);
        }
    }

    /** Opcode 35 — magic on object. Client: LEShort x, ShortA spell, ShortA y, LEShort objectId. */
    public static final class MagicOnObject {
        private final int x;
        private final int spellId;
        private final int y;
        private final int objectId;

        public MagicOnObject(int x, int spellId, int y, int objectId) {
            this.x = x;
            this.spellId = spellId;
            this.y = y;
            this.objectId = objectId;
        }

        public int x() {
            return x;
        }

        public int spellId() {
            return spellId;
        }

        public int y() {
            return y;
        }

        public int objectId() {
            return objectId;
        }

        public static final int OPCODE = 35;
        public static final int SIZE = 8;

        public static MagicOnObject decode(ByteBuf buf) {
            if (buf.readableBytes() != SIZE) return null;
            int x = ByteBufReader.readShortSigned(buf, ByteOrder.LITTLE, ValueType.NORMAL);
            int spellId = ByteBufReader.readShortSigned(buf, ByteOrder.BIG, ValueType.ADD);
            int y = ByteBufReader.readShortSigned(buf, ByteOrder.BIG, ValueType.ADD);
            int objectId = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.NORMAL);
            return new MagicOnObject(x, spellId, y, objectId);
        }
    }

    /**
     * NPC click options (single npc slot index). Per-opcode encoding:
     * 155: LEShort | 17: LEShortA | 21: Short | 18: LEShort | 72 (attack): ShortA.
     */
    public static final class NpcClick {
        private final int npcIndex;

        public NpcClick(int npcIndex) {
            this.npcIndex = npcIndex;
        }

        public int npcIndex() {
            return npcIndex;
        }

        public static final int SIZE = 2;

        public static NpcClick decode(int opcode, ByteBuf buf) {
            if (buf.readableBytes() != SIZE) return null;
            int npcIndex;
            switch (opcode) {
                case 155:
                case 18:
                    npcIndex = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.NORMAL);
                    break;
                case 17:
                    npcIndex = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.ADD);
                    break;
                case 21:
                    npcIndex = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.NORMAL);
                    break;
                case 72:
                    npcIndex = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);
                    break;
                default:
                    return null;
            }
            return new NpcClick(npcIndex);
        }
    }

    /**
     * Player-menu clicks carrying a single player slot index. Per-opcode encoding:
     * 73 (attack): LEShort | 139: LEShort | 128: Short | 39: LEShort | 153: LEShort.
     *
     * Active semantic routing is 39=Trade, 139=Follow, and 153=Duel. Opcode 128 is
     * retained for the legacy slot-1 action and its big-endian encoding.
     */
    public static final class PlayerMenuClick {
        private final int playerIndex;

        public PlayerMenuClick(int playerIndex) {
            this.playerIndex = playerIndex;
        }

        public int playerIndex() {
            return playerIndex;
        }

        public static final int SIZE = 2;

        public static PlayerMenuClick decode(int opcode, ByteBuf buf) {
            if (buf.readableBytes() != SIZE) return null;
            int playerIndex;
            switch (opcode) {
                case 73:
                case 139:
                case 39:
                case 153:
                    playerIndex = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.NORMAL);
                    break;
                case 128:
                    playerIndex = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.NORMAL);
                    break;
                default:
                    return null;
            }
            return new PlayerMenuClick(playerIndex);
        }
    }

    /** Opcode 131 — magic on npc. Client: LEShortA npcIndex, ShortA spellId. */
    public static final class MagicOnNpc {
        private final int npcIndex;
        private final int spellId;

        public MagicOnNpc(int npcIndex, int spellId) {
            this.npcIndex = npcIndex;
            this.spellId = spellId;
        }

        public int npcIndex() {
            return npcIndex;
        }

        public int spellId() {
            return spellId;
        }

        public static final int OPCODE = 131;
        public static final int SIZE = 4;

        public static MagicOnNpc decode(ByteBuf buf) {
            if (buf.readableBytes() != SIZE) return null;
            int npcIndex = ByteBufReader.readShortSigned(buf, ByteOrder.LITTLE, ValueType.ADD);
            int spellId = ByteBufReader.readShortSigned(buf, ByteOrder.BIG, ValueType.ADD);
            return new MagicOnNpc(npcIndex, spellId);
        }
    }

    /** Opcode 249 — magic on player. Client: ShortA playerIndex, LEShort spellId. */
    public static final class MagicOnPlayer {
        private final int playerIndex;
        private final int spellId;

        public MagicOnPlayer(int playerIndex, int spellId) {
            this.playerIndex = playerIndex;
            this.spellId = spellId;
        }

        public int playerIndex() {
            return playerIndex;
        }

        public int spellId() {
            return spellId;
        }

        public static final int OPCODE = 249;
        public static final int SIZE = 4;

        public static MagicOnPlayer decode(ByteBuf buf) {
            if (buf.readableBytes() != SIZE) return null;
            int playerIndex = ByteBufReader.readShortSigned(buf, ByteOrder.BIG, ValueType.ADD);
            int spellId = ByteBufReader.readShortSigned(buf, ByteOrder.LITTLE, ValueType.NORMAL);
            return new MagicOnPlayer(playerIndex, spellId);
        }
    }

    /**
     * Bank quantity-preset actions (iface, itemId, slot). Per-opcode field order/encoding:
     * 43 (bank 10): LEShort iface, ShortA id, ShortA slot |
     * 117 (bank 5): LEShortA iface, LEShortA id, LEShort slot |
     * 129 (bank all): LEShort iface, ShortA id, ShortA slot |
     * 140 (all-but-one): ShortA slot, Short iface, ShortA id |
     * 135 (bank X part 1): LEShort slot, ShortA iface, LEShort id.
     */
    public static final class BankPresetAction {
        private final int interfaceId;
        private final int itemId;
        private final int slot;

        public BankPresetAction(int interfaceId, int itemId, int slot) {
            this.interfaceId = interfaceId;
            this.itemId = itemId;
            this.slot = slot;
        }

        public int interfaceId() {
            return interfaceId;
        }

        public int itemId() {
            return itemId;
        }

        public int slot() {
            return slot;
        }

        public static final int SIZE = 6;

        public static BankPresetAction decode(int opcode, ByteBuf buf) {
            if (buf.readableBytes() != SIZE) return null;
            int interfaceId;
            int itemId;
            int slot;
            switch (opcode) {
                case 43:
                case 129:
                    interfaceId = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.NORMAL);
                    itemId = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);
                    slot = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);
                    break;
                case 117:
                    interfaceId = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.ADD);
                    itemId = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.ADD);
                    slot = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.NORMAL);
                    break;
                case 140:
                    slot = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);
                    interfaceId = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.NORMAL);
                    itemId = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);
                    break;
                case 135:
                    slot = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.NORMAL);
                    interfaceId = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);
                    itemId = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.NORMAL);
                    break;
                default:
                    return null;
            }
            return new BankPresetAction(interfaceId, itemId, slot);
        }
    }

    /** Opcode 141 — withdraw remembered X. Client: ShortA slot, Short iface, ShortA id, DWord amount. */
    public static final class BankWithdrawRememberedX {
        private final int slot;
        private final int interfaceId;
        private final int itemId;
        private final int amount;

        public BankWithdrawRememberedX(int slot, int interfaceId, int itemId, int amount) {
            this.slot = slot;
            this.interfaceId = interfaceId;
            this.itemId = itemId;
            this.amount = amount;
        }

        public int slot() {
            return slot;
        }

        public int interfaceId() {
            return interfaceId;
        }

        public int itemId() {
            return itemId;
        }

        public int amount() {
            return amount;
        }

        public static final int OPCODE = 141;
        public static final int SIZE = 10;

        public static BankWithdrawRememberedX decode(ByteBuf buf) {
            if (buf.readableBytes() != SIZE) return null;
            int slot = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);
            int interfaceId = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.NORMAL);
            int itemId = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.ADD);
            int amount = buf.readInt();
            return new BankWithdrawRememberedX(slot, interfaceId, itemId, amount);
        }
    }

    /** Opcode 208 — entered amount for a pending X action. Client: DWord amount. */
    public static final class EnteredAmount {
        private final int amount;

        public EnteredAmount(int amount) {
            this.amount = amount;
        }

        public int amount() {
            return amount;
        }

        public static final int OPCODE = 208;
        public static final int SIZE = 4;

        public static EnteredAmount decode(ByteBuf buf) {
            if (buf.readableBytes() != SIZE) return null;
            return new EnteredAmount(buf.readInt());
        }
    }

    /** Opcode 214 — move/swap container items. Client: LEShortA iface, negated byte mode, LEShortA from, LEShort to. */
    public static final class MoveItems {
        private final int interfaceId;
        private final int mode;
        private final int fromSlot;
        private final int toSlot;

        public MoveItems(int interfaceId, int mode, int fromSlot, int toSlot) {
            this.interfaceId = interfaceId;
            this.mode = mode;
            this.fromSlot = fromSlot;
            this.toSlot = toSlot;
        }

        public int interfaceId() {
            return interfaceId;
        }

        public int mode() {
            return mode;
        }

        public int fromSlot() {
            return fromSlot;
        }

        public int toSlot() {
            return toSlot;
        }

        public static final int OPCODE = 214;
        public static final int SIZE = 7;

        public static MoveItems decode(ByteBuf buf) {
            if (buf.readableBytes() != SIZE) return null;
            int interfaceId = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.ADD);
            int mode = ByteBufReader.readSignedByte(buf, ValueType.NEGATE);
            int fromSlot = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.ADD);
            int toSlot = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.NORMAL);
            return new MoveItems(interfaceId, mode, fromSlot, toSlot);
        }
    }

    /** Opcode 236 — pick up ground item. Client: LEShort y, Short itemId, LEShort x. */
    public static final class PickupGroundItem {
        private final int y;
        private final int itemId;
        private final int x;

        public PickupGroundItem(int y, int itemId, int x) {
            this.y = y;
            this.itemId = itemId;
            this.x = x;
        }

        public int y() {
            return y;
        }

        public int itemId() {
            return itemId;
        }

        public int x() {
            return x;
        }

        public static final int OPCODE = 236;
        public static final int SIZE = 6;

        public static PickupGroundItem decode(ByteBuf buf) {
            if (buf.readableBytes() != SIZE) return null;
            int y = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.NORMAL);
            int itemId = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.NORMAL);
            int x = ByteBufReader.readShortUnsigned(buf, ByteOrder.LITTLE, ValueType.NORMAL);
            return new PickupGroundItem(y, itemId, x);
        }
    }

    /** Opcode 150 — examine. Client: Byte type, Short id, Short param1, Short param2. */
    public static final class Examine {
        private final int type;
        private final int id;
        private final int param1;
        private final int param2;

        public Examine(int type, int id, int param1, int param2) {
            this.type = type;
            this.id = id;
            this.param1 = param1;
            this.param2 = param2;
        }

        public int type() {
            return type;
        }

        public int id() {
            return id;
        }

        public int param1() {
            return param1;
        }

        public int param2() {
            return param2;
        }

        public static final int OPCODE = 150;
        public static final int SIZE = 7;

        public static Examine decode(ByteBuf buf) {
            if (buf.readableBytes() != SIZE) return null;
            int type = buf.readUnsignedByte();
            int id = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.NORMAL);
            int param1 = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.NORMAL);
            int param2 = ByteBufReader.readShortUnsigned(buf, ByteOrder.BIG, ValueType.NORMAL);
            return new Examine(type, id, param1, param2);
        }
    }

    /**
     * Encoded-name payloads (a single QWord player name): 188 add friend, 133 add ignore,
     * 215 remove friend, 74 remove ignore, 60 syntax input.
     */
    public static final class EncodedName {
        private final long name;

        public EncodedName(long name) {
            this.name = name;
        }

        public long name() {
            return name;
        }

        public static final int SIZE = 8;

        public static EncodedName decode(ByteBuf buf) {
            if (buf.readableBytes() < SIZE) return null;
            return new EncodedName(buf.readLong());
        }
    }

    /** Opcode 101 — appearance change. Client: 13 plain bytes (gender, 7 styles, 5 colors). */
    public static final class AppearanceChange {
        private final int gender;
        private final int[] styles;
        private final int[] colors;

        public AppearanceChange(int gender, int[] styles, int[] colors) {
            this.gender = gender;
            this.styles = styles;
            this.colors = colors;
        }

        public int gender() {
            return gender;
        }

        public int[] styles() {
            return styles;
        }

        public int[] colors() {
            return colors;
        }

        public static final int OPCODE = 101;
        public static final int SIZE = 13;

        public static AppearanceChange decode(ByteBuf buf) {
            if (buf.readableBytes() != SIZE) return null;
            int gender = buf.readUnsignedByte();
            int[] styles = new int[7];
            for (int i = 0; i < styles.length; i++) {
                styles[i] = buf.readUnsignedByte();
            }
            int[] colors = new int[5];
            for (int i = 0; i < colors.length; i++) {
                colors[i] = buf.readUnsignedByte();
            }
            return new AppearanceChange(gender, styles, colors);
        }
    }

    /** Opcode 241 — mouse click telemetry. Client: DWord ((pos&lt;&lt;20) | (mode&lt;&lt;19) | time). */
    public static final class MouseClick {
        private final int packed;

        public MouseClick(int packed) {
            this.packed = packed;
        }

        public int packed() {
            return packed;
        }

        public static final int OPCODE = 241;
        public static final int SIZE = 4;

        public static MouseClick decode(ByteBuf buf) {
            if (buf.readableBytes() < SIZE) return null;
            return new MouseClick(buf.readInt());
        }
    }

    /** Opcode 3 — window focus change. Client: Byte (1 focused, 0 unfocused). */
    public static final class FocusChange {
        private final boolean focused;

        public FocusChange(boolean focused) {
            this.focused = focused;
        }

        public boolean focused() {
            return focused;
        }

        public static final int OPCODE = 3;
        public static final int SIZE = 1;

        public static FocusChange decode(ByteBuf buf) {
            if (buf.readableBytes() < SIZE) return null;
            return new FocusChange(buf.readUnsignedByte() == 1);
        }
    }

    /**
     * Opcode 255 — dropdown menu selection. UNVERIFIED: no writeOpcode(255) site was located in
     * the Tarnish client during the audit; this mirrors the pre-existing server decode as-is.
     */
    public static final class DropdownSelect {
        private final int componentId;
        private final int value;

        public DropdownSelect(int componentId, int value) {
            this.componentId = componentId;
            this.value = value;
        }

        public int componentId() {
            return componentId;
        }

        public int value() {
            return value;
        }

        public static final int OPCODE = 255;
        public static final int MIN_SIZE = 5;

        public static DropdownSelect decode(ByteBuf buf) {
            if (buf.readableBytes() < MIN_SIZE) return null;
            int componentId = buf.readInt();
            int value = buf.readByte();
            return new DropdownSelect(componentId, value);
        }
    }

    // ------------------------------------------------------------------------------------------
    // Opcodes intentionally NOT modeled here:
    //  - 0 keepalive; 45/86/120/202 no-ops; 23/79/136/149/156/181/187/218/253 unsupported by
    //    this server (see their listeners).
    //  - 121/210 (region change ack), 40 (dialogue advance), 130 (interface closed): the client
    //    sends a small payload the server deliberately ignores — NO_PAYLOAD semantics.
    //  - 186: the client never writes it; ClickingButtonsListener's 186 branch is server-only.
    //  - Variable-length text opcodes 4 (public chat), 126 (private message): wire format lives
    //    in their dedicated codecs (PublicChatCodec / TextInput packing) — single-sourced there.
    //  - 103 (command), 26 (npc drop lookup), 142 (input field): length-prefixed/terminated
    //    strings decoded via ByteBufReader.readTerminatedString in their listeners.
    // ------------------------------------------------------------------------------------------
}
