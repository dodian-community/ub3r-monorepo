package net.dodian.uber.net.codec.update

import net.dodian.uber.cache.FileDescriptor


class OnDemandRequest(
    private val descriptor: FileDescriptor,
    private val priority: Priority
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