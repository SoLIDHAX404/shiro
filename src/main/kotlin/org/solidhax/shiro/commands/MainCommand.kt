package org.solidhax.shiro.commands

import com.github.stivais.commodore.Commodore
import org.solidhax.shiro.Shiro.mc
import org.solidhax.shiro.gui.ClickGUI
import org.solidhax.shiro.gui.HudEditor

val mainCommand = Commodore("shiro") {
    runs {
        mc.schedule { mc.gui.setScreen(ClickGUI) }
    }

    literal("hud").runs {
        mc.schedule { HudEditor.open() }
    }
}