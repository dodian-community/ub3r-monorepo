package net.dodian.uber.game.engine.metrics

object EventDispatchTelemetry {
    @JvmStatic
    fun recordMissingSubscriber(eventName: String) {
        OperationalTelemetry.incrementCounter("event.dispatch.missing_subscriber.total")
        OperationalTelemetry.incrementCounter("event.dispatch.missing_subscriber.$eventName")
    }

    @JvmStatic
    fun recordDispatchException(eventName: String) {
        OperationalTelemetry.incrementCounter("event.dispatch.exception.total")
        OperationalTelemetry.incrementCounter("event.dispatch.exception.$eventName")
    }

    @JvmStatic
    fun recordDuplicateListenerRegistration(eventName: String) {
        OperationalTelemetry.incrementCounter("event.registration.duplicate.listener.total")
        OperationalTelemetry.incrementCounter("event.registration.duplicate.listener.$eventName")
    }

    @JvmStatic
    fun recordDuplicateReturnableRegistration(eventName: String) {
        OperationalTelemetry.incrementCounter("event.registration.duplicate.returnable.total")
        OperationalTelemetry.incrementCounter("event.registration.duplicate.returnable.$eventName")
    }

    @JvmStatic
    fun recordBootstrapInvocation(alreadyBootstrapped: Boolean, bootstrapCount: Int) {
        OperationalTelemetry.incrementCounter("event.bootstrap.invoked")
        OperationalTelemetry.incrementCounter("event.bootstrap.count.$bootstrapCount")
        if (alreadyBootstrapped) {
            OperationalTelemetry.incrementCounter("event.bootstrap.duplicate_attempt")
        }
    }
}
