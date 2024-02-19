package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.entity.npc.Npc;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.item.Equipment;
import net.dodian.uber.game.model.player.packets.Packet;

import static net.dodian.uber.game.combat.ClientExtensionsKt.magicBonusDamage;
import static net.dodian.uber.game.combat.PlayerAttackCombatKt.*;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.uber.game.model.player.skills.Skill;
import net.dodian.utilities.Utils;

public class MagicOnNpc implements Packet {

    @Override
    public void ProcessPacket(Client client, int packetType, int packetSize) {
        int npcIndex = client.getInputStream().readSignedWordBigEndianA();
        Npc tempNpc = Server.npcManager.getNpc(npcIndex);
        if (tempNpc == null) { //No null shiet here!
            return;
        }
        client.target = tempNpc;
        int magicID = client.getInputStream().readSignedWordA();
        int id = tempNpc.getId();
        int EnemyX2 = tempNpc.getPosition().getX();
        int EnemyY2 = tempNpc.getPosition().getY();
        int EnemyHP2 = tempNpc.getCurrentHealth();
        int distance = 4;

        if (!client.goodDistanceEntity(tempNpc, distance) || (client.goodDistanceEntity(tempNpc, distance) && !canAttackNpc(client, id))) {
            return;
        }
        if(client.goodDistanceEntity(tempNpc, distance)) {
            client.stopMovement();
        }
        if(EnemyHP2 < 1 || client.deathTimer > 0) {
            client.send(new SendMessage("That monster has already been killed!"));
            return;
        }
            int slot = -1, type = 0;
            for (int i2 = 0; i2 < client.ancientId.length && slot == -1; i2++) {
                if(magicID == client.ancientId[i2]) {
                    slot = i2;
                    type = i2%4;
                }
            }
            if (System.currentTimeMillis() - client.lastAttack < client.coolDown[type]) {
                return;
            }
            if (client.getLevel(Skill.MAGIC) < client.requiredLevel[slot]) {
                client.send(new SendMessage("You need a magic level of " + client.requiredLevel[slot]));
                return;
            }
                if (client.runeCheck()) {
                    int hitDiff;
                    double extra = client.getLevel(Skill.MAGIC) * 0.195;
                    double critChance = client.getLevel(Skill.AGILITY) / 9D;
                    boolean hitCrit = Math.random() * 100 <= critChance * (client.getEquipment()[Equipment.Slot.SHIELD.getId()] == 4224 ? 1.5 : 1);
                    client.deleteItem(565, 1);
                    double dmg = client.baseDamage[slot] * magicBonusDamage(client);
                    double hit = client.blackMaskImbueEffect(type) ? 1.2 * dmg : dmg;
                    hitDiff = Utils.random((int) hit);
                    hitDiff = hitCrit ? hitDiff + (int)(Utils.dRandom2((extra))) : hitDiff;
                    if (hitDiff >= EnemyHP2)
                        hitDiff = EnemyHP2;
                    client.requestAnim(1979, 0);
                    if(type == 2) { //Blood effect!
                        client.stillgfx(377, EnemyY2, EnemyX2);
                        client.heal(hitDiff/3);
                    } else if (type == 3) { //Freeze effect!
                        client.stillgfx(369, EnemyY2, EnemyX2);
                    } else
                        client.stillgfx(78, EnemyY2, EnemyX2);
                    client.setFocus(EnemyX2, EnemyY2);
                    client.giveExperience(40 * hitDiff, Skill.MAGIC);
                    client.giveExperience(hitDiff * 15, Skill.HITPOINTS);
                    tempNpc.dealDamage(client, hitDiff, hitCrit);
                    client.lastAttack = System.currentTimeMillis();
                }
    }

}