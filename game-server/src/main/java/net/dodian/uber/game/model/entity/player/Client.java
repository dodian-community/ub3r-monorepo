package net.dodian.uber.game.model.entity.player;



import net.dodian.uber.game.Constants;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.event.GameEventScheduler;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.ShopHandler;
import net.dodian.uber.game.model.UpdateFlag;
import net.dodian.uber.game.model.entity.Entity;
import net.dodian.uber.game.model.entity.npc.Npc;
import net.dodian.uber.game.model.item.*;
import net.dodian.uber.game.model.object.DoorHandler;
import net.dodian.uber.game.model.object.RS2Object;
import net.dodian.uber.game.model.player.content.Skillcape;
import net.dodian.uber.game.model.player.bank.PlayerBankService;
import net.dodian.uber.game.netty.listener.OutgoingPacket;
import net.dodian.uber.game.model.player.quests.QuestSend;
import net.dodian.uber.game.model.player.skills.Skill;
import net.dodian.uber.game.model.player.skills.Skills;
import net.dodian.uber.game.model.player.skills.prayer.Prayers;
import net.dodian.uber.game.content.skills.slayer.SlayerService;
import net.dodian.uber.game.content.skills.farming.FarmingService;
import net.dodian.uber.game.content.skills.farming.FarmingState;
import net.dodian.uber.game.persistence.command.CommandDbService;
import net.dodian.uber.game.persistence.account.AccountPersistenceService;
import net.dodian.uber.game.persistence.db.DbTables;
import net.dodian.uber.game.persistence.player.PlayerSaveReason;
import net.dodian.uber.game.persistence.player.PlayerSaveSegment;
import net.dodian.uber.game.engine.net.InboundPacketMailbox;
import net.dodian.uber.game.engine.net.OutboundSessionQueue;
import net.dodian.uber.game.engine.processing.EntityProcessor;
import net.dodian.uber.game.content.skills.mining.MiningService;
import net.dodian.uber.game.content.skills.woodcutting.WoodcuttingService;
import net.dodian.uber.game.content.skills.fletching.FletchingService;
import net.dodian.uber.game.content.skills.fletching.FletchingState;
import net.dodian.uber.game.content.skills.fishing.FishingService;
import net.dodian.uber.game.content.skills.fishing.FishingState;
import net.dodian.uber.game.content.skills.cooking.CookingService;
import net.dodian.uber.game.content.skills.cooking.CookingState;
import net.dodian.uber.game.content.skills.crafting.CraftingService;
import net.dodian.uber.game.content.skills.crafting.CraftingMode;
import net.dodian.uber.game.content.skills.crafting.CraftingState;
import net.dodian.uber.game.content.skills.crafting.GoldJewelryService;
import net.dodian.uber.game.content.skills.crafting.TanningRequest;
import net.dodian.uber.game.content.skills.crafting.TanningService;
import net.dodian.uber.game.content.skills.prayer.PrayerInteractionService;
import net.dodian.uber.game.content.skills.prayer.PrayerOfferingState;
import net.dodian.uber.game.content.skills.runecrafting.RunecraftingDefinitions;
import net.dodian.uber.game.content.skills.runecrafting.RunecraftingPouchService;
import net.dodian.uber.game.content.skills.runecrafting.RunecraftingService;
import net.dodian.uber.game.content.skills.runecrafting.RunecraftingState;
import net.dodian.uber.game.systems.ui.dialogue.DialogueOptionService;
import net.dodian.uber.game.systems.ui.dialogue.DialogueDisplayService;
import net.dodian.uber.game.systems.ui.dialogue.DialogueService;
import net.dodian.uber.game.content.skills.smithing.SmithingDefinitions;
import net.dodian.uber.game.content.skills.smithing.SmithingInterfaceService;
import net.dodian.uber.game.content.skills.smithing.SmeltingInterfaceService;
import net.dodian.uber.game.netty.listener.out.*;
import net.dodian.uber.game.content.events.partyroom.RewardItem;
import net.dodian.uber.game.persistence.audit.*;
import net.dodian.uber.game.systems.action.PlayerActionCancellationService;
import net.dodian.uber.game.systems.action.PlayerActionCancelReason;
import net.dodian.uber.game.systems.action.PlayerActionType;
import net.dodian.uber.game.systems.action.ProductionActionService;
import net.dodian.uber.game.systems.action.SkillingActionService;
import net.dodian.uber.game.systems.action.SmithingActionService;
import net.dodian.uber.game.systems.action.TeleportActionService;
import net.dodian.uber.game.systems.animation.PlayerAnimationService;
import net.dodian.uber.game.systems.combat.CombatStartService;
import net.dodian.uber.game.systems.interaction.PlayerInteractionGuardService;
import net.dodian.uber.game.systems.interaction.InteractionAnchorState;
import net.dodian.uber.game.engine.lifecycle.PlayerDeferredLifecycleService;
import net.dodian.utilities.*;
import net.dodian.uber.game.content.skills.core.progression.SkillProgressionService;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import io.netty.channel.Channel;

/* Kotlin imports */
import static net.dodian.uber.game.systems.combat.ClientExtensionsKt.getRangedStr;
import static net.dodian.uber.game.systems.combat.PlayerAttackCombatKt.attackTarget;
import static net.dodian.uber.game.model.player.skills.Skill.*;
import static net.dodian.uber.game.persistence.db.DatabaseKt.getDbConnection;
import static net.dodian.uber.game.config.DotEnvKt.*;
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

    public FarmingService farming = new FarmingService();
    public FarmingState farmingJson = new FarmingState();
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
    public int maxQuests = QuestSend.values().length;
    public int[] quests = new int[maxQuests];
    public int[] playerBonus = new int[12];
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
    private boolean outboundDirty = false;
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
    public CopyOnWriteArrayList<Friend> friends = new CopyOnWriteArrayList<>();
    public CopyOnWriteArrayList<Friend> ignores = new CopyOnWriteArrayList<>();
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
    private final int[] stakeConfigId = new int[23];
    private final int[] duelRuleConfigIds = {11, 12, 13, 22, 15, 16, 17, 18, 19, 20, 21};
    public int[] duelLine = {6698, 6699, 6697, 7817, 669, 6696, 6701, 6702, 6703, 6704, 6731};
    public boolean duelRequested = false, inDuel = false, duelConfirmed = false, duelConfirmed2 = false,
            duelFight = false;
    public int duel_with = 0;
    public boolean tradeRequested = false, inTrade = false, canOffer = true, tradeConfirmed = false,
            tradeConfirmed2 = false, tradeResetNeeded = false;
    public int trade_reqId = 0;
    public CopyOnWriteArrayList<GameItem> offeredItems = new CopyOnWriteArrayList<>();
    public CopyOnWriteArrayList<GameItem> otherOfferedItems = new CopyOnWriteArrayList<>();
    public boolean adding = false;
    public ArrayList<RS2Object> objects = new ArrayList<>();
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

    public int[] requiredLevel = {1, 10, 20, 30, 40, 50, 60, 70, 74, 76, 80, 82, 86, 88, 92,
            94, 96};

    public int[] baseDamage = {2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30, 32};
    public String[] spellName = {"Smoke Rush", "Shadow Rush", "Blood Rush", "Ice Rush",
            "Smoke Burst", "Shadow Burst", "Blood Burst", "Ice Burst",
            "Smoke Blitz", "Shadow Blitz", "Blood Blitz", "Ice Blitz",
            "Smoke Barrage", "Shadow Barrage", "Blood Barrage", "Ice Barrage"};
    public int[] ancientId = {12939, 12987, 12901, 12861, 12963, 13011, 12919, 12881, 12951, 12999, 12911, 12871, 12975, 13023, 12929, 12891};
    public int[] coolDown = {5, 5, 6, 6};
    public int[] ancientButton = {51133, 51185, 51091, 24018, 51159, 51211, 51111, 51069, 51146, 51198, 51102, 51058, 51172, 51224, 51122, 51080};
    public String properName = "";
    public int actionButtonId = 0;
    public int skillX = 0, skillY = 0;
    public int stairs = 0, stairDistance = 0;
    public boolean validLogin = false;

    /**
     * Legacy NPC compatibility bridge.
     */
    public void openTan() {
        TanningService.open(this);
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
    @Deprecated
    public void refreshSkill(Skill skill) {
        SkillProgressionService.refresh(this, skill);
    }

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
        String wep = GetItemName(weapon).toLowerCase();
        //2952 aka wolfbane to strong as 3 tick weapon!
        int wepPlainTick = 4; //Default tick for many weapons
        if (wep.contains("dart") || wep.contains("knife")) {
            wepPlainTick = 2;
        } else if (wep.contains("longsword") || wep.contains("mace") || wep.contains("axe") && !wep.contains("dharok")
                || wep.contains("spear") || wep.contains("tzhaar-ket-em") || wep.contains("torag") || wep.contains("guthan")
                || wep.contains("verac") || (wep.contains("staff") && !wep.contains("ahrim")) || wep.contains("composite")
                || wep.contains("crystal") || wep.contains("thrownaxe") || wep.contains("longbow")) {
            wepPlainTick = 5;
        } else if (wep.contains("battleaxe") || wep.contains("warhammer") || wep.contains("godsword")
                || wep.contains("barrelchest") || wep.contains("ahrim") || wep.contains("toktz-mej-tal")
                || wep.contains("dorgeshuun") || (wep.contains("crossbow") && !wep.contains("karil")) || wep.contains("javelin")) {
            wepPlainTick = 6;
        } else if (wep.contains("2h sword") || wep.contains("halberd") || wep.contains("maul") || wep.contains("balmung")
                || wep.contains("tzhaar-ket-om") || wep.contains("dharok")) {
            wepPlainTick = 7;
        }
        if (usingBow && fightType == 2) //Rapid style
            wepPlainTick -= 1;
        return wepPlainTick;
    }

    public boolean hasStaff() {
        for (int staff : staffs) {
            if (getEquipment()[Equipment.Slot.WEAPON.getId()] == staff)
                return true;
        }
        return false;
    }

    public void CheckGear() {
        checkBow();
        if (!hasStaff()) autocast_spellIndex = -1;
    }

    public int distanceToPoint(int pointX, int pointY) {
        return (int) Math.sqrt(Math.pow(getPosition().getX() - pointX, 2) + Math.pow(getPosition().getY() - pointY, 2));
    }

    public int distanceToPoint(Position checkOne, Position checkTwo) {
        return (int) Math.sqrt(Math.pow(checkOne.getX() - checkTwo.getX(), 2) + Math.pow(checkOne.getY() - checkTwo.getY(), 2));
    }

    public void animation(int id, Position pos) {
        for (int i = 0; i < PlayerHandler.players.length; i++) {
            Player p = net.dodian.uber.game.systems.world.player.PlayerRegistry.players[i];
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
            for (int i = 0; i < PlayerHandler.players.length; i++) {
                Player p = net.dodian.uber.game.systems.world.player.PlayerRegistry.players[i];
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
     * @param angle The starting angle of the projectile
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
                               int offsetX, int angle, int speed, int gfxMoving, int startHeight,
                               int endHeight, int targetIndex, int begin, int slope, int initDistance) {
        try {
            Position casterPosition = new Position(casterX, casterY);
            send(new Projectile(casterPosition, offsetY, offsetX, angle, speed, gfxMoving,
                    startHeight, endHeight, targetIndex, begin, slope, initDistance));
        } catch (Exception e) {
            Server.logError(e.getMessage());
        }
    }

    public void arrowGfx(int offsetY, int offsetX, int angle, int speed,
                         int gfxMoving, int startHeight, int endHeight, int index, int begin, int slope) {
        forEachProjectileViewer(viewer ->
                viewer.createProjectile(getPosition().getY(), getPosition().getX(), offsetY, offsetX, angle, speed, gfxMoving,
                        startHeight, endHeight, index, begin, slope, 64));
    }

    public void arrowNpcGfx(Position pos, int offsetY, int offsetX, int angle, int speed,
                            int gfxMoving, int startHeight, int endHeight, int index, int begin, int slope) {
        forEachProjectileViewer(viewer ->
                viewer.createProjectile(pos.getY(), pos.getX(), offsetY, offsetX, angle, speed, gfxMoving,
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
        PlayerHandler.forEachActivePlayer(viewer -> {
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
        return;
    }

    public void print_debug(String str) {
        // TODO: Is this method necessary? I commented out the implementation of it and will just redirect it to the println variant.
        println_debug(str);

        // String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        // System.out.print("[" + timestamp + "] [client-" + getSlot() + "-" + getPlayerName() + "]: " + str);
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

    public void sendFrame200(int MainFrame, int SubFrame) {
        send(new SendFrame200(MainFrame, SubFrame));
    }

    public void sendInterfaceAnimation(int mainFrame, int subFrame) {
        sendFrame200(mainFrame, subFrame);
    }

    public void sendFrame164(int Frame) {
        send(new SendFrame164(Frame));
    }

    public void sendChatboxInterface(int frame) {
        sendFrame164(frame);
    }

    public void sendFrame246(int MainFrame, int SubFrame, int SubFrame2) {
        send(new SendFrame246(MainFrame, SubFrame, SubFrame2));
    }

    public void sendInterfaceModel(int mainFrame, int subFrame, int subFrame2) {
        sendFrame246(mainFrame, subFrame, subFrame2);
    }

    public void sendQuestSomething(int id) {
        send(new SendQuestSomething(id));
    }

    public void clearQuestInterface() {
        for (int j : QuestInterface)
            send(new SendString("", j));
    }

    public void showInterface(int interfaceid) {
        resetAction();
        send(new ShowInterface(interfaceid));
    }

    public void openInterface(int interfaceId) {
        showInterface(interfaceId);
    }

    public void closeInterfaces() {
        send(new RemoveInterfaces());
        clearWalkableInterface();
    }

    public int ancients = 1;
    public int[] QuestInterface = {8145, 8147, 8148, 8149, 8150, 8151, 8152, 8153, 8154, 8155, 8156, 8157, 8158, 8159,
            8160, 8161, 8162, 8163, 8164, 8165, 8166, 8167, 8168, 8169, 8170, 8171, 8172, 8173, 8174, 8175, 8176, 8177, 8178,
            8179, 8180, 8181, 8182, 8183, 8184, 8185, 8186, 8187, 8188, 8189, 8190, 8191, 8192, 8193, 8194, 8195, 12174,
            12175, 12176, 12177, 12178, 12179, 12180, 12181, 12182, 12183, 12184, 12185, 12186, 12187, 12188, 12189, 12190,
            12191, 12192, 12193, 12194, 12195, 12196, 12197, 12198, 12199, 12200, 12201, 12202, 12203, 12204, 12205, 12206,
            12207, 12208, 12209, 12210, 12211, 12212, 12213, 12214, 12215, 12216, 12217, 12218, 12219, 12220, 12221, 12222,
            12223};

    public int[] statId = {10252, 11000, 10253, 11001, 10254, 11002, 10255, 11011, 11013, 11014, 11010, 11012, 11006,
            11009, 11008, 11004, 11003, 11005, 47002, 54090, 11007};
    public String[] BonusName = {"Stab", "Slash", "Crush", "Magic", "Range", "Stab", "Slash", "Crush", "Prayer", "Range",
            "Str", "Spell Dmg"};

    public int i;
    public int XremoveSlot = 0;
    public int XinterfaceID = 0;
    public int XremoveID = 0;
    public int currentBankTab = 0;
    public int previousBankTab = 0;
    public int lastButtonActionIndex = -1;
    public boolean bankSearchActive = false;
    public boolean bankSearchPendingInput = false;
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

    public record InboundProcessResult(int processedPackets, int walkPacketsProcessed, int mousePacketsProcessed,
                                       int walkPacketsReplaced, int mousePacketsReplaced, int fifoPacketsDropped) {
    }

    public record OutboundFlushStats(int flushedMessages, int flushedBytes) {

        public static OutboundFlushStats empty() {
                return new OutboundFlushStats(0, 0);
            }
        }

    @Override
    public void destruct() {
        PlayerDeferredLifecycleService.cancelAll(this);
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

    public boolean queueInboundPacket(net.dodian.uber.game.netty.game.GamePacket packet) {
        if (packet == null || disconnected) {
            return false;
        }
        InboundPacketMailbox.EnqueueResult result = inboundPacketMailbox.enqueue(packet);
        if (!result.accepted()) {
            return false;
        }
        markInboundReadyIfNeeded();
        return true;
    }

    public InboundProcessResult processQueuedPackets(int maxPacketsPerTick) {
        if (maxPacketsPerTick <= 0 || disconnected) {
            InboundPacketMailbox.MailboxCounters counters = inboundPacketMailbox.snapshotAndResetCounters();
            return new InboundProcessResult(0, 0, 0, counters.walkReplaced(), counters.mouseReplaced(), counters.fifoDropped());
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
                disconnected = true;
                println_debug("Error processing opcode " + packet.opcode() + " for " + getPlayerName() + ": " + ex.getMessage());
                break;
            } finally {
                if (packet.payload() != null && packet.payload().refCnt() > 0) {
                    packet.payload().release();
                }
            }
        }
        InboundPacketMailbox.MailboxCounters counters = inboundPacketMailbox.snapshotAndResetCounters();
        return new InboundProcessResult(
                processedCount,
                walkProcessed,
                mouseProcessed,
                counters.walkReplaced(),
                counters.mouseReplaced(),
                counters.fifoDropped()
        );
    }

    private void dispatchQueuedPacket(net.dodian.uber.game.netty.game.GamePacket packet) throws Exception {
        recordInboundPacket(packet);
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
        return opcode == 155 || opcode == 17 || opcode == 21 || opcode == 18 || opcode == 72;
    }

    private void recordInboundPacket(net.dodian.uber.game.netty.game.GamePacket packet) {
        int slot = recentInboundWriteIndex;
        recentInboundOpcodes[slot] = packet.opcode();
        recentInboundSizes[slot] = packet.size();
        recentInboundCycles[slot] = PlayerHandler.cycle;
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
        outboundDirty = false;
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
        if (shouldQueueOutbound()) {
            outboundSessionQueue.enqueue(message);
            outboundDirty = true;
            return;
        }
        channel.writeAndFlush(message);
    }

    public OutboundFlushStats flushOutbound() {
        if (disconnected || channel == null || !channel.isActive()) {
            return OutboundFlushStats.empty();
        }
        if (!outboundDirty) {
            return OutboundFlushStats.empty();
        }
        OutboundSessionQueue.DrainResult drain = outboundSessionQueue.drainTo(channel);
        if (drain.messageCount() <= 0) {
            outboundDirty = !outboundSessionQueue.isEmpty();
            return OutboundFlushStats.empty();
        }
        channel.flush();
        outboundDirty = !outboundSessionQueue.isEmpty();
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
        Integer openedInterfaceId = null;
        String openedVia = null;

        if (packet instanceof ShowInterface) {
            openedInterfaceId = ((ShowInterface) packet).interfaceId();
            openedVia = "ShowInterface";
        } else if (packet instanceof InventoryInterface) {
            openedInterfaceId = ((InventoryInterface) packet).getInterfaceId();
            openedVia = "InventoryInterface";
        } else if (packet instanceof SendFrame164) {
            openedInterfaceId = ((SendFrame164) packet).frame();
            openedVia = "Frame164";
        } else if (packet instanceof RemoveInterfaces) {
            activeInterfaceId = -1;
        }

        if (openedInterfaceId != null) {
            activeInterfaceId = openedInterfaceId;
        }

        packet.send(this);

    }

    @Override
    public void run() {
        // Login is now handled by Netty's LoginProcessorHandler
        // This method is kept for backward compatibility but does nothing for Netty connections

    }

    public void setSidebarInterface(int menuId, int form) {
        send(new SetSidebarInterface(menuId, form));
    }

    @Deprecated
    public void setSkillLevel(Skill skill, int currentLevel, int XP) {
        SkillProgressionService.setSkillLevel(this, skill, currentLevel, XP);
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
        PlayerActionCancellationService.cancel(this, PlayerActionCancelReason.LOGOUT, false, false, false, true);
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
            PlayerHandler.playersOnline.remove(longName);
            PlayerHandler.allOnline.remove(longName);
            println_debug(getPlayerName() + " has logged out correctly!");
        /*for (Player p : PlayerHandler.players) {
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
            for (Client c : PlayerHandler.playersOnline.values()) {
                if (c.hasFriend(longName)) {
                    c.refreshFriends();
                }
            }
            if (inTrade) declineTrade();
            else if (inDuel && !duelFight) declineDuel();
            else if (duel_with > 0 && validClient(duel_with) && inDuel && duelFight) {
                Client p = getClient(duel_with);
                p.duelWin = true;
                p.DuelVictory();
            }
        }
        // TODO: Look into improving this, and potentially a system to configure player saving per world id...
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
        boolean bankChanged = false;
        int id = GetNotedItem(itemID);
        if (amount == -2) { //draw all from bank!
            if (!takeAsNote && !Server.itemManager.isStackable(itemID))
                amount = freeSlots() == 0 ? 1 : Math.min(bankItemsN[fromSlot], freeSlots());
            else
                amount = freeSlots() == 0 && !playerHasItem(id == 0 ? itemID : id) ? 1 : bankItemsN[fromSlot];
        }
        if (bankItems[fromSlot] - 1 != itemID || (bankItems[fromSlot] - 1 != itemID && bankItemsN[fromSlot] != amount)) {
            return;
        }
        if (amount > 0) {
            if (bankItems[fromSlot] > 0) {
                if (!takeAsNote) {
                    if (Server.itemManager.isStackable(itemID)) {
                        if (bankItemsN[fromSlot] > amount) {
                            if (addItem((bankItems[fromSlot] - 1), amount)) {
                                bankItemsN[fromSlot] -= amount;
                                bankChanged = true;
                            }
                        } else {
                            if (addItem(itemID, bankItemsN[fromSlot])) {
                                bankItems[fromSlot] = 0;
                                bankItemsN[fromSlot] = 0;
                                bankChanged = true;
                            }
                        }
                    } else {
                        while (amount > 0) {
                            if (bankItemsN[fromSlot] > 0) {
                                if (addItem(itemID, 1)) {
                                    bankItemsN[fromSlot] -= 1;
                                    bankChanged = true;
                                    amount--;
                                } else {
                                    amount = 0;
                                }
                            } else {
                                amount = 0;
                            }
                        }
                    }
                } else if (id > 0) {
                    if (bankItemsN[fromSlot] > amount) {
                        if (addItem(id, amount)) {
                            bankItemsN[fromSlot] -= amount;
                            bankChanged = true;
                        }
                    } else {
                        if (addItem(id, bankItemsN[fromSlot])) {
                            bankItems[fromSlot] = 0;
                            bankItemsN[fromSlot] = 0;
                            bankChanged = true;
                        }
                    }
                } else {
                    send(new SendMessage(Server.itemManager.getName(itemID) + " can't be drawn as note."));
                    if (Server.itemManager.isStackable(itemID)) {
                        if (bankItemsN[fromSlot] > amount) {
                            if (addItem(itemID, amount)) {
                                bankItemsN[fromSlot] -= amount;
                                bankChanged = true;
                            }
                        } else {
                            if (addItem(itemID, bankItemsN[fromSlot])) {
                                bankItems[fromSlot] = 0;
                                bankItemsN[fromSlot] = 0;
                                bankChanged = true;
                            }
                        }
                    } else {
                        while (amount > 0) {
                            if (bankItemsN[fromSlot] > 0) {
                                if (addItem(itemID, 1)) {
                                    bankItemsN[fromSlot] -= 1;
                                    bankChanged = true;
                                    amount--;
                                } else {
                                    amount = 0;
                                }
                            } else {
                                amount = 0;
                            }
                        }
                    }
                }
            }
        }
        checkItemUpdate();
        if (bankChanged) {
            markSaveDirty(PlayerSaveSegment.BANK.getMask());
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
        int slot = -1;
        for (int i = 0; i < bankSize() && slot == -1; i++)
            if (bankItems[i] == itemID + 1)
                slot = i;
        return slot == -1 ? 0 : bankItemsN[slot];
    }
    public int getBankSlot(int itemID) {
        int slot = -1;
        for (int i = 0; i < bankSize() && slot == -1; i++)
            if (bankItems[i] == itemID + 1) slot = i;
        return slot;
    }

    @Deprecated
    public boolean giveExperience(int amount, Skill skill) {
        return SkillProgressionService.gainXp(this, amount, skill);
    }

    public void bankItem(int itemID, int fromSlot, int amount) {
        if (playerItemsN[fromSlot] <= 0 || playerItems[fromSlot] <= 0 || playerItems[fromSlot] - 1 != itemID) {
            return;
        }
        if (!IsBanking) {
            send(new RemoveInterfaces());
            return;
        }
        boolean bankChanged = false;
        ensureBankTabState();
        int id = GetUnnotedItem(itemID);
        if (id == 0) {
            if (playerItems[fromSlot] <= 0) {
                return;
            }
            amount = Math.min(amount, getInvAmt(itemID));
            if (Server.itemManager.isStackable(itemID) || playerItemsN[fromSlot] > 1) {
                int toBankSlot = 0;
                boolean alreadyInBank = false;
                for (int i = 0; i < bankSize(); i++) {
                    if (bankItems[i] - 1 == itemID) { //Bank starts at value 0 while items should start at -1!
                        if (playerItemsN[fromSlot] < amount) {
                            amount = playerItemsN[fromSlot];
                        }
                        alreadyInBank = true;
                        toBankSlot = i;
                        i = bankSize() + 1;
                    }
                }

                if (!alreadyInBank && freeBankSlots() > 0) {
                    for (int i = 0; i < bankSize(); i++) {
                        if (bankItems[i] <= 0) {
                            toBankSlot = i;
                            i = bankSize() + 1;
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

                for (int i = 0; i < bankSize(); i++) {
                    if (bankItems[i] == playerItems[fromSlot]) {
                        alreadyInBank = true;
                        toBankSlot = i;
                        i = bankSize() + 1;
                    }
                }
                if (!alreadyInBank && freeBankSlots() > 0) {
                    for (int i = 0; i < bankSize(); i++) {
                        if (bankItems[i] <= 0) {
                            toBankSlot = i;
                            i = bankSize() + 1;
                        }
                    }
                    int firstPossibleSlot = 0;
                    boolean itemExists = false;

                    while (amount > 0) {
                        for (int i = firstPossibleSlot; i < playerItems.length; i++) {
                            if ((playerItems[i]) == itemID) {
                                firstPossibleSlot = i;
                                itemExists = true;
                                i = 30;
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
                                i = 30;
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
                for (int i = 0; i < bankSize(); i++) {
                    if (bankItems[i] == GetUnnotedItem(playerItems[fromSlot] - 1) + 1) {
                        if (playerItemsN[fromSlot] < amount) {
                            amount = playerItemsN[fromSlot];
                        }
                        alreadyInBank = true;
                        toBankSlot = i;
                        i = bankSize() + 1;
                    }
                }
                if (!alreadyInBank && freeBankSlots() > 0) {
                    for (int i = 0; i < bankSize(); i++) {
                        if (bankItems[i] <= 0) {
                            toBankSlot = i;
                            i = bankSize() + 1;
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

                for (int i = 0; i < bankSize(); i++) {
                    if (bankItems[i] == (playerItems[fromSlot] - 1)) {
                        alreadyInBank = true;
                        toBankSlot = i;
                        i = bankSize() + 1;
                    }
                }
                if (!alreadyInBank && freeBankSlots() > 0) {
                    for (int i = 0; i < bankSize(); i++) {
                        if (bankItems[i] <= 0) {
                            toBankSlot = i;
                            i = bankSize() + 1;
                        }
                    }
                    int firstPossibleSlot = 0;
                    boolean itemExists = false;

                    while (amount > 0) {
                        for (int i = firstPossibleSlot; i < playerItems.length; i++) {
                            if ((playerItems[i]) == itemID) {
                                firstPossibleSlot = i;
                                itemExists = true;
                                i = 30;
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
                                i = 30;
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
        if (PlayerBankService.moveBankItems(this, from, to, moveWindow)) {
        }
    }

    public int freeBankSlots() {
        int freeS = 0;

        for (int i = 0; i < bankSize(); i++) {
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

        if (!Ground.isTracked(target) || target.isTaken() || !Ground.canPickup(this, target)) {
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

        if (addItem(target.id, target.amount)) {
            Ground.deleteItem(target);
            ItemLog.playerPickup(this, target.npc ? target.npcId : target.playerId, target.id, target.amount, getPosition().copy(), target.npc);
            checkItemUpdate();
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
        if (!Server.shopping) {
            send(new SendMessage("Shopping have been disabled!"));
            return;
        }
        if (ShopID == 20 || ShopID == 34) {
            if (!premium) {
                send(new SendMessage("You need to be a premium member to access this shop."));
                return;
            }
        }
        MyShopID = ShopID;
        checkItemUpdate();
        send(new SendString(ShopHandler.ShopName[ShopID], 3901));
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
        if (item < 0 || amount < 1) {
            return false;
        }
        amount = !Server.itemManager.isStackable(item) ? 1 : amount;
        if ((freeSlots() >= amount && !Server.itemManager.isStackable(item)) || freeSlots() > 0) {
            for (int i = 0; i < playerItems.length; i++) {
                if (playerItems[i] == (item + 1) && Server.itemManager.isStackable(item) && playerItems[i] > 0) {
                    playerItems[i] = (item + 1);
                    if ((playerItemsN[i] + amount) < maxItemAmount && (playerItemsN[i] + amount) > -1) {
                        playerItemsN[i] += amount;
                    } else {
                        playerItemsN[i] = maxItemAmount;
                    }
                    markSaveDirty(PlayerSaveSegment.INVENTORY.getMask());
                    return true;
                }
            }
            for (int i = 0; i < playerItems.length; i++) {
                if (playerItems[i] <= 0) {
                    playerItems[i] = item + 1;
                    playerItemsN[i] = Math.min(amount, maxItemAmount);
                    markSaveDirty(PlayerSaveSegment.INVENTORY.getMask());
                    return true;
                }
            }
            return false;
        } else if (contains(item) && Server.itemManager.isStackable(item)) {
            int slot = -1;
            for (int i = 0; i < playerItems.length; i++) {
                if (playerItems[i] == item + 1) {
                    slot = i;
                    break;
                }
            }
            if ((long) playerItemsN[slot] + (long) amount > (long) Integer.MAX_VALUE) {
                send(new SendMessage("Failed! Reached max item amount!"));
                return false;
            }
            playerItemsN[slot] = playerItemsN[slot] + amount;
            markSaveDirty(PlayerSaveSegment.INVENTORY.getMask());
            checkItemUpdate();
            return true;
        } else {
            send(new SendMessage("Not enough space in your inventory."));
            return false;
        }
    }

    public void addItemSlot(int item, int amount, int slot) {
        item++;
        playerItems[slot] = item;
        playerItemsN[slot] = amount;
        markSaveDirty(PlayerSaveSegment.INVENTORY.getMask());
        checkItemUpdate();
    }

    public void dropItem(int id, int slot) {
        if (isBusy()) {
            send(new SendMessage("You are currently busy!"));
            return;
        }
        if (!Server.dropping) {
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
        deleteItem(id, GetItemSlot(id), amount);
    }

    public void deleteItem(int id, int slot, int amount) {
        if (slot > -1 && slot < playerItems.length) {
            if ((playerItems[slot] - 1) == id) {
                if (playerItemsN[slot] > amount) {
                    playerItemsN[slot] -= amount;
                } else {
                    playerItemsN[slot] = 0;
                    playerItems[slot] = 0;
                }
                markSaveDirty(PlayerSaveSegment.INVENTORY.getMask());
            }
        }
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

    public void wear(int wearID, int slot, int interFace) {
        if (isBusy() || interFace != 3214) {
            return;
        }
        if (net.dodian.uber.game.content.skills.runecrafting.RunecraftingPlugin.emptyPouch(this, wearID)) { //Runecrafting Pouches
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
            net.dodian.uber.game.content.skills.slayer.SlayerPlugin.sendCurrentTask(this);
            return;
        }
        if (duelConfirmed && !duelFight)
            return;
        if (!playerHasItem(wearID)) {
            return;
        }
        int targetSlot = Server.itemManager.getSlot(wearID);
        if (canUse(wearID)) {
            send(new SendMessage("You must be a premium member to use this item"));
            return;
        }
        if (targetSlot != 8 && duelBodyRules[falseSlots[targetSlot]]) {
            send(new SendMessage("Current duel rules restrict this from being worn!"));
            return;
        }
        if ((playerItems[slot] - 1) == wearID) {
            if (!checkEquip(wearID, targetSlot, slot))
                return;
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
            setEquipment(getEquipment()[targetSlot], getEquipmentN()[targetSlot], targetSlot);
            markSaveDirty(PlayerSaveSegment.EQUIPMENT.getMask());
            wearing = false;
            getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
        }
    }

    public boolean checkEquip(int id, int slot, int invSlot) {
        boolean maxCheck = GetItemName(id).contains(("Max cape")) || GetItemName(id).contains(("Max hood"));
        if (maxCheck && totalLevel() < Skills.maxTotalLevel()) {
            send(new SendMessage("You need a total level of " + Skills.maxTotalLevel() + " to equip the " + GetItemName(id).toLowerCase() + "."));
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
        return !failCheck;
    }

    public boolean remove(int slot, boolean force) {
        if (duelConfirmed && !force) {
            return false;
        }
        getEquipment()[slot] = -1;
        getEquipmentN()[slot] = 0;
        setEquipment(getEquipment()[slot], getEquipmentN()[slot], slot);
        getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
        return true;
    }

    public void deleteequiment(int wearID, int slot) {
        if (getEquipment()[slot] == wearID) {
            getEquipment()[slot] = -1;
            getEquipmentN()[slot] = 0;
            setEquipment(getEquipment()[slot], getEquipmentN()[slot], slot);
        }
    }

    public void setChatOptions(int publicChat, int privateChat, int tradeBlock) {
        send(new SetChatOptions(publicChat, privateChat, tradeBlock));
    }


    public void initialize() {
        new PlayerInitializer().initializePlayer(this);
    }

    public void update() { //Update player before npc for some reason!
        sendPlayerSynchronization();
        sendNpcSynchronization();
    }

    public void sendPlayerSynchronization() {
        PlayerUpdatePacket.sendTo(this, this);
    }

    public void sendNpcSynchronization() {
        NpcUpdatePacket.sendTo(this, this);
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
        for (Player p : PlayerHandler.players) {
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
        for (Player p : PlayerHandler.players) {
            if (p == null || !p.isActive)
                continue;
            Client temp = (Client) p;
            temp.send(new SendMessage(message + ":yell:"));
        }
    }

    public void yellKilled(String message) {
        for (Player p : PlayerHandler.players) {
            if (p == null || !p.isActive || !(p.inWildy() || p.inEdgeville()))
                continue;
            Client temp = (Client) p;
            temp.send(new SendMessage(message + ":yell:"));
        }
    }

    public void yellAreaKilled(String message, String area) {
        for (Player p : PlayerHandler.players) {
            if (p == null || !p.isActive || !p.getPositionName().contains(area))
                continue;
            Client temp = (Client) p;
            temp.send(new SendMessage("<col=FFFF00>[Area]<col=000000> " + message + ":yell:"));
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

    public boolean IsItemInBag(int ItemID) {
        for (int playerItem : playerItems) {
            if ((playerItem - 1) == ItemID) {
                return true;
            }
        }
        return false;
    }

    public boolean hasItemInInventory(int itemId) {
        return IsItemInBag(itemId);
    }

    public boolean AreXItemsInBag(int ItemID, int Amount) {
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

    public boolean hasItemsInInventory(int itemId, int amount) {
        return AreXItemsInBag(itemId, amount);
    }

    public int GetItemSlot(int ItemID) {
        for (int i = 0; i < playerItems.length; i++) {
            if ((playerItems[i] - 1) == ItemID) {
                return i;
            }
        }
        return -1;
    }

    public int getItemSlot(int itemId) {
        return GetItemSlot(itemId);
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
            if (GetItemName(playerItem - 1).contains(name))
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
        send(new PrivateMessage(name, rights, chatmessage, messagesize, handler.lastchatid++));
    }

    public void loadpm(long name, int world) {
        send(new LoadPrivateMessage(name, world));
    }

    public int[] staffs = {1391, 1393, 1395, 1397, 1399, 2415, 2416, 2417, 4675, 6526, 6914, 4710};

    /**
     * Decrements the arrow count and updates the client.
     * 
     * @return true if an arrow was deleted, false if no arrows were left
     */
    public boolean DeleteArrow() {
        int arrowSlot = Equipment.Slot.ARROWS.getId();
        if (getEquipmentN()[arrowSlot] > 0) {
            // Decrement arrow count
            getEquipmentN()[arrowSlot] -= 1;
            int remainingAmount = getEquipmentN()[arrowSlot];
            
            // Get the current arrow item ID (or -1 if no arrows left)
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
        return Server.npcManager.getName(NpcID).replaceAll("_", " ");
    }

    public String GetItemName(int ItemID) {
        return Server.itemManager.getName(ItemID);
    }

    public String getItemName(int itemId) {
        return GetItemName(itemId);
    }

    public double GetShopSellValue(int ItemID) {
        return Server.itemManager.getShopSellValue(ItemID);
    }

    public double GetShopBuyValue(int ItemID) {
        return Server.itemManager.getShopBuyValue(ItemID);
    }

    public int GetUnnotedItem(int ItemID) {
        String NotedName = Server.itemManager.getName(ItemID);
        for (Item item : Server.itemManager.items.values()) {
            String checkName = item.getName(), checkDesc = item.getDescription();
            if (item.getNoteable() && item.getId() != ItemID && (checkName != null && checkName.equals(NotedName)) && (checkDesc != null && !checkDesc.startsWith("Swap this note at any bank for a"))) {
                return item.getId();
            }
        }
        return 0;
    }

    public int getUnnotedItem(int itemId) {
        return GetUnnotedItem(itemId);
    }

    public int GetNotedItem(int ItemID) {
        String NotedName = Server.itemManager.getName(ItemID);
        for (Item item : Server.itemManager.items.values()) {
            String checkName = item.getName(), checkDesc = item.getDescription();
            if (!item.getNoteable() && item.getId() != ItemID && (checkName != null && checkName.equals(NotedName)) && (checkDesc != null && checkDesc.startsWith("Swap_this_note_at_any_bank"))) {
                return item.getId();
            }
        }
        return 0;
    }

    public int getNotedItem(int itemId) {
        return GetNotedItem(itemId);
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
    }

    public int neglectDmg() {
        int bonus = 0;
        if (getEquipment()[Equipment.Slot.SHIELD.getId()] == 11284)
            bonus += ((getLevel(Skill.FIREMAKING) + 1) / 5) * 10;
        return Math.min(1000, playerBonus[11] + bonus);
    }

    public double magicDmg() {
        double bonus = playerBonus[3] / 10D;
        return bonus <= 0.0 ? 1.0 : (1.0 + (bonus / 100D));
    }

    public void updateBonus(int id) {
        String send;
        if (id == 3) {
            double dmg = (magicDmg() - 1.0) * 100D;
            send = "Spell Dmg: " + String.format("%3.1f", dmg) + "%";
        } else if (id == 11)
            send = "Neglect Dmg: " + String.format("%3.1f", neglectDmg() / 10D) + "%";
        else if (id == 10)
            send = (usingBow ? "Ranged str: " : "Melee str: ") + (usingBow && getRangedStr(this) >= 0 ? "+" : playerBonus[id] >= 0 ? "+" : "-") + (usingBow ? getRangedStr(this) : playerBonus[id]);
        else
            send = BonusName[id] + ": " + (playerBonus[id] >= 0 ? "+" + playerBonus[id] : playerBonus[id]);
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
        if (!net.dodian.uber.game.systems.interaction.PlayerTickThrottleService.tryAcquireMs(this, net.dodian.uber.game.systems.interaction.PlayerTickThrottleService.TRADE_CONFIRM_STAGE_ONE, 200L) || !canOffer) {
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
            boolean found = false;
            for (GameItem item : offeredItems) {
                if (item.getId() == itemID) {
                    if (item.isStackable()) {
                        if (amount < item.getAmount())
                            offeredItems.set(fromSlot, new GameItem(item.getId(), item.getAmount() - amount));
                        else
                            offeredItems.remove(item);
                        found = true;
                    } else {
            /*if (item.getAmount() > amount) {
              item.removeAmount(amount);
              found = true;
            } else {
              amount = item.getAmount();
              found = true;
              offeredItems.remove(item);
            }*/
                        if (amount == 1) {
                            offeredItems.remove(item);
                            found = true;
                        } else {
                            offeredItems.remove(item);
                            addItem(itemID, 1);
                            amount--;
                        }
                    }
                    if (found) { //If found add item to inventory!
                        addItem(itemID, amount);
                        break;
                    }
                }
            }
            tradeConfirmed = false;
            other.tradeConfirmed = false;
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
        if (!net.dodian.uber.game.systems.interaction.PlayerTickThrottleService.tryAcquireMs(this, net.dodian.uber.game.systems.interaction.PlayerTickThrottleService.TRADE_CONFIRM_STAGE_TWO, 200L)) {
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
        if (Server.itemManager.isStackable(itemID)) {
            boolean inTrade = false;
            for (GameItem item : offeredItems) {
                if (item.getId() == itemID) {
                    inTrade = true;
                    item.addAmount(amount);
                    deleteItem(itemID, fromSlot, amount);
                    break;
                }
            }
            if (!inTrade) {
                offeredItems.add(new GameItem(itemID, amount));
                deleteItem(itemID, fromSlot, amount);
            }
        } else {
            for (int a = 1; a <= amount; a++) {
                if (a == 1) {
                    offeredItems.add(new GameItem(itemID, 1));
                    deleteItem(itemID, fromSlot, amount);
                } else {
                    int slot = findItem(itemID, playerItems, playerItemsN);
                    if (slot >= 0 && slot < 28)
                        //tradeItem(itemID, slot, 1);
                        offeredItems.add(new GameItem(itemID, 1));
                    deleteItem(itemID, slot, amount);
                }
            }
        }
        resetItems(3322);
        resetTItems(3415);
        other.resetOTItems(3416);
        send(new SendString("", 3431));
        other.send(new SendString("", 3431));
    }

    /* Shops */
    public void sellItem(int itemID, int fromSlot, int amount) {
        if (itemID != playerItems[fromSlot] && playerItemsN[fromSlot] < 1) {
            return;
        }
        /* Item Values */
        int original = itemID;
        int price = (int) Math.floor(GetShopBuyValue(itemID));
        itemID = GetUnnotedItem(original) > 0 ? GetUnnotedItem(original) : itemID;
        /* Functions */
        if (!Server.shopping || tradeLocked) {
            send(new SendMessage(tradeLocked ? "You are trade locked!" : "Currently selling stuff to the store has been disabled!"));
            return;
        }
        if (price < 0 || !Server.itemManager.isTradable(itemID) || ShopHandler.ShopBModifier[MyShopID] > 2) {
            send(new SendMessage("You cannot sell " + GetItemName(itemID).toLowerCase() + " in this store."));
            return;
        }
        if (ShopHandler.ShopBModifier[MyShopID] == 2 && !ShopHandler.findDefaultItem(MyShopID, itemID)) {
            send(new SendMessage("Can't sell that item to the store!"));
            return;
        }
        int slot = -1;
        for (int i = 0; i < ShopHandler.MaxShopItems; i++) {
            if (ShopHandler.ShopItems[MyShopID][i] <= 0 && slot == -1)
                slot = i;
            else if (itemID == ShopHandler.ShopItems[MyShopID][i] - 1) {
                slot = i;
                i = ShopHandler.MaxShopItems; //Just to stop the loop!
            }
        }
        if (slot == -1) { //If we do not have a slot means the store is full!
            send(new SendMessage("Can't sell more items to the store!"));
            return;
        }
        /* Amount checks */
        boolean stack = Server.itemManager.isStackable(original);
        amount = Math.min(amount, getInvAmt(original));
        amount = Math.min(Integer.MAX_VALUE - ShopHandler.ShopItemsN[MyShopID][slot], amount);
        amount = Integer.MAX_VALUE - getInvAmt(995) < amount * price ? (Integer.MAX_VALUE - getInvAmt(995)) / price : amount;

        if (amount > 0) { // Code to check if there is any amount to sell!
            if (!stack) {
                for (int i = 0; i < amount; i++) {
                    deleteItem(original, 1);
                }
            } else {
                deleteItem(original, amount);
            }
            ShopHandler.ShopItems[MyShopID][slot] = itemID + 1;
            ShopHandler.ShopItemsN[MyShopID][slot] += amount;
            addItem(995, amount * price);
        } else
            send(new SendMessage("Could not sell anything!"));
        /* Store update! */
        UpdatePlayerShop();
        checkItemUpdate();
    }

    public int eventShopValues(int slot) {
        switch (slot) {
            case 0:
                return 8000;
            case 1:
            case 2:
                return 12000;
            case 3:
            case 4:
                return 4000;
            case 5:
                return 25000;
            case 6:
                return 15000;
        }
        return 0;
    }

    public void buyItem(int itemID, int fromSlot, int amount) {
        if (amount > 0 && itemID == (ShopHandler.ShopItems[MyShopID][fromSlot] - 1)) {
            boolean stack = Server.itemManager.isStackable(itemID);
            amount = Math.min(ShopHandler.ShopItemsN[MyShopID][fromSlot], amount);
            if (canUse(itemID)) {
                send(new SendMessage("You must be a premium member to buy this item"));
                send(new SendMessage("Visit Dodian.net to subscribe"));
                return;
            }
            if (!stack && freeSlots() < 1) {
                send(new SendMessage("Not enough space in your inventory."));
                return;
            }

            int currency = MyShopID == 55 ? 11997 : 995;
            int TotPrice2 = MyShopID == 55 ? eventShopValues(fromSlot) : (int) Math.floor(GetShopSellValue(itemID));
            TotPrice2 = MyShopID >= 7 && MyShopID <= 11 ? (int) (TotPrice2 * 1.5) : TotPrice2;
            int coins = getInvAmt(currency);
            amount = amount * TotPrice2 > coins ? coins / TotPrice2 : amount;
            if (amount == 0) {
                send(new SendMessage("You don't have enough " + GetItemName(currency).toLowerCase()));
                return;
            }
            if (!stack) {
                for (int i = amount; i > 0; i--) {
                    if (freeSlots() == 0) {
                        send(new SendMessage("Not enough space in your inventory."));
                        return;
                    }
                    if (addItem(itemID, 1)) {
                        deleteItem(currency, TotPrice2);
                        ShopHandler.ShopItemsN[MyShopID][fromSlot] -= 1;
                        if ((fromSlot + 1) > ShopHandler.ShopItemsStandard[MyShopID] && ShopHandler.ShopItemsN[MyShopID][fromSlot] <= 0) {
                            ShopHandler.resetAnItem(MyShopID, fromSlot);
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
                    ShopHandler.ShopItemsN[MyShopID][fromSlot] -= amount;
                    if ((fromSlot + 1) > ShopHandler.ShopItemsStandard[MyShopID] && ShopHandler.ShopItemsN[MyShopID][fromSlot] <= 0) {
                        ShopHandler.resetAnItem(MyShopID, fromSlot);
                    }
                } else
                    return;
            }
            /* Store update! */
            UpdatePlayerShop();
            checkItemUpdate();
        }
    }

    public void UpdatePlayerShop() {
        for (int i = 1; i < Constants.maxPlayers; i++) {
            if (net.dodian.uber.game.systems.world.player.PlayerRegistry.players[i] != null) {
                if (net.dodian.uber.game.systems.world.player.PlayerRegistry.players[i].isShopping() && net.dodian.uber.game.systems.world.player.PlayerRegistry.players[i].MyShopID == MyShopID
                        && i != getSlot()) {
                    ((Client) net.dodian.uber.game.systems.world.player.PlayerRegistry.players[i]).checkItemUpdate();
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
        String ItemName = GetItemName(ItemID);
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
        String checkName = GetItemName(ItemID).toLowerCase();
        if (checkName.endsWith("arrow") || checkName.endsWith("hat") || (checkName.endsWith("axe") && !checkName.startsWith("battle")))
            return 1;
        String ItemName = GetItemName(ItemID);
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
        String ItemName = GetItemName(ItemID);
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
        String ItemName = GetItemName(ItemID);
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
        int configValue = 0;
        for (int i = 0; i < duelLine.length; i++) {
            if (duelRule[i]) {
                send(new SendString(/* "@red@" + */duelNames[i], duelLine[i]));
                configValue += stakeConfigId[duelRuleConfigIds[i]];
            } else {
                send(new SendString(/* "@gre@" + */duelNames[i], duelLine[i]));
            }
        }
        for (int i = 0; i < duelBodyRules.length; i++) {
            if (duelBodyRules[i])
                configValue += stakeConfigId[i];
        }
        varbit(286, configValue);
    }

    public void DuelVictory() {
        Client other = getClient(duel_with);
        if (validClient(duel_with)) {
            send(new SendMessage("You have defeated " + other.getPlayerName() + "!"));
            send(new SendString("" + other.determineCombatLevel(), 6839));
            send(new SendString(other.getPlayerName(), 6840));
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
        if (stake) {
            showInterface(6733);
        }
        heal(getMaxHealth());
        getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);

    }

    public void itemsToVScreen_old() {
        send(new ItemsToVScreen(otherOfferedItems));
    }

    public void refreshDuelScreen() {
        Client other = getClient(duel_with);
        if (!validClient(duel_with)) {
            return;
        }
        
        // Send our offered items to interface 6669
        send(new DuelItemsUpdate(6669, offeredItems, true));
        
        // Send other player's offered items to interface 6670  
        send(new DuelItemsUpdate(6670, other.offeredItems, true));
    }

    public void stakeItem(int itemID, int fromSlot, int amount) {
        if (!net.dodian.uber.game.systems.interaction.PlayerTickThrottleService.tryAcquireMs(this, net.dodian.uber.game.systems.interaction.PlayerTickThrottleService.DUEL_CONFIRM_STAGE_ONE, 200L) || !canOffer) {
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
        if (Server.itemManager.isStackable(itemID)) {
            boolean inTrade = false;
            for (GameItem item : offeredItems) {
                if (item.getId() == itemID) {
                    inTrade = true;
                    item.addAmount(amount);
                    deleteItem(itemID, fromSlot, amount);
                    break;
                }
            }
            if (!inTrade) {
                offeredItems.add(new GameItem(itemID, amount));
                deleteItem(itemID, fromSlot, amount);
            }
        } else {
            for (int a = 1; a <= amount; a++) {
                if (a == 1) {
                    offeredItems.add(new GameItem(itemID, 1));
                    deleteItem(itemID, fromSlot, amount);
                } else {
                    int slot = findItem(itemID, playerItems, playerItemsN);
                    if (slot >= 0 && slot < 28)
                        //tradeItem(itemID, slot, 1);
                        offeredItems.add(new GameItem(itemID, 1));
                    deleteItem(itemID, slot, amount);
                }
            }
        }
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
        if (!net.dodian.uber.game.systems.interaction.PlayerTickThrottleService.tryAcquireMs(this, net.dodian.uber.game.systems.interaction.PlayerTickThrottleService.DUEL_CONFIRM_STAGE_TWO, 200L)) {
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
        boolean found = false;
        for (GameItem item : offeredItems) {
            if (item.getId() == itemID) {
                if (item.isStackable()) {
                    if (amount < item.getAmount())
                        offeredItems.set(fromSlot, new GameItem(item.getId(), item.getAmount() - amount));
                    else
                        offeredItems.remove(item);
                    found = true;
                } else {
                    if (amount == 1) {
                        offeredItems.remove(item);
                        found = true;
                    } else {
                        offeredItems.remove(item);
                        addItem(itemID, 1);
                        amount--;
                    }
                }
                if (found) { //If found add item to inventory!
                    addItem(itemID, amount);
                    break;
                }
            }
        }
        duelConfirmed = false;
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
        showInterface(2808);
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
        showInterface(2808);
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
        PlayerActionCancellationService.cancel(this, PlayerActionCancelReason.MANUAL_RESET, full, false, false, true);
    }

    public void resetAction() {
        resetAction(true);
    }

    public void replaceDoors() {
        for (int d = 0; d < DoorHandler.doorX.length; d++) {
            if (DoorHandler.doorX[d] > 0 && DoorHandler.doorHeight[d] == getPosition().getZ()
                    && Math.abs(DoorHandler.doorX[d] - getPosition().getX()) <= 120
                    && Math.abs(DoorHandler.doorY[d] - getPosition().getY()) <= 120) {
                if (distanceToPoint(DoorHandler.doorX[d], DoorHandler.doorY[d]) < 50) {
                    ReplaceObject(DoorHandler.doorX[d], DoorHandler.doorY[d], DoorHandler.doorId[d], DoorHandler.doorFace[d], 0);
                }
            }
        }
    }

    public void modYell(String msg) {
        for (int i = 0; i < PlayerHandler.players.length; i++) {
            Client p = (Client) net.dodian.uber.game.systems.world.player.PlayerRegistry.players[i];
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
        inTrade = true;
        tradeRequested = false;
        resetItems(3322);
        resetTItems(3415);
        resetOTItems(3416);
        send(new InventoryInterface(3323, 3321)); // trading window + bag
        String out = net.dodian.uber.game.systems.world.player.PlayerRegistry.players[trade_reqId].getPlayerName();
        if (net.dodian.uber.game.systems.world.player.PlayerRegistry.players[trade_reqId].playerRights == 1) {
            out = "@cr1@" + out;
        } else if (net.dodian.uber.game.systems.world.player.PlayerRegistry.players[trade_reqId].playerRights == 2) {
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
        Client other = getClient(trade_reqId);
        /* Prevent a dupe? */
        inTrade = false;
        if (tellOther && validClient(trade_reqId))
            other.declineTrade(false);
        /* Clear the trade! */
        for (GameItem item : offeredItems) {
            if (item.getAmount() > 0) {
                if (Server.itemManager.isStackable(item.getId())) {
                    addItem(item.getId(), item.getAmount());
                } else {
                    for (int i = 0; i < item.getAmount(); i++) {
                        addItem(item.getId(), 1);
                    }
                }
            }
        }
        send(new RemoveInterfaces());
        canOffer = true;
        tradeConfirmed = false;
        tradeConfirmed2 = false;
        offeredItems.clear();
        trade_reqId = -1;
        faceTarget(trade_reqId);
        checkItemUpdate();
    }

    public boolean validClient(int index) {
        Client p = (Client) net.dodian.uber.game.systems.world.player.PlayerRegistry.players[index];
        return p != null && !p.disconnected && p.dbId > 0;
    }

    public Client getClient(int index) {
        return index < 0 ? null : ((Client) net.dodian.uber.game.systems.world.player.PlayerRegistry.players[index]);
    }

    public void tradeReq(int id) {
        facePlayer(id);
        if (!Server.trading) {
            send(new SendMessage("Trading has been temporarily disabled"));
            return;
        }
        for (int a = 0; a < PlayerHandler.players.length; a++) {
            Client o = getClient(a);
            if (a != getSlot() && validClient(a) && o.dbId > 0 && o.dbId == dbId) {
                logout();
            }
        }
        Client other = (Client) net.dodian.uber.game.systems.world.player.PlayerRegistry.players[id];
        String tradeBlockMessage = net.dodian.uber.game.systems.interaction.PlayerInteractionGuardService.tradeBlockMessage(this, other);
        if (tradeBlockMessage != null) {
            send(new SendMessage(tradeBlockMessage));
            return;
        }
        if (validClient(trade_reqId)) {
            setFocus(other.getPosition().getX(), other.getPosition().getY());
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
            openTrade();
            other.openTrade();
        } else if (validClient(trade_reqId) && !inTrade && net.dodian.uber.game.systems.interaction.PlayerTickThrottleService.tryAcquireMs(this, net.dodian.uber.game.systems.interaction.PlayerTickThrottleService.TRADE_REQUEST, 1000L)) {
            tradeRequested = true;
            trade_reqId = id;
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
                    offerItems.append(GetItemName(item.getId()));
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
                    otherOfferItems.append(GetItemName(item.getId()));
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
        if (validClient(trade_reqId)) {
            try {
                CopyOnWriteArrayList<GameItem> offerCopy = new CopyOnWriteArrayList<>();
                CopyOnWriteArrayList<GameItem> otherOfferCopy = new CopyOnWriteArrayList<>();
                for (GameItem item : other.offeredItems) {
                    otherOfferCopy.add(new GameItem(item.getId(), item.getAmount()));
                }
                for (GameItem item : offeredItems) {
                    offerCopy.add(new GameItem(item.getId(), item.getAmount()));
                }
                for (GameItem item : other.offeredItems) {
                    if (item.getId() > 0) {
                        addItem(item.getId(), item.getAmount());
                        println("TradeConfirmed, item=" + item.getId());
                    }
                }
                if (this.dbId > other.dbId)
                    TradeLog.recordTrade(dbId, other.dbId, offerCopy, otherOfferCopy, true);
                send(new RemoveInterfaces());
                tradeResetNeeded = true;
                PlayerDeferredLifecycleService.signalTradeFinalizeReady(this);
                saveStats(PlayerSaveReason.TRADE, false, false);
                tradeSuccessful = true;
                faceTarget(-1);
                checkItemUpdate();
                //System.out.println("trade succesful");
            } catch (Exception e) {
                logger.warn("Giving items failed for {}", getPlayerName(), e);
            }
        }
    }

    public void resetTrade() {
        offeredItems.clear();
        inTrade = false;
        trade_reqId = 0;
        canOffer = true;
        tradeConfirmed = false;
        tradeConfirmed2 = false;
        send(new RemoveInterfaces());
        tradeResetNeeded = false;
        send(new SendString("Are you sure you want to make this trade?", 3535));
    }

    public void duelReq(int pid) {
        facePlayer(pid);
        Client other = getClient(pid);
        String duelBlockMessage = net.dodian.uber.game.systems.interaction.PlayerInteractionGuardService.duelBlockMessage(this, other);
        if (duelBlockMessage != null) {
            send(new SendMessage(duelBlockMessage));
            return;
        }
        if (isBusy() || other.isBusy()) {
            send(new SendMessage(isBusy() ? "You are currently busy" : other.getPlayerName() + " is currently busy!"));
            return;
        }
        if (net.dodian.uber.game.systems.combat.CombatLogoutLockService.isLocked(this)
                || net.dodian.uber.game.systems.combat.CombatLogoutLockService.isLocked(other)) {
            send(new SendMessage(net.dodian.uber.game.systems.combat.CombatLogoutLockService.isLocked(this)
                    ? "You can't duel while in combat."
                    : other.getPlayerName() + " can't duel while in combat."));
            return;
        }
        if (inWildy() || other.inWildy()) {
            send(new SendMessage("You cant duel in the wilderness!"));
            return;
        }
        if (!Server.dueling) {
            send(new SendMessage("Dueling has been temporarily disabled"));
            return;
        }
        for (int a = 0; a < PlayerHandler.players.length; a++) {
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
        RefreshDuelRules();
        refreshDuelScreen();
        inDuel = true;
        Client other = getClient(duel_with);
        send(new SendString("Dueling with: " + other.getPlayerName() + " (level-" + other.determineCombatLevel() + ")", 6671));
        send(new SendString("", 6684));
        resetItems(3322);
        send(new InventoryInterface(6575, 3321));
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
        duelRequested = false;
        duelConfirmed = false;
        duelConfirmed2 = false;
        duelFight = false;
        for (GameItem item : offeredItems) {
            if (item.getAmount() < 1) {
                continue;
            }
            println("adding " + item.getId() + ", " + item.getAmount());
            if (Server.itemManager.isStackable(item.getId()) || Server.itemManager.isNote(item.getId())) {
                addItem(item.getId(), item.getAmount());
            } else {
                addItem(item.getId(), 1);
            }
        }
        offeredItems.clear();
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
        send(new InventoryInterface(6412, 3321)); // Duel confirm
        Client other = getClient(duel_with);
        if (!validClient(duel_with)) {
            declineDuel();
        }
        /* Reset duel item containers! */
        send(new ClearItemContainer(6509, 1));
        send(new ClearItemContainer(6507, 1));
        send(new ClearItemContainer(6502, 1));
        send(new ClearItemContainer(6508, 1));
        
        /* Send duel items! */
        send(DuelConfirmItems.forOwnItems(offeredItems, other.offeredItems));
        send(DuelConfirmItems.forOtherItems(offeredItems, other.offeredItems));
        send(new SendString(offeredItems.isEmpty() ? "Absolutely nothing!" : "", 6516));
        send(new SendString(other.offeredItems.isEmpty() ? "Absolutely nothing!" : "", 6517));

        send(new SendString("Movement will be disabled", 8242));
        for (int i = 8243; i <= 8253; i++) {
            send(new SendString("", i));
        }
        send(new SendString("Hitpoints will be restored", 8250));
        send(new SendString("", 6571));
    }

    public void startDuel() {
        send(new RemoveInterfaces());
        canAttack = false;
        canOffer = false;
        duelFight = true;
        prayers.reset();
        addEffectTime(2, 0); //Need to reset this for dueling!
        GetBonus(true); //Set bonus due to blessing!
        for (int i = 0; i < boostedLevel.length; i++) {
            boostedLevel[i] = 0;
            SkillProgressionService.refresh(this, Skill.getSkill(i));
        }
        Client other = getClient(duel_with);
        for (GameItem item : other.offeredItems) {
            otherOfferedItems.add(new GameItem(item.getId(), item.getAmount()));
        }
        otherdbId = other.dbId;
        final Client player = this;
        final int[] countDown = {7};
        player.requestForceChat("It is time to D-D-D-DUEL!");
        GameEventScheduler.runRepeatingMs(600, () -> {
            countDown[0]--;
            if (countDown[0] > 0 && countDown[0] % 2 == 0) {
                player.requestForceChat("" + (countDown[0] / 2));
                return true;
            }
            if (countDown[0] < 1) {
                player.requestForceChat("Fight!");
                player.canAttack = true;
                return false;
            }
            return true;
        });
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

    public void varbit(int id, int value) {
        // Preserve old signature but delegate to the new Netty-based packet implementation.
        send(new SetVarbit(id, value));
    }

    public boolean toggleDuelRule(int ruleIndex) {
        Client other = getClient(duel_with);
        if (other == null || ruleIndex < 0 || ruleIndex >= duelRule.length
                || !net.dodian.uber.game.systems.interaction.PlayerTickThrottleService.tryAcquireMs(this, net.dodian.uber.game.systems.interaction.PlayerTickThrottleService.DUEL_RULES, 800L)) {
            return false;
        }
        if (inDuel && !duelFight && !duelConfirmed2 && !other.duelConfirmed2 && !(duelConfirmed && other.duelConfirmed)) {
            duelRule[ruleIndex] = !duelRule[ruleIndex];
            other.duelRule[ruleIndex] = duelRule[ruleIndex];
            duelConfirmed = false;
            other.duelConfirmed = false;
            send(new SendString("", 6684));
            other.send(new SendString("", 6684));
            RefreshDuelRules();
            other.RefreshDuelRules();
            return true;
        }
        return false;
    }

    public boolean toggleDuelBodyRule(int ruleIndex) {
        Client other = getClient(duel_with);
        if (other == null || ruleIndex < 0 || ruleIndex >= duelBodyRules.length
                || !net.dodian.uber.game.systems.interaction.PlayerTickThrottleService.tryAcquireMs(this, net.dodian.uber.game.systems.interaction.PlayerTickThrottleService.DUEL_BODY_RULES, 400L)) {
            return false;
        }
        if (inDuel && !duelFight && !duelConfirmed2 && !other.duelConfirmed2 && !(duelConfirmed && other.duelConfirmed)) {
            duelBodyRules[ruleIndex] = !duelBodyRules[ruleIndex];
            other.duelBodyRules[ruleIndex] = duelBodyRules[ruleIndex];
            duelConfirmed = false;
            other.duelConfirmed = false;
            send(new SendString("", 6684));
            other.send(new SendString("", 6684));
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
        for (Client c : PlayerHandler.playersOnline.values()) {
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
        if (PlayerHandler.playersOnline.containsKey(friend)) {
            Client to = PlayerHandler.playersOnline.get(friend);
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
            Client player = PlayerHandler.playersOnline.get(f.name);
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
                Client player = PlayerHandler.playersOnline.get(f.name);
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
            Client player = PlayerHandler.playersOnline.get(name);
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
        if (time > 0) walkBlock = System.currentTimeMillis() + time;
        AddToCords(X, Y, false);
    }

    public void AddToRunCords(int X, int Y, long time) {
        newWalkCmdIsRunning = true;
        if (time > 0) walkBlock = System.currentTimeMillis() + time;
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
        target = null;
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
            // The wilderness overlay uses walkable interface id 197.
            // Keep the cached walkable-interface state in sync so that
            // setWalkableInterface(6673) can properly restore the normal UI.
            currentWalkableInterface = 197;
        }
    }

    public void updatePlayerDisplay() {
        String serverName = getGameWorldId() == 1 ? "Uber Server 3.0" : "Beta World";
        String text = serverName + " (" + PlayerHandler.getPlayerCount() + " online)";
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
        for (Entity e : getDamage().keySet()) {
            if (getDamage().get(e) > highestDmg) {
                highestDmg = getDamage().get(e);
                slot = e.getSlot();
            }
        }
        getDamage().clear();
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
            if (!net.dodian.uber.game.systems.interaction.PlayerTickThrottleService.tryAcquireMs(this, net.dodian.uber.game.systems.interaction.PlayerTickThrottleService.DUEL_ACCEPT_WIN, 1000L)) {
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
            for (GameItem item : otherOfferedItems) {
                if (item.getId() > 0 && item.getAmount() > 0) {
                    if (Server.itemManager.isStackable(item.getId())) {
                        addItem(item.getId(), item.getAmount());
                    } else {
                        addItem(item.getId(), 1);
                    }
                }
            }
            for (GameItem item : offeredItems) {
                if (item.getId() > 0 && item.getAmount() > 0) {
                    addItem(item.getId(), item.getAmount());
                }
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
        stakeConfigId[11] = 16; // No ranging
        stakeConfigId[12] = 32; // No melee
        stakeConfigId[13] = 64; // No magic
        stakeConfigId[14] = 8192; // no gear change
        stakeConfigId[15] = 4096; // fun weapons
        stakeConfigId[16] = 1; // no retreat
        stakeConfigId[17] = 128; // No drinks
        stakeConfigId[18] = 256; // No food
        stakeConfigId[19] = 512; // No prayer
        stakeConfigId[20] = 2; // movement
        stakeConfigId[21] = 1024; // obstacles
        stakeConfigId[22] = -1; // No specials
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
        // Create and send a new DuelArmourUpdate packet with the current equipment
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
        send(new RemoveInterfaces());
        int slot = -1;
        for (int s = 0; s < playerItems.length; s++) {
            if ((playerItems[s] - 1) == 2996) {
                slot = s;
                break;
            }
        }
        if (slot == -1) {
            send(new SendMessage("You have no agility tickets!"));
        } else if (playerItemsN[slot] < 10) {
            send(new SendMessage("You must hand in at least 10 tickets at once"));
        } else {
            int amount = playerItemsN[slot];
            SkillProgressionService.gainXp(this, amount * 700, Skill.AGILITY);
            send(new SendMessage("You exchange your " + amount + " agility tickets"));
            deleteItem(2996, playerItemsN[slot]);
            checkItemUpdate();
        }
    }

    public Prayers getPrayerManager() {
        return prayers;
    }

    public void checkBow() {
        int weaponId = getEquipment()[Equipment.Slot.WEAPON.getId()];
        usingBow = bowWeapon(weaponId);
    }

    public boolean bowWeapon(int weaponId) {
        boolean bow = false;
        if (net.dodian.uber.game.content.skills.fletching.FletchingDefinitions.isBowWeapon(weaponId)) {
            bow = true;
        }
        if (weaponId == 839 || weaponId == 841 || weaponId == 4212 || weaponId == 6724 || weaponId == 20997 ||
                weaponId == 11235 || weaponId == 4734 || (weaponId >= 12765 && weaponId <= 12768))
            bow = true;
        return bow;
    }

    public boolean checkInv = false;

    public void openUpOtherInventory(String player) {
        if (IsBanking || isShopping() || duelFight) {
            send(new SendMessage("Please finish with what you are doing!"));
            return;
        }
        ArrayList<GameItem> otherInv = new ArrayList<>();
        if (PlayerHandler.getPlayer(player) != null) { //Online check
            Client other = (Client) PlayerHandler.getPlayer(player);
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
        if (PlayerHandler.getPlayer(player) != null) { //Online check
            Client other = (Client) PlayerHandler.getPlayer(player);
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
                if (item.isTaken() || dbId != item.playerId || !GoodDistance(getPosition().getX(), getPosition().getY(), item.x, item.y, 104))
                    continue;
                send(new RemoveGroundItem(new GameItem(item.id, item.amount), new Position(item.x, item.y, item.z)));
                send(new CreateGroundItem(new GameItem(item.id, item.amount), new Position(item.x, item.y, item.z)));
            }
        /* Tradeable items prio 2! */
        if (!Ground.tradeable_items.isEmpty())
            for (GroundItem item : Ground.tradeable_items) {
                if (item.isTaken() || (item.playerId != dbId && !item.isVisible()) || !GoodDistance(getPosition().getX(), getPosition().getY(), item.x, item.y, 104))
                    continue;
                send(new RemoveGroundItem(new GameItem(item.id, item.amount), new Position(item.x, item.y, item.z)));
                send(new CreateGroundItem(new GameItem(item.id, item.amount), new Position(item.x, item.y, item.z)));
            }
        /* Static ground items prio last! */
        if (!Ground.ground_items.isEmpty())
            for (GroundItem item : Ground.ground_items) {
                if (item.isTaken() || !item.visible || !GoodDistance(getPosition().getX(), getPosition().getY(), item.x, item.y, 104))
                    continue;
                send(new RemoveGroundItem(new GameItem(item.id, item.amount), new Position(item.x, item.y, item.z)));
                send(new CreateGroundItem(new GameItem(item.id, item.amount), new Position(item.x, item.y, item.z)));
            }
    }



    @Deprecated
    public void removeItemsFromPlayer(String user, int id, int amount) {
        int totalItemRemoved = 0;
        if (PlayerHandler.getPlayer(user) != null) { //Online check
            Client other = (Client) PlayerHandler.getPlayer(user);
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
                    if (other.getEquipmentN()[i] <= 0)
                        other.getEquipment()[i] = -1;
                    other.deleteequiment(0, i);
                }
            }
            if (totalItemRemoved > 0) { //Update items only if there is any deleted!
                send(new SendMessage("Finished deleting " + totalItemRemoved + " of " + GetItemName(id).toLowerCase()));
                other.checkItemUpdate();
                other.getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
            } else
                send(new SendMessage("The user '" + user + "' did not had any " + GetItemName(id).toLowerCase()));
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
                            send(new SendMessage("Finished deleting " + result.getTotalItemRemoved() + " of " + GetItemName(id).toLowerCase()));
                        } else {
                            send(new SendMessage("The user " + user + " did not had any " + GetItemName(id).toLowerCase()));
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
        showInterface(802);
    }

    public void transport(Position pos) {
        resetActionTeleport();
        moveTo(pos.getX(), pos.getY(), pos.getZ());
        teleportToX = pos.getX();
        teleportToY = pos.getY();
        teleportToZ = pos.getZ();
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
        int[] posTrigger = {1, 3, 4, 7, 10, 2, 5, 6, 11}; //0-4 is to something! rest is to the Catherby
        int[][] travel = {
                {3057, 2803, 3421, 0}, //Catherby
                {3058, -1, -1, 0}, //Mountain aka Trollheim?
                {3059, 3511, 3506, 0}, //Castle aka Canifis
                {3060, 3274, 2798, 0}, //Tent aka Sophanem
                {3056, 2863, 2971, 0}, //Tree aka shilo
                {48054, 2772, 3234, 0} //Totem aka Brimhaven
        };
        for (int i = 0; i < travel.length; i++)
            if (travel[i][0] == buttonId) { //Initiate the teleport!
                /* Check conditions! */
                if ((!home && i == 0) || (home && i != 0)) {
                    send(new SendMessage(!home ? "You are already here!" : "Please select Catherby!"));
                    return;
                }
                if (travel[i][1] == -1) {
                    send(new SendMessage("This will lead you to nothing!"));
                    return;
                }
                if (i > 0 && !getTravel(i - 1)) {
                    DialogueService.setCompatDialogueId(this, 48054);
                    DialogueService.setDialogueSent(this, false);
                    return;
                }
                /* Set configs! */
                final int pos = i;
                varbit(153, home ? posTrigger[checkPos + 3] : posTrigger[i - 1]);
                travelInitiate = true;
                GameEventScheduler.runLaterMs(1800, () -> {
                    if (!disconnected) {
                        transport(new Position(travel[pos][1], travel[pos][2], 0));
                        send(new RemoveInterfaces());
                        travelInitiate = false;
                    }
                });
            }
    }

    public int refundSlot = -1;
    public ArrayList<RewardItem> rewardList = new ArrayList<>();

    public void setRefundList() {
        rewardList.clear();
        try (Connection conn = getDbConnection();
             Statement stm = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE)) {
            String query = "SELECT * FROM " + DbTables.GAME_REFUND_ITEMS + " WHERE receivedBy='" + dbId + "' AND claimed IS NULL ORDER BY date ASC";
            ResultSet result = stm.executeQuery(query);
            while (result.next()) {
                rewardList.add(new RewardItem(result.getInt("item"), result.getInt("amount")));
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
            text[i + 1] = "Claim " + rewardList.get(slot + i).getAmount() + " of " + GetItemName(rewardList.get(slot + i).getId());
        text[position + 1] = text.length < 6 && slot == 0 ? "Close" : text.length == 6 ? "Next" : "Previous";
        if (text.length == 6)
            text[position + 2] = slot == 0 ? "Close" : "Previous";
        showPlayerOption(text);
    }

    public void reclaim(int position) {
        int slot = refundSlot + position;
        try (Connection conn = getDbConnection();
             Statement stm = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
            String query = "SELECT * FROM " + DbTables.GAME_REFUND_ITEMS + " WHERE receivedBy='" + dbId + "' AND claimed IS NULL ORDER BY date ASC";
            ResultSet result = stm.executeQuery(query);
            String date = "";
            RewardItem item = rewardList.get(slot - 1);
            while (result.next() && date.isEmpty()) {
                if (result.getRow() == slot) {
                    date = result.getString("date");
                }
            }
            stm.executeUpdate("UPDATE " + DbTables.GAME_REFUND_ITEMS + " SET claimed='" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "' where date='" + date + "'");
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
        return Skill.enabledSkills()
                .mapToInt(skill -> Skills.getLevelForExperience(getExperience(skill)))
                .sum() + (int) Skill.disabledSkills().count();
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
