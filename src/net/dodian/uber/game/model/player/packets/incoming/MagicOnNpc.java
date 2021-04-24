package net.dodian.uber.game.model.player.packets.incoming;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.UpdateFlag;
import net.dodian.uber.game.model.entity.npc.Npc;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Player.positions;
import net.dodian.uber.game.model.player.packets.Packet;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.uber.game.model.player.skills.Skill;
import net.dodian.uber.game.model.player.skills.slayer.SlayerTask;
import net.dodian.uber.game.model.player.skills.slayer.SlayerTask.slayerTasks;
import net.dodian.utilities.Utils;

public class MagicOnNpc implements Packet {

  @Override
  public void ProcessPacket(Client client, int packetType, int packetSize) {
    int npcIndex = client.getInputStream().readSignedWordBigEndianA();
    Npc tempNpc = Server.npcManager.getNpc(npcIndex);
    if (tempNpc == null)
      return;
    int magicID = client.getInputStream().readSignedWordA();
    int EnemyX2 = tempNpc.getPosition().getX();
    int EnemyY2 = tempNpc.getPosition().getY();
    int EnemyHP2 = tempNpc.getCurrentHealth();
    int hitDiff = 0;
    client.resetWalkingQueue();
    {
      try {
        if (EnemyHP2 < 1 || client.deathTimer > 0) {
          client.send(new SendMessage("That monster has already been killed!"));
          return;
        }
        if(!client.GoodDistance(EnemyX2, EnemyY2, client.getPosition().getX(), client.getPosition().getY(), 5)) {
          return;
        }
        int type = tempNpc.getId();

        SlayerTask.slayerTasks slayerTask = SlayerTask.slayerTasks.getSlayerNpc(type);
        boolean slayExceptions = slayerTask == null ||
        (slayerTask == slayerTasks.MUMMY && client.getPositionName(client.selectedNpc.getPosition()) == positions.KEYDUNG)
        ;
        if (!slayExceptions && slayerTask.isSlayerOnly() && (slayerTask.ordinal() != client.getSlayerData().get(1) || client.getSlayerData().get(3) <= 0)) {
            client.send(new SendMessage("You need a Slayer task to kill this monster."));
          return;
        }
        if(type == 2266) { //Prime slayer requirement
            if (client.getLevel(Skill.SLAYER) < 90) {
            	client.send(new SendMessage("You need a slayer level of 90 to harm this monster."));
                return;
              }
        }
        /* Dad 50 combat requirement */
        if (type == 4130) {
          if (client.determineCombatLevel() < 50) {
            client.send(new SendMessage("You must be level 50 or higher to attack Dad"));
            return;
          }
        }
        /* Key check mobs! */
        if (type == 1443 || type == 289) {
          if (!client.checkItem(1545) && client.getPositionName(client.selectedNpc.getPosition()) == positions.KEYDUNG) {
            client.resetPos();
            return;
          }
        }
        if (type == 4067 || type == 950) {
            if (!client.checkItem(1544) && client.getPositionName(client.selectedNpc.getPosition()) == positions.KEYDUNG) {
              client.resetPos();
              return;
            }
          }
        if (type == 3964 || type == 2075) {
            if (!client.checkItem(1543) && client.getPositionName(client.selectedNpc.getPosition()) == positions.KEYDUNG) {
              client.resetPos();
              return;
            }
          }

        int[] prem = { 1643, 158, 49, 1613 };
        for (int p = 0; p < prem.length; p++) {
          if (prem[p] == type && !client.premium) {
            client.resetPos();
            break;
          }
        }
        for (int i2 = 0; i2 < client.ancientId.length; i2++) {
          if (magicID == client.ancientId[i2]) {
            if (!client.runeCheck(magicID)) {
              client.send(new SendMessage("You are missing some of the runes required by this spell"));
              break;
            }
            client.deleteItem(565, 1);
            if (System.currentTimeMillis() - client.lastAttack < client.coolDown[client.coolDownGroup[i2]]) {
              break;
            }
            client.setInCombat(true);
            client.setLastCombat(System.currentTimeMillis());
            client.lastAttack = client.getLastCombat();
            if (client.getLevel(Skill.MAGIC) >= client.requiredLevel[i2]) {
              int dmg = client.baseDamage[i2] + (int) Math.ceil(client.playerBonus[11] * 0.5);
              double hit = Utils.random(dmg);
              if (hit >= EnemyHP2)
                hit = EnemyHP2;
              hitDiff = (int) hit;
              tempNpc.dealDamage(client, (int) hit, false);
              if (hit > 0 && tempNpc.getId() == 3200)
                tempNpc.addMagicHit(client);
              client.requestAnim(1979, 0);
              client.teleportToX = client.getPosition().getX();
              client.teleportToY = client.getPosition().getY();
              if (client.ancientType[i2] == 3) {
                // coolDown[coolDownGroup[i2]] = 35;
                // server.npcHandler.npcs[npcIndex].effects[0]
                // = 15;
                client.stillgfx(369, EnemyY2, EnemyX2);
              } else if (client.ancientType[i2] == 2) {
                client.stillgfx(377, EnemyY2, EnemyX2);
                // coolDown[coolDownGroup[i2]] = 12;
                client.setCurrentHealth(client.getCurrentHealth() + (int) (hit / 5));
                if (client.getCurrentHealth() > client.getLevel(Skill.HITPOINTS)) {
                  client.setCurrentHealth(client.getLevel(Skill.HITPOINTS));
                }
              } else {
                client.animation(78, EnemyY2, EnemyX2);
              }
            } else {
              client.send(new SendMessage("You need a magic level of " + client.requiredLevel[i2]));
            }
          }
        }
        client.setFocus(EnemyX2, EnemyY2);
        client.giveExperience(40 * hitDiff, Skill.MAGIC);
        client.giveExperience(hitDiff * 15, Skill.HITPOINTS);
        client.getUpdateFlags().setRequired(UpdateFlag.APPEARANCE, true);
      } catch (Exception e) {
        e.printStackTrace();
      }

    }
  }

}