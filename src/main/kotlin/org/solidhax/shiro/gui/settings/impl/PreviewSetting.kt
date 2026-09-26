package org.solidhax.shiro.gui.settings.impl

import org.solidhax.shiro.gui.EntityPreview
import org.solidhax.shiro.gui.PreviewCamera
import org.solidhax.shiro.gui.settings.Setting

class PreviewSetting(name: String, vararg previews: EntityPreview) : Setting<EntityPreview>(name) {

    val previews = previews.toList().also { require(it.isNotEmpty()) { "PreviewSetting needs at least one preview" } }

    init {
        val camera = PreviewCamera()
        this.previews.forEach { it.camera = camera }
    }

    override val default: EntityPreview = this.previews.first()

    override var value: EntityPreview = default

    val cycles: Boolean get() = previews.size > 1

    fun cycle(step: Int) {
        value = previews[(previews.indexOf(value) + step).mod(previews.size)]
    }
}
