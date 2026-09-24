package org.solidhax.shiro.events

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import org.solidhax.shiro.utils.render.RenderBatchManager

object EventDispatcher {

    init {
        ClientPlayConnectionEvents.JOIN.register { _, _, _ -> LevelEvent.Load.postAndCatch() }
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ -> LevelEvent.Unload.postAndCatch() }

        ClientTickEvents.END_LEVEL_TICK.register { level -> TickEvent.End(level).postAndCatch() }

        LevelRenderEvents.COLLECT_SUBMITS.register { context ->
            RenderEvent.Extract(context, RenderBatchManager.renderConsumer).postAndCatch()
            RenderEvent.Last(context).postAndCatch()
        }
    }
}
