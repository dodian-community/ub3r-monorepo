package net.dodian.uber.game.model.entity.npc;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.event.Event;
import net.dodian.uber.game.event.EventManager;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.UpdateFlag;
import net.dodian.uber.game.model.entity.Entity;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.uber.game.model.item.Equipment;
import net.dodian.uber.game.model.item.Ground;
import net.dodian.uber.game.model.item.GroundItem;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.uber.game.model.player.packets.outgoing.SendString;
import net.dodian.uber.game.model.player.skills.Skill;
import net.dodian.uber.game.model.player.skills.slayer.SlayerTask;
import net.dodian.uber.game.security.DropLog;
import net.dodian.utilities.Misc;
import net.dodian.utilities.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Owner
 */
public class Npc extends Entity {
    public long inFrenzy = -1;
    public boolean hadFrenzy = false;
    private int id, currentHealth = 10, maxHealth = 10, respawn = 60, combat = 0, maxHit;
    public boolean alive = true, visible = true, boss = false;
    private long deathTime = 0, lastAttack = 0;
    public int[] boostedStat = {0, 0, 0, 0, 0}; //defence, attack, strength, magic dmg, range
    public int[] boostedStatOrig = {0, 0, 0, 0, 0}; //defence, attack, strength, magic dmg, range
    public long lastBoostedStat = System.currentTimeMillis();
    private int moveX = 0, moveY = 0, direction = -1, defaultFace, viewX, viewY;
    private int damageDealt = 0;
    private int deathEmote;
    public NpcData data;
    private boolean fighting = false;
    private Map<Integer, Client> enemies = new HashMap<Integer, Client>();
    private final int TIME_ON_FLOOR = 500;
    private int[] level = new int[7];
    private boolean walking = false;

    public Npc(int slot, int id, Position position, int face) {
        super(position.copy(), slot, Entity.Type.NPC);
        this.id = id;
        this.defaultFace = face;
        data = Server.npcManager.getData(id);
        if (data != null) {
            deathEmote = data.getDeathEmote();
            respawn = data.getRespawn();
            combat = data.getCombat();
            for (int i = 0; i < data.getLevel().length; i++) {
                level[i] = data.getLevel()[i];
            }
            CalculateMaxHit(true);
            this.currentHealth = data.getHP();
            this.maxHealth = data.getHP();

            //Boss?
            if (id == 4130) { //Dad
                boss = true;
            } else if (id == 3957) { //Ungadulu
                boss = true;
            } else if (id == 2585) { //Abyssal Guardian
                boss = true;
            } else if (id == 3964) { //San Tajalon
                boss = true;
            } else if (id == 4067) { //Black Knight Titan
                boss = true;
            } else if (id == 1443) { //Jungle demon
                boss = true;
            } else if (id == 8) { //Nechrayel
                boss = true;
            } else if (id == 4922) { //Ice queen
                boss = true;
            } else if (id == 239) { //King black dragon
                boss = true;
            } else if (id == 5311) { //Head Mourner
                boss = true;
            } else if (id == 1432) { //Black demon
                boss = true;
            } else if (id == 2266) { //Daganoth prime
                boss = true;
            } else if (id == 2261) { //Rock crab boss
                boss = true;
            } else if (id == 3127) { //Jad
                boss = true;
            }
        }
        alive = true;
    }

    public void reloadData() {
        data = Server.npcManager.getData(this.getId());
        if (data != null) {
            deathEmote = data.getDeathEmote();
            respawn = data.getRespawn();
            combat = data.getCombat();
            for (int i = 0; i < data.getLevel().length; i++) {
                level[i] = data.getLevel()[i];
            }
            CalculateMaxHit(true);
            this.currentHealth = data.getHP() < 1 ? 0 : data.getHP() > this.currentHealth ? this.currentHealth + (data.getHP() - this.currentHealth) :
                    data.getHP() < this.currentHealth ? this.currentHealth - (this.currentHealth - data.getHP()) : this.currentHealth;
            this.maxHealth = data.getHP();
        }
    }

    public void CalculateMaxHit(boolean melee) {
        double effectiveStrength = melee ? ((getStrength() + 1) + 8) : ((getRange() + 1) + 8);
        double maximumHit = 0.5 + effectiveStrength * (64 / 640D);
        this.maxHit = (int) Math.floor(maximumHit);
    }

    public Client getClient(int index) {
        return ((Client) PlayerHandler.players[index]);
    }

    public boolean validClient(int index) {
        Client p = (Client) PlayerHandler.players[index];
        return p != null && !p.disconnected && p.dbId > 0;
    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public int getFace() {
        return defaultFace;
    }
    public void setFace(int face) {
        defaultFace = face;
    }

    public void clearUpdateFlags() {
        direction = -1;
        crit = false;
        getUpdateFlags().clear();
    }

    public static int getCurrentHP(int i, int i1, int i2) {
        double x = (double) i / (double) i1;
        return (int) Math.round(x * i2);
    }

    public int getViewX() {
        return this.viewX;
    }

    public int getViewY() {
        return this.viewY;
    }

    public int getDirection() {
        return this.direction;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    public boolean isWalking() {
        return this.walking;
    }

    private boolean crit;

    public void showConfig(Client client) {
        int magicDamage = (int) Math.floor(maxHit * this.getMagic());
        CalculateMaxHit(false);
        int rangeMaxHit = maxHit;
        CalculateMaxHit(true);
        String[] commando = {
                "id - " + this.getId(),
                "name - " + data.getName(),
                "combat - " + data.getCombat(),
                "attackEmote - " + data.getAttackEmote(),
                "deathEmote - " + data.getDeathEmote(),
                "hitpoints - " + data.getHP(),
                "respawn - " + data.getRespawn(),
                "size - " + data.getSize(),
                "attack - " + this.getAttack(),
                "strength - " + this.getStrength(),
                "defence - " + this.getDefence(),
                "magic dmg - " + String.format("%3.1f", this.getMagic() * 100D) + "% (maxHit: " + magicDamage + ")",
                "ranged - " + this.getRange(),
                "Max hit Range: " + rangeMaxHit,
                "Max hit Melee: " + this.maxHit
        };
        client.send(new SendString("@dre@               Uber 3.0 Npc Configs", 8144));
        client.clearQuestInterface();
        int line = 8145;
        int count = 0;
        for (int i = 0; i < commando.length; i++) {
            client.send(new SendString(commando[i], line));
            line++;
            count++;
            if (line == 8146)
                line = 8147;
            if (line == 8196)
                line = 12174;
            if (count > 100)
                break;
        }
        client.sendQuestSomething(8143);
        client.showInterface(8134);
        client.flushOutStream();
    }

    public void dealDamage(Client client, int hitDiff, boolean crit) {
        if (!alive || (currentHealth < 1 && maxHealth > 0))
            hitDiff = 0;
        else if (hitDiff > currentHealth)
            hitDiff = currentHealth;
        this.crit = crit;
        getUpdateFlags().setRequired(UpdateFlag.HIT, true);
        currentHealth -= hitDiff;
        damageDealt = hitDiff;
        int dmg = damageDealt;
        if (validClient(client)) {
            if (getDamage().containsKey(client)) {
                dmg += getDamage().get(client);
                getDamage().remove(client);
            }
            getDamage().put(client, dmg);
            fighting = true;
        }
        if (currentHealth <= 0 && maxHealth > 0) {
            alive = false;
            if (currentHealth < 0)
                currentHealth = 0;
            die();
        }
    }

    public void attack() {
        if(this.getId() != 3127) {
            Client enemy = getTarget(true);
            if (enemy == null) {
                fighting = false;
                return;
            }
            if(!specialCondition(enemy)) {
                CalculateMaxHit(true);
                int hitDiff = landHit(enemy, true) ? Utils.random(maxHit) : 0;
                requestAnim(data.getAttackEmote(), 0);
                enemy.dealDamage(hitDiff, false);
                setLastAttack(System.currentTimeMillis());
            }
            setFocus(enemy.getPosition().getX(), enemy.getPosition().getY());
            getUpdateFlags().setRequired(UpdateFlag.FACE_COORDINATE, true);
        } else { //Jad!
            Client enemy = null;
            Client target = getTarget(true);
            for (Entity e : getDamage().keySet()) {
                if (e instanceof Player) {
                    if (fighting && (!getPosition().withinDistance(e.getPosition(), 6) || ((Player) e).getCurrentHealth() < 1))
                        continue;
                    enemy = Server.playerHandler.getClient(e.getSlot());
                    setFocus(target.getPosition().getX(), target.getPosition().getY());
                    getUpdateFlags().setRequired(UpdateFlag.FACE_COORDINATE, true);
                    int hitDiff = landHit(enemy, true) ? Utils.random(maxHit) : 0;
                    enemy.dealDamage(hitDiff, false);
                }
            }
            if(enemy == null) fighting = false;
            setLastAttack(System.currentTimeMillis());
        }
    }

    public boolean landHit(Client p, boolean melee) {
        double defLevel = p.getLevel(Skill.DEFENCE);
        double defBonus = 0.0;
        double atkLevel = (melee ? getAttack() : getRange()) * (getId() == 2261 && enraged(20000) ? 1.15 : 1.0);
        double atkBonus = 0.0;
        double NpcHitChance = 0.0;
        for(int i = 5; i <= 7; i++)
            if(p.playerBonus[i] > defBonus)
                defBonus = p.checkObsidianWeapons() ? (int) (p.playerBonus[i] * 0.9) : p.playerBonus[i];
        defBonus = !melee ? p.playerBonus[9] : defBonus;
        double playerDef = defLevel * (defBonus + 64D);
        double npcAccuracy = atkLevel * (atkBonus + 64D);
        if (npcAccuracy > playerDef)
            NpcHitChance = 1 - ((playerDef + 2) / (2 * (npcAccuracy + 1)));
        else
            NpcHitChance = npcAccuracy / (2 * (playerDef + 1));
        double chance = Misc.chance(100000) / 1000D;
        p.debug("Npc Accuracy Hit: " + (NpcHitChance*100) + "% out of " + chance + "%");
        return chance < (NpcHitChance*100);
    }

    public void addBossCount(Player p, int ID) {
        for (int i = 0; i < p.boss_name.length; i++) {
            if (npcName().toLowerCase().equals(p.boss_name[i].replace("_", " ").toLowerCase())) {
                if (p.boss_amount[i] >= 100000)
                    return;
                p.boss_amount[i] += 1;
            }
        }
    }

    public int killCount(Player p) {
        for (int i = 0; i < p.boss_name.length; i++)
            if (npcName().toLowerCase().equals(p.boss_name[i].replace("_", " ").toLowerCase()))
                return p.boss_amount[i];
        return 0;
    }

    public void die() {
        alive = false;
        fighting = false;
        deathTime = System.currentTimeMillis();
        requestAnim(deathEmote, 0);
        Client p = getTarget(false);
        if (p == null)
            return;
        if (boss) {
            addBossCount(p, id);
            if (id != 2266 && id != 1432 && id != 5311 && id != 2585) {
                String yell = npcName() + " has been slain by " + p.getPlayerName() + " (level-" + p.determineCombatLevel() + ")";
                p.yell("<col=FFFF00>System<col=000000> <col=292BA3>" + yell);
            }
        }

        SlayerTask.slayerTasks task = SlayerTask.slayerTasks.getSlayerNpc(id);
        if (task != null) {
            if (task.ordinal() == p.getSlayerData().get(1) && p.getSlayerData().get(3) > 0) {
                p.getSlayerData().set(3, p.getSlayerData().get(3) - 1);
                p.giveExperience(maxHealth * 11, Skill.SLAYER);
                p.triggerRandom(maxHealth * 11);
                    if(p.getSlayerData().get(3) == 0) { // Finish task!
                        p.getSlayerData().set(4, p.getSlayerData().get(4) + 1);
                        /* Bonus slayer experience 1k, 500, 250, 100, 50 and 10 tasks! */
                        int[] taskStreak = {1000, 500, 250, 100, 50, 10};
                        int[] experience = {50, 30, 20, 11, 6, 2};
                        int bonusXp = -1;
                        p.send(new SendMessage("You have completed your slayer task!"));
                        for(int i = 0; i < taskStreak.length && bonusXp == -1; i++)
                            if(p.getSlayerData().get(4)%taskStreak[i] == 0) {
                                bonusXp = experience[i] * p.getSlayerData().get(2) * maxHealth;
                                p.giveExperience(bonusXp, Skill.SLAYER);
                                p.send(new SendMessage("You have gained some bonus experience from finishing your " + taskStreak[i] + " task in a row."));
                            }
                    }
                }
        }
    }

    public void drop() {
        Client target = getTarget(false);
        if (target == null) {
            System.out.println("Target null.. Please investigate for " + getId());
            return;
        }
        int pid = target.getSlot();
        double rolledChance, currentChance, checkChance;
        int roll = 1;
        boolean wealth = target.getEquipment()[Equipment.Slot.RING.getId()] == 2572;
        boolean itemDropped;

        for (int rolls = 0; rolls < roll; rolls++) {
            rolledChance = Misc.chance(100000) / 1000D;
            itemDropped = false;
            currentChance = 0.0;
            for (NpcDrop drop : data.getDrops()) {
                if (drop == null) continue;
                checkChance = drop.getChance();
                if (wealth && drop.getChance() < 10.0)
                    checkChance += drop.getId() >= 5509 && drop.getId() <= 5515 ? 0.0 : drop.getChance() <= 1.0 ? 0.2 : 0.1;

                if (checkChance >= 100.0 || (checkChance + currentChance >= rolledChance && !itemDropped)) { // 100% items!
                    if (drop.getId() >= 5509 && drop.getId() <= 5515) //Just incase shiet!
                        if (target.checkItem(drop.getId()))
                            continue;
                    if (Server.itemManager.isStackable(drop.getId())) {
                        GroundItem item = new GroundItem(getPosition().getX(), getPosition().getY(), getPosition().getZ(), drop.getId(), drop.getAmount(), pid, getId());
                        Ground.items.add(item);
                    } else {
                        for (int i = 0; i < drop.getAmount(); i++) {
                            GroundItem item = new GroundItem(getPosition().getX(), getPosition().getY(), getPosition().getZ(), drop.getId(), 1, pid, getId());
                            Ground.items.add(item);
                        }
                    }
                    if (checkChance < 100.0)
                        itemDropped = true;
                    if (wealth && drop.getChance() < 10.0)
                        target.send(new SendMessage("<col=FF6347>Your ring of wealth shines more brightly!"));
                    if (drop.rareShout()) {
                        String yell = "<col=292BA3>" + target.getPlayerName() + " has recieved a "
                                + target.GetItemName(drop.getId()).toLowerCase() + " from " + npcName().toLowerCase() + (killCount(target) > 0 && boss ? " (Kill: " + killCount(target) + ")" : "");
                        target.yell("<col=FFFF00>System<col=000000> <col=FFFF00>" + yell);
                    }
                    DropLog.recordDrop(target, drop.getId(), drop.getAmount(), Server.npcManager.getName(id), getPosition().copy());
                } else if (!itemDropped && checkChance < 100.0)
                    currentChance += checkChance;
            }
        }
    }

    public void respawn() {
        getPosition().moveTo(getOriginalPosition().getX(), getOriginalPosition().getY());
        alive = true;
        visible = true;
        currentHealth = maxHealth;
        hadFrenzy = false;
        inFrenzy = -1;
        getDamage().clear();
        enemies.clear();
        for(int i = 0; i < boostedStat.length; i++) {
            boostedStatOrig[i] = 0;
            boostedStat[i] = 0;
        }
    }

    public Client getTarget(boolean fighting) {
        int highest = -1;
        Client killer = null;
        if (getDamage().isEmpty())
            return null;
        for (Entity e : getDamage().keySet()) {
            if (e instanceof Player) {
                if (fighting && (!getPosition().withinDistance(e.getPosition(), 6) || ((Player) e).getCurrentHealth() < 1))
                    continue;
                int damage = getDamage().get(e);
                if (damage > highest) {
                    highest = damage == 0 ? -1 : damage;
                    killer = (Client) e;
                }
            }
        }
        return killer;
    }
    public int getNextWalkingDirection() {
        int dir;
        dir = Utils.direction(getPosition().getX(), getPosition().getY(), (getPosition().getX() + moveX),
                (getPosition().getY() + moveY));
        if (dir == -1) {
            System.out.println("returning -1");
            return -1;
        }
        dir >>= 1;
        return dir;
    }

    /**
     * @return the alive
     */
    public boolean isAlive() {
        return alive;
    }

    /**
     * @param alive the alive to set
     */
    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    /**
     * @return the currentHealth
     */
    public int getCurrentHealth() {
        return currentHealth;
    }

    /**
     * @return the visible
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * @return the deathtime
     */
    public long getDeathTime() {
        return deathTime;
    }

    /**
     * @param visible the visible to set
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * @return the respawn
     */
    public int getRespawn() {
        return boss ? respawn - Math.max(30, PlayerHandler.getPlayerCount() - 1) : respawn;
    }

    /**
     * @return the fighting
     */
    public boolean isFighting() {
        return fighting;
    }

    public boolean validClient(Client c) {
        if (c != null && !c.disconnected && c.dbId > 0)
            return true;
        return false;
    }

    /**
     * @return the combat
     */
    public int getCombatLevel() {
        return combat;
    }

    /**
     * @return the lastAttack
     */
    public long getLastAttack() {
        return lastAttack;
    }

    public void setLastAttack(long lastAttack) {
        this.lastAttack = lastAttack;
    }

    public int getAttackTimer() {
        return getId() == 2261 && enraged(20000) ? 1200 : 2400;
    }

    public int distanceToPlayer(Client p) {
        return (int) Math.sqrt(Math.pow(p.getPosition().getX() - getPosition().getX(), 2)
                + Math.pow(p.getPosition().getY() - getPosition().getY(), 2));
    }

    public void removeEnemy(Client enemy) {
        if (getDamage().containsKey(enemy)) {
            getDamage().remove(enemy);
        }
        if (enemies.containsKey(enemy.dbId)) {
            enemies.remove(enemy.dbId);
        }
    }

    public int getHealth() {
        return maxHealth;
    }

    public int getAttack() {
        return level[1] + boostedStat[1];
    }

    public int getStrength() {
        return level[2] + boostedStat[2];
    }

    public int getDefence() {
        return level[0] + boostedStat[0];
    }

    public int getRange() {
        return level[4] + boostedStat[4];
    }

    public double getMagic() {
        double bonus = (level[6] + boostedStat[3]) / 10D;
        return bonus <= 0.0 ? 1.0 : (1.0 + (bonus / 100D));
    }

    public void boostedStat(int stat, int amount) {
        boostedStatOrig[stat] = amount;
        boostedStat[stat] = amount;
    }
    public void changeStat() {
        for(int i = 0; i < boostedStat.length; i++)
            boostedStat[i] = boostedStat[i] > 0 ? --boostedStat[i] : boostedStat[i] < 0 ? ++boostedStat[i] : 0;
        lastBoostedStat = System.currentTimeMillis();
    }

    public String npcName() {
        return Server.npcManager.getName(id).replace("_", " ");
    }

    public int getDamageDealt() {
        return this.damageDealt;
    }

    public boolean isCrit() {
        return this.crit;
    }

    public int getMaxHealth() {
        return this.maxHealth;
    }

    public int getTimeOnFloor() {
        return id == 3127 ? 2500 : TIME_ON_FLOOR;
    }

    public NpcData getData() {
        return data;
    }

    public boolean enraged(int timer) {
        return inFrenzy != -1 && !(System.currentTimeMillis() - inFrenzy >= timer);
    }
    public void calmedDown() {
        inFrenzy = -1;
        hadFrenzy = true;
    }

    public void sendFightMessage(String msg) {
        for (Entity e : getDamage().keySet()) {
            if (e instanceof Player) {
                if (e == null || (fighting && !getPosition().withinDistance(e.getPosition(), 12)))
                    continue;
                ((Client) e).send(new SendMessage(msg));
            }
        }
    }

    public void heal(int heal) {
        int maxHealth = getMaxHealth();
        currentHealth = currentHealth + heal > maxHealth ? maxHealth : currentHealth + heal;
    }

    public boolean specialCondition(Client c) {
        boolean attack = true;
        switch(getId()) {
            case 1611: //Battle mage
            case 2585: // Abyssal guardian boss!
                if(Misc.chance(3) == 1) {
                    int hitDiff = Utils.random((int)Math.floor(maxHit * this.getMagic()));
                    requestAnim(data.getAttackEmote() + 1, 0);
                    c.callGfxMask(getId() == 2585 ? 89 : 76, 100);
                    c.dealDamage(hitDiff, false);
                    setLastAttack(System.currentTimeMillis());
                } else attack = false;
            break;
            case 1432: //Black demon
                if(Misc.chance(4) == 1) { //Cast magic attack!
                    int hitDiff = Utils.random((int)Math.floor(maxHit * this.getMagic()));
                    requestAnim(69, 0);
                    c.stillgfx(381, c.getPosition().getY(), c.getPosition().getX());
                    c.dealDamage(hitDiff, false);
                    setLastAttack(System.currentTimeMillis());
                } else attack = false;
                break;
            case 1443: //Jungle demon
                if(Misc.chance(3) == 1) { //Cast magic attack!
                    boolean heal = Misc.random(2) == 1;
                    int hitDiff = Utils.random((int)Math.floor(maxHit * this.getMagic()));
                    requestAnim(69, 0);
                    c.stillgfx(heal ? 377 : 78, c.getPosition().getY(), c.getPosition().getX());
                    if(heal)
                        heal(hitDiff);
                    c.dealDamage(hitDiff, false);
                    setLastAttack(System.currentTimeMillis());
                } else attack = false;
            break;
            case 4922: //Ice queen
                if(Misc.chance(4) == 1) { //Cast magic attack!
                    int hitDiff = Utils.random((int)Math.floor(maxHit * this.getMagic()));
                    requestAnim(1979, 0);
                    c.stillgfx(369, c.getPosition().getY(), c.getPosition().getX());
                    c.dealDamage(hitDiff, false);
                    setLastAttack(System.currentTimeMillis());
                } else attack = false;
            break;
            case 3209: //Cave Horror
                if(Misc.chance(5) == 1) { //Cast range Attack
                    CalculateMaxHit(false);
                    int hitDiff = landHit(c, false) ? Utils.random(maxHit) : 0;
                    requestAnim(4237, 0);
                    c.stillgfx(378, c.getPosition().getY(), c.getPosition().getX());
                    c.dealDamage(hitDiff, false);
                    setLastAttack(System.currentTimeMillis());
                } else attack = false;
                break;
            case 3957: //Ungadulu
                if(Misc.chance(4) == 1) { //Cast range Attack
                    CalculateMaxHit(false);
                    int hitDiff = landHit(c, false) ? Utils.random(maxHit) : 0;
                    requestAnim(1978, 0);
                    c.stillgfx(180, c.getPosition().getY(), c.getPosition().getX());
                    c.dealDamage(hitDiff, false);
                    setLastAttack(System.currentTimeMillis());
                } else attack = false;
            break;
            case 3964: //San Tojalon
                if(Misc.chance(5) == 1 && (boostedStat[0] <= boostedStatOrig[0] / 2)) { //Cast boost
                    setText("Let the shadow protect me!");
                    requestAnim(5489, 0);
                    lastAttack += 600; //1 tick drink delay!
                    boostedStat(0, 60);
                } else attack = false;
                break;
            case 4067: //Black knight titan
                if(Misc.chance(5) == 1 && (boostedStat[2] <= boostedStatOrig[2] / 2)) { //Cast boost
                    setText("GRRRRR!");
                    requestAnim(129, 0);
                    lastAttack += 600; //1 tick drink delay!
                    boostedStat(2, 60);
                } else attack = false;
                break;
            case 8: //Nechrayel
                if(Misc.chance(6) == 1 && (boostedStat[0] <= boostedStatOrig[0] / 2)) { //Cast boost
                    setText("I HAVE AWAKEN!");
                    requestAnim(1529, 0);
                    lastAttack += 600; //1 tick drink delay!
                    for(int i = 0; i <= 2; i++)
                        boostedStat(i, 40);
                } else attack = false;
                break;
            case 2266: //Prime
                if(Misc.chance(5) == 1 && (boostedStat[4] <= boostedStatOrig[4] / 2)) { //Cast boost
                    setText("Rawr!");
                    requestAnim(2855, 0);
                    lastAttack += 600; //1 tick drink delay!
                    boostedStat(1, 60);
                    boostedStat(4, 60);
                } else if(Misc.chance(4) == 1) {
                    CalculateMaxHit(false);
                    int hitDiff = landHit(c, false) ? Utils.random(maxHit) : 0;
                    requestAnim(2854, 0);
                    c.stillgfx(406, c.getPosition().getY(), c.getPosition().getX());
                    c.dealDamage(hitDiff, false);
                    setLastAttack(System.currentTimeMillis());
                } else attack = false;
                break;
            case 5311: //Head Mourner
                if(Misc.chance(5) == 1) { //True melee damage!
                    setText("Get out of my farm!");
                    requestAnim(1203, 0);
                    c.dealDamage((int)(maxHit * 0.8), true);
                } else attack = false;
                break;
            case 239: //King black dragon
                int landChance = Misc.chance(15);
                if(landChance == 1) { //Fire breath, guarantee hit as crit with 50% reduce dmg as melee
                    setText("GRRRR!");
                    delayGfx(c, 81, 393, 2, (int)(maxHit * 0.5), true);
                    setLastAttack(System.currentTimeMillis());
                } else if(landChance == 5) { //Blue breath, magic dmg
                    setText("Grrrr!");
                    int hitDiff = Utils.random((int)Math.floor(maxHit * this.getMagic()));
                    delayGfx(c, 82, 396, 2, hitDiff, false);
                    setLastAttack(System.currentTimeMillis());
                } else if(landChance == 10) { //Green breath, range dmg
                    setText("RAWR!!");
                    CalculateMaxHit(false);
                    int hitDiff = Utils.random((int)Math.floor(maxHit));
                    delayGfx(c, 83, 394, 2, landHit(c, false) ? hitDiff : 0, false);
                    setLastAttack(System.currentTimeMillis());
                } else if(landChance == 15) { //White breath, melee dmg check with 20% increase dmg
                    setText("Rawr!!");
                    CalculateMaxHit(true);
                    int hitDiff = Utils.random((int)Math.floor(maxHit * 1.2));
                    delayGfx(c, 84, 395, 2, landHit(c, true) ? hitDiff : 0, false);
                    setLastAttack(System.currentTimeMillis());
                } else attack = false;
                break;
            default: attack = false;
        }
        if(!attack)
            setLastAttack(System.currentTimeMillis());
        return attack;
    }

    public void delayGfx(Client c, int anim, int gfx, int delay, int dmg, boolean crit) {
        requestAnim(anim, 0);
        EventManager.getInstance().registerEvent(new Event(delay * 600) {

            public void execute() {
                if(c.disconnected) {
                    stop();
                    return;
                }
                c.stillgfx(gfx, getPosition().getY(), getPosition().getX());
                c.dealDamage(dmg, crit);
                stop();
            }

        });
    }

}