package net.dodian.uber.game.architecture

import net.dodian.uber.game.engine.metrics.InfraParityReport
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InfraParityReportContractTest {
    @Test
    fun `infra parity report snapshot includes events tasks and state domains`() {
        val snapshot = InfraParityReport.snapshot()

        assertTrue(snapshot.sections.containsKey("events"))
        assertTrue(snapshot.sections.containsKey("tasks"))
        assertTrue(snapshot.sections.containsKey("state"))
        assertTrue(snapshot.sections["events"]!!.containsKey("contractCatalogSize"))
        assertTrue(snapshot.sections["tasks"]!!.containsKey("queuePressureOwners"))
        assertTrue(snapshot.sections["state"]!!.containsKey("ownershipDomains"))
    }
}
