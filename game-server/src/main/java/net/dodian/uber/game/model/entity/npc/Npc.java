package net.dodian.uber.game.model.entity.npc;

import net.dodian.uber.game.Server;
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
    private int moveX = 0, moveY = 0, direction = -1, defaultFace, viewX, viewY;
    private int damageDealt = 0;
    private String text = "";
    private int deathEmote;
    public NpcData data;
    private boolean fighting = false;
    private Map<Integer, Client> enemies = new HashMap<Integer, Client>();
    private final int TIME_ON_FLOOR = 500;
    private int[] level = new int[7];
    private boolean walking = false;
    private long lastChatMessage;

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
            CalculateMaxHit();
            this.currentHealth = data.getHP();
            this.maxHealth = data.getHP();

            //Boss?
            if (id == 4130) { //Dad
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
            } else if (id == 3957) { //Ungadulu
                boss = true;
            } else if (id == 239) { //King black dragon
                boss = true;
            } else if (id == 5311) { //Head Mourner
                boss = true;
            } else if (id == 1432) { //Black demon
                boss = true;
            } else if (id == 2261) { //Rock crab boss
                boss = true;
            } else if (id == 2266) { //Daganoth prime
                boss = true;
            }
            //3375, 2475 bosses?
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
            CalculateMaxHit();
            this.currentHealth = data.getHP() < 1 ? 0 : data.getHP() > this.currentHealth ? this.currentHealth + (data.getHP() - this.currentHealth) :
                    data.getHP() < this.currentHealth ? this.currentHealth - (this.currentHealth - data.getHP()) : this.currentHealth;
            this.maxHealth = data.getHP();
        }
    }

    private ArrayList<Client> magicList = new ArrayList<Client>();

    public void CalculateMaxHit() {
        double MaxHit = 0;
        int Strength = getStrength(); // Strength
        MaxHit += (double) (Strength * 0.12);
        maxHit = (int) Math.floor(MaxHit);
    }

    public Client getClient(int index) {
        return ((Client) PlayerHandler.players[index]);
    }

    public boolean validClient(int index) {
        Client p = (Client) PlayerHandler.players[index];
        if (p != null && !p.disconnected && p.dbId > 0) {
            return true;
        }
        return false;
    }

    public void bossAttack() {
        Client enemy = getTarget(true);
        if (enemy == null) {
            fighting = false;
            return;
        }
        if (enemy.deathStage > 0) {
            fighting = false;
            return;
        }
        requestAnim(data.getAttackEmote(), 0);
        setFocus(enemy.getPosition().getX(), enemy.getPosition().getY());
        getUpdateFlags().setRequired(UpdateFlag.FACE_COORDINATE, true);
        for (int i = 0; i < PlayerHandler.getPlayerCount() + 1; i++) {
            Client c = getClient(i);
            if (!validClient(i))
                continue;
            if ((c.selectedNpc != null && c.selectedNpc.getSlot() == getSlot() && c.attackingNpc)
                    || (!magicList.isEmpty() && magicList.contains(c))) {
                int damage = Utils.random(maxHit);
                c.dealDamage(damage, false);
            }
        }
        magicList.clear();
        lastAttack = System.currentTimeMillis();
    }

    public void attack_new() {
        requestAnim(data.getAttackEmote(), 0);
        Client enemy = getTarget(true);
        if (enemy == null) {
            fighting = false;
            return;
        }
        if (enemy.deathStage > 0) {
            fighting = false;
            return;
        }
        setFocus(enemy.getPosition().getX(), enemy.getPosition().getY());
        getUpdateFlags().setRequired(UpdateFlag.FACE_COORDINATE, true);
        int def = enemy.playerBonus[6];
        double blocked = Utils.dRandom(def / 17);
        int hitDiff = Utils.random2(maxHit);
        int hitChance = (int) ((combat * 1.5) - enemy.getLevel(Skill.DEFENCE) * 1.3); // Chance
        // to
        // hit
        // ;)
        if (hitChance < 15)
            hitChance = 15;
        if (hitChance > 70)
            hitChance = 70;
        double roll = Math.random() * 100;
        if (roll <= hitChance) {
        } else {
            hitDiff = 0;
        }
        hitDiff -= (int) blocked;
        if (hitDiff < 0)
            hitDiff = 0;
        enemy.send(new SendMessage("npc's % of hit = " + hitChance + ", hit = " + hitDiff));
        enemy.dealDamage(hitDiff, false);
        lastAttack = System.currentTimeMillis();
    }

    public boolean isAttackable() {
        if (maxHealth > 0)
            return true;
        return false;
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
        getUpdateFlags().clear();
        crit = false;
        direction = -1;
    }

    public void setText(String text) {
        this.text = text;
        getUpdateFlags().setRequired(UpdateFlag.FORCED_CHAT, true);
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
                "magic - " + this.getMagic(),
                "ranged - " + this.getRange(),
                "Max hit: " + this.maxHit
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
		/*if (client.morph) {
			return;
		}*/
        //hitDiff = client.instaKo ? hitDiff = currentHealth
        //	: GlobalEvents.event.equals(DifferentEvents.DOUBLE_NPC_DAMAGE) ? hitDiff * 2 : hitDiff;
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
        requestAnim(data.getAttackEmote(), 0);
        Client enemy = getTarget(true);
        if (enemy == null) {
            fighting = false;
            return;
        }
        setFocus(enemy.getPosition().getX(), enemy.getPosition().getY());
        getUpdateFlags().setRequired(UpdateFlag.FACE_COORDINATE, true);
        int def_bonus = enemy.checkObsidianWeapons() ? (int) (enemy.playerBonus[6] * 0.9) : enemy.playerBonus[6];
        int def = enemy.getLevel(Skill.DEFENCE);
        int rand = Utils.random(def + (def_bonus / 5));
        int rand_npc = Utils.random(getAttack());
        double blocked = (0.08 * (double) def_bonus) / 100;
        int hitDiff = 0;
        double bonus = getId() == 2261 && enraged(20000) ? 1.15 : 1.0;
        if (rand_npc > rand) {
            int new_max_hit = (int) Math.ceil(maxHit * (1 - blocked) * bonus);
            hitDiff = Utils.random(new_max_hit);
        }
        if (hitDiff < 0)
            hitDiff = 0;
        enemy.dealDamage(hitDiff, false);
        lastAttack = System.currentTimeMillis();
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
                if (p.getSlayerData().get(3) == 0) {
                    p.getSlayerData().set(4, p.getSlayerData().get(4) + 1);
                    p.giveExperience(p.getSlayerData().get(2) * maxHealth, Skill.SLAYER);
                    p.send(new SendMessage("You have completed your slayer task and gained some bonus experience!"));
                    p.triggerRandom(p.getSlayerData().get(2) * maxHealth);
                } else {
                    p.giveExperience(maxHealth * 10, Skill.SLAYER);
                    p.triggerRandom(maxHealth * 10);
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
                        GroundItem item = new GroundItem(getPosition().getX(), getPosition().getY(), drop.getId(), drop.getAmount(), pid, getId());
                        Ground.items.add(item);
                    } else {
                        for (int i = 0; i < drop.getAmount(); i++) {
                            GroundItem item = new GroundItem(getPosition().getX(), getPosition().getY(), drop.getId(), 1, pid, getId());
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
    }

  /*public Client getTarget() {
    for (int i = 0; i < 5; i++) {
      int maxDmg = 0, maxId = 0;
      if (getDamage().isEmpty()) {
        return null;
      }
      for (int id : getDamage().keySet()) {
        if (getDamage().get(id) > maxDmg && validClient(enemies.get(id))) {
          maxDmg = getDamage().get(id);
          maxId = id;
        }
      }
      if (!getDamage().containsKey(maxId))
        continue;
      Client enemy = enemies.get(maxId);
      if (!validClient(enemy) || distanceToPlayer(enemy) > 5) {
        getDamage().remove(maxId);
        enemies.remove(maxId);
      }
      return enemy;
    }
    return null;
  }*/

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
        return level[1];
    }

    public int getStrength() {
        return level[2];
    }

    public int getDefence() {
        return level[0];
    }

    public int getRange() {
        return level[4];
    }

    public int getMagic() {
        return level[6];
    }

    public String npcName() {
        return Server.npcManager.getName(id).replace("_", " ");
    }

    public String getText() {
        return this.text;
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

    public void addMagicHit(Client client) {
        synchronized (magicList) {
            magicList.add(client);
        }
    }

    public long getLastChatMessage() {
        return this.lastChatMessage;
    }

    public void setLastChatMessage() {
        lastChatMessage = System.currentTimeMillis();
    }

    public int getTimeOnFloor() {
        return id == 3127 ? 2500 : TIME_ON_FLOOR;
    }

    public NpcData getData() {
        return data;
    }

    public boolean enraged(int timer) {
        return inFrenzy != -1 && System.currentTimeMillis() - inFrenzy >= timer;
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

}