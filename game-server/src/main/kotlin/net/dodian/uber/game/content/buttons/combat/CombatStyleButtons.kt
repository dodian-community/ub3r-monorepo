package net.dodian.uber.game.content.buttons.combat

import net.dodian.uber.game.content.buttons.ButtonContent
import net.dodian.uber.game.model.combat.impl.CombatStyleHandler
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Player

object CombatStyleButtons : ButtonContent {
    override val buttonIds: IntArray = intArrayOf(
        1177, 1080, 14218, 22228, 48010, 21200, 6221, 6236, 17102, 8234, 30088, 18103, 9125, 6168,
        1175, 22229, 1078, 3015, 33019, 6169, 8235, 9126, 18078, 21201, 48008, 14219, 6219, 6234, 17100,
        14220, 33018, 48009, 9127, 18077, 18080, 18079,
        1079, 1176, 14221, 18106, 30091, 22230, 21203, 21202, 18105, 9128, 6170, 6171, 33020, 6220, 6235,
        17101, 8237, 8236,
    )

    override fun onClick(client: Client, buttonId: Int): Boolean {
        when (buttonId) {
            1177, 1080, 14218, 22228, 48010, 21200, 6221, 6236, 17102, 8234, 30088, 18103, 9125, 6168 -> {
                client.weaponStyle =
                    if (buttonId == 1177 || buttonId == 1080 || buttonId == 14218) Player.fightStyle.POUND
                    else if (buttonId == 22228) Player.fightStyle.PUNCH
                    else if (buttonId == 48010) Player.fightStyle.FLICK
                    else if (buttonId == 21200) Player.fightStyle.SPIKE
                    else if (buttonId == 6221 || buttonId == 6236 || buttonId == 17102) Player.fightStyle.ACCURATE
                    else if (buttonId == 8234) Player.fightStyle.STAB
                    else Player.fightStyle.CHOP
                client.fightType = 0
                CombatStyleHandler.setWeaponHandler(client)
                if (buttonId == 1080 && client.autocast_spellIndex != -1) {
                    client.resetAttack()
                    client.autocast_spellIndex = -1
                }
                return true
            }

            1175, 22229, 1078, 3015, 33019, 6169, 8235, 9126, 18078, 21201, 48008, 14219, 6219, 6234, 17100 -> {
                client.weaponStyle =
                    if (buttonId == 1175 || buttonId == 22229) Player.fightStyle.BLOCK_THREE
                    else if (buttonId == 33019) Player.fightStyle.FEND
                    else if (buttonId == 48008) Player.fightStyle.DEFLECT
                    else if (buttonId == 6219 || buttonId == 6234 || buttonId == 17100) Player.fightStyle.LONGRANGE
                    else Player.fightStyle.BLOCK
                client.fightType = 1
                CombatStyleHandler.setWeaponHandler(client)
                if (buttonId == 1078 && client.autocast_spellIndex != -1) {
                    client.resetAttack()
                    client.autocast_spellIndex = -1
                }
                return true
            }

            14220, 33018, 48009, 9127, 18077, 18080, 18079 -> {
                client.weaponStyle =
                    if (buttonId == 14220) Player.fightStyle.SPIKE
                    else if (buttonId == 33018) Player.fightStyle.JAB
                    else if (buttonId == 18077) Player.fightStyle.LUNGE
                    else if (buttonId == 18079) Player.fightStyle.POUND_CON
                    else if (buttonId == 18080) Player.fightStyle.SWIPE
                    else if (buttonId == 9127) Player.fightStyle.CONTROLLED
                    else Player.fightStyle.LASH
                client.fightType = 3
                CombatStyleHandler.setWeaponHandler(client)
                return true
            }

            1079, 1176, 14221, 18106, 30091, 22230, 21203, 21202, 18105, 9128, 6170, 6171, 33020, 6220,
            6235, 17101, 8237, 8236 -> {
                client.weaponStyle =
                    if (buttonId == 1079 || buttonId == 1176 || buttonId == 14221) Player.fightStyle.PUMMEL
                    else if (buttonId == 9128 || buttonId == 18106 || buttonId == 30091 || buttonId == 8236) Player.fightStyle.SLASH
                    else if (buttonId == 22230) Player.fightStyle.KICK
                    else if (buttonId == 21203) Player.fightStyle.IMPALE
                    else if (buttonId == 6170 || buttonId == 21202 || buttonId == 18105) Player.fightStyle.SMASH
                    else if (buttonId == 6171) Player.fightStyle.HACK
                    else if (buttonId == 33020) Player.fightStyle.SWIPE
                    else if (buttonId == 6220 || buttonId == 6235 || buttonId == 17101) Player.fightStyle.RAPID
                    else Player.fightStyle.LUNGE_STR
                client.fightType = 2
                CombatStyleHandler.setWeaponHandler(client)
                if (buttonId == 1079 && client.autocast_spellIndex != -1) {
                    client.resetAttack()
                    client.autocast_spellIndex = -1
                }
                return true
            }
        }
        return false
    }
}

