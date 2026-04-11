package net.dodian.uber.game.content.ui

import net.dodian.uber.game.content.skills.crafting.Crafting
import net.dodian.uber.game.content.skills.crafting.TanningRequest
import net.dodian.uber.game.model.player.skills.Skill
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.api.content.ContentActions
import net.dodian.uber.game.api.content.ContentProductionRequest
import net.dodian.uber.game.content.ui.buttons.InterfaceButtonContent
import net.dodian.uber.game.content.ui.buttons.buttonBinding

object CraftingInterface : InterfaceButtonContent {
    private const val LEATHER_INTERFACE_ID = 8880
    private const val PRODUCTION_AMOUNT_INTERFACE_ID = 8888
    private const val GLASS_INTERFACE_ID = 11462
    private const val TANNING_INTERFACE_ID = 14670

    private data class ButtonGroup(
        val componentId: Int,
        val componentKey: String,
        val rawButtonIds: IntArray,
        val amountByButton: Map<Int, Int>,
    )

    private val hideCraftGroup =
        ButtonGroup(
            componentId = 0,
            componentKey = "crafting.leather.hide_set",
            rawButtonIds = intArrayOf(34185, 34184, 34183, 34182, 34189, 34188, 34187, 34186, 34193, 34192, 34191, 34190),
            amountByButton = mapOf(34185 to 1, 34184 to 5, 34183 to 10, 34182 to 27, 34189 to 1, 34188 to 5, 34187 to 10, 34186 to 27, 34193 to 1, 34192 to 5, 34191 to 10, 34190 to 27),
        )

    private val standardCraftGroup =
        ButtonGroup(
            componentId = 1,
            componentKey = "crafting.leather.standard_set",
            rawButtonIds = intArrayOf(33187, 33186, 33185, 33190, 33189, 33188, 33193, 33192, 33191, 33196, 33195, 33194, 33199, 33198, 33197, 33202, 33201, 33200, 33205, 33204, 33203),
            amountByButton = mapOf(33187 to 1, 33186 to 5, 33185 to 10, 33190 to 1, 33189 to 5, 33188 to 10, 33193 to 1, 33192 to 5, 33191 to 10, 33196 to 1, 33195 to 5, 33194 to 10, 33199 to 1, 33198 to 5, 33197 to 10, 33202 to 1, 33201 to 5, 33200 to 10, 33205 to 1, 33204 to 5, 33203 to 10),
        )

    private val productionAmountButtons = mapOf(10239 to 1, 10238 to 5, 6212 to 10, 6211 to 28)

    private data class GlassProductBinding(
        val componentId: Int,
        val componentKey: String,
        val rawButtonIds: IntArray,
        val productId: Int,
        val minimumLevel: Int,
        val levelMessage: String?,
        val experiencePerUnit: Int,
        val amountByButton: Map<Int, Int>,
    )

    private val glassProducts =
        listOf(
            GlassProductBinding(
                componentId = 2,
                componentKey = "crafting.glass.vial",
                rawButtonIds = intArrayOf(44210, 44209, 44208, 44207),
                productId = 229,
                minimumLevel = 1,
                levelMessage = null,
                experiencePerUnit = 80,
                amountByButton = mapOf(44210 to 27, 44209 to 10, 44208 to 5, 44207 to 1),
            ),
            GlassProductBinding(
                componentId = 3,
                componentKey = "crafting.glass.cup",
                rawButtonIds = intArrayOf(48108, 48107, 48106, 48105),
                productId = 1980,
                minimumLevel = 18,
                levelMessage = "You need level 18 crafting to craft a empty cup.",
                experiencePerUnit = 120,
                amountByButton = mapOf(48108 to 27, 48107 to 10, 48106 to 5, 48105 to 1),
            ),
            GlassProductBinding(
                componentId = 4,
                componentKey = "crafting.glass.fishbowl",
                rawButtonIds = intArrayOf(48112, 48111, 48110, 48109),
                productId = 6667,
                minimumLevel = 32,
                levelMessage = "You need level 32 crafting to craft a fishbowl.",
                experiencePerUnit = 160,
                amountByButton = mapOf(48112 to 27, 48111 to 10, 48110 to 5, 48109 to 1),
            ),
            GlassProductBinding(
                componentId = 5,
                componentKey = "crafting.glass.orb",
                rawButtonIds = intArrayOf(48116, 48115, 48114, 48113),
                productId = 567,
                minimumLevel = 48,
                levelMessage = "You need level 48 crafting to craft a unpowered orb.",
                experiencePerUnit = 240,
                amountByButton = mapOf(48116 to 27, 48115 to 10, 48114 to 5, 48113 to 1),
            ),
        )

    private data class TanningBinding(
        val componentId: Int,
        val componentKey: String,
        val rawButtonIds: IntArray,
        val hideType: Int,
        val amountByButton: Map<Int, Int>,
    )

    private val tanningOptions =
        listOf(
            TanningBinding(6, "crafting.tanning.soft_leather", intArrayOf(57225, 57217, 57201, 57209), 0, mapOf(57225 to 1, 57217 to 5, 57201 to 27, 57209 to 27)),
            TanningBinding(7, "crafting.tanning.hard_leather", intArrayOf(57229, 57221, 57205, 57213), 1, mapOf(57229 to 1, 57221 to 5, 57205 to 27, 57213 to 27)),
            TanningBinding(8, "crafting.tanning.snakeskin", intArrayOf(57227, 57219, 57211, 57203), 2, mapOf(57227 to 1, 57219 to 5, 57211 to 27, 57203 to 27)),
            TanningBinding(9, "crafting.tanning.green_dhide", intArrayOf(57228, 57220, 57212, 57204), 3, mapOf(57228 to 1, 57220 to 5, 57212 to 27, 57204 to 27)),
            TanningBinding(10, "crafting.tanning.blue_dhide", intArrayOf(57231, 57223, 57215, 57207), 4, mapOf(57231 to 1, 57223 to 5, 57215 to 27, 57207 to 27)),
            TanningBinding(11, "crafting.tanning.red_dhide", intArrayOf(57232, 57224, 57216, 57208), 5, mapOf(57232 to 1, 57224 to 5, 57216 to 27, 57208 to 27)),
        )

    override val bindings =
        buildList {
            add(
                buttonBinding(
                    interfaceId = LEATHER_INTERFACE_ID,
                    componentId = hideCraftGroup.componentId,
                    componentKey = hideCraftGroup.componentKey,
                    rawButtonIds = hideCraftGroup.rawButtonIds,
                    requiredInterfaceId = LEATHER_INTERFACE_ID,
                ) { client, request ->
                    val amount = hideCraftGroup.amountByButton[request.rawButtonId] ?: return@buttonBinding false
                    val productGroup = hideCraftGroup.rawButtonIds.indexOf(request.rawButtonId) / 4
                    Crafting.startHide(client, productGroup, amount)
                    true
                },
            )
            add(
                buttonBinding(
                    interfaceId = LEATHER_INTERFACE_ID,
                    componentId = standardCraftGroup.componentId,
                    componentKey = standardCraftGroup.componentKey,
                    rawButtonIds = standardCraftGroup.rawButtonIds,
                    requiredInterfaceId = LEATHER_INTERFACE_ID,
                ) { client, request ->
                    val amount = standardCraftGroup.amountByButton[request.rawButtonId] ?: return@buttonBinding false
                    val productIndex = standardCraftGroup.rawButtonIds.indexOf(request.rawButtonId) / 3
                    Crafting.startLeather(client, productIndex, amount)
                    true
                },
            )
            add(
                buttonBinding(
                    interfaceId = PRODUCTION_AMOUNT_INTERFACE_ID,
                    componentId = 12,
                    componentKey = "crafting.production.amount",
                    rawButtonIds = productionAmountButtons.keys.toIntArray(),
                ) { client, request ->
                    if (client.getPendingProductionSelection() == null) {
                        return@buttonBinding true
                    }
                    val amount = productionAmountButtons[request.rawButtonId] ?: return@buttonBinding false
                    ContentActions.startPendingProduction(client, amount)
                    true
                },
            )
            glassProducts.forEach { product ->
                add(
                    buttonBinding(
                        interfaceId = GLASS_INTERFACE_ID,
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
                    },
                )
            }
            tanningOptions.forEach { tanning ->
                add(
                    buttonBinding(
                        interfaceId = TANNING_INTERFACE_ID,
                        componentId = tanning.componentId,
                        componentKey = tanning.componentKey,
                        rawButtonIds = tanning.rawButtonIds,
                    ) { client, request ->
                        val amount = tanning.amountByButton[request.rawButtonId] ?: return@buttonBinding false
                        Crafting.startTanning(client, TanningRequest(tanning.hideType, amount))
                        true
                    },
                )
            }
        }
}
