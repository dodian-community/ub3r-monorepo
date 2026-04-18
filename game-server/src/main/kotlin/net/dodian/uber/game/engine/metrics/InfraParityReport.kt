package net.dodian.uber.game.engine.metrics

import net.dodian.uber.game.engine.state.StateOwnershipPolicy
import net.dodian.uber.game.events.EventContractCatalog

data class InfraParityReportSnapshot(
    val sections: Map<String, Map<String, Any>>,
)

object InfraParityReport {
    @JvmStatic
    fun snapshot(): InfraParityReportSnapshot {
        val taskSnapshot = TaskLifecycleTelemetry.snapshot()
        return InfraParityReportSnapshot(
            sections = mapOf(
                "events" to mapOf(
                    "contractCatalogSize" to EventContractCatalog.contractsByEventSimpleName.size,
                ),
                "tasks" to mapOf(
                    "queuePressureOwners" to taskSnapshot.queuePressureByOwner.keys.sorted(),
                ),
                "state" to mapOf(
                    "ownershipDomains" to StateOwnershipPolicy.ownershipByDomain.keys.map { it.name },
                ),
            ),
        )
    }
}
