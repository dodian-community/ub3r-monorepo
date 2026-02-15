package net.dodian.uber.game.netty.listener;

import net.dodian.uber.game.netty.listener.in.WalkingListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages packet listener registration and provides backward compatibility.
 * Now uses the centralized PacketRepository for improved performance and maintainability.
 */
public final class PacketListenerManager {

    private static final Logger logger = LoggerFactory.getLogger(PacketListenerManager.class);
    
    /**
     * The centralized packet repository.
     */
    private static final PacketRepository repository = PacketRepository.getInstance();
    
    static {
        // Ensure essential listeners are registered
        try {
            logger.debug("Starting to register packet listeners...");
            Class.forName("net.dodian.uber.game.netty.listener.in.KeepAliveListener");
            // Load specific opcode listeners first so bridge sees them as occupied
            Class.forName("net.dodian.uber.game.netty.listener.in.WalkingListener");
            // Load ChangeRegionListener so opcodes 121/210 are claimed
            Class.forName("net.dodian.uber.game.netty.listener.in.ChangeRegionListener");
            // Load ChangeAppearanceListener for opcode 101
            Class.forName("net.dodian.uber.game.netty.listener.in.ChangeAppearanceListener");
            // Load ChatListener for opcode 4
            Class.forName("net.dodian.uber.game.netty.listener.in.ChatListener");
            // Load UseItemOnPlayerListener for opcode 14
            Class.forName("net.dodian.uber.game.netty.listener.in.UseItemOnPlayerListener");
            // Load FollowPlayerListener for opcode 39
            Class.forName("net.dodian.uber.game.netty.listener.in.FollowPlayerListener");
            // Load ItemOnGroundItemListener for opcode 25
            Class.forName("net.dodian.uber.game.netty.listener.in.ItemOnGroundItemListener");
            // Load UseItemOnNpcListener for opcode 57
            Class.forName("net.dodian.uber.game.netty.listener.in.UseItemOnNpcListener");
            // Load LegacyBridge then ItemOnItem so listener overwrites any bridge registration
            Class.forName("net.dodian.uber.game.netty.listener.in.ItemOnItemListener");
            // Load ClickItem2Listener for opcode 16
            Class.forName("net.dodian.uber.game.netty.listener.in.ClickItem2Listener");
            // Load ClickItemListener for opcode 122
            Class.forName("net.dodian.uber.game.netty.listener.in.ClickItemListener");
            // Load consolidated object interaction listener (132/252/70/192/35)
            Class.forName("net.dodian.uber.game.netty.listener.in.ObjectInteractionListener");
            // Load NpcInteractionListener for click/attack npc opcodes (155/17/21/18/72)
            Class.forName("net.dodian.uber.game.netty.listener.in.NpcInteractionListener");
            // Load MagicOnPlayerListener for opcode 249
            Class.forName("net.dodian.uber.game.netty.listener.in.MagicOnPlayerListener");
            // Load TradeListener for opcode 139
            Class.forName("net.dodian.uber.game.netty.listener.in.TradeListener");
            // Load TradeRequestListener for opcode 128
            Class.forName("net.dodian.uber.game.netty.listener.in.TradeRequestListener");
            // Load SendPrivateMessageListener for opcode 126
            Class.forName("net.dodian.uber.game.netty.listener.in.SendPrivateMessageListener");
            Class.forName("net.dodian.uber.game.netty.listener.in.MagicOnItemsListener"); // opcode 237
            Class.forName("net.dodian.uber.game.netty.listener.in.RemoveItemListener");
            // Load WearItemListener for opcode 41
            Class.forName("net.dodian.uber.game.netty.listener.in.AttackPlayerListener"); // opcode
            Class.forName("net.dodian.uber.game.netty.listener.in.WearItemListener");
            // Load DropItemListener for opcode 87
            Class.forName("net.dodian.uber.game.netty.listener.in.FocusChangeListener"); // opcode 3
            Class.forName("net.dodian.uber.game.netty.listener.in.CommandsListener"); // opcode 103
            // Load examine listeners for Mystic client compatibility
            Class.forName("net.dodian.uber.game.netty.listener.in.ExamineListener"); // opcode 2
            Class.forName("net.dodian.uber.game.netty.listener.in.ClickingButtonsListener"); // opcode 185
            Class.forName("net.dodian.uber.game.netty.listener.in.AddFriendListener"); // opcode 188
            Class.forName("net.dodian.uber.game.netty.listener.in.AddIgnoreListener"); // opcode 133
            Class.forName("net.dodian.uber.game.netty.listener.in.DuelRequestListener"); // opcode 153
            Class.forName("net.dodian.uber.game.netty.listener.in.RemoveFriendListener"); // opcode 215
            Class.forName("net.dodian.uber.game.netty.listener.in.RemoveIgnoreListener"); // opcode 74
            // Load ClickItem3Listener for opcode 75
            Class.forName("net.dodian.uber.game.netty.listener.in.ClickItem3Listener");
            // Load DialogueListener for opcode 40
            Class.forName("net.dodian.uber.game.netty.listener.in.DialogueListener");
            // Load ClickingStuffListener for opcode 130
            Class.forName("net.dodian.uber.game.netty.listener.in.ClickingStuffListener");
            // Load DropItemListener for opcode 87
            Class.forName("net.dodian.uber.game.netty.listener.in.DropItemListener");
            // Load PickUpGroundItemListener for opcode 236
            Class.forName("net.dodian.uber.game.netty.listener.in.PickUpGroundItemListener");
            
            // Load bank-related listeners
            Class.forName("net.dodian.uber.game.netty.listener.in.BankAllListener");  // opcode 129
            Class.forName("net.dodian.uber.game.netty.listener.in.Bank5Listener");     // opcode 117
            Class.forName("net.dodian.uber.game.netty.listener.in.BankX1Listener");    // opcode 135
            Class.forName("net.dodian.uber.game.netty.listener.in.BankX2Listener");    // opcode 208
            
            // Load MagicOnNpcListener for opcode 131
            Class.forName("net.dodian.uber.game.netty.listener.in.MagicOnNpcListener");
            
            // Load other missing listeners
            Class.forName("net.dodian.uber.game.netty.listener.in.Bank10Listener");
            Class.forName("net.dodian.uber.game.netty.listener.in.MouseClicksListener");
            Class.forName("net.dodian.uber.game.netty.listener.in.MoveItemsListener");
            Class.forName("net.dodian.uber.game.netty.listener.in.UpdateChatListener");
            
            // Register no-op handlers for unused opcodes
            repository.registerNoOp(77);  // Currently unused
            repository.registerNoOp(202); // Client idle logout (not used)
            repository.registerNoOp(230); // Anti-cheat/checks for 2nd click npc (not used)
            repository.registerNoOp(86);  // Camera movement (unused)
            
            // Note: WalkingListener registers itself for opcodes 248, 164, 98 in its static block
            
            // Lock the repository to prevent further modifications
            repository.lock();
            logger.info("All packet listeners registered successfully");
        } catch (Exception e) {
            logger.error("Failed to register packet listeners", e);
        }
    }

    private PacketListenerManager() {}

    /**
     * Registers a packet listener for the specified opcode.
     * Delegates to the centralized PacketRepository.
     */
    public static void register(int opcode, PacketListener listener) {
        repository.register(opcode, listener);
    }

    /**
     * Retrieves a packet listener for the specified opcode.
     * Delegates to the centralized PacketRepository.
     */
    public static PacketListener get(int opcode) {
        return repository.get(opcode);
    }

    /**
     * Gets the packet repository instance.
     * Useful for advanced operations.
     */
    public static PacketRepository getRepository() {
        return repository;
    }
}
