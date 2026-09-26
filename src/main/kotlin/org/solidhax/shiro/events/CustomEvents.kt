package org.solidhax.shiro.events

import net.fabricmc.fabric.api.client.rendering.v1.level.AbstractLevelRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.Packet
import net.minecraft.world.entity.Entity
import org.solidhax.shiro.events.core.CancellableEvent
import org.solidhax.shiro.events.core.Event
import org.solidhax.shiro.utils.render.RenderConsumer
import org.solidhax.shiro.utils.skyblock.TabListUtils
import org.solidhax.shiro.utils.skyblock.TabWidget

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

class HudRenderEvent(val graphics: GuiGraphicsExtractor, val partialTick: Float) : Event

class EntityGlowEvent(val entity: Entity) : Event {
    var color: Int? = null

    companion object {
        @JvmStatic
        fun colorOf(entity: Entity): Int? = EntityGlowEvent(entity).apply { postAndCatch() }.color
    }
}

class TabListChangeEvent(
    val old: List<List<String>>,
    val new: List<List<String>>,
    val newComponents: List<List<Component>>,
) : Event

class TabListHeaderFooterChangeEvent(
    val oldHeader: Component,
    val oldFooter: Component,
    val newHeader: Component,
    val newFooter: Component,
) : Event {
    val newHeaderSections: List<List<String>> by lazy { TabListUtils.sectionsOf(newHeader) }
    val newFooterSections: List<List<String>> by lazy { TabListUtils.sectionsOf(newFooter) }
    val oldHeaderSections: List<List<String>> by lazy { TabListUtils.sectionsOf(oldHeader) }
    val oldFooterSections: List<List<String>> by lazy { TabListUtils.sectionsOf(oldFooter) }
}

class TabWidgetChangeEvent(
    val widget: TabWidget,
    val oldTitle: String?,
    val oldLines: List<String>,
) : Event {
    val title: String? get() = widget.title
    val lines: List<String> get() = widget.lines
    val values: Map<String, String> get() = widget.values

    val isAdded: Boolean get() = oldTitle == null && widget.isActive
    val isRemoved: Boolean get() = !widget.isActive

    operator fun get(key: String): String? = widget[key]

    fun group(name: String): String? = widget.group(name)
}
