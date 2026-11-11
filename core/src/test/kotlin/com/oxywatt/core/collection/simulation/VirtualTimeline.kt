package com.oxywatt.core.collection.simulation

import java.util.PriorityQueue

/** Harness-owned acquisition time. Timers at a deadline precede input at that deadline. */
class VirtualTimeline {
    private data class Timer(val deadline: Long, val order: Long, val action: () -> Unit)

    private val timers = PriorityQueue(compareBy<Timer> { it.deadline }.thenBy { it.order })
    private var order = 0L
    var now: Long = 0
        private set

    fun at(deadline: Long, action: () -> Unit): AutoCloseable {
        require(deadline >= now)
        val timer = Timer(deadline, order++, action)
        timers.add(timer)
        return AutoCloseable { timers.remove(timer) }
    }

    fun advanceTo(deadline: Long) {
        require(deadline >= now)
        while (timers.peek()?.deadline?.let { it <= deadline } == true) {
            val timer = timers.remove()
            now = timer.deadline
            timer.action()
        }
        now = deadline
    }
}
