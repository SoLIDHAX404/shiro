package org.solidhax.shiro.gui.settings.impl

import org.solidhax.shiro.gui.settings.Setting

class ActionSetting(
    name: String,
    desc: String,
    override val default: () -> Unit = {}
) : Setting<() -> Unit>(name, desc) {

    override var value: () -> Unit = default

    var action: () -> Unit by this::value
}
