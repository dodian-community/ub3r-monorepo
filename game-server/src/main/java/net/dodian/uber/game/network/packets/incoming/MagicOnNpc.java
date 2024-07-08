package net.dodian.uber.game.network.packets.incoming;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.entity.Entity;
import net.dodian.uber.game.model.entity.npc.Npc;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.item.Equipment;
import net.dodian.uber.game.network.packets.Packet;

import static net.dodian.uber.game.combat.ClientExtensionsKt.magicBonusDamage;
import static net.dodian.uber.game.combat.PlayerAttackCombatKt.*;
import net.dodian.uber.game.network.packets.outgoing.SendMessage;
import net.dodian.uber.game.model.player.skills.Skill;
import net.dodian.utilities.Misc;
import net.dodian.utilities.Utils;

public class MagicOnNpc implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        int npcIndex = client.getInputStream().readSignedWordBigEndianA();
        Npc tempNpc = Server.npcManager.getNpc(npcIndex);
        if (tempNpc == null) { //No null shiet here!
            return;
        }
        if (client.randomed || client.UsingAgility) {
            return;
        }
        client.target = tempNpc;
        client.magicId = client.getInputStream().readSignedWordA();
        int id = tempNpc.getId();
        int EnemyX2 = tempNpc.getPosition().getX();
        int EnemyY2 = tempNpc.getPosition().getY();
        int EnemyHP2 = tempNpc.getCurrentHealth();
        int distance = 5;

        int slot = -1, type = 0;
        for (int i2 = 0; i2 < client.ancientId.length && slot == -1; i2++) {
            if(client.magicId == client.ancientId[i2]) {
                slot = i2;
                type = i2%4;
            }
        }
        if(slot == -1) client.magicId = slot; //Negative if slot is -1!

        if (!client.goodDistanceEntity(tempNpc, distance) || (client.goodDistanceEntity(tempNpc, distance) && !canAttackNpc(client, id))) {
            return;
        }
        if(client.goodDistanceEntity(tempNpc, distance)) {
            client.resetWalkingQueue();
        }
        if(EnemyHP2 < 1 || client.deathTimer > 0) {
            client.send(new SendMessage("That monster has already been killed!"));
            return;
        }
            if (client.getLevel(Skill.MAGIC) < client.requiredLevel[slot] || client.getCombatTimer() > 0) {
                if(client.getLevel(Skill.MAGIC) < client.requiredLevel[slot]) client.send(new SendMessage("You need a magic level of " + client.requiredLevel[slot]));
                return;
            }
                if (client.runeCheck()) {
                    client.setLastCombat(16);
                    int hitDiff;
                    double extra = client.getLevel(Skill.MAGIC) * 0.195;
                    double critChance = client.getLevel(Skill.AGILITY) / 9D;
                    boolean hitCrit = Math.random() * 100 <= critChance * (client.getEquipment()[Equipment.Slot.SHIELD.getId()] == 4224 ? 1.5 : 1);
                    client.deleteItem(565, 1);
                    client.checkItemUpdate();
                    double dmg = client.baseDamage[slot] * magicBonusDamage(client);
                    double hit = client.blackMaskImbueEffect(type) ? 1.2 * dmg : dmg;
                    hitDiff = Utils.random((int) hit);
                    hitDiff = hitCrit ? hitDiff + (int)(Utils.dRandom2((extra))) : hitDiff;
                    if (hitDiff >= EnemyHP2) hitDiff = EnemyHP2;
                    client.setFocus(EnemyX2, EnemyY2);
                    client.requestAnim(1979, 0);
                    if(type == 2) { //Blood effect!
                        client.stillgfx(377, EnemyY2, EnemyX2);
                    } else if (type == 3) { //Freeze effect!
                        client.stillgfx(369, EnemyY2, EnemyX2);
                    } else
                        client.stillgfx(78, EnemyY2, EnemyX2);
                    client.giveExperience(40 * hitDiff, Skill.MAGIC);
                    client.giveExperience(hitDiff * 13, Skill.HITPOINTS);
                    tempNpc.dealDamage(client, hitDiff, hitCrit ? Entity.hitType.CRIT : Entity.hitType.STANDARD);

                    boolean chance = Misc.chance(8) == 1 && client.armourSet("ahrim");
                    if(chance && hitDiff > 0) { //Ahrim effect!
                        client.stillgfx(400, tempNpc.getPosition(), 100);
                        client.heal(hitDiff / 2);
                    } else if(slot == 2) //Heal effect!
                        client.heal(hitDiff / 3);
                }
    }

}