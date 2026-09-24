// Adapted from Odin (https://github.com/odtheking/Odin), Copyright (c) 2025, odtheking, BSD 3-Clause License.
package org.solidhax.shiro.utils.render

import com.mojang.renderpearl.api.pipeline.RenderPipeline
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import org.solidhax.shiro.Shiro.MOD_ID
import java.util.Optional

object CustomRenderPipelines {
    val LINES_ESP: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withDepthStencilState(Optional.empty())
            .withLocation(Identifier.fromNamespaceAndPath(MOD_ID, "pipeline/lines_esp"))
            .build()
    )

    val LINES_TRANSLUCENT_ESP: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withDepthStencilState(Optional.empty())
            .withLocation(Identifier.fromNamespaceAndPath(MOD_ID, "pipeline/lines_translucent_esp"))
            .build()
    )

    val QUADS_ESP: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withDepthStencilState(Optional.empty())
            .withLocation(Identifier.fromNamespaceAndPath(MOD_ID, "pipeline/quads_esp"))
            .build()
    )
}
