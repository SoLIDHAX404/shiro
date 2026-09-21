package org.solidhax.shiro.commands

import com.github.stivais.commodore.Commodore
import org.solidhax.shiro.Shiro.mc
import org.solidhax.shiro.gui.ClickGUI

val mainCommand = Commodore("shiro") {
    runs {
        mc.schedule { mc.setScreenAndShow(ClickGUI) }
    }
}