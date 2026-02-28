package net.dodian.uber.game.model.entity.player;


import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.UpdateFlag;
import net.dodian.uber.game.model.chunk.ChunkPlayerComparator;
import net.dodian.uber.game.model.entity.Entity;
import net.dodian.uber.game.model.entity.EntityUpdating;
import net.dodian.uber.game.model.item.Equipment;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;
import net.dodian.uber.game.netty.codec.MessageType;
import net.dodian.utilities.Utils;

/**
 * @author blakeman8192
 * @author lare96 <<a href="http://github.com/lare96">...</a>>
 * @author Dashboard
 */
public class PlayerUpdating extends EntityUpdating<Player> {

    private static final boolean DEBUG_REGION_UPDATES = false;
    private static final boolean DEBUG_ADDED_LOCAL_PLAYERS = false;
    private static final int MAX_LOCAL_PLAYER_ADDS_PER_TICK = 15;
    private static final int MAX_LOCAL_PLAYER_CAP = 255;

    private static final PlayerUpdating instance = new PlayerUpdating();
    private static final java.util.concurrent.atomic.AtomicInteger DEBUG_ADDED_LOCAL_COUNTER = new java.util.concurrent.atomic.AtomicInteger();
    private static final PlayerUpdateBlockSet BLOCK_SET = new PlayerUpdateBlockSet();

    enum UpdatePhase {
        UPDATE_SELF,
        UPDATE_LOCAL,
        ADD_LOCAL
    }

    public static PlayerUpdating getInstance() {
        return instance;
    }

    @Override
    public void update(Player player, ByteMessage stream) {
        ByteMessage updateBlock = ByteMessage.raw(8192); // replaced legacy Stream buffer
        try {
            if (Server.updateRunning) {
                // Send server update packet (114) as separate message
                ByteMessage updateMsg = ByteMessage.message(114, MessageType.FIXED);
                int seconds = Server.updateSeconds + ((int)(Server.updateStartTime - System.currentTimeMillis()) / 1000);
                updateMsg.putShort(seconds * 50 / 30, ByteOrder.BIG);
                ((Client) player).send(updateMsg);
            }

            // Ensure the player is registered in the chunk index before discovery.
            player.syncChunkMembership();

            boolean localPlayerUpdateRequired = hasUpdatesForPhase(player, UpdatePhase.UPDATE_SELF);
            updateLocalPlayerMovement(player, stream, localPlayerUpdateRequired);

            // Handle teleportation - clear player list but continue to local player discovery
            if (player.didTeleport()) {
                // Clear existing player list when teleporting (similar to Hyperion's approach)
                java.util.Arrays.fill(player.playerList, 0, player.playerListSize, null);
                player.playerListSize = 0;
                player.playersUpdating.clear();
                // Don't return early - allow local player discovery to happen
            }

            appendBlockUpdate(player, updateBlock, UpdatePhase.UPDATE_SELF);
            if (player.loaded) {
                pruneLocalsToProtocolCap(player);
                stream.putBits(8, player.playerListSize);
                int size = player.playerListSize;
                int keep = 0;
                for (int i = 0; i < size; i++) {
                    Player local = player.playerList[i];
                    if (local != null && player.loaded && !local.didTeleport() && !player.didTeleport()
                            && player.withinDistance(local)) {
                        local.updatePlayerMovement(stream);
                        appendBlockUpdate(local, updateBlock, UpdatePhase.UPDATE_LOCAL);
                        player.playerList[keep++] = local;
                    } else {
                        if (local != null) {
                            player.playersUpdating.remove(local);
                        }
                        stream.putBits(1, 1);
                        stream.putBits(2, 3);
                    }
                }
                java.util.Arrays.fill(player.playerList, keep, size, null);
                player.playerListSize = keep;

                addLocalPlayers(player, stream, updateBlock);
            } else {
                stream.putBits(8, 0);
            }

            // Always write the magic termination value (2047) - required by protocol
            stream.putBits(11, 2047);
            stream.endBitAccess();

            if (updateBlock.getBuffer().writerIndex() > 0) {
                stream.putBytes(updateBlock);
            }
            // Note: endFrameVarSizeWord equivalent is handled by the outer packet wrapper

            if (DEBUG_REGION_UPDATES) {
                int rx = player.getPosition().getX() >> 6;
                int ry = player.getPosition().getY() >> 6;
                System.out.println("[RegionUpdate] " + player.getPlayerName() + " region(" + rx + "," + ry + ") locals=" + player.playerListSize);
            }
        } finally {
            updateBlock.releaseAll();
        }
    }

    private void addLocalPlayers(Player player, ByteMessage stream, ByteMessage updateBlock) {
        int remainingAdds = Math.min(MAX_LOCAL_PLAYER_ADDS_PER_TICK, MAX_LOCAL_PLAYER_CAP - player.playerListSize);
        if (remainingAdds <= 0) {
            return;
        }

        if (Server.chunkManager == null) {
            java.util.List<Player> candidates = PlayerHandler.getLocalPlayers(player);
            if (candidates.isEmpty()) {
                return;
            }
            if (player.playerListSize > 50) {
                candidates.sort(new ChunkPlayerComparator(player));
            }
            addLocalPlayersFromCollection(player, stream, updateBlock, candidates, remainingAdds);
            return;
        }

        if (player.playerListSize > 50) {
            addPrioritizedChunkLocalPlayers(player, stream, updateBlock, remainingAdds);
            return;
        }

        final int[] playersAdded = {0};
        Server.chunkManager.forEachUpdatePlayerCandidate(player, 16, other -> {
            if (playersAdded[0] >= remainingAdds || !shouldAddLocalPlayerCandidate(player, other)) {
                return;
            }
            player.addNewPlayer(other, stream, updateBlock);
            if (!player.playersUpdating.contains(other)) {
                return;
            }
            playersAdded[0]++;
            if (DEBUG_ADDED_LOCAL_PLAYERS) {
                DEBUG_ADDED_LOCAL_COUNTER.incrementAndGet();
            }
        });
    }

    private void addLocalPlayersFromCollection(Player player,
                                               ByteMessage stream,
                                               ByteMessage updateBlock,
                                               java.util.Collection<Player> candidates,
                                               int remainingAdds) {
        int playersAdded = 0;
        for (Player other : candidates) {
            if (!shouldAddLocalPlayerCandidate(player, other)) {
                continue;
            }

            player.addNewPlayer(other, stream, updateBlock);
            if (!player.playersUpdating.contains(other)) {
                continue;
            }
            playersAdded++;
            if (DEBUG_ADDED_LOCAL_PLAYERS) {
                DEBUG_ADDED_LOCAL_COUNTER.incrementAndGet();
            }

            if (playersAdded >= remainingAdds || player.playerListSize >= MAX_LOCAL_PLAYER_CAP) {
                break;
            }
        }
    }

    private void addPrioritizedChunkLocalPlayers(Player player,
                                                 ByteMessage stream,
                                                 ByteMessage updateBlock,
                                                 int remainingAdds) {
        Player[] prioritized = new Player[remainingAdds];
        int[] prioritizedDistances = new int[remainingAdds];
        int[] prioritizedCount = {0};

        Server.chunkManager.forEachUpdatePlayerCandidate(player, 16, other -> {
            if (!shouldAddLocalPlayerCandidate(player, other)) {
                return;
            }
            insertPrioritizedCandidate(player, prioritized, prioritizedDistances, prioritizedCount, other);
        });

        for (int i = 0; i < prioritizedCount[0]; i++) {
            Player other = prioritized[i];
            if (!shouldAddLocalPlayerCandidate(player, other)) {
                continue;
            }
            player.addNewPlayer(other, stream, updateBlock);
            if (player.playersUpdating.contains(other) && DEBUG_ADDED_LOCAL_PLAYERS) {
                DEBUG_ADDED_LOCAL_COUNTER.incrementAndGet();
            }
        }
    }

    private void insertPrioritizedCandidate(Player viewer,
                                            Player[] prioritized,
                                            int[] prioritizedDistances,
                                            int[] prioritizedCount,
                                            Player candidate) {
        int distance = computeLongestDistance(viewer, candidate);
        int count = prioritizedCount[0];
        int limit = prioritized.length;

        if (count == limit) {
            Player worst = prioritized[count - 1];
            if (compareCandidate(candidate, distance, worst, prioritizedDistances[count - 1]) >= 0) {
                return;
            }
        } else {
            prioritizedCount[0] = count + 1;
        }

        int insert = Math.min(count, limit - 1);
        while (insert > 0 && compareCandidate(candidate, distance, prioritized[insert - 1], prioritizedDistances[insert - 1]) < 0) {
            prioritized[insert] = prioritized[insert - 1];
            prioritizedDistances[insert] = prioritizedDistances[insert - 1];
            insert--;
        }

        prioritized[insert] = candidate;
        prioritizedDistances[insert] = distance;
    }

    private int compareCandidate(Player left, int leftDistance, Player right, int rightDistance) {
        if (right == null) {
            return -1;
        }
        int distanceCompare = Integer.compare(leftDistance, rightDistance);
        if (distanceCompare != 0) {
            return distanceCompare;
        }
        return Integer.compare(left.getSlot(), right.getSlot());
    }

    private int computeLongestDistance(Player viewer, Player other) {
        int dx = Math.abs(viewer.getPosition().getX() - other.getPosition().getX());
        int dy = Math.abs(viewer.getPosition().getY() - other.getPosition().getY());
        return Math.max(dx, dy);
    }

    private boolean shouldAddLocalPlayerCandidate(Player player, Player other) {
        if (player == other || other == null || !other.isActive) {
            return false;
        }

        if (!player.withinDistance(other) || (!player.didTeleport() && player.playersUpdating.contains(other))) {
            return false;
        }

        return !other.invis || player.invis;
    }

    private void pruneLocalsToProtocolCap(Player player) {
        if (player.playerListSize <= MAX_LOCAL_PLAYER_CAP) {
            return;
        }
        int originalSize = player.playerListSize;
        int keep = 0;
        for (int i = 0; i < originalSize && keep < MAX_LOCAL_PLAYER_CAP; i++) {
            Player local = player.playerList[i];
            if (local == null) {
                continue;
            }
            player.playerList[keep++] = local;
        }
        for (int i = keep; i < originalSize; i++) {
            Player local = player.playerList[i];
            if (local != null) {
                player.playersUpdating.remove(local);
                player.playerList[i] = null;
            }
        }
        player.playerListSize = keep;
    }


    public void updateLocalPlayerMovement(Player player, ByteMessage stream, boolean localPlayerUpdateRequired) {
        /* Noob! */
        if(player.didMapRegionChange()) {
            // Send map region change as separate packet (73)
            ByteMessage mapMsg = ByteMessage.message(73, MessageType.FIXED);
            mapMsg.putShort(player.mapRegionX + 6, ByteOrder.BIG, ValueType.ADD); // writeWordA
            mapMsg.putShort(player.mapRegionY + 6, ByteOrder.BIG); // writeWord
            ((Client) player).send(mapMsg);
            ((Client) player).updateGroundItems();
        }
        // This should match the original: createFrameVarSizeWord(81) + initBitAccess()
        // But we're doing this in the packet wrapper instead
        stream.startBitAccess();
        if (player.didTeleport()) {
            stream.putBits(1, 1);
            stream.putBits(2, 3); // updateType
            stream.putBits(2, player.getPosition().getZ());
            stream.putBits(1, player.didTeleport() ? 1 : 0);
            stream.putBits(1, localPlayerUpdateRequired ? 1 : 0);
            stream.putBits(7, player.getCurrentY());
            stream.putBits(7, player.getCurrentX());
            return;
        }
        if (player.getPrimaryDirection() == -1) {
            if (localPlayerUpdateRequired) {
                stream.putBits(1, 1);
                stream.putBits(2, 0);
            } else {
                stream.putBits(1, 0);
            }
        } else
        if (player.getSecondaryDirection() == -1) {
            stream.putBits(1, 1);
            stream.putBits(2, 1);
            stream.putBits(3, Utils.xlateDirectionToClient[player.getPrimaryDirection()]);
            stream.putBits(1, localPlayerUpdateRequired ? 1 : 0);
        } else {
            stream.putBits(1, 1);
            stream.putBits(2, 2);
            stream.putBits(3, Utils.xlateDirectionToClient[player.getPrimaryDirection()]);
            stream.putBits(3, Utils.xlateDirectionToClient[player.getSecondaryDirection()]);
            stream.putBits(1, localPlayerUpdateRequired ? 1 : 0);
        }
    }


    @Override
    public void appendBlockUpdate(Player player, ByteMessage buf) {
        appendBlockUpdate(player, buf, UpdatePhase.UPDATE_LOCAL);
    }

    void appendBlockUpdate(Player player, ByteMessage buf, UpdatePhase phase) {
        BLOCK_SET.encode(this, player, buf, phase);
    }

    public void appendAddLocalBlockUpdate(Player player, ByteMessage buf) {
        appendBlockUpdate(player, buf, UpdatePhase.ADD_LOCAL);
    }

    private boolean hasUpdatesForPhase(Player player, UpdatePhase phase) {
        if (phase == UpdatePhase.ADD_LOCAL) {
            return true;
        }
        if (phase == UpdatePhase.UPDATE_SELF) {
            return player.getUpdateFlags().isRequired(UpdateFlag.FORCED_MOVEMENT)
                    || player.getUpdateFlags().isRequired(UpdateFlag.GRAPHICS)
                    || player.getUpdateFlags().isRequired(UpdateFlag.ANIM)
                    || player.getUpdateFlags().isRequired(UpdateFlag.FORCED_CHAT)
                    || player.getUpdateFlags().isRequired(UpdateFlag.FACE_CHARACTER)
                    || player.getUpdateFlags().isRequired(UpdateFlag.APPEARANCE)
                    || player.getUpdateFlags().isRequired(UpdateFlag.FACE_COORDINATE)
                    || player.getUpdateFlags().isRequired(UpdateFlag.HIT)
                    || player.getUpdateFlags().isRequired(UpdateFlag.HIT2);
        }
        return player.getUpdateFlags().isUpdateRequired();
    }

    public static void resetDebugAddedLocalCounter() {
        DEBUG_ADDED_LOCAL_COUNTER.set(0);
    }

    public static int consumeDebugAddedLocalCounter() {
        return DEBUG_ADDED_LOCAL_COUNTER.getAndSet(0);
    }

    public void appendGraphic(Player player, ByteMessage buf) {
        buf.putShort(player.getGraphicId(), ByteOrder.LITTLE); // writeWordBigEndian  
        buf.putInt(player.getGraphicHeight()); // writeDWord
    }

    @Override
    public void appendAnimationRequest(Player player, ByteMessage buf) {
        buf.putShort(player.getAnimationId(), ByteOrder.LITTLE); // writeWordBigEndian is actually little-endian!
        buf.put(player.getAnimationDelay(), ValueType.NEGATE); // writeByteC = -value, not 128-value
    }

    public static void appendForcedChatText(Player player, ByteMessage buf) {
        buf.putString(player.getForcedChat());
    }

    public static void appendPlayerChatText(Player player, ByteMessage buf) {
        buf.putShort(((player.getChatTextColor() & 0xFF) << 8) + (player.getChatTextEffects() & 0xFF), ByteOrder.LITTLE); // writeWordBigEndian
        buf.put(player.playerRights);

        // Mystic client expects a null-terminated string for chat (not the packed 317 bytes).
        String chatMessage = player.getChatTextMessage();
        if (chatMessage == null) {
            chatMessage = "";
        }
        buf.putString(chatMessage);
    }

    @Override
    public void appendFaceCharacter(Player player, ByteMessage buf) {
        buf.putShort(player.getFaceTarget(), ByteOrder.LITTLE); // writeWordBigEndian
    }

    public static void appendPlayerAppearance(Player player, ByteMessage buf) {
        ByteMessage playerProps = ByteMessage.raw(128);
        try {
            playerProps.put(player.getGender());
            playerProps.put((byte) player.headIcon); // Head icon aka prayer over head
            playerProps.put((byte) player.skullIcon); // Skull icon
            if (!player.isNpc) {
                if (player.getEquipment()[Equipment.Slot.HEAD.getId()] > 1) {
                    playerProps.putShort(0x200 + player.getEquipment()[Equipment.Slot.HEAD.getId()]);
                } else {
                    playerProps.put(0);
                }
                if (player.getEquipment()[Equipment.Slot.CAPE.getId()] > 1) {
                    playerProps.putShort(0x200 + player.getEquipment()[Equipment.Slot.CAPE.getId()]);
                } else {
                    playerProps.put(0);
                }
                if (player.getEquipment()[Equipment.Slot.NECK.getId()] > 1) {
                    playerProps.putShort(0x200 + player.getEquipment()[Equipment.Slot.NECK.getId()]);
                } else {
                    playerProps.put(0);
                }
                if (player.getEquipment()[Equipment.Slot.WEAPON.getId()] > 1 && !player.UsingAgility) {
                    playerProps.putShort(0x200 + player.getEquipment()[Equipment.Slot.WEAPON.getId()]);
                } else {
                    playerProps.put(0);
                }
                if (player.getEquipment()[Equipment.Slot.CHEST.getId()] > 1) {
                    playerProps.putShort(0x200 + player.getEquipment()[Equipment.Slot.CHEST.getId()]);
                } else {
                    playerProps.putShort(0x100 + player.getTorso());
                }
                if (player.getEquipment()[Equipment.Slot.SHIELD.getId()] > 1 && !player.UsingAgility) {
                    playerProps.putShort(0x200 + player.getEquipment()[Equipment.Slot.SHIELD.getId()]);
                } else {
                    playerProps.put(0);
                }
                if (!Server.itemManager.isFullBody(player.getEquipment()[Equipment.Slot.CHEST.getId()])) {
                    playerProps.putShort(0x100 + player.getArms());
                } else {
                    playerProps.put(0);
                }
                if (player.getEquipment()[Equipment.Slot.LEGS.getId()] > 1) {
                    playerProps.putShort(0x200 + player.getEquipment()[Equipment.Slot.LEGS.getId()]);
                } else {
                    playerProps.putShort(0x100 + player.getLegs());
                }
                if (!Server.itemManager.isFullHelm(player.getEquipment()[Equipment.Slot.HEAD.getId()]) && !Server.itemManager.isMask(player.getEquipment()[Equipment.Slot.HEAD.getId()])) {
                    playerProps.putShort(0x100 + player.getHead()); // head
                } else {
                    playerProps.put(0);
                }
                if (player.getEquipment()[Equipment.Slot.HANDS.getId()] > 1) {
                    playerProps.putShort(0x200 + player.getEquipment()[Equipment.Slot.HANDS.getId()]);
                } else {
                    playerProps.putShort(0x100 + player.getHands());
                }
                if (player.getEquipment()[Equipment.Slot.FEET.getId()] > 1) {
                    playerProps.putShort(0x200 + player.getEquipment()[Equipment.Slot.FEET.getId()]);
                } else {
                    playerProps.putShort(0x100 + player.getFeet());
                }
                if (!Server.itemManager.isMask(player.getEquipment()[Equipment.Slot.HEAD.getId()]) && (player.playerLooks[0] != 1)) {
                    playerProps.putShort(0x100 + player.getBeard());
                } else {
                    playerProps.put(0); // 0 = nothing on and girl don't have beard
                    // so send 0. -bakatool
                }
            } else {
                playerProps.putShort(-1);
                playerProps.putShort(player.getPlayerNpc());
            }
            // array of 5 bytes defining the colors
            playerProps.put(player.playerLooks[8]); // hair color
            playerProps.put(player.playerLooks[9]); // torso color.
            playerProps.put(player.playerLooks[10]); // leg color
            playerProps.put(player.playerLooks[11]); // feet color
            playerProps.put(player.playerLooks[12]); // skin color (0-6)
            playerProps.putShort(player.getStandAnim()); // standAnimIndex
            playerProps.putShort(player.getWalkAnim()); // standTurnAnimIndex, 823 default
            playerProps.putShort(player.getWalkAnim()); // walkAnimIndex
            playerProps.putShort(player.getWalkAnim()); // turn180AnimIndex, 820 default
            playerProps.putShort(player.getWalkAnim()); // turn90CWAnimIndex, 821 default
            playerProps.putShort(player.getWalkAnim()); // turn90CCWAnimIndex, 822 default
            playerProps.putShort(player.getRunAnim()); // runAnimIndex

            playerProps.putLong(Utils.playerNameToInt64(player.getPlayerName()));
            playerProps.put(player.determineCombatLevel()); // combat level
            playerProps.putShort(0); // incase != 0, writes skill-%d

            buf.put(playerProps.getBuffer().writerIndex(), ValueType.NEGATE); // writeByteC = -value
            buf.putBytes(playerProps);
        } finally {
            playerProps.releaseAll();
        }
    }

    @Override
    public void appendFaceCoordinates(Player player, ByteMessage buf) {
        buf.putShort(player.getFacePosition().getX(), ByteOrder.LITTLE, ValueType.ADD); // writeWordBigEndianA
        buf.putShort(player.getFacePosition().getY(), ByteOrder.LITTLE); // writeWordBigEndian
    }

    @Override
    public void appendPrimaryHit(Player player, ByteMessage buf) {
        synchronized (this) {
            // Client appendPlayerUpdateMask (mask & 0x20) expects:
            // short damage, byte type, short currentHp, short maxHp
            int damage = player.getDamageDealt();
            if (damage < Short.MIN_VALUE) damage = Short.MIN_VALUE;
            if (damage > Short.MAX_VALUE) damage = Short.MAX_VALUE;
            buf.putShort(damage);

            int type;
            if (player.getDamageDealt() == 0) {
                type = 0; // miss
            } else if (player.getHitType() == Entity.hitType.BURN) {
                type = 4;
            } else if (player.getHitType() == Entity.hitType.CRIT) {
                type = 3;
            } else if (player.getHitType() == Entity.hitType.POISON) {
                type = 2;
            } else {
                type = 1; // normal
            }
            buf.put(type);

            int current = Math.max(0, player.getCurrentHealth());
            int max = Math.max(1, player.getMaxHealth());
            buf.putShort(current);
            buf.putShort(max);
        }
    }

    public void appendPrimaryHit2(Player player, ByteMessage buf) {
        synchronized(this) {
            // Client appendPlayerUpdateMask (mask & 0x200) uses same layout as primary hit
            int damage = player.getDamageDealt2();
            if (damage < Short.MIN_VALUE) damage = Short.MIN_VALUE;
            if (damage > Short.MAX_VALUE) damage = Short.MAX_VALUE;
            buf.putShort(damage);

            int type;
            if (player.getDamageDealt2() == 0) {
                type = 0; // miss
            } else if (player.getHitType2() == Entity.hitType.BURN) {
                type = 4;
            } else if (player.getHitType2() == Entity.hitType.CRIT) {
                type = 3;
            } else if (player.getHitType2() == Entity.hitType.POISON) {
                type = 2;
            } else {
                type = 1; // normal
            }
            buf.put(type);

            int current = Math.max(0, player.getCurrentHealth());
            int max = Math.max(1, player.getMaxHealth());
            buf.putShort(current);
            buf.putShort(max);
        }
    }

}
