package net.dodian.uber.game.content.interfaces.magic

import net.dodian.uber.game.combat.style.CombatStyleService
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.utilities.Misc
import net.dodian.uber.game.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.ui.buttons.buttonBinding

object MagicInterfaceButtons : InterfaceButtonContent {
    override val bindings =
        buildList {
            add(
                buttonBinding(
                    interfaceId = MagicComponents.NORMAL_INTERFACE_ID,
                    componentId = 0,
                    componentKey = "magic.spellbook_toggle",
                    rawButtonIds = MagicComponents.spellbookToggleButtons,
                ) { client, _ ->
                    if (client.ancients == 1) {
                        client.setSidebarInterface(6, 1151)
                        client.ancients = 0
                        client.send(SendMessage("Normal magic enabled"))
                    } else {
                        client.setSidebarInterface(6, 12855)
                        client.ancients = 1
                        client.send(SendMessage("Ancient magic enabled"))
                    }
                    true
                }
            )
            add(
                buttonBinding(
                    interfaceId = MagicComponents.ANCIENT_INTERFACE_ID,
                    componentId = 1,
                    componentKey = "magic.autocast.clear",
                    rawButtonIds = MagicComponents.autocastClearButtons,
                ) { client, _ ->
                    client.autocast_spellIndex = -1
                    client.setSidebarInterface(0, 1689)
                    true
                }
            )
            add(
                buttonBinding(
                    interfaceId = MagicComponents.ANCIENT_INTERFACE_ID,
                    componentId = 2,
                    componentKey = "magic.autocast.select",
                    rawButtonIds = MagicComponents.autocastSelectButtons,
                ) { client, request ->
                    for (index in client.ancientButton.indices) {
                        if (client.autocast_spellIndex == -1 && request.rawButtonId == client.ancientButton[index]) {
                            client.autocast_spellIndex = index
                        }
                    }
                    CombatStyleService.refreshWeaponStyleUi(client)
                    true
                }
            )
            add(
                buttonBinding(
                    interfaceId = MagicComponents.ANCIENT_INTERFACE_ID,
                    componentId = 3,
                    componentKey = "magic.autocast.refresh",
                    rawButtonIds = MagicComponents.autocastRefreshButtons,
                ) { client, _ ->
                    CombatStyleService.refreshWeaponStyleUi(client)
                    true
                }
            )
            MagicComponents.teleports.forEach { teleport ->
                add(
                    buttonBinding(
                        interfaceId = MagicComponents.ANCIENT_INTERFACE_ID,
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
                    }
                )
            }
        }
}
