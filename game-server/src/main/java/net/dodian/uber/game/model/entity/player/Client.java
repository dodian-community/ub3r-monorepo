package net.dodian.uber.game.model.entity.player;
import net.dodian.uber.game.api.content.ContentInteraction;
import net.dodian.uber.game.api.content.ContentActions;
import net.dodian.uber.game.combat.AncientSpellRegistry;
import net.dodian.uber.game.engine.config.DotEnvKt;
import net.dodian.uber.game.engine.loop.GameCycleClock;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.engine.event.GameEventBus;
import net.dodian.uber.game.engine.event.GameEventScheduler;
import net.dodian.uber.game.events.player.PlayerLogoutEvent;
import net.dodian.uber.game.events.item.ItemPickupEvent;
import net.dodian.uber.game.events.trade.TradeCancelEvent;
import net.dodian.uber.game.events.trade.TradeCompleteEvent;
import net.dodian.uber.game.events.trade.TradeRequestEvent;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.shop.ShopCatalog;
import net.dodian.uber.game.shop.ShopManager;
import net.dodian.uber.game.shop.ShopRulesService;
import net.dodian.uber.game.model.entity.UpdateFlag;
import net.dodian.uber.game.model.entity.Entity;
import net.dodian.uber.game.model.entity.npc.Npc;
import net.dodian.uber.game.model.item.*;
import net.dodian.uber.game.model.objects.DoorRegistry;
import net.dodian.uber.game.model.objects.WorldObject;
import net.dodian.uber.skills.api.Skillcape;
import net.dodian.uber.game.ui.bank.PlayerBankService;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.ui.QuestTabEntry;
import net.dodian.uber.game.model.player.skills.Skill;
import net.dodian.uber.game.model.player.skills.Skills;
import net.dodian.uber.game.engine.systems.skills.prayer.PrayerManager;
import net.dodian.uber.game.engine.systems.skills.farming.Farming;
import net.dodian.uber.game.engine.systems.skills.farming.FarmingState;
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry;
import net.dodian.uber.game.persistence.admin.CommandDbService;
import net.dodian.uber.game.persistence.account.AccountPersistenceService;
import net.dodian.uber.game.persistence.player.PlayerSaveReason;
import net.dodian.uber.game.persistence.player.PlayerSaveSegment;
import net.dodian.uber.game.engine.net.InboundPacketMailbox;
import net.dodian.uber.game.engine.net.OutboundSessionQueue;
import net.dodian.uber.game.engine.processing.EntityProcessor;
import net.dodian.uber.game.engine.metrics.PacketErrorTelemetry;
import net.dodian.uber.game.engine.metrics.PacketRejectTelemetry;
import net.dodian.uber.game.engine.systems.dialogue.DialogueOptionService;
import net.dodian.uber.game.ui.dialogue.DialogueDisplayService;
import net.dodian.uber.game.engine.systems.dialogue.DialogueService;
import net.dodian.uber.game.netty.listener.out.*;
import net.dodian.uber.game.activity.partyroom.PartyRoomRewardItem;
import net.dodian.uber.game.persistence.audit.*;
import net.dodian.uber.game.engine.systems.action.PlayerActionCancellationService;
import net.dodian.uber.game.engine.systems.action.PlayerActionCancelReason;
import net.dodian.uber.game.engine.systems.action.PlayerActionType;
import net.dodian.uber.game.engine.systems.action.TeleportActionService;
import net.dodian.uber.game.engine.systems.action.TravelDecision;
import net.dodian.uber.game.engine.systems.action.TravelRouteService;
import net.dodian.uber.game.engine.systems.action.DuelCountdownService;
import net.dodian.uber.game.engine.systems.action.DuelCountdownState;
import net.dodian.uber.game.engine.systems.animation.PlayerAnimationService;
import net.dodian.uber.game.engine.systems.combat.CombatStartService;
import net.dodian.uber.game.engine.systems.interaction.PlayerInteractionGuardService;
import net.dodian.uber.game.engine.systems.interaction.items.ItemDispatcher;
import net.dodian.uber.game.engine.systems.interaction.InteractionAnchorState;
import net.dodian.uber.game.engine.systems.interaction.ui.TradeDuelSessionService;
import net.dodian.uber.game.engine.lifecycle.PlayerDeferredLifecycleService;
import net.dodian.uber.game.engine.tasking.PlayerScopedCoroutineService;
import net.dodian.uber.game.engine.systems.net.PacketRejectReason;
import net.dodian.uber.game.engine.util.Misc;
import net.dodian.utilities.MD5;
import net.dodian.utilities.Utils;
import net.dodian.uber.game.engine.systems.skills.ProgressionService;
import net.dodian.uber.game.engine.systems.inventory.EconomyTransaction;
import net.dodian.uber.game.model.item.transaction.OfferTransactions;

import java.util.*;
import io.netty.channel.Channel;

/* Kotlin imports */
import net.dodian.uber.game.engine.systems.world.item.Ground;
import static net.dodian.uber.game.combat.CombatPlayerExtensionsKt.getRangedStr;
import static net.dodian.uber.game.model.player.skills.Skill.*;
import static net.dodian.uber.game.engine.config.DotEnvKt.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class Client extends Player implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(Client.class);
    private static final int MAX_PENDING_INBOUND_PACKETS = 200;
    private static final int RECENT_INBOUND_TRACE_SIZE = 10;

    public Channel channel;
    private final InboundPacketMailbox inboundPacketMailbox = new InboundPacketMailbox(MAX_PENDING_INBOUND_PACKETS);
    private long currentInboundPacketSequence = -1L;
    private final OutboundSessionQueue outboundSessionQueue = new OutboundSessionQueue();
    private final java.util.concurrent.atomic.AtomicBoolean inboundReadyQueued = new java.util.concurrent.atomic.AtomicBoolean();
    private final int[] recentInboundOpcodes = new int[RECENT_INBOUND_TRACE_SIZE];
    private final int[] recentInboundSizes = new int[RECENT_INBOUND_TRACE_SIZE];
    private final int[] recentInboundCycles = new int[RECENT_INBOUND_TRACE_SIZE];
    private int recentInboundWriteIndex = 0;
    private int recentInboundCount = 0;

    public Farming farming = new Farming();
    public FarmingState farmingJson = new FarmingState();
    public final InterfaceManager interfaceManager = new InterfaceManager(this);
    public boolean immune = false, loadingDone = false, reloadHp = false;
    public boolean canPreformAction = true;
    public long lastDropCycle = 0L; //used for limiting drops per tick and logging out delayd
    public boolean isLoggingOut = false; //new flag 
    long lastBar = 0;
    public long lastSave, lastProgressSave;
    public Entity target = null;
    public int otherdbId = -1;
    public int convoId = -1, nextDiag = -1, npcFace = 591;
    public boolean pLoaded = false;
    public int maxQuests = QuestTabEntry.VALUES.length;
    public int[] quests = new int[maxQuests];
    public int[] playerBonus = new int[14];
    private final Map<Integer, String> uiTextCache = new HashMap<>();
    private int lastWildLevelSent = -1;
    private String lastTopBarText = null;
    private int currentWalkableInterface = -1;
    private int lastWalkableInterfaceSent = -2;
    private boolean walkableInterfaceDirty = true;
    private final boolean[] lastMenuEnabled = new boolean[6];
    private final String[] lastMenuText = new String[6];
    private boolean menuCacheInitialized = false;
    private int lastPlayerUpdateCapacity = 8192;
    private int lastNpcUpdateCapacity = 16384;
    /**
     * Tracks if the client window currently has focus.
     */
    private boolean windowFocused = true;
    public String failer = "";
    public long mutedTill;
    public long session_start = 0;
    public GroundItem attemptGround = null;
    // Game-thread-only: mutated by friend/ignore-list packet handlers, read during login
    // (AccountLoginMapper) and periodic save-snapshot building (WorldMaintenanceService), all of
    // which run on the game thread. The async DB save worker only ever touches the already-copied
    // snapshot data, never these live lists, so CopyOnWriteArrayList's per-write full-array-copy
    // cost was pure overhead with no concurrency to guard against.
    public ArrayList<Friend> friends = new ArrayList<>();
    public ArrayList<Friend> ignores = new ArrayList<>();
    public boolean tradeLocked = false;
    public boolean officialClient = true;
    public String[] UUID = null;

    private net.dodian.uber.game.social.exchange.ExchangePlayerState exchangeState() {
        return net.dodian.uber.game.social.exchange.ExchangePlayerStateRegistry.state(this);
    }

    public boolean getDuelRequested() { return exchangeState().getDuelRequested(); }
    public void setDuelRequested(boolean value) { exchangeState().setDuelRequested(value); }
    public boolean getInDuel() { return exchangeState().getInDuel(); }
    public void setInDuel(boolean value) { exchangeState().setInDuel(value); }
    public boolean getDuelConfirmed() { return exchangeState().getDuelConfirmed(); }
    public void setDuelConfirmed(boolean value) { exchangeState().setDuelConfirmed(value); }
    public boolean getDuelConfirmed2() { return exchangeState().getDuelConfirmed2(); }
    public void setDuelConfirmed2(boolean value) { exchangeState().setDuelConfirmed2(value); }
    public boolean getDuelFight() { return exchangeState().getDuelFight(); }
    public void setDuelFight(boolean value) { exchangeState().setDuelFight(value); }
    public boolean getDuelWin() { return exchangeState().getDuelWin(); }
    public void setDuelWin(boolean value) { exchangeState().setDuelWin(value); }
    public long getLastDuelItemChangeCycle() { return exchangeState().getLastDuelItemChangeCycle(); }
    public void setLastDuelItemChangeCycle(long value) { exchangeState().setLastDuelItemChangeCycle(value); }
    public int getDuel_with() { return exchangeState().getDuelPartnerSlot(); }
    public void setDuel_with(int value) { exchangeState().setDuelPartnerSlot(value); }
    public boolean getTradeRequested() { return exchangeState().getTradeRequested(); }
    public void setTradeRequested(boolean value) { exchangeState().setTradeRequested(value); }
    public boolean getInTrade() { return exchangeState().getInTrade(); }
    public void setInTrade(boolean value) { exchangeState().setInTrade(value); }
    public boolean getCanOffer() { return exchangeState().getCanOffer(); }
    public void setCanOffer(boolean value) { exchangeState().setCanOffer(value); }
    public boolean getTradeConfirmed() { return exchangeState().getTradeConfirmed(); }
    public void setTradeConfirmed(boolean value) { exchangeState().setTradeConfirmed(value); }
    public boolean getTradeConfirmed2() { return exchangeState().getTradeConfirmed2(); }
    public void setTradeConfirmed2(boolean value) { exchangeState().setTradeConfirmed2(value); }
    public boolean getTradeResetNeeded() { return exchangeState().getTradeResetNeeded(); }
    public void setTradeResetNeeded(boolean value) { exchangeState().setTradeResetNeeded(value); }
    public int getTrade_reqId() { return exchangeState().getTradePartnerSlot(); }
    public void setTrade_reqId(int value) { exchangeState().setTradePartnerSlot(value); }
    public boolean[] getDuelRule() { return exchangeState().getDuelRule(); }
    public void setDuelRule(boolean[] value) { exchangeState().setDuelRule(value); }
    public boolean[] getDuelBodyRules() { return exchangeState().getDuelBodyRules(); }
    public int acceptAid = 0; // 0 = off, 1 = on
    public int rightClickToggle = 0; // 0 = off, 1 = on
    public int scrollCameraToggle = 0; // 0 = off, 1 = on
    public int entityAttackOption = 0; // 0 = Depends on combat levels, 1 = Always right-click, 2 = Left click where available, 3 = Hidden
    public boolean adding = false;
    public ArrayList<WorldObject> objects = new ArrayList<>();
    public long lastButton = 0;
    public int enterAmountId = 0;
    public int makeAllSelectedAmount = 1;
    // Dodian: teleports
    private int tH = 0;
    public int cSelected = -1, cIndex = -1;
    public String dMsg = "";

    public int dialogInterface = 2459;
    public int random_skill = -1;
    public String[] otherGroups = new String[10];
    public int autocast_spellIndex = -1, magicId = -1;
    public int loginDelay = 0;
    public boolean validClient = true;
    public int newPms = 0;
    private java.util.List<Integer> dropDisplayKey = new java.util.ArrayList<>();
    public java.util.List<Integer> getDropDisplayKey() { return dropDisplayKey; }
    public void setDropDisplayKey(java.util.List<Integer> key) { this.dropDisplayKey = key; }

    public String properName = "";
    public int actionButtonId = 0;
    public int skillX = 0, skillY = 0;
    public int stairs = 0, stairDistance = 0;
    public boolean validLogin = false;

    /**
     * Replaces an object in the game world.
     * @param pos The position of the object
     * @param newObjectId The new object ID to place (-1 to remove)
     * @param face The object's face/direction
     * @param objectType The type of the object
     */
    public void ReplaceObject2(Position pos, int newObjectId, int face, int objectType) {
        if (!withinDistance(new int[]{pos.getX(), pos.getY(), 60}) || getPosition().getZ() != pos.getZ()) {
            return;
        }
       // System.out.println("ReplaceObject2: " + pos + ", " + newObjectId + ", " + face + ", " + objectType);
        send(new SetMap(pos));
        send(new ReplaceObject2(newObjectId, face, objectType));
    }

    /**
     * @param o 0 = X | 1 = Y | = Distance allowed.
     */
    private boolean withinDistance(int[] o) {
        int dist = o[2];
        int deltaX = o[0] - getPosition().getX(), deltaY = o[1] - getPosition().getY();
        return (deltaX <= (dist - 1) && deltaX >= -dist && deltaY <= (dist - 1) && deltaY >= -dist);
    }

    public boolean wearing = false;

    /** Sends interface text only when the component value has changed. */
    public void sendCachedString(String text, int lineId) {
        String previous = uiTextCache.get(lineId);
        if (previous != null && previous.equals(text)) {
            return;
        }
        uiTextCache.put(lineId, text);
        send(new SendString(text, lineId));
    }

    public void invalidateUiText(int lineId) {
        uiTextCache.remove(lineId);
        if (lineId == 6570) {
            lastTopBarText = null;
        }
    }

    public void invalidateWalkableUiTexts() {
        invalidateUiText(6570);
        invalidateUiText(6572);
        invalidateUiText(6664);
    }

    public void setWalkableInterface(int id) {
        if (currentWalkableInterface != id) {
            currentWalkableInterface = id;
            walkableInterfaceDirty = true;
            invalidateWalkableUiTexts();
        }
        if (walkableInterfaceDirty || lastWalkableInterfaceSent != id) {
            lastWalkableInterfaceSent = id;
            walkableInterfaceDirty = false;
            send(new SetInterfaceWalkable(id));
        }
    }

    public void clearWalkableInterface() {
        setWalkableInterface(-1);
    }

    public void forceWalkableInterfaceRefresh() {
        walkableInterfaceDirty = true;
        lastWalkableInterfaceSent = -2;
        invalidateWalkableUiTexts();
    }

    public int getCurrentWalkableInterface() {
        return currentWalkableInterface;
    }

    public boolean isWalkableInterfaceActive(int id) {
        return currentWalkableInterface == id;
    }

    public void onPostLoginUiInit() {
        WriteEnergy();
        forceWalkableInterfaceRefresh();
        updatePlayerDisplay();
    }

    public void setPlayerContextMenu(int slot, boolean enabled, String text) {
        if (slot < 0 || slot >= lastMenuEnabled.length) {
            send(new PlayerContextMenu(slot, enabled, text));
            return;
        }
        if (!menuCacheInitialized) {
            Arrays.fill(lastMenuText, null);
            menuCacheInitialized = true;
        }
        if (lastMenuEnabled[slot] == enabled && Objects.equals(lastMenuText[slot], text)) {
            return;
        }
        lastMenuEnabled[slot] = enabled;
        lastMenuText[slot] = text;
        send(new PlayerContextMenu(slot, enabled, text));
    }

    public int getbattleTimer(int weapon) {
        int wepPlainTick = Server.itemManager.getAttackSpeed(weapon);
        if (getContentRuntimeState().isCombatUsingBow() && fightType == 2) //Rapid style
            wepPlainTick -= 1;
        return wepPlainTick;
    }

    public boolean hasStaff() {
        int weaponId = getEquipment()[Equipment.Slot.WEAPON.getId()];
        String weaponType = Server.itemManager.getWeaponType(weaponId);
        return weaponType.equals("staff") || weaponType.equals("powered_staff");
    }

    public void CheckGear() {
        checkBow();
        if (!hasStaff()) autocast_spellIndex = -1;
    }

    public int distanceToPoint(int pointX, int pointY) {
        int dx = getPosition().getX() - pointX;
        int dy = getPosition().getY() - pointY;
        return (int) Math.sqrt(dx * dx + dy * dy);
    }

    public int distanceToPoint(Position checkOne, Position checkTwo) {
        int dx = checkOne.getX() - checkTwo.getX();
        int dy = checkOne.getY() - checkTwo.getY();
        return (int) Math.sqrt(dx * dx + dy * dy);
    }

    public void animation(int id, Position pos) {
        for (int i = 0; i < PlayerRegistry.players.length; i++) {
            Player p = net.dodian.uber.game.engine.systems.world.player.PlayerRegistry.players[i];
            if (p != null) {
                Client person = (Client) p;
                if (person.isActive && !person.disconnected && person.getPosition() != null
                        && person.distanceToPoint(pos.getX(), pos.getY()) <= 60
                        && person.getPosition().getZ() == pos.getZ())
                    person.animation2(id, pos);
            }
        }
    }

    public void animation2(int id, Position pos) {
        send(new Animation2(id, pos, 0, 0));
    }

    public void stillgfx(int id, Position pos, int height, boolean showAll) {
        if (showAll) {
            for (int i = 0; i < PlayerRegistry.players.length; i++) {
                Player p = net.dodian.uber.game.engine.systems.world.player.PlayerRegistry.players[i];
                if (p != null) {
                    Client person = (Client) p;
                    // Plane check must be against the viewer (person), not the caster (this) -
                    // comparing this.getPosition().getZ() let the graphic render through every
                    // floor of a multi-storey building for anyone within 60 tiles on any plane.
                    if (person.isActive && !person.disconnected && person.getPosition() != null
                            && person.distanceToPoint(pos.getX(), pos.getY()) <= 60
                            && person.getPosition().getZ() == pos.getZ())
                        person.stillgfx2(id, pos, height, 0);
                }
            }
        } else stillgfx2(id, pos, height, 0);
    }

    public void stillgfx(int id, int y, int x) {
        stillgfx(id, new Position(x, y, getPosition().getZ()), 0, true);
    }

    public void stillgfx(int id, Position pos, int height) {
        stillgfx(id, pos, height, false);
    }

    /**
     * Displays a still graphic at the specified position.
     * 
     * @param id The graphic ID to display
     * @param pos The position where to display the graphic
     * @param height The height offset of the graphic
     * @param time The time before the graphic is cast (in game ticks)
     */
    public void stillgfx2(int id, Position pos, int height, int time) {
        send(new StillGraphic(id, pos, height, time, false));
    }

    /**
     * Creates a projectile in the game world.
     *
     * @param casterY The Y coordinate of the caster
     * @param casterX The X coordinate of the caster
     * @param offsetY The Y offset from the caster's position
     * @param offsetX The X offset from the caster's position
     * @param speed The speed of the projectile
     * @param gfxMoving The graphic ID of the projectile
     * @param startHeight The starting height of the projectile
     * @param endHeight The ending height of the projectile
     * @param targetIndex The index of the target (NPC or player)
     * @param begin The tick when the projectile is created
     * @param slope The initial slope of the projectile
     * @param initDistance The initial distance from the source
     */
    public void createProjectile(int casterY, int casterX, int offsetY,
                               int offsetX, int speed, int gfxMoving, int startHeight,
                               int endHeight, int targetIndex, int begin, int slope, int initDistance) {
        try {
            Position casterPosition = new Position(casterX, casterY);
            send(new Projectile(casterPosition, offsetY, offsetX, speed, gfxMoving,
                    startHeight, endHeight, targetIndex, begin, slope, initDistance));
        } catch (Exception e) {
            Server.logError(e.getMessage());
        }
    }

    public void arrowGfx(int offsetY, int offsetX, int speed,
                         int gfxMoving, int startHeight, int endHeight, int index, int begin, int slope) {
        forEachProjectileViewer(viewer ->
                viewer.createProjectile(getPosition().getY(), getPosition().getX(), offsetY, offsetX, speed, gfxMoving,
                        startHeight, endHeight, index, begin, slope, 64));
    }

    public void arrowNpcGfx(Position pos, int offsetY, int offsetX, int speed,
                            int gfxMoving, int startHeight, int endHeight, int index, int begin, int slope) {
        forEachProjectileViewer(viewer ->
                viewer.createProjectile(pos.getY(), pos.getX(), offsetY, offsetX, speed, gfxMoving,
                        startHeight, endHeight, index, begin, slope, 64));
    }

    private void forEachProjectileViewer(java.util.function.Consumer<Client> consumer) {
        Position source = getPosition();
        if (source == null || consumer == null) {
            return;
        }
        if (Server.chunkManager != null) {
            Server.chunkManager.forEachUpdatePlayerCandidate(this, 64, other -> {
                Client viewer = (Client) other;
                if (canViewProjectile(viewer, source, true)) {
                    consumer.accept(viewer);
                }
            });
            if (canViewProjectile(this, source, true)) {
                consumer.accept(this);
            }
            return;
        }
        PlayerRegistry.forEachActivePlayer(viewer -> {
            if (canViewProjectile(viewer, source, false)) {
                consumer.accept(viewer);
            }
        });
    }

    private boolean canViewProjectile(Client viewer, Position source, boolean requireSamePlane) {
        if (viewer == null || viewer.dbId <= 0 || viewer.disconnected || viewer.getPosition() == null
                || viewer.getPosition().getX() <= 0) {
            return false;
        }
        if (requireSamePlane && viewer.getPosition().getZ() != source.getZ()) {
            return false;
        }
        return viewer.getPosition().withinDistance(source, 64);
    }

    public void println_debug(String str) {
        logger.debug("[client-{}-{}] {}", getSlot(), getPlayerName(), str);
    }

    public void print_debug(String str) {
        println_debug(str);
    }

    public void println(String str) {
        logger.info("[client-{}-{}] {}", getSlot(), getPlayerName(), str);
    }

    public void rerequestAnim() {
        PlayerAnimationService.requestResetClear(this);
    }

    public void sendMessage(String text) {
        send(new SendMessage(text));
    }

    public void sendString(String text, int lineId) {
        send(new SendString(text, lineId));
    }

    public void sendInterfaceAnimation(int MainFrame, int SubFrame) {
        send(new SendInterfaceAnimation(MainFrame, SubFrame));
    }

    public void sendChatboxInterface(int Frame) {
        send(new SendChatboxInterface(Frame));
    }

    public void sendInterfaceModel(int MainFrame, int SubFrame, int SubFrame2) {
        send(new SendInterfaceModel(MainFrame, SubFrame, SubFrame2));
    }

    public void sendQuestSomething(int id) {
        send(new SetScrollPosition(id));
    }

    public void sendScrollbar(int scrollbar, int size) {
        send(new SendScrollbar(scrollbar, size));
    }

    public void clearQuestInterface() {
        for (int j : net.dodian.uber.game.ui.InterfaceConstants.QUEST_TEXT_COMPONENTS)
            send(new SendString("", j));
    }

    public void openInterface(int interfaceid) {
        interfaceManager.open(interfaceid);
    }

    public void closeInterfaces() {
        interfaceManager.close();
    }

    public int ancients = 1;

    public int i;
    public int XremoveSlot = 0;
    public int XinterfaceID = 0;
    public int XremoveID = 0;
    public int currentBankTab = 0;
    public int previousBankTab = 0;
    public int lastButtonActionIndex = -1;
    public boolean bankSearchActive = false;
    public int modcpDialogState = 0;
    public int accountServicesDialogState = 0;
    public boolean viewingAccountServices = false;
    public volatile int accountServiceRequestToken = 0;
    public java.util.List<String> modcpPlayerList = new java.util.ArrayList<>();
    public String managingName = "";
    public String bankSearchQuery = "";
    public int[] bankSlotTabs = null;
    public int[][] bankContainerSlotMap = null;
    public int[][] bankStyleViewSlotMap = null;
    public ArrayList<Integer> bankStyleViewIds = new ArrayList<>();
    public ArrayList<Integer> bankStyleViewAmounts = new ArrayList<>();
    public String bankStyleViewTitle = "";

    /**
     * Best-effort tracking of the currently opened "main" interface (via {@link ShowInterface}).
     * Used as a fail-safe for interface-owned button handlers (anti-spoof / anti-dupe).
     * Tab interfaces (sidebar) should not rely on this.
     */
    public int activeInterfaceId = -1;

    /**
     * Gameplay reason for the next interface open, consumed by the audit hook in {@link #send}.
     * Content code (bank, shops, ...) sets this right before sending its interface packets so the
     * INTERFACE OPEN audit line can say WHY the interface opened, not just which packet carried it.
     */
    public String pendingInterfaceOpenContext = null;

    public int WanneBank = 0;
    public int WanneShop = 0;
    public int WanneThieve = 0;


    // Stream cipher fields removed; Netty owns packet I/O now.
   // public ISAACCipher inStreamDecryption = null, outStreamDecryption = null;

    public final java.util.concurrent.atomic.AtomicInteger timeOutCounter = new java.util.concurrent.atomic.AtomicInteger(); // to detect timeouts on the connection to the client
    public int returnCode = 2; // Tells the client if the login was successfull
    private volatile long lastInboundKeepAliveAtMillis = System.currentTimeMillis();
    private volatile long lastIdlePlayerSyncSentAtMillis = 0L;
    private volatile String lastDisconnectReason = "unknown";




    public Client(Channel channel, int _playerId) {
        super(_playerId);
        this.channel = channel;

    }

    public static final class InboundProcessResult {
        private final int processedPackets;
        private final int walkPacketsProcessed;
        private final int mousePacketsProcessed;
        private final int walkPacketsReplaced;
        private final int mousePacketsReplaced;
        private final int itemClickPacketsReplaced;
        private final int fifoPacketsDropped;

        public InboundProcessResult(int processedPackets, int walkPacketsProcessed, int mousePacketsProcessed,
                                    int walkPacketsReplaced, int mousePacketsReplaced, int itemClickPacketsReplaced,
                                    int fifoPacketsDropped) {
            this.processedPackets = processedPackets;
            this.walkPacketsProcessed = walkPacketsProcessed;
            this.mousePacketsProcessed = mousePacketsProcessed;
            this.walkPacketsReplaced = walkPacketsReplaced;
            this.mousePacketsReplaced = mousePacketsReplaced;
            this.itemClickPacketsReplaced = itemClickPacketsReplaced;
            this.fifoPacketsDropped = fifoPacketsDropped;
        }

        public int processedPackets() {
            return processedPackets;
        }

        public int walkPacketsProcessed() {
            return walkPacketsProcessed;
        }

        public int mousePacketsProcessed() {
            return mousePacketsProcessed;
        }

        public int walkPacketsReplaced() {
            return walkPacketsReplaced;
        }

        public int mousePacketsReplaced() {
            return mousePacketsReplaced;
        }

        public int itemClickPacketsReplaced() {
            return itemClickPacketsReplaced;
        }

        public int fifoPacketsDropped() {
            return fifoPacketsDropped;
        }
    }

    public static final class OutboundFlushStats {
        private final int flushedMessages;
        private final int flushedBytes;

        public OutboundFlushStats(int flushedMessages, int flushedBytes) {
            this.flushedMessages = flushedMessages;
            this.flushedBytes = flushedBytes;
        }

        public int flushedMessages() {
            return flushedMessages;
        }

        public int flushedBytes() {
            return flushedBytes;
        }

        public static OutboundFlushStats empty() {
            return new OutboundFlushStats(0, 0);
        }
    }

    @Override
    public void destruct() {
        setSynchronizationReady(false);
        net.dodian.uber.game.ui.AccountServices.close(this);
        net.dodian.uber.game.economy.PriceCheckerService.close(this);
        PlayerDeferredLifecycleService.cancelAll(this);
        net.dodian.uber.game.engine.systems.interaction.ui.PlayerSocialApproachService.cancel(this);
        PlayerScopedCoroutineService.cancelForPlayer(this, "Player destruct");
        net.dodian.uber.game.engine.systems.action.VerticalTransitionService.clear(this);
        releaseQueuedInboundPackets();
        releaseQueuedOutboundPackets();
        clearInboundReadyFlag();
        cancelFarmDebugTask();

        // Transport-specific shutdown
        if (channel != null) {
            channel.close();
        }
        logger.debug("Thread removed from Server for player={}", getPlayerName());
        isLoggingOut = false;

        if (saveNeeded && !TradeDuelSessionService.isTradeSettled(this)) {
            GameEventBus.post(new PlayerLogoutEvent(this, PlayerSaveReason.DISCONNECT));
            saveStats(PlayerSaveReason.DISCONNECT, true, true);
        }

        // Release references
        
        channel = null;
        // Stream cipher cleanup is no longer needed with Netty-owned I/O.
       // inStreamDecryption = null;
       // outStreamDecryption = null;


        super.destruct();
    }


    public io.netty.channel.Channel getChannel() {
        return this.channel;
    }

    @Override
    public boolean isSynchronizationReady() {
        return super.isSynchronizationReady() && channel != null && channel.isActive();
    }

    public boolean queueInboundPacket(net.dodian.uber.game.netty.game.GamePacket packet) {
        if (packet == null || disconnected) {
            return false;
        }
        InboundPacketMailbox.EnqueueResult result = inboundPacketMailbox.enqueue(packet);
        if (!result.accepted()) {
            pauseInboundReads();
            return false;
        }
        if (inboundPacketMailbox.pendingCount() >= net.dodian.uber.game.netty.NetworkConstants.INBOUND_READ_PAUSE_THRESHOLD) {
            pauseInboundReads();
        }
        markInboundReadyIfNeeded();
        return true;
    }

    /**
     * rsprot-style backpressure: rather than dropping packets or disconnecting when the
     * mailbox backs up, stop reading from the socket so TCP throttles the client. Reading
     * resumes from the game thread once the backlog drains below the resume threshold.
     */
    private void pauseInboundReads() {
        io.netty.channel.Channel currentChannel = channel;
        if (currentChannel != null && currentChannel.config().isAutoRead()) {
            currentChannel.config().setAutoRead(false);
        }
    }

    private void resumeInboundReadsIfDrained() {
        io.netty.channel.Channel currentChannel = channel;
        if (currentChannel == null || disconnected || currentChannel.config().isAutoRead()) {
            return;
        }
        if (inboundPacketMailbox.pendingCount() <= net.dodian.uber.game.netty.NetworkConstants.INBOUND_READ_RESUME_THRESHOLD) {
            currentChannel.config().setAutoRead(true);
        }
    }

    public InboundProcessResult processQueuedPackets(int maxPacketsPerTick) {
        net.dodian.uber.game.engine.loop.GameThreadContext.validateGameThread("inbound.packet-dispatch");
        if (maxPacketsPerTick <= 0 || disconnected) {
            InboundPacketMailbox.MailboxCounters counters = inboundPacketMailbox.snapshotAndResetCounters();
            return new InboundProcessResult(0, 0, 0, counters.walkReplaced(), counters.mouseReplaced(),
                    counters.itemClickReplaced(), counters.fifoDropped());
        }

        int processedCount = 0;
        int walkProcessed = 0;
        int mouseProcessed = 0;
        for (int processed = 0; processed < maxPacketsPerTick; processed++) {
            InboundPacketMailbox.PollResult next = inboundPacketMailbox.pollNext();
            if (next == null) {
                break;
            }
            net.dodian.uber.game.netty.game.GamePacket packet = next.packet();
            currentInboundPacketSequence = next.sequence();
            processedCount++;
            if (next.family() == InboundPacketMailbox.Family.WALK) {
                walkProcessed++;
            } else if (next.family() == InboundPacketMailbox.Family.MOUSE) {
                mouseProcessed++;
            }

            try {
                dispatchQueuedPacket(packet);
            } catch (Exception ex) {
                // A content bug in one packet listener must not kill the whole session —
                // the packet is dropped and logged, and the player keeps playing. Anything
                // else turns every content exception (a bad wield handler, a broken skill
                // action) into a forced disconnect and, worse, a zombie session.
                PacketRejectTelemetry.record(packet.opcode(), PacketRejectReason.LISTENER_EXCEPTION);
                PacketErrorTelemetry.recordListenerException(packet.opcode(), getPlayerName(), getSlot(), packet.size(), ex);
                logger.error(
                        "Inbound listener exception player={} slot={} opcode={} size={} recent={}",
                        getPlayerName(),
                        getSlot(),
                        packet.opcode(),
                        packet.size(),
                        describeRecentInboundPackets(),
                        ex
                );
                net.dodian.uber.game.engine.metrics.OperationalTelemetry.incrementCounter("packet.listener_exception", 1L);
                println_debug("Error processing opcode " + packet.opcode() + " for " + getPlayerName() + ": " + ex.getMessage());
            } finally {
                currentInboundPacketSequence = -1L;
                if (packet.payload() != null && packet.payload().refCnt() > 0) {
                    packet.payload().release();
                }
            }
        }
        resumeInboundReadsIfDrained();
        InboundPacketMailbox.MailboxCounters counters = inboundPacketMailbox.snapshotAndResetCounters();
        return new InboundProcessResult(
                processedCount,
                walkProcessed,
                mouseProcessed,
                counters.walkReplaced(),
                counters.mouseReplaced(),
                counters.itemClickReplaced(),
                counters.fifoDropped()
        );
    }

    private void dispatchQueuedPacket(net.dodian.uber.game.netty.game.GamePacket packet) throws Exception {
        recordInboundPacket(packet);
        if (debugPackets) {
            String hex = io.netty.buffer.ByteBufUtil.hexDump(packet.payload());
            logger.info("[Packet Debug] IN for {}: opcode={} size={} payload={}", getPlayerName(), packet.opcode(), packet.size(), hex);
        }
        net.dodian.uber.game.netty.listener.PacketListener listener =
                net.dodian.uber.game.netty.listener.PacketListenerManager.get(packet.opcode());
        if (listener != null) {
            boolean sample = net.dodian.uber.game.engine.metrics.InboundOpcodeProfiler.shouldSample();
            if (logger.isDebugEnabled() && isNpcTraceOpcode(packet.opcode())) {
                logger.debug(
                        "Inbound npc-trace opcode={} size={} preview={} recent={} player={}",
                        packet.opcode(),
                        packet.size(),
                        previewInboundPayload(packet, 4),
                        describeRecentInboundPackets(),
                        getPlayerName()
                );
            }
            if (sample) {
                long startNs = System.nanoTime();
                listener.handle(this, packet);
                long elapsedNs = System.nanoTime() - startNs;
                net.dodian.uber.game.engine.metrics.InboundOpcodeProfiler.record(this, packet, listener, elapsedNs);
            } else {
                listener.handle(this, packet);
            }
        } else {
            logger.warn(
                    "Unhandled inbound opcode={} size={} player={}",
                    packet.opcode(),
                    packet.size(),
                    getPlayerName()
            );
        }
    }

    /**
     * Transfers approach ownership from a client's predictive walk packet to the
     * authoritative interaction router without deleting newer movement input.
     */
    public void claimServerRoutedInteractionMovement() {
        if (currentInboundPacketSequence >= 0L) {
            inboundPacketMailbox.discardWalkAtOrBefore(currentInboundPacketSequence);
        }
        resetWalkingQueue();
    }

    private boolean isNpcTraceOpcode(int opcode) {
        return opcode == 155 || opcode == 17 || opcode == 21 || opcode == 18 || opcode == 26 || opcode == 72;
    }

    private void recordInboundPacket(net.dodian.uber.game.netty.game.GamePacket packet) {
        int slot = recentInboundWriteIndex;
        recentInboundOpcodes[slot] = packet.opcode();
        recentInboundSizes[slot] = packet.size();
        recentInboundCycles[slot] = PlayerRegistry.cycle;
        recentInboundWriteIndex = (recentInboundWriteIndex + 1) % RECENT_INBOUND_TRACE_SIZE;
        if (recentInboundCount < RECENT_INBOUND_TRACE_SIZE) {
            recentInboundCount++;
        }
    }

    public String describeRecentInboundPackets() {
        if (recentInboundCount <= 0) {
            return "[]";
        }
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < recentInboundCount; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            int index = (recentInboundWriteIndex - recentInboundCount + i + RECENT_INBOUND_TRACE_SIZE) % RECENT_INBOUND_TRACE_SIZE;
            builder.append(recentInboundCycles[index])
                    .append(':')
                    .append(recentInboundOpcodes[index])
                    .append('/')
                    .append(recentInboundSizes[index]);
        }
        return builder.append(']').toString();
    }

    public String previewInboundPayload(net.dodian.uber.game.netty.game.GamePacket packet, int maxBytes) {
        if (packet == null || packet.payload() == null || packet.size() <= 0 || maxBytes <= 0) {
            return "[]";
        }
        io.netty.buffer.ByteBuf payload = packet.payload();
        int start = payload.readerIndex();
        int count = Math.min(maxBytes, payload.readableBytes());
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                builder.append(' ');
            }
            int value = payload.getUnsignedByte(start + i);
            if (value < 0x10) {
                builder.append('0');
            }
            builder.append(Integer.toHexString(value).toUpperCase());
        }
        if (payload.readableBytes() > count) {
            builder.append(" ...");
        }
        return builder.append(']').toString();
    }

    private void releaseQueuedInboundPackets() {
        inboundPacketMailbox.clear(this::releaseInboundPacket);
    }

    private void releaseQueuedOutboundPackets() {
        outboundSessionQueue.releaseAll();
    }

    public int getPendingInboundPacketCount() {
        return inboundPacketMailbox.pendingCount();
    }

    public void clearInboundReadyFlag() {
        inboundReadyQueued.set(false);
    }

    public void markInboundReadyIfNeeded() {
        if (inboundReadyQueued.compareAndSet(false, true)) {
            EntityProcessor.enqueueInboundReady(this);
        }
    }

    public void resetTimeOutCounter() {
        lastInboundKeepAliveAtMillis = System.currentTimeMillis();
        timeOutCounter.set(0);
    }

    public void incrementTimeOutCounter() {
        timeOutCounter.incrementAndGet();
    }

    public void noteIdlePlayerSyncSent() {
        lastIdlePlayerSyncSentAtMillis = System.currentTimeMillis();
    }

    public long getLastInboundKeepAliveAtMillis() {
        return lastInboundKeepAliveAtMillis;
    }

    public long getLastIdlePlayerSyncSentAtMillis() {
        return lastIdlePlayerSyncSentAtMillis;
    }

    public String getLastDisconnectReason() {
        return lastDisconnectReason;
    }

    public void noteDisconnectReason(String reason) {
        if (reason != null && !reason.isEmpty()) {
            lastDisconnectReason = reason;
        }
    }

    public String connectionHealthSummary() {
        long now = System.currentTimeMillis();
        long inboundAge = lastInboundKeepAliveAtMillis <= 0L ? -1L : Math.max(0L, now - lastInboundKeepAliveAtMillis);
        long idleSyncAge = lastIdlePlayerSyncSentAtMillis <= 0L ? -1L : Math.max(0L, now - lastIdlePlayerSyncSentAtMillis);
        return "reason=" + lastDisconnectReason
                + " timeoutCounter=" + timeOutCounter.get()
                + " idleSyncAgeMs=" + idleSyncAge
                + " inboundKeepAliveAgeMs=" + inboundAge;
    }

    private boolean shouldQueueOutbound() {
        return isActive && loaded && !disconnected;
    }

    private void releaseInboundPacket(net.dodian.uber.game.netty.game.GamePacket packet) {
        if (packet != null && packet.payload() != null && packet.payload().refCnt() > 0) {
            packet.payload().release();
        }
    }

    public void send(net.dodian.uber.game.netty.codec.ByteMessage message) {
        if (disconnected || channel == null || !channel.isActive()) {
            message.release();
            return;
        }
        if (debugPackets) {
            String hex = io.netty.buffer.ByteBufUtil.hexDump(message.content());
            logger.info("[Packet Debug] OUT for {}: opcode={} type={} payload={}", getPlayerName(), message.getOpcode(), message.getType(), hex);
        }
        if (shouldQueueOutbound()) {
            outboundSessionQueue.enqueue(message);
            return;
        }
        channel.writeAndFlush(message);
    }

    /**
     * Queues a stateful synchronization packet and reports whether the client
     * can advance its local-view state. Unlike ordinary best-effort packets,
     * callers must disconnect when this returns false because later deltas
     * cannot repair a missing player/NPC synchronization packet.
     */
    public boolean sendRequiredSynchronization(net.dodian.uber.game.netty.codec.ByteMessage message) {
        if (disconnected || channel == null || !channel.isActive()) {
            message.releaseAll();
            return false;
        }
        if (debugPackets) {
            String hex = io.netty.buffer.ByteBufUtil.hexDump(message.content());
            logger.info("[Packet Debug] OUT (Sync) for {}: opcode={} type={} payload={}", getPlayerName(), message.getOpcode(), message.getType(), hex);
        }
        if (shouldQueueOutbound()) {
            return outboundSessionQueue.enqueue(message);
        }
        channel.writeAndFlush(message);
        return true;
    }

    public OutboundFlushStats flushOutbound() {
        if (disconnected || channel == null || !channel.isActive()) {
            return OutboundFlushStats.empty();
        }
        if (outboundSessionQueue.isEmpty()) {
            return OutboundFlushStats.empty();
        }
        OutboundSessionQueue.DrainResult drain = outboundSessionQueue.drainTo(channel);
        if (drain.messageCount() <= 0) {
            return OutboundFlushStats.empty();
        }
        channel.flush();
        return new OutboundFlushStats(drain.messageCount(), drain.byteCount());
    }

    public int getPlayerUpdateCapacity() {
        return lastPlayerUpdateCapacity;
    }

    public int getNpcUpdateCapacity() {
        return lastNpcUpdateCapacity;
    }

    public void updatePlayerUpdateCapacity(int size) {
        if (size > lastPlayerUpdateCapacity) {
            lastPlayerUpdateCapacity = Math.min(size, 65536);
        }
    }

    public void updateNpcUpdateCapacity(int size) {
        if (size > lastNpcUpdateCapacity) {
            lastNpcUpdateCapacity = Math.min(size, 65536);
        }
    }

    public void send(OutgoingPacket packet) {
        if (this.disconnected || this.channel == null || !this.channel.isActive()) {
            return; // Client is shutting down or not ready; skip sending packet
        }
        int previousInterfaceId = activeInterfaceId;
        Integer openedInterfaceId = null;
        String openedVia = null;
        String closedVia = null;

        if (packet instanceof ShowInterface) {
            openedInterfaceId = ((ShowInterface) packet).interfaceId();
            openedVia = "ShowInterface";
        } else if (packet instanceof InventoryInterface) {
            openedInterfaceId = ((InventoryInterface) packet).interfaceId();
            openedVia = "InventoryInterface";
        } else if (packet instanceof SendChatboxInterface) {
            openedInterfaceId = ((SendChatboxInterface) packet).frame();
            openedVia = "Frame164";
        } else if (packet instanceof SetTabInterface) {
            openedInterfaceId = ((SetTabInterface) packet).mainFrame();
            openedVia = "SetTabInterface";
        } else if (packet instanceof RemoveInterfaces) {
            closedVia = "RemoveInterfaces";
            activeInterfaceId = -1;
        }

        if (viewingAccountServices &&
                ((openedInterfaceId != null && openedInterfaceId != 36700) || closedVia != null)) {
            net.dodian.uber.game.ui.AccountServices.close(this);
        }

        if (openedInterfaceId != null) {
            activeInterfaceId = openedInterfaceId;
        }

        if (closedVia != null && previousInterfaceId != -1) {
            pendingInterfaceOpenContext = null;
            ConsoleAuditLog.interfaceClose(this, previousInterfaceId, closedVia);
        }
        if (openedInterfaceId != null && openedInterfaceId != previousInterfaceId) {
            String openContext = pendingInterfaceOpenContext;
            pendingInterfaceOpenContext = null;
            ConsoleAuditLog.interfaceOpen(this, openedInterfaceId, openedVia, openContext);
        }

        packet.send(this);

    }

    @Override
    public void run() {
        // Login is now handled by Netty's LoginProcessorHandler
        // This method is kept for backward compatibility but does nothing for Netty connections

    }

    public void setSidebarInterface(int menuId, int form) {
        interfaceManager.setSidebar(menuId, form);
    }

    public void updateAutoRetaliate() {
        varbit(172, autoRetaliate ? 1 : 0);
        sendString(autoRetaliate ? "Auto Retaliate (On)" : "Auto Retaliate (Off)", 3983);
    }


    public void logout() {
        send(new SendMessage("Please wait... logging out may take time"));
        send(new SendString("     Please wait...", 2458));

        if (isLoggingOut) {
            return;
        }
        if (GameCycleClock.currentCycle() - lastDropCycle < 1) {
            send(new SendMessage("You cannot log out so soon after dropping an item."));
            return;
        }
        isLoggingOut = true;
        net.dodian.uber.game.persistence.account.LogoutReLoginGuard.recordLogout(dbId);
        ContentActions.cancel(this, PlayerActionCancelReason.LOGOUT, false, false, false, true);
        if (!saveNeeded || !validClient || UsingAgility) {
            if (UsingAgility) {
                xLog = true; // Existing logic for agility delay
                PlayerDeferredLifecycleService.scheduleXLogExpiry(this, getWalkBlockUntilCycle());
            }
            isLoggingOut = false;
            return;
        }

        PlayerDeferredLifecycleService.cancelAll(this);
        // Save player data before disconnecting
        GameEventBus.post(new PlayerLogoutEvent(this, PlayerSaveReason.LOGOUT));
        saveStats(PlayerSaveReason.LOGOUT, true, true);

        // Send the logout packet
        send(new Logout());
    }

    public void saveStats(PlayerSaveReason reason, boolean logout, boolean updateProgress) {
        if (loginDelay > 0 && !logout) {
            println("Incomplete login, aborting save");
            return;
        }
        if (!loadingDone || !validLogin) {
            return;
        }
        if (getPlayerName() == null || getPlayerName().equals("null") || dbId < 1) {
            println_debug("Could not save due to null! " + (dbId < 1 ? "db is less than 1!" : "Dbid = " + dbId));
            return;
        }
        if (getPlayerName().indexOf("'") > 0 || playerPass.indexOf("`") > 0) {
            println_debug("Invalid player name");
            return;
        }
        if (logout) {
            saveNeeded = false;
            /* Remove player from list! */
            PlayerRegistry.playersOnline.remove(longName);
            PlayerRegistry.allOnline.remove(longName);
            println_debug(getPlayerName() + " has logged out correctly!");
        /*for (Player p : PlayerRegistry.players) {
            if (p != null && !p.disconnected && p.dbId > 0) {
                if (p.getDamage().containsKey(getSlot())) {
                    p.getDamage().put(getSlot(), 0);
                }
            }
        }*/ //TODO: Fix this pvp shiet
            if (getGameWorldId() < 2) {
                long elapsed = System.currentTimeMillis() - start;
                int minutes = (int) (elapsed / 60000);
                Server.login.sendSession(dbId, officialClient ? 1 : 1337, minutes, connectedFrom, start, System.currentTimeMillis());
            }
            for (Client c : PlayerRegistry.playersOnline.values()) {
                if (c.hasFriend(longName)) {
                    c.refreshFriends();
                }
            }
            TradeDuelSessionService.closeOnLogout(this);
            if (getDuel_with() > 0 && validClient(getDuel_with()) && getInDuel() && getDuelFight()) {
                Client p = getClient(getDuel_with());
                p.setDuelWin(true);
                net.dodian.uber.game.social.exchange.DuelingService.victory(p);
            }
        }
        if ((getServerEnv().equals("prod") && getGameWorldId() < 2) || getServerEnv().equals("dev") || getPlayerName().toLowerCase().startsWith("pro noob")) {
            try {
                AccountPersistenceService.requestSave(this, reason, updateProgress, logout);
            } catch (Exception e) {
                println_debug("Save Exception: " + getSlot() + ", " + getPlayerName() + ", msg: " + e);
            }
        }
    }

    public void saveStats(boolean logout, boolean updateProgress) {
        PlayerSaveReason reason;
        if (logout) {
            reason = PlayerSaveReason.LOGOUT;
        } else if (updateProgress) {
            reason = PlayerSaveReason.PERIODIC_PROGRESS;
        } else {
            reason = PlayerSaveReason.PERIODIC;
        }
        saveStats(reason, logout, updateProgress);
    }
    public void saveStats(boolean logout) {
        saveStats(logout, false);
    }

    public int getInvAmt(int itemID) {
        int amt = 0;
        for (int slot = 0; slot < playerItems.length; slot++) {
            if (playerItems[slot] == (itemID + 1)) {
                amt += playerItemsN[slot];
            }
        }
        return amt;
    }

    public int getBankAmt(int itemID) {
        int size = bankSize();
        for (int i = 0; i < size; i++) {
            if (bankItems[i] == itemID + 1) {
                return bankItemsN[i];
            }
        }
        return 0;
    }
    public int getBankSlot(int itemID) {
        int size = bankSize();
        for (int i = 0; i < size; i++) {
            if (bankItems[i] == itemID + 1) {
                return i;
            }
        }
        return -1;
    }

    private static final int INVENTORY_DIRTY_SLOT_THRESHOLD = 10;
    private int[] lastSentInventoryIds;
    private int[] lastSentInventoryAmounts;

    public void resetItems(int WriteFrame) {
        if (WriteFrame == 3214) {
            if (net.dodian.uber.game.social.exchange.ExchangeRuntime.session(this) != null) {
                send(new ResetItems(3214));
                return;
            }
            sendInventoryDelta();
            return;
        }
        send(new ResetItems(WriteFrame));
    }

    /**
     * Sends only the inventory slots that changed since the last send instead of a full 28-slot
     * resend, using the existing single-slot wire format (TarnishItemContainerEncoder.slot /
     * opcode 34) already relied on elsewhere. Falls back to a full resend on the first call (no
     * prior snapshot to diff against) or when more slots changed than a single resend would cost.
     */
    private void sendInventoryDelta() {
        int size = playerItems.length;
        if (lastSentInventoryIds == null || lastSentInventoryIds.length != size) {
            send(new ResetItems(3214));
            lastSentInventoryIds = new int[size];
            lastSentInventoryAmounts = new int[size];
            for (int i = 0; i < size; i++) {
                lastSentInventoryIds[i] = playerItems[i] - 1;
                lastSentInventoryAmounts[i] = playerItemsN[i];
            }
            return;
        }
        int changedCount = 0;
        for (int i = 0; i < size; i++) {
            if (playerItems[i] - 1 != lastSentInventoryIds[i] || playerItemsN[i] != lastSentInventoryAmounts[i]) {
                changedCount++;
            }
        }
        if (changedCount == 0) {
            return;
        }
        if (changedCount > INVENTORY_DIRTY_SLOT_THRESHOLD) {
            send(new ResetItems(3214));
        } else {
            for (int i = 0; i < size; i++) {
                int id = playerItems[i] - 1;
                int amount = playerItemsN[i];
                if (id != lastSentInventoryIds[i] || amount != lastSentInventoryAmounts[i]) {
                    send(TarnishItemContainerEncoder.slot(3214, i, id, amount));
                }
            }
        }
        for (int i = 0; i < size; i++) {
            lastSentInventoryIds[i] = playerItems[i] - 1;
            lastSentInventoryAmounts[i] = playerItemsN[i];
        }
    }

    public void sendInventory(int interfaceId, ArrayList<GameItem> inv) {
        send(new SendInventory(interfaceId, inv));
    }



    public void resetOTItems(int WriteFrame) {
        Client other = getClient(getTrade_reqId());
        if (!validClient(getTrade_reqId())) {
            return;
        }
        send(new TradeItemsUpdate(WriteFrame, net.dodian.uber.game.social.exchange.ExchangeRuntime.offers(other)));
    }

    public void resetTItems(int WriteFrame) {
        send(new TradeItemsUpdate(WriteFrame, net.dodian.uber.game.social.exchange.ExchangeRuntime.offers(this)));
    }

    public void resetShop(int shopId) {
        send(new ResetShop(shopId));
    }

    public void resetBank() {
        send(new ResetBank());
    }

    public void sendBank(ArrayList<Integer> id, ArrayList<Integer> amt) {
        send(new SendBankItems(id, amt));
    }

    public void openBankStyleView(ArrayList<Integer> id, ArrayList<Integer> amt, String title) {
        PlayerBankService.openBankStyleView(this, id, amt, title);
    }

    public void clearBankStyleView() {
        PlayerBankService.clearBankStyleView(this);
    }

    public void sendBank(int interfaceId, ArrayList<GameItem> bank) {
        send(new ViewOtherPlayerBank(interfaceId, bank));
    }

    public void moveItems(int from, int to, int moveWindow) {
        if (moveWindow == 3214 || moveWindow == 5064) {
            if (net.dodian.uber.game.social.exchange.ExchangeRuntime.hasReservationAt(this, from)
                    || net.dodian.uber.game.social.exchange.ExchangeRuntime.hasReservationAt(this, to)) {
                send(new SendMessage("You cannot move an item while it is offered."));
                return;
            }
            int tempI = playerItems[to];
            int tempN = playerItemsN[to];
            playerItems[to] = playerItems[from];
            playerItemsN[to] = playerItemsN[from];
            playerItems[from] = tempI;
            playerItemsN[from] = tempN;
            markSaveDirty(PlayerSaveSegment.INVENTORY.getMask());
            resetItems(moveWindow);
        }
        if (PlayerBankService.moveBankItems(this, from, to, moveWindow, 0)) {
        }
    }

    public void moveBankItems(int from, int to, int moveWindow, int mode) {
        PlayerBankService.moveBankItems(this, from, to, moveWindow, mode);
    }

    public boolean createBankPlaceholder(int itemId) {
        return PlayerBankService.createPlaceholder(this, itemId);
    }

    public int freeBankSlots() {
        int freeS = 0;
        int size = bankSize();
        for (int i = 0; i < size; i++) {
            if (bankItems[i] <= 0) {
                freeS++;
            }
        }
        return freeS;
    }

    public int freeSlots() {
        int freeSlot = 0;

        for (int playerItem : playerItems) {
            if (playerItem <= 0) {
                freeSlot++;
            }
        }
        return freeSlot;
    }

    public void pickUpItem(int x, int y) {
        boolean specialItems = (x == 2611 && y == 3096) || (x == 2612 && y == 3096) || (x == 2563 && y == 9511) || (x == 2564 && y == 9511);
        if (specialItems && playerRights < 2) {
            dropAllItems();
            attemptGround = null;
            getContentRuntimeState().setPickupWanted(false);
            PlayerDeferredLifecycleService.cancelGroundPickupArrivalWatch(this);
            return;
        }
        GroundItem target = attemptGround;
        if (target == null) {
            PlayerDeferredLifecycleService.cancelGroundPickupArrivalWatch(this);
            return;
        }

        if (target.x != x || target.y != y || target.z != getPosition().getZ()) {
            attemptGround = null;
            getContentRuntimeState().setPickupWanted(false);
            PlayerDeferredLifecycleService.cancelGroundPickupArrivalWatch(this);
            return;
        }

        if (!hasSpace() && Server.itemManager.isStackable(target.id) && !playerHasItem(target.id)) {
            send(new SendMessage("Your inventory is full!"));
            attemptGround = null;
            getContentRuntimeState().setPickupWanted(false);
            PlayerDeferredLifecycleService.cancelGroundPickupArrivalWatch(this);
            return;
        }

        if (premiumItem(target.id) && !premium) {
            send(new SendMessage("You must be a premium member to use this item"));
            attemptGround = null;
            getContentRuntimeState().setPickupWanted(false);
            PlayerDeferredLifecycleService.cancelGroundPickupArrivalWatch(this);
            return;
        }

        if (!Ground.tryClaimPickup(this, target)) {
            attemptGround = null;
            getContentRuntimeState().setPickupWanted(false);
            PlayerDeferredLifecycleService.cancelGroundPickupArrivalWatch(this);
            return;
        }

        boolean added = addItem(target.id, target.amount);
        if (added) {
            GameEventBus.post(new ItemPickupEvent(this, target, getPosition().copy()));
            Ground.deleteItem(target);
            ItemLog.playerPickup(this, target.npc ? target.npcId : target.playerId, target.id, target.amount, getPosition().copy(), target.npc);
            checkItemUpdate();
        } else {
            Ground.releaseClaim(target);
        }
        attemptGround = null;
        getContentRuntimeState().setPickupWanted(false);
        PlayerDeferredLifecycleService.cancelGroundPickupArrivalWatch(this);
    }

    public void openUpBank() {
        PlayerBankService.openUpBank(this);
    }

    public void openUpBankRouted() {
        WanneBank = 0;
        WanneShop = -1;
        openUpBank();
    }

    public void openUpShop(int ShopID) {
        String blockMessage = PlayerInteractionGuardService.blockingInteractionMessage(this);
        if (blockMessage != null && !PlayerInteractionGuardService.canOpenShop(this)) {
            send(new SendMessage(blockMessage));
            return;
        }
        if (!net.dodian.uber.game.engine.config.FeatureStateService.shopping.get()) {
            send(new SendMessage("Shopping have been disabled!"));
            return;
        }
        if (ShopID < 0 || ShopID >= ShopManager.MaxShops || ShopCatalog.find(ShopID) == null) {
            send(new SendMessage("This shop is currently unavailable."));
            return;
        }
        String accessDeniedMessage = ShopRulesService.accessDeniedMessage(this, ShopID);
        if (accessDeniedMessage != null) {
            send(new SendMessage(accessDeniedMessage));
            return;
        }
        MyShopID = ShopID;
        checkItemUpdate();
        send(new SendString(ShopManager.ShopName[ShopID], 3901));
        pendingInterfaceOpenContext = "shop " + ShopID + ": \"" + ShopManager.ShopName[ShopID] + "\"";
        send(new InventoryInterface(3824, 3822));
    }

    public void openUpShopRouted(int shopId) {
        WanneShop = 0;
        openUpShop(shopId);
    }

    public void startNpcDialogue(int dialogueId, int npcId) {
        String blockMessage = PlayerInteractionGuardService.blockingInteractionMessage(this);
        if (blockMessage != null && !PlayerInteractionGuardService.canStartDialogue(this)) {
            send(new SendMessage(blockMessage));
            return;
        }
        NpcWanneTalk = 0;
        DialogueService.startDialogueId(this, dialogueId, npcId);
    }

    public void setTeleportStage(int stage) {
        // Compatibility no-op retained for untouched callers during teleport cutover.
    }

    public int getTeleportHeight() {
        return tH;
    }

    public void checkItemUpdate() { //Checking bank etc..
        PlayerBankService.checkItemUpdate(this);
    }

    public void applyBankSearch(String query) {
        PlayerBankService.applyBankSearch(this, query);
    }

    public void ensureBankTabState() {
        PlayerBankService.ensureBankTabState(this);
    }

    public void sendBankContainers() {
        PlayerBankService.sendBankContainers(this);
    }

    public void sendBankStyleViewContainers() {
        PlayerBankService.sendBankStyleViewContainers(this);
    }

    public int resolveBankSlot(int interfaceId, int containerSlot) {
        return PlayerBankService.resolveBankSlot(this, interfaceId, containerSlot);
    }

    public int resolveBankItemId(int interfaceId, int containerSlot, int fallbackItemId) {
        return PlayerBankService.resolveBankItemId(this, interfaceId, containerSlot, fallbackItemId);
    }

    public int resolveBankSlotByItemId(int itemId) {
        return PlayerBankService.resolveBankSlotByItemId(this, itemId);
    }

    public void assignBankSlotToTab(int bankSlot, int tab) {
        PlayerBankService.assignBankSlotToTab(this, bankSlot, tab);
    }

    public void selectBankTab(int tab) {
        PlayerBankService.selectBankTab(this, tab);
    }

    public void collapseBankTab(int tab) {
        PlayerBankService.collapseBankTab(this, tab);
    }

    public void clearBankSearch() {
        PlayerBankService.clearBankSearch(this);
    }

    public boolean hasBankTabItems(int tab) {
        return PlayerBankService.hasBankTabItems(this, tab);
    }


    public void refreshBankHeader() {
        PlayerBankService.refreshBankHeader(this);
    }

    public boolean addItem(int item, int amount) {
        boolean committed = EconomyTransaction.addToInventory(this, item, amount);
        if (!committed && item >= 0 && amount > 0) {
            send(new SendMessage("Not enough space in your inventory."));
        }
        return committed;
    }

    public void addItemSlot(int item, int amount, int slot) {
        EconomyTransaction.replaceInventorySlot(this, slot, item, amount);
    }

    public void dropItem(int id, int slot) {
        if (isBusy()) {
            send(new SendMessage("You are currently busy!"));
            return;
        }
        if (!net.dodian.uber.game.engine.config.FeatureStateService.dropping.get()) {
            send(new SendMessage("Dropping has been disabled.  Please try again later"));
            return;
        }
        send(new RemoveInterfaces()); //Need this to stop interface abuse
        int amount = 0;
        if (playerItems[slot] == (id + 1) && playerItemsN[slot] > 0) {
            amount = playerItemsN[slot];
        }
        if (amount < 1 || id < 0) {
            return;
        }
        //send(new Sound(376));
        deleteItem(id, slot, amount);
        checkItemUpdate();
        Ground.addFloorItem(this, id, amount);
        ItemLog.playerDrop(this, id, amount, getPosition().copy(), "Inventory Drop", slot, 3214);
    }

    public void deleteItem(int id, int amount) {
        deleteItem(id, getItemSlot(id), amount);
    }

    public void deleteItem(int id, int slot, int amount) {
        EconomyTransaction.removeFromInventory(this, id, slot, amount);
    }

    public void deleteItemBank(int id, int slot, int amount) {
        if (slot > -1 && slot < bankItems.length) {
            if ((bankItems[slot] - 1) == id) {
                if (bankItemsN[slot] > amount) {
                    bankItemsN[slot] -= amount;
                } else {
                    bankItemsN[slot] = 0;
                    bankItems[slot] = 0;
                }
                markSaveDirty(PlayerSaveSegment.BANK.getMask());
                checkItemUpdate();
            }
        }
    }

    public void deleteItemBank(int id, int amount) {
        deleteItemBank(id, GetBankItemSlot(id), amount);
    }

    public void setEquipment(int wearID, int amount, int targetSlot) {
        send(new SetEquipment(wearID, amount, targetSlot));
    }

    /**
     * The only live-state notification path for equipment.  The old code
     * updated the interface packet in a number of places but left the cached
     * player appearance untouched, so observers could keep seeing an older
     * weapon or armour model.  Call this after the equipment arrays have been
     * changed successfully.
     */
    public void equipmentChanged(int... changedSlots) {
        applyEquipmentSideEffects(changedSlots, true);
    }

    /**
     * Shared by {@link #equipmentChanged} and {@link #refreshEquipmentState}.
     * {@code sendIndividualSlotPackets} is false for the login/full-refresh path:
     * Tarnish's reference {@code Equipment.login()} fires the same weapon/bonus/
     * appearance side effects per slot but with its container's {@code refresh}
     * flag off, then sends exactly one full-container packet - individual
     * per-slot packets right before a full snapshot of the same widget are
     * immediately superseded and never visible to the client.
     */
    private void applyEquipmentSideEffects(int[] changedSlots, boolean sendIndividualSlotPackets) {
        boolean weaponChanged = false;
        boolean[] sent = new boolean[getEquipment().length];
        if (changedSlots != null) {
            for (int slot : changedSlots) {
                if (slot < 0 || slot >= getEquipment().length || sent[slot]) {
                    continue;
                }
                sent[slot] = true;
                if (sendIndividualSlotPackets) {
                    setEquipment(getEquipment()[slot], getEquipmentN()[slot], slot);
                }
                weaponChanged |= slot == Equipment.Slot.WEAPON.getId();
            }
        }
        if (weaponChanged) {
            CheckGear();
            net.dodian.uber.game.ui.combat.CombatStyleService.refreshWeaponStyleUi(this);
            net.dodian.uber.game.engine.systems.combat.CombatSpecialService.onWeaponEquip(this);
            requestWeaponAnims();
        }
        GetBonus(true);
        markAppearanceDirty();
        getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
    }

    /** Refreshes the full equipment container after account hydration/UI setup. */
    public void refreshEquipmentState() {
        int[] slots = new int[getEquipment().length];
        for (int slot = 0; slot < slots.length; slot++) {
            slots[slot] = slot;
        }
        applyEquipmentSideEffects(slots, false);
        send(new SendItemOnInterface(
                1688,
                java.util.Arrays.copyOf(getEquipment(), getEquipment().length),
                java.util.Arrays.copyOf(getEquipmentN(), getEquipmentN().length)));
    }

    /** Commits a staged equipment snapshot and publishes one visual update. */
    public void replaceEquipmentState(int[] ids, int[] amounts) {
        if (ids == null || amounts == null || ids.length != getEquipment().length || amounts.length != getEquipmentN().length) {
            throw new IllegalArgumentException("Invalid equipment snapshot");
        }
        java.util.ArrayList<Integer> changed = new java.util.ArrayList<>();
        for (int slot = 0; slot < ids.length; slot++) {
            if (getEquipment()[slot] != ids[slot] || getEquipmentN()[slot] != amounts[slot]) {
                changed.add(slot);
            }
        }
        if (changed.isEmpty()) {
            return;
        }
        System.arraycopy(ids, 0, getEquipment(), 0, ids.length);
        System.arraycopy(amounts, 0, getEquipmentN(), 0, amounts.length);
        markSaveDirty(PlayerSaveSegment.EQUIPMENT.getMask());
        equipmentChanged(changed.stream().mapToInt(Integer::intValue).toArray());
    }

    private void wearLog(String format, Object... args) {
        if (getGameWorldId() == 2) {
            logger.debug(format, args);
        }
    }

    public void wear(int wearID, int slot, int interFace) {
        net.dodian.uber.game.engine.systems.inventory.EquipmentService.wear(this, wearID, slot, interFace);
    }

    /** Legacy-only path for special item actions and two-handed/bow conflict handling. */
    public void performLegacyWear(int wearID, int slot, int interFace) {
                    wearLog("[WEAR:WEAR] wearID={} slot={} interface={}", wearID, slot, interFace);
        if (isBusy() || interFace != 3214) {
                            wearLog("[WEAR:WEAR] rejected busy={} interface={}", isBusy(), interFace);
            return;
        }
        if (net.dodian.uber.game.engine.systems.skills.RunecraftingPouchPacketService.empty(this, wearID)) { //Runecrafting Pouches
            return;
        }
        if (wearID == 5733) { //Potato
            wipeInv();
            return;
        }
        if (wearID == 6583 || wearID == 7927) {
            if (isWalkBlocked()) { //Not usable during a walkBlock!
                return;
            }
            send(new RemoveInterfaces());
            resetWalkingQueue();
            morphTab("Unmorph");
            morph = true;
            isNpc = true;
            setPlayerNpc(wearID == 6583 ? 2188 : 5538 + Misc.random(5));
            getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
            return;
        }
        if (wearID == 4155) { //Enchanted gem
            ItemDispatcher.tryHandle(this, 1, wearID, slot, interFace);
            return;
        }
        if (getDuelConfirmed() && !getDuelFight()) {
                            wearLog("[WEAR:WEAR] rejected pending duel confirmation");
            return;
        }
        if (!playerHasItem(wearID)) {
                            wearLog("[WEAR:WEAR] rejected item missing id={}", wearID);
            return;
        }
        int targetSlot = Server.itemManager.getSlot(wearID);
                    wearLog("[WEAR:WEAR] targetSlot={}", targetSlot);
        if (canUse(wearID)) {
                            wearLog("[WEAR:WEAR] rejected premium item id={}", wearID);
            send(new SendMessage("You must be a premium member to use this item"));
            return;
        }
        if (net.dodian.uber.game.social.exchange.DuelingService.isEquipmentRestricted(this, targetSlot)) {
                            wearLog("[WEAR:WEAR] rejected by duel equipment rule slot={}", targetSlot);
            send(new SendMessage("Current duel rules restrict this from being worn!"));
            return;
        }
        if ((playerItems[slot] - 1) == wearID) {
                            wearLog("[WEAR:WEAR] validating requirements id={} slot={}", wearID, targetSlot);
            if (!checkEquip(wearID, targetSlot, slot)) {
                                    wearLog("[WEAR:WEAR] rejected by equipment requirements id={}", wearID);
                return;
            }
            int wearAmount = playerItemsN[slot];
            if (wearAmount < 1) {
                return;
            }
            if (net.dodian.uber.game.social.exchange.ExchangeRuntime.hasReservationAt(this, slot)) {
                send(new SendMessage("You cannot equip an item while it is offered."));
                return;
            }
            if (wearID >= 0) {
                deleteItem(wearID, slot, wearAmount);
                if (getEquipment()[targetSlot] != wearID && getEquipment()[targetSlot] >= 0) {
                    addItem(getEquipment()[targetSlot], getEquipmentN()[targetSlot]);
                } else if (Server.itemManager.isStackable(wearID) && getEquipment()[targetSlot] == wearID) {
                    wearAmount = getEquipmentN()[targetSlot] + wearAmount;
                } else if (getEquipment()[targetSlot] >= 0) {
                    addItem(getEquipment()[targetSlot], getEquipmentN()[targetSlot]);
                }
                checkItemUpdate();
            }
            getEquipment()[targetSlot] = wearID;
            getEquipmentN()[targetSlot] = wearAmount;
            markSaveDirty(PlayerSaveSegment.EQUIPMENT.getMask());
            wearing = false;
            equipmentChanged(targetSlot);
            logger.info("[WEAR] player={} item={} ({}) equipSlot={} invSlot={}", getPlayerName(), wearID, getItemName(wearID), targetSlot, slot);
            ConsoleAuditLog.equipmentWear(this, wearID, wearAmount, slot, targetSlot,
                    null);
            wearLog("[WEAR:WEAR] equipped id={} targetSlot={} amount={}", wearID, targetSlot, wearAmount);
        } else {
            wearLog("[WEAR:WEAR] rejected slot/item mismatch playerItems[slot]={} expected={}", playerItems[slot] - 1, wearID);
        }
    }

    public boolean checkEquip(int id, int slot, int invSlot) {
                    wearLog("[WEAR:EQUIP] id={} slot={} invSlot={}", id, slot, invSlot);
        boolean maxCheck = getItemName(id).contains(("Max cape")) || getItemName(id).contains(("Max hood"));
        if (maxCheck && totalLevel() < Skills.maxTotalLevel()) {
            send(new SendMessage("You need a total level of " + Skills.maxTotalLevel() + " to equip the " + getItemName(id).toLowerCase() + "."));
            return false;
        }
        int CLAttack = GetCLAttack(id);
        int CLDefence = GetCLDefence(id);
        int CLStrength = GetCLStrength(id);
        int CLMagic = GetCLMagic(id);
        int CLRanged = GetCLRanged(id);
        boolean failCheck = false;
        String itemName = Server.itemManager.getName(id);
        if (CLAttack > Skills.getLevelForExperience(getExperience(Skill.ATTACK))) {
            send(new SendMessage("You need " + CLAttack + " Attack to equip " + itemName.toLowerCase() + "."));
            failCheck = true;
        }
        if (CLDefence > Skills.getLevelForExperience(getExperience(Skill.DEFENCE))) {
            send(new SendMessage("You need " + CLDefence + " Defence to equip " + itemName.toLowerCase() + "."));
            failCheck = true;
        }
        if (CLStrength > Skills.getLevelForExperience(getExperience(Skill.STRENGTH))) {
            send(new SendMessage("You need " + CLStrength + " Strength to equip " + itemName.toLowerCase() + "."));
            failCheck = true;
        }
        if (CLMagic > Skills.getLevelForExperience(getExperience(Skill.MAGIC))) {
            send(new SendMessage("You need " + CLMagic + " Magic to equip " + itemName.toLowerCase() + "."));
            failCheck = true;
        }
        if (CLRanged > Skills.getLevelForExperience(getExperience(Skill.RANGED))) {
            send(new SendMessage("You need " + CLRanged + " Ranged to equip " + itemName.toLowerCase() + "."));
            failCheck = true;
        }
        Integer agilityRequirement = net.dodian.uber.skills.agility.AgilityCombatService.equipmentLevelRequirement(id);
        if (agilityRequirement != null && Skills.getLevelForExperience(getExperience(Skill.AGILITY)) < agilityRequirement) {
            send(new SendMessage("You need " + agilityRequirement + " Agility to equip " + itemName.toLowerCase() + "."));
            failCheck = true;
        }
        if (Skills.getLevelForExperience(getExperience(PRAYER)) < 25 && id == 2952) {
            send(new SendMessage("You need 25 Prayer to equip " + itemName.toLowerCase() + "."));
            failCheck = true;
        }
        boolean isHood = Server.itemManager.getName(id).contains("hood");
        Skillcape skillcape = Skillcape.getSkillCape(isHood ? id - 2 : id);
        if (skillcape != null) {
            if (Skillcape.isTrimmed(id) && getExperience(skillcape.getSkill()) < 50000000) {
                send(new SendMessage("This cape requires 50 million " + skillcape.getSkill().getName() + " experience to wear."));
                failCheck = true;
            } else if (Skills.getLevelForExperience(getExperience(skillcape.getSkill())) < 99) {
                send(new SendMessage("This " + (isHood ? "hood" : "cape") + " requires level 99 " + skillcape.getSkill().getName() + " to wear."));
                failCheck = true;
            }
        }
        /* 2handed check! */
        int shield = getEquipment()[Equipment.Slot.SHIELD.getId()];
        int weapon = getEquipment()[Equipment.Slot.WEAPON.getId()];
        if (weapon < 1) weapon = -1; //Prevent adding a dwarf remain to inventory!
        boolean twoHanded = Server.itemManager.isTwoHanded(id) || Server.itemManager.isTwoHanded(weapon);
        if (twoHanded && !failCheck) {
            if (slot == Equipment.Slot.WEAPON.getId()) {
                if (shield > 0 && hasSpace()) {
                    remove(slot, true);
                    remove(Equipment.Slot.SHIELD.getId(), true);
                    if (invSlot == -1)
                        addItem(weapon, 1);
                    else
                        addItemSlot(weapon, 1, invSlot);
                    addItem(shield, 1);
                } else if (shield > 0) {
                    send(new SendMessage("Not enough space to equip this item!"));
                    failCheck = true;
                }
            } else if (slot == Equipment.Slot.SHIELD.getId()) {
                remove(Equipment.Slot.WEAPON.getId(), true);
                if (invSlot == -1)
                    addItem(weapon, 1);
                else
                    addItemSlot(weapon, 1, invSlot);
            }
            checkItemUpdate();
        }
        /* Bow check! */
        boolean check = (id == 4212 || id == 6724 || id == 4734 || id == 20997) || (weapon == 4212 || weapon == 6724 || weapon == 4734 || weapon == 20997);
        if (check && !failCheck) {
            if (slot == 5 && id != 3844 && id != 4224 && id != 1540) {
                if (shield > 0 && hasSpace()) {
                    remove(slot, true);
                    remove(Equipment.Slot.WEAPON.getId(), true);
                    if (invSlot == -1)
                        addItem(weapon, 1);
                    else
                        addItemSlot(weapon, 1, invSlot);
                    addItem(shield, 1);
                } else if (shield > 0) {
                    send(new SendMessage("Not enough space to equip this item!"));
                    failCheck = true;
                } else if (invSlot != -1) {
                    remove(Equipment.Slot.WEAPON.getId(), true);
                    addItemSlot(weapon, 1, invSlot);
                } else failCheck = true;
            }
            if (slot == 3 && invSlot != -1 && (id == 4212 || id == 6724 || id == 4734 || id == 20997)) {
                boolean shieldCheck = shield == 3844 || shield == 4224 || shield == 1540;
                if (!shieldCheck && shield > 0 && weapon > 0 && hasSpace()) {
                    remove(slot, true);
                    remove(Equipment.Slot.SHIELD.getId(), true);
                    addItemSlot(weapon, 1, invSlot);
                    addItem(shield, 1);
                } else if (shield > 0 && weapon > 0 && !hasSpace()) {
                    send(new SendMessage("Not enough space to equip this item!"));
                    failCheck = true;
                } else if (shield > 0 && !shieldCheck) {
                    remove(Equipment.Slot.SHIELD.getId(), true);
                    addItemSlot(shield, 1, invSlot);
                }
            }
            checkItemUpdate();
        }
        wearLog("[WEAR:EQUIP] id={} slot={} invSlot={} result={}", id, slot, invSlot, !failCheck);
        return !failCheck;
    }

    public boolean remove(int slot, boolean force) {
        if (getDuelConfirmed() && !force) {
            return false;
        }
        getEquipment()[slot] = -1;
        getEquipmentN()[slot] = 0;
        markSaveDirty(PlayerSaveSegment.EQUIPMENT.getMask());
        equipmentChanged(slot);
        return true;
    }

    public void deleteequiment(int wearID, int slot) {
        if (getEquipment()[slot] == wearID) {
            getEquipment()[slot] = -1;
            getEquipmentN()[slot] = 0;
            markSaveDirty(PlayerSaveSegment.EQUIPMENT.getMask());
            equipmentChanged(slot);
        }
    }

    public void setChatOptions(int publicChat, int privateChat, int tradeBlock) {
        send(new SetChatOptions(publicChat, privateChat, tradeBlock));
    }


    public void initialize() {
        new PlayerInitializer().initializePlayer(this);
    }


    /**
     * Shows or hides an interface
     * @param inter The interface ID to show/hide
     * @param show Whether to show (true) or hide (false) the interface
     */
    public void changeInterfaceStatus(int inter, boolean show) {
        send(new InterfaceStatus(inter, show));
    }

    public void setMenuItems(int[] items) {
        send(new SetMenuItems(items));
    }

    public void setMenuItems(int[] items, int[] amount) {
        send(new ShowMenuItems2(items, amount));
    }

    public static void publicyell(String message) {
        net.dodian.uber.game.social.WorldAnnouncementService.broadcast(message);
    }

    public void yell(String message) {
        net.dodian.uber.game.social.WorldAnnouncementService.broadcastAll(message);
    }

    public void yellKilled(String message) {
        net.dodian.uber.game.social.WorldAnnouncementService.broadcastWilderness(message);
    }

    public void yellAreaKilled(String message, String area) {
        net.dodian.uber.game.social.WorldAnnouncementService.broadcastArea(message, area);
    }

    public boolean hasItemInInventory(int ItemID) {
        for (int playerItem : playerItems) {
            if ((playerItem - 1) == ItemID) {
                return true;
            }
        }
        return false;
    }

    public boolean hasItemsInInventory(int ItemID, int Amount) {
        int ItemCount = 0;

        for (int playerItem : playerItems) {
            if ((playerItem - 1) == ItemID) {
                ItemCount++;
            }
            if (ItemCount == Amount) {
                return true;
            }
        }
        return false;
    }

    public int getItemSlot(int ItemID) {
        for (int i = 0; i < playerItems.length; i++) {
            if ((playerItems[i] - 1) == ItemID) {
                return i;
            }
        }
        return -1;
    }

    public int GetBankItemSlot(int ItemID) {
        for (int i = 0; i < bankItems.length; i++) {
            if ((bankItems[i] - 1) == ItemID) {
                return i;
            }
        }
        return -1;
    }

    public int getBankItemSlot(int itemId) {
        return GetBankItemSlot(itemId);
    }

    // private int setLastVote = 0;

    public void pmstatus(int status) { // status: loading = 0 connecting = 1
        // fine = 2
        send(new PrivateMessageStatus(status));
    }

    public boolean playerHasItem(int itemID) {
        itemID++;
        for (int playerItem : playerItems)
            if (playerItem == itemID)
                return true;
        return false;
    }

    public boolean playerHasItem(String name) {
        for (int playerItem : playerItems)
            if (getItemName(playerItem - 1).contains(name))
                return true;
        return false;
    }

    public void wipeInv() {
        for (int i = 0; i < playerItems.length; i++) {
            if (playerItems[i] - 1 != 5733)
                deleteItem(playerItems[i] - 1, i, playerItemsN[i]);
        }
        checkItemUpdate();
        send(new SendMessage("Your inventory has been wiped!"));
    }

    public boolean checkItem(int itemID) {
        int rawId = itemID;
        itemID++;
        for (int playerItem : playerItems) {
            if (playerItem == itemID) {
                return true;
            }
        }
        // getEquipment() stores raw item ids with no +1 offset, unlike playerItems/bankItems.
        for (int i = 0; i < getEquipment().length; i++) {
            if (getEquipment()[i] == rawId) {
                return true;
            }
        }
        for (int bankItem : bankItems) {
            if (bankItem == itemID) {
                return true;
            }
        }
        return false;
    }

    public boolean playerHasItem(int itemID, int amt) {
        itemID++;
        int found = 0;
        for (int i = 0; i < playerItems.length; i++) {
            if (playerItems[i] == itemID) {
                if (playerItemsN[i] >= amt) {
                    return true;
                } else {
                    found++;
                }
            }
        }
        return found >= amt;
    }

    public void sendpm(long name, int rights, byte[] chatmessage, int messagesize) {
        // Preserve old signature but route through new outgoing packet implementation.
        send(new PrivateMessage(name, rights, chatmessage, messagesize, net.dodian.uber.game.engine.systems.world.player.PlayerRegistry.lastchatid++));
    }

    public void loadpm(long name, int world) {
        send(new LoadPrivateMessage(name, world));
    }

    
    /**
     * Decrements the arrow count and updates the client.
     * 
     * @return true if an arrow was deleted, false if no arrows were left
     */
    public boolean DeleteArrow() {
        int arrowSlot = Equipment.Slot.ARROWS.getId();
        if (getEquipmentN()[arrowSlot] > 0) {
            getEquipmentN()[arrowSlot] -= 1;
            int remainingAmount = getEquipmentN()[arrowSlot];
            
            int arrowId = getEquipment()[arrowSlot];
            if (remainingAmount < 1) {
                getEquipment()[arrowSlot] = -1;
            }
            
            // Send the update packet
            send(new DeleteArrow(arrowId, arrowSlot, remainingAmount));
            return true;
        }
        return false;
    }

    public void ReplaceObject(int objectX, int objectY, int newObjectID, int face, int objectType) {
        // Unlike ReplaceObject2, this had no range guard at all - any caller iterating every
        // online player (e.g. a door toggle broadcast) would send SetMap for a tile far outside
        // that viewer's loaded scene. The client truncates local coordinates to a byte
        // (SetMap.java) and wraps mod 256, so a distant SetMap can land back inside the visible
        // 0..103 range and render a phantom object at an unrelated tile. Match ReplaceObject2's
        // distance guard so this method is safe regardless of caller. No plane check here - this
        // method (unlike ReplaceObject2) is never given a z, so callers remain responsible for
        // filtering by plane themselves (DoorToggleObjectContent already does).
        if (!withinDistance(new int[]{objectX, objectY, 60})) {
            return;
        }
        send(new SetMap(new Position(objectX, objectY)));
        send(new ReplaceObject(newObjectID, face, objectType));
    }

    public int GetNPCID(int coordX, int coordY) {
        for (Npc n : Server.npcManager.getNpcs()) {
            if (n.getPosition().getX() == coordX && n.getPosition().getY() == coordY) {
                return n.getId();
            }
        }
        return 1;
    }

    public String GetNpcName(int NpcID) {
        if (Server.npcManager == null) {
            return "Npc";
        }
        String npcName = Server.npcManager.getName(NpcID);
        if (npcName == null) {
            return "Npc";
        }
        return npcName.replaceAll("_", " ");
    }

    public String getItemName(int ItemID) {
        return Server.itemManager.getName(ItemID);
    }

    public double GetShopSellValue(int ItemID) {
        return Server.itemManager.getShopSellValue(ItemID);
    }

    public double GetShopBuyValue(int ItemID) {
        return Server.itemManager.getShopBuyValue(ItemID);
    }

    public int getUnnotedItem(int ItemID) {
        return Server.itemManager.getLinkedItemId(ItemID);
    }

    public int getNotedItem(int ItemID) {
        return Server.itemManager.getLinkedNotedId(ItemID);
    }

    public void WriteEnergy() {
        // Stub: always report 100% run energy to the client
        send(new SendRunEnergy(100));
        invalidateUiText(149);
        send(new SendString("100%", 149));
    }

    public void updateRunEnergy() {
        WriteEnergy();
    }

    public void ResetBonus() {
        Arrays.fill(playerBonus, 0);
    }

    public void GetBonus(boolean update) {
        ResetBonus();
        for (int i = 0; i < 14; i++) {
            if (getEquipment()[i] > 0) {
                int timed = checkObsidianBonus(getEquipment()[i]) ? 2 : 1;
                if (!(getDuelFight() && i == 8)) {
                    for (int k = 0; k < playerBonus.length; k++) {
                        int bonus = Server.itemManager.getBonus(getEquipment()[i], k);
                        playerBonus[k] += bonus * timed;
                    }
                }
            }
        }
        if (update) WriteBonus();
    }

    public void WriteBonus() {
        for (int i = 0; i < playerBonus.length; i++)
            updateBonus(i);
        writeOsrsBonuses();
    }

    private static final String[] OSRS_BONUS_NAMES = {
        "Stab", "Slash", "Crush", "Magic", "Range",
        "Stab", "Slash", "Crush", "Magic", "Range",
        "Strength", "Ranged Strength", "Magic Strength", "Prayer"
    };
    private static final int[] OSRS_BONUS_IDS = {
        15130, 15131, 15132, 15133, 15134,
        15135, 15136, 15137, 15138, 15139,
        15140, 15141, 15142, 15143
    };

    public void writeOsrsBonuses() {
        for (int i = 0; i < 14; i++) {
            int val = playerBonus[i];
            String bonusStr;
            if (i == 12) {
                bonusStr = OSRS_BONUS_NAMES[i] + ": " + (val >= 0 ? "+" : "") + val + "%";
            } else {
                bonusStr = OSRS_BONUS_NAMES[i] + ": " + (val >= 0 ? "+" : "") + val;
            }
            send(new SendString(bonusStr, OSRS_BONUS_IDS[i]));
        }

        int meleeMax = net.dodian.uber.game.combat.CombatPlayerExtensionsKt.meleeMaxHit(this);
        int rangedMax = net.dodian.uber.game.combat.CombatPlayerExtensionsKt.rangedMaxHit(this);
        send(new SendString("Melee Maxhit: <col=ff7000>" + meleeMax + "</col>", 15116));
        send(new SendString("Range Maxhit: <col=ff7000>" + rangedMax + "</col>", 15117));

        double totalWeight = 0.0;
        for (int i = 0; i < 14; i++) {
            if (getEquipment()[i] > 0) {
                totalWeight += Server.itemManager.getWeight(getEquipment()[i]);
            }
        }
        send(new SendString(String.format("%.1f kg", totalWeight), 15145));
    }

    public void loadAndShowModcpDetails(String targetName, boolean isOnline, String ipAddress) {
        net.dodian.uber.game.persistence.DbDispatchers.accountExecutor.execute(() -> {
            final String[] resultData = new String[4]; // [realName, rankName, createdDate, lastLoginDate]
            resultData[0] = targetName;
            resultData[1] = "Player";
            resultData[2] = "N/A";
            resultData[3] = "N/A";

            try {
                net.dodian.uber.game.persistence.repository.DbAsyncRepository.withConnection(conn -> {
                    try {
                        String query = "SELECT c.name, c.lastlogin, u.joindate, u.usergroupid " +
                                       "FROM characters c " +
                                       "LEFT JOIN user u ON LOWER(c.name) = LOWER(u.username) " +
                                       "WHERE LOWER(c.name) = ?";
                        java.sql.PreparedStatement ps = conn.prepareStatement(query);
                        ps.setString(1, targetName.trim().toLowerCase());
                        java.sql.ResultSet rs = ps.executeQuery();
                        if (rs.next()) {
                            resultData[0] = rs.getString("name");
                            int mgroup = rs.getInt("usergroupid");
                            int rights = (mgroup == 9 || mgroup == 5) ? 1 : ((mgroup == 6 || mgroup == 18 || mgroup == 10) ? 2 : 0);
                            resultData[1] = rights == 1 ? "Moderator" : rights >= 2 ? "Administrator" : "Player";

                            long joinSeconds = rs.getLong("joindate");
                            if (joinSeconds > 0) {
                                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
                                resultData[2] = sdf.format(new java.util.Date(joinSeconds * 1000L));
                            }

                            String lastLoginStr = rs.getString("lastlogin");
                            if (lastLoginStr != null && !lastLoginStr.isEmpty() && !lastLoginStr.equals("0")) {
                                try {
                                    long lastLoginMs = Long.parseLong(lastLoginStr);
                                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
                                    resultData[3] = sdf.format(new java.util.Date(lastLoginMs));
                                } catch (NumberFormatException e) {
                                    resultData[3] = lastLoginStr;
                                }
                            }
                        }
                    } catch (java.sql.SQLException e) {
                        logger.error("Failed to load moderation details target={} requester={} slot={} pos={} interface={} recent={}",
                                targetName, getPlayerName(), getSlot(), getPosition(), activeInterfaceId, describeRecentInboundPackets(), e);
                    }
                    return null;
                });
            } catch (Exception e) {
                logger.error("Moderation details request failed target={} requester={} slot={} pos={} interface={} recent={}",
                        targetName, getPlayerName(), getSlot(), getPosition(), activeInterfaceId, describeRecentInboundPackets(), e);
            }

            net.dodian.uber.game.engine.loop.GameThreadIngress.submitCritical("modcp-details", () -> {
                send(new SendString(resultData[0], 36706));
                send(new SendString(resultData[1], 36708));
                send(new SendString(resultData[2], 36710)); // Created date
                send(new SendString("Last Login:", 36711)); // Change label from "Play Time:" to "Last Login:"
                send(new SendString(resultData[3], 36712)); // Last login value
                send(new SendString(isOnline ? ipAddress : "Offline", 36714)); // IP address
            });
        });
    }

    public void openModcpList() {
        if (playerRights < 1) {
            sendMessage("You do not have permission to use the Game Control Panel.");
            return;
        }

        modcpPlayerList.clear();
        int count = 0;
        for (int i = 0; i < net.dodian.uber.game.engine.systems.world.player.PlayerRegistry.players.length; i++) {
            net.dodian.uber.game.model.entity.player.Player p = net.dodian.uber.game.engine.systems.world.player.PlayerRegistry.players[i];
            if (p != null) {
                modcpPlayerList.add(p.playerName);
                count++;
                if (count >= 20) {
                    break;
                }
            }
        }

        // Fill the 20 slots in the left scroll list (36731 to 36769)
        int stringId = 36732;
        for (int i = 0; i < 20; i++) {
            if (i < modcpPlayerList.size()) {
                send(new SendString(modcpPlayerList.get(i), stringId));
            } else {
                send(new SendString("", stringId));
            }
            stringId += 2;
        }

        // Set the right side details to the staff member's own info
        loadAndShowModcpDetails(playerName, true, connectedFrom);

        openInterface(36700);
    }

    public void openModcp(String targetName) {
        if (playerRights < 1) {
            sendMessage("You do not have permission to use the Game Control Panel.");
            return;
        }

        Client other = null;
        for (int i = 0; i < net.dodian.uber.game.engine.systems.world.player.PlayerRegistry.players.length; i++) {
            net.dodian.uber.game.model.entity.player.Player p = net.dodian.uber.game.engine.systems.world.player.PlayerRegistry.players[i];
            if (p != null && p.playerName.equalsIgnoreCase(targetName.trim())) {
                other = (Client) p;
                break;
            }
        }

        managingName = targetName;
        loadAndShowModcpDetails(targetName, other != null, other != null ? other.connectedFrom : "Offline");
        openInterface(36700);
    }

    public void handleModcpDialogue(int option) {
        String targetName = managingName;
        if (targetName == null || targetName.isEmpty()) {
            sendMessage("No target player selected.");
            modcpDialogState = 0;
            send(new net.dodian.uber.game.netty.listener.out.RemoveInterfaces());
            return;
        }

        Client other = null;
        for (int i = 0; i < net.dodian.uber.game.engine.systems.world.player.PlayerRegistry.players.length; i++) {
            net.dodian.uber.game.model.entity.player.Player p = net.dodian.uber.game.engine.systems.world.player.PlayerRegistry.players[i];
            if (p != null && p.playerName.equalsIgnoreCase(targetName.trim())) {
                other = (Client) p;
                break;
            }
        }

        if (modcpDialogState == 1) { // Main Menu: 1=Teleport, 2=Mod, 3=Containers, 4=Cancel
            if (option == 1) { // Teleport actions
                modcpDialogState = 2;
                showPlayerOption(new String[]{"Teleport: " + targetName, "Teleport to them", "Teleport them to me", "Move them Home", "Back"});
            } else if (option == 2) { // Moderator actions
                modcpDialogState = 3;
                showPlayerOption(new String[]{"Mod: " + targetName, "Kick player", "Mute (24h)", "Un-mute player", "Back"});
            } else if (option == 3) { // Check containers
                modcpDialogState = 4;
                showPlayerOption(new String[]{"View: " + targetName, "Check Bank", "Check Inventory", "Back"});
            } else { // Cancel
                modcpDialogState = 0;
                send(new net.dodian.uber.game.netty.listener.out.RemoveInterfaces());
            }
        } else if (modcpDialogState == 2) { // Teleport Sub-menu: 1=To, 2=To Me, 3=Home, 4=Back
            if (option == 4) { // Back
                modcpDialogState = 1;
                showPlayerOption(new String[]{"Manage " + targetName, "Teleport Actions", "Moderator Actions", "Check Containers", "Cancel"});
                return;
            }
            if (other == null) {
                sendMessage("Player is offline.");
                return;
            }
            if (option == 1) {
                transport(other.getPosition().copy());
                sendMessage("Teleporting to " + other.playerName + "...");
            } else if (option == 2) {
                other.transport(getPosition().copy());
                other.sendMessage("You have been teleported to " + playerName + ".");
                sendMessage("Teleporting " + other.playerName + " to you...");
            } else if (option == 3) {
                other.transport(new Position(2611, 3093, 0));
                other.sendMessage("You have been teleported home by " + playerName + ".");
                sendMessage("Teleporting " + other.playerName + " home...");
            }
            modcpDialogState = 0;
            send(new net.dodian.uber.game.netty.listener.out.RemoveInterfaces());
        } else if (modcpDialogState == 3) { // Moderator Actions: 1=Kick, 2=Mute, 3=Unmute, 4=Back
            if (option == 4) { // Back
                modcpDialogState = 1;
                showPlayerOption(new String[]{"Manage " + targetName, "Teleport Actions", "Moderator Actions", "Check Containers", "Cancel"});
                return;
            }
            if (option == 1) { // Kick
                if (other == null) {
                    sendMessage("Player is offline.");
                    return;
                }
                other.disconnected = true;
                sendMessage("You have kicked " + other.playerName + ".");
            } else if (option == 2) { // Mute
                long muteTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000L); // 24 hours
                if (other != null) {
                    other.mutedTill = muteTime;
                    other.sendMessage("You have been muted for 24 hours by " + playerName + ".");
                } else {
                    net.dodian.uber.game.persistence.DbDispatchers.accountExecutor.execute(() -> {
                        try {
                            net.dodian.uber.game.persistence.repository.DbAsyncRepository.withConnection(conn -> {
                                try {
                                    java.sql.PreparedStatement ps = conn.prepareStatement("UPDATE characters SET unmutetime = ? WHERE name = ?");
                                    ps.setLong(1, muteTime);
                                    ps.setString(2, targetName);
                                    ps.executeUpdate();
                                } catch (java.sql.SQLException e) {
                                    logger.error("Failed to persist offline mute target={} requester={} slot={} pos={} interface={} recent={}",
                                            targetName, getPlayerName(), getSlot(), getPosition(), activeInterfaceId, describeRecentInboundPackets(), e);
                                }
                                return null;
                            });
                        } catch (Exception e) {
                            net.dodian.uber.game.engine.loop.GameThreadIngress.submitCritical("mute-error", () -> {
                                sendMessage("Error muting offline player: " + e.getMessage());
                            });
                        }
                    });
                }
                sendMessage("You have muted " + targetName + " for 24 hours.");
            } else if (option == 3) { // Unmute
                if (other != null) {
                    other.mutedTill = 0;
                    other.sendMessage("You have been unmuted by " + playerName + ".");
                } else {
                    net.dodian.uber.game.persistence.DbDispatchers.accountExecutor.execute(() -> {
                        try {
                            net.dodian.uber.game.persistence.repository.DbAsyncRepository.withConnection(conn -> {
                                try {
                                    java.sql.PreparedStatement ps = conn.prepareStatement("UPDATE characters SET unmutetime = 0 WHERE name = ?");
                                    ps.setString(1, targetName);
                                    ps.executeUpdate();
                                } catch (java.sql.SQLException e) {
                                    logger.error("Failed to persist offline unmute target={} requester={} slot={} pos={} interface={} recent={}",
                                            targetName, getPlayerName(), getSlot(), getPosition(), activeInterfaceId, describeRecentInboundPackets(), e);
                                }
                                return null;
                            });
                        } catch (Exception e) {
                            net.dodian.uber.game.engine.loop.GameThreadIngress.submitCritical("unmute-error", () -> {
                                sendMessage("Error unmuting offline player: " + e.getMessage());
                            });
                        }
                    });
                }
                sendMessage("You have unmuted " + targetName + ".");
            }
            modcpDialogState = 0;
            send(new net.dodian.uber.game.netty.listener.out.RemoveInterfaces());
        } else if (modcpDialogState == 4) { // Check Containers: 1=Bank, 2=Inventory, 3=Back
            if (option == 3) { // Back
                modcpDialogState = 1;
                showPlayerOption(new String[]{"Manage " + targetName, "Teleport Actions", "Moderator Actions", "Check Containers", "Cancel"});
                return;
            }
            // Close the option dialogue before opening the read-only inspection view.
            // Sending RemoveInterfaces afterwards would immediately close the newly
            // opened bank-style view, which made both ModCP container actions appear
            // to do nothing. The inspection service handles online and offline names.
            modcpDialogState = 0;
            send(new net.dodian.uber.game.netty.listener.out.RemoveInterfaces());
            if (option == 1) { // Check Bank
                net.dodian.uber.game.economy.AdminContainerInspectionService.openBank(this, targetName);
            } else if (option == 2) { // Check Inventory
                net.dodian.uber.game.economy.AdminContainerInspectionService.openInventory(this, targetName);
            }
        }
    }

    public int neglectDmg() {
        int bonus = 0;
        if (getEquipment()[Equipment.Slot.SHIELD.getId()] == 11284)
            bonus += net.dodian.uber.skills.firemaking.FiremakingCombatService.dragonfireNeglectBonus(getLevel(Skill.FIREMAKING));
        return Math.min(1000, playerBonus[13] + bonus);
    }

    public double magicDmg() {
        return 1.0 + (playerBonus[12] / 100.0);
    }

    public void updateBonus(int id) {
        String send;
        if (id >= 0 && id < 14) {
            send = net.dodian.uber.game.ui.InterfaceConstants.BONUS_NAMES[id] + ": " + (playerBonus[id] >= 0 ? "+" + playerBonus[id] : playerBonus[id]);
        } else {
            send = "Bonus: " + playerBonus[id];
        }
        send(new SendString(send, 1675 + (id >= 10 ? id + 1 : id)));
    }

    public boolean GoodDistance2(int objectX, int objectY, int playerX, int playerY, int distance) {
        for (int i = 0; i <= distance; i++) {
            for (int j = 0; j <= distance; j++) {
                if (objectX == playerX
                        && ((objectY + j) == playerY || (objectY - j) == playerY)) {
                    return true;
                } else if (objectY == playerY
                        && ((objectX + j) == playerX || (objectX - j) == playerX)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void setEntityAttackOption(int value) {
        this.entityAttackOption = value;
    }

    /* NPC Talking */
    public void UpdateNPCChat() {
        DialogueDisplayService.updateNpcChat(this);
    }

    public void showPlayerOption(String[] text) {
        DialogueDisplayService.showPlayerOption(this, text);
    }

    public void showNPCChat(int npcId, int emote, String[] text) {
        DialogueDisplayService.showNpcChat(this, npcId, emote, text);
    }

    public void showPlayerChat(String[] text, int emote) {
        DialogueDisplayService.showPlayerChat(this, text, emote);
    }

    /* Equipment level checking */
    public int GetCLAttack(int ItemID) {
        if (ItemID == -1) {
            return 1;
        }
        String ItemName = getItemName(ItemID);
        String ItemName2 = ItemName.replace("Bronze", "");

        ItemName2 = ItemName2.replace("Iron", "");
        ItemName2 = ItemName2.replace("Steel", "");
        ItemName2 = ItemName2.replace("Black", "");
        ItemName2 = ItemName2.replace("Mithril", "");
        ItemName2 = ItemName2.replace("Adamant", "");
        ItemName2 = ItemName2.replace("Rune", "");
        ItemName2 = ItemName2.replace("Granite", "");
        ItemName2 = ItemName2.replace("Dragon", "");
        ItemName2 = ItemName2.replace("Crystal", "");
        ItemName2 = ItemName2.trim();
        if (ItemName2.startsWith("claws") || ItemName2.startsWith("dagger") || ItemName2.startsWith("sword")
                || ItemName2.startsWith("scimitar") || ItemName2.startsWith("mace") || ItemName2.startsWith("longsword")
                || ItemName2.startsWith("battleaxe") || ItemName2.startsWith("warhammer") || ItemName2.startsWith("2h sword")
                || ItemName2.startsWith("halberd") || ItemName2.endsWith("axe") || ItemName2.endsWith("pickaxe")) {
            if (ItemName.startsWith("Bronze")) {
                return 1;
            } else if (ItemName.startsWith("Iron")) {
                return 1;
            } else if (ItemName.startsWith("Steel")) {
                return 10;
            } else if (ItemName.startsWith("Black")) {
                return 10;
            } else if (ItemName.startsWith("Mithril")) {
                return 20;
            } else if (ItemName.startsWith("Adamant")) {
                return 30;
            } else if (ItemName.startsWith("Rune")) {
                return 40;
            } else if (ItemName.startsWith("Dragon")) {
                return 60;
            }
        }
        if (ItemID >= 1393 && ItemID <= 1400) //Elemental battlestaff
            return 20;
        if (ItemID == 2952 || ItemID == 10581)
            return 40;
        if (ItemID == 21646)
            return 50;
        if (ItemID == 6523 || ItemID == 6525 || ItemID == 6527)
            return 55;
        if (ItemID == 3842 || ItemID == 20223)
            return 45;
        if (ItemID == 4755 || ItemID == 4747) //Barrows weapons!
            return 70;
        return 1;
    }

    public int GetCLDefence(int ItemID) {
        if (ItemID == -1) return 1;
        String checkName = getItemName(ItemID).toLowerCase();
        if (checkName.endsWith("arrow") || checkName.endsWith("hat") || (checkName.endsWith("axe") && !checkName.startsWith("battle")))
            return 1;
        String ItemName = getItemName(ItemID);
        if (ItemName.toLowerCase().contains("beret") || ItemName.toLowerCase().contains("cavalier") || ItemName.toLowerCase().contains("mystic") || checkName.contains("mask") || checkName.contains("partyhat"))
            return 1;
        String ItemName2 = ItemName.replace("Bronze", "");
        ItemName2 = ItemName2.replace("Iron", "");
        ItemName2 = ItemName2.replace("Steel", "");
        ItemName2 = ItemName2.replace("Black", "");
        ItemName2 = ItemName2.replace("Mithril", "");
        ItemName2 = ItemName2.replace("Adamant", "");
        ItemName2 = ItemName2.replace("Rune", "");
        ItemName2 = ItemName2.replace("Granite", "");
        ItemName2 = ItemName2.replace("Dragon", "");
        ItemName2 = ItemName2.replace("Crystal", "");
        ItemName2 = ItemName2.trim();
        if (ItemName2.startsWith("claws") || ItemName2.startsWith("dagger") || ItemName2.startsWith("sword")
                || ItemName2.startsWith("scimitar") || ItemName2.startsWith("mace") || ItemName2.startsWith("longsword")
                || ItemName2.startsWith("battleaxe") || ItemName2.startsWith("warhammer") || ItemName2.startsWith("2h sword")
                || ItemName2.startsWith("harlberd") || ItemName2.startsWith("pickaxe")) {// It's a weapon,
            return 1;
        } else if (ItemName.endsWith("crossbow") || ItemName.endsWith("hammers")
                || ItemName.endsWith("flail") || ItemName.endsWith("warspear") || ItemName.endsWith("greataxe")) {
            return 1;
        } else {
            if (ItemName.startsWith("Saradomin") || ItemName.startsWith("Zamorak") || ItemName.startsWith("Guthix") ||
                    checkName.contains("staff") || checkName.contains("cape"))
                return 1;
            if (ItemID == 11284)
                return 75;
            if (ItemID == 4224)
                return 70;
            if ((ItemName.startsWith("Ahrim") && !checkName.contains("staff")) || (ItemName.startsWith("Karil") && !checkName.contains("crossbow")) ||
                    (ItemName.startsWith("Verac") && !checkName.contains("flail")) || (ItemName.startsWith("Dharok") && !checkName.contains("greataxe")) ||
                    (ItemName.startsWith("Torag") && !checkName.contains("hammers")) || (ItemName.startsWith("Guthan") && !checkName.contains("warspear")))
                return 70;
            if (ItemName.startsWith("Bronze")) {
                return 1;
            } else if (ItemName.startsWith("Iron")) {
                return 1;
            } else if (ItemName.startsWith("Steel") && !ItemName.contains("arrow")) {
                return 5;
            } else if (ItemName.startsWith("Black") && !ItemName.contains("hide")) {
                return 10;
            } else if (ItemName.startsWith("Slayer helmet") || ItemName.startsWith("Spiny helmet")) {
                return 10;
            } else if (ItemName.startsWith("Mithril") && !ItemName.contains("arrow")) {
                return 20;
            } else if (ItemName.startsWith("Splitbark")) {
                return 20;
            } else if (ItemName.startsWith("Adamant") && !ItemName.contains("arrow")) {
                return 30;
            } else if (ItemName.startsWith("Rune") && !ItemName.endsWith("cape") && !ItemName.contains("arrow")) {
                return 40;
            } else if (ItemName.startsWith("Granite") && ItemID != 4153 && ItemID != 21646) {
                return 50;
            } else if (ItemName.startsWith("Dragon") && !ItemName.contains("hide") && !ItemName.toLowerCase().contains("ring") && !ItemName.toLowerCase().contains("necklace") && !ItemName.toLowerCase().contains("amulet")) {
                return 60;
            } else if (ItemName.startsWith("Rock-shell")) {
                return 60;
            }
        }
        if (ItemName.toLowerCase().contains("ghostly"))
            return 70;
        if (ItemName.startsWith("Skeletal"))
            return 1;
        if (ItemName.startsWith("Snakeskin body") || ItemName.startsWith("Snakeskin chaps"))
            return 60;
        if (ItemID == 1135 || ItemID == 2499 || ItemID == 2501 || ItemID == 2503)
            return 40;
        if (ItemID == 20235)
            return 45;
        if (ItemID == 6524 || ItemID == 21298 || ItemID == 21301 || ItemID == 21304) //Obsidian
            return 55;
        return 1;
    }

    public int GetCLStrength(int ItemID) {
        if (ItemID == -1) return 1;
        if (ItemID == 3842 || ItemID == 20223 || ItemID == 20232)
            return 45;
        if (ItemID == 4153)
            return 50;
        if (ItemID == 6528)
            return 55;
        if (ItemID == 4718 || ItemID == 4726) //Barrows weapons!
            return 70;
        return 1;
    }

    public int GetCLMagic(int ItemID) {
        if (ItemID == -1) return 1;
        String ItemName = getItemName(ItemID);
        if (ItemID >= 2415 && ItemID <= 2417)
            return 10;
        if (ItemName.startsWith("White Mystic") || ItemName.startsWith("Splitbark"))
            return 20;
        if (ItemID == 4675)
            return 25;
        if (ItemName.startsWith("Black Mystic"))
            return 35;
        if (ItemID == 6526)
            return 40;
        if (ItemID == 3840 || ItemID == 20220)
            return 45;
        if (ItemName.startsWith("Infinity") || ItemID == 6914)
            return 50;
        if (ItemID == 13235)
            return 60;
        if (ItemName.toLowerCase().contains("ghostly") || ItemName.startsWith("Ahrim"))
            return 70;
        return 1;
    }

    public int GetCLRanged(int ItemID) {
        if (ItemID == -1) return 1;
        String ItemName = getItemName(ItemID);
        if (ItemName.startsWith("Oak")) {
            return 1;
        }
        if (ItemName.startsWith("Willow")) {
            return 20;
        }
        if (ItemName.startsWith("Maple")) {
            return 30;
        }
        if (ItemName.startsWith("Yew")) {
            return 40;
        }
        if (ItemName.startsWith("Magic") && !ItemName.toLowerCase().contains("cape")) {
            return 50;
        }
        if (ItemName.startsWith("New crystal bow")) {
            return 65;
        }
        if (ItemID == 6724) //Seercull
            return 75;
        if (ItemID == 20997) //Twisted bow
            return 85;
        if (ItemName.startsWith("Green d")) {
            return 40;
        }
        if (ItemName.startsWith("Blue d")) {
            return 50;
        }
        if (ItemName.startsWith("Red d")) {
            return 60;
        }
        if (ItemName.startsWith("Black d")) {
            return 70;
        }
        if (ItemName.startsWith("Karil")) {
            return 70;
        }
        if (ItemName.startsWith("Spined")) {
            return 75;
        }
        if (ItemName.startsWith("Snakeskin")) { //Do we want snakeskin?!
            return 80;
        }
        if (ItemName.startsWith("Steel arr")) {
            return 10;
        }
        if (ItemName.startsWith("Mithril arr")) {
            return 20;
        }
        if (ItemName.startsWith("Adamant arr")) {
            return 30;
        }
        if (ItemName.startsWith("Rune arr")) {
            return 40;
        }
        if (ItemName.startsWith("Dragon arr")) {
            return 60;
        }
        if (ItemID == 3844 || ItemID == 20226 || ItemID == 20229) //Shield or blessings!
            return 45;
        return 1;
    }



    public static String passHash(String in, String salt) {
        String passM = new MD5(in).compute();
        return new MD5(passM + salt).compute();
    }

    public String getLook() {
        StringBuilder out = new StringBuilder();
        for (int playerLook : playerLooks) {
            out.append(playerLook).append(" ");
        }
        return out.toString();
    }

    public String getPouches() {
        return getRunePouchState().saveValue();
    }

    public void setLook(int[] parts) {
        boolean canSet = true;
        for (int i = 0; i < parts.length && canSet; i++) { //0 3 14 18 26 34 38 42 2 14 5 4 0
            if (parts[i] < -1) {
                canSet = false;
                send(new SendMessage("You need to set your look again as it was bugged!"));
                defaultCharacterLook(this);
            }
        }
        if (canSet) {
            setGender(parts[0]);
            setHead(parts[1]);
            setBeard(parts[2]);
            setTorso(parts[3]);
            setArms(parts[4]);
            setHands(parts[5]);
            setLegs(parts[6]);
            setFeet(parts[7]);
            pHairC = parts[8];
            pTorsoC = parts[9];
            pLegsC = parts[10];
            pFeetC = parts[11];
            pSkinC = parts[12];
            playerLooks = parts;
            getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
        }
    }

    public void resetPos() {
        transport(new Position(2604 + Misc.random(6), 3101 + Misc.random(3), 0));
    }

    public boolean canUse(int id) {
        return !premium && premiumItem(id);
    }

    public boolean premiumItem(int id) {
        return Server.itemManager.isPremium(id);
    }

    public void debug(String text) {
        if (debug) {
            send(new SendMessage(text));
        }
    }

    public int findItem(int id, int[] items, int[] amounts) {
        for (int i = 0; i < playerItems.length; i++) {
            if ((items[i] - 1) == id && amounts[i] > 0) {
                return i;
            }
        }
        return -1;
    }

    public boolean hasSpace() {
        for (int playerItem : playerItems) {
            if (playerItem == -1 || playerItem == 0) {
                return true;
            }
        }
        return false;
    }

    public int getFreeSpace() {
        int spaces = 0;
        for (int playerItem : playerItems) {
            if (playerItem == -1 || playerItem == 0) {
                spaces += 1;
            }
        }
        return spaces;
    }

    public void resetAction(boolean full) {
        ContentActions.cancel(this, PlayerActionCancelReason.MANUAL_RESET, full, false, false, true);
    }

    public void resetAction() {
        resetAction(true);
    }

    public void replaceDoors() {
        for (int d = 0; d < DoorRegistry.doorX.length; d++) {
            if (DoorRegistry.doorX[d] > 0 && DoorRegistry.doorHeight[d] == getPosition().getZ()
                    && Math.abs(DoorRegistry.doorX[d] - getPosition().getX()) <= 52
                    && Math.abs(DoorRegistry.doorY[d] - getPosition().getY()) <= 52) {
                ReplaceObject(DoorRegistry.doorX[d], DoorRegistry.doorY[d], DoorRegistry.doorId[d], DoorRegistry.doorFace[d], 0);
            }
        }
    }

    public void modYell(String msg) {
        for (int i = 0; i < PlayerRegistry.players.length; i++) {
            Client p = (Client) net.dodian.uber.game.engine.systems.world.player.PlayerRegistry.players[i];
            if (p != null && !p.disconnected && p.getPosition().getX() > 0 && p.dbId > 0 && p.playerRights > 0) {
                p.send(new SendMessage(msg));
            }
        }
    }

    public void triggerTele(int x, int y, int height, boolean prem) {
        triggerTele(x, y, height, prem, ancients == 1 ? 1979 : 714);
    }

    public void triggerTele(int x, int y, int height, boolean prem, int emote) {
        if (getInDuel() || UsingAgility || doingTeleport() || getStunTimer() > 0) {
            return;
        }
        if (rejectTeleport()) {
            send(new SendMessage("A magical force stop you from teleporting."));
            return;
        }
        if (isInCombat() || getSkillingEventState().isSecondaryRandomEventPending()) {
            send(new SendMessage(isInCombat() ? "You cant teleport during combat!" : "You can't teleport out of here!"));
            return;
        }
        if (inWildy()) {
            send(new SendMessage("You can't teleport out of the wilderness!"));
            return;
        }
        if (prem && !premium) {
            send(new SendMessage("This spell is only available to premium members, visit Dodian.net for info"));
            return;
        }
        resetActionTeleport();
        resetAction(false);
        resetWalkingQueue();
        UsingAgility = true;
        tH = height;
        addEffectTime(0, -1); //If we teleport, reset desert shiez!
        TeleportActionService.startTeleport(this, x, y, height, emote);
    }

    public boolean inHeat() { //King black dragon's domain!
        return getPosition().getX() >= 3264 && getPosition().getX() <= 3327 && getPosition().getY() >= 9344 && getPosition().getY() <= 9407;
    }

    public boolean validClient(int index) {
        return PlayerRegistry.validClient(index);
    }

    public Client getClient(int index) {
        return PlayerRegistry.getClient(index);
    }

    public void processPendingApproaches() {
        net.dodian.uber.game.engine.systems.interaction.ui.PlayerSocialApproachService.process(this);
    }

    /*
     * Danno: Edited for new duel rules, for future use.
     */
    public void setVarp(int id, int value) {
        send(new SetVarbit(id, value));
    }

    public void varbit(int id, int value) {
        // Compatibility alias. This writes a raw client settings/varp slot.
        setVarp(id, value);
    }

    public boolean isMuted() {
        long rightNow = System.currentTimeMillis();
        return mutedTill - rightNow > 0;
    }

    public boolean hasFriend(long name) {
        for (Friend f : friends) {
            if (f.name == name) {
                return true;
            }
        }
        return false;
    }

    public void refreshFriends() {
        for (Friend f : friends) {
            Client player = PlayerRegistry.playersOnline.get(f.name);
            if (player == null) {
                loadpm(f.name, 0);
                continue;
            }
            boolean ignored = false;
            for (Friend ignore : player.ignores) {
                if (ignore.name == this.longName) {
                    ignored = true;
                    break;
                }
            }
            loadpm(f.name, ignored ? 0 : 1);
        }
    }

    public void triggerChat(int button) {
        DialogueOptionService.triggerChat(this, button);
    }

    public void callGfxMask(int id, int height) {
        setGraphic(id, height == 0 ? 65536 : 65536 * height);
        getUpdateFlags().setRequired(UpdateFlag.GRAPHICS, true);
    }

    public void AddToCords(int X, int Y, boolean run) {
        if (X == 0 && Y == 0) {
            resetWalkingQueue();
            return;
        }
        MovementPoint destination = new MovementPoint(
                getPosition().getX() + X,
                getPosition().getY() + Y,
                getPosition().getZ()
        );
        replaceMovementRoute(Collections.singletonList(destination), run);
    }

    public void appendForcemovement(Position startPos, Position endPos, int... speed) {
        if (speed.length != 3) { //Need atleast 3 values!
            return;
        }
        
        int startX = startPos.getX();
        int startY = startPos.getY();
        int endX = endPos.getX();
        int endY = endPos.getY();

        m4001 = startPos.getLocalX();
        m4002 = startPos.getLocalY();
        m4003 = startX + endX;
        m4004 = startY + endY;
        m4006 = speed[0];
        m4005 = speed[1];
        m4007 = speed[2];
        getUpdateFlags().setRequired(UpdateFlag.FORCED_MOVEMENT, true);
		/*
		int startX = startPos.getLocalX();
		int startY = startPos.getLocalY();
		m4001 = startX;
		m4002 = startY;
		m4003 = startX + 4;
		m4004 = startY;
		m4005 = speed[1];
		m4006 = speed[0];
		m4007 = speed[2];
		 */
		/*getPacketBuffer().writeByteA(startPos.getX());
		getPacketBuffer().writeByteA(startPos.getY());
		getPacketBuffer().writeByteA(startPos.getX() + endX);
		getPacketBuffer().writeByteA(startPos.getY() + endY);
		getPacketBuffer().writeWordBigEndianA(speed[0]);
		getPacketBuffer().writeWordA(speed[1]);
		getPacketBuffer().writeByteA(speed[2]);*/
    }

    public void AddToWalkCords(int X, int Y, long time) {
        if (time > 0) applyWalkBlockMs(time);
        AddToCords(X, Y, false);
    }

    public void AddToRunCords(int X, int Y, long time) {
        if (time > 0) applyWalkBlockMs(time);
        AddToCords(X, Y, true);
    }

    public void startAttack(Entity enemy) {
        target = enemy;
        if (target instanceof Npc) {
            faceNpc(target.getSlot());
        } else {
            facePlayer(target.getSlot());
        }
    }

    public void resetAttack() {
        //rerequestAnim();
        magicId = -1;
        CombatStartService.clearCombatTarget(this);
    }

    public void requestWeaponAnims() {
        setStandAnim(Server.itemManager.getStandAnim(getEquipment()[Equipment.Slot.WEAPON.getId()]));
        setWalkAnim(Server.itemManager.getWalkAnim(getEquipment()[Equipment.Slot.WEAPON.getId()]));
        setRunAnim(Server.itemManager.getRunAnim(getEquipment()[Equipment.Slot.WEAPON.getId()]));
    }

    public int getWildLevel() {
        int lvl = 0;
        if (getPosition().getY() >= 3524 && getPosition().getY() < 3904 && getPosition().getX() >= 2954
                && getPosition().getX() <= 3327)
            lvl = (((getPosition().getY() - 3520) / 8)) + 1;
        return lvl;
    }

    public void setWildLevel(int level) {
        wildyLevel = level;
        // When leaving wilderness (level <= 0), do not send a "Level: 0" overlay.
        // updatePlayerDisplay() will restore the normal walkable interface instead.
        if (level <= 0) {
            lastWildLevelSent = level;
            return;
        }

        if (level != lastWildLevelSent) {
            send(new SetWildernessLevel(level));
            lastWildLevelSent = level;
            // Keep the cached walkable-interface state in sync so that
            // setWalkableInterface(6673) can properly restore the normal UI.
            currentWalkableInterface = 197;
        }
    }

    public void updatePlayerDisplay() {
        String serverName = getGameWorldId() == 1 ? "Uber Server 3.0" : "Beta World";
        String text = serverName + " (" + PlayerRegistry.getPlayerCount() + " online)";
        sendCachedString(text, 6570);
        lastTopBarText = text;
        sendCachedString("", 6664);
        setWalkableInterface(6673);
    }

    public void playerKilled(Client other) {
        other.skullIcon = 1;
        other.getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
    }

    public void setSnared(int gfx, int time) {
        stillgfx(gfx, getPosition().getX(), getPosition().getY());
        if (getSnareTimer() < 1) //Send message only if snare is less than 1!
            send(new SendMessage("You have been snared!"));
        setSnareTimer(time);
        resetWalkingQueue();
    }

    public void died() {
        int highestDmg = 0, slot = -1;
        for (Iterator<Map.Entry<Entity, Integer>> it = getDamage().entrySet().iterator(); it.hasNext();) {
            Map.Entry<Entity, Integer> entry = it.next();
            Entity e = entry.getKey();
            if (!(e instanceof Client) || !validClient(e.getSlot())) {
                it.remove();
                continue;
            }
            if (entry.getValue() > highestDmg) {
                highestDmg = entry.getValue();
                slot = e.getSlot();
            }
        }
        clearDamageAttribution();
        if (slot >= 0) {
            if (validClient(slot)) {
                Ground.addFloorItem(getClient(slot), 526, 1);
                getClient(slot).send(new SendMessage("You have defeated " + getPlayerName() + "!"));
                yellKilled(getClient(slot).getPlayerName() + " has just slain " + getPlayerName() + " in the wild!");
                Client other = getClient(slot);
                playerKilled(other);
            }
        }
        /* Stuff dropped to the floor! */
      /*for (int i = 0; i < getEquipment().length; i++) {
        if (getEquipment()[i] > 0) {
          if (Server.itemManager.isTradable(getEquipment()[i]))
            Ground.items.add(new GroundItem(getPosition().getX(), getPosition().getY(), getEquipment()[i],
                    getEquipmentN()[i], slot, -1));
          else
            Ground.items.add(new GroundItem(getPosition().getX(), getPosition().getY(), getEquipment()[i],
                    getEquipmentN()[i], getSlot(), -1));
        }
        getEquipment()[i] = -1;
        getEquipmentN()[i] = 0;
        deleteequiment(0, i);
      }
      for (int i = 0; i < playerItems.length; i++) {
        if (playerItems[i] > 0) {
          if (Server.itemManager.isTradable((playerItems[i] - 1)))
            Ground.items.add(new GroundItem(getPosition().getX(), getPosition().getY(), (playerItems[i] - 1),
                    playerItemsN[i], slot, -1));
          else
            Ground.items.add(new GroundItem(getPosition().getX(), getPosition().getY(), (playerItems[i] - 1),
                    playerItemsN[i], getSlot(), -1));
        }
        deleteItem((playerItems[i] - 1), i, playerItemsN[i]);
      }*/
    }

    public boolean contains(int item) {
        for (int playerItem : playerItems) {
            if (playerItem == item + 1)
                return true;
        }
        return false;
    }

    public void requestForceChat(String s) {
        forcedChat = s;
        getUpdateFlags().setRequired(UpdateFlag.FORCED_CHAT, true);
    }

    public void setInteractionAnchor(int x, int y) {
        setInteractionAnchor(x, y, getPosition().getZ());
    }

    public void setInteractionAnchor(int x, int y, int z) {
        setInteractionAnchorState(new InteractionAnchorState(x, y, z));
        if (WanneBank > 0)
            WanneBank = 0;
        if (NpcWanneTalk > 0) NpcWanneTalk = 0;
    }

    public int getInteractionAnchorX() {
        InteractionAnchorState state = getInteractionAnchorState();
        return state == null ? -1 : state.getX();
    }

    public int getInteractionAnchorY() {
        InteractionAnchorState state = getInteractionAnchorState();
        return state == null ? -1 : state.getY();
    }

    public void spendTickets() {
        // Agility ticket id 2996 - routes through the real item-click dispatch pipeline so this
        // uses the exact same AgilityModule.exchangeTickets() logic a plain inventory click on
        // the ticket already reaches, instead of a duplicate legacy implementation.
        int slot = getItemSlot(2996);
        if (slot < 0) {
            send(new SendMessage("You have no agility tickets!"));
            return;
        }
        ItemDispatcher.tryHandle(this, 1, 2996, slot, 3214);
    }

    public PrayerManager getPrayerManager() {
        return prayerManager;
    }

    public void checkBow() {
        int weaponId = getEquipment()[Equipment.Slot.WEAPON.getId()];
        getContentRuntimeState().setCombatUsingBow(bowWeapon(weaponId));
    }

    public boolean bowWeapon(int weaponId) {
        String weaponType = Server.itemManager.getWeaponType(weaponId);
        if (weaponType.isEmpty()) {
            return net.dodian.uber.skills.fletching.FletchingWeaponService.isBowWeapon(weaponId);
        }
        return weaponType.equals("bow") || weaponType.equals("crossbow") || weaponType.equals("thrown")
            || weaponType.equals("chinchompas") || weaponType.equals("salamander");
    }

    public boolean checkInv = false;

    public void updateGroundItems() {
        /* Untradeable items prio 1! */
        if (!Ground.untradeable_items.isEmpty())
            for (GroundItem item : Ground.untradeable_items) {
                if (item.isTaken() || dbId != item.playerId || getPosition().getZ() != item.z || !isWithinDistance(getPosition().getX(), getPosition().getY(), item.x, item.y, 104))
                    continue;
                send(new RemoveGroundItem(new GameItem(item.id, item.amount), new Position(item.x, item.y, item.z)));
                send(new CreateGroundItem(new GameItem(item.id, item.amount), new Position(item.x, item.y, item.z)));
            }
        /* Tradeable items prio 2! */
        if (!Ground.tradeable_items.isEmpty())
            for (GroundItem item : Ground.tradeable_items) {
                if (item.isTaken() || (item.playerId != dbId && !item.isVisible()) || getPosition().getZ() != item.z || !isWithinDistance(getPosition().getX(), getPosition().getY(), item.x, item.y, 104))
                    continue;
                send(new RemoveGroundItem(new GameItem(item.id, item.amount), new Position(item.x, item.y, item.z)));
                send(new CreateGroundItem(new GameItem(item.id, item.amount), new Position(item.x, item.y, item.z)));
            }
        /* Static ground items prio last! */
        if (!Ground.ground_items.isEmpty())
            for (GroundItem item : Ground.ground_items) {
                if (item.isTaken() || !item.visible || getPosition().getZ() != item.z || !isWithinDistance(getPosition().getX(), getPosition().getY(), item.x, item.y, 104))
                    continue;
                send(new RemoveGroundItem(new GameItem(item.id, item.amount), new Position(item.x, item.y, item.z)));
                send(new CreateGroundItem(new GameItem(item.id, item.amount), new Position(item.x, item.y, item.z)));
            }
    }



    @Deprecated
    public void removeItemsFromPlayer(String user, int id, int amount) {
        int totalItemRemoved = 0;
        if (PlayerRegistry.getPlayer(user) != null) { //Online check
            Client other = (Client) PlayerRegistry.getPlayer(user);
            for (int i = 0; i < Objects.requireNonNull(other).bankItems.length; i++) {
                if (other.bankItems[i] - 1 == id) {
                    int canRemove = Math.min(other.bankItemsN[i], amount);
                    other.bankItemsN[i] -= canRemove;
                    amount -= canRemove;
                    totalItemRemoved += canRemove;
                    if (other.bankItemsN[i] <= 0)
                        other.bankItems[i] = 0;
                    other.resetBank();
                }
            }
            for (int i = 0; i < other.playerItems.length; i++) {
                if (other.playerItems[i] - 1 == id) {
                    int canRemove = Math.min(other.playerItemsN[i], amount);
                    other.playerItemsN[i] -= canRemove;
                    amount -= canRemove;
                    totalItemRemoved += canRemove;
                    if (other.playerItemsN[i] <= 0)
                        other.playerItems[i] = 0;
                }
            }
            for (int i = 0; i < getEquipment().length; i++) {
                if (other.getEquipment()[i] == id) {
                    int canRemove = Math.min(other.getEquipmentN()[i], amount);
                    other.getEquipmentN()[i] -= canRemove;
                    amount -= canRemove;
                    totalItemRemoved += canRemove;
                    if (other.getEquipmentN()[i] <= 0) {
                        other.deleteequiment(id, i);
                    } else {
                        other.markSaveDirty(PlayerSaveSegment.EQUIPMENT.getMask());
                        other.equipmentChanged(i);
                    }
                }
            }
            if (totalItemRemoved > 0) { //Update items only if there is any deleted!
                send(new SendMessage("Finished deleting " + totalItemRemoved + " of " + getItemName(id).toLowerCase()));
                other.checkItemUpdate();
                other.getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
            } else
                send(new SendMessage("The user '" + user + "' did not had any " + getItemName(id).toLowerCase()));
        } else { //Database check!
            final int requestedAmount = amount;
            CommandDbService.submit(
                    "remove-items",
                    () -> CommandDbService.removeOfflineItems(user, id, requestedAmount),
                    result -> {
                        if (disconnected) {
                            return;
                        }
                        if (result.getStatus() == CommandDbService.OfflineItemRemovalResult.Status.NOT_FOUND) {
                            send(new SendMessage("username '" + user + "' have yet to login!"));
                            return;
                        }
                        if (result.getTotalItemRemoved() > 0) {
                            send(new SendMessage("Finished deleting " + result.getTotalItemRemoved() + " of " + getItemName(id).toLowerCase()));
                        } else {
                            send(new SendMessage("The user " + user + " did not had any " + getItemName(id).toLowerCase()));
                        }
                    },
                    exception -> {
                        if (!disconnected) {
                            logger.debug("issue: {}", exception.getMessage(), exception);
                            send(new SendMessage("Could not remove those items right now."));
                        }
                    }
            );
        }
    }

    public void dropAllItems() {
        //BAHAHAHAAHAH
        resetPos();
        modYell(getPlayerName() + " is currently bug abusing on a item!");
    }

    public boolean checkObsidianWeapons() {
        if (getEquipment()[2] != 11128) return false;
        int[] weapons = {6522, 6523, 6525, 6526, 6527, 6528};
        for (int weapon : weapons)
            if (getEquipment()[3] == weapon)
                return true;
        return false;
    }

    private boolean travelInitiate = false;

    public void setTravelMenu() {
        varbit(153, 0);
        send(new SendString("Brimhaven", 12338));
        send(new SendString("Island", 12339));
        send(new SendString("Catherby", 809));
        send(new SendString("Canifis", 810));
        send(new SendString("", 811)); //Trollheim?!
        send(new SendString("Shilo", 812));
        send(new SendString("Sophanem", 813));
        openInterface(802);
    }

    public void transport(Position pos) {
        resetActionTeleport();
        teleportTo(pos.getX(), pos.getY(), pos.getZ());
    }

    public void resetActionTeleport() {
        if (getPositionName(getPosition()) == positions.DESERT_MENAPHOS && effects.get(2) > 0) //Test area boost! MAybe do this for raid :O ?
            addEffectTime(2, 0);
    }

    public void travelTrigger(int checkPos) {
        travelTrigger(checkPos, actionButtonId);
    }

    public void travelTrigger(int checkPos, int buttonId) {
        if (travelInitiate) {
            return;
        }
        boolean home = checkPos != 0;
        TravelDecision decision = TravelRouteService.resolve(home, checkPos, buttonId, this::getTravel);
        switch (decision) {
            case TravelDecision.Ignored ignored -> { }
            case TravelDecision.Rejected rejected -> send(new SendMessage(rejected.getMessage()));
            case TravelDecision.RequireUnlockDialogue unlockDialogue -> {
                DialogueService.setDialogueId(this, unlockDialogue.getDialogueId());
                DialogueService.setDialogueSent(this, false);
            }
            case TravelDecision.Approved approved -> {
                varbit(153, approved.getVarbitValue());
                travelInitiate = true;
                Position destination = approved.getDestination();
                GameEventScheduler.runLaterMs(1800, () -> {
                    if (!disconnected) {
                        transport(destination);
                        send(new RemoveInterfaces());
                        travelInitiate = false;
                    }
                });
            }
            default -> { }
        }
    }

    public int refundSlot = -1;
    public ArrayList<PartyRoomRewardItem> rewardList = new ArrayList<>();
    private final ArrayList<String> refundDates = new ArrayList<>();

    /** Runtime-only refund row keys used by ItemReclaimService. */
    public ArrayList<String> getRefundDates() {
        return refundDates;
    }

    public int totalLevel() {
        int total = 0;
        for (Skill skill : Skill.VALUES) {
            if (skill.isEnabled()) {
                total += Skills.getLevelForExperience(getExperience(skill));
            } else {
                total++;
            }
        }
        return total;
    }

    public boolean doingTeleport() {
        return getActiveActionType() == PlayerActionType.TELEPORT;
    }

    public boolean isWindowFocused() {
        return windowFocused;
    }
    
    /**
     * Sets whether the client window currently has focus.
     * 
     * @param focused true if the window has focus, false otherwise
     */
    public void setWindowFocused(boolean focused) {
        this.windowFocused = focused;

        if (getServerDebugMode()) {
           // println_debug("Window focus changed to: " + focused);
        }
    }

    public boolean isBusy() {
        return getInTrade() || getInDuel() || getDuelFight() || IsBanking;
    }
}
