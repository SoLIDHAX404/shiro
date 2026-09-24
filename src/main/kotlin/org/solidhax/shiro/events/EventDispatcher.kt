package org.solidhax.shiro.events

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents

object EventDispatcher {

    init {
        ClientPlayConnectionEvents.JOIN.register { _, _, _ -> LevelEvent.Load.postAndCatch() }
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ -> LevelEvent.Unload.postAndCatch() }

        ClientTickEvents.END_LEVEL_TICK.register { level -> TickEvent.End(level).postAndCatch() }

        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register { context ->
            RenderEvent.Extract(context).postAndCatch()
            RenderEvent.Last(context).postAndCatch()
        }
    }
}
