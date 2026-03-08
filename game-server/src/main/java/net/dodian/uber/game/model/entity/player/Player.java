package net.dodian.uber.game.model.entity.player;

import net.dodian.uber.game.Constants;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.model.EntityType;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.UpdateFlag;
import net.dodian.uber.game.model.WalkToTask;
import net.dodian.uber.game.model.entity.Entity;
import net.dodian.uber.game.model.entity.npc.Npc;
import net.dodian.uber.game.model.entity.npc.NpcData;
import net.dodian.uber.game.model.entity.npc.NpcDrop;
import net.dodian.uber.game.model.item.Equipment;
import net.dodian.uber.game.model.chunk.Chunk;
import net.dodian.uber.game.model.chunk.ChunkRepository;
import net.dodian.uber.game.model.music.RegionSong;
import net.dodian.uber.game.model.object.GlobalObject;
import net.dodian.uber.game.model.object.Object;
import net.dodian.uber.game.model.player.content.Skillcape;
import net.dodian.uber.game.netty.listener.out.InventoryInterface;
import net.dodian.uber.game.netty.listener.out.SendMessage;
import net.dodian.uber.game.netty.listener.out.SendSideTab;
import net.dodian.uber.game.netty.listener.out.SendString;
import net.dodian.uber.game.model.player.skills.Skill;
import net.dodian.uber.game.model.player.skills.Skills;
import net.dodian.uber.game.model.player.skills.prayer.Prayers;
import net.dodian.uber.game.model.player.skills.slayer.SlayerTask;
import net.dodian.uber.game.model.player.skills.thieving.PyramidPlunder;
import net.dodian.uber.game.content.dialogue.DialogueService;
import net.dodian.uber.game.party.Balloons;
import net.dodian.uber.game.party.RewardItem;
import net.dodian.uber.game.persistence.player.PlayerSaveSegment;
import net.dodian.uber.game.skills.mining.MiningState;
import net.dodian.uber.game.skills.woodcutting.WoodcuttingState;
import net.dodian.uber.game.runtime.interaction.ActiveInteraction;
import net.dodian.uber.game.runtime.interaction.InteractionIntent;
import net.dodian.uber.game.runtime.combat.CombatCancellationReason;
import net.dodian.uber.game.runtime.combat.CombatTargetState;
import net.dodian.uber.game.runtime.action.PlayerActionCancelReason;
import net.dodian.uber.game.runtime.action.PlayerActionType;
import net.dodian.uber.game.runtime.scheduler.QueueTaskHandle;
import net.dodian.uber.game.runtime.tasking.GameTaskSet;
import net.dodian.utilities.Misc;
import net.dodian.utilities.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public abstract class Player extends Entity {
    private static final Logger logger = LoggerFactory.getLogger(Player.class);
    public boolean yellOn = true, genie = false, antique = false, instaLoot = false;
    public long longName = 0;
    public int wildyLevel = 0;
    public long lastAction = 0, lastMagic = 0;
    public long lastPickAction = 0, lastFishAction = 0;
    public boolean premium = false, randomed = false, genieCombatFlag = false;
    public int playerGroup = 3, latestNews = 0, dbId = -1, questPage = 0, playerRights; //Online stuff!
    public int[] playerLooks = new int[13];
    public boolean saveNeeded = true, lookNeeded = false, discord = false;
    private final PlayerAccountState accountState = new PlayerAccountState(this);
    private final PlayerInteractionState interactionState = new PlayerInteractionState();
    private final PlayerMovementState movementState = new PlayerMovementState(this);
    private final PlayerUpdateState updateState = new PlayerUpdateState(this);
    int lastCombat = 0, combatTimer = 0, snareTimer = 0, stunTimer = 0;
    public long start = 0, lastPlayerCombat = 0;
    public static int id = -1, localId = -1;
    public boolean busy = false, invis = false;
    public String[] boss_name = {"Dad", "Abyssal_Guardian", "San_Tojalon", "Black_Knight_Titan", "Jungle_Demon", "Ungadulu", "Nechryael", "Ice_Queen",
            "King_Black_Dragon", "Head_Mourners", "Black_Demon", "Dagannoth_Prime", "Dwayne", "TzTok-Jad", "Kalphite_queen", "Kalphite_king", "Venenatis"};
    public int[] boss_amount = new int[boss_name.length];
    public int duelStatus = -1, iconTimer = 6; // duelStatus 0 = Requesting duel, 1 = in duel screen, 2 = waiting for other player to accept, 3 = in duel, 4 = won
    public String forcedChat = "";
    public int headIcon = -1, skullIcon = -1, customCombat = -1;
    private WalkToTask walkToTask;
    public boolean IsPMLoaded = false;
    public int playerIsMember = 1;
    public int[] playerBonus = new int[12];
    public int fightType = 1; //What it should do!
    public fightStyle weaponStyle = fightStyle.PUNCH;
    public boolean IsCutting = false, IsAnvil = false;
    public boolean isFiremaking = false;
    public PyramidPlunder getPlunder = new PyramidPlunder(((Client) this));
    public int MyShopID = -1;
    public int NpcDialogue = 0, NpcTalkTo = 0, NpcWanneTalk = 0;
    public boolean IsBanking = false, isPartyInterface = false, checkBankInterface, bankStyleViewOpen = false, NpcDialogueSend = false;
    private Entity.hitType hitType, hitType2 = Entity.hitType.STANDARD;
    public boolean isNpc, morph = false;
    public boolean initialized = false, disconnected = false, isKicked = false;
    public boolean isActive = false, debug = false;
    public int actionTimer = 0;
    public String connectedFrom = "";
    public int ip = 0;
    public String UUID = "";
    public boolean takeAsNote = false;
    public String playerName = null, playerPass = null;
    public PlayerHandler handler = null;
    public int maxItemAmount = Integer.MAX_VALUE;
    public int[] playerItems = new int[28];
    public int[] playerItemsN = new int[28];
    public int playerBankSize = 1300;
    public int[] bankItems = new int[playerBankSize];
    public int[] bankItemsN = new int[playerBankSize];
    public int pHairC, pTorsoC, pLegsC, pFeetC, pSkinC;
    private final int[] playerEquipment = new int[14];
    private final int[] playerEquipmentN = new int[14];
    public int maxHealth = 10, currentHealth = 10;
    public int maxPrayer = 1, currentPrayer = 1;
    public final static int maxPlayerListSize = Constants.maxPlayers;
    public Player[] playerList = new Player[maxPlayerListSize]; // To remove -Dashboard
    public int playerListSize = 0;
    public Set<Player> playersUpdating = new LinkedHashSet<>(255);
    private final Set<Npc> localNpcs = new LinkedHashSet<>(254);
    private long localPlayerMembershipRevision = 0L;
    private long localNpcMembershipRevision = 0L;
    private Chunk currentChunk;
    private ChunkRepository chunkRepository;
    public boolean loaded = false;
    public int[] newWalkCmdX = new int[WALKING_QUEUE_SIZE];
    public int[] newWalkCmdY = new int[WALKING_QUEUE_SIZE];
    public int[] tmpNWCX = new int[WALKING_QUEUE_SIZE];
    public int[] tmpNWCY = new int[WALKING_QUEUE_SIZE];
    public int newWalkCmdSteps = 0;
    public boolean newWalkCmdIsRunning = false;
    public int[] travelBackX = new int[WALKING_QUEUE_SIZE];
    public int[] travelBackY = new int[WALKING_QUEUE_SIZE];
    public int numTravelBackSteps = 0;
    public int m4001 = 0;
    public int m4002 = 0;
    public int m4003 = 0;
    public int m4004 = 0;
    public int m4005 = 0;
    public int m4006 = 0;
    public int m4007 = 0;
    //Runecrafting
    public int[] runePouchesAmount = {0, 0, 0, 0};
    public int[] runePouchesLevel = {1, 20, 40, 60};
    public int[] runePouchesMaxAmount = {4, 7, 10, 13};
    //Agility
    public boolean UsingAgility = false;
    public int agilityCourseStage = 0;
    public long walkBlock = 0;
    public boolean xLog = false;
    public ArrayList<RewardItem> offeredPartyItems = new ArrayList<>();
    /*
     Entity(1 = player, 2 = npc, 3 = object, 4 = itemInv)
     slot
     id
     functionId (1 = use item on entity, 2 = click entity)
     */
    public ArrayList<Integer> playerPotato = new ArrayList<>();
    //Herblore service
    public int herbMaking = -1;
    public ArrayList<RewardItem> herbOptions = new ArrayList<>();
    //Slayer
    public int unlockLength = 2;
    public int lastRecoverEffect = 0, lastRecover = 4;
    public int[] boostedLevel = new int[21];
    public int chestEvent = 0;
    public boolean chestEventOccur = false;
    public ArrayList<Integer> effects = new ArrayList<>();
    public int dailyLogin = 1;
    public ArrayList<String> dailyReward = new ArrayList<>();
    public int staffSize = 5;
    private final PlayerStats stats = new PlayerStats(this);
    private final PlayerAppearanceState appearanceState = new PlayerAppearanceState(this);
    private final PlayerProgressState progressState = new PlayerProgressState(this);
    private final PlayerCombatState combatState = new PlayerCombatState(this);

    public Player(int slot) {
        super(new Position(-1, -1, 0), slot, Entity.Type.PLAYER);
        // Delegated state components are not safe to call from field initializers.
        currentHealth = maxHealth;
        currentPrayer = maxPrayer;
        movementState.initializeRegionState();
        resetWalkingQueue();
    }

    public boolean isShopping() {
        return MyShopID != -1;
    }

    public void bossCount(String name, int amount) {
        progressState.bossCount(name, amount);
    }

    public void defaultDailyReward(Client c) {
        accountState.defaultDailyReward(c);
    }
    public void battlestavesData() {
        accountState.battlestavesData();
    }

    public void addEffectTime(int slot, int ticks) {
        if(effects.size() - 1 < slot) { //Set default values!
            for(int i = effects.size(); i < slot + 1; i++)
                effects.add(i, i == slot ? ticks : -1);
        } else effects.set(slot, ticks);
    }
    public void changeEffectTime() {
        if(effects.isEmpty()) {
            return;
        }
        Client c = ((Client) this);
        boolean inDesert = getPositionName(getPosition()) == positions.DESERT && !c.UsingAgility;

        for(int i = 0; i < effects.size(); i++) {
            if(effects.get(i) > 0 && (i != 0 || inDesert)) //Remove 1 tick from timer
                effects.set(i, effects.get(i) - 1);

            if(i == 2 && effects.get(i)%10 == 0 && effects.get(i) > 0) {
                for(int skill = 0; skill < 4; skill++) {
                    skill = skill == 3 ? 4 : skill;
                    boost(5 + (int) (Skills.getLevelForExperience(getExperience(Objects.requireNonNull(Skill.getSkill(skill)))) * 0.15), Skill.getSkill(skill));
                }
            }
            if(i == 1 && effects.get(i)%100 == 0 && effects.get(i) >= 100)
                c.send(new SendMessage("<col=702963>You have " + ((int)(effects.get(i) * 0.6) / 60) + " minutes left on your antifire potion."));
            else if(i == 1 && effects.get(i)%50 == 0 && effects.get(i) > 0 && effects.get(i) < 100) c.send(new SendMessage("<col=702963>Your antifire potion is about to expire."));

            if(i == 0 && effects.get(i) == 0) { //Desert heat
                boolean waterSource = false;
                if (c.playerHasItem(1823) || c.playerHasItem(1825) || c.playerHasItem(1827) || c.playerHasItem(1829)) {
                    waterSource = true;
                    int deleteItem = c.playerHasItem(1829) ? 1829 : c.playerHasItem(1827) ? 1827 : c.playerHasItem(1825) ? 1825 : 1823;
                    c.deleteItem(deleteItem, 1);
                    c.addItem(deleteItem + 2, 1);
                } else if (c.playerHasItem(1929)) {
                    waterSource = true;
                    c.deleteItem(1929, 1);
                    c.addItem(1925, 1);
                } else if (c.playerHasItem(1921)) {
                    waterSource = true;
                    c.deleteItem(1921, 1);
                    c.addItem(1923, 1);
                } else if (c.playerHasItem(1937)) {
                    waterSource = true;
                    c.deleteItem(1937, 1);
                    c.addItem(1935, 1);
                } else if (c.playerHasItem(4458)) {
                    waterSource = true;
                    c.deleteItem(4458, 1);
                    c.addItem(1980, 1);
                } else if (c.playerHasItem(227)) {
                    waterSource = true;
                    c.deleteItem(227, 1);
                    c.addItem(229, 1);
                }
                if (!waterSource) { //Damage player!
                    dealDamage(null, 3 + Misc.random(12), Entity.hitType.STANDARD);
                    c.send(new SendMessage("The thirst from the heat damage you!"));
                } else {
                    c.requestAnim(829, 0);
                    c.checkItemUpdate();
                    //c.send(new SendMessage("You take a sip on some water.")); //Should we add a msg when drinking?!
                }
                addEffectTime(0, 30 + Misc.random(40)); //18 - 42 seconds!
            } else if (i == 1 && effects.get(i) == 0) {
                addEffectTime(1, -1);
                c.send(new SendMessage("<col=702963>Your antifire potion has expired."));
                c.markSaveDirty(PlayerSaveSegment.EFFECTS.getMask());
            } else if(i == 2 && effects.get(i) == 0) { //Overload
                for(int skill = 0; skill < 4; skill++) {
                    skill = skill == 3 ? 4 : skill;
                    boostedLevel[skill] = 0;
                    c.refreshSkill(Skill.getSkill(skill));
                }
                addEffectTime(2, -1);
                c.send(new SendMessage("Your overload effect is now over!"));
                c.markSaveDirty(PlayerSaveSegment.EFFECTS.getMask() | PlayerSaveSegment.STATS.getMask());
            }
        }
    }
    boolean antiFireEffect() {
        return effects.size() > 1 && effects.get(1) > 0;
    }

    public void defaultCharacterLook(Client temp) {
        appearanceState.defaultCharacterLook(temp);
    }

    public enum fightStyle {
        PUNCH, KICK, BLOCK, // Unarmed
        STAB, LUNGE_STR, SLASH, CONTROLLED, // Dagger & sword
        CHOP, LUNGE, // Scimitar & longsword & 2h (Smash instead of lunge
        HACK, SMASH, // Axe & battleaxe
        POUND, PUMMEL, SPIKE, // BLOCK // Mace & warhammer
        JAB, SWIPE, FEND, /* Halberd */ IMPALE, // Pickaxe
        ACCURATE, RAPID, LONGRANGE, // Range weapons
        FLICK, LASH, DEFLECT, // Abyssal Whip
        SWIPE_CON, POUND_CON, // Spear
        BLOCK_THREE // MAUL!
    }

    void destruct() {
        updateState.releaseCachedUpdateBlock();
        cancelMiningTask();
        clearMiningState();
        cancelWoodcuttingTask();
        clearWoodcuttingState();
        interactionState.terminatePlayerTasks();
        removeFromChunk();
        getPosition().moveTo(-1, -1);
        movementState.resetTransientState();
        resetWalkingQueue();
    }

    public void setTask(String input) {
        progressState.setTask(input);
    }
    public String saveTaskAsString() {
        return progressState.saveTaskAsString();
    }
    public ArrayList<Integer> getSlayerData() {
        return progressState.getSlayerData();
    }

    private void ensureSlayerDataSize() {
        progressState.ensureSlayerDataSize();
    }

    public void setTravel(String input) {
        progressState.setTravel(input);
    }
    public String saveTravelAsString() {
        return progressState.saveTravelAsString();
    }
    public boolean getTravel(int i) {
        return progressState.getTravel(i);
    }
    public void saveTravel(int i) {
        progressState.saveTravel(i);
    }
    public void addUnlocks(int i, String... check) {
        progressState.addUnlocks(i, check);
    }
    public String saveUnlocksAsString() {
        return progressState.saveUnlocksAsString();
    }
    public boolean checkUnlock(int i) {
        return progressState.checkUnlock(i);
    }
    public int checkUnlockPaid(int i) {
        return progressState.checkUnlockPaid(i);
    }

    public abstract void initialize();

    public abstract void update();

    public void receieveDamage(Entity e, int amount, Entity.hitType type) {
        amount = Math.min(amount, currentHealth);
        if (getDamage().containsKey(e)) {
            getDamage().put(e, getDamage().get(e) + amount);
        } else {
            getDamage().put(e, amount);
        }
        dealDamage(e, amount, type);
    }

    public void println_debug(String str) {
        if (!net.dodian.utilities.DotEnvKt.getClientPacketTraceEnabled()
                && !net.dodian.utilities.DotEnvKt.getClientUiTraceEnabled()) {
            return;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("[player-{}]: {}", getSlot(), str);
        }
    }

    public void println(String str) {
        if (!net.dodian.utilities.DotEnvKt.getClientPacketTraceEnabled()
                && !net.dodian.utilities.DotEnvKt.getClientUiTraceEnabled()) {
            return;
        }
        logger.info("[player-{}]: {}", getSlot(), str);
    }

    public String getSongUnlockedSaveText() {
        return progressState.getSongUnlockedSaveText();
    }

    public boolean withinDistance(Object o) {
        if (getPosition().getZ() != o.z)
            return false;
        int deltaX = o.x - getPosition().getX(),
                deltaY = o.y - getPosition().getY();
        return deltaX <= 64 && deltaX >= -64 && deltaY <= 64 && deltaY >= -64;
    }

    public boolean withinDistance(Player otherPlr) {
        if (!otherPlr.isActivePlayer())
            return false;
        if (getPosition().getZ() != otherPlr.getPosition().getZ())
            return false;
        int deltaX = otherPlr.getPosition().getX() - getPosition().getX(),
                deltaY = otherPlr.getPosition().getY() - getPosition().getY();
        return deltaX <= 16 && deltaX >= -16 && deltaY <= 16 && deltaY >= -16;
    }

    public boolean withinDistance(Npc npc) {
        if (getPosition().getZ() != npc.getPosition().getZ())
            return false;
        if (!npc.visible)
            return false;
        int deltaX = npc.getPosition().getX() - getPosition().getX(),
                deltaY = npc.getPosition().getY() - getPosition().getY();
        return deltaX <= 15 && deltaX >= -16 && deltaY <= 15 && deltaY >= -16;
    }

    /**
     * @return The username of the player.
     */
    public String getPlayerName() {
        return accountState.getPlayerName();
    }

    public void setPlayerName(String playerName) {
        accountState.setPlayerName(playerName);
    }

    public String getPlayerPass() {
        return accountState.getPlayerPass();
    }

    public void setPlayerPass(String playerPass) {
        accountState.setPlayerPass(playerPass);
    }

    public int getDbId() {
        return accountState.getDbId();
    }

    public void setDbId(int dbId) {
        accountState.setDbId(dbId);
    }

    public int getPlayerRights() {
        return accountState.getPlayerRights();
    }

    public void setPlayerRights(int playerRights) {
        accountState.setPlayerRights(playerRights);
    }

    public int getPlayerGroup() {
        return accountState.getPlayerGroup();
    }

    public void setPlayerGroup(int playerGroup) {
        accountState.setPlayerGroup(playerGroup);
    }

    public boolean isPremium() {
        return accountState.isPremium();
    }

    public void setPremium(boolean premium) {
        accountState.setPremium(premium);
    }

    public int getLatestNews() {
        return accountState.getLatestNews();
    }

    public void setLatestNews(int latestNews) {
        accountState.setLatestNews(latestNews);
    }

    public String getConnectedFrom() {
        return accountState.getConnectedFrom();
    }

    public void setConnectedFrom(String connectedFrom) {
        accountState.setConnectedFrom(connectedFrom);
    }

    public int getIp() {
        return accountState.getIp();
    }

    public void setIp(int ip) {
        accountState.setIp(ip);
    }

    public String getUUID() {
        return accountState.getUUID();
    }

    public void setUUID(String UUID) {
        accountState.setUUID(UUID);
    }

    public boolean isInitialized() {
        return accountState.isInitialized();
    }

    public void setInitialized(boolean initialized) {
        accountState.setInitialized(initialized);
    }

    public boolean isDisconnected() {
        return accountState.isDisconnected();
    }

    public void setDisconnected(boolean disconnected) {
        accountState.setDisconnected(disconnected);
    }

    public boolean isActivePlayer() {
        return accountState.isActivePlayer();
    }

    public void setActivePlayer(boolean active) {
        accountState.setActivePlayer(active);
    }

    public boolean isKicked() {
        return accountState.isKicked();
    }

    public void setKicked(boolean kicked) {
        accountState.setKicked(kicked);
    }

    public boolean isSaveNeeded() {
        return accountState.isSaveNeeded();
    }

    public void setSaveNeeded(boolean saveNeeded) {
        accountState.setSaveNeeded(saveNeeded);
    }

    public boolean isYellOn() {
        return accountState.isYellOn();
    }

    public void setYellOn(boolean yellOn) {
        accountState.setYellOn(yellOn);
    }

    public boolean isDiscordEnabled() {
        return accountState.isDiscord();
    }

    public void setDiscordEnabled(boolean discord) {
        accountState.setDiscord(discord);
    }

    public boolean isInstaLootEnabled() {
        return accountState.isInstaLoot();
    }

    public void setInstaLootEnabled(boolean instaLoot) {
        accountState.setInstaLoot(instaLoot);
    }

    public int getDailyLogin() {
        return accountState.getDailyLogin();
    }

    public void setDailyLogin(int dailyLogin) {
        accountState.setDailyLogin(dailyLogin);
    }

    public ArrayList<String> getDailyReward() {
        return accountState.getDailyReward();
    }

    public WalkToTask getWalkToTask() {
        return this.walkToTask;
    }

    public void setWalkToTask(WalkToTask walkToTask) {
        this.walkToTask = walkToTask;
    }

    public int mapRegionX, mapRegionY; // the map region the player is
    // currently in

    public static final int WALKING_QUEUE_SIZE = 80;
    public int[] walkingQueueX = new int[WALKING_QUEUE_SIZE], walkingQueueY = new int[WALKING_QUEUE_SIZE];
    public int wQueueReadPtr = 0; // points to slot for reading from queue
    public int wQueueWritePtr = 0; // points to (first free) slot for writing
    public boolean isRunning = false;
    public int teleportToX, teleportToY, teleportToZ; // contain absolute x/y
    public boolean walkingBlock = false;
    public void resetWalkingQueue() {
        movementState.resetWalkingQueue();
    }

    public void addToWalkingQueue(int x, int y) {
        movementState.addToWalkingQueue(x, y);
    }

    // returns 0-7 for next walking direction or -1, if we're not moving
    public int getNextWalkingDirection() {
        return movementState.getNextWalkingDirection();
    }

    public boolean firstSend = false;

    public void getNextPlayerMovement() {
        movementState.getNextPlayerMovement();
    }

    public int getLevel(Skill skill) {
        return stats.getLevel(skill);
    }

    public int getExperience(Skill skill) {
        return stats.getExperience(skill);
    }

    public void addExperience(int experience, Skill skill) {
        stats.addExperience(experience, skill);
    }

    public void setLevel(int level, Skill skill) {
        stats.setLevel(level, skill);
    }

    public void setExperience(int experience, Skill skill) {
        stats.setExperience(experience, skill);
    }

    // handles anything related to character position basically walking, running
    // and standing
    // applies to only to "non-thisPlayer" charracters
    public void updatePlayerMovement(ByteMessage str) {
        int primaryDirection = getPrimaryDirection();
        int secondaryDirection = getSecondaryDirection();
        if (primaryDirection == -1) {
            // don't have to update the character position, because the char is
            // just standing
            if (getUpdateFlags().isUpdateRequired() || getUpdateFlags().get(UpdateFlag.CHAT)) {
                // tell client there's an update block appended at the end
                str.putBits(1, 1);
                str.putBits(2, 0);
            } else
                str.putBits(1, 0);
        } else if (secondaryDirection == -1) {
            // send "walking packet"
            int translatedPrimary = translateDirectionToClient(primaryDirection, "local-primary");
            if (translatedPrimary == -1) {
                if (getUpdateFlags().isUpdateRequired() || getUpdateFlags().get(UpdateFlag.CHAT)) {
                    str.putBits(1, 1);
                    str.putBits(2, 0);
                } else {
                    str.putBits(1, 0);
                }
                return;
            }
            str.putBits(1, 1);
            str.putBits(2, 1);
            str.putBits(3, translatedPrimary);
            str.putBits(1, getUpdateFlags().isUpdateRequired() ? 1 : 0);
        } else {
            // send "running packet"
            int translatedPrimary = translateDirectionToClient(primaryDirection, "local-primary");
            int translatedSecondary = translateDirectionToClient(secondaryDirection, "local-secondary");
            if (translatedPrimary == -1) {
                if (getUpdateFlags().isUpdateRequired() || getUpdateFlags().get(UpdateFlag.CHAT)) {
                    str.putBits(1, 1);
                    str.putBits(2, 0);
                } else {
                    str.putBits(1, 0);
                }
                return;
            }
            str.putBits(1, 1);
            str.putBits(2, translatedSecondary == -1 ? 1 : 2);
            str.putBits(3, translatedPrimary);
            if (translatedSecondary != -1) {
                str.putBits(3, translatedSecondary);
            }
            str.putBits(1, getUpdateFlags().isUpdateRequired() ? 1 : 0);
        }

    }

    private int translateDirectionToClient(int direction, String phase) {
        if (direction < 0 || direction >= Utils.xlateDirectionToClient.length) {
            logger.warn("Invalid player direction {} for {} during {}", direction, getPlayerName(), phase);
            return -1;
        }
        return Utils.xlateDirectionToClient[direction];
    }

    public void addNewPlayer(Player plr, ByteMessage str, ByteMessage updateBlock) {
        if (plr == null || playerListSize >= 255 || playerListSize >= playerList.length) {
            return;
        }
        int id = plr.getSlot();
        playerList[playerListSize++] = plr;
        playersUpdating.add(plr);
        bumpLocalPlayerMembershipRevision();
        str.putBits(11, id);
        str.putBits(1, 1);// Requires update?
        PlayerUpdating.getInstance().appendAddLocalBlockUpdate(plr, updateBlock);
        str.putBits(1, 1); // set to true, if we want to discard the
        // (clientside) walking queue
        // no idea what this might be useful for yet
        int z = plr.getPosition().getY() - getPosition().getY();
        if (z < 0)
            z += 32;
        str.putBits(5, z); // y coordinate relative to thisPlayer
        z = plr.getPosition().getX() - getPosition().getX();
        if (z < 0)
            z += 32;
        str.putBits(5, z); // x coordinate relative to thisPlayer
    }

    public boolean isCachedUpdateBlockValid() {
        return updateState.isCachedUpdateBlockValid();
    }

    public void writeCachedUpdateBlock(ByteMessage dst) {
        updateState.writeCachedUpdateBlock(dst);
    }

    public void cacheUpdateBlock(ByteMessage src) {
        updateState.cacheUpdateBlock(src);
    }

    public void invalidateCachedUpdateBlock() {
        updateState.invalidateCachedUpdateBlock();
    }

    public void markAppearanceDirty() {
        updateState.markAppearanceDirty();
    }

    public long getAppearanceRevision() {
        return updateState.getAppearanceRevision();
    }

    public boolean isCachedAppearanceValid() {
        return updateState.isCachedAppearanceValid();
    }

    public byte[] getCachedAppearanceBytes() {
        return updateState.getCachedAppearanceBytes();
    }

    public void cacheAppearanceBytes(byte[] bytes) {
        updateState.cacheAppearanceBytes(bytes);
    }

    public byte[] getChatText() {
        return updateState.getChatText();
    }

    public int getChatTextSize() {
        return updateState.getChatTextSize();
    }

    public void setChatTextSize(int chatTextSize) {
        updateState.setChatTextSize(chatTextSize);
    }

    public int getChatTextEffects() {
        return updateState.getChatTextEffects();
    }

    public void setChatTextEffects(int chatTextEffects) {
        updateState.setChatTextEffects(chatTextEffects);
    }

    public int getChatTextColor() {
        return updateState.getChatTextColor();
    }

    public void setChatTextColor(int chatTextColor) {
        updateState.setChatTextColor(chatTextColor);
    }

    public String getChatTextMessage() {
        return updateState.getChatTextMessage();
    }

    public void setChatTextMessage(String chatTextMessage) {
        updateState.setChatTextMessage(chatTextMessage);
    }

    public void clearUpdateFlags() {
        updateState.clearUpdateFlags();
        combatState.clearPendingHits();
    }

    public void faceTarget(int index) {
        updateState.faceTarget(index);
    }
    public void faceNpc(int index) {
        faceTarget(index);
    }
    public void facePlayer(int index) {
        faceTarget(32768 + index);
    }

    public void gfx0(int gfx) {
        updateState.gfx0(gfx);
    }

    public int getFaceTarget() {
        return updateState.getFaceTarget();
    }
    public abstract void process(); //Send every 600 ms

    public void markSaveDirty(int segmentMask) {
        accountState.markSaveDirty(segmentMask);
    }

    public void clearSaveDirtyMask(int segmentMask) {
        accountState.clearSaveDirtyMask(segmentMask);
    }

    public void clearAllSaveDirty() {
        accountState.clearAllSaveDirty();
    }

    public int getSaveDirtyMask() {
        return accountState.getSaveDirtyMask();
    }

    public long getLastSavedRevision() {
        return accountState.getLastSavedRevision();
    }

    public long getSaveRevision() {
        return accountState.getSaveRevision();
    }

    public void setLastSavedRevision(long lastSavedRevision) {
        accountState.setLastSavedRevision(lastSavedRevision);
    }

    public long getLastProcessedCycle() {
        return accountState.getLastProcessedCycle();
    }

    public void setLastProcessedCycle(long lastProcessedCycle) {
        accountState.setLastProcessedCycle(lastProcessedCycle);
    }

    public InteractionIntent getPendingInteraction() {
        return interactionState.getPendingInteraction();
    }

    public void setPendingInteraction(InteractionIntent pendingInteraction) {
        interactionState.setPendingInteraction(pendingInteraction);
    }

    public ActiveInteraction getActiveInteraction() {
        return interactionState.getActiveInteraction();
    }

    public void setActiveInteraction(ActiveInteraction activeInteraction) {
        interactionState.setActiveInteraction(activeInteraction);
    }

    public long getInteractionEarliestCycle() {
        return interactionState.getInteractionEarliestCycle();
    }

    public void setInteractionEarliestCycle(long interactionEarliestCycle) {
        interactionState.setInteractionEarliestCycle(interactionEarliestCycle);
    }

    public QueueTaskHandle getInteractionTaskHandle() {
        return interactionState.getInteractionTaskHandle();
    }

    public void setInteractionTaskHandle(QueueTaskHandle interactionTaskHandle) {
        interactionState.setInteractionTaskHandle(interactionTaskHandle);
    }

    public void cancelInteractionTask() {
        interactionState.cancelInteractionTask();
    }

    public QueueTaskHandle getFarmDebugTaskHandle() {
        return interactionState.getFarmDebugTaskHandle();
    }

    public void setFarmDebugTaskHandle(QueueTaskHandle farmDebugTaskHandle) {
        interactionState.setFarmDebugTaskHandle(farmDebugTaskHandle);
    }

    public void cancelFarmDebugTask() {
        interactionState.cancelFarmDebugTask();
    }

    public QueueTaskHandle getMiningTaskHandle() {
        return interactionState.getMiningTaskHandle();
    }

    public void setMiningTaskHandle(QueueTaskHandle miningTaskHandle) {
        interactionState.setMiningTaskHandle(miningTaskHandle);
    }

    public void cancelMiningTask() {
        interactionState.cancelMiningTask();
    }

    public MiningState getMiningState() {
        return interactionState.getMiningState();
    }

    public void setMiningState(MiningState miningState) {
        interactionState.setMiningState(miningState);
    }

    public void clearMiningState() {
        interactionState.clearMiningState();
    }

    public QueueTaskHandle getWoodcuttingTaskHandle() {
        return interactionState.getWoodcuttingTaskHandle();
    }

    public void setWoodcuttingTaskHandle(QueueTaskHandle woodcuttingTaskHandle) {
        interactionState.setWoodcuttingTaskHandle(woodcuttingTaskHandle);
    }

    public void cancelWoodcuttingTask() {
        interactionState.cancelWoodcuttingTask();
    }

    public WoodcuttingState getWoodcuttingState() {
        return interactionState.getWoodcuttingState();
    }

    public void setWoodcuttingState(WoodcuttingState woodcuttingState) {
        interactionState.setWoodcuttingState(woodcuttingState);
    }

    public void clearWoodcuttingState() {
        interactionState.clearWoodcuttingState();
    }

    public QueueTaskHandle getActiveActionHandle() {
        return interactionState.getActiveActionHandle();
    }

    public void setActiveActionHandle(QueueTaskHandle activeActionHandle) {
        interactionState.setActiveActionHandle(activeActionHandle);
    }

    public PlayerActionType getActiveActionType() {
        return interactionState.getActiveActionType();
    }

    public void setActiveActionType(PlayerActionType activeActionType) {
        interactionState.setActiveActionType(activeActionType);
    }

    public long getActionStartedCycle() {
        return interactionState.getActionStartedCycle();
    }

    public void setActionStartedCycle(long actionStartedCycle) {
        interactionState.setActionStartedCycle(actionStartedCycle);
    }

    public void cancelActiveAction() {
        interactionState.cancelActiveAction();
    }

    public void clearActiveActionState() {
        interactionState.clearActiveActionState();
    }

    public PlayerActionCancelReason getActiveActionCancelReason() {
        return interactionState.getActiveActionCancelReason();
    }

    public void setActiveActionCancelReason(PlayerActionCancelReason activeActionCancelReason) {
        interactionState.setActiveActionCancelReason(activeActionCancelReason);
    }

    public PlayerActionCancelReason getLastActionCancelReason() {
        return interactionState.getLastActionCancelReason();
    }

    public void setLastActionCancelReason(PlayerActionCancelReason lastActionCancelReason) {
        interactionState.setLastActionCancelReason(lastActionCancelReason);
    }

    public long getLastActionCancelCycle() {
        return interactionState.getLastActionCancelCycle();
    }

    public void setLastActionCancelCycle(long lastActionCancelCycle) {
        interactionState.setLastActionCancelCycle(lastActionCancelCycle);
    }

    public CombatTargetState getCombatTargetState() {
        return interactionState.getCombatTargetState();
    }

    public void setCombatTargetState(CombatTargetState combatTargetState) {
        interactionState.setCombatTargetState(combatTargetState);
    }

    public void clearCombatTargetState() {
        interactionState.clearCombatTargetState();
    }

    public CombatCancellationReason getCombatCancellationReason() {
        return interactionState.getCombatCancellationReason();
    }

    public void setCombatCancellationReason(CombatCancellationReason combatCancellationReason) {
        interactionState.setCombatCancellationReason(combatCancellationReason);
    }

    public void clearCombatCancellationReason() {
        interactionState.clearCombatCancellationReason();
    }

    public long getCombatLogoutLockUntilCycle() {
        return interactionState.getCombatLogoutLockUntilCycle();
    }

    public void setCombatLogoutLockUntilCycle(long combatLogoutLockUntilCycle) {
        interactionState.setCombatLogoutLockUntilCycle(combatLogoutLockUntilCycle);
    }

    public long getLastBlockAnimationCycle() {
        return interactionState.getLastBlockAnimationCycle();
    }

    public void setLastBlockAnimationCycle(long lastBlockAnimationCycle) {
        interactionState.setLastBlockAnimationCycle(lastBlockAnimationCycle);
    }

    public GameTaskSet<?> getPlayerTaskSet() {
        return interactionState.getPlayerTaskSet();
    }

    public void setPlayerTaskSet(GameTaskSet<?> playerTaskSet) {
        interactionState.setPlayerTaskSet(playerTaskSet);
    }

    public long getThrottleUntilCycle(String key) {
        return interactionState.getThrottleUntilCycle(key);
    }

    public void setThrottleUntilCycle(String key, long cycle) {
        interactionState.setThrottleUntilCycle(key, cycle);
    }

    public void clearThrottleUntilCycle(String key) {
        interactionState.clearThrottleUntilCycle(key);
    }

    public void postProcessing() {
        movementState.postProcessing();
    }

    public boolean buttonOnRun = true;

    public int deathStage = 0;
    public long deathTimer = 0;
    public long deathStartedCycle = 0;

    public void appendMask400Update(ByteMessage buf) { // Forcemovement mask!
        updateState.appendMask400Update(buf);
    }

    // PM Stuff
    public abstract void loadpm(long l, int world);

    public int Privatechat = 0;

    public abstract void sendpm(long name, int rights, byte[] chatmessage, int messagesize);

    public void dealDamage(Entity attacker, int amt, hitType type) {
        combatState.dealDamage(attacker, amt, type);
    }

    public void dealDamage(int amt, Entity.hitType type, Entity attacker, Entity.damageType dmg) {
        combatState.dealDamage(amt, type, attacker, dmg);
    }

    public void sendAnimation(int id) {
        this.setAnimationId(id);
        getUpdateFlags().setRequired(UpdateFlag.ANIM, true);
    }

    public void teleportTo(int x, int y, int z) {
        getPosition().moveTo(x, y, z); //Update position!
        teleportToX = getPosition().getX();
        teleportToY = getPosition().getY();
        teleportToZ = getPosition().getZ();
    }

    Prayers prayers = new Prayers(this);

    public boolean isSongUnlocked(int songId) {
        return progressState.isSongUnlocked(songId);
    }
    public boolean blackMaskEffect(int npcId) {
        String taskName = getSlayerData().get(0) == -1 || getSlayerData().get(3) <= 0 ? "" : Objects.requireNonNull(SlayerTask.slayerTasks.getTask(getSlayerData().get(1))).getTextRepresentation();
        SlayerTask.slayerTasks slayerTask = SlayerTask.slayerTasks.getSlayerNpc(npcId);
        boolean onTask = slayerTask != null && slayerTask.getTextRepresentation().equals(taskName) && getSlayerData().get(3) > 0;
        int itemId = getEquipment()[Equipment.Slot.HEAD.getId()];
        return (itemId == 8921 || itemId == 11864) && onTask;
    }
    public boolean blackMaskImbueEffect(int npcId) {
        String taskName = getSlayerData().get(0) == -1 || getSlayerData().get(3) <= 0 ? "" : Objects.requireNonNull(SlayerTask.slayerTasks.getTask(getSlayerData().get(1))).getTextRepresentation();
        SlayerTask.slayerTasks slayerTask = SlayerTask.slayerTasks.getSlayerNpc(npcId);
        boolean onTask = slayerTask != null && slayerTask.getTextRepresentation().equals(taskName) && getSlayerData().get(3) > 0;
        String headName = ((Client) this).GetItemName(getEquipment()[Equipment.Slot.HEAD.getId()]).toLowerCase();
        return (headName.contains("black mask (i)") || headName.contains("slayer helmet (i)")) && onTask;
    }

    public int getEquipment(int slot) {
        return playerEquipment[slot];
    }
    public String getEquipName(int slot) {
        return ((Client) this).GetItemName(getEquipment(slot));
    }
    public boolean armourSet(String armourName) {
        return switch (armourName) {
            case "ahrim" ->
                    getEquipName(Equipment.Slot.HEAD.getId()).startsWith("Ahrim") && getEquipName(Equipment.Slot.CHEST.getId()).startsWith("Ahrim") &&
                            getEquipName(Equipment.Slot.LEGS.getId()).startsWith("Ahrim") && getEquipName(Equipment.Slot.WEAPON.getId()).startsWith("Ahrim");
            case "karil" ->
                    getEquipName(Equipment.Slot.HEAD.getId()).startsWith("Karil") && getEquipName(Equipment.Slot.CHEST.getId()).startsWith("Karil") &&
                            getEquipName(Equipment.Slot.LEGS.getId()).startsWith("Karil") && getEquipName(Equipment.Slot.WEAPON.getId()).startsWith("Karil");
            case "verac" ->
                    getEquipName(Equipment.Slot.HEAD.getId()).startsWith("Verac") && getEquipName(Equipment.Slot.CHEST.getId()).startsWith("Verac") &&
                            getEquipName(Equipment.Slot.LEGS.getId()).startsWith("Verac") && getEquipName(Equipment.Slot.WEAPON.getId()).startsWith("Verac");
            case "dharok" ->
                    getEquipName(Equipment.Slot.HEAD.getId()).startsWith("Dharok") && getEquipName(Equipment.Slot.CHEST.getId()).startsWith("Dharok") &&
                            getEquipName(Equipment.Slot.LEGS.getId()).startsWith("Dharok") && getEquipName(Equipment.Slot.WEAPON.getId()).startsWith("Dharok");
            case "torag" ->
                    getEquipName(Equipment.Slot.HEAD.getId()).startsWith("Torag") && getEquipName(Equipment.Slot.CHEST.getId()).startsWith("Torag") &&
                            getEquipName(Equipment.Slot.LEGS.getId()).startsWith("Torag") && getEquipName(Equipment.Slot.WEAPON.getId()).startsWith("Torag");
            case "guthan" ->
                    getEquipName(Equipment.Slot.HEAD.getId()).startsWith("Guthan") && getEquipName(Equipment.Slot.CHEST.getId()).startsWith("Guthan") &&
                            getEquipName(Equipment.Slot.LEGS.getId()).startsWith("Guthan") && getEquipName(Equipment.Slot.WEAPON.getId()).startsWith("Guthan");
            default -> false;
        };
    }
    public boolean checkObsidianBonus(int id) {
        int[] acceptedItems = {
        11128, 6585, 6568, 6570, //Berserker necklace, fury, obsidian cape, fire cape,
        6522, 6523, 6525, 6526, 6527, 6528, 6526, //Obsidian weapons
        6524, 21298, 21301, 21304 //Obsidian armour
        };
        boolean inArea = getPositionName(getPosition()) == positions.TZHAAR  || getPositionName(getPosition()) == positions.JAD;
        for (int acceptedItem : acceptedItems)
            if (inArea && id == acceptedItem)
                return true;
        return false;
    }
    public boolean gotSlayerHelmet(Client c) {
        return c.GetItemName(getEquipment()[Equipment.Slot.HEAD.getId()]).toLowerCase().contains("slayer helm");
    }

    public boolean areAllSongsUnlocked() {
        return progressState.areAllSongsUnlocked();
    }

    public int dateDays(Date lowestDate, Date highestDate) {
        return (int)( (highestDate.getTime() - lowestDate.getTime()) / (1000 * 60 * 60 * 24));
    }
    public Date checkCalendarDate(Date date, int days) {
        Calendar checkCal = Calendar.getInstance();
        checkCal.setTime(date);
        checkCal.set(Calendar.HOUR_OF_DAY, 0);
        checkCal.set(Calendar.MINUTE, 0);
        checkCal.set(Calendar.SECOND, 0);
        checkCal.set(Calendar.MILLISECOND, 0);
        checkCal.add(Calendar.DATE, days);
        return checkCal.getTime();
    }

    public void setSongUnlocked(int songId, boolean unlocked) {
        progressState.setSongUnlocked(songId, unlocked);
    }

    /**
     * Gets the hash collection of the local npcs.
     *
     * @return the local npcs.
     */
    public Set<Npc> getLocalNpcs() {
        return localNpcs;
    }

    public long getLocalPlayerMembershipRevision() {
        return localPlayerMembershipRevision;
    }

    public void bumpLocalPlayerMembershipRevision() {
        localPlayerMembershipRevision++;
    }

    public long getLocalNpcMembershipRevision() {
        return localNpcMembershipRevision;
    }

    public void bumpLocalNpcMembershipRevision() {
        localNpcMembershipRevision++;
    }

    public boolean didTeleport() {
        return movementState.didTeleport();
    }

    public boolean didMapRegionChange() {
        return movementState.didMapRegionChange();
    }

    public int getPrimaryDirection() {
        return movementState.getPrimaryDirection();
    }

    public int getSecondaryDirection() {
        return movementState.getSecondaryDirection();
    }

    public int getCurrentX() {
        return movementState.getCurrentX();
    }

    public int getCurrentY() {
        return movementState.getCurrentY();
    }

    public void setGraphic(int graphicId, int graphicHeight) {
        updateState.setGraphic(graphicId, graphicHeight);
    }

    public int getGraphicId() {
        return updateState.getGraphicId();
    }

    public int getGraphicHeight() {
        return updateState.getGraphicHeight();
    }

    public String getForcedChat() {
        return this.forcedChat;
    }

    public int[] getEquipment() {
        return this.playerEquipment;
    }

    public int[] getEquipmentN() {
        return this.playerEquipmentN;
    }

    public int getGender() {
        return appearanceState.getGender();
    }

    public void setGender(int pGender) {
        appearanceState.setGender(pGender);
    }

    public int getTorso() {
        return appearanceState.getTorso();
    }

    public void setTorso(int pTorso) {
        appearanceState.setTorso(pTorso);
    }

    public int getArms() {
        return appearanceState.getArms();
    }

    public void setArms(int pArms) {
        appearanceState.setArms(pArms);
    }

    public int getLegs() {
        return appearanceState.getLegs();
    }

    public void setLegs(int pLegs) {
        appearanceState.setLegs(pLegs);
    }

    public int getHands() {
        return appearanceState.getHands();
    }

    public void setHands(int pHands) {
        appearanceState.setHands(pHands);
    }

    public int getFeet() {
        return appearanceState.getFeet();
    }

    public void setFeet(int pFeet) {
        appearanceState.setFeet(pFeet);
    }

    public int getBeard() {
        return appearanceState.getBeard();
    }

    public void setBeard(int pBeard) {
        appearanceState.setBeard(pBeard);
    }

    public int getHead() {
        return appearanceState.getHead();
    }

    public void setHead(int pHead) {
        appearanceState.setHead(pHead);
    }

    public int getStandAnim() {
        return appearanceState.getStandAnim();
    }

    public void setStandAnim(int playerSE) {
        appearanceState.setStandAnim(playerSE);
    }

    public int getWalkAnim() {
        return appearanceState.getWalkAnim();
    }

    public void setAgilityEmote(int walk, int run) {
        appearanceState.setAgilityEmote(walk, run);
    }

    public void setWalkAnim(int playerSEW) {
        appearanceState.setWalkAnim(playerSEW);
    }

    public int getRunAnim() {
        return appearanceState.getRunAnim();
    }

    public void setRunAnim(int playerSER) {
        appearanceState.setRunAnim(playerSER);
    }

    public int getPlayerNpc() {
        return appearanceState.getPlayerNpc();
    }

    public void setPlayerNpc(int playerNpc) {
        appearanceState.setPlayerNpc(playerNpc);
    }

    public int getDamageDealt() {
        return combatState.getDamageDealt();
    }
    public int getDamageDealt2() {
        return combatState.getDamageDealt2();
    }
    public Entity.hitType getHitType() {
        return combatState.getHitType();
    }
    public Entity.hitType getHitType2() {
        return combatState.getHitType2();
    }

    public int getCurrentHealth() {
        return stats.getCurrentHealth();
    }
    public int getMaxHealth() {
        return stats.getMaxHealth();
    }
    public void setCurrentHealth(int currentHealth) {
        stats.setCurrentHealth(currentHealth);
    }
    public void heal(int healing) {
        stats.heal(healing);
    }
    public void heal(int healing, int overHeal) {
        stats.heal(healing, overHeal);
    }
    public void eat(int healing, int removeId, int removeSlot) {
        stats.eat(healing, removeId, removeSlot);
    }

    public void boost(int boosted, Skill skill) {
        stats.boost(boosted, skill);
    }

    public int getCurrentPrayer() {
        return stats.getCurrentPrayer();
    }
    public int getMaxPrayer() {
        return stats.getMaxPrayer();
    }
    public void setCurrentPrayer(int amount) {
        stats.setCurrentPrayer(amount);
    }
    public void drainPrayer(int amount) {
        stats.drainPrayer(amount);
    }
    public void pray(int healing) {
        stats.pray(healing);
    }

    public boolean isInCombat() {
        return combatState.isInCombat();
    }

    public boolean isDeadOrDying() {
        return combatState.isDeadOrDying();
    }

    public int getLastCombat() {
        return combatState.getLastCombat();
    }
    public void setLastCombat(int lastCombat) {
        combatState.setLastCombat(lastCombat);
    }

    public int getCombatTimer() {
        return combatState.getCombatTimer();
    }
    public void setCombatTimer(int timer) {
        combatState.setCombatTimer(timer);
    }
    public int getStunTimer() {
        return combatState.getStunTimer();
    }
    public void setStunTimer(int timer) {
        combatState.setStunTimer(timer);
    }
    public int getSnareTimer() {
        return combatState.getSnareTimer();
    }
    public void setSnareTimer(int timer) {
        combatState.setSnareTimer(timer);
    }

    /**
     * Calculates and returns the combat level for this player.
     *
     * @return the combat level.
     */
    public int determineCombatLevel() {
        return stats.determineCombatLevel();
    }

    public int getSkillLevel(Skill skill) {
        return stats.getSkillLevel(skill);
    }

    public boolean skillcapePerk(Skill skill, boolean checkInventory) {
        Skillcape skillcape = Skillcape.getSkillCape(getEquipment()[Equipment.Slot.CAPE.getId()]);
        boolean maxCape = ((Client) this).GetItemName(getEquipment()[Equipment.Slot.CAPE.getId()]).toLowerCase().contains("max cape");

        if(checkInventory && (!maxCape || (skillcape != null && skillcape.getSkill() != skill))) {
            for(int i = 0; i < 28 && !maxCape; i++) {
                skillcape = Skillcape.getSkillCape(playerItems[i] - 1);
                if(((Client) this).GetItemName(playerItems[i] - 1).toLowerCase().contains("max cape")
                || (skillcape != null && skillcape.getSkill() == skill))
                    maxCape = true; //I am lazy and this is some shiet that could work :L
            }
        }

        return maxCape || (skillcape != null && skillcape.getSkill() == skill); //prio list maxcape above all else!
    }

    public enum positions {
        //name, x1, x2, y1, y2
        YANILLE("in Yanille.", 2562, 2620, 3072, 3110),
        WESTYANILLE("in West Yanille.", 2525, 2561, 3072, 3110),
        SEERS("in Seer's Village.", 2686, 2742, 3449, 3531),
        EASTARDY("in East Ardougne.", 2559, 2688, 3260, 3345),
        WESTARDY("in West Ardougne.", 2437, 2559, 3260, 3337),
        CATHERBY("in Catherby.", 2789, 2840, 3434, 3469),
        CATHDOCKS("at Catherby docks.", 2802, 2806, 3417, 3430),
        CATHBEACH("at Catherby beach.", 2833, 2859, 3418, 3436),
        LEGENDSGUILD("in Legend's Guild.", 2726, 2731, 3350, 3361),
        LEGENDSGUILD2("in Legend's Guild.", 2719, 2738, 3362, 3386),
        LEGENDSDUNG("in Legend's dungeon.", 2688, 2742, 9725, 9786),
        TAVERLY("in Taverly.", 2876, 2936, 3405, 3490),
        TAVDUNG("in Taverly dungeon.", 2813, 2970, 9669, 9856),
        FISHGUILD("in Fishing Guild.", 2577, 2642, 3390, 3446),
        GNOMECOURSE("at Gnome course.", 2469, 2490, 3414, 3440),
        GNOMEHIDDENCAVE_WEST("in the Gnome hidden cave.", 2434, 2452, 9896, 9918),
        GNOMEHIDDENCAVE_MIDDLE("in the Gnome hidden cave.", 2453, 2478, 9901, 9918),
        GNOMEHIDDENCAVE_EAST("in the Gnome hidden cave.", 2479, 2495, 9907, 9918),
        BARBCOURSE("at Barbarian course.", 2528, 2553, 3541, 3559),
        WILDERNESSCOURSE("at Wilderness course.", 2987, 3009, 3931, 3966),
        HEROESGUILD("in Heroes Guild.", 2883, 2901, 3503, 3518),
        HEROESDUNG("in Heroes dungeon.", 2882, 2944, 9878, 9920),
        DRAGCAVE("in Dragon cave.", 3200, 3304, 9342, 9406),
        DRAGCAVE2("in Dragon cave.", 3303, 3326, 9394, 9406),
        DRAGCAVE3("in Dragon cave.", 3303, 3326, 9342, 9357),
        KBDLAIR("in the Dragon's den!", 3303, 3326, 9360, 9394),
        STAFFZONE("in the Staffzone.", 2880, 2943, 4672, 4735),
        EDGEVILLE("in the Edgeville.", 3065, 3133, 3463, 3521),
        SHILO("in the Shilo village.", 2817, 2878, 2945, 3006),
        TZHAAR("in the Tzhaar cave.", 2397, 2494, 5120, 5183),
        JAD("in the Jad cave.", 2368, 2428, 5057, 5118),
        BRIMHAVEN_DOCKS("in the Brimhaven docks.", 2758, 2787, 3218, 3240),
        BRIMHAVEN_WEST("in the Brimhaven west.", 2689, 2757, 3137, 3247),
        BRIMHAVEN_EAST("in the Brimhaven east.", 2757, 2815, 3137, 3217),
        BRIMHAVEN_DUNGEON("in the Brimhaven dungeon.", 3713, 3837, 9346, 9469),
        KEYDUNG("in Key dungeon.", 2559, 2622, 9475, 9534),
        IKOVDUNG("in the Temple of Ikov.", 2626, 2750, 9784, 9918),
        PARTYROOM("in the Partyroom.", 3035, 3055, 3370, 3385),
        SOPHANEM_CARPET("near carpet travel", 3285, 3288, 2811, 2815),
        POLLNIVNEACH_CARPET("near carpet travel", 3348, 3351, 2999, 3003),
        NARDAH_CARPET("near carpet travel", 3399, 3402, 2916, 2919),
        BANDIT_CAMP_CARPET("near carpet travel", 3180, 3183, 3041, 3045),
        DESERT_NARDAH("in the Nardah.", 3396, 3452, 2885, 2942), //3396, 2942, 3452, 2885
        DESERT_SOPHANEM("in the Sophanem.", 3271, 3322, 2747, 2809),
        DESERT_MENAPHOS("in the Menaphos.", 3202, 3270, 2750, 2809),
        DESERT_POLLNIVNEACH_MAIN("in the Pollnivneach.", 3334, 3372, 2958, 3004), //3334, 3004, 3372, 2958
        DESERT_POLLNIVNEACH_OUTSKIRT_1("in the Pollnivneach.", 3373, 3380, 2961, 2992),
        DESERT_POLLNIVNEACH_OUTSKIRT_2("in the Pollnivneach.", 3329, 3358, 2940, 2968),
        DESERT_POLLNIVNEACH_OUTSKIRT_3("in the Pollnivneach.", 3359, 3373, 2957, 2964),
        DESERT_POLLNIVNEACH_OUTSKIRT_4("in the Pollnivneach.", 3359, 3369, 2953, 2956),
        DESERT_POLLNIVNEACH_OUTSKIRT_5("in the Pollnivneach.", 3359, 3364, 2949, 2952),
        DESERT_BANDIT_CAMP("in the Bandit camp.", 3138, 3192, 2963, 2992),
        DESERT_BEDABIN_CAMP("in the Bedabin camp.", 3159, 3187, 3023, 3050),
        PYRAMID_PLUNDER("doing Pyramid Plunder", 1916, 1983, 4418, 4479),
        KALPHITE_KING("in the Kalphite King lair.", 10, 100, 20, 110),
        KALPHITE_QUEEN("in the Kalphite Queen lair.", 10, 100, 20, 110),
        DESERT("in the Desert.", 3137, 3565, 2647, 3162), //3565, 2647, 3137, 3162
        VENENATIS("in the Venenatis lair.", 10, 100, 20, 110)
        ;
        //Area[] area = {
        //    new Area(3193, 2953, 3171, 2996),
        //    new Area(3170, 2956, 3139, 2996)
        //};
        final String name;
        final int[] coordValue;

        positions(String name, int... coordValue) {
            this.name = name;
            this.coordValue = coordValue;
        }
	  /*
	  2619, 3073
	  2539, 3073
	  2539, 3108
	  */ //Test Yanille coords.
    }

    private static final positions[] POSITION_VALUES = positions.values();

    public int getSkillId(String name) {
        for (int i = 0; i < Skill.values().length; i++)
            if (Skill.values()[i].getName().startsWith(name.toLowerCase()))
                return i;
        return -1;
    }

    public positions getPositionName(Position current) {
        for (positions pos : POSITION_VALUES) {
            if (current.getX() >= pos.coordValue[0] && current.getX() <= pos.coordValue[1] && current.getY() >= pos.coordValue[2] && current.getY() <= pos.coordValue[3])
                return pos;
        }
        return null;
    }

    public String getPositionName() {
        Position current = getPosition();
        for (positions pos : POSITION_VALUES) {
            if (current.getX() >= pos.coordValue[0] && current.getX() <= pos.coordValue[1] && current.getY() >= pos.coordValue[2] && current.getY() <= pos.coordValue[3])
                return pos.name;
        }
        return "exploring the world.";
    }

    public static void openPage(Client c, String pageName) {
        try {
            c.send(new SendMessage(pageName + "#url#"));
        } catch (Exception e) {
            logger.warn("error opening page..", e);
        }
    }

    public boolean inWildy() {
        // if(absX > 2600 && absY > 8900) return true;
        return wildyLevel > 0 || (getPosition().getY() == 3523);
    }

    public boolean inEdgeville() {
        return getPositionName(getPosition()) == positions.EDGEVILLE;
    }

    public void respawnBoss(int id) {
        Client c = (Client) this;
        int status = -1;
        for(Npc n : Server.npcManager.getNpcs()) {
            if(n.getId() == id && status == -1) {
                status = !n.alive ? 0 : 1;
                if(!n.alive)
                    n.respawn();
            }
        }
        String npcName = Server.npcManager.getName(id);
        c.send(new SendMessage(status == -1 ? "Could not found the npc with the name of '" + npcName + "'." : status == 0 ? "You respawn " + npcName + "!" : npcName + " is already alive!"));
    }

    public int bankSize() {
        return this.playerBankSize / 2;
    }

    public ArrayList<String> monsterName = new ArrayList<>();
    public ArrayList<Integer> monsterCount = new ArrayList<>();
    public void addMonsterName(String name) {
        progressState.addMonsterName(name);
    }
    public int getMonsterIndex(String name) {
        return progressState.getMonsterIndex(name);
    }
    public void incrementMonsterLog(Npc npc) {
        progressState.incrementMonsterLog(npc);
    }
    public void addMonsterLog(Npc npc, int index) {
        progressState.addMonsterLog(npc, index);
    }
    public int monsterKC(Npc npc) {
        return progressState.monsterKC(npc);
    }

    public void checkLoot(Client c, NpcData n) {
            ArrayList<Integer> lootedItem = new ArrayList<>();
            ArrayList<Integer> lootedAmount = new ArrayList<>();
            boolean wealth = c.getEquipment()[Equipment.Slot.RING.getId()] == 2572, itemDropped;
            double chance, currentChance, checkChance;
            for (int LOOP = 0; LOOP < 1000; LOOP++) {
                chance = Misc.chance(100000) / 1000D;
                currentChance = 0.0;
                itemDropped = false;
                for (NpcDrop drop : n.getDrops()) {
                    if (drop == null) continue;
                    checkChance = drop.getChance();
                    if (wealth && drop.getChance() < 10.0)
                        checkChance *= drop.getId() >= 5509 && drop.getId() <= 5515 ? 1.0 : drop.getChance() <= 0.1 ? 1.25 : drop.getChance() <= 1.0 ? 1.15 : 1.05;

                    if (drop.getChance() >= 100.0) { // 100% items!
                        int pos = lootedItem.lastIndexOf(drop.getId());
                        if (pos == -1) {
                            lootedItem.add(drop.getId());
                            lootedAmount.add(drop.getAmount());
                        } else
                            lootedAmount.set(pos, lootedAmount.get(pos) + drop.getAmount());
                    } else if (checkChance + currentChance >= chance && !itemDropped) { // user won the roll
                        if (drop.getId() >= 5509 && drop.getId() <= 5515) //Just incase shiet!
                            if (c.checkItem(drop.getId()))
                                continue;
                        int pos = lootedItem.lastIndexOf(drop.getId());
                        if (pos == -1) {
                            lootedItem.add(drop.getId());
                            lootedAmount.add(drop.getAmount());
                        } else
                            lootedAmount.set(pos, lootedAmount.get(pos) + drop.getAmount());
                        itemDropped = true;
                    }
                    if (!itemDropped && drop.getChance() < 100.0)
                        currentChance += checkChance;
                }
            }
            c.openBankStyleView(lootedItem, lootedAmount, "Loot from 1000 " + n.getName());
            if (wealth)
                c.send(new SendMessage("<col=FF6347>This is a result with a ring of wealth!"));
    }

    public void examineNpc(Client c, int npcId) {
        NpcData n = Server.npcManager.getData(npcId);
        if (n == null) { return; } //No data!
        if (!n.getDrops().isEmpty())
            checkLoot(c, n);
        if(!n.getExamine().isEmpty())
            c.send(new SendMessage(n.getExamine()));
    }
    public void examineObject(Client c, int objectId, Position objPos) {
        //Do we handle objects?!
        c.farming.examineBin(c, objPos);
        if(objectId == 378 && objPos.getX() == 2593 && objPos.getY() == 3108 && objPos.getZ() == 1) { //Check timer on a object!
            long timeLeft = (long) Objects.requireNonNull(GlobalObject.getGlobalObject(objPos.getX(), objPos.getY())).getAttachment();
            int secondsLeft = (int)((timeLeft - System.currentTimeMillis()) / 1000L);
            c.send(new SendMessage("This chest respawn in " + (secondsLeft + 1) + " seconds!"));
        }
        if(objectId == 378 && objPos.getX() == 2733 && objPos.getY() == 3374 && objPos.getZ() == 0) { //Check timer on a object!
            long timeLeft = (long) Objects.requireNonNull(GlobalObject.getGlobalObject(objPos.getX(), objPos.getY())).getAttachment();
            int secondsLeft = (int)((timeLeft - System.currentTimeMillis()) / 1000L);
            c.send(new SendMessage("This chest respawn in " + (secondsLeft + 1) + " seconds!"));
        }
        if(objectId == 375 && objPos.getX() == 2593 && objPos.getY() == 3108 && objPos.getZ() == 1) { //Check timer on a object!
            ArrayList<Integer> lootedItem = new ArrayList<>();
            ArrayList<Integer> lootedAmount = new ArrayList<>();
            for(int i = 0; i < 1000; i++) {
                double roll = Math.random() * 100;
                if (roll <= 0.3) {
                    int[] items = {2577, 2579, 2631};
                    int r = (int) (Math.random() * items.length);
                    int pos = lootedItem.lastIndexOf(items[r]);
                    if (pos == -1) {
                        lootedItem.add(items[r]);
                        lootedAmount.add(1);
                    } else
                        lootedAmount.set(pos, lootedAmount.get(pos) + 1);
                } else {
                    int coins = 300 + Utils.random(1200);
                    int pos = lootedItem.lastIndexOf(995);
                    if (pos == -1) {
                        lootedItem.add(995);
                        lootedAmount.add(coins);
                    } else
                        lootedAmount.set(pos, lootedAmount.get(pos) + coins);
                }
            }
            c.openBankStyleView(lootedItem, lootedAmount, "Loot from 1000 Yanille Chest");
        }
        if(objectId == 375 && objPos.getX() == 2733 && objPos.getY() == 3374 && objPos.getZ() == 0) { //Check timer on a object!
            ArrayList<Integer> lootedItem = new ArrayList<>();
            ArrayList<Integer> lootedAmount = new ArrayList<>();
            for(int i = 0; i < 1000; i++) {
                double roll = Math.random() * 100;
                if (roll <= 0.3) {
                    int[] items = {1050, 2581, 2631};
                    int r = (int) (Math.random() * items.length);
                    int pos = lootedItem.lastIndexOf(items[r]);
                    if (pos == -1) {
                        lootedItem.add(items[r]);
                        lootedAmount.add(1);
                    } else
                        lootedAmount.set(pos, lootedAmount.get(pos) + 1);
                } else {
                    int coins = 500 + Utils.random(2000);
                    int pos = lootedItem.lastIndexOf(995);
                    if (pos == -1) {
                        lootedItem.add(995);
                        lootedAmount.add(coins);
                    } else
                        lootedAmount.set(pos, lootedAmount.get(pos) + coins);
                }
            }
            c.openBankStyleView(lootedItem, lootedAmount, "Loot from 1000 Yanille Chest");
        }
    }

    public void customObjects() {
        Client client = (Client) this;
        client.replaceDoors();
        Balloons.updateBalloons(client);
        GlobalObject.updateObject(client);
        for(int i = 0; i <= 4; i++) //Refresh farming varbits!
            client.varbit(client.farming.getFarmData().getFarmPatchConfig() + i, 0);
        client.farming.updateCompost(client);
        client.farming.updateFarmPatch(client);
        if(client.getPosition().getZ() == 0) {
            /* NMZ object removal!*/
            for (int x = 0; x <= 9; x++)
                for (int y = 0; y <= 8; y++)
                    client.ReplaceObject2(new Position(2600 + x, 3111 + y, 0), -1, 0, 10);
            client.ReplaceObject2(new Position(2869, 9813, 0), 2343, 0, 10); //Brick
            client.ReplaceObject2(new Position(2870, 9813, 0), 2343, 0, 10); //Brick
            client.ReplaceObject2(new Position(2871, 9813, 0), 2343, 0, 10); //Brick

            client.ReplaceObject2(new Position(2866, 9797, 0), 2343, 0, 10); //Brick
            client.ReplaceObject2(new Position(2866, 9798, 0), 2343, 0, 10); //Brick
            client.ReplaceObject2(new Position(2866, 9799, 0), 2343, 0, 10); //Brick
            client.ReplaceObject2(new Position(2866, 9800, 0), 2343, 0, 10); //Brick

            client.ReplaceObject2(new Position(2885, 9794, 0), 882, 0, 10); // Shortcut entrance Taverly
            client.ReplaceObject2(new Position(2899, 9728, 0), 882, 0, 10); // Shortcut exit Taverly

            client.ReplaceObject2(new Position(2572, 3105, 0), 14890, 0, 10); //Sand Pit in Yanille!
            client.ReplaceObject2(new Position(2542, 3097, 0), -1, 0, 10); //Remove portal near dad!
            client.ReplaceObject2(new Position(2942, 4688, 0), 12260, 3, 10); //Teleport of some sort!

            client.ReplaceObject2(new Position(2613, 3084, 0), 3994, -3, 11);
            client.ReplaceObject2(new Position(2628, 3151, 0), 2104, -3, 11);
            client.ReplaceObject2(new Position(2629, 3151, 0), 2105, -3, 11);
            client.ReplaceObject2(new Position(2733, 3374, 0), 375, -1, 11);
            client.ReplaceObject2(new Position(2688, 3481, 0), 27978, 1, 11); //Blood altar
            client.ReplaceObject2(new Position(2626, 3116, 0), 14905, -1, 11); //Nature altar
            client.ReplaceObject2(new Position(2595, 3409, 0), 133, -1, 10); // Dragon lair

            client.ReplaceObject2(new Position(2669, 2713, 0), -1, -1, 11); // Remove door?
            client.ReplaceObject2(new Position(2713, 3483, 0), -1, -1, 0); // Remove seers door?
            client.ReplaceObject2(new Position(2716, 3472, 0), -1, -1, 0); // Remove seers door?
            client.ReplaceObject2(new Position(2594, 3102, 0), -1, -1, 0); // Remove Yanille door?
            client.ReplaceObject2(new Position(2816, 3438, 0), -1, -1, 0); // Remove Catherby door?
            /* Rope from Tzhaar city */
            client.ReplaceObject2(new Position(2443, 5169, 0), 2352, 0, 10);
            /*
             * Danno: Box off new area from noobs =]
             */
            client.ReplaceObject2(new Position(2770, 3140, 0), 2050, 0, 10);
            client.ReplaceObject2(new Position(2771, 3140, 0), 2050, 0, 10);
            client.ReplaceObject2(new Position(2772, 3140, 0), 2050, 0, 10);
            client.ReplaceObject2(new Position(2772, 3141, 0), 2050, 0, 10);
            client.ReplaceObject2(new Position(2772, 3142, 0), 2050, 0, 10);
            client.ReplaceObject2(new Position(2772, 3143, 0), 2050, 0, 10);
            /* Blocking object! */
            //client.ReplaceObject2(new Position(2832, 2971, 0), 2050, 0, 10);
            /* ? */
            client.ReplaceObject2(new Position(2998, 3931, 0), 6951, 0, 0);
            client.ReplaceObject2(new Position(2904, 9678, 0), 6951, 0, 10);
            // slayer update
            // ReplaceObject2(2904, 9678, -1, -1, 11);
            // ReplaceObject2(2691, 9774, 2107, 0, 11);
            // Ancient slayer dunegon
            client.ReplaceObject(2661, 9815, 2391, 0, 0);
            client.ReplaceObject(2662, 9815, 2392, -2, 0);
            /* Gnome mining cavern */
            client.ReplaceObject2(new Position(2492, 9916, 0), 7491, 0, 10);
            client.ReplaceObject2(new Position(2493, 9915, 0), 7491, 0, 10);
            /* Elemental obelisk */
            client.ReplaceObject2(new Position(2863, 3427, 0), 2151, 0, 10); //Water
            client.ReplaceObject2(new Position(3531, 3536, 0), 2150, 0, 10); //Earth
            client.ReplaceObject2(new Position(3059, 3564, 0), 2153, 0, 10); //Fire
            client.ReplaceObject2(new Position(2743, 3174, 0), 2152, 0, 10); //Air
            /* Desert shiet */
            client.ReplaceObject2(new Position(3284, 2809, 0), 20391, 2, 0);
            client.ReplaceObject2(new Position(3283, 2809, 0), 20391, 4, 0);
        }
    }

    public boolean rejectTeleport() {
        return getPlunder.hinderTeleport();
    }
    public void loginPosition(int x, int y, int z) {
        moveTo(x, y, z);
        if(getPositionName(getPosition()) == positions.PYRAMID_PLUNDER)
            getPlunder.resetPlunder();
    }

    public void examineItem(Client c, int id, int amount) {
        String name = c.GetItemName(id);
        if(amount >= 0x186a0)
            c.send(new SendMessage(amount + " x " + name));
        else { //Got this incase we need to do future stuff for item examine!
            c.send(new SendMessage(Server.itemManager.getExamine(id)));
        }
    }

    public void resetTabs() {
        Client c = ((Client) this);
        c.setEquipment(c.getEquipment()[Equipment.Slot.WEAPON.getId()], c.getEquipmentN()[Equipment.Slot.WEAPON.getId()], Equipment.Slot.WEAPON.getId());
        c.setSidebarInterface(1, 3917); // skills tab
        c.setSidebarInterface(2, 638); // quest tab (original)
        c.setSidebarInterface(3, 3213); // backpack tab
        c.setSidebarInterface(4, 1644); // items wearing tab
        c.setSidebarInterface(5, 5608); // pray tab
        c.setSidebarInterface(6, c.ancients == 1 ? 12855 : 1151); // magic spellbook
       // c.setSidebarInterface(7, 37128); // clan chat tab
        c.setSidebarInterface(8, 5065); // friend
        c.setSidebarInterface(9, 5715); // ignore
        c.setSidebarInterface(10, 2449); // logout tab
        c.setSidebarInterface(11, 44500); // wrench tab - complete settings (fullscreen, zoom, key bindings, etc.)
        c.setSidebarInterface(12, 147);   // run/emotes tab (mystic keeps 147 here)
        c.setSidebarInterface(13, 962); // PvP/info 32000
    }
    public void clearTabs() {
        for (int i = 0; i <= 13; i++)
            ((Client) this).setSidebarInterface(i, -1); // attack tab
    }
    public void morphTab(String text) {
        clearTabs();
        ((Client) this).send(new SendSideTab(3));
        ((Client) this).send(new SendString(text, 6020));
        ((Client) this).setSidebarInterface(3, 6014);
    }
    public void unMorph() {
        resetTabs();
        morph = false;
        isNpc = false;
        setPlayerNpc(-1);
        getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
    }

    public void setHerbOptions() {
        Client c = ((Client) this);
        if(herbOptions.isEmpty()) {
            herbMaking = -1;
            DialogueService.setLegacyNextDialogueId(c, -1);
            return;
        }
        int slot = herbMaking;
        String[] text = new String[herbOptions.size() < 4 ? herbOptions.size() + 2 : herbOptions.size() - slot <= 3 ? herbOptions.size() - slot + 2 : 6];
        text[0] = "What do you wish me to do?";
        int position = Math.min(3, herbOptions.size() - slot);
        for(int i = 0; i < position; i++)
            text[i + 1] = c.GetItemName(herbOptions.get(slot + i).getId());
        text[position + 1] = text.length < 6 && slot == 0 ? "Close" : text.length == 6 ? "Next" : "Previous";
        if(text.length == 6)
            text[position + 2] = slot == 0 ? "Close" : "Previous";
        c.showPlayerOption(text);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.PLAYER;
    }

    public Chunk getCurrentChunk() {
        return currentChunk;
    }

    public ChunkRepository getChunkRepository() {
        return chunkRepository;
    }

    public void syncChunkMembership() {
        if (Server.chunkManager == null) {
            return;
        }

        Chunk newChunk = getPosition().getChunk();

        if (currentChunk != null && currentChunk.equals(newChunk) && chunkRepository != null) {
            return;
        }

        if (chunkRepository != null) {
            chunkRepository.remove(this);
        }

        ChunkRepository repo = Server.chunkManager.load(newChunk);
        repo.add(this);
        currentChunk = newChunk;
        chunkRepository = repo;
    }

    public void removeFromChunk() {
        if (chunkRepository != null) {
            chunkRepository.remove(this);
        }
        chunkRepository = null;
        currentChunk = null;
    }

}
