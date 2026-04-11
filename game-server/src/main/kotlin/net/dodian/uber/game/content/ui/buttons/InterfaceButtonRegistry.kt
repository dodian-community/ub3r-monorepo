package net.dodian.uber.game.content.ui.buttons

import net.dodian.uber.game.api.plugin.ContentModuleIndex
import net.dodian.uber.game.model.entity.player.Client
import java.util.concurrent.atomic.AtomicBoolean

object InterfaceButtonRegistry {
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
            definitions += ContentModuleIndex.interfaceButtons
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
        val semanticOwners = HashMap<String, String>()
        val routeOwners = HashMap<Long, String>()

        for (binding in allBindings) {
            val owner = binding.handler::class.java.name
            val semanticKey = "${binding.interfaceId}:${binding.componentId}:${binding.opIndex ?: -1}:${binding.componentKey}"
            val existingSemanticOwner = semanticOwners.putIfAbsent(semanticKey, owner)
            check(existingSemanticOwner == null) {
                "Duplicate interface button semantic binding key=$semanticKey existing=$existingSemanticOwner new=$owner"
            }
            for (rawButtonId in binding.rawButtonIds) {
                val op = binding.opIndex ?: -1
                val routeKey = (rawButtonId.toLong() shl 32) or (op.toLong() and 0xffffffffL)
                val existingRouteOwner = routeOwners.putIfAbsent(routeKey, owner)
                check(existingRouteOwner == null) {
                    "Duplicate interface button route rawButtonId=$rawButtonId op=$op existing=$existingRouteOwner new=$owner"
                }
                val list = rebuilt[rawButtonId] ?: mutableListOf<InterfaceButtonBinding>().also { rebuilt[rawButtonId] = it }
                list += binding
            }
        }
        byRawButtonId = Array(rebuilt.size) { index -> rebuilt[index]?.toList() }
    }

}
