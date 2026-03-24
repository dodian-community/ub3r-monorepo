package net.dodian.uber.game.ui.buttons

import net.dodian.uber.game.content.interfaces.combat.CombatInterfaceButtons
import net.dodian.uber.game.content.interfaces.crafting.CraftingInterfaceButtons
import net.dodian.uber.game.content.interfaces.dialogue.DialogueInterfaceButtons
import net.dodian.uber.game.content.interfaces.fletching.FletchingInterfaceButtons
import net.dodian.uber.game.content.interfaces.bank.BankInterfaceButtons
import net.dodian.uber.game.content.interfaces.magic.MagicInterfaceButtons
import net.dodian.uber.game.content.interfaces.prayer.PrayerInterfaceButtons
import net.dodian.uber.game.content.interfaces.settings.SettingsInterfaceButtons
import net.dodian.uber.game.content.interfaces.skillguide.SkillGuideInterfaceButtons
import net.dodian.uber.game.content.interfaces.smithing.SmithingInterfaceButtons
import net.dodian.uber.game.content.interfaces.travel.TravelInterfaceButtons
import net.dodian.uber.game.content.interfaces.ui.UiInterfaceButtons
import net.dodian.uber.game.model.entity.player.Client
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

object InterfaceButtonRegistry {
    private val logger = LoggerFactory.getLogger(InterfaceButtonRegistry::class.java)
    private val bootstrapped = AtomicBoolean(false)
    private val definitions = mutableListOf<InterfaceButtonContent>()

    @Volatile
    private var byRawButtonId: Array<List<InterfaceButtonBinding>?> = emptyArray()

    @JvmStatic
    fun bootstrap() {
        if (bootstrapped.get()) {
            return
        }
        synchronized(this) {
            if (bootstrapped.get()) {
                return
            }
            definitions += builtinContents()
            rebuildLookupLocked()
            bootstrapped.set(true)
        }
    }

    fun register(content: InterfaceButtonContent) {
        synchronized(this) {
            definitions += content
            if (bootstrapped.get()) {
                rebuildLookupLocked()
            }
        }
    }

    fun resolve(client: Client, rawButtonId: Int, opIndex: Int): InterfaceButtonBinding? {
        bootstrap()
        val table = byRawButtonId
        if (rawButtonId < 0 || rawButtonId >= table.size) {
            return null
        }
        val bindings = table[rawButtonId] ?: return null
        val opMatches = bindings.filter { it.opIndex == null || it.opIndex == opIndex }
        return opMatches.firstOrNull { it.requiredInterfaceId == -1 || it.requiredInterfaceId == client.activeInterfaceId }
            ?: opMatches.firstOrNull()
    }

    fun tryHandle(client: Client, rawButtonId: Int, opIndex: Int): Boolean {
        val binding = resolve(client, rawButtonId, opIndex) ?: return false
        val request =
            ButtonClickRequest(
                client = client,
                rawButtonId = rawButtonId,
                opIndex = opIndex,
                activeInterfaceId = client.activeInterfaceId,
                interfaceId = binding.interfaceId,
                componentId = binding.componentId,
                componentKey = binding.componentKey,
            )
        return binding.handler.onClick(request)
    }

    private fun rebuildLookupLocked() {
        val allBindings = definitions.flatMap { it.bindings }
        val maxButtonId = allBindings.asSequence().flatMap { it.rawButtonIds.asSequence() }.maxOrNull() ?: -1
        val rebuilt = arrayOfNulls<MutableList<InterfaceButtonBinding>>(maxButtonId + 1)
        val semanticKeys = HashSet<String>()

        for (binding in allBindings) {
            val semanticKey = "${binding.interfaceId}:${binding.componentId}:${binding.opIndex ?: -1}:${binding.componentKey}"
            if (!semanticKeys.add(semanticKey)) {
                logger.error("Duplicate interface button semantic binding {}", semanticKey)
            }
            for (rawButtonId in binding.rawButtonIds) {
                val list = rebuilt[rawButtonId] ?: mutableListOf<InterfaceButtonBinding>().also { rebuilt[rawButtonId] = it }
                list += binding
            }
        }
        byRawButtonId = Array(rebuilt.size) { index -> rebuilt[index]?.toList() }
    }

    private fun builtinContents(): List<InterfaceButtonContent> =
        listOf(
            SkillGuideInterfaceButtons,
            DialogueInterfaceButtons,
            CombatInterfaceButtons,
            CraftingInterfaceButtons,
            FletchingInterfaceButtons,
            SmithingInterfaceButtons,
            BankInterfaceButtons,
            PrayerInterfaceButtons,
            MagicInterfaceButtons,
            SettingsInterfaceButtons,
            TravelInterfaceButtons,
            UiInterfaceButtons,
        )
}
