@file:Suppress("unused")

package net.dodian.uber.game.ui

import net.dodian.uber.game.content.combat.style.CombatStyleService
import net.dodian.uber.game.engine.util.Misc

object MagicInterface : InterfaceButtonContent {
    private const val NORMAL_INTERFACE_ID = 1151
    private const val ANCIENT_INTERFACE_ID = 12855

    private val spellbookToggleButtons = intArrayOf(74212, 49047, 49046, 23024)
    private val autocastClearButtons = intArrayOf(1097, 1094, 1093)
    private val autocastSelectButtons = intArrayOf(51133, 51185, 51091, 24018, 51159, 51211, 51111, 51069, 51146, 51198, 51102, 51058, 51172, 51224, 51122, 51080)
    private val autocastRefreshButtons = intArrayOf(24017)

    private class TeleportBinding(
        val componentId: Int,
        val componentKey: String,
        val rawButtonIds: IntArray,
        val x: Int,
        val xRand: Int,
        val y: Int,
        val yRand: Int,
        val premium: Boolean,
    )

    private val teleports =
        listOf(
            TeleportBinding(0, "magic.teleport.yanille", intArrayOf(21741, 75010, 84237), 2604, 6, 3101, 3, false),
            TeleportBinding(1, "magic.teleport.seers", intArrayOf(13035, 4143, 50235), 2722, 6, 3484, 2, false),
            TeleportBinding(2, "magic.teleport.ardougne", intArrayOf(13045, 4146, 50245), 2660, 4, 3306, 4, false),
            TeleportBinding(3, "magic.teleport.catherby", intArrayOf(13053, 4150, 50253), 2802, 4, 3432, 3, false),
            TeleportBinding(4, "magic.teleport.legends_guild", intArrayOf(13061, 6004, 51005), 2726, 5, 3346, 2, false),
            TeleportBinding(5, "magic.teleport.taverly", intArrayOf(13069, 6005, 51013), 2893, 4, 3454, 3, false),
            TeleportBinding(6, "magic.teleport.fishing_guild", intArrayOf(13079, 29031, 51023), 2596, 3, 3406, 4, true),
            TeleportBinding(7, "magic.teleport.gnome_village", intArrayOf(13087, 72038, 51031), 2472, 6, 3436, 3, false),
            TeleportBinding(8, "magic.teleport.edgeville", intArrayOf(13095, 4140, 51039), 3085, 4, 3488, 4, false),
        )

    override val bindings =
        buildList {
            add(
                buttonBinding(
                    interfaceId = NORMAL_INTERFACE_ID,
                    componentId = 0,
                    componentKey = "magic.spellbook_toggle",
                    rawButtonIds = spellbookToggleButtons,
                ) { client, _ ->
                    if (client.ancients == 1) {
                        client.setSidebarInterface(6, 1151)
                        client.ancients = 0
                        client.sendMessage("Normal magic enabled")
                    } else {
                        client.setSidebarInterface(6, 12855)
                        client.ancients = 1
                        client.sendMessage("Ancient magic enabled")
                    }
                    true
                },
            )
            add(
                buttonBinding(
                    interfaceId = ANCIENT_INTERFACE_ID,
                    componentId = 1,
                    componentKey = "magic.autocast.clear",
                    rawButtonIds = autocastClearButtons,
                ) { client, _ ->
                    client.autocast_spellIndex = -1
                    client.setSidebarInterface(0, 1689)
                    true
                },
            )
            add(
                buttonBinding(
                    interfaceId = ANCIENT_INTERFACE_ID,
                    componentId = 2,
                    componentKey = "magic.autocast.select",
                    rawButtonIds = autocastSelectButtons,
                ) { client, request ->
                    for (index in client.ancientButton.indices) {
                        if (client.autocast_spellIndex == -1 && request.rawButtonId == client.ancientButton[index]) {
                            client.autocast_spellIndex = index
                        }
                    }
                    CombatStyleService.refreshWeaponStyleUi(client)
                    true
                },
            )
            add(
                buttonBinding(
                    interfaceId = ANCIENT_INTERFACE_ID,
                    componentId = 3,
                    componentKey = "magic.autocast.refresh",
                    rawButtonIds = autocastRefreshButtons,
                ) { client, _ ->
                    CombatStyleService.refreshWeaponStyleUi(client)
                    true
                },
            )
            teleports.forEach { teleport ->
                add(
                    buttonBinding(
                        interfaceId = ANCIENT_INTERFACE_ID,
                        componentId = teleport.componentId,
                        componentKey = teleport.componentKey,
                        rawButtonIds = teleport.rawButtonIds,
                    ) { client, _ ->
                        client.triggerTele(
                            teleport.x + Misc.random(teleport.xRand),
                            teleport.y + Misc.random(teleport.yRand),
                            0,
                            teleport.premium,
                        )
                        true
                    },
                )
            }
        }
}
