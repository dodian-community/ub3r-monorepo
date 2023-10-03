package net.dodian.uber.net.update

import io.netty.channel.Channel
import io.netty.handler.codec.http.HttpRequest
import net.dodian.uber.net.codec.jaggrab.JagGrabRequest
import net.dodian.uber.net.codec.update.OnDemandRequest
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.PriorityBlockingQueue

private const val MAXIMUM_QUEUE_SIZE = 1024

class UpdateDispatcher {
    private val demand: BlockingQueue<ComparableChannelRequest<OnDemandRequest>> = PriorityBlockingQueue()
    private val http: BlockingQueue<ChannelRequest<HttpRequest>> = LinkedBlockingQueue()
    private val jaggrab: BlockingQueue<ChannelRequest<JagGrabRequest>> = LinkedBlockingQueue()

    fun dispatch(channel: Channel, request: HttpRequest) {
        if (http.size >= MAXIMUM_QUEUE_SIZE)
            channel.close()

        http.add(ChannelRequest(channel, request))
    }

    fun dispatch(channel: Channel, request: JagGrabRequest) {
        if (jaggrab.size >= MAXIMUM_QUEUE_SIZE)
            channel.close()

        jaggrab.add(ChannelRequest(channel, request))
    }

    fun dispatch(channel: Channel, request: OnDemandRequest) {
        if (demand.size >= MAXIMUM_QUEUE_SIZE)
            channel.close()

        demand.add(ComparableChannelRequest(channel, request))
    }

    val nextHttpRequest: ChannelRequest<HttpRequest> get() = http.take()
    val nextJagGrabRequest: ChannelRequest<JagGrabRequest> get() = jaggrab.take()
    val nextOnDemandRequest: ComparableChannelRequest<OnDemandRequest> get() = demand.take()
}