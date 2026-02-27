package net.dodian.uber.game.model.entity.player;

import net.dodian.uber.game.Constants;
import net.dodian.uber.game.Server;
import net.dodian.uber.game.netty.codec.ByteMessage;
import net.dodian.uber.game.netty.codec.ByteOrder;
import net.dodian.uber.game.netty.codec.ValueType;
import net.dodian.uber.game.event.Event;
import net.dodian.uber.game.event.EventManager;
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
import net.dodian.uber.game.party.Balloons;
import net.dodian.uber.game.party.RewardItem;
import net.dodian.utilities.Misc;
import net.dodian.utilities.Utils;

import java.util.*;

public abstract class Player extends Entity {
    public boolean yellOn = true, genie = false, antique = false, instaLoot = false;
    public long longName = 0;
    public int wildyLevel = 0;
    public long lastAction = 0, lastMagic = 0;
    public long lastPickAction = 0, lastAxeAction = 0, lastFishAction = 0;
    private int playerNpc = -1;
    public boolean premium = false, randomed = false, genieCombatFlag = false;
    public int playerGroup = 3, latestNews = 0, dbId = -1, questPage = 0, playerRights; //Online stuff!
    public int[] playerLooks = new int[13];
    public boolean saveNeeded = true, lookNeeded = false, discord = false;
    private int lastCombat = 0, combatTimer = 0, snareTimer = 0, stunTimer = 0;
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
    private int playerSE = 0x328; // SE = Standard Emotion
    private int playerSEW = 0x333; // SEW = Standard Emotion Walking
    private int playerSER = 0x338; // SER = Standard Emotion Run
    public boolean IsCutting = false, IsAnvil = false;
    public boolean isFiremaking = false;
    public PyramidPlunder getPlunder = new PyramidPlunder(((Client) this));
    public boolean attackingPlayer = false, attackingNpc = false;
    public int MyShopID = -1;
    public int NpcDialogue = 0, NpcTalkTo = 0, NpcWanneTalk = 0;
    public boolean IsBanking = false, isPartyInterface = false, checkBankInterface, NpcDialogueSend = false;
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
    public int playerBankSize = 800;
    public int[] bankItems = new int[playerBankSize];
    public int[] bankItemsN = new int[playerBankSize];
    public int pHairC, pTorsoC, pLegsC, pFeetC, pSkinC;
    private int pGender, pHead, pTorso, pArms, pHands, pLegs, pFeet, pBeard;
    private final int[] playerEquipment = new int[14];
    private final int[] playerEquipmentN = new int[14];
    private final int[] playerLevel = new int[21];
    private final int[] playerXP = new int[21];
    public int maxHealth = 10, currentHealth = getLevel(Skill.HITPOINTS);
    public int maxPrayer = 1, currentPrayer = getLevel(Skill.PRAYER);
    public final static int maxPlayerListSize = Constants.maxPlayers;
    public Player[] playerList = new Player[maxPlayerListSize]; // To remove -Dashboard
    public int playerListSize = 0;
    public Set<Player> playersUpdating = new HashSet<>();
    private int localPlayerSelectionCursor = 0;
    private final Set<Npc> localNpcs = new LinkedHashSet<>(254);
    private Chunk currentChunk;
    private ChunkRepository chunkRepository;
    public boolean loaded = false;
    private final boolean[] songUnlocked = new boolean[RegionSong.values().length];
    private int faceTarget = -1;
    public int[] newWalkCmdX = new int[WALKING_QUEUE_SIZE];
    public int[] newWalkCmdY = new int[WALKING_QUEUE_SIZE];
    public int[] tmpNWCX = new int[WALKING_QUEUE_SIZE];
    public int[] tmpNWCY = new int[WALKING_QUEUE_SIZE];
    public int newWalkCmdSteps = 0;
    public boolean newWalkCmdIsRunning = false;
    public int[] travelBackX = new int[WALKING_QUEUE_SIZE];
    public int[] travelBackY = new int[WALKING_QUEUE_SIZE];
    public int numTravelBackSteps = 0;
    private int graphicId = 0;
    private int graphicHeight = 0;
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
    private final ArrayList<Integer> slayerData = new ArrayList<>();
    private final ArrayList<Boolean> travelData = new ArrayList<>();
    private final ArrayList<Integer> paid = new ArrayList<>();
    private final ArrayList<Boolean> unlocked = new ArrayList<>();
    public int unlockLength = 2;
    public int lastRecoverEffect = 0, lastRecover = 4;
    public int[] boostedLevel = new int[21];
    public int chestEvent = 0;
    public boolean chestEventOccur = false;
    public ArrayList<Integer> effects = new ArrayList<>();
    public int dailyLogin = 1;
    public ArrayList<String> dailyReward = new ArrayList<>();
    public int staffSize = 5;

    public Player(int slot) {
        super(new Position(-1, -1, 0), slot, Entity.Type.PLAYER);
        teleportToX = teleportToY = -1;
        mapRegionX = mapRegionY = -1;
        currentX = currentY = teleportToZ = 0;
        resetWalkingQueue();
    }

    public boolean isShopping() {
        return MyShopID != -1;
    }

    public void bossCount(String name, int amount) {
        for(int i = 0; i < boss_name.length; i++) {
            if(boss_name[i].equalsIgnoreCase(name))
                boss_amount[i] = amount;
        }
    }

    public void defaultDailyReward(Client c) {
        dailyReward.add(0, c.today.getTime() + "");
        dailyReward.add(1, "6000"); //1 hour added to the timer for battlestaff
        dailyReward.add(2, "0");
        dailyReward.add(3, "0");
        dailyReward.add(4, "60");
    }
    public void battlestavesData() {
        if(dailyReward.isEmpty()) { //If size is empty do not send!
            return;
        }
        int time = Integer.parseInt(dailyReward.get(1));
        int amount = Integer.parseInt(dailyReward.get(2));
        int current = Integer.parseInt(dailyReward.get(3));
        int maxAmount = Integer.parseInt(dailyReward.get(4));
        if(current == maxAmount) { //Cant get anymore battlestaffs this day!
            return;
        }
        time -= 1;
        if(time <= 0) { //Need this just incase someone is in the negative!
            dailyReward.set(1, "6000");
            dailyReward.set(2, (amount + 20) + "");
            dailyReward.set(3, (current + 20) + "");
            ((Client) this).send(new SendMessage("<col=ff6200>You got "+(amount + 20)+" battlestaves that you can claim at Baba Yaga."));
        } else dailyReward.set(1, time + "");
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
            } else if(i == 2 && effects.get(i) == 0) { //Overload
                for(int skill = 0; skill < 4; skill++) {
                    skill = skill == 3 ? 4 : skill;
                    boostedLevel[skill] = 0;
                    c.refreshSkill(Skill.getSkill(skill));
                }
                addEffectTime(2, -1);
                c.send(new SendMessage("Your overload effect is now over!"));
            }
        }
    }
    private boolean antiFireEffect() {
        return effects.size() > 1 && effects.get(1) > 0;
    }

    public void defaultCharacterLook(Client temp) {
        int[] testLook = {0, 3, 14, 18, 26, 34, 38, 42, 2, 14, 5, 4, 0}; // DEfault look!
        System.arraycopy(testLook, 0, playerLooks, 0, 13);
        temp.setLook(playerLooks);
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
        releaseCachedUpdateBlock();
        removeFromChunk();
        getPosition().moveTo(-1, -1);
        mapRegionX = mapRegionY = -1;
        currentX = currentY = 0;
        resetWalkingQueue();
    }

    public void setTask(String input) {
        if (input.isEmpty())
            input = "-1,-1,0,0,0,0,-1";
        slayerData.clear();
        String[] tasks = input.split(",");
        for (String task : tasks) slayerData.add(Integer.parseInt(task));
        ensureSlayerDataSize();
    }
    public String saveTaskAsString() {
        StringBuilder tasks = new StringBuilder();
        for (int i = 0; i < slayerData.size(); i++) {
            tasks.append(slayerData.get(i).toString()).append(i == slayerData.size() - 1 ? "" : ",");
        }
        return tasks.toString();
    }
    public ArrayList<Integer> getSlayerData() {
        //"-1,-1,0,0,0,0,-1" //master, taskid, total, currentAmount, streak, points, partner
        ensureSlayerDataSize();
        return slayerData;
    }

    private void ensureSlayerDataSize() {
        final int[] defaultSlayerData = {-1, -1, 0, 0, 0, 0, -1};
        for (int i = slayerData.size(); i < defaultSlayerData.length; i++) {
            slayerData.add(defaultSlayerData[i]);
        }
    }

    public void setTravel(String input) {
        if (input.isEmpty()) input = "0:0:0:0:0";
        String[] travel = input.split(":");
        for (String s : travel) travelData.add(s.equals("1"));
    }
    public String saveTravelAsString() {
        StringBuilder travel = new StringBuilder();
        for (int i = 0; i < travelData.size(); i++) {
            int id = travelData.get(i) ? 1 : 0;
            travel.append(id).append(i == travelData.size() - 1 ? "" : ":");
        }
        return travel.toString();
    }
    public boolean getTravel(int i) {
        return travelData.get(i);
    }
    public void saveTravel(int i) {
        travelData.set(i, true);
    }
    public void addUnlocks(int i, String... check) {
            if(check.length == 1) {
                if(unlocked.isEmpty() || i == unlocked.size()) {
                    paid.add(i, -1);
                    unlocked.add(i, check[0].equals("1"));
                } else {
                    paid.set(i, -1);
                    unlocked.set(i, check[0].equals("1"));
                }
            } else if (check.length == 2) {
                if(unlocked.isEmpty() || i == unlocked.size()) {
                    unlocked.add(i, check[1].equals("1"));
                    paid.add(i, check[0].equals("1") ? 1 : 0);
                } else {
                    unlocked.set(i, check[1].equals("1"));
                    paid.set(i, check[0].equals("1") ? 1 : 0);
                }
            }
    }
    public String saveUnlocksAsString() {
        StringBuilder unlocks = new StringBuilder();
        for (int i = 0; i < unlocked.size(); i++) {
            int unlock = unlocked.get(i) ? 1 : 0;
            unlocks.append(paid.get(i) == -1 ? unlock + "" : paid.get(i) + "," + unlock).append(i == unlocked.size() - 1 ? "" : ":");
        }
        return unlocks.toString();
    }
    public boolean checkUnlock(int i) {
        return !unlocked.isEmpty() && unlocked.size() >= i && unlocked.get(i);
    }
    public int checkUnlockPaid(int i) {
        return paid.isEmpty() || paid.size() < i  ? -1 : paid.get(i);
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
        System.out.println("[player-" + getSlot() + "]: " + str);
    }

    public void println(String str) {
        System.out.println("[player-" + getSlot() + "]: " + str);
    }

    public String getSongUnlockedSaveText() {
        StringBuilder out = new StringBuilder();
        for (boolean b : songUnlocked) {
            out.append(b ? 1 : 0).append(" ");
        }
        return out.toString();
    }

    public boolean withinDistance(Object o) {
        if (getPosition().getZ() != o.z)
            return false;
        int deltaX = o.x - getPosition().getX(),
                deltaY = o.y - getPosition().getY();
        return deltaX <= 64 && deltaX >= -64 && deltaY <= 64 && deltaY >= -64;
    }

    public boolean withinDistance(Player otherPlr) {
        if (!otherPlr.isActive)
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
        return this.playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public WalkToTask getWalkToTask() {
        return this.walkToTask;
    }

    public void setWalkToTask(WalkToTask walkToTask) {
        this.walkToTask = walkToTask;
    }

    public int mapRegionX, mapRegionY; // the map region the player is
    // currently in
    private int currentX, currentY; // relative x/y coordinates (to map region)
    // Note that mapRegionX*8+currentX yields absX

    public static final int WALKING_QUEUE_SIZE = 80;
    public int[] walkingQueueX = new int[WALKING_QUEUE_SIZE], walkingQueueY = new int[WALKING_QUEUE_SIZE];
    public int wQueueReadPtr = 0; // points to slot for reading from queue
    public int wQueueWritePtr = 0; // points to (first free) slot for writing
    public boolean isRunning = false;
    public int teleportToX, teleportToY, teleportToZ; // contain absolute x/y
    public boolean walkingBlock = false;
    public void resetWalkingQueue() {
        walkingBlock = true;
        wQueueReadPtr = wQueueWritePtr = 0;
        newWalkCmdSteps = 0;
        // properly initialize this to make the "travel back" algorithm work
        for (int i = 0; i < WALKING_QUEUE_SIZE; i++) {
            walkingQueueX[i] = currentX;
            walkingQueueY[i] = currentY;
        }
    }

    public void addToWalkingQueue(int x, int y) {
        int next = (wQueueWritePtr + 1) % WALKING_QUEUE_SIZE;
        if (next == wQueueWritePtr)
            return; // walking queue full, silently discard the data
        walkingQueueX[wQueueWritePtr] = x;
        walkingQueueY[wQueueWritePtr] = y;
        wQueueWritePtr = next;
    }

    // returns 0-7 for next walking direction or -1, if we're not moving
    public int getNextWalkingDirection() {
        if (wQueueReadPtr == wQueueWritePtr)
            return -1; // walking queue empty
        int dir;
        do {
            dir = Utils.direction(currentX, currentY, walkingQueueX[wQueueReadPtr], walkingQueueY[wQueueReadPtr]);
            if (dir == -1) {
                wQueueReadPtr = (wQueueReadPtr + 1) % WALKING_QUEUE_SIZE;
            } else {
                // Convert the 16-direction value to 8-direction by dividing by 2
                dir = dir / 2;
                // Ensure the direction is within 0-7 range
                if (dir < 0 || dir > 7) {
                    println_debug("Invalid direction calculated: " + dir);
                    resetWalkingQueue();
                    return -1;
                }
            }
        } while (dir == -1 && wQueueReadPtr != wQueueWritePtr);
        
        if (dir == -1) {
            return -1;
        }
        
        // Update position using the 8-direction deltas
        Position newPos = new Position(getPosition().getX(), getPosition().getY(), getPosition().getZ());
        int deltaX = Utils.directionDeltaX[dir];
        int deltaY = Utils.directionDeltaY[dir];
        
        currentX += deltaX;
        currentY += deltaY;
        newPos.move(deltaX, deltaY);
        getPosition().moveTo(newPos.getX(), newPos.getY());
        
        return dir;
    }

    // calculates directions of player movement, or the new coordinates when
    // teleporting
    private boolean didTeleport = false; // set to true if char did teleport in
    // this cycle
    private boolean mapRegionDidChange = false;
    public boolean firstSend = false;
    private int primaryDirection = -1, secondaryDirection = -1; // direction char is going in this cycle

    public void getNextPlayerMovement() {
        Client temp = (Client) this;
        mapRegionDidChange = false;
        didTeleport = false;
        primaryDirection = secondaryDirection = -1;

        if (teleportToX != -1 && teleportToY != -1) {
            mapRegionDidChange = true;
            if (mapRegionX != -1 && mapRegionY != -1) {
                // check, whether destination is within current map region
                int relX = teleportToX - mapRegionX * 8, relY = teleportToY - mapRegionY * 8;
                if (relX >= 2 * 8 && relX < 11 * 8 && relY >= 2 * 8 && relY < 11 * 8)
                    mapRegionDidChange = false;
            }
            if (mapRegionDidChange) {
                // after map region change the relative coordinates range
                // between 48 - 55+
                if (firstSend) {
                    temp.pLoaded = false;
                } else {
                    firstSend = true;
                }
                mapRegionX = (teleportToX >> 3) - 6;
                mapRegionY = (teleportToY >> 3) - 6;
                // playerListSize = 0; // completely rebuild playerList after
                // teleport AND map region change
            }
            currentX = teleportToX - 8 * mapRegionX;
            currentY = teleportToY - 8 * mapRegionY;
            Position newPos = new Position(teleportToX, teleportToY, teleportToZ);
            resetWalkingQueue();

            teleportToX = teleportToY = -1;
            teleportToZ = 0;
            didTeleport = true;
            temp.getPosition().moveTo(newPos.getX(), newPos.getY(), newPos.getZ());
        } else {

            primaryDirection = getNextWalkingDirection();
            if (primaryDirection == -1)
                return; // standing

            if (isRunning) {
                secondaryDirection = getNextWalkingDirection();
            }

            // check, if we're required to change the map region
            int deltaX = 0, deltaY = 0;
            if (currentX < 2 * 8) {
                deltaX = 4 * 8;
                mapRegionX -= 4;
                mapRegionDidChange = true;
            } else if (currentX >= 11 * 8) {
                deltaX = -4 * 8;
                mapRegionX += 4;
                mapRegionDidChange = true;
            }
            if (currentY < 2 * 8) {
                deltaY = 4 * 8;
                mapRegionY -= 4;
                mapRegionDidChange = true;
            } else if (currentY >= 11 * 8) {
                deltaY = -4 * 8;
                mapRegionY += 4;
                mapRegionDidChange = true;
            }

            if (mapRegionDidChange) {
                // have to adjust all relative coordinates
                if (firstSend) {
                    temp.pLoaded = false;
                } else {
                    firstSend = true;
                }
                currentX += deltaX;
                currentY += deltaY;
                for (int i = 0; i < WALKING_QUEUE_SIZE; i++) {
                    walkingQueueX[i] += deltaX;
                    walkingQueueY[i] += deltaY;
                }
            }
        }
    }

    public int getLevel(Skill skill) {
        return playerLevel[skill.getId()];
    }

    public int getExperience(Skill skill) {
        return playerXP[skill.getId()];
    }

    public void addExperience(int experience, Skill skill) {
        playerXP[skill.getId()] += experience;
    }

    public void setLevel(int level, Skill skill) {
        playerLevel[skill.getId()] = level;
    }

    public void setExperience(int experience, Skill skill) {
        playerXP[skill.getId()] = experience;
    }

    // handles anything related to character position basically walking, running
    // and standing
    // applies to only to "non-thisPlayer" charracters
    public void updatePlayerMovement(ByteMessage str) {
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
            str.putBits(1, 1);
            str.putBits(2, 1);
            str.putBits(3, Utils.xlateDirectionToClient[primaryDirection]);
            str.putBits(1, getUpdateFlags().isUpdateRequired() ? 1 : 0);
        } else {
            // send "running packet"
            str.putBits(1, 1);
            str.putBits(2, 2);
            str.putBits(3, Utils.xlateDirectionToClient[primaryDirection]);
            str.putBits(3, Utils.xlateDirectionToClient[secondaryDirection]);
            str.putBits(1, getUpdateFlags().isUpdateRequired() ? 1 : 0);
        }

    }

    public void addNewPlayer(Player plr, ByteMessage str, ByteMessage updateBlock) {
        int id = plr.getSlot();
        playerList[playerListSize++] = plr;
        playersUpdating.add(plr);
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

    public int getLocalPlayerSelectionCursor() {
        return localPlayerSelectionCursor;
    }

    public void advanceLocalPlayerSelectionCursor(int totalCandidates, int amount) {
        if (totalCandidates <= 0) {
            localPlayerSelectionCursor = 0;
            return;
        }
        if (amount <= 0) {
            amount = 1;
        }
        localPlayerSelectionCursor = (localPlayerSelectionCursor + amount) % totalCandidates;
    }

    // --- Cached player update block (for efficient multi-viewer reuse) ---
    private ByteMessage cachedUpdateBlock = null;
    private boolean cachedUpdateBlockValid = false;

    /**
     * Returns true if the cached update block can be reused this cycle.
     */
    public boolean isCachedUpdateBlockValid() {
        return cachedUpdateBlockValid
                && cachedUpdateBlock != null
                && cachedUpdateBlock.getBuffer().refCnt() > 0;
    }

    /**
     * Writes the cached block into the supplied stream. Caller must ensure it
     * is valid first.
     */
    public void writeCachedUpdateBlock(ByteMessage dst) {
        dst.putBytes(cachedUpdateBlock);
    }

    /**
     * Stores a freshly-built update block for re-use next time.
     */
    public void cacheUpdateBlock(ByteMessage src) {
        releaseCachedUpdateBlock();
        if (src != null) {
            src.retain();
            this.cachedUpdateBlock = src;
            this.cachedUpdateBlockValid = true;
            return;
        }
        this.cachedUpdateBlockValid = false;
    }

    /**
     * Invalidates the cached block (called whenever an update flag is set or
     * at the end of the tick).
     */
    public void invalidateCachedUpdateBlock() {
        this.cachedUpdateBlockValid = false;
        releaseCachedUpdateBlock();
    }

    private void releaseCachedUpdateBlock() {
        if (cachedUpdateBlock == null) {
            return;
        }
        if (cachedUpdateBlock.getBuffer().refCnt() > 0) {
            cachedUpdateBlock.release();
        }
        cachedUpdateBlock = null;
    }

    private final byte[] chatText = new byte[4096];
    private int chatTextSize = 0;  // Changed from byte to int to handle chat text > 127 chars
    private int chatTextEffects = 0, chatTextColor = 0;
    private String chatTextMessage = "";

    public byte[] getChatText() {
        return this.chatText;
    }

    public int getChatTextSize() {
        return this.chatTextSize;
    }

    public void setChatTextSize(int chatTextSize) {
        this.chatTextSize = chatTextSize;
    }

    public int getChatTextEffects() {
        return this.chatTextEffects;
    }

    public void setChatTextEffects(int chatTextEffects) {
        this.chatTextEffects = chatTextEffects;
    }

    public int getChatTextColor() {
        return this.chatTextColor;
    }

    public void setChatTextColor(int chatTextColor) {
        this.chatTextColor = chatTextColor;
    }

    public String getChatTextMessage() {
        return chatTextMessage;
    }

    public void setChatTextMessage(String chatTextMessage) {
        this.chatTextMessage = chatTextMessage == null ? "" : chatTextMessage;
    }

    public void clearUpdateFlags() {
        IsStair = false; //What is this?!
        faceTarget(-1);
        getUpdateFlags().clear();
        
        // Reset chat-related fields when clearing flags to prevent T2 packet size mismatches
        chatTextSize = 0;
        chatTextColor = 0;
        chatTextEffects = 0;
        // Note: chatText buffer doesn't need to be cleared as chatTextSize controls usage
        
        // Any change this tick means our cached block is no longer valid.
        invalidateCachedUpdateBlock();
    }

    public void faceTarget(int index) {
        this.faceTarget = index;
        getUpdateFlags().setRequired(UpdateFlag.FACE_CHARACTER, true);
    }
    public void faceNpc(int index) {
        faceTarget(index);
    }
    public void facePlayer(int index) {
        faceTarget(32768 + index);
    }

    public void gfx0(int gfx) {
        graphicId = gfx;
        graphicHeight = 65536;
        getUpdateFlags().setRequired(UpdateFlag.GRAPHICS, true);
    }

    public int getFaceTarget() {
        return this.faceTarget;
    }
    public abstract void process(); //Send every 600 ms



    public void postProcessing() {
        if (walkingBlock) {
            walkingBlock = false;
            return;
        }
        if (newWalkCmdSteps > 0) {
            int firstX = newWalkCmdX[0], firstY = newWalkCmdY[0]; // travel backwards to find a proper connection vertex
            int lastDir;
            boolean found = false;
            numTravelBackSteps = 0;
            int ptr = wQueueReadPtr;
            int dir = Utils.direction(currentX, currentY, firstX, firstY);
            if (dir != -1 && (dir & 1) != 0) { // we can't connect first and current directly
                do {
                    lastDir = dir;
                    if (--ptr < 0)
                        ptr = WALKING_QUEUE_SIZE - 1;

                    travelBackX[numTravelBackSteps] = walkingQueueX[ptr];
                    travelBackY[numTravelBackSteps++] = walkingQueueY[ptr];
                    dir = Utils.direction(walkingQueueX[ptr], walkingQueueY[ptr], firstX, firstY);
                    if (lastDir != dir) {
                        found = true;
                        break;
                    }
                } while (ptr != wQueueWritePtr);
            } else found = true;

            wQueueWritePtr = wQueueReadPtr;
            addToWalkingQueue(currentX, currentY);

            if (dir != -1 && (dir & 1) != 0) {
                for (int i = 0; i < numTravelBackSteps - 1; i++) {
                    addToWalkingQueue(travelBackX[i], travelBackY[i]);
                }
                int wayPointX2 = travelBackX[numTravelBackSteps - 1], wayPointY2 = travelBackY[numTravelBackSteps - 1];
                int wayPointX1, wayPointY1;
                if (numTravelBackSteps == 1) {
                    wayPointX1 = currentX;
                    wayPointY1 = currentY;
                } else {
                    wayPointX1 = travelBackX[numTravelBackSteps - 2];
                    wayPointY1 = travelBackY[numTravelBackSteps - 2];
                }
                dir = Utils.direction(wayPointX1, wayPointY1, wayPointX2, wayPointY2);
                if (!(dir == -1 || (dir & 1) != 0)) {
                    dir >>= 1;
                    int x = wayPointX1, y = wayPointY1;
                    while (x != wayPointX2 || y != wayPointY2) {
                        x += Utils.directionDeltaX[dir];
                        y += Utils.directionDeltaY[dir];
                        if ((Utils.direction(x, y, firstX, firstY) & 1) == 0) {
                            found = true;
                            break;
                        }
                    }
                    if (found)
                        addToWalkingQueue(wayPointX1, wayPointY1);
                }
            } else {
                for (int i = 0; i < numTravelBackSteps; i++) {
                    addToWalkingQueue(travelBackX[i], travelBackY[i]);
                }
            }
            // now we can finally add those waypoints because we made sure
            // about the connection to first
            for (int i = 0; i < newWalkCmdSteps; i++) {
                addToWalkingQueue(newWalkCmdX[i], newWalkCmdY[i]);
            }
        }
        isRunning = UsingAgility && System.currentTimeMillis() < walkBlock ? newWalkCmdIsRunning : buttonOnRun;
        newWalkCmdSteps = 0;
    }

    public boolean buttonOnRun = true;

    private int damageDealt = 0, damageDealt2;
    protected boolean IsStair = false;
    public int deathStage = 0;
    public long deathTimer = 0;

    public void appendMask400Update(ByteMessage buf) { // Forcemovement mask!
        buf.put(m4001, ValueType.SUBTRACT); // writeByteS
        buf.put(m4002, ValueType.SUBTRACT);
        buf.put(m4003, ValueType.SUBTRACT);
        buf.put(m4004, ValueType.SUBTRACT);
        buf.putShort(m4006, ByteOrder.BIG, ValueType.ADD); // writeWordBigEndianA
        buf.putShort(m4005, ValueType.ADD); // writeWordA
        buf.put(m4007, ValueType.SUBTRACT);
    }

    // PM Stuff
    public abstract void loadpm(long l, int world);

    public int Privatechat = 0;

    public abstract void sendpm(long name, int rights, byte[] chatmessage, int messagesize);

    public void dealDamage(Entity attacker, int amt, hitType type) {
        Client plr = ((Client) this);
        if(attacker != null && ((attacker.getType() == Type.NPC && ((Npc) attacker).getCurrentHealth() < 1) || (attacker.getType() == Type.PLAYER && ((Client) attacker).getCurrentHealth() < 1)))
            setLastCombat(16); //Sets this if the guy attacking is not below 1 health aka dead!
        if (deathStage >= 0 && getCurrentHealth() < 1)
            amt = 0;
        else if (amt > currentHealth) amt = currentHealth;
        double rolledChance = Math.random();
        double level = ((getLevel(Skill.PRAYER) + 1) / 8D) / 100D;
        double chance = level + 0.025; //(((Client) this).getEquipment()[3] == 11284 ? 0.1 : 0.0), maybe?!
        double dmg = ((Client) this).neglectDmg() / 10D;
        double reduceDamage = 1.0 - (dmg / 100);
        int oldDmg = amt;
        if (!(plr.inDuel && plr.duelRule[5]) && rolledChance <= chance && playerBonus[11] > 0 && oldDmg > 0) {
            amt = reduceDamage <= 0 ? 0 : (int)(amt * reduceDamage);
            if(amt != oldDmg)
                ((Client) this).send(new SendMessage("<col=FFD700>You neglected "+(amt == 0 ? "all" : "some")+" of the damage!"));
        }
        if (!getUpdateFlags().isRequired(UpdateFlag.HIT2)) {
            this.hitType2 = type;
            damageDealt2 = amt;
            getUpdateFlags().setRequired(UpdateFlag.HIT2, true);
        } else if(!getUpdateFlags().isRequired(UpdateFlag.HIT)) {
            this.hitType = type;
            damageDealt = amt;
            getUpdateFlags().setRequired(UpdateFlag.HIT, true);
        }
        setCurrentHealth(Math.max(getCurrentHealth() - amt, 0));
        ((Client) this).refreshSkill(Skill.HITPOINTS);
        plr.debug("Dealing " + amt + " damage to you (hp=" + currentHealth + ")");
        if (attacker instanceof Player) { //Pvp damage profile!
            int totalDmg;
            if (getDamage().containsKey(attacker)) {
                totalDmg = getDamage().get(attacker) + amt;
                getDamage().remove(attacker);
            } else
                totalDmg = amt;
            getDamage().put(attacker, totalDmg);
        }
        boolean veracEffect = Misc.chance(8) == 1 && armourSet("verac");
        if (veracEffect && amt > 0 && getCurrentHealth() > 0 && attacker instanceof Player) {
            ((Client) this).stillgfx(1041, attacker.getPosition(), 100);
            ((Player) attacker).dealDamage(plr, amt, type);
        } else if (veracEffect && amt > 0 && getCurrentHealth() > 0 && attacker instanceof Npc) {
            ((Client) this).stillgfx(1041, attacker.getPosition(), 100);
            ((Npc) attacker).dealDamage(plr, amt, type);
        }
    }

    public void dealDamage(int amt, Entity.hitType type, Entity attacker, Entity.damageType dmg) {
        Client plr = ((Client) this);
        Npc npc = ((Npc) attacker);
        if(dmg.equals(damageType.FIRE_BREATH)) { //Dragons new effect!
            boolean gotAntiEffect = plr.getEquipment()[Equipment.Slot.SHIELD.getId()] == 1540
                    || plr.getEquipment()[Equipment.Slot.SHIELD.getId()] == 11284
                    || prayers.isPrayerOn(Prayers.Prayer.PROTECT_MAGIC) || antiFireEffect();
            if(npc != null && npc.getId() == 239 && gotAntiEffect) amt /= 2;
            else if (npc != null && npc.getId() != 239 && gotAntiEffect) {
                amt *= 3; amt /= 10; //Ugly way to write reduce dmg by 70%
            } else plr.send(new SendMessage("You are badly burnt by the dragon fire!"));
        } else if(dmg.equals(damageType.MELEE) && prayers.isPrayerOn(Prayers.Prayer.PROTECT_MELEE)) amt /= 2;
        else if(dmg.equals(damageType.RANGED) && prayers.isPrayerOn(Prayers.Prayer.PROTECT_RANGE)) amt /= 2;
        else if(dmg.equals(damageType.MAGIC) && prayers.isPrayerOn(Prayers.Prayer.PROTECT_MAGIC)) amt /= 2;
        else if(dmg.equals(damageType.JAD_RANGED) && prayers.isPrayerOn(Prayers.Prayer.PROTECT_RANGE)) amt = 0;
        else if(dmg.equals(damageType.JAD_MAGIC) && prayers.isPrayerOn(Prayers.Prayer.PROTECT_MAGIC)) amt = 0;
        dealDamage(attacker, amt, type);
    }

    private void delayedHit(Entity source, Entity target, final int damage, Entity.hitType type, int delay) {
        if(source instanceof Client && target instanceof Npc) {
            final Client p = (Client) source;
            final Npc n = (Npc) target;
            EventManager.getInstance().registerEvent(new Event(delay) {

                public void execute() {
                    if(p.disconnected) {
                        stop();
                        return;
                    }
                    if(!n.alive) {
                        stop();
                        return;
                    }
                    n.dealDamage(p, damage, type);
                    stop();
                }

            });
        }
        if(source instanceof Client && target instanceof Client) {
            final Client p = (Client) source;
            final Client other = (Client) target;
            EventManager.getInstance().registerEvent(new Event(delay) {

                public void execute() {
                    if(p.disconnected) {
                        stop();
                        return;
                    }
                    if(other.disconnected || other.deathStage > 0) {
                        stop();
                        return;
                    }
                    other.receieveDamage(p, damage, type);
                    stop();
                }

            });
        }
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
        return this.songUnlocked[songId];
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
        for (boolean unlocked : songUnlocked) {
            if (!unlocked)
                return false;
        }
        return true;
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
        this.songUnlocked[songId] = unlocked;
    }

    /**
     * Gets the hash collection of the local npcs.
     *
     * @return the local npcs.
     */
    public Set<Npc> getLocalNpcs() {
        return localNpcs;
    }

    public boolean didTeleport() {
        return this.didTeleport;
    }

    public boolean didMapRegionChange() {
        return this.mapRegionDidChange;
    }

    public int getPrimaryDirection() {
        return this.primaryDirection;
    }

    public int getSecondaryDirection() {
        return this.secondaryDirection;
    }

    public int getCurrentX() {
        return this.currentX;
    }

    public int getCurrentY() {
        return this.currentY;
    }

    public void setGraphic(int graphicId, int graphicHeight) {
        this.graphicId = graphicId;
        this.graphicHeight = graphicHeight;
    }

    public int getGraphicId() {
        return this.graphicId;
    }

    public int getGraphicHeight() {
        return this.graphicHeight;
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
        return this.pGender;
    }

    public void setGender(int pGender) {
        this.pGender = pGender;
    }

    public int getTorso() {
        return this.pTorso;
    }

    public void setTorso(int pTorso) {
        this.pTorso = pTorso;
    }

    public int getArms() {
        return this.pArms;
    }

    public void setArms(int pArms) {
        this.pArms = pArms;
    }

    public int getLegs() {
        return this.pLegs;
    }

    public void setLegs(int pLegs) {
        this.pLegs = pLegs;
    }

    public int getHands() {
        return this.pHands;
    }

    public void setHands(int pHands) {
        this.pHands = pHands;
    }

    public int getFeet() {
        return this.pFeet;
    }

    public void setFeet(int pFeet) {
        this.pFeet = pFeet;
    }

    public int getBeard() {
        return this.pBeard;
    }

    public void setBeard(int pBeard) {
        this.pBeard = pBeard;
    }

    public int getHead() {
        return this.pHead;
    }

    public void setHead(int pHead) {
        this.pHead = pHead;
    }

    public int getStandAnim() {
        return this.playerSE;
    }

    public void setStandAnim(int playerSE) {
        this.playerSE = playerSE;
    }

    public int getWalkAnim() {
        return this.playerSEW;
    }

    public void setAgilityEmote(int walk, int run) {
        setWalkAnim(walk);
        setRunAnim(run);
        getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
    }

    public void setWalkAnim(int playerSEW) {
        this.playerSEW = playerSEW;
        this.getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
    }

    public int getRunAnim() {
        return this.playerSER;
    }

    public void setRunAnim(int playerSER) {
        this.playerSER = playerSER;
        this.getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
    }

    public int getPlayerNpc() {
        return this.playerNpc;
    }

    public void setPlayerNpc(int playerNpc) {
        this.playerNpc = playerNpc;
    }

    public int getDamageDealt() {
        return this.damageDealt;
    }
    public int getDamageDealt2() {
        return this.damageDealt2;
    }
    public Entity.hitType getHitType() {
        return this.hitType;
    }
    public Entity.hitType getHitType2() {
        return this.hitType2;
    }

    public int getCurrentHealth() {
        return this.currentHealth;
    }
    public int getMaxHealth() {
        return this.maxHealth;
    }
    public void setCurrentHealth(int currentHealth) {
        this.currentHealth = currentHealth;
    }
    public void heal(int healing) {
        heal(healing, 0);
    }
    public void heal(int healing, int overHeal) {
        Client c = (Client) this;
        int maxLevel = getMaxHealth() + overHeal;
        setCurrentHealth(Math.min(getCurrentHealth() + healing, maxLevel));
        c.refreshSkill(Skill.HITPOINTS);
    }
    public void eat(int healing, int removeId, int removeSlot) {
        Client c = (Client) this;
        if (c.deathStage > 0 || c.deathTimer > 0 || c.getCurrentHealth() < 1) {
            return;
        }
        if(getCurrentHealth() < getMaxHealth()) {
            c.requestAnim(829, 0);
            c.deleteItem(removeId, removeSlot, 1);
            c.send(new SendMessage("You eat the " + Server.itemManager.getName(removeId).toLowerCase() + "."));
            heal(healing);
        } else c.send(new SendMessage("You have full health already, so you spare the "+ Server.itemManager.getName(removeId).toLowerCase() +" for later."));
    }

    public void boost(int boosted, Skill skill) {
        if (skill == null)
            return;

        if(skill == Skill.HITPOINTS || skill == Skill.PRAYER) //Cant do health or prayer with this method!
            return;

        Client c = (Client) this;
        int lvl = Skills.getLevelForExperience(getExperience(skill));
        int currentLevel = c.getLevel(skill);
        boosted = currentLevel >= lvl + boosted ? currentLevel - lvl : boosted;
        boostedLevel[skill.getId()] = boosted;
        c.refreshSkill(skill);
    }

    public int getCurrentPrayer() {
        return this.currentPrayer;
    }
    public int getMaxPrayer() {
        return this.maxPrayer;
    }
    public void setCurrentPrayer(int amount) {
        this.currentPrayer = amount;
    }
    public void drainPrayer(int amount) {
        pray(-amount);
        if(getCurrentPrayer() <= 0) {
            setCurrentPrayer(0);
            prayers.reset();
            ((Client) this).send(new SendMessage("<col=8B8000>Your prayer has ran out! Please recharge at a nearby altar!"));
        }
    }
    public void pray(int healing) {
        Client c = (Client) this;
        int maxLevel = getMaxPrayer();
        setCurrentPrayer(Math.min(getCurrentPrayer() + healing, maxLevel));
        c.refreshSkill(Skill.PRAYER);
    }

    public boolean isInCombat() {
        return getLastCombat() > 0;
    }

    public int getLastCombat() {
        return lastCombat;
    }
    public void setLastCombat(int lastCombat) {
        this.lastCombat = lastCombat;
    }

    public int getCombatTimer() {
        return combatTimer;
    }
    public void setCombatTimer(int timer) {
        this.combatTimer = timer;
    }
    public int getStunTimer() {
        return stunTimer;
    }
    public void setStunTimer(int timer) {
        this.stunTimer = timer;
    }
    public int getSnareTimer() {
        return snareTimer;
    }
    public void setSnareTimer(int timer) {
        this.snareTimer = timer;
    }

    /**
     * Calculates and returns the combat level for this player.
     *
     * @return the combat level.
     */
    public int determineCombatLevel() {
        int magLvl = Skills.getLevelForExperience(getExperience(Skill.MAGIC));
        int ranLvl = Skills.getLevelForExperience(getExperience(Skill.RANGED));
        int attLvl = Skills.getLevelForExperience(getExperience(Skill.ATTACK));
        int strLvl = Skills.getLevelForExperience(getExperience(Skill.STRENGTH));
        int defLvl = Skills.getLevelForExperience(getExperience(Skill.DEFENCE));
        int hitLvl = Skills.getLevelForExperience(getExperience(Skill.HITPOINTS));
        int prayLvl = Skills.getLevelForExperience(getExperience(Skill.PRAYER));
        double mag = magLvl * 1.5;
        double ran = ranLvl * 1.5;
        double attstr = attLvl + strLvl;
        double combatLevel;
        if (ran > attstr && ran > mag) { // player is ranged class
            combatLevel = ((double)(defLvl) * 0.25) + ((double)(hitLvl) * 0.25) + ((double)(prayLvl / 2) * 0.25) + ((double)(ranLvl) * 0.4875);
        } else if (mag > attstr) { // player is mage class
            combatLevel = (((double)(defLvl) * 0.25) + ((double)(hitLvl) * 0.25) + ((double)(prayLvl / 2) * 0.25) + ((double)(magLvl) * 0.4875));
        } else {
            combatLevel = (((double)(defLvl) * 0.25) + ((double)(hitLvl) * 0.25) + ((double)(prayLvl / 2) * 0.25) + ((double)(attLvl) * 0.325) + ((double)(strLvl) * 0.325));
        }
        return customCombat != -1 ? customCombat : (int)combatLevel;
    }

    public int getSkillLevel(Skill skill) {
        return Skills.getLevelForExperience(getExperience(skill));
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

    public int getSkillId(String name) {
        for (int i = 0; i < Skill.values().length; i++)
            if (Skill.values()[i].getName().startsWith(name.toLowerCase()))
                return i;
        return -1;
    }

    public positions getPositionName(Position current) {
        for (positions pos : positions.values()) {
            if (current.getX() >= pos.coordValue[0] && current.getX() <= pos.coordValue[1] && current.getY() >= pos.coordValue[2] && current.getY() <= pos.coordValue[3])
                return pos;
        }
        return null;
    }

    public String getPositionName() {
        Position current = getPosition().copy();
        for (positions pos : positions.values()) {
            if (current.getX() >= pos.coordValue[0] && current.getX() <= pos.coordValue[1] && current.getY() >= pos.coordValue[2] && current.getY() <= pos.coordValue[3])
                return pos.name;
        }
        return "exploring the world.";
    }

    public static void openPage(Client c, String pageName) {
        try {
            c.send(new SendMessage(pageName + "#url#"));
        } catch (Exception e) {
            System.out.println("error opening page.." + e);
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
        int index = monsterName.size();
        if(index == 0) { //Add the first entry!
            monsterName.add(name);
            monsterCount.add(1);
        } else { //Sorting after first entry!
            ArrayList<String> nameClone = (ArrayList<String>) monsterName.clone();
            ArrayList<Integer> countClone = (ArrayList<Integer>) monsterCount.clone();
            monsterName.clear();
            monsterCount.clear();
            monsterName.add(name);
            monsterCount.add(1);
            for(int i = 0; i < nameClone.size(); i++) {
                monsterName.add(nameClone.get(i));
                monsterCount.add(countClone.get(i));
            }
        }
    }
    public int getMonsterIndex(String name) {
        int slot = -1;
        for(int i = 0; i < monsterName.size() && slot == -1; i++)
            if(monsterName.get(i).equals(name)) {
                slot = i;
            }
        return slot;
    }
    public void incrementMonsterLog(Npc npc) {
        String name = npc.npcName().toLowerCase();
        int index = getMonsterIndex(name);
        if(index >= 0)
            addMonsterLog(npc, index);
        else addMonsterName(name);
    }
    public void addMonsterLog(Npc npc, int index) {
        String name = npc.npcName().toLowerCase();
        int amount = index == -1 ? 0 : monsterCount.get(index);
        int newAmount = amount < 1048576 ? amount + 1 : amount;
        if(index == 0)
            monsterCount.set(index, newAmount);
        else if(index > 0) { //Sorting time!
            ArrayList<String> nameClone = (ArrayList<String>) monsterName.clone();
            ArrayList<Integer> countClone = (ArrayList<Integer>) monsterCount.clone();
            monsterName.clear();
            monsterCount.clear();
            nameClone.remove(index);
            countClone.remove(index);
            monsterName.add(name);
            monsterCount.add(newAmount);
            for(int i = 0; i < nameClone.size(); i++) {
                monsterName.add(nameClone.get(i));
                monsterCount.add(countClone.get(i));
            }
        }
    }
    public int monsterKC(Npc npc) {
        int index = getMonsterIndex(npc.npcName().toLowerCase());
        if(index >= 0)
            return monsterCount.get(index);
        return 0;
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
            c.send(new SendString("Loot from 1000 " + n.getName(), 5383));
            c.checkBankInterface = true;
            c.sendBank(lootedItem, lootedAmount);
            c.resetItems(5064);
            c.send(new InventoryInterface(5292, 5063));
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
            c.send(new SendString("Loot from 1000 Yanille Chest", 5383));
            c.checkBankInterface = true;
            c.sendBank(lootedItem, lootedAmount);
            c.resetItems(5064);
            c.send(new InventoryInterface(5292, 5063));
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
            c.send(new SendString("Loot from 1000 Yanille Chest", 5383));
            c.checkBankInterface = true;
            c.sendBank(lootedItem, lootedAmount);
            c.resetItems(5064);
            c.send(new InventoryInterface(5292, 5063));
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
            c.nextDiag = -1;
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
