package net.dodian.uber.game.model.entity.player;

import net.dodian.uber.game.netty.listener.out.*;
import net.dodian.uber.game.engine.systems.dialogue.DialogueService;
import net.dodian.uber.game.engine.systems.action.PlayerActionCancelReason;
import net.dodian.uber.game.api.content.ContentActions;

public class InterfaceManager {

    /** The player instance. */
    private final Client player;

    /** The current overlay interface. */
    private int overlay = -1;

    /** The current walkable-interface. */
    private int walkable = -1;

    /** The current dialogue. */
    private int dialogue = -1;

    private final int[] sidebars = new int[15];

    /** Creates a new InterfaceManager. */
    public InterfaceManager(Client player) {
        this.player = player;
    }

    /** Opens an interface for the player. */
    public void open(int identification) {
        open(identification, true);
    }

    /** Opens an interface for the player with security checks. */
    public void open(int identification, boolean secure) {
        if (player.viewingAccountServices && identification != 36700) {
            net.dodian.uber.game.ui.AccountServices.close(player);
        }
        if (secure) {
            if (player.activeInterfaceId == identification) {
                return;
            }

            if (player.isInCombat()) {
                player.send(new SendMessage("You can't do this right now!"));
                return;
            }

            if (DialogueService.hasActiveSession(player)) {
                DialogueService.clear(player, false);
            }
        }

        player.activeInterfaceId = identification;
        player.resetWalkingQueue();
        player.resetAction();
        player.send(new ShowInterface(identification));
        player.send(new SendString("[CLOSE_MENU]", 0));
        setSidebar(14, -1); // 14 is LOGOUT_TAB
    }

    /** Opens a walkable-interface for the player. */
    public void openWalkable(int identification) {
        if (walkable == identification) {
            return;
        }
        walkable = identification;
        player.setWalkableInterface(identification);
    }

    /** Opens an inventory interface for the player. */
    public void openInventory(int identification, int overlay) {
        if (player.viewingAccountServices) {
            net.dodian.uber.game.ui.AccountServices.close(player);
        }
        if (player.activeInterfaceId == identification && this.overlay == overlay) {
            return;
        }

        player.activeInterfaceId = identification;
        this.overlay = overlay;
        player.resetWalkingQueue();
        player.resetAction();
        player.send(new SendString("[CLOSE_MENU]", 0));
        player.send(new InventoryInterface(identification, overlay));
        setSidebar(14, -1); // 14 is LOGOUT_TAB
    }

    public void close(int interfaceId) {
        if (isInterfaceOpen(interfaceId)) {
            close();
        }
    }

    /** Clears the player's screen. */
    public void close() {
        close(true);
    }

    /** Handles clearing the screen. */
    public void close(boolean walkable) {
        if (player.viewingAccountServices) {
            net.dodian.uber.game.ui.AccountServices.close(player);
        }
        boolean refreshItems = false;

        if (player.IsBanking) {
            player.IsBanking = false;
            player.bankSearchActive = false;
            player.getContentRuntimeState().clearPendingInputState();
            player.bankSearchQuery = "";
            player.currentBankTab = 0;
            refreshItems = true;
        }

        if (player.checkBankInterface) {
            player.checkBankInterface = false;
            refreshItems = true;
        }

        if (player.bankStyleViewOpen) {
            player.clearBankStyleView();
            refreshItems = true;
        }

        if (player.checkInv) {
            player.checkInv = false;
            player.resetItems(3214);
        }

        if (player.isPartyInterface) {
            player.isPartyInterface = false;
            refreshItems = true;
        }

        if (player.isShopping()) {
            player.MyShopID = -1;
            refreshItems = true;
        }

        if (net.dodian.uber.game.economy.PriceCheckerService.isOpen(player)) {
            net.dodian.uber.game.economy.PriceCheckerService.close(player);
            refreshItems = true;
        }

        if (player.getInTrade()) {
            net.dodian.uber.game.social.exchange.TradingService.decline(player);
        }

        if (player.getInDuel()) {
            net.dodian.uber.game.social.exchange.DuelingService.decline(player);
        }

        if (walkable) {
            openWalkable(-1);
        }

        player.activeInterfaceId = -1;
        dialogue = -1;
        
        ContentActions.cancel(
            player,
            PlayerActionCancelReason.INTERFACE_CLOSED,
            false,
            false,
            false,
            true
        );
        DialogueService.closeBlockingDialogue(player, false);
        
        player.send(new RemoveInterfaces());
        setSidebar(14, 2449); // 14 is LOGOUT_TAB, 2449 is default logout interface
        
        if (player.refundSlot != -1) {
            player.refundSlot = -1;
        }
        net.dodian.uber.game.engine.systems.skills.SkillItemListMenuService.INSTANCE.clear(player);

        if (refreshItems) {
            player.checkItemUpdate();
        }
    }

    public void setSidebar(int tab, int id) {
        if (tab < 0 || tab >= sidebars.length) {
            return;
        }
        if (sidebars[tab] == id && id != -1) {
            return;
        }
        sidebars[tab] = id;
        player.send(new SetSidebarInterface(tab, id));
    }

    /** Checks if a certain interface is open. */
    public boolean isInterfaceOpen(int id) {
        return player.activeInterfaceId == id;
    }

    public boolean hasAnyOpen(int... ids) {
        for (int id : ids) {
            if (player.activeInterfaceId == id)
                return true;
        }
        return false;
    }

    /** Checks if the player's screen is clear. */
    public boolean isClear() {
        return player.activeInterfaceId == -1 && dialogue == -1 && walkable == -1;
    }

    /** Checks if the main interface is clear. */
    public boolean isMainClear() {
        return player.activeInterfaceId == -1;
    }

    /** Checks if the dialogue interface is clear. */
    public boolean isDialogueClear() {
        return dialogue == -1;
    }

    /** Sets the current interface. */
    public void setMain(int currentInterface) {
        player.activeInterfaceId = currentInterface;
    }

    /** Gets the current main interface. */
    public int getMain() {
        return player.activeInterfaceId;
    }

    /** Gets the dialogue interface. */
    public int getDialogue() {
        return dialogue;
    }

    /** Sets the dialogue interface. */
    public void setDialogue(int dialogueInterface) {
        this.dialogue = dialogueInterface;
    }

    /** Gets the walkable interface. */
    public int getWalkable() {
        return walkable;
    }

    /** Sets the walkable interface. */
    public void setWalkable(int walkableInterface) {
        this.walkable = walkableInterface;
    }

    public int getSidebar(int tab) {
        if (tab < 0 || tab >= sidebars.length) {
            return -1;
        }
        return sidebars[tab];
    }

    public boolean isSidebar(int tab, int id) {
        return tab >= 0 && tab < sidebars.length && sidebars[tab] == id;
    }

    public boolean hasSidebar(int id) {
        for (int sidebar : sidebars) {
            if (sidebar == id) {
                return true;
            }
        }
        return false;
    }
}
