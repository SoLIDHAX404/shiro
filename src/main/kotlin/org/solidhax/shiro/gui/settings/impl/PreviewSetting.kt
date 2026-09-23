package org.solidhax.shiro.gui.settings.impl

import org.solidhax.shiro.gui.EntityPreview
import org.solidhax.shiro.gui.settings.Setting

class PreviewSetting(name: String, override val default: EntityPreview) : Setting<EntityPreview>(name) {

    override var value: EntityPreview = default
}
