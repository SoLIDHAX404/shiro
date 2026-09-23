package org.solidhax.shiro.gui

import foo.starred.cascade.graphics.extensions.blur.blur
import foo.starred.cascade.graphics.extensions.rectangle.hollow.hollowRectangle
import foo.starred.cascade.graphics.extensions.rectangle.rounded.roundedRectangle
import foo.starred.cascade.graphics.extensions.rectangle.solid.rectangle
import foo.starred.cascade.graphics.extensions.shadow.dropShadow
import foo.starred.cascade.graphics.geometry.CascadeGeometricOffset
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import org.solidhax.shiro.gui.ClickGUI.theme
import org.solidhax.shiro.utils.ui.Radius
import org.solidhax.shiro.utils.ui.text

class Panel(private val title: String) {

    private val sidebar = Sidebar()
    private val profilePage = ProfilePage()
    private val settingsPage = SettingsPage()
    private val moduleList = ModuleList({ sidebar.selected }) { settingsPage.module = it }
    private val pages = listOf(profilePage, settingsPage, moduleList)

    private val page: Page
        get() = when {
            sidebar.profileOpen -> profilePage
            settingsPage.module != null -> settingsPage
            else -> moduleList
        }

    fun draw(graphics: GuiGraphicsExtractor, x: Float, y: Float, mouseX: Float, mouseY: Float) {
        graphics.dropShadow(x, y, WIDTH, HEIGHT, CascadeGeometricOffset(0f, 2f), 10f, 0f, theme.shadow, Radius.LARGE)
        graphics.blur(x, y, WIDTH, HEIGHT, theme.panelTint, Radius.LARGE, 30f)

        graphics.roundedRectangle(x, y, WIDTH, HEADER_HEIGHT, theme.headerTint, Radius.TOP)
        graphics.rectangle(x, y + HEADER_HEIGHT, WIDTH, 1f, theme.divider)
        graphics.text(title, x + Sidebar.TEXT_X, y + (HEADER_HEIGHT - TITLE_SIZE) / 2f, theme.text, TITLE_SIZE)
        SearchBar.draw(graphics, x + WIDTH - SEARCH_MARGIN - SearchBar.WIDTH, y + (HEADER_HEIGHT - SearchBar.HEIGHT) / 2f)

        val bodyY = y + HEADER_HEIGHT + 1f
        sidebar.draw(graphics, x, bodyY, mouseX, mouseY)
        page.draw(graphics, x + Sidebar.WIDTH + 1f, bodyY, WIDTH - Sidebar.WIDTH - 1f, Sidebar.HEIGHT, mouseX, mouseY)

        graphics.hollowRectangle(x, y, WIDTH, HEIGHT, 1f, theme.border, Radius.LARGE)
    }

    fun mouseClicked(mouseX: Float, mouseY: Float, button: Int, doubleClick: Boolean): Boolean {
        if (!sidebar.mouseClicked(mouseX, mouseY, button)) return page.mouseClicked(mouseX, mouseY, button, doubleClick)
        unfocus()
        settingsPage.module = null
        return true
    }

    fun mouseDragged(mouseX: Float, mouseY: Float, button: Int, deltaX: Float, deltaY: Float): Boolean =
        page.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)

    fun mouseReleased(button: Int) = pages.forEach { it.mouseReleased(button) }

    fun mouseScrolled(mouseX: Float, mouseY: Float, amount: Float): Boolean = page.mouseScrolled(mouseX, mouseY, amount)

    fun charTyped(event: CharacterEvent): Boolean = page.charTyped(event)

    fun keyPressed(event: KeyEvent): Boolean = page.keyPressed(event)

    fun unfocus() = pages.forEach(Page::unfocus)

    companion object {
        const val WIDTH = 465f
        const val HEADER_HEIGHT = 30f
        const val BODY_HEIGHT = 300f
        const val HEIGHT = HEADER_HEIGHT + BODY_HEIGHT

        private const val TITLE_SIZE = 10f
        private const val SEARCH_MARGIN = 8f
    }
}
