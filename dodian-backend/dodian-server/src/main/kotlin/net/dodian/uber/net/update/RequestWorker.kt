package net.dodian.uber.net.update

import io.netty.channel.Channel

abstract class RequestWorker<T, P>(
    private val dispatcher: UpdateDispatcher,
    private val provider: P
) : Runnable {
    var running = true

    protected abstract fun nextRequest(dispatcher: UpdateDispatcher): ChannelRequest<T>
    protected abstract fun service(provider: P, channel: Channel, request: T): Boolean

    override fun run() {
        while (true) {
            synchronized(this) {
                if (!running) return
            }

            val request = nextRequest(dispatcher)
            val channel = request.channel
            if (!service(provider, channel, request.request))
                channel.close()
        }
    }

    fun stop() {
        synchronized(this) {
            running = false
        }
    }
}