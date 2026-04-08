/**
 *
 */
package net.dodian.uber.game.model.entity.npc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.util.ArrayList;

/**
 * @author Owner
 *
 */
public class NpcData {

    private static final Logger logger = LoggerFactory.getLogger(NpcData.class);

    private final ArrayList<NpcDrop> drops = new ArrayList<>();
    private String name = "", examine="";
    private int attackEmote, deathEmote, respawn, combat, size;
    private final int[] level = new int[7];

    public NpcData(String name, String examine, int attackEmote, int deathEmote, int respawn, int combat, int size, int[] levels) {
        this.name = name == null ? "" : name;
        this.examine = examine;
        this.attackEmote = attackEmote;
        this.deathEmote = deathEmote;
        this.respawn = respawn;
        this.combat = combat;
        this.size = size;
        if (levels != null && levels.length >= this.level.length) {
            System.arraycopy(levels, 0, this.level, 0, this.level.length);
        }
    }

    public NpcData(ResultSet row) {
        try {
            NpcDefinitionData parsed = NpcDataInterop.fromResultSet(row);
            NpcData mapped = NpcDataInterop.toLegacy(parsed);
            attackEmote = mapped.attackEmote;
            deathEmote = mapped.deathEmote;
            name = mapped.name;
            examine = mapped.examine;
            respawn = mapped.respawn;
            combat = mapped.combat;
            size = mapped.size;
            System.arraycopy(mapped.level, 0, level, 0, level.length);
        } catch (Exception e) {
            logger.error("NpcData error while mapping result set", e);
        }
    }

    public void addDrop(int id, int min, int max, double percent, boolean rareShout) {
        drops.add(new NpcDrop(id, min, max, percent, rareShout));
    }

    /**
     * @return the drops
     */
    public ArrayList<NpcDrop> getDrops() {
        return drops;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name.replaceAll("_", " ");
    }
    public String getExamine() {
        return examine == null ? "" : examine.replaceAll("_", " ");
    }

    /**
     * @return the attackEmote
     */
    public int getAttackEmote() {
        return attackEmote;
    }

    public void setAttackEmote(int animationId) {
        this.attackEmote = animationId;
    }

    /**
     * @return the deathEmote
     */
    public int getDeathEmote() {
        return deathEmote;
    }

    /**
     * @return the respawn
     */
    public int getRespawn() {
        return respawn;
    }

    /**
     * @return the combat
     */
    public int getCombat() {
        return combat;
    }

    /**
     * @return the health
     */
    public int getHP() {
        return level[3];
    }

    /**
     * Returns the amount of tiles that the npc occupies on the game map.
     *
     */
    public int getSize() {
        return size;
    }

    public int[] getLevel() {
        return level;
    }
}
