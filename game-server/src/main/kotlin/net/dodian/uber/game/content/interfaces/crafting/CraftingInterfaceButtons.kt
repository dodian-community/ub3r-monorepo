package net.dodian.uber.game.content.interfaces.crafting

import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.netty.listener.out.SendMessage
import net.dodian.uber.game.systems.api.content.ContentActions
import net.dodian.uber.game.systems.api.content.ContentProductionRequest
import net.dodian.uber.game.content.skills.crafting.TanningRequest
import net.dodian.uber.game.content.skills.crafting.CraftingPlugin
import net.dodian.uber.game.systems.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.systems.ui.buttons.buttonBinding

object CraftingInterfaceButtons : InterfaceButtonContent {
    override val bindings =
        buildList {
            add(
            buttonBinding(
                interfaceId = CraftingInterfaceComponents.LEATHER_INTERFACE_ID,
                componentId = CraftingInterfaceComponents.hideCraftGroup.componentId,
                componentKey = CraftingInterfaceComponents.hideCraftGroup.componentKey,
                rawButtonIds = CraftingInterfaceComponents.hideCraftGroup.rawButtonIds,
                requiredInterfaceId = CraftingInterfaceComponents.LEATHER_INTERFACE_ID,
            ) { client, request ->
                val amount = CraftingInterfaceComponents.hideCraftGroup.amountByButton[request.rawButtonId] ?: return@buttonBinding false
                val productGroup = CraftingInterfaceComponents.hideCraftGroup.rawButtonIds.indexOf(request.rawButtonId) / 4
                CraftingPlugin.startHide(client, productGroup, amount)
                true
            })
            add(
            buttonBinding(
                interfaceId = CraftingInterfaceComponents.LEATHER_INTERFACE_ID,
                componentId = CraftingInterfaceComponents.standardCraftGroup.componentId,
                componentKey = CraftingInterfaceComponents.standardCraftGroup.componentKey,
                rawButtonIds = CraftingInterfaceComponents.standardCraftGroup.rawButtonIds,
                requiredInterfaceId = CraftingInterfaceComponents.LEATHER_INTERFACE_ID,
            ) { client, request ->
                val amount = CraftingInterfaceComponents.standardCraftGroup.amountByButton[request.rawButtonId] ?: return@buttonBinding false
                val productIndex = CraftingInterfaceComponents.standardCraftGroup.rawButtonIds.indexOf(request.rawButtonId) / 3
                CraftingPlugin.startLeather(client, productIndex, amount)
                true
            })
            add(
                buttonBinding(
                    interfaceId = CraftingInterfaceComponents.PRODUCTION_AMOUNT_INTERFACE_ID,
                    componentId = 12,
                    componentKey = "crafting.production.amount",
                    rawButtonIds = CraftingInterfaceComponents.productionAmountButtons.keys.toIntArray(),
                ) { client, request ->
                    if (client.getPendingProductionSelection() == null) {
                        return@buttonBinding true
                    }
                    val amount = CraftingInterfaceComponents.productionAmountButtons[request.rawButtonId] ?: return@buttonBinding false
                    ContentActions.startPendingProduction(client, amount)
                    true
                }
            )
            CraftingInterfaceComponents.glassProducts.forEach { product ->
                add(
                    buttonBinding(
                        interfaceId = CraftingInterfaceComponents.GLASS_INTERFACE_ID,
                        componentId = product.componentId,
                        componentKey = product.componentKey,
                        rawButtonIds = product.rawButtonIds,
                    ) { client, request ->
                        client.send(RemoveInterfaces())
                        if (product.minimumLevel > 1 && client.getLevel(Skill.CRAFTING) < product.minimumLevel) {
                            product.levelMessage?.let { client.sendMessage(it) }
                            return@buttonBinding true
                        }
                        val amount = product.amountByButton[request.rawButtonId] ?: return@buttonBinding false
                        ContentActions.startProduction(
                            client,
                            ContentProductionRequest(
                                skillId = Skill.CRAFTING.id,
                                productId = product.productId,
                                amountPerCycle = 1,
                                primaryItemId = 1775,
                                secondaryItemId = -1,
                                experiencePerUnit = product.experiencePerUnit,
                                animationId = 884,
                                tickDelay = 3,
                            ),
                            amount,
                        )
                        true
                    }
                )
            }
            CraftingInterfaceComponents.tanningOptions.forEach { tanning ->
                add(
                    buttonBinding(
                        interfaceId = CraftingInterfaceComponents.TANNING_INTERFACE_ID,
                        componentId = tanning.componentId,
                        componentKey = tanning.componentKey,
                        rawButtonIds = tanning.rawButtonIds,
                    ) { client, request ->
                        val amount = tanning.amountByButton[request.rawButtonId] ?: return@buttonBinding false
                        CraftingPlugin.startTanning(client, TanningRequest(tanning.hideType, amount))
                        true
                    }
                )
            }
        }
}
