// Adapted from Odin (https://github.com/odtheking/Odin), Copyright (c) 2025, odtheking, BSD 3-Clause License.
package org.solidhax.shiro.events.core

import net.minecraft.network.protocol.Packet
import net.minecraft.util.profiling.Profiler
import net.minecraft.util.profiling.ProfilerFiller
import org.solidhax.shiro.events.PacketEvent
import org.solidhax.shiro.events.TabWidgetChangeEvent
import org.solidhax.shiro.utils.skyblock.TabWidget
import java.util.EnumSet

object EventBus {

    private val listenerArrays = HashMap<Class<out Event>, Array<ListenerEntry<out Event>>>()
    private val subscriberClasses = HashMap<Any, Class<*>>()
    private val invokers = HashMap<Class<out Event>, Invoker>()
    private val profilerNameCache = HashMap<Class<out Event>, String>()

    fun subscribe(subscriber: Any) {
        if (subscriberClasses.putIfAbsent(subscriber, subscriber.javaClass) == null) rebuildAffectedCaches(subscriber.javaClass)
    }

    fun unsubscribe(subscriber: Any) {
        subscriberClasses.remove(subscriber)?.let { rebuildAffectedCaches(it) }
    }

    @JvmStatic
    fun <T : Event> post(event: T) {
        val eventClass = event.javaClass
        val invoker = invokers[eventClass] ?: return

        val profiler = Profiler.get()
        profiler.push(profilerNameCache.getOrPut(eventClass) { "Shiro: ${eventClass.simpleName}" })
        try {
            invoker.invoke(event, profiler)
        } finally {
            profiler.pop()
        }
    }

    fun <T : Event> registerListener(
        subscriberClass: Class<*>,
        eventClass: Class<T>,
        priority: Int,
        ignoreCancelled: Boolean,
        handler: (T) -> Unit
    ) {
        val name = subscriberClass.simpleName.ifEmpty { subscriberClass.name }
        val entry = ListenerEntry(subscriberClass, EventListener(priority, ignoreCancelled, name, handler))

        val listeners = ((listenerArrays[eventClass] ?: emptyArray()) + entry).sortedByDescending { it.listener.priority }.toTypedArray()
        listenerArrays[eventClass] = listeners
        rebuildInvoker(eventClass, listeners)
    }

    private fun rebuildAffectedCaches(changedClass: Class<*>) {
        for ((eventClass, listeners) in listenerArrays) {
            if (listeners.any { it.subscriber == changedClass }) rebuildInvoker(eventClass, listeners)
        }
    }

    private fun rebuildInvoker(eventClass: Class<out Event>, allListeners: Array<ListenerEntry<*>>) {
        val activeClasses = subscriberClasses.values

        @Suppress("UNCHECKED_CAST")
        val activeListeners = allListeners
            .filter { it.subscriber in activeClasses }
            .map { it.listener as EventListener<Event> }
            .toTypedArray()

        invokers[eventClass] = if (activeListeners.isEmpty()) EmptyInvoker else ListenerInvoker(activeListeners)
    }

    data class ListenerEntry<T : Event>(
        val subscriber: Class<*>,
        val listener: EventListener<T>
    )

    class EventListener<T : Event>(
        val priority: Int,
        private val ignoreCancelled: Boolean,
        val subscriberName: String,
        val handler: (T) -> Unit
    ) {
        fun invoke(event: T) {
            if (!ignoreCancelled || event !is CancellableEvent || !event.isCancelled) handler(event)
        }
    }

    interface Invoker {
        fun invoke(event: Event, profiler: ProfilerFiller)
    }

    private object EmptyInvoker : Invoker {
        override fun invoke(event: Event, profiler: ProfilerFiller) {}
    }

    private class ListenerInvoker(private val listeners: Array<EventListener<Event>>) : Invoker {
        override fun invoke(event: Event, profiler: ProfilerFiller) {
            for (listener in listeners) {
                profiler.push(listener.subscriberName)
                try {
                    listener.invoke(event)
                } finally {
                    profiler.pop()
                }
            }
        }
    }
}

inline fun <reified T : Event> Any.on(
    priority: Int = 0,
    ignoreCancelled: Boolean = false,
    noinline handler: T.() -> Unit
) = EventBus.registerListener(this.javaClass, T::class.java, priority, ignoreCancelled) {
    it.handler()
}

inline fun <reified P : Packet<*>> Any.onReceive(
    priority: Int = 0,
    ignoreCancelled: Boolean = false,
    noinline handler: P.(PacketEvent.Receive) -> Unit
) = EventBus.registerListener(this.javaClass, PacketEvent.Receive::class.java, priority, ignoreCancelled) {
    (it.packet as? P)?.handler(it)
}

inline fun <reified P : Packet<*>> Any.onSend(
    priority: Int = 0,
    ignoreCancelled: Boolean = false,
    noinline handler: P.(PacketEvent.Send) -> Unit
) = EventBus.registerListener(this.javaClass, PacketEvent.Send::class.java, priority, ignoreCancelled) {
    (it.packet as? P)?.handler(it)
}

fun Any.onTabWidget(
    vararg widgets: TabWidget,
    priority: Int = 0,
    handler: TabWidgetChangeEvent.() -> Unit
) {
    val filter = EnumSet.noneOf(TabWidget::class.java).apply { addAll(widgets) }
    EventBus.registerListener(this.javaClass, TabWidgetChangeEvent::class.java, priority, false) {
        if (it.widget in filter) it.handler()
    }
}
