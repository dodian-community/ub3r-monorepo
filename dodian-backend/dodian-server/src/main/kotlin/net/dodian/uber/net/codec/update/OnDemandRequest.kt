package net.dodian.uber.net.codec.update

import org.apollo.cache.FileDescriptor

class OnDemandRequest(
    val descriptor: FileDescriptor,
    val priority: Priority
) : Comparable<OnDemandRequest> {

    override fun compareTo(other: OnDemandRequest): Int {
        return priority.compareTo(other.priority)
    }
}

enum class Priority(val value: Int) {
    HIGH(0),
    MEDIUM(1),
    LOW(2);

    companion object {
        fun byValue(value: Int) = entries.firstOrNull { it.value == value }
    }
}