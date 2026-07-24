package net.dodian.uber.game.model.entity.player;
import net.dodian.uber.game.api.content.ContentInteraction;
import net.dodian.uber.game.api.content.ContentActions;
import net.dodian.uber.game.combat.AncientSpellRegistry;
import net.dodian.uber.game.engine.config.DotEnvKt;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.engine.event.GameEventBus;
import net.dodian.uber.game.engine.systems.follow.FollowRouting;
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
import net.dodian.uber.game.skill.Skillcape;
import net.dodian.uber.game.ui.bank.PlayerBankService;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.ui.QuestTabEntry;
import net.dodian.uber.game.model.player.skills.Skill;
import net.dodian.uber.game.model.player.skills.Skills;
import net.dodian.uber.game.skill.prayer.PrayerManager;
import net.dodian.uber.game.skill.farming.Farming;
import net.dodian.uber.game.skill.farming.FarmingState;
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry;
import net.dodian.uber.game.persistence.admin.CommandDbService;
import net.dodian.uber.game.persistence.account.AccountPersistenceService;
import net.dodian.uber.game.persistence.player.RefundRecord;
import net.dodian.uber.game.persistence.player.RefundRepository;
import net.dodian.uber.game.persistence.player.PlayerSaveReason;
import net.dodian.uber.game.persistence.player.PlayerSaveSegment;
import net.dodian.uber.game.engine.net.InboundPacketMailbox;
import net.dodian.uber.game.engine.net.OutboundSessionQueue;
import net.dodian.uber.game.engine.processing.EntityProcessor;
import net.dodian.uber.game.engine.metrics.PacketErrorTelemetry;
import net.dodian.uber.game.engine.metrics.PacketRejectTelemetry;
import net.dodian.uber.game.skill.crafting.Tanning;
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
import net.dodian.uber.game.model.item.transaction.BankTransactions;
import net.dodian.uber.game.skill.agility.AgilityTicketExchangeService;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
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
    public long lastDropTime = 0; //used for limiting drops per 600ms and logging out delayd 
    public boolean isLoggingOut = false; //new flag 
    long lastBar = 0;
    public long lastSave, lastProgressSave;
    public Entity target = null;
    int otherdbId = -1;
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
    private long lastEffectsPeriodicDirtyAtMs = 0L;
    private int lastPlayerUpdateCapacity = 8192;
    private int lastNpcUpdateCapacity = 16384;
    /**
     * Tracks if the client window currently has focus.
     */
    private boolean windowFocused = true;
    public String failer = "";
    public Date now = new Date();
    public Date today = checkCalendarDate(now, 0);
    public long mutedTill;
    public long rightNow = now.getTime();
    public int resourcesGathered = 0;
    public long lastDoor = 0;
    public long session_start = 0;
    public boolean pickupWanted = false, duelWin = false;
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
    public String[] duelNames = {"No Ranged", "No Melee", "No Magic", "No Sp. Atk", "Fun Weapons", "No Forfeit",
            "No Drinks", "No Food", "No Prayer", "No Movement", "Obstacles"};
    public boolean[] duelRule = {false, false, false, false, false, false, false, false, false, false, false};

    /*
     * Danno: Testing for armor restriction rules.
     */
    private final boolean[] duelBodyRules = new boolean[11];

    private final int[] trueSlots = {0, 1, 2, 13, 3, 4, 5, 7, 12, 10, 9};
    private final int[] falseSlots = {0, 1, 2, 4, 5, 6, -1, 7, -1, 10, 9, -1, 9, 3};
    private final int[] stakeConfigId = new int[11];
    private final int[] duelRuleConfigIds = {631, 632, 633, 634, 635, 636, 638, 639, 637, 640, 641};
    public int[] duelLine = {31045, 31046, 31047, 31048, 31049, 31050, 31052, 31053, 31051, 31054, 31055};
    public boolean duelRequested = false, inDuel = false, duelConfirmed = false, duelConfirmed2 = false,
            duelFight = false;
    public long lastDuelItemChangeMs = 0L;
    public int duel_with = 0;
    public int duelApproachPendingSlot = -1;
    public int duelApproachTicks = 0;
    /** Target coordinates the currently-staged duel approach route was computed toward. */
    public int duelApproachTargetX = -1;
    public int duelApproachTargetY = -1;
    public boolean tradeRequested = false, inTrade = false, canOffer = true, tradeConfirmed = false,
            tradeConfirmed2 = false, tradeResetNeeded = false;
    public int trade_reqId = 0;
    /** Game-thread-owned identity and revisions for the active trade session. */
    public long tradeSessionId = 0L;
    public long tradeOfferRevision = 0L;
    public long tradeConfirmedOwnRevision = -1L;
    public long tradeConfirmedPartnerRevision = -1L;
    public long tradeSettledSessionId = -1L;
    public int tradeApproachPendingSlot = -1;
    public int tradeApproachTicks = 0;
    /** Target coordinates the currently-staged trade approach route was computed toward. */
    public int tradeApproachTargetX = -1;
    public int tradeApproachTargetY = -1;
    public CopyOnWriteArrayList<GameItem> offeredItems = new CopyOnWriteArrayList<>();
    public CopyOnWriteArrayList<GameItem> otherOfferedItems = new CopyOnWriteArrayList<>();
    public CopyOnWriteArrayList<GameItem> priceCheckerItems = new CopyOnWriteArrayList<>();
    public boolean priceCheckerOpen = false;
    public int priceCheckerMode = 0; // 0 = Value, 1 = High Alch
    public boolean priceCheckerSearchPendingInput = false;
    public GameItem priceCheckerSearchedItem = null;
    public int acceptAid = 0; // 0 = off, 1 = on
    public int rightClickToggle = 0; // 0 = off, 1 = on
    public int scrollCameraToggle = 0; // 0 = off, 1 = on
    public int entityAttackOption = 0; // 0 = Depends on combat levels, 1 = Always right-click, 2 = Left click where available, 3 = Hidden
    public boolean adding = false;
    public ArrayList<WorldObject> objects = new ArrayList<>();
    public long lastButton = 0;
    public int enterAmountId = 0;
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

    public int[] requiredLevel = AncientSpellRegistry.requiredLevel();
    public int[] baseDamage = AncientSpellRegistry.baseDamage();
    public String[] spellName = AncientSpellRegistry.spellName();
    public int[] ancientId = AncientSpellRegistry.ancientId();
    public int[] coolDown = AncientSpellRegistry.coolDown();
    public int[] ancientButton = AncientSpellRegistry.ancientButton();
    public String properName = "";
    public int actionButtonId = 0;
    public int skillX = 0, skillY = 0;
    public int stairs = 0, stairDistance = 0;
    public boolean validLogin = false;

    public void openTan() {
        Tanning.open(this);
    }

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

    /**
     * Refreshes a skill's level and experience on the client.
     * @param skill The skill to refresh
     */
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
        if (usingBow && fightType == 2) //Rapid style
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
                if (person.distanceToPoint(pos.getX(), pos.getY()) <= 60 && pos.getZ() == getPosition().getZ())
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
                    if (person.distanceToPoint(pos.getX(), pos.getY()) <= 60 && getPosition().getZ() == pos.getZ())
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
        for (int j : QuestInterface)
            send(new SendString("", j));
    }

    public void openInterface(int interfaceid) {
        interfaceManager.open(interfaceid);
    }

    public void closeInterfaces() {
        interfaceManager.close();
    }

    public int ancients = 1;
    public int[] QuestInterface = {8145, 8147, 8148, 8149, 8150, 8151, 8152, 8153, 8154, 8155, 8156, 8157, 8158, 8159,
            8160, 8161, 8162, 8163, 8164, 8165, 8166, 8167, 8168, 8169, 8170, 8171, 8172, 8173, 8174, 8175, 8176, 8177, 8178,
            8179, 8180, 8181, 8182, 8183, 8184, 8185, 8186, 8187, 8188, 8189, 8190, 8191, 8192, 8193, 8194, 8195, 12174,
            12175, 12176, 12177, 12178, 12179, 12180, 12181, 12182, 12183, 12184, 12185, 12186, 12187, 12188, 12189, 12190,
            12191, 12192, 12193, 12194, 12195, 12196, 12197, 12198, 12199, 12200, 12201, 12202, 12203, 12204, 12205, 12206,
            12207, 12208, 12209, 12210, 12211, 12212, 12213, 12214, 12215, 12216, 12217, 12218, 12219, 12220, 12221, 12222,
            12223};

    public int[] statId = {2812, 2816, 2813, 2817, 2814, 2818, 2815, 2827, 2829, 2830, 2826, 2828, 2822,
            2825, 2824, 2820, 2819, 2821, 47002, 54090, 2823};
    public String[] BonusName = {"Stab", "Slash", "Crush", "Magic", "Range", "Stab", "Slash", "Crush", "Magic", "Range",
            "Melee Str", "Ranged Str", "Magic Dmg", "Prayer"};

    public int i;
    public int XremoveSlot = 0;
    public int XinterfaceID = 0;
    public int XremoveID = 0;
    public int currentBankTab = 0;
    public int previousBankTab = 0;
    public int lastButtonActionIndex = -1;
    public boolean bankSearchActive = false;
    public boolean bankSearchPendingInput = false;
    public boolean modcpSearchPendingInput = false;
    public boolean addFriendPendingInput = false;
    public boolean removeFriendPendingInput = false;
    public boolean addIgnorePendingInput = false;
    public boolean removeIgnorePendingInput = false;
    public int modcpDialogState = 0;
    public int accountServicesDialogState = 0;
    public boolean viewingAccountServices = false;
    public int accountPasswordState = 0;
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

    private long verticalTransitionSequence = 0L;
    private long activeVerticalTransitionToken = 0L;
    private long verticalTransitionUntilMillis = 0L;
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
        if (priceCheckerOpen) {
            closePriceChecker();
        }
        PlayerDeferredLifecycleService.cancelAll(this);
        PlayerScopedCoroutineService.cancelForPlayer(this, "Player destruct");
        clearVerticalTravelState();
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

        if (saveNeeded && !tradeSuccessful) {
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

        if (openedInterfaceId != null) {
            activeInterfaceId = openedInterfaceId;
        }

        if (closedVia != null && previousInterfaceId != -1) {
            ConsoleAuditLog.interfaceClose(this, previousInterfaceId, closedVia);
        }
        if (openedInterfaceId != null && openedInterfaceId != previousInterfaceId) {
            ConsoleAuditLog.interfaceOpen(this, openedInterfaceId, openedVia);
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
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastDropTime < 600) {
            send(new SendMessage("You cannot log out so soon after dropping an item."));
            return;
        }
        isLoggingOut = true;
        ContentActions.cancel(this, PlayerActionCancelReason.LOGOUT, false, false, false, true);
        if (!saveNeeded || !validClient || UsingAgility) {
            if (UsingAgility) {
                xLog = true; // Existing logic for agility delay
                PlayerDeferredLifecycleService.scheduleXLogExpiry(this, walkBlock);
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
            if (duel_with > 0 && validClient(duel_with) && inDuel && duelFight) {
                Client p = getClient(duel_with);
                p.duelWin = true;
                p.DuelVictory();
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

    public void fromBank(int itemID, int fromSlot, int amount) {
        if (!IsBanking) {
            send(new RemoveInterfaces());
            return;
        }
        if (fromSlot < 0 || fromSlot >= bankItems.length || bankItems[fromSlot] - 1 != itemID) {
            return;
        }
        if (bankItemsN[fromSlot] == 0) {
            PlayerBankService.releasePlaceholder(this, fromSlot);
            return;
        }
        int id = getNotedItem(itemID);
        if (amount == -2) { //draw all from bank!
            if (!takeAsNote && !Server.itemManager.isStackable(itemID))
                amount = freeSlots() == 0 ? 1 : Math.min(bankItemsN[fromSlot], freeSlots());
            else
                amount = freeSlots() == 0 && !playerHasItem(id == 0 ? itemID : id) ? 1 : bankItemsN[fromSlot];
        }
        if (amount <= 0) {
            return;
        }
        int receivedItemId = takeAsNote && id > 0 ? id : itemID;
        if (takeAsNote && id <= 0) {
            send(new SendMessage(Server.itemManager.getName(itemID) + " can't be drawn as note."));
        }
        amount = Math.min(amount, bankItemsN[fromSlot]);
        if (!BankTransactions.withdraw(this, itemID, fromSlot, amount, receivedItemId, bankPlaceholdersEnabled)) {
            send(new SendMessage("Not enough space in your inventory."));
        }
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

    public void bankItem(int itemID, int fromSlot, int amount) {
        if (bankItemTransactional(itemID, fromSlot, amount)) {
            return;
        }
        if (playerItemsN[fromSlot] <= 0 || playerItems[fromSlot] <= 0 || playerItems[fromSlot] - 1 != itemID) {
            return;
        }
        if (!IsBanking) {
            send(new RemoveInterfaces());
            return;
        }
        boolean bankChanged = false;
        ensureBankTabState();
        int id = getUnnotedItem(itemID);
        int size = bankSize();
        if (id == 0) {
            if (playerItems[fromSlot] <= 0) {
                return;
            }
            amount = Math.min(amount, getInvAmt(itemID));
            if (Server.itemManager.isStackable(itemID) || playerItemsN[fromSlot] > 1) {
                int toBankSlot = 0;
                boolean alreadyInBank = false;
                for (int i = 0; i < size; i++) {
                    if (bankItems[i] - 1 == itemID) { //Bank starts at value 0 while items should start at -1!
                        if (playerItemsN[fromSlot] < amount) {
                            amount = playerItemsN[fromSlot];
                        }
                        alreadyInBank = true;
                        toBankSlot = i;
                        break;
                    }
                }

                if (!alreadyInBank && freeBankSlots() > 0) {
                    for (int i = 0; i < size; i++) {
                        if (bankItems[i] <= 0) {
                            toBankSlot = i;
                            break;
                        }
                    }
                    bankItems[toBankSlot] = itemID + 1; //To continue on comment above..Dodian thing :D
                    bankSlotTabs[toBankSlot] = currentBankTab > 0 && currentBankTab < 10 && !bankSearchActive ? currentBankTab : 0;
                    bankChanged = true;
                    if (playerItemsN[fromSlot] < amount) {
                        amount = playerItemsN[fromSlot];
                    }
                    if ((bankItemsN[toBankSlot] + amount) <= maxItemAmount && (bankItemsN[toBankSlot] + amount) > -1) {
                        bankItemsN[toBankSlot] += amount;
                        bankChanged = true;
                    } else {
                        send(new SendMessage("Bank full!"));
                        return;
                    }
                    deleteItem(itemID, fromSlot, amount);
                } else if (alreadyInBank) {
                    if ((bankItemsN[toBankSlot] + amount) <= maxItemAmount && (bankItemsN[toBankSlot] + amount) > -1) {
                        bankItemsN[toBankSlot] += amount;
                        bankChanged = true;
                    } else {
                        send(new SendMessage("Bank full!"));
                        return;
                    }
                    deleteItem(itemID, fromSlot, amount);
                } else {
                    send(new SendMessage("Bank full!"));
                }
            } else {
                itemID = playerItems[fromSlot];
                int toBankSlot = 0;
                boolean alreadyInBank = false;

                for (int i = 0; i < size; i++) {
                    if (bankItems[i] == playerItems[fromSlot]) {
                        alreadyInBank = true;
                        toBankSlot = i;
                        break;
                    }
                }
                if (!alreadyInBank && freeBankSlots() > 0) {
                    for (int i = 0; i < size; i++) {
                        if (bankItems[i] <= 0) {
                            toBankSlot = i;
                            break;
                        }
                    }
                    int firstPossibleSlot = 0;
                    boolean itemExists = false;

                    while (amount > 0) {
                        for (int i = firstPossibleSlot; i < playerItems.length; i++) {
                            if ((playerItems[i]) == itemID) {
                                firstPossibleSlot = i;
                                itemExists = true;
                                break;
                            }
                        }
                        if (itemExists) {
                            bankItems[toBankSlot] = playerItems[firstPossibleSlot];
                            bankSlotTabs[toBankSlot] = currentBankTab > 0 && currentBankTab < 10 && !bankSearchActive ? currentBankTab : 0;
                            bankItemsN[toBankSlot] += 1;
                            bankChanged = true;
                            deleteItem((playerItems[firstPossibleSlot] - 1), firstPossibleSlot, 1);
                            amount--;
                        } else {
                            amount = 0;
                        }
                    }
                } else if (alreadyInBank) {
                    int firstPossibleSlot = 0;
                    boolean itemExists = false;

                    while (amount > 0) {
                        for (int i = firstPossibleSlot; i < playerItems.length; i++) {
                            if ((playerItems[i]) == itemID) {
                                firstPossibleSlot = i;
                                itemExists = true;
                                break;
                            }
                        }
                        if (itemExists) {
                            bankItemsN[toBankSlot] += 1;
                            bankChanged = true;
                            deleteItem((playerItems[firstPossibleSlot] - 1), firstPossibleSlot, 1);
                            amount--;
                        } else {
                            amount = 0;
                        }
                    }
                } else {
                    send(new SendMessage("Bank full!"));
                }
            }
        } else if (id > 0) {
            if (playerItems[fromSlot] <= 0) {
                return;
            }
            amount = Math.min(amount, getInvAmt(itemID));
            if (Server.itemManager.isStackable(playerItems[fromSlot] - 1) || playerItemsN[fromSlot] > 1) {
                int toBankSlot = 0;
                boolean alreadyInBank = false;
                for (int i = 0; i < size; i++) {
                    if (bankItems[i] == getUnnotedItem(playerItems[fromSlot] - 1) + 1) {
                        if (playerItemsN[fromSlot] < amount) {
                            amount = playerItemsN[fromSlot];
                        }
                        alreadyInBank = true;
                        toBankSlot = i;
                        break;
                    }
                }
                if (!alreadyInBank && freeBankSlots() > 0) {
                    for (int i = 0; i < size; i++) {
                        if (bankItems[i] <= 0) {
                            toBankSlot = i;
                            break;
                        }
                    }
                    bankItems[toBankSlot] = id + 1;
                    bankSlotTabs[toBankSlot] = currentBankTab > 0 && currentBankTab < 10 && !bankSearchActive ? currentBankTab : 0;
                    bankChanged = true;
                    if (playerItemsN[fromSlot] < amount) {
                        amount = playerItemsN[fromSlot];
                    }
                    if ((bankItemsN[toBankSlot] + amount) <= maxItemAmount && (bankItemsN[toBankSlot] + amount) > -1) {
                        bankItemsN[toBankSlot] += amount;
                        bankChanged = true;
                    } else {
                        return;
                    }
                    deleteItem((playerItems[fromSlot] - 1), fromSlot, amount);
                } else if (alreadyInBank) {
                    if ((bankItemsN[toBankSlot] + amount) <= maxItemAmount && (bankItemsN[toBankSlot] + amount) > -1) {
                        bankItemsN[toBankSlot] += amount;
                        bankChanged = true;
                    } else {
                        return;
                    }
                    deleteItem((playerItems[fromSlot] - 1), fromSlot, amount);
                } else {
                    send(new SendMessage("Bank full!"));
                }
            } else {
                itemID = playerItems[fromSlot];
                int toBankSlot = 0;
                boolean alreadyInBank = false;

                for (int i = 0; i < size; i++) {
                    if (bankItems[i] == (playerItems[fromSlot] - 1)) {
                        alreadyInBank = true;
                        toBankSlot = i;
                        break;
                    }
                }
                if (!alreadyInBank && freeBankSlots() > 0) {
                    for (int i = 0; i < size; i++) {
                        if (bankItems[i] <= 0) {
                            toBankSlot = i;
                            break;
                        }
                    }
                    int firstPossibleSlot = 0;
                    boolean itemExists = false;

                    while (amount > 0) {
                        for (int i = firstPossibleSlot; i < playerItems.length; i++) {
                            if ((playerItems[i]) == itemID) {
                                firstPossibleSlot = i;
                                itemExists = true;
                                break;
                            }
                        }
                        if (itemExists) {
                            bankItems[toBankSlot] = (playerItems[firstPossibleSlot] - 1);
                            bankSlotTabs[toBankSlot] = currentBankTab > 0 && currentBankTab < 10 && !bankSearchActive ? currentBankTab : 0;
                            bankItemsN[toBankSlot] += 1;
                            bankChanged = true;
                            deleteItem((playerItems[firstPossibleSlot] - 1), firstPossibleSlot, 1);
                            amount--;
                        } else {
                            amount = 0;
                        }
                    }
                } else if (alreadyInBank) {
                    int firstPossibleSlot = 0;
                    boolean itemExists = false;

                    while (amount > 0) {
                        for (int i = firstPossibleSlot; i < playerItems.length; i++) {
                            if ((playerItems[i]) == itemID) {
                                firstPossibleSlot = i;
                                itemExists = true;
                                break;
                            }
                        }
                        if (itemExists) {
                            bankItemsN[toBankSlot] += 1;
                            bankChanged = true;
                            deleteItem((playerItems[firstPossibleSlot] - 1), firstPossibleSlot, 1);
                            amount--;
                        } else {
                            amount = 0;
                        }
                    }
                } else {
                    send(new SendMessage("Bank full!"));
                }
            }
        } else {
            send(new SendMessage("Item not supported " + itemID));
        }
        if (bankChanged) {
            markSaveDirty(PlayerSaveSegment.BANK.getMask());
        }
    }

    /**
     * Compatibility entry point for the legacy bank packet.  It intentionally
     * returns true for rejected requests too, so runtime bank mutations never
     * fall through to the old piecemeal implementation below.
     */
    private boolean bankItemTransactional(int itemID, int fromSlot, int amount) {
        if (!IsBanking || fromSlot < 0 || fromSlot >= playerItems.length
                || playerItems[fromSlot] != itemID + 1 || playerItemsN[fromSlot] <= 0) {
            return true;
        }
        ensureBankTabState();
        int unnoted = getUnnotedItem(itemID);
        int bankItemId = unnoted > 0 ? unnoted : itemID;
        int available = Server.itemManager.isStackable(itemID) ? playerItemsN[fromSlot] : getInvAmt(itemID);
        int requested = Math.min(Math.max(amount, 0), available);
        if (requested == 0) {
            return true;
        }
        boolean newBankEntry = getBankSlot(bankItemId) < 0;
        if (!BankTransactions.deposit(this, itemID, fromSlot, requested, bankItemId)) {
            send(new SendMessage("Bank full!"));
            return true;
        }
        if (newBankEntry) {
            int bankSlot = getBankSlot(bankItemId);
            if (bankSlot >= 0) {
                bankSlotTabs[bankSlot] = currentBankTab > 0 && currentBankTab < 10 && !bankSearchActive ? currentBankTab : 0;
            }
        }
        return true;
    }

    public void resetItems(int WriteFrame) {
        send(new ResetItems(WriteFrame));
    }

    public void sendInventory(int interfaceId, ArrayList<GameItem> inv) {
        send(new SendInventory(interfaceId, inv));
    }



    public void resetOTItems(int WriteFrame) {
        Client other = getClient(trade_reqId);
        if (!validClient(trade_reqId)) {
            return;
        }
        send(new TradeItemsUpdate(WriteFrame, other.offeredItems));
    }

    public void resetTItems(int WriteFrame) {
        send(new TradeItemsUpdate(WriteFrame, offeredItems));
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
            pickupWanted = false;
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
            pickupWanted = false;
            PlayerDeferredLifecycleService.cancelGroundPickupArrivalWatch(this);
            return;
        }

        if (!hasSpace() && Server.itemManager.isStackable(target.id) && !playerHasItem(target.id)) {
            send(new SendMessage("Your inventory is full!"));
            attemptGround = null;
            pickupWanted = false;
            PlayerDeferredLifecycleService.cancelGroundPickupArrivalWatch(this);
            return;
        }

        if (premiumItem(target.id) && !premium) {
            send(new SendMessage("You must be a premium member to use this item"));
            attemptGround = null;
            pickupWanted = false;
            PlayerDeferredLifecycleService.cancelGroundPickupArrivalWatch(this);
            return;
        }

        if (!Ground.tryClaimPickup(this, target)) {
            attemptGround = null;
            pickupWanted = false;
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
        pickupWanted = false;
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
        ItemLog.playerDrop(this, id, amount, getPosition().copy(), "Inventory Drop");
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
            logger.info(format, args);
        }
    }

    public void wear(int wearID, int slot, int interFace) {
                    wearLog("[WEAR:WEAR] wearID={} slot={} interface={}", wearID, slot, interFace);
        if (isBusy() || interFace != 3214) {
                            wearLog("[WEAR:WEAR] rejected busy={} interface={}", isBusy(), interFace);
            return;
        }
        if (net.dodian.uber.game.skill.runecrafting.Runecrafting.emptyPouch(this, wearID)) { //Runecrafting Pouches
            return;
        }
        if (wearID == 5733) { //Potato
            wipeInv();
            return;
        }
        if (wearID == 6583 || wearID == 7927) {
            if (System.currentTimeMillis() < walkBlock) { //Not usable during a walkBlock!
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
        if (duelConfirmed && !duelFight) {
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
        if (targetSlot != 8 && duelBodyRules[falseSlots[targetSlot]]) {
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
        if (Skills.getLevelForExperience(getExperience(Skill.AGILITY)) < 60 && id == 4224) {
            send(new SendMessage("You need 60 Agility to equip " + itemName.toLowerCase() + "."));
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
        if (duelConfirmed && !force) {
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

    public boolean canAttack = true;

    public long getLastEffectsPeriodicDirtyAtMs() {
        return lastEffectsPeriodicDirtyAtMs;
    }

    public void setLastEffectsPeriodicDirtyAtMs(long atMillis) {
        lastEffectsPeriodicDirtyAtMs = atMillis;
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

    public int currentSkill = -1;

    public static void publicyell(String message) {
        for (Player p : PlayerRegistry.players) {
            if (p == null || !p.isActive) {
                continue;
            }
            Client temp = (Client) p;
            if (temp.getPosition().getX() > 0 && temp.getPosition().getY() > 0) {
                if (!temp.disconnected && p.isActive) {
                    temp.send(new SendMessage(message));
                }
            }
        }
    }

    public void yell(String message) {
        for (Player p : PlayerRegistry.players) {
            if (p == null || !p.isActive)
                continue;
            Client temp = (Client) p;
            temp.send(new SendMessage(message));
        }
    }

    public void yellKilled(String message) {
        for (Player p : PlayerRegistry.players) {
            if (p == null || !p.isActive || !(p.inWildy() || p.inEdgeville()))
                continue;
            Client temp = (Client) p;
            temp.send(new SendMessage(message));
        }
    }

    public void yellAreaKilled(String message, String area) {
        for (Player p : PlayerRegistry.players) {
            if (p == null || !p.isActive || !p.getPositionName().contains(area))
                continue;
            Client temp = (Client) p;
            temp.send(new SendMessage("<col=FFFF00>[Area]<col=000000> " + message));
        }
    }

    public long beginVerticalTransition(long delayMs) {
        resetWalkingQueue();
        long now = System.currentTimeMillis();
        activeVerticalTransitionToken = ++verticalTransitionSequence;
        verticalTransitionUntilMillis = now + Math.max(delayMs, 0L);
        walkBlock = Math.max(walkBlock, verticalTransitionUntilMillis);
        return activeVerticalTransitionToken;
    }

    public boolean isVerticalTransitionActive() {
        return activeVerticalTransitionToken != 0L && verticalTransitionUntilMillis > System.currentTimeMillis();
    }

    public void clearVerticalTransition() {
        activeVerticalTransitionToken = 0L;
        verticalTransitionUntilMillis = 0L;
    }

    public void clearVerticalTravelState() {
        clearVerticalTransition();
    }

    public String verticalTransitionDebugSummary() {
        return "token=" + activeVerticalTransitionToken +
                ",until=" + verticalTransitionUntilMillis +
                ",tele=(" + teleportToX + "," + teleportToY + "," + teleportToZ + ")" +
                ",pos=" + getPosition();
    }

    public void queueTransport(Position pos) {
        resetActionTeleport();
        teleportToX = pos.getX();
        teleportToY = pos.getY();
        teleportToZ = pos.getZ();
    }

    public void finishVerticalTransition(long token, Position destination) {
        if (activeVerticalTransitionToken != token || disconnected) {
            return;
        }
        queueTransport(destination);
        clearVerticalTransition();
    }

    public boolean usingBow = false;

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

    public boolean randomed2;
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
        itemID++;
        for (int playerItem : playerItems) {
            if (playerItem == itemID) {
                return true;
            }
        }
        for (int i = 0; i < getEquipment().length; i++) {
            if (getEquipment()[i] == itemID) {
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
                if (!(duelFight && i == 8)) {
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
            if (other == null) {
                sendMessage("Player is offline.");
                return;
            }
            if (option == 1) { // Check Bank
                openUpOtherBank(other.playerName);
            } else if (option == 2) { // Check Inventory
                openUpOtherInventory(other.playerName);
            }
            modcpDialogState = 0;
            send(new net.dodian.uber.game.netty.listener.out.RemoveInterfaces());
        }
    }

    public void openAccountServices() {
        net.dodian.uber.game.ui.AccountServices.open(this);
    }

    public void handleAccountServicesRowClick(int index) {
        net.dodian.uber.game.ui.AccountServices.handleRowClick(this, index);
    }

    public void verifyCurrentPassword(String inputPass) {
        net.dodian.uber.game.ui.AccountServices.verifyCurrentPassword(this, inputPass);
    }

    public void handleAccountServicesDialogue(int option) {
        net.dodian.uber.game.ui.AccountServices.handleAccountServicesDialogue(this, option);
    }

    public void showAccountInfo() {
        net.dodian.uber.game.ui.AccountServices.showDefaultStatus(this);
    }

    public void changePassword(String newPassword) {
        net.dodian.uber.game.ui.AccountServices.changePassword(this, newPassword);
    }


    public int neglectDmg() {
        int bonus = 0;
        if (getEquipment()[Equipment.Slot.SHIELD.getId()] == 11284)
            bonus += ((getLevel(Skill.FIREMAKING) + 1) / 5) * 10;
        return Math.min(1000, playerBonus[13] + bonus);
    }

    public double magicDmg() {
        return 1.0 + (playerBonus[12] / 100.0);
    }

    public void updateBonus(int id) {
        String send;
        if (id >= 0 && id < 14) {
            send = BonusName[id] + ": " + (playerBonus[id] >= 0 ? "+" + playerBonus[id] : playerBonus[id]);
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

    public void fromTrade(int itemID, int fromSlot, int amount) {
        if (!ContentInteraction.tryAcquireMs(this, ContentInteraction.TRADE_CONFIRM_STAGE_ONE, 200L) || !canOffer) {
            if(!canOffer)  declineTrade(); //Not sure if we need this here but..Maybe?!
            return;
        }
        try {
            Client other = getClient(trade_reqId);
            if (!inTrade || !validClient(trade_reqId)) {
                declineTrade();
                return;
            }
            if (checkGameitemAmount(fromSlot, amount, offeredItems) || offeredItems.get(fromSlot).getId() != itemID) {
                return;
            }
            int count = 0;
            if (!Server.itemManager.isStackable(itemID)) {
                for (GameItem item : offeredItems) {
                    if (item.getId() == itemID) {
                        count++;
                    }
                }
            } else
                count = offeredItems.get(fromSlot).getAmount();
            amount = Math.min(amount, count);
            if (!OfferTransactions.offerToInventory(this, itemID, fromSlot, amount)) {
                return;
            }
            TradeDuelSessionService.offerChanged(this, other);
            resetItems(3322);
            resetTItems(3415);
            other.resetOTItems(3416);
            send(new SendString("", 3431));
            other.send(new SendString("", 3431));
        } catch (Exception e) {
            logger.warn("Error with trade for {}", getPlayerName(), e);
        }
    }

    public void tradeItem(int itemID, int fromSlot, int amount) {
        if (!ContentInteraction.tryAcquireMs(this, ContentInteraction.TRADE_CONFIRM_STAGE_TWO, 200L)) {
            return;
        }
        if (!Server.itemManager.isStackable(itemID))
            amount = Math.min(amount, getInvAmt(itemID));
        else
            amount = Math.min(amount, playerItemsN[fromSlot]);
        Client other = getClient(trade_reqId);
        if (!inTrade || !validClient(trade_reqId) || !canOffer) {
            
            declineTrade();
            return;
        }
        if (!playerHasItem(itemID, amount) || playerItems[fromSlot] != (itemID + 1)) {
            return;
        }
        if (!Server.itemManager.isTradable(itemID) && playerRights < 2 && other.playerRights < 2) {
            send(new SendMessage("You can't trade this item"));
            return;
        }
        if (itemID == 7927 && new Date().before(new Date("06/1/2024")) && other.checkItem(7927)) {
            send(new SendMessage(other.getPlayerName() + " already have the ring. Wait until after May!"));
            return;
        }
        if (!OfferTransactions.stakeToOffer(this, itemID, fromSlot, amount)) {
            return;
        }
        TradeDuelSessionService.offerChanged(this, other);
        resetItems(3322);
        resetTItems(3415);
        other.resetOTItems(3416);
        send(new SendString("", 3431));
        other.send(new SendString("", 3431));
    }

    public void openPriceChecker() {
        priceCheckerOpen = true;
        priceCheckerItems.clear();
        priceCheckerSearchedItem = null;
        priceCheckerSearchPendingInput = false;
        priceCheckerMode = 0; // default to value
        send(new InventoryInterface(48500, 5063));
        priceCheckerRefresh();
    }

    public void closePriceChecker() {
        if (!priceCheckerOpen) return;
        priceCheckerOpen = false;
        priceCheckerWithdrawAll();
        priceCheckerSearchedItem = null;
        priceCheckerSearchPendingInput = false;
        send(new RemoveInterfaces());
    }

    public void priceCheckerDeposit(int itemID, int fromSlot, int amount) {
        if (!priceCheckerOpen) return;
        if (fromSlot < 0 || fromSlot >= 28) return;
        if (itemID != (playerItems[fromSlot] - 1)) return;
        if (!playerHasItem(itemID, amount) || playerItems[fromSlot] != (itemID + 1)) return;
        if (!Server.itemManager.isTradable(itemID)) {
            send(new SendMessage("This item is untradeable!"));
            return;
        }
        if (!Server.itemManager.isStackable(itemID)) {
            amount = Math.min(amount, getInvAmt(itemID));
        } else {
            amount = Math.min(amount, playerItemsN[fromSlot]);
        }
        if (amount <= 0) return;

        // Check slots: max 28 slots
        if (priceCheckerItems.size() >= 28) {
            boolean found = false;
            for (GameItem item : priceCheckerItems) {
                if (item.getId() == itemID && item.isStackable()) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                send(new SendMessage("Your price checker is full!"));
                return;
            }
        }

        if (Server.itemManager.isStackable(itemID)) {
            boolean found = false;
            for (GameItem item : priceCheckerItems) {
                if (item.getId() == itemID) {
                    found = true;
                    item.addAmount(amount);
                    deleteItem(itemID, fromSlot, amount);
                    break;
                }
            }
            if (!found) {
                priceCheckerItems.add(new GameItem(itemID, amount));
                deleteItem(itemID, fromSlot, amount);
            }
        } else {
            for (int a = 1; a <= amount; a++) {
                if (priceCheckerItems.size() >= 28) {
                    send(new SendMessage("Your price checker is full!"));
                    break;
                }
                if (a == 1) {
                    priceCheckerItems.add(new GameItem(itemID, 1));
                    deleteItem(itemID, fromSlot, 1);
                } else {
                    int slot = findItem(itemID, playerItems, playerItemsN);
                    if (slot >= 0 && slot < 28) {
                        priceCheckerItems.add(new GameItem(itemID, 1));
                        deleteItem(itemID, slot, 1);
                    } else {
                        break;
                    }
                }
            }
        }
        priceCheckerRefresh();
    }

    public void priceCheckerWithdraw(int itemID, int fromSlot, int amount) {
        if (!priceCheckerOpen) return;
        if (fromSlot < 0 || fromSlot >= priceCheckerItems.size()) return;
        GameItem item = priceCheckerItems.get(fromSlot);
        if (item == null || item.getId() != itemID) return;

        int count = item.getAmount();
        amount = Math.min(amount, count);
        if (amount <= 0) return;

        // Check inventory space
        if (!item.isStackable() && getFreeSpace() < amount) {
            amount = getFreeSpace();
            if (amount <= 0) {
                send(new SendMessage("Not enough space in your inventory!"));
                return;
            }
        }

        if (item.isStackable()) {
            if (amount < item.getAmount()) {
                priceCheckerItems.set(fromSlot, new GameItem(item.getId(), item.getAmount() - amount));
            } else {
                priceCheckerItems.remove(fromSlot);
            }
            addItem(itemID, amount);
        } else {
            for (int a = 1; a <= amount; a++) {
                if (getFreeSpace() <= 0) {
                    send(new SendMessage("Not enough space in your inventory!"));
                    break;
                }
                for (int i = 0; i < priceCheckerItems.size(); i++) {
                    if (priceCheckerItems.get(i).getId() == itemID) {
                        priceCheckerItems.remove(i);
                        addItem(itemID, 1);
                        break;
                    }
                }
            }
        }
        priceCheckerRefresh();
    }

    public void priceCheckerWithdrawAll() {
        if (!priceCheckerOpen) return;
        ArrayList<GameItem> temp = new ArrayList<>(priceCheckerItems);
        priceCheckerItems.clear();
        for (GameItem item : temp) {
            addItem(item.getId(), item.getAmount());
        }
        priceCheckerRefresh();
    }

    public void priceCheckerDepositAll() {
        if (!priceCheckerOpen) return;
        for (int slot = 0; slot < 28; slot++) {
            int itemId = playerItems[slot] - 1;
            int amount = playerItemsN[slot];
            if (itemId >= 0 && amount > 0) {
                priceCheckerDeposit(itemId, slot, amount);
            }
        }
    }

    public void priceCheckerSetMode(int mode) {
        priceCheckerMode = mode;
        priceCheckerRefresh();
    }

    public void priceCheckerSearch(String name) {
        if (!priceCheckerOpen) return;
        int foundId = -1;
        String searchName = name.trim().toLowerCase().replace("'", "");
        for (int i = 0; i < 20000; i++) {
            String itemName = Server.itemManager.getName(i);
            if (itemName != null && !itemName.isEmpty() && !Server.itemManager.isNote(i)) {
                if (itemName.toLowerCase().replace("'", "").contains(searchName)) {
                    foundId = i;
                    break;
                }
            }
        }
        if (foundId == -1) {
            send(new SendMessage("No items found matching: " + name));
            priceCheckerSearchedItem = null;
        } else {
            priceCheckerSearchedItem = new GameItem(foundId, 1);
        }
        priceCheckerRefresh();
    }

    public int getPriceCheckerItemPrice(int itemId, int mode) {
        if (mode == 1) {
            return Server.itemManager.getAlchemy(itemId);
        } else {
            return Server.itemManager.getShopSellValue(itemId);
        }
    }

    public void priceCheckerRefresh() {
        if (!priceCheckerOpen) return;
        for (int i = 0; i < 28; i++) {
            String value = "";
            if (i < priceCheckerItems.size()) {
                GameItem item = priceCheckerItems.get(i);
                if (item != null && item.getId() >= 0) {
                    long price = getPriceCheckerItemPrice(item.getId(), priceCheckerMode);
                    int amount = item.getAmount();
                    if (item.isStackable()) {
                        value = String.format("%,d", amount) + " x " + String.format("%,d", price) + "\\n= " + String.format("%,d", price * amount);
                    } else {
                        value = String.format("%,d", price);
                    }
                }
            }
            send(new SendString(value, 48550 + i));
        }

        if (priceCheckerSearchedItem != null) {
            String itemName = Server.itemManager.getName(priceCheckerSearchedItem.getId());
            long price = getPriceCheckerItemPrice(priceCheckerSearchedItem.getId(), priceCheckerMode);
            send(new SendString("<col=ffb000>" + itemName + ":", 48582));
            send(new SendString(String.format("%,d", price), 48583));
            send(new SendItemOnInterfaceSlot(48581, priceCheckerSearchedItem.getId(), priceCheckerSearchedItem.getAmount(), 0));
        } else {
            send(new SendString("\\nSelect item to search", 48582));
            send(new SendString("", 48583));
            send(new ClearItemContainer(48581, 1));
        }

        long total = 0;
        for (GameItem item : priceCheckerItems) {
            if (item != null && item.getId() >= 0) {
                total += (long) getPriceCheckerItemPrice(item.getId(), priceCheckerMode) * item.getAmount();
            }
        }
        send(new SendString(String.format("%,d", total), 48513));

        send(new TradeItemsUpdate(48542, priceCheckerItems));
        send(new ResetItems(5064));
        send(new SetVarbit(237, priceCheckerMode));
    }

    public void setEntityAttackOption(int value) {
        this.entityAttackOption = value;
    }

    /* Shops */
    public void sellItem(int itemID, int fromSlot, int amount) {
        if (itemID != playerItems[fromSlot] && playerItemsN[fromSlot] < 1) {
            return;
        }
        /* Item Values */
        int original = itemID;
        itemID = getUnnotedItem(original) > 0 ? getUnnotedItem(original) : itemID;
        int price = ShopRulesService.sellPrice(itemID);
        /* Functions */
        if (!net.dodian.uber.game.engine.config.FeatureStateService.shopping.get() || tradeLocked) {
            send(new SendMessage(tradeLocked ? "You are trade locked!" : "Currently selling stuff to the store has been disabled!"));
            return;
        }
        if (price < 0 || !Server.itemManager.isTradable(itemID) || !ShopRulesService.canSellItemToShop(MyShopID, itemID)) {
            send(new SendMessage("You cannot sell " + getItemName(itemID).toLowerCase() + " in this store."));
            return;
        }
        int slot = -1;
        for (int i = 0; i < ShopManager.MaxShopItems; i++) {
            if (ShopManager.ShopItems[MyShopID][i] <= 0 && slot == -1)
                slot = i;
            else if (itemID == ShopManager.ShopItems[MyShopID][i] - 1) {
                slot = i;
                i = ShopManager.MaxShopItems; //Just to stop the loop!
            }
        }
        if (slot == -1) { //If we do not have a slot means the store is full!
            send(new SendMessage("Can't sell more items to the store!"));
            return;
        }
        /* Amount checks */
        int currency = ShopRulesService.currencyItemId(MyShopID);
        boolean stack = Server.itemManager.isStackable(original);
        amount = Math.min(amount, getInvAmt(original));
        amount = Math.min(Integer.MAX_VALUE - ShopManager.ShopItemsN[MyShopID][slot], amount);
        amount = Integer.MAX_VALUE - getInvAmt(currency) < amount * price ? (Integer.MAX_VALUE - getInvAmt(currency)) / price : amount;

        if (amount > 0) { // Code to check if there is any amount to sell!
            if (!stack) {
                for (int i = 0; i < amount; i++) {
                    deleteItem(original, 1);
                }
            } else {
                deleteItem(original, amount);
            }
            ShopManager.ShopItems[MyShopID][slot] = itemID + 1;
            ShopManager.ShopItemsN[MyShopID][slot] += amount;
            int totalPrice = amount * price;
            addItem(currency, totalPrice);
            ConsoleAuditLog.shopSell(this, MyShopID, slot, itemID, amount, currency, totalPrice);
        } else
            send(new SendMessage("Could not sell anything!"));
        /* Store update! */
        UpdatePlayerShop();
        checkItemUpdate();
    }

    public int eventShopValues(int slot) {
        int itemId = ShopManager.ShopItems[MyShopID][slot] - 1;
        return ShopRulesService.buyPrice(MyShopID, itemId, slot);
    }

    public void buyItem(int itemID, int fromSlot, int amount) {
        if (amount > 0 && itemID == (ShopManager.ShopItems[MyShopID][fromSlot] - 1)) {
            boolean stack = Server.itemManager.isStackable(itemID);
            amount = Math.min(ShopManager.ShopItemsN[MyShopID][fromSlot], amount);
            if (canUse(itemID)) {
                send(new SendMessage("You must be a premium member to buy this item"));
                send(new SendMessage("Visit Dodian.net to subscribe"));
                return;
            }
            if (!stack && freeSlots() < 1) {
                send(new SendMessage("Not enough space in your inventory."));
                return;
            }

            int currency = ShopRulesService.currencyItemId(MyShopID);
            int TotPrice2 = ShopRulesService.buyPrice(MyShopID, itemID, fromSlot);
            int coins = getInvAmt(currency);
            amount = amount * TotPrice2 > coins ? coins / TotPrice2 : amount;
            if (amount == 0) {
                send(new SendMessage("You don't have enough " + getItemName(currency).toLowerCase()));
                return;
            }
            int purchasedAmount = 0;
            if (!stack) {
                for (int i = amount; i > 0; i--) {
                    if (freeSlots() == 0) {
                        send(new SendMessage("Not enough space in your inventory."));
                        return;
                    }
                    if (addItem(itemID, 1)) {
                        deleteItem(currency, TotPrice2);
                        ShopManager.ShopItemsN[MyShopID][fromSlot] -= 1;
                        purchasedAmount += 1;
                        if ((fromSlot + 1) > ShopManager.ShopItemsStandard[MyShopID] && ShopManager.ShopItemsN[MyShopID][fromSlot] <= 0) {
                            ShopManager.resetAnItem(MyShopID, fromSlot);
                            break;
                        }
                    } else {
                        send(new SendMessage("Not enough space in your inventory."));
                        return;
                    }
                }
            } else {
                if (addItem(itemID, amount)) {
                    deleteItem(currency, TotPrice2 * amount);
                    ShopManager.ShopItemsN[MyShopID][fromSlot] -= amount;
                    purchasedAmount = amount;
                    if ((fromSlot + 1) > ShopManager.ShopItemsStandard[MyShopID] && ShopManager.ShopItemsN[MyShopID][fromSlot] <= 0) {
                        ShopManager.resetAnItem(MyShopID, fromSlot);
                    }
                } else
                    return;
            }
            if (purchasedAmount > 0) {
                ConsoleAuditLog.shopBuy(this, MyShopID, fromSlot, itemID, purchasedAmount, currency, TotPrice2 * purchasedAmount);
            }
            /* Store update! */
            UpdatePlayerShop();
            checkItemUpdate();
        }
    }

    public void UpdatePlayerShop() {
        for (int i = 1; i < DotEnvKt.getGameMaxPlayers(); i++) {
            if (net.dodian.uber.game.engine.systems.world.player.PlayerRegistry.players[i] != null) {
                if (net.dodian.uber.game.engine.systems.world.player.PlayerRegistry.players[i].isShopping() && net.dodian.uber.game.engine.systems.world.player.PlayerRegistry.players[i].MyShopID == MyShopID
                        && i != getSlot()) {
                    ((Client) net.dodian.uber.game.engine.systems.world.player.PlayerRegistry.players[i]).checkItemUpdate();
                }
            }
        }
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
        String ItemName2 = ItemName.replaceAll("Bronze", "");

        ItemName2 = ItemName2.replaceAll("Iron", "");
        ItemName2 = ItemName2.replaceAll("Steel", "");
        ItemName2 = ItemName2.replaceAll("Black", "");
        ItemName2 = ItemName2.replaceAll("Mithril", "");
        ItemName2 = ItemName2.replaceAll("Adamant", "");
        ItemName2 = ItemName2.replaceAll("Rune", "");
        ItemName2 = ItemName2.replaceAll("Granite", "");
        ItemName2 = ItemName2.replaceAll("Dragon", "");
        ItemName2 = ItemName2.replaceAll("Crystal", "");
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
        String ItemName2 = ItemName.replaceAll("Bronze", "");
        ItemName2 = ItemName2.replaceAll("Iron", "");
        ItemName2 = ItemName2.replaceAll("Steel", "");
        ItemName2 = ItemName2.replaceAll("Black", "");
        ItemName2 = ItemName2.replaceAll("Mithril", "");
        ItemName2 = ItemName2.replaceAll("Adamant", "");
        ItemName2 = ItemName2.replaceAll("Rune", "");
        ItemName2 = ItemName2.replaceAll("Granite", "");
        ItemName2 = ItemName2.replaceAll("Dragon", "");
        ItemName2 = ItemName2.replaceAll("Crystal", "");
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



    public void RefreshDuelRules() {
        for (int i = 0; i < duelRule.length; i++) {
            int configId = duelRuleConfigIds[i];
            boolean on = duelRule[i];
            if (i == 9) on = true;
            if (i == 3 || i == 4 || i == 5 || i == 10) on = false;
            String color = on ? "@red@" : ((i == 3 || i == 4 || i == 5 || i == 10) ? "@or2@" : "@gre@");
            send(new SendString(color + duelNames[i], duelLine[i]));
            setVarp(configId, on ? 1 : 0);
        }
        int bodyConfigValue = 0;
        for (int i = 0; i < duelBodyRules.length; i++) {
            if (duelBodyRules[i])
                bodyConfigValue += stakeConfigId[i];
        }
        varbit(286, bodyConfigValue);
    }

    public void DuelVictory() {
        Client other = getClient(duel_with);
        if (validClient(duel_with)) {
            send(new SendMessage("You have defeated " + other.getPlayerName() + "!"));
            send(new SendString(other.getPlayerName(), 31706));
            send(new SendString("" + other.determineCombatLevel(), 31707));
        }
        boolean stake = false;
        StringBuilder playerStake = new StringBuilder();
        for (GameItem item : offeredItems) {
            if (item.getId() > 0 && item.getAmount() > 0) {
                playerStake.append("(").append(item.getId()).append(", ").append(item.getAmount()).append(")");
                stake = true;
            }
        }
        StringBuilder opponentStake = new StringBuilder();
        for (GameItem item : otherOfferedItems) {
            if (item.getId() > 0 && item.getAmount() > 0) {
                opponentStake.append("(").append(item.getId()).append(", ").append(item.getAmount()).append(")");
                stake = true;
            }
        }
        resetAttack();

        if (stake) {
            DuelLog.recordDuel(this.getPlayerName(), other.getPlayerName(), playerStake.toString(), opponentStake.toString(), this.getPlayerName());
            itemsToVScreen_old();
            acceptDuelWon();
            other.resetDuel();
        } else {
            if (validClient(duel_with))
                other.resetDuel();
            resetDuel();
        }
        long totalValue = 0;
        for (GameItem item : otherOfferedItems) {
            if (item.getId() > 0 && item.getAmount() > 0) {
                totalValue += (long) Server.itemManager.getShopSellValue(item.getId()) * item.getAmount();
            }
        }
        send(new SendString("<col=E1981F>Total Value: " + String.format("%,d", totalValue), 31709));
        openInterface(31700);
        heal(getMaxHealth());
        getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);

    }

    public void itemsToVScreen_old() {
        send(new DuelItemsUpdate(31708, otherOfferedItems, false));
    }

    public void refreshDuelScreen() {
        Client other = getClient(duel_with);
        if (!validClient(duel_with)) {
            return;
        }
        send(new DuelItemsUpdate(31012, offeredItems, true));
        send(new DuelItemsUpdate(31014, other.offeredItems, true));
    }

    public void stakeItem(int itemID, int fromSlot, int amount) {
        if (!ContentInteraction.tryAcquireMs(this, ContentInteraction.DUEL_CONFIRM_STAGE_ONE, 200L) || !canOffer) {
            if(!canOffer) declineDuel(); //Not sure if we need this here but..Maybe?!
            return;
        }
        if (!Server.itemManager.isStackable(itemID))
            amount = Math.min(amount, getInvAmt(itemID));
        else
            amount = Math.min(amount, playerItemsN[fromSlot]);
        if (!Server.itemManager.isTradable(itemID)) {
            send(new SendMessage("You can't trade that item"));
            return;
        }
        Client other = getClient(duel_with);
        if (!inDuel || !validClient(duel_with)) {
            declineDuel();
            return;
        }
        if (!playerHasItem(itemID, amount) || playerItems[fromSlot] != (itemID + 1)) {
            return;
        }
        if (!Server.itemManager.isTradable(itemID)) {
            send(new SendMessage("You can't trade this item"));
            return;
        }
        if (itemID == 7927 && new Date().before(new Date("06/1/2024")) && other.checkItem(7927)) {
            send(new SendMessage(other.getPlayerName() + " already have the ring. Wait until after May!"));
            return;
        }
        if (!OfferTransactions.stakeToOffer(this, itemID, fromSlot, amount)) {
            return;
        }
        duelConfirmed = false;
        other.duelConfirmed = false;
        long timeNow = System.currentTimeMillis();
        lastDuelItemChangeMs = timeNow;
        other.lastDuelItemChangeMs = timeNow;
        resetItems(3214);
        resetItems(3322);
        other.resetItems(3214);
        other.resetItems(3322);
        refreshDuelScreen();
        other.refreshDuelScreen();
        send(new SendString("", 6684));
        other.send(new SendString("", 6684));
    }

    public boolean checkGameitemAmount(int slot, int amount, CopyOnWriteArrayList<GameItem> item) {
        int count = 0;
        if (item.isEmpty()) return true;
        if (!item.get(slot).isStackable()) {
            for (GameItem checkItem : item) {
                if (checkItem.getId() == id)
                    count++;
            }
        } else
            count = item.get(slot).getAmount();
        return amount < count;
    }

    public void fromDuel(int itemID, int fromSlot, int amount) {
        if (!canOffer) {
            declineDuel();
            return;
        }
        if (!ContentInteraction.tryAcquireMs(this, ContentInteraction.DUEL_CONFIRM_STAGE_TWO, 200L)) {
            return;
        }
        Client other = getClient(duel_with);
        if (!inDuel || !validClient(duel_with)) {
            declineDuel();
            return;
        }
        if (checkGameitemAmount(fromSlot, amount, offeredItems) || offeredItems.get(fromSlot).getId() != itemID) {
            return;
        }
        int count = 0;
        if (!Server.itemManager.isStackable(itemID)) {
            for (GameItem item : offeredItems) {
                if (item.getId() == itemID) {
                    count++;
                }
            }
        } else
            count = offeredItems.get(fromSlot).getAmount();
        amount = Math.min(amount, count);
        if (!OfferTransactions.offerToInventory(this, itemID, fromSlot, amount)) {
            return;
        }
        duelConfirmed = false;
        long timeNow = System.currentTimeMillis();
        lastDuelItemChangeMs = timeNow;
        other.lastDuelItemChangeMs = timeNow;
        resetItems(3214);
        resetItems(3322);
        refreshDuelScreen();
        send(new SendString("", 6684));
        other.duelConfirmed = false;
        other.resetItems(3214);
        other.resetItems(3322);
        other.refreshDuelScreen();
        other.send(new SendString("", 6684));
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
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < runePouchesAmount.length; i++) {
            out.append(runePouchesAmount[i]).append(i == runePouchesAmount.length - 1 ? "" : ":");
        }
        return out.toString();
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

    public void openGenie() {
        if (inDuel || duelFight || IsBanking) {
            send(new SendMessage("Finish what you are doing first!"));
            return;
        }
        send(new SendString("Select a skill in which you wish to gain experience!", 2810));
        send(new SendString("", 2811));
        send(new SendString("", 2831));
        genie = true;
        openInterface(2808);
    }

    public void openAntique() {
        if (inDuel || duelFight || IsBanking) {
            send(new SendMessage("Finish what you are doing first!"));
            return;
        }
        send(new SendString("Select a skill in which you wish to gain experience!", 2810));
        send(new SendString("", 2811));
        send(new SendString("", 2831));
        antique = true;
        openInterface(2808);
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
                    && Math.abs(DoorRegistry.doorX[d] - getPosition().getX()) <= 120
                    && Math.abs(DoorRegistry.doorY[d] - getPosition().getY()) <= 120) {
                if (distanceToPoint(DoorRegistry.doorX[d], DoorRegistry.doorY[d]) < 50) {
                    ReplaceObject(DoorRegistry.doorX[d], DoorRegistry.doorY[d], DoorRegistry.doorId[d], DoorRegistry.doorFace[d], 0);
                }
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
        if (inDuel || duelStatus == 3 || UsingAgility || doingTeleport() || getStunTimer() > 0) {
            return;
        }
        if (rejectTeleport()) {
            send(new SendMessage("A magical force stop you from teleporting."));
            return;
        }
        if (isInCombat() || randomed2) {
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

    public void openTrade() {
        Client other = getClient(trade_reqId);
        if (other == null) {
            declineTrade(false);
            return;
        }
        inTrade = true;
        tradeRequested = false;
        resetItems(3322);
        resetTItems(3415);
        resetOTItems(3416);
        send(new InventoryInterface(3323, 3321)); // trading window + bag
        String out = other.getPlayerName();
        if (other.playerRights == 1) {
            out = "@cr1@" + out;
        } else if (other.playerRights == 2) {
            out = "@cr2@" + out;
        }
        send(new SendString("Trading With: " + out, 3417));
        send(new SendString("", 3431));
        send(new SendString("Are you sure you want to make this trade?", 3535));
    }

    public void declineTrade() {
        declineTrade(true);
    }

    public void declineTrade(boolean tellOther) {
        if (!inTrade || tradeSessionId <= 0L || tradeSettledSessionId == tradeSessionId)
            return;
        Client other = getClient(trade_reqId);
        boolean cancelOther = tellOther && other != null && other.inTrade && other.trade_reqId == getSlot()
                && tradeSessionId > 0L && tradeSessionId == other.tradeSessionId;
        boolean refunded = TradeDuelSessionService.cancelTrade(this, cancelOther ? other : null);
        if (!refunded) {
            logger.error("Unable to safely refund cancelled trade for player={} partner={}", getPlayerName(), other == null ? "none" : other.getPlayerName());
            send(new SendMessage("Your trade could not be cancelled safely. Please contact staff."));
            return;
        }
        finishDeclineTrade();
        if (cancelOther) {
            other.finishDeclineTrade();
            GameEventBus.post(new TradeCancelEvent(this, other));
        }
    }

    private void finishDeclineTrade() {
        send(new RemoveInterfaces());
        inTrade = false;
        canOffer = true;
        tradeConfirmed = false;
        tradeConfirmed2 = false;
        offeredItems.clear();
        trade_reqId = -1;
        tradeApproachPendingSlot = -1;
        tradeApproachTicks = 0;
        TradeDuelSessionService.resetTradeSession(this);
        faceTarget(trade_reqId);
        checkItemUpdate();
    }

    public boolean validClient(int index) {
        return PlayerRegistry.validClient(index);
    }

    public Client getClient(int index) {
        return PlayerRegistry.getClient(index);
    }

    public void tradeReq(int id) {
        Client other = getClient(id);
        if (other == null) return;
        setFocus(other.getPosition().getX(), other.getPosition().getY());
        if (!net.dodian.uber.game.engine.config.FeatureStateService.trading.get()) {
            send(new SendMessage("Trading has been temporarily disabled"));
            return;
        }
        for (int a = 0; a < PlayerRegistry.players.length; a++) {
            Client o = getClient(a);
            if (a != getSlot() && validClient(a) && o.dbId > 0 && o.dbId == dbId) {
                logout();
            }
        }
        if (!goodDistanceEntity(other, 1)) {
            tradeApproachPendingSlot = id;
            tradeApproachTicks = 0;
            tradeApproachTargetX = other.getPosition().getX();
            tradeApproachTargetY = other.getPosition().getY();
            resetWalkingQueue();
            // resetWalkingQueue() sets walkingBlock=true, which makes PlayerMovementState.postProcessing()
            // skip queue processing entirely for this cycle. Since we're about to stage a fresh route
            // right below, clear it back so that route isn't silently dropped for a tick.
            walkingBlock = false;
            if (!FollowRouting.routeToEntityBoundary(
                this, other.getPosition().getX(), other.getPosition().getY(),
                1, getPosition().getZ(), null, false
            )) {
                tradeApproachPendingSlot = -1;
                tradeApproachTicks = 0;
                send(new SendMessage("I can't reach " + other.getPlayerName() + "!"));
            }
            return;
        }
        tradeApproachPendingSlot = -1;
        resetWalkingQueue();
        if (getPosition().getX() == other.getPosition().getX() && getPosition().getY() == other.getPosition().getY()) {
            tradeApproachPendingSlot = id;
            tradeApproachTicks = 0;
            tradeApproachTargetX = other.getPosition().getX();
            tradeApproachTargetY = other.getPosition().getY();
            walkingBlock = false;
            if (!FollowRouting.routeToEntityBoundary(
                this, other.getPosition().getX(), other.getPosition().getY(),
                1, getPosition().getZ(), null, false
            )) {
                tradeApproachPendingSlot = -1;
                tradeApproachTicks = 0;
                send(new SendMessage("I can't reach " + other.getPlayerName() + "!"));
            }
            return;
        }
        String tradeBlockMessage = net.dodian.uber.game.engine.systems.interaction.PlayerInteractionGuardService.tradeBlockMessage(this, other);
        if (tradeBlockMessage != null) {
            send(new SendMessage(tradeBlockMessage));
            return;
        }
        if (validClient(trade_reqId)) {
            if (isBusy() || other.isBusy()) {
                send(new SendMessage("That player is busy at the moment"));
                trade_reqId = 0;
                return;
            }
            if (tradeLocked && other.playerRights < 1) {
                return;
            }
        }
        if (dbId == other.dbId) {
            return;
        }
        /*
         * if(other.connectedFrom.equals(connectedFrom) &&
         * !connectedFrom.equals("127.0.0.1")){ tradeRequested = false; return; }
         */
        if (validClient(trade_reqId) && !inTrade && other.tradeRequested && other.trade_reqId == getSlot()) {
            TradeDuelSessionService.beginTradeSession(this, other);
            openTrade();
            other.openTrade();
        } else if (validClient(trade_reqId) && !inTrade && ContentInteraction.tryAcquireMs(this, ContentInteraction.TRADE_REQUEST, 1000L)) {
            tradeRequested = true;
            trade_reqId = id;
            GameEventBus.post(new TradeRequestEvent(this, other));
            send(new SendMessage("Sending trade request..."));
            other.send(new SendMessage(getPlayerName() + ":tradereq:"));
        }
    }

    public void confirmScreen() {
        canOffer = false;
        inTrade = true;
        resetItems(3322);
        send(new InventoryInterface(3443, 3321)); // trade confirm
        Client other = getClient(trade_reqId);
        /* Reset item containers! */
        send(new ClearItemContainer(3538, 28));
        send(new ClearItemContainer(3539, 28));
        /* Set text if 16 or below items! */
        StringBuilder offerItems = new StringBuilder();
        if (offeredItems.size() <= 16) {
            if (offeredItems.isEmpty()) offerItems.append("Absolutely nothing!");
            else {
                int id = 0;
                for (GameItem item : offeredItems) {
                    if (id > 0) offerItems.append("\\n");
                    offerItems.append(getItemName(item.getId()));
                    String amt = Misc.format(item.getAmount());
                    if (item.getAmount() >= 1000000000) {
                        amt = "@gre@" + (item.getAmount() / 1000000000) + " billion @whi@(" + Misc.format(item.getAmount()) + ")";
                    } else if (item.getAmount() >= 1000000) {
                        amt = "@gre@" + (item.getAmount() / 1000000) + " million @whi@(" + Misc.format(item.getAmount()) + ")";
                    } else if (item.getAmount() >= 1000) {
                        amt = "@cya@" + (item.getAmount() / 1000) + "K @whi@(" + Misc.format(item.getAmount()) + ")";
                    }
                    if (item.getAmount() > 1) offerItems.append(" x ").append(amt);
                    id++;
                }
            }
        }
        StringBuilder otherOfferItems = new StringBuilder();
        if (other.offeredItems.size() <= 16) {
            if (other.offeredItems.isEmpty()) otherOfferItems.append("Absolutely nothing!");
            else {
                int id = 0;
                for (GameItem item : other.offeredItems) {
                    if (id > 0) otherOfferItems.append("\\n");
                    otherOfferItems.append(getItemName(item.getId()));
                    String amt = Misc.format(item.getAmount());
                    if (item.getAmount() >= 1000000000) {
                        amt = "@gre@" + (item.getAmount() / 1000000000) + " billion @whi@(" + Misc.format(item.getAmount()) + ")";
                    } else if (item.getAmount() >= 1000000) {
                        amt = "@gre@" + (item.getAmount() / 1000000) + " million @whi@(" + Misc.format(item.getAmount()) + ")";
                    } else if (item.getAmount() >= 1000) {
                        amt = "@cya@" + (item.getAmount() / 1000) + "K @whi@(" + Misc.format(item.getAmount()) + ")";
                    }
                    if (item.getAmount() > 1) otherOfferItems.append(" x ").append(amt);
                    id++;
                }
            }
        }
        /* Sending trading items! */
        if (offeredItems.size() > 16) {
            send(new TradeItemsUpdate(3538, offeredItems));
        }
        if (other.offeredItems.size() > 16) {
            send(new TradeItemsUpdate(3539, other.offeredItems));
        }

        send(new SendString(offeredItems.isEmpty() ? "Absolutely nothing!" : offerItems.toString(), 3557));
        send(new SendString(other.offeredItems.isEmpty() ? "Absolutely nothing!" : otherOfferItems.toString(), 3558));
    }

    private boolean tradeSuccessful = false;

    public void giveItems() {
        Client other = getClient(trade_reqId);
        if (!validClient(trade_reqId) || tradeSuccessful || other.tradeSuccessful) {
            return;
        }
        try {
            TradeDuelSessionService.settleTrade(this, other);
        } catch (Exception e) {
            logger.warn("Giving items failed for {}", getPlayerName(), e);
        }
    }

    public void completeTradeSettlementState() {
        send(new RemoveInterfaces());
        tradeResetNeeded = true;
        tradeSuccessful = true;
        PlayerDeferredLifecycleService.signalTradeFinalizeReady(this);
        saveStats(PlayerSaveReason.TRADE, false, false);
        faceTarget(-1);
    }

    public void resetTrade() {
        offeredItems.clear();
        inTrade = false;
        trade_reqId = 0;
        canOffer = true;
        tradeConfirmed = false;
        tradeConfirmed2 = false;
        tradeSuccessful = false;
        TradeDuelSessionService.resetTradeSession(this);
        send(new RemoveInterfaces());
        tradeResetNeeded = false;
        send(new SendString("Are you sure you want to make this trade?", 3535));
    }

    public void duelReq(int pid) {
        Client other = getClient(pid);
        if (other == null) return;
        setFocus(other.getPosition().getX(), other.getPosition().getY());
        if (!goodDistanceEntity(other, 1)) {
            duelApproachPendingSlot = pid;
            duelApproachTicks = 0;
            duelApproachTargetX = other.getPosition().getX();
            duelApproachTargetY = other.getPosition().getY();
            resetWalkingQueue();
            // resetWalkingQueue() sets walkingBlock=true, which makes PlayerMovementState.postProcessing()
            // skip queue processing entirely for this cycle. Since we're about to stage a fresh route
            // right below, clear it back so that route isn't silently dropped for a tick.
            walkingBlock = false;
            if (!FollowRouting.routeToEntityBoundary(
                this, other.getPosition().getX(), other.getPosition().getY(),
                1, getPosition().getZ(), null, false
            )) {
                duelApproachPendingSlot = -1;
                duelApproachTicks = 0;
                send(new SendMessage("I can't reach " + other.getPlayerName() + "!"));
            }
            return;
        }
        duelApproachPendingSlot = -1;
        resetWalkingQueue();
        if (getPosition().getX() == other.getPosition().getX() && getPosition().getY() == other.getPosition().getY()) {
            duelApproachPendingSlot = pid;
            duelApproachTicks = 0;
            duelApproachTargetX = other.getPosition().getX();
            duelApproachTargetY = other.getPosition().getY();
            walkingBlock = false;
            if (!FollowRouting.routeToEntityBoundary(
                this, other.getPosition().getX(), other.getPosition().getY(),
                1, getPosition().getZ(), null, false
            )) {
                duelApproachPendingSlot = -1;
                duelApproachTicks = 0;
                send(new SendMessage("I can't reach " + other.getPlayerName() + "!"));
            }
            return;
        }
        String duelBlockMessage = net.dodian.uber.game.engine.systems.interaction.PlayerInteractionGuardService.duelBlockMessage(this, other);
        if (duelBlockMessage != null) {
            send(new SendMessage(duelBlockMessage));
            return;
        }
        if (isBusy() || other.isBusy()) {
            send(new SendMessage(isBusy() ? "You are currently busy" : other.getPlayerName() + " is currently busy!"));
            return;
        }
        if (net.dodian.uber.game.engine.systems.combat.CombatLogoutLockService.isLocked(this)
                || net.dodian.uber.game.engine.systems.combat.CombatLogoutLockService.isLocked(other)) {
            send(new SendMessage(net.dodian.uber.game.engine.systems.combat.CombatLogoutLockService.isLocked(this)
                    ? "You can't duel while in combat."
                    : other.getPlayerName() + " can't duel while in combat."));
            return;
        }
        if (inWildy() || other.inWildy()) {
            send(new SendMessage("You cant duel in the wilderness!"));
            return;
        }
        if (!net.dodian.uber.game.engine.config.FeatureStateService.dueling.get()) {
            send(new SendMessage("Dueling has been temporarily disabled"));
            return;
        }
        for (int a = 0; a < PlayerRegistry.players.length; a++) {
            Client o = getClient(a);
            if (a != getSlot() && validClient(a) && o.dbId > 0 && o.dbId == dbId) {
                logout();
            }
        }
        duel_with = pid;
        duelRequested = true;
        if (!validClient(duel_with)) {
            return;
        }
        setFocus(other.getPosition().getX(), other.getPosition().getY());
        if (isBusy() || other.isBusy() || other.duelConfirmed || other.duelConfirmed2) {
            send(new SendMessage("Other player is busy at the moment"));
            duelRequested = false;
            return;
        }
        if (tradeLocked && other.playerRights < 1) {
            return;
        } //I decided to enable duel from same ip, go wild! If we catch you dupe..Oh gosh!
        if (duelRequested && other.duelRequested && duel_with == other.getSlot() && other.duel_with == getSlot()) {
            openDuel();
            other.openDuel();
        } else {
            send(new SendMessage("Sending duel request..."));
            other.send(new SendMessage(getPlayerName() + ":duelreq:"));
        }
    }

    public void openDuel() {
        duelRule[9] = true;
        RefreshDuelRules();
        refreshDuelScreen();
        inDuel = true;
        Client other = getClient(duel_with);
        send(new SendString("Dueling with: " + other.getPlayerName() + " (level-" + other.determineCombatLevel() + ")", 31005));
        send(new SendString("Opponent's combat level: @or2@" + other.determineCombatLevel(), 31006));
        send(new SendString("", 31009));
        resetItems(3322);
        send(new InventoryInterface(31000, 3321));
        sendDuelArmour(other);
    }

    public void declineDuel() {
        Client other = getClient(duel_with);
        inDuel = false;
        if (validClient(duel_with) && other.inDuel) {
            other.declineDuel();
        }
        send(new RemoveInterfaces());
        canOffer = true;
        duel_with = 0;
        duelApproachPendingSlot = -1;
        duelApproachTicks = 0;
        duelRequested = false;
        duelConfirmed = false;
        duelConfirmed2 = false;
        duelFight = false;
        if (!OfferTransactions.refundOffers(this)) {
            send(new SendMessage("Your staked items could not be returned safely. Please contact staff."));
            return;
        }
        /*
         * Danno: Reset's duel options when duel declined to stop scammers.
         */
        resetDuel();
        RefreshDuelRules();
        failer = "";
        faceTarget(-1);
        checkItemUpdate();
    }

    public void confirmDuel() {
        canOffer = false;
        resetItems(3322);
        send(new InventoryInterface(31500, 3321));
        Client other = getClient(duel_with);
        if (!validClient(duel_with)) {
            declineDuel();
        }
        int lineIndex = 0;
        for (int i = 0; i < duelRule.length; i++) {
            if (lineIndex >= 15) break;
            boolean on = duelRule[i];
            if (i == 9) on = true;
            if (i == 3 || i == 4 || i == 5 || i == 10) on = false;
            String color = on ? "@red@" : ((i == 3 || i == 4 || i == 5 || i == 10) ? "@or2@" : "@gre@");
            send(new SendString(color + duelNames[i], 31505 + lineIndex));
            lineIndex++;
        }
        while (lineIndex < 15) {
            send(new SendString("", 31505 + lineIndex));
            lineIndex++;
        }
        int slot = 0;
        for (GameItem item : offeredItems) {
            if (slot >= 28) break;
            if (item.getId() > 0 && item.getAmount() > 0) {
                send(new SendString("</col>" + getItemName(item.getId()) + " @whi@x " + String.format("%,d", item.getAmount()), 31531 + slot));
                slot++;
            }
        }
        while (slot < 28) {
            send(new SendString("", 31531 + slot));
            slot++;
        }
        slot = 0;
        if (validClient(duel_with)) {
            for (GameItem item : other.offeredItems) {
                if (slot >= 28) break;
                if (item.getId() > 0 && item.getAmount() > 0) {
                    send(new SendString("</col>" + other.getItemName(item.getId()) + " @whi@x " + String.format("%,d", item.getAmount()), 31561 + slot));
                    slot++;
                }
            }
        }
        while (slot < 28) {
            send(new SendString("", 31561 + slot));
            slot++;
        }
        send(new SendString("", 31526));
    }

    public void startDuel() {
        send(new RemoveInterfaces());
        canAttack = false;
        canOffer = false;
        duelFight = true;
        prayerManager.reset();
        addEffectTime(2, 0); //Need to reset this for dueling!
        GetBonus(true); //Set bonus due to blessing!
        for (int i = 0; i < boostedLevel.length; i++) {
            boostedLevel[i] = 0;
            ProgressionService.refresh(this, Skill.getSkill(i));
        }
        Client other = getClient(duel_with);
        for (GameItem item : other.offeredItems) {
            otherOfferedItems.add(new GameItem(item.getId(), item.getAmount()));
        }
        otherdbId = other.dbId;
        final Client player = this;
        final DuelCountdownState[] countdownState = {DuelCountdownState.initial()};
        player.requestForceChat("It is time to D-D-D-DUEL!");
        GameEventScheduler.runRepeatingMs(600, () -> {
            var step = DuelCountdownService.advance(countdownState[0]);
            countdownState[0] = step.getNextState();

            if (step.getForceChat() != null) {
                player.requestForceChat(step.getForceChat());
            }
            if (step.getEnableCombat()) {
                player.canAttack = true;
                return false;
            }
            return true;
        });
    }

    public void processPendingApproaches() {
        if (duelApproachPendingSlot != -1) {
            duelApproachTicks++;
            if (duelApproachTicks > 40) {
                duelApproachPendingSlot = -1;
                duelApproachTicks = 0;
                return;
            }
            Client other = getClient(duelApproachPendingSlot);
            if (other == null) {
                duelApproachPendingSlot = -1;
                duelApproachTicks = 0;
                return;
            }
            if (goodDistanceEntity(other, 1)) {
                if (getPosition().getX() == other.getPosition().getX() && getPosition().getY() == other.getPosition().getY()) {
                    if (!FollowRouting.routeToEntityBoundary(
                        this, other.getPosition().getX(), other.getPosition().getY(),
                        1, getPosition().getZ(), null, false
                    )) {
                        duelApproachPendingSlot = -1;
                        duelApproachTicks = 0;
                        send(new SendMessage("I can't reach " + other.getPlayerName() + "!"));
                    }
                    return;
                }
                int pending = duelApproachPendingSlot;
                duelApproachPendingSlot = -1;
                duelApproachTicks = 0;
                duelReq(pending);
            } else if (other.getPosition().getX() != duelApproachTargetX
                    || other.getPosition().getY() != duelApproachTargetY
                    || (wQueueReadPtr == wQueueWritePtr && newWalkCmdSteps == 0)) {
                // Target moved since the route was staged, or we've exhausted the previously staged
                // route without getting close (e.g. it walked away) - re-route toward where they are now.
                duelApproachTargetX = other.getPosition().getX();
                duelApproachTargetY = other.getPosition().getY();
                if (!FollowRouting.routeToEntityBoundary(
                    this, other.getPosition().getX(), other.getPosition().getY(),
                    1, getPosition().getZ(), null, false
                )) {
                    duelApproachPendingSlot = -1;
                    duelApproachTicks = 0;
                    send(new SendMessage("I can't reach " + other.getPlayerName() + "!"));
                }
            }
        }
        if (tradeApproachPendingSlot != -1) {
            tradeApproachTicks++;
            if (tradeApproachTicks > 40) {
                tradeApproachPendingSlot = -1;
                tradeApproachTicks = 0;
                return;
            }
            Client other = getClient(tradeApproachPendingSlot);
            if (other == null) {
                tradeApproachPendingSlot = -1;
                tradeApproachTicks = 0;
                return;
            }
            if (goodDistanceEntity(other, 1)) {
                if (getPosition().getX() == other.getPosition().getX() && getPosition().getY() == other.getPosition().getY()) {
                    if (!FollowRouting.routeToEntityBoundary(
                        this, other.getPosition().getX(), other.getPosition().getY(),
                        1, getPosition().getZ(), null, false
                    )) {
                        tradeApproachPendingSlot = -1;
                        tradeApproachTicks = 0;
                        send(new SendMessage("I can't reach " + other.getPlayerName() + "!"));
                    }
                    return;
                }
                int pending = tradeApproachPendingSlot;
                tradeApproachPendingSlot = -1;
                tradeApproachTicks = 0;
                tradeReq(pending);
            } else if (other.getPosition().getX() != tradeApproachTargetX
                    || other.getPosition().getY() != tradeApproachTargetY
                    || (wQueueReadPtr == wQueueWritePtr && newWalkCmdSteps == 0)) {
                // Target moved since the route was staged, or we've exhausted the previously staged
                // route without getting close (e.g. it walked away) - re-route toward where they are now.
                tradeApproachTargetX = other.getPosition().getX();
                tradeApproachTargetY = other.getPosition().getY();
                if (!FollowRouting.routeToEntityBoundary(
                    this, other.getPosition().getX(), other.getPosition().getY(),
                    1, getPosition().getZ(), null, false
                )) {
                    tradeApproachPendingSlot = -1;
                    tradeApproachTicks = 0;
                    send(new SendMessage("I can't reach " + other.getPlayerName() + "!"));
                }
            }
        }
    }

    /*
     * Danno: Edited for new duel rules, for future use.
     */
    public void resetDuel() {
        send(new RemoveInterfaces());
        duelWin = false;
        canOffer = true;
        duel_with = 0;
        duelRequested = false;
        duelConfirmed = false;
        duelConfirmed2 = false;
        offeredItems.clear();
        otherOfferedItems.clear();
        duelFight = false;
        canAttack = true;
        inDuel = false;
        duelRule = new boolean[]{false, false, false, false, false, false, false, false, false, false, false};
        Arrays.fill(duelBodyRules, false);
        otherdbId = -1;
    }

    public void setVarp(int id, int value) {
        send(new SetVarbit(id, value));
    }

    public void varbit(int id, int value) {
        // Compatibility alias. This writes a raw client settings/varp slot.
        setVarp(id, value);
    }

    public boolean toggleDuelRule(int ruleIndex) {
        Client other = getClient(duel_with);
        if (other == null || ruleIndex < 0 || ruleIndex >= duelRule.length
                || ruleIndex == 3 || ruleIndex == 4 || ruleIndex == 5 || ruleIndex == 9 || ruleIndex == 10
                || !ContentInteraction.tryAcquireMs(this, ContentInteraction.DUEL_RULES, 800L)) {
            return false;
        }
        if (inDuel && !duelFight && !duelConfirmed2 && !other.duelConfirmed2 && !(duelConfirmed && other.duelConfirmed)) {
            duelRule[ruleIndex] = !duelRule[ruleIndex];
            other.duelRule[ruleIndex] = duelRule[ruleIndex];
            duelConfirmed = false;
            other.duelConfirmed = false;
            send(new SendString("", 31009));
            other.send(new SendString("", 31009));
            RefreshDuelRules();
            other.RefreshDuelRules();
            return true;
        }
        return false;
    }

    public boolean toggleDuelBodyRule(int ruleIndex) {
        Client other = getClient(duel_with);
        if (other == null || ruleIndex < 0 || ruleIndex >= duelBodyRules.length
                || !ContentInteraction.tryAcquireMs(this, ContentInteraction.DUEL_BODY_RULES, 400L)) {
            return false;
        }
        if (inDuel && !duelFight && !duelConfirmed2 && !other.duelConfirmed2 && !(duelConfirmed && other.duelConfirmed)) {
            duelBodyRules[ruleIndex] = !duelBodyRules[ruleIndex];
            other.duelBodyRules[ruleIndex] = duelBodyRules[ruleIndex];
            duelConfirmed = false;
            other.duelConfirmed = false;
            send(new SendString("", 31009));
            other.send(new SendString("", 31009));
            RefreshDuelRules();
            other.RefreshDuelRules();
            return true;
        }
        return false;
    }

    public void addFriend(long name) {
        // On = 0, Friends = 1, Off = 2
        for (Friend f : friends) {
            if (f.name == name) {
                send(new SendMessage(Utils.longToPlayerName(name) + " is already on your friends list"));
                return;
            }
        }
        friends.add(new Friend(name, true));
        for (Client c : PlayerRegistry.playersOnline.values()) {
            if (c.hasFriend(longName)) {
                c.refreshFriends();
            }
        }
        refreshFriends();
    }

    public boolean isMuted() {
        long rightNow = System.currentTimeMillis();
        return mutedTill - rightNow > 0;
    }

    public void sendPmMessage(long friend, byte[] pmchatText, int pmchatTextSize) {
        if (isMuted()) {
            send(new SendMessage("You are currently muted!"));
            return;
        }
        boolean found = false;
        for (Friend f : friends) {
            if (f.name == friend) {
                found = true;
                break;
            }
        }
        if (!found) {
            send(new SendMessage("That player is not on your friends list"));
            return;
        }
        if (PlayerRegistry.playersOnline.containsKey(friend)) {
            Client to = PlayerRegistry.playersOnline.get(friend);
            boolean specialRights = to.playerGroup == 6 || to.playerGroup == 10 || to.playerGroup == 35;
            if (specialRights && to.busy && playerRights < 1) {
                send(new SendMessage("<col=FF0000>This player is busy and did not receive your message."));
                return;
            }
            if (to.Privatechat == 0 || (to.Privatechat == 1 && to.hasFriend(longName))) {
                to.sendpm(longName, playerRights, pmchatText, pmchatTextSize);
                ChatLog.recordPrivateChat(this, to, Utils.textUnpack(pmchatText, pmchatTextSize));
            } else {
                send(new SendMessage("That player is not available"));
            }
        } else {
            send(new SendMessage("That player is not online"));
        }
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

    public void removeFriend(long name) {
        for (Friend f : friends) {
            if (f.name == name) {
                friends.remove(f);
                // Notify client to remove this friend from its local list (opcode 51)
                send(new RemoveFriend(name));
                // Refresh remaining friends' online/offline statuses
                refreshFriends();
                return;
            }
        }
    }

    public void removeIgnore(long name) {
        for (Friend f : ignores) {
            if (f.name == name) {
                ignores.remove(f);
                refreshFriends();
                Client player = PlayerRegistry.playersOnline.get(f.name);
                if (player != null) {
                    player.refreshFriends();
                }
                break;
            }
        }
    }

    public void addIgnore(long name) {
        if (ignores.size() >= 100) {
            send(new SendMessage("Maximum ignores reached!"));
            return;
        }
        boolean canAdd = true;
        for (Friend f : ignores) {
            if (f.name == name) {
                send(new SendMessage("You already got this guy on your ignoreList!"));
                canAdd = false;
                break;
            }
        }
        if (canAdd) {
            ignores.add(new Friend(name, true));
            Client player = PlayerRegistry.playersOnline.get(name);
            if (player != null) {
                player.refreshFriends();
            }
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
        if (X < 0 && Y > 0)
            newWalkCmdSteps = Math.abs(Y - X);
        else if (Y < 0 && X > 0)
            newWalkCmdSteps = Math.abs(X - Y);
        else if (Y < 0 && X < 0)
            newWalkCmdSteps = Math.abs(-X - Y);
        else newWalkCmdSteps = Math.abs(X + Y);

        if (newWalkCmdSteps == 1) newWalkCmdSteps = 2; //Need this incase value is 1!
        if (newWalkCmdSteps % 2 != 0) {
            newWalkCmdSteps /= 2;
        }

        if (++newWalkCmdSteps > 50) {
            newWalkCmdSteps = 0;
        }
        int l = getPosition().getX();
        l -= mapRegionX * 8;
        for (i = 1; i < newWalkCmdSteps; i++) {
            newWalkCmdX[i] = X;
            newWalkCmdY[i] = Y;
            tmpNWCX[i] = newWalkCmdX[i];
            tmpNWCY[i] = newWalkCmdY[i];
        }
        newWalkCmdX[0] = newWalkCmdY[0] = tmpNWCX[0] = tmpNWCY[0] = 0;
        int j1 = getPosition().getY();
        j1 -= mapRegionY * 8;
        newWalkCmdIsRunning = run; //isRunning = run;
        for (i = 0; i < newWalkCmdSteps; i++) {
            newWalkCmdX[i] += l;
            newWalkCmdY[i] += j1;
        }
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
        newWalkCmdIsRunning = false;
        if (time > 0) applyWalkBlockMs(time);
        AddToCords(X, Y, false);
    }

    public void AddToRunCords(int X, int Y, long time) {
        newWalkCmdIsRunning = true;
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

    public void acceptDuelWon() {
        if (duelFight && duelWin) {
            duelWin = false;
            if (!ContentInteraction.tryAcquireMs(this, ContentInteraction.DUEL_ACCEPT_WIN, 1000L)) {
                return;
            }
            Client other = getClient(duel_with);
            CopyOnWriteArrayList<GameItem> offerCopy = new CopyOnWriteArrayList<>();
            CopyOnWriteArrayList<GameItem> otherOfferCopy = new CopyOnWriteArrayList<>();
            for (GameItem item : otherOfferedItems) {
                otherOfferCopy.add(new GameItem(item.getId(), item.getAmount()));
            }
            for (GameItem item : offeredItems) {
                offerCopy.add(new GameItem(item.getId(), item.getAmount()));
            }
            if (!OfferTransactions.settleDuelPayout(this, other)) {
                duelWin = true;
                send(new SendMessage("Not enough inventory space to collect your winnings. Free up space and try again."));
                return;
            }
            if (this.dbId > other.dbId)
                TradeLog.recordTrade(dbId, otherdbId, offerCopy, otherOfferCopy, false);
            resetDuel();
            GetBonus(true);
            saveStats(PlayerSaveReason.DUEL, false, false);
            faceTarget(-1);
            checkItemUpdate();
            if (validClient(duel_with)) {
                other.resetDuel();
                GetBonus(true);
                other.saveStats(PlayerSaveReason.DUEL, false, false);
                other.faceTarget(-1);
                other.checkItemUpdate();
            }
        }
    }

    public boolean contains(int item) {
        for (int playerItem : playerItems) {
            if (playerItem == item + 1)
                return true;
        }
        return false;
    }

    public void setConfigIds() {
        stakeConfigId[0] = 16384; // No head armour
        stakeConfigId[1] = 32768; // No capes
        stakeConfigId[2] = 65536; // No amulets
        stakeConfigId[3] = 134217728; // No arrows
        stakeConfigId[4] = 131072; // No weapon
        stakeConfigId[5] = 262144; // No body armour
        stakeConfigId[6] = 524288; // No shield
        stakeConfigId[7] = 2097152; // No leg armour
        stakeConfigId[8] = 67108864; // No hand armour
        stakeConfigId[9] = 16777216; // No feet armour
        stakeConfigId[10] = 8388608; // No rings
    }

    /**
     * Shows armour in the duel screen slots! (hopefully lol)
     */
    /**
     * Sends the player's equipment to the duel interface.
     * 
     * @param c The client to send the equipment to
     */
    public void sendDuelArmour(Client c) {
        c.send(new DuelArmourUpdate(getEquipment(), getEquipmentN()));
    }

    public boolean hasTradeSpace() {
        if (!validClient(trade_reqId)) {
            return true;
        }
        Client o = getClient(trade_reqId);
        int spaces = 0;
        ArrayList<GameItem> items = new ArrayList<>();
        for (GameItem item : o.offeredItems) {
            if (item == null)
                continue;
            if (item.getAmount() > 0) {
                if (!items.contains(item)) {
                    items.add(item);
                    spaces += 1;
                } else {
                    if (!item.isStackable()) {
                        spaces += 1;
                    }
                }
            }
        }
        if (spaces > getFreeSpace()) {
            failer = getPlayerName() + " does not have enough space to hold items being traded.";
            o.failer = getPlayerName() + " does not have enough space to hold items being traded.";
            return true;
        }
        return false;
    }

    /**
     * @return if player has enough space to remove items.
     */
    public boolean hasEnoughSpace() {
        if (!inDuel || !validClient(duel_with)) {
            return true;
        }
        Client o = getClient(duel_with);
        int spaces = 0;
        for (int i = 0; i < duelBodyRules.length; i++) {
            if (!duelBodyRules[i])
                continue;
            if (getEquipmentN()[trueSlots[i]] > 0) {
                spaces += 1;
            }
        }
        ArrayList<GameItem> items = new ArrayList<>();
        for (GameItem item : offeredItems) {
            if (item == null)
                continue;
            if (item.getAmount() > 0) {
                if (!items.contains(item)) {
                    items.add(item);
                    spaces += 1;
                } else {
                    if (!item.isStackable()) {
                        spaces += 1;
                    }
                }
            }
        }
        for (GameItem item : o.offeredItems) {
            if (item == null)
                continue;
            if (item.getAmount() > 0) {
                if (!items.contains(item)) {
                    items.add(item);
                    spaces += 1;
                } else {
                    if (!Server.itemManager.isStackable(item.getId())) {
                        spaces += 1;
                    }
                }
            }
        }
        if (spaces > getFreeSpace()) {
            failer = getPlayerName() + " does not have enough space to hold items being removed and/or staked.";
            o.failer = getPlayerName() + " does not have enough space to hold items being removed and/or staked.";
            return true;
        }
        return false;

    }

    public void removeEquipment() {
        for (int i = 0; i < duelBodyRules.length; i++) {
            if (!duelBodyRules[i])
                continue;
            if (getEquipmentN()[trueSlots[i]] > 0) {
                int id = getEquipment()[trueSlots[i]];
                int amount = getEquipmentN()[trueSlots[i]];
                if (remove(trueSlots[i], true)) {
                    markSaveDirty(PlayerSaveSegment.EQUIPMENT.getMask());
                    addItem(id, amount);
                }
                checkItemUpdate();
            }
        }
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
        AgilityTicketExchangeService.spendTickets(this);
    }

    public PrayerManager getPrayerManager() {
        return prayerManager;
    }

    public void checkBow() {
        int weaponId = getEquipment()[Equipment.Slot.WEAPON.getId()];
        usingBow = bowWeapon(weaponId);
    }

    public boolean bowWeapon(int weaponId) {
        String weaponType = Server.itemManager.getWeaponType(weaponId);
        if (weaponType.isEmpty()) {
            boolean bow = net.dodian.uber.game.skill.fletching.FletchingData.isBowWeapon(weaponId);
            if (weaponId == 839 || weaponId == 841 || weaponId == 4212 || weaponId == 6724 || weaponId == 20997 ||
                    weaponId == 11235 || weaponId == 4734 || (weaponId >= 12765 && weaponId <= 12768))
                bow = true;
            return bow;
        }
        return weaponType.equals("bow") || weaponType.equals("crossbow") || weaponType.equals("thrown")
            || weaponType.equals("chinchompas") || weaponType.equals("salamander");
    }

    public boolean checkInv = false;

    public void openUpOtherInventory(String player) {
        if (IsBanking || isShopping() || duelFight) {
            send(new SendMessage("Please finish with what you are doing!"));
            return;
        }
        ArrayList<GameItem> otherInv = new ArrayList<>();
        if (PlayerRegistry.getPlayer(player) != null) { //Online check
            Client other = (Client) PlayerRegistry.getPlayer(player);
            for (int i = 0; i < Objects.requireNonNull(other).playerItems.length; i++) {
                otherInv.add(i, new GameItem(other.playerItems[i] - 1, other.playerItemsN[i]));
            }
            sendInventory(3214, otherInv);
            send(new SendMessage("User " + player + "'s inventory is now being shown."));
            checkInv = true;
        } else {
            send(new SendMessage("Loading " + player + "'s inventory..."));
            CommandDbService.submit(
                    "check-inventory",
                    () -> CommandDbService.loadOfflineContainerView(player, "inventory"),
                    result -> applyOfflineInventoryView(player, result),
                    exception -> {
                        if (!disconnected) {
                            logger.debug("issue: {}", exception.getMessage(), exception);
                            send(new SendMessage("Could not load that inventory right now."));
                        }
                    }
            );
        }
    }

    public void openUpOtherBank(String player) {
        if (IsBanking || isShopping() || duelFight) {
            send(new SendMessage("Please finish with what you are doing!"));
            return;
        }
        ArrayList<GameItem> otherBank = new ArrayList<>();
        IsBanking = false;
        clearBankStyleView();
        if (PlayerRegistry.getPlayer(player) != null) { //Online check
            Client other = (Client) PlayerRegistry.getPlayer(player);
            ArrayList<Integer> ids = new ArrayList<>();
            ArrayList<Integer> amounts = new ArrayList<>();
            for (int i = 0; i < Objects.requireNonNull(other).bankItems.length; i++) {
                if (other.bankItems[i] > 0 && other.bankItemsN[i] > 0) {
                    ids.add(other.bankItems[i] - 1);
                    amounts.add(other.bankItemsN[i]);
                }
            }
            openBankStyleView(ids, amounts, "Examine the bank of " + player);
            IsBanking = false;
        } else {
            send(new SendMessage("Loading " + player + "'s bank..."));
            CommandDbService.submit(
                    "check-bank",
                    () -> CommandDbService.loadOfflineContainerView(player, "bank"),
                    result -> applyOfflineBankView(player, result),
                    exception -> {
                        if (!disconnected) {
                            logger.debug("issue: {}", exception.getMessage(), exception);
                            send(new SendMessage("Could not load that bank right now."));
                        }
                    }
            );
        }
    }

    public void updateGroundItems() {
        /* Untradeable items prio 1! */
        if (!Ground.untradeable_items.isEmpty())
            for (GroundItem item : Ground.untradeable_items) {
                if (item.isTaken() || dbId != item.playerId || !isWithinDistance(getPosition().getX(), getPosition().getY(), item.x, item.y, 104))
                    continue;
                send(new RemoveGroundItem(new GameItem(item.id, item.amount), new Position(item.x, item.y, item.z)));
                send(new CreateGroundItem(new GameItem(item.id, item.amount), new Position(item.x, item.y, item.z)));
            }
        /* Tradeable items prio 2! */
        if (!Ground.tradeable_items.isEmpty())
            for (GroundItem item : Ground.tradeable_items) {
                if (item.isTaken() || (item.playerId != dbId && !item.isVisible()) || !isWithinDistance(getPosition().getX(), getPosition().getY(), item.x, item.y, 104))
                    continue;
                send(new RemoveGroundItem(new GameItem(item.id, item.amount), new Position(item.x, item.y, item.z)));
                send(new CreateGroundItem(new GameItem(item.id, item.amount), new Position(item.x, item.y, item.z)));
            }
        /* Static ground items prio last! */
        if (!Ground.ground_items.isEmpty())
            for (GroundItem item : Ground.ground_items) {
                if (item.isTaken() || !item.visible || !isWithinDistance(getPosition().getX(), getPosition().getY(), item.x, item.y, 104))
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

    private void applyOfflineInventoryView(String player, CommandDbService.OfflineContainerViewResult result) {
        if (disconnected) {
            return;
        }
        if (IsBanking || isShopping() || duelFight) {
            send(new SendMessage("Inventory view cancelled because you started another action."));
            return;
        }
        if (result.getStatus() == CommandDbService.OfflineContainerViewResult.Status.USERNAME_NOT_FOUND) {
            send(new SendMessage("username '" + player + "' do not exist in the database!"));
            return;
        }
        if (result.getStatus() == CommandDbService.OfflineContainerViewResult.Status.CHARACTER_NOT_FOUND) {
            send(new SendMessage("username '" + player + "' have yet to login!"));
            return;
        }
        sendInventory(3214, result.getItems());
        send(new SendMessage("User " + player + "'s inventory is now being shown."));
        checkInv = true;
    }

    private void applyOfflineBankView(String player, CommandDbService.OfflineContainerViewResult result) {
        if (disconnected) {
            return;
        }
        if (IsBanking || isShopping() || duelFight) {
            send(new SendMessage("Bank view cancelled because you started another action."));
            return;
        }
        if (result.getStatus() == CommandDbService.OfflineContainerViewResult.Status.USERNAME_NOT_FOUND) {
            send(new SendMessage("username '" + player + "' do not exist in the database!"));
            return;
        }
        if (result.getStatus() == CommandDbService.OfflineContainerViewResult.Status.CHARACTER_NOT_FOUND) {
            send(new SendMessage("username '" + player + "' have yet to login!"));
            return;
        }
        ArrayList<Integer> ids = new ArrayList<>();
        ArrayList<Integer> amounts = new ArrayList<>();
        for (GameItem item : result.getItems()) {
            if (item.getId() >= 0 && item.getAmount() > 0) {
                ids.add(item.getId());
                amounts.add(item.getAmount());
            }
        }
        openBankStyleView(ids, amounts, "Examine the bank of " + player);
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
        if (decision instanceof TravelDecision.Ignored) {
            return;
        }
        if (decision instanceof TravelDecision.Rejected) {
            TravelDecision.Rejected rejected = (TravelDecision.Rejected) decision;
            send(new SendMessage(rejected.getMessage()));
            return;
        }
        if (decision instanceof TravelDecision.RequireUnlockDialogue) {
            TravelDecision.RequireUnlockDialogue unlockDialogue = (TravelDecision.RequireUnlockDialogue) decision;
            DialogueService.setDialogueId(this, unlockDialogue.getDialogueId());
            DialogueService.setDialogueSent(this, false);
            return;
        }
        if (decision instanceof TravelDecision.Approved) {
            TravelDecision.Approved approved = (TravelDecision.Approved) decision;
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
    }

    public int refundSlot = -1;
    public ArrayList<PartyRoomRewardItem> rewardList = new ArrayList<>();
    private final ArrayList<String> refundDates = new ArrayList<>();

    public void setRefundList() {
        rewardList.clear();
        refundDates.clear();
        try {
            for (RefundRecord refund : RefundRepository.loadUnclaimed(dbId)) {
                rewardList.add(new PartyRoomRewardItem(refund.getItemId(), refund.getAmount()));
                refundDates.add(refund.getDate());
            }
        } catch (Exception e) {
            logger.warn("Error in checking sql!! {}", e.getMessage(), e);
        }
    }

    public void setRefundOptions() {
        if (rewardList.isEmpty()) {
            refundSlot = -1;
            send(new SendMessage("You got no items to collect!"));
            return;
        }
        int slot = refundSlot;
        String[] text = new String[rewardList.size() < 4 ? rewardList.size() + 2 : rewardList.size() - slot <= 3 ? rewardList.size() - slot + 2 : 6];
        text[0] = "Refund Item List";
        int position = Math.min(3, rewardList.size() - slot);
        for (int i = 0; i < position; i++)
            text[i + 1] = "Claim " + rewardList.get(slot + i).getAmount() + " of " + getItemName(rewardList.get(slot + i).getId());
        text[position + 1] = text.length < 6 && slot == 0 ? "Close" : text.length == 6 ? "Next" : "Previous";
        if (text.length == 6)
            text[position + 2] = slot == 0 ? "Close" : "Previous";
        showPlayerOption(text);
    }

    public void reclaim(int position) {
        int slot = refundSlot + position;
        try {
            int rowIndex = slot - 1;
            if (rowIndex < 0 || rowIndex >= rewardList.size() || rowIndex >= refundDates.size()) {
                sendMessage("That refund entry is no longer available.");
                setRefundList();
                return;
            }
            PartyRoomRewardItem item = rewardList.get(rowIndex);
            if (!RefundRepository.markClaimed(dbId, refundDates.get(rowIndex))) {
                sendMessage("That refund entry was already claimed.");
                setRefundList();
                return;
            }
            /* Set back options! */
            setRefundList();
            if (!rewardList.isEmpty()) {
                refundSlot = 0;
                setRefundOptions();
            } else send(new RemoveInterfaces());
            /* Refund item function */
            int amount = item.getAmount() - getFreeSpace();
            if (Server.itemManager.isStackable(item.getId())) {
                if (getFreeSpace() == 0) {
                    Ground.addFloorItem(this, item.getId(), item.getAmount());
                    send(new SendMessage("Some items have been dropped to the ground!"));
                    ItemLog.playerDrop(this, item.getId(), item.getAmount(), getPosition().copy(), "Claim Items Dropped");
                } else addItem(item.getId(), item.getAmount());
            } else if (amount > 0) {
                addItem(item.getId(), getFreeSpace());
                for (int i = 0; i < getFreeSpace(); i++)
                    addItem(item.getId(), 1);
                for (int i = 0; i < amount; i++) {
                    Ground.addFloorItem(this, item.getId(), 1);
                }
                send(new SendMessage("Some items have been dropped to the ground!"));
                ItemLog.playerDrop(this, item.getId(), amount, getPosition().copy(), "Claim Items Dropped");
            } else
                for (int i = 0; i < item.getAmount(); i++)
                    addItem(item.getId(), 1);
            checkItemUpdate();
        } catch (Exception e) {
            logger.warn("Error in checking sql!! {}", e.getMessage(), e);
        }
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
        return inTrade || inDuel || duelFight || IsBanking;
    }
}
