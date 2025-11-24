package net.dodian.uber.game.model.entity.player;


import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.EntityType;
import net.dodian.uber.game.model.UpdateFlag;
import net.dodian.uber.game.model.chunk.ChunkPlayerComparator;
import net.dodian.uber.game.model.entity.Entity;
import net.dodian.uber.game.model.entity.EntityUpdating;
import net.dodian.uber.game.model.item.Equipment;
import net.dodian.utilities.Misc;
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

    private static final PlayerUpdating instance = new PlayerUpdating();

    public static PlayerUpdating getInstance() {
        return instance;
    }

    @Override
    public void update(Player player, ByteMessage stream) {
        ByteMessage updateBlock = ByteMessage.raw(8192); // replaced legacy Stream buffer
        boolean localPlayerUpdateRequired = false; // Track if local player movement indicated update required

        if (Server.updateRunning) {
            // Send server update packet (114) as separate message
            ByteMessage updateMsg = ByteMessage.message(114, MessageType.FIXED);
            int seconds = Server.updateSeconds + ((int)(Server.updateStartTime - System.currentTimeMillis()) / 1000);
            updateMsg.putShort(seconds * 50 / 30, ByteOrder.BIG);
            ((Client) player).send(updateMsg);
        }

        // Ensure the player is registered in the chunk index before discovery.
        player.syncChunkMembership();

        localPlayerUpdateRequired = player.getUpdateFlags().isUpdateRequired();
        updateLocalPlayerMovement(player, stream);
        
        // Handle teleportation - clear player list but continue to local player discovery
        if (player.didTeleport()) {
            // Clear existing player list when teleporting (similar to Hyperion's approach)
            player.playerListSize = 0;
            player.playersUpdating.clear();
            // Don't return early - allow local player discovery to happen
        }
        
        // Rebuild the local player's block fresh (don't reuse cached block) to avoid
        // sending CHAT back to themselves after another viewer cached it.
        player.invalidateCachedUpdateBlock();
        boolean saveChatTextUpdate = player.getUpdateFlags().isRequired(UpdateFlag.CHAT);
        player.getUpdateFlags().setRequired(UpdateFlag.CHAT, false);
        appendBlockUpdate(player, updateBlock);
        player.getUpdateFlags().setRequired(UpdateFlag.CHAT, saveChatTextUpdate);
        
        // The block we just cached (with CHAT suppressed) should not be reused by others.
        player.invalidateCachedUpdateBlock();
        if (player.loaded) {
            stream.putBits(8, player.playerListSize);
            int size = player.playerListSize;
            player.playersUpdating.clear();
            player.playerListSize = 0;
            for (int i = 0; i < size; i++) {
                if (player.playerList[i] != null && player.loaded && !player.playerList[i].didTeleport() && !player.didTeleport()
                        && player.withinDistance(player.playerList[i])) {
                    player.playerList[i].updatePlayerMovement(stream);
                    appendBlockUpdate(player.playerList[i], updateBlock);
                    player.playerList[player.playerListSize++] = player.playerList[i];
                    player.playersUpdating.add(player.playerList[i]);
                } else {
                    stream.putBits(1, 1);
                    stream.putBits(2, 3);
                }
            }

            // Chunk-based player discovery with prioritization/limits.
            java.util.Set<Player> nearbyPlayers = findNearbyPlayers(player);
            java.util.List<Player> candidates = new java.util.ArrayList<>(nearbyPlayers);

            if (candidates.size() > 50) {
                candidates.sort(new ChunkPlayerComparator(player));
            }

            int playersAdded = 0;
            for (Player other : candidates) {
                if (player == other || other == null || !other.isActive) {
                    continue;
                }

                if (!player.withinDistance(other) || (!player.didTeleport() && player.playersUpdating.contains(other))) {
                    continue;
                }

                if (other.invis && !player.invis) {
                    continue;
                }

                player.addNewPlayer(other, stream, updateBlock);
                playersAdded++;

                if (playersAdded >= 15 || player.playerListSize >= 255) {
                    break; // cap additions to avoid overflow
                }
            }
        } else {
            stream.putBits(8, 0);
        }

        // Always write the magic termination value (2047) - required by protocol
        stream.putBits(11, 2047);
        stream.endBitAccess();
        
        if (updateBlock.getBuffer().writerIndex() > 0) {
            // Only copy the written bytes, not the entire buffer capacity
            byte[] updateData = new byte[updateBlock.getBuffer().writerIndex()];
            updateBlock.getBuffer().getBytes(0, updateData);
            stream.putBytes(updateData);
        } else {
            // Check if movement indicated update was required but we have no data
            // This can happen when CHAT flag was suppressed for local player
            if (localPlayerUpdateRequired) {
                stream.put(0); // Empty update mask - no flags set
            }
        }
        // Note: endFrameVarSizeWord equivalent is handled by the outer packet wrapper

        if (DEBUG_REGION_UPDATES) {
            int rx = player.getPosition().getX() >> 6;
            int ry = player.getPosition().getY() >> 6;
            System.out.println("[RegionUpdate] " + player.getPlayerName() + " region(" + rx + "," + ry + ") locals=" + player.playerListSize);
        }
    }

    private java.util.Set<Player> findNearbyPlayers(Player player) {
        if (Server.chunkManager == null) {
            return new java.util.HashSet<>(PlayerHandler.getLocalPlayers(player));
        }

        java.util.Set<Player> nearby = Server.chunkManager.find(player.getPosition(), EntityType.PLAYER, 16);
        nearby.remove(player); // exclude self if present
        return nearby;
    }


    public void updateLocalPlayerMovement(Player player, ByteMessage stream) {
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
            stream.putBits(1, player.getUpdateFlags().isUpdateRequired() ? 1 : 0);
            stream.putBits(7, player.getCurrentY());
            stream.putBits(7, player.getCurrentX());
            return;
        }
        if (player.getPrimaryDirection() == -1) {
            if (player.getUpdateFlags().isUpdateRequired()) {
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
            stream.putBits(1, player.getUpdateFlags().isUpdateRequired() ? 1 : 0);
        } else {
            stream.putBits(1, 1);
            stream.putBits(2, 2);
            stream.putBits(3, Utils.xlateDirectionToClient[player.getPrimaryDirection()]);
            stream.putBits(3, Utils.xlateDirectionToClient[player.getSecondaryDirection()]);
            stream.putBits(1, player.getUpdateFlags().isUpdateRequired() ? 1 : 0);
        }
    }


    @Override
    public void appendBlockUpdate(Player player, ByteMessage buf) {

        /*
         * Fast-path: if this player's cached update block is valid, just copy it
         * straight into the output stream and return. This avoids recomputing
         * masks and appearance data for every viewer each cycle.
         */
        if (player != null && player.isCachedUpdateBlockValid()) {
            player.writeCachedUpdateBlock(buf);
            return;
        }
        if (!player.getUpdateFlags().isUpdateRequired() && !player.getUpdateFlags().isRequired(UpdateFlag.CHAT))
            return; // nothing required

        // Build update mask using the UpdateFlag system (reverted from hardcoded values)
        int updateMask = 0;
        for (UpdateFlag flag : player.getUpdateFlags().keySet()) {
            if (player.getUpdateFlags().isRequired(flag)) {
                updateMask |= flag.getMask(player.getType());
            }
        }

        int cacheStartOffset = buf.getBuffer().writerIndex(); // mark beginning for cache copy

        // Hyperion-style mask overflow handling with proper flag indication
        if (updateMask >= 0x100) {
            updateMask |= 0x40;  // Set overflow flag
            buf.put(updateMask & 0xFF);      // Low byte
            buf.put(updateMask >> 8);        // High byte
        } else {
            buf.put(updateMask);             // Single byte
        }

        // Process update blocks in Hyperion's proven order for client compatibility
        if (player.getUpdateFlags().isRequired(UpdateFlag.GRAPHICS))
            appendGraphic(player, buf);
        if (player.getUpdateFlags().isRequired(UpdateFlag.ANIM))
            appendAnimationRequest(player, buf);
        if (player.getUpdateFlags().isRequired(UpdateFlag.FORCED_CHAT))
            appendForcedChatText(player, buf);
        if (player.getUpdateFlags().isRequired(UpdateFlag.CHAT))
            appendPlayerChatText(player, buf);
        if (player.getUpdateFlags().isRequired(UpdateFlag.FACE_CHARACTER))
            appendFaceCharacter(player, buf);
        if (player.getUpdateFlags().isRequired(UpdateFlag.APPEARANCE))
            appendPlayerAppearance(player, buf);
        if (player.getUpdateFlags().isRequired(UpdateFlag.FACE_COORDINATE))
            appendFaceCoordinates(player, buf);
        if (player.getUpdateFlags().isRequired(UpdateFlag.HIT))
            appendPrimaryHit(player, buf);
        if (player.getUpdateFlags().isRequired(UpdateFlag.HIT2))
            appendPrimaryHit2(player, buf);
        if (player.getUpdateFlags().isRequired(UpdateFlag.FORCED_MOVEMENT))
            player.appendMask400Update(buf);

        // ---- Cache the freshly built update block for reuse ----
        int length = buf.getBuffer().writerIndex() - cacheStartOffset;
        if (length > 0) {
            byte[] copy = new byte[length];
            buf.getBuffer().getBytes(cacheStartOffset, copy, 0, length);
            player.cacheUpdateBlock(copy, length);
        }
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
        // Only write the used portion of the appearance buffer
        byte[] appearanceData = new byte[playerProps.getBuffer().writerIndex()];
        playerProps.getBuffer().getBytes(0, appearanceData);
        buf.putBytes(appearanceData);
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
