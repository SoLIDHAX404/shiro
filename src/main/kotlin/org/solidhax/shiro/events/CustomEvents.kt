package org.solidhax.shiro.events

import net.fabricmc.fabric.api.client.rendering.v1.level.AbstractLevelRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.network.protocol.Packet
import org.solidhax.shiro.events.core.CancellableEvent
import org.solidhax.shiro.events.core.Event
import org.solidhax.shiro.utils.render.RenderConsumer

interface TickEvent : Event {
    class End(val level: ClientLevel) : TickEvent
    object Server : TickEvent
}

interface LevelEvent : Event {
    object Load : LevelEvent
    object Unload : LevelEvent
}

abstract class RenderEvent(open val context: AbstractLevelRenderContext) : Event {
    class Extract(override val context: LevelRenderContext, val consumer: RenderConsumer) : RenderEvent(context)
    class Last(override val context: LevelRenderContext) : RenderEvent(context)
}

abstract class PacketEvent(val packet: Packet<*>) : CancellableEvent() {
    class Receive(packet: Packet<*>) : PacketEvent(packet)
    class Send(packet: Packet<*>) : PacketEvent(packet)
}

object LocationChangeEvent : Event
