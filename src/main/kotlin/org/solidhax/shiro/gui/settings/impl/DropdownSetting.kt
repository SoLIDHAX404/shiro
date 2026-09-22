package org.solidhax.shiro.gui.settings.impl

import org.solidhax.shiro.gui.settings.Setting

class DropdownSetting(
    name: String,
    override val default: Boolean = false,
    desc: String = ""
) : Setting<Boolean>(name, desc) {

    override var value: Boolean = default
    var enabled: Boolean by this::value
}
