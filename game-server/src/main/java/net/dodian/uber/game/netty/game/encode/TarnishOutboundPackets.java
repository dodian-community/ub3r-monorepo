package net.dodian.uber.game.netty.game.encode;

import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.MessageType;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;

/**
 * Central definitions for outbound packets (server to client) for the Tarnish 317 client.
 *
 * Each nested class defines a packet type, matching the exact read sequence in the Tarnish client's
 * {@code com.osroyale.Client} ({@code incoming.read*()} blocks).
 */
public final class TarnishOutboundPackets {

    private TarnishOutboundPackets() {}

    /**
     * VAR_SHORT packets carry a 2-byte length prefix (max 0xffff). A payload that overflows it
     * would silently truncate/wrap the length the client reads, corrupting the frame boundary
     * for every packet that follows on the connection — so this must throw, not clamp. The
     * buffer is released before throwing to avoid leaking it (callers won't get to release it).
     */
    private static void validateVarShortPayloadSize(ByteMessage message) {
        if (message.getBuffer().writerIndex() > 0xffff) {
            message.releaseAll();
            throw new IllegalArgumentException("Tarnish VAR_SHORT payload exceeds variable-short limit");
        }
    }

    /** Opcode 253 - Send message. Client: reads String, then 1-byte filter boolean. */
    public static final class SendMessage {
        public static final int OPCODE = 253;
        public static final MessageType TYPE = MessageType.VAR;

        private final String message;
        private final boolean filtered;

        public SendMessage(String message, boolean filtered) {
            this.message = message;
            this.filtered = filtered;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.putString(this.message);
            message.put(filtered ? 1 : 0);
            return message;
        }
    }

    /** Opcode 107 - Camera Reset. Client: locks/unlocks camera, no payload read. */
    public static final class CameraReset {
        public static final int OPCODE = 107;
        public static final MessageType TYPE = MessageType.FIXED;

        public ByteMessage encode() {
            return ByteMessage.message(OPCODE, TYPE);
        }
    }

    /** Opcode 249 - Player details. Client: readUByteA memberFlag, readLEUShortA slot. */
    public static final class PlayerDetails {
        public static final int OPCODE = 249;
        public static final MessageType TYPE = MessageType.FIXED;

        private final int memberFlag;
        private final int slot;

        public PlayerDetails(int memberFlag, int slot) {
            this.memberFlag = memberFlag;
            this.slot = slot;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.put(memberFlag, ValueType.ADD);
            message.putShort(slot, ByteOrder.LITTLE, ValueType.ADD);
            return message;
        }
    }

    /**
     * Opcode 114 - Game message overlay. Client reads: readUnsignedShort id, readUnsignedShort time, readString context.
     * id=0 renders: context + " " + getFormattedTime(time) in yellow above chatbox. time is in frames (50fps).
     */
    public static final class GameMessage {
        public static final int OPCODE = 114;
        public static final MessageType TYPE = MessageType.VAR;

        private final int id;
        private final int time;
        private final String context;

        public GameMessage(int id, int time, String context) {
            this.id = id;
            this.time = time;
            this.context = context;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.putShort(id);
            message.putShort(time);
            message.putString(context);
            return message;
        }
    }

    /** Opcode 110 - Send Run Energy. Client: readUnsignedByte energy. */
    public static final class SendRunEnergy {
        public static final int OPCODE = 110;
        public static final MessageType TYPE = MessageType.FIXED;

        private final int energy;

        public SendRunEnergy(int energy) {
            this.energy = energy;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.put(energy & 0xFF);
            return message;
        }
    }

    /** Opcode 106 - Send Side Tab. Client: readNegUByte tabId. */
    public static final class SendSideTab {
        public static final int OPCODE = 106;
        public static final MessageType TYPE = MessageType.FIXED;

        private final int tabId;

        public SendSideTab(int tabId) {
            this.tabId = tabId;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.put(tabId, ValueType.NEGATE);
            return message;
        }
    }

    /** Opcode 126 - Send String. Client: readString text, readUnsignedInt (4-byte) id. */
    public static final class SendString {
        public static final int OPCODE = 126;
        public static final MessageType TYPE = MessageType.VAR_SHORT;

        private final String string;
        private final int lineId;

        public SendString(String string, int lineId) {
            this.string = string;
            this.lineId = lineId;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.putString(string);
            message.putInt(lineId);
            return message;
        }
    }

    /** Opcode 97 - Show Interface. Client: readUnsignedInt (4-byte) interfaceID. */
    public static final class ShowInterface {
        public static final int OPCODE = 97;
        public static final MessageType TYPE = MessageType.FIXED;

        private final int interfaceId;

        public ShowInterface(int interfaceId) {
            this.interfaceId = interfaceId;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.putInt(interfaceId);
            return message;
        }
    }

    /** Opcode 219 - Remove Interfaces. Client: readUnsignedByte closeInterface. */
    public static final class RemoveInterfaces {
        public static final int OPCODE = 219;
        public static final MessageType TYPE = MessageType.FIXED;

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.put(1);
            return message;
        }
    }

    /** Opcode 71 - Set Sidebar Interface. Client: readUnsignedShort form, readUByteA menuId. */
    public static final class SetSidebarInterface {
        public static final int OPCODE = 71;
        public static final MessageType TYPE = MessageType.FIXED;

        private final int menuId;
        private final int form;

        public SetSidebarInterface(int menuId, int form) {
            this.menuId = menuId;
            this.form = form;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.putShort(form);
            message.put(menuId, ValueType.ADD);
            return message;
        }
    }

    /** Opcode 85 - Set Map. Client: readNegUByte localY, readNegUByte localX. */
    public static final class SetMap {
        public static final int OPCODE = 85;
        public static final MessageType TYPE = MessageType.FIXED;

        private final int localX;
        private final int localY;

        public SetMap(int localX, int localY) {
            this.localX = localX;
            this.localY = localY;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.put(localY, ValueType.NEGATE);
            message.put(localX, ValueType.NEGATE);
            return message;
        }
    }

    /** Opcode 4 - Animation 2. Client: readUnsignedByte j2, readUnsignedShort k10, readUnsignedByte l12, readUnsignedShort j15. */
    public static final class Animation2 {
        public static final int OPCODE = 4;
        public static final MessageType TYPE = MessageType.FIXED;

        private final int offsetByte;
        private final int animationId;
        private final int height;
        private final int delay;

        public Animation2(int offsetByte, int animationId, int height, int delay) {
            this.offsetByte = offsetByte;
            this.animationId = animationId;
            this.height = height;
            this.delay = delay;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.put(offsetByte);
            message.putShort(animationId);
            message.put(height);
            message.putShort(delay);
            return message;
        }
    }

    /** Opcode 75 - NPC dialogue head. Client: readLEUShortA monster, readLEUShortA interfaceId. */
    public static final class NpcDialogueHead {
        public static final int OPCODE = 75;
        public static final MessageType TYPE = MessageType.FIXED;

        private final int mainFrame;
        private final int subFrame;

        public NpcDialogueHead(int mainFrame, int subFrame) {
            this.mainFrame = mainFrame;
            this.subFrame = subFrame;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.putShort(mainFrame, ByteOrder.LITTLE, ValueType.ADD);
            message.putShort(subFrame, ByteOrder.LITTLE, ValueType.ADD);
            return message;
        }
    }

    /** Opcode 185 - Player dialogue head. Client: readLEUShortA mainFrame. */
    public static final class PlayerDialogueHead {
        public static final int OPCODE = 185;
        public static final MessageType TYPE = MessageType.FIXED;

        private final int mainFrame;

        public PlayerDialogueHead(int mainFrame) {
            this.mainFrame = mainFrame;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.putShort(mainFrame, ByteOrder.LITTLE, ValueType.ADD);
            return message;
        }
    }

    /** Opcode 196 - Private message. Client: readUnsignedLong recipient, readUnsignedInt messageId, readUnsignedByte rights, text decode. */
    public static final class PrivateMessage {
        public static final int OPCODE = 196;
        public static final MessageType TYPE = MessageType.VAR;

        private final long recipient;
        private final int rights;
        private final byte[] message;
        private final int size;
        private final int messageId;

        public PrivateMessage(long recipient, int rights, byte[] message, int size, int messageId) {
            this.recipient = recipient;
            this.rights = rights;
            this.message = message;
            this.size = size;
            this.messageId = messageId;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.putLong(recipient);
            message.putInt(messageId);
            message.put(rights);
            for (int i = 0; i < size; i++) {
                message.put(this.message[i]);
            }
            message.put(10);
            return message;
        }
    }

    /** Opcode 221 - Private message status. Client: readUnsignedByte status. */
    public static final class PrivateMessageStatus {
        public static final int OPCODE = 221;
        public static final MessageType TYPE = MessageType.FIXED;

        private final int status;

        public PrivateMessageStatus(int status) {
            this.status = status;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.put(status);
            return message;
        }
    }

    /** Opcode 164 - Send chatbox interface. Client: readLEUShort frame. */
    public static final class SendChatboxInterface {
        public static final int OPCODE = 164;
        public static final MessageType TYPE = MessageType.FIXED;

        private final int frame;

        public SendChatboxInterface(int frame) {
            this.frame = frame;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.putShort(frame, ByteOrder.LITTLE);
            return message;
        }
    }

    /** Opcode 53 - Update Item Container. Client: readInt interface, readUnsignedShort count, readUnsignedShort tabCount, loop elements. */
    public static final class UpdateItemContainer {
        public static final int OPCODE = 53;
        public static final MessageType TYPE = MessageType.VAR_SHORT;

        private final int interfaceId;
        private final int[] itemIds;
        private final int[] amounts;
        private final int[] tabAmounts;
        private final boolean preserveZeroAmounts;

        public UpdateItemContainer(int interfaceId, int[] itemIds, int[] amounts, int[] tabAmounts, boolean preserveZeroAmounts) {
            this.interfaceId = interfaceId;
            this.itemIds = itemIds;
            this.amounts = amounts;
            this.tabAmounts = tabAmounts;
            this.preserveZeroAmounts = preserveZeroAmounts;
            for (int i = 0; i < itemIds.length; i++) {
                if (amounts[i] < 0) {
                    throw new IllegalArgumentException("Negative item amount: " + amounts[i]);
                }
                if (itemIds[i] >= 0xffff) {
                    throw new IllegalArgumentException("Item ID cannot be encoded for Tarnish: " + itemIds[i]);
                }
            }
            if (tabAmounts != null) {
                for (int amount : tabAmounts) {
                    if (amount < 0) {
                        throw new IllegalArgumentException("Negative bank-tab amount: " + amount);
                    }
                }
            }
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.putInt(interfaceId);
            message.putShort(itemIds.length);
            message.putShort(tabAmounts == null ? 0 : tabAmounts.length);
            for (int slot = 0; slot < itemIds.length; slot++) {
                int itemId = itemIds[slot];
                int amount = amounts[slot];
                if (itemId < 0 || (amount == 0 && !preserveZeroAmounts)) {
                    message.put(0);
                    message.putShort(0);
                } else {
                    if (amount > 254) {
                        message.put(255);
                        message.putInt(amount);
                    } else {
                        message.put(amount);
                    }
                    message.putShort(itemId + 1);
                }
            }
            if (tabAmounts != null) {
                for (int amount : tabAmounts) {
                    message.putInt(amount);
                }
            }
            validateVarShortPayloadSize(message);
            return message;
        }
    }

    /** Opcode 34 - Update Item Slot. Client: readUnsignedShort interface, readUnsignedShort slot, readUnsignedShort itemId, loop/write amount. */
    public static final class UpdateItemSlot {
        public static final int OPCODE = 34;
        public static final MessageType TYPE = MessageType.VAR_SHORT;

        private final int interfaceId;
        private final int slot;
        private final int itemId;
        private final int amount;

        public UpdateItemSlot(int interfaceId, int slot, int itemId, int amount) {
            if (amount < 0) {
                throw new IllegalArgumentException("Negative item amount: " + amount);
            }
            if (itemId >= 0xffff) {
                throw new IllegalArgumentException("Item ID cannot be encoded for Tarnish: " + itemId);
            }
            this.interfaceId = interfaceId;
            this.slot = slot;
            this.itemId = itemId;
            this.amount = amount;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.putShort(interfaceId);
            message.putShort(slot);
            boolean empty = itemId < 0 || amount == 0;
            message.putShort(empty ? 0 : itemId + 1);
            int val = empty ? 0 : amount;
            if (val > 254) {
                message.put(255);
                message.putInt(val);
            } else {
                message.put(val);
            }
            validateVarShortPayloadSize(message);
            return message;
        }
    }

    /** Opcode 171 - Set Interface Config. Client: readUnsignedByte state, readUnsignedShort interfaceId. */
    public static final class SetInterfaceConfig {
        public static final int OPCODE = 171;
        public static final MessageType TYPE = MessageType.FIXED;

        private final int state;
        private final int interfaceId;

        public SetInterfaceConfig(int state, int interfaceId) {
            this.state = state;
            this.interfaceId = interfaceId;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.put(state);
            message.putShort(interfaceId);
            return message;
        }
    }

    /** Opcode 248 - Set Inventory Interface. Client: readUnsignedInt interfaceId, readUnsignedShort inventoryId. */
    public static final class SetInventoryInterface {
        public static final int OPCODE = 248;
        public static final MessageType TYPE = MessageType.FIXED;

        private final int interfaceId;
        private final int inventoryId;

        public SetInventoryInterface(int interfaceId, int inventoryId) {
            this.interfaceId = interfaceId;
            this.inventoryId = inventoryId;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.putInt(interfaceId);
            message.putShort(inventoryId);
            return message;
        }
    }

    /** Opcode 50 - Load Private Message. Client: readUnsignedLong name, readUnsignedByte world, readUnsignedByte display. */
    public static final class LoadPrivateMessage {
        public static final int OPCODE = 50;
        public static final MessageType TYPE = MessageType.FIXED;

        private final long name;
        private final int world;

        public LoadPrivateMessage(long name, int world) {
            this.name = name;
            this.world = world;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.putLong(name);
            message.put(world);
            message.put(1);
            return message;
        }
    }

    /** Opcode 109 - Logout. Client: disconnects, no payload. */
    public static final class Logout {
        public static final int OPCODE = 109;
        public static final MessageType TYPE = MessageType.FIXED;

        public ByteMessage encode() {
            return ByteMessage.message(OPCODE, TYPE);
        }
    }

    /** Opcode 73 - Map Region Update. Client: readLEUShortA mapRegionX, readUnsignedShort mapRegionY. */
    public static final class MapRegionUpdate {
        public static final int OPCODE = 73;
        public static final MessageType TYPE = MessageType.FIXED;

        private final int mapRegionX;
        private final int mapRegionY;

        public MapRegionUpdate(int mapRegionX, int mapRegionY) {
            this.mapRegionX = mapRegionX;
            this.mapRegionY = mapRegionY;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.putShort(mapRegionX + 6, ByteOrder.BIG, ValueType.ADD);
            message.putShort(mapRegionY + 6, ByteOrder.BIG);
            return message;
        }
    }

    /** Opcode 160 - Object Animation. Client: readUnsignedByte tile, readUnsignedByte typeAndFace, readUnsignedShortA animation. */
    public static final class ObjectAnimation {
        public static final int OPCODE = 160;
        public static final MessageType TYPE = MessageType.FIXED;

        private final int config;
        private final int animation;

        public ObjectAnimation(int config, int animation) {
            this.config = config;
            this.animation = animation;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.put(0, ValueType.SUBTRACT);
            message.put(config, ValueType.SUBTRACT);
            message.putShort(animation, ValueType.ADD);
            return message;
        }
    }

    /** Opcode 104 - Player Context Menu. Client: readNegUByte slot, readUByteA enabled, readString command. */
    public static final class PlayerContextMenu {
        public static final int OPCODE = 104;
        public static final MessageType TYPE = MessageType.VAR;

        private final int commandSlot;
        private final boolean enabled;
        private final String command;

        public PlayerContextMenu(int commandSlot, boolean enabled, String command) {
            this.commandSlot = commandSlot;
            this.enabled = enabled;
            this.command = command;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.put(commandSlot, ValueType.NEGATE);
            message.put(enabled ? 1 : 0, ValueType.ADD);
            message.putString(command);
            return message;
        }
    }

    /** Opcode 134 - Refresh Skill. Client: readUnsignedByte skill, readInt (MIDDLE) experience, readUnsignedByte level. */
    public static final class RefreshSkill {
        public static final int OPCODE = 134;
        public static final MessageType TYPE = MessageType.FIXED;

        private final int skillId;
        private final int experience;
        private final int level;

        public RefreshSkill(int skillId, int experience, int level) {
            this.skillId = skillId;
            this.experience = experience;
            this.level = level;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.put(skillId);
            message.putInt(experience, ByteOrder.MIDDLE);
            message.put(level);
            return message;
        }
    }

    /** Opcode 156 - Remove Ground Item. Client: readUByteA tile, readUnsignedShort itemId. */
    public static final class RemoveGroundItem {
        public static final int OPCODE = 156;
        public static final MessageType TYPE = MessageType.FIXED;

        private final int itemId;

        public RemoveGroundItem(int itemId) {
            this.itemId = itemId;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.put(0, ValueType.ADD);
            message.putShort(itemId);
            return message;
        }
    }

    /** Opcode 101 - Clear Object. Client: readNegUByte typeAndFace, readUnsignedByte tile. */
    public static final class ClearObject {
        public static final int OPCODE = 101;
        public static final MessageType TYPE = MessageType.FIXED;

        private final int config;

        public ClearObject(int config) {
            this.config = config;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.put(config, ValueType.NEGATE);
            message.put(0);
            return message;
        }
    }

    /** Opcode 151 - Place Object. Client: readUByteA tile, readLEUShort itemId, readSubUByte typeAndFace. */
    public static final class PlaceObject {
        public static final int OPCODE = 151;
        public static final MessageType TYPE = MessageType.FIXED;

        private final int itemId;
        private final int config;

        public PlaceObject(int itemId, int config) {
            this.itemId = itemId;
            this.config = config;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.put(0, ValueType.ADD);
            message.putShort(itemId, ByteOrder.LITTLE);
            message.put(config, ValueType.SUBTRACT);
            return message;
        }
    }

    /** Opcode 44 - Create Ground Item. Client: readLEUShortA itemId, readLong amount, readUnsignedByte type, readUnsignedByte tile. */
    public static final class CreateGroundItem {
        public static final int OPCODE = 44;
        public static final MessageType TYPE = MessageType.FIXED;

        private final int itemId;
        private final long amount;

        public CreateGroundItem(int itemId, long amount) {
            this.itemId = itemId;
            this.amount = amount;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.putShort(itemId, ByteOrder.LITTLE, ValueType.ADD);
            message.putLong(amount);
            message.put(0); // normal ground-item type
            message.put(0); // packed local coordinate
            return message;
        }
    }

    /** Opcode 117 - Projectile. Client: readUnsignedByte tile, readUnsignedByte offsetX, readUnsignedByte offsetY, readUnsignedShort target, readUnsignedShort gfx, readUnsignedByte startHeight, readUnsignedByte endHeight, readUnsignedShort delay, readUnsignedShort speed, readUnsignedByte slope, readUnsignedByte distance. */
    public static final class Projectile {
        public static final int OPCODE = 117;
        public static final MessageType TYPE = MessageType.FIXED;

        private final int offsetByte;
        private final int finalOffsetX;
        private final int finalOffsetY;
        private final int targetIndex;
        private final int gfxMoving;
        private final int startHeight;
        private final int endHeight;
        private final int begin;
        private final int speed;
        private final int slope;
        private final int initDistance;

        public Projectile(int offsetByte, int finalOffsetX, int finalOffsetY, int targetIndex, int gfxMoving, int startHeight, int endHeight, int begin, int speed, int slope, int initDistance) {
            this.offsetByte = offsetByte;
            this.finalOffsetX = finalOffsetX;
            this.finalOffsetY = finalOffsetY;
            this.targetIndex = targetIndex;
            this.gfxMoving = gfxMoving;
            this.startHeight = startHeight;
            this.endHeight = endHeight;
            this.begin = begin;
            this.speed = speed;
            this.slope = slope;
            this.initDistance = initDistance;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.put(offsetByte);
            message.put(finalOffsetX);
            message.put(finalOffsetY);
            message.putShort(targetIndex);
            message.putShort(gfxMoving);
            message.put(startHeight);
            message.put(endHeight);
            message.putShort(begin);
            message.putShort(speed);
            message.put(slope);
            message.put(initDistance);
            return message;
        }
    }

    /** Opcode 166 - Camera Position. Client: readUnsignedByte x, readUnsignedByte y, readUnsignedShort z, readUnsignedByte speed, readUnsignedByte angle. */
    public static final class CameraPosition {
        public static final int OPCODE = 166;
        public static final MessageType TYPE = MessageType.FIXED;

        private final int x;
        private final int y;
        private final int z;
        private final int speed;
        private final int angle;

        public CameraPosition(int x, int y, int z, int speed, int angle) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.speed = speed;
            this.angle = angle;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.put(x);
            message.put(y);
            message.putShort(z);
            message.put(speed);
            message.put(angle);
            return message;
        }
    }

    /** Opcode 177 - Camera Rotation. Client: readUnsignedByte x, readUnsignedByte y, readUnsignedShort z, readUnsignedByte speed1, readUnsignedByte speed2. */
    public static final class CameraRotation {
        public static final int OPCODE = 177;
        public static final MessageType TYPE = MessageType.FIXED;

        private final int x;
        private final int y;
        private final int z;
        private final int sp1;
        private final int sp2;

        public CameraRotation(int x, int y, int z, int sp1, int sp2) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.sp1 = sp1;
            this.sp2 = sp2;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.put(x);
            message.put(y);
            message.putShort(z);
            message.put(sp1);
            message.put(sp2);
            return message;
        }
    }

    /** Opcode 35 - Camera Shake. Client: readUnsignedByte slot, readUnsignedByte amount, readUnsignedByte amplitude, readUnsignedByte speed. */
    public static final class CameraShake {
        public static final int OPCODE = 35;
        public static final MessageType TYPE = MessageType.FIXED;

        private final int slot;
        private final int amount;
        private final int amplitude;
        private final int speed;

        public CameraShake(int slot, int amount, int amplitude, int speed) {
            this.slot = slot;
            this.amount = amount;
            this.amplitude = amplitude;
            this.speed = speed;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.put(slot);
            message.put(amount);
            message.put(amplitude);
            message.put(speed);
            return message;
        }
    }

    /** Opcode 187 - Send Enter Name. Client: readString title, readUShortA maxLength. */
    public static final class SendEnterName {
        public static final int OPCODE = 187;
        public static final MessageType TYPE = MessageType.VAR_SHORT;

        private final String title;
        private final int maxLength;

        public SendEnterName(String title) {
            this(title, 12);
        }

        public SendEnterName(String title, int maxLength) {
            this.title = title;
            this.maxLength = maxLength;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.putString(title);
            message.putShort(maxLength, ValueType.ADD);
            return message;
        }
    }

    /** Opcode 28 - masked, RSA-backed account password prompt. */
    public static final class SendPasswordPrompt {
        public static final int OPCODE = 28;
        public static final MessageType TYPE = MessageType.VAR_SHORT;

        private final int stage;
        private final String nonce;
        private final String title;

        public SendPasswordPrompt(int stage, String nonce, String title) {
            this.stage = stage;
            this.nonce = nonce;
            this.title = title;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.put(stage);
            message.putString(nonce);
            message.putString(title);
            return message;
        }
    }

    /** Opcode 127 - Send Exp Counter. Client: readUnsignedByte skill, readInt experience, readUnsignedByte counter. */
    public static final class SendExpCounter {
        public static final int OPCODE = 127;
        public static final MessageType TYPE = MessageType.FIXED;

        private final int skill;
        private final int experience;
        private final boolean counter;

        public SendExpCounter(int skill, int experience, boolean counter) {
            this.skill = skill;
            this.experience = experience;
            this.counter = counter;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.put(skill);
            message.putInt(experience);
            message.put(counter ? 1 : 0);
            return message;
        }
    }

    /** Opcode 103 - Send Exp Counter Setting. Client: readInt type, readInt modification. */
    public static final class SendExpCounterSetting {
        public static final int OPCODE = 103;
        public static final MessageType TYPE = MessageType.FIXED;

        private final int type;
        private final int modification;

        public SendExpCounterSetting(int type, int modification) {
            this.type = type;
            this.modification = modification;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.putInt(type);
            message.putInt(modification);
            return message;
        }
    }

    /** Opcode 27 - Send Frame 27. Client: readString title, readUnsignedShort inputLength. */
    public static final class SendFrame27 {
        public static final int OPCODE = 27;
        public static final MessageType TYPE = MessageType.VAR_SHORT;

        private final String title;
        private final int inputLength;

        public SendFrame27(String title, int inputLength) {
            this.title = title;
            this.inputLength = inputLength;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.putString(title);
            message.putShort(inputLength, ValueType.ADD);
            return message;
        }
    }

    /** Opcode 200 - Send Interface Animation. Client: readUnsignedShort mainFrame, readUnsignedShort subFrame. */
    public static final class SendInterfaceAnimation {
        public static final int OPCODE = 200;
        public static final MessageType TYPE = MessageType.FIXED;

        private final int mainFrame;
        private final int subFrame;

        public SendInterfaceAnimation(int mainFrame, int subFrame) {
            this.mainFrame = mainFrame;
            this.subFrame = subFrame;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.putShort(mainFrame, ByteOrder.BIG);
            message.putShort(subFrame, ByteOrder.BIG);
            return message;
        }
    }

    /** Opcode 246 - Send Interface Model. Client: readLEUShort mainFrame, readUnsignedShort subFrame, readUnsignedShort subFrame2. */
    public static final class SendInterfaceModel {
        public static final int OPCODE = 246;
        public static final MessageType TYPE = MessageType.FIXED;

        private final int mainFrame;
        private final int subFrame;
        private final int subFrame2;

        public SendInterfaceModel(int mainFrame, int subFrame, int subFrame2) {
            this.mainFrame = mainFrame;
            this.subFrame = subFrame;
            this.subFrame2 = subFrame2;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.putShort(mainFrame, ByteOrder.LITTLE);
            message.putShort(subFrame);
            message.putShort(subFrame2);
            return message;
        }
    }

    /** Opcode 108 - Send Screen Mode. Client: readLEUShortA width, readInt height. */
    public static final class SendScreenMode {
        public static final int OPCODE = 108;
        public static final MessageType TYPE = MessageType.FIXED;

        private final int width;
        private final int height;

        public SendScreenMode(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.putShort(width, ByteOrder.LITTLE, ValueType.ADD);
            message.putInt(height);
            return message;
        }
    }

    /** Opcode 204 - Send Scrollbar. Client: readInt scrollbar, readInt size. */
    public static final class SendScrollbar {
        public static final int OPCODE = 204;
        public static final MessageType TYPE = MessageType.FIXED;

        private final int scrollbar;
        private final int size;

        public SendScrollbar(int scrollbar, int size) {
            this.scrollbar = scrollbar;
            this.size = size;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.putInt(scrollbar);
            message.putInt(size);
            return message;
        }
    }

    /** Opcode 203 - Send Tooltip. Client: readString string, readUnsignedShortA id. */
    public static final class SendTooltip {
        public static final int OPCODE = 203;
        public static final MessageType TYPE = MessageType.VAR_SHORT;

        private final String string;
        private final int id;

        public SendTooltip(String string, int id) {
            this.string = string;
            this.id = id;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.putString(string);
            message.putShort(id, ValueType.ADD);
            return message;
        }
    }

    /** Opcode 208 - Set Interface Walkable. Client: readUnsignedShort id. */
    public static final class SetInterfaceWalkable {
        public static final int OPCODE = 208;
        public static final MessageType TYPE = MessageType.FIXED;

        private final int id;

        public SetInterfaceWalkable(int id) {
            this.id = id;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.putShort(id);
            return message;
        }
    }

    /** Opcode 174 - Sound. Client: readUnsignedShort soundId, readUnsignedByte volume, readUnsignedShort delay. */
    public static final class Sound {
        public static final int OPCODE = 174;
        public static final MessageType TYPE = MessageType.FIXED;

        private final int soundId;
        private final int volume;
        private final int delay;

        public Sound(int soundId, int volume, int delay) {
            this.soundId = soundId;
            this.volume = volume;
            this.delay = delay;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.putShort(soundId);
            message.put(volume);
            message.putShort(delay);
            return message;
        }
    }

    /** Opcode 4 - Still Graphic. Client: readUnsignedByte tile, readUnsignedShort id, readUnsignedByte height, readUnsignedShort delay. */
    public static final class StillGraphic {
        public static final int OPCODE = 4;
        public static final MessageType TYPE = MessageType.FIXED;

        private final int offsetByte;
        private final int id;
        private final int height;
        private final int time;

        public StillGraphic(int offsetByte, int id, int height, int time) {
            this.offsetByte = offsetByte;
            this.id = id;
            this.height = height;
            this.time = time;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.put(offsetByte);
            message.putShort(id);
            message.put(height);
            message.putShort(time);
            return message;
        }
    }

    /** Opcode 114 - System Update Timer. Client: readLEUShort clientTicks. */
    public static final class SystemUpdateTimer {
        public static final int OPCODE = 114;
        public static final MessageType TYPE = MessageType.FIXED;

        private final int clientTicks;

        public SystemUpdateTimer(int clientTicks) {
            this.clientTicks = clientTicks;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.putShort(clientTicks, ByteOrder.LITTLE);
            return message;
        }
    }

    /** Opcode 206 - Set Chat Options. Client: readUnsignedByte publicChat, readUnsignedByte privateChat, readUnsignedByte clanChat, readUnsignedByte tradeBlock. */
    public static final class SetChatOptions {
        public static final int OPCODE = 206;
        public static final MessageType TYPE = MessageType.FIXED;

        private final int publicChat;
        private final int privateChat;
        private final int tradeBlock;

        public SetChatOptions(int publicChat, int privateChat, int tradeBlock) {
            this.publicChat = publicChat;
            this.privateChat = privateChat;
            this.tradeBlock = tradeBlock;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.put(publicChat);
            message.put(privateChat);
            message.put(0); // clan chat mode
            message.put(tradeBlock);
            return message;
        }
    }

    /** Opcode 74 - Set Region Song. Client: readLEUShort songId. */
    public static final class SetRegionSong {
        public static final int OPCODE = 74;
        public static final MessageType TYPE = MessageType.FIXED;

        private final int songId;

        public SetRegionSong(int songId) {
            this.songId = songId;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.putShort(songId, ByteOrder.LITTLE);
            return message;
        }
    }

    /** Opcode 79 - Set Scroll Position. Client: readUnsignedShort id, readUnsignedShort offset. */
    public static final class SetScrollPosition {
        public static final int OPCODE = 79;
        public static final MessageType TYPE = MessageType.FIXED;

        private final int id;

        public SetScrollPosition(int id) {
            this.id = id;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.putShort(id, ByteOrder.BIG);
            message.putShort(0, ValueType.NORMAL);
            return message;
        }
    }

    /** Opcode 87 - Set Varbit Int. Client: readLEUShort id, readInt (MIDDLE) value. */
    public static final class SetVarbitInt {
        public static final int OPCODE = 87;
        public static final MessageType TYPE = MessageType.FIXED;

        private final int id;
        private final int value;

        public SetVarbitInt(int id, int value) {
            this.id = id;
            this.value = value;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.putShort(id, ByteOrder.LITTLE);
            message.putInt(value, ByteOrder.MIDDLE);
            return message;
        }
    }

    /** Opcode 36 - Set Varbit Byte. Client: readLEUShort id, readUnsignedByte value. */
    public static final class SetVarbitByte {
        public static final int OPCODE = 36;
        public static final MessageType TYPE = MessageType.FIXED;

        private final int id;
        private final int value;

        public SetVarbitByte(int id, int value) {
            this.id = id;
            this.value = value;
        }

        public ByteMessage encode() {
            ByteMessage message = ByteMessage.message(OPCODE, TYPE);
            message.putShort(id, ByteOrder.LITTLE);
            message.put(value);
            return message;
        }
    }
}
