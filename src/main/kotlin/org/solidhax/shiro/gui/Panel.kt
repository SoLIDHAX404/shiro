package org.solidhax.shiro.gui

import foo.starred.cascade.graphics.extensions.blur.blur
import foo.starred.cascade.graphics.extensions.rectangle.hollow.hollowRectangle
import foo.starred.cascade.graphics.extensions.rectangle.rounded.roundedRectangle
import foo.starred.cascade.graphics.extensions.rectangle.solid.rectangle
import foo.starred.cascade.graphics.extensions.shadow.dropShadow
import foo.starred.cascade.graphics.font.CascadeFonts
import foo.starred.cascade.graphics.geometry.CascadeGeometricOffset
import foo.starred.cascade.graphics.geometry.CascadeGeometricRadius
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import org.solidhax.shiro.features.Module
import org.solidhax.shiro.gui.ClickGUI.theme

class Panel(private val title: String) {

    private val sidebar = Sidebar()
    private val profilePage = ProfilePage()
    private val searchBar = SearchBar()
    private val moduleList = ModuleList(::openSettings)
    private val settingsPage = SettingsPage(::closeSettings)

    private var openModule: Module? = null

    fun draw(graphics: GuiGraphicsExtractor, x: Float, y: Float, mouseX: Float, mouseY: Float) {
        graphics.dropShadow(x, y, WIDTH, HEIGHT, CascadeGeometricOffset(0f, 2f), 10f, 0f, theme.shadow, ALL_CORNERS)
        graphics.blur(x, y, WIDTH, HEIGHT, theme.panelTint, ALL_CORNERS, 30f)

        drawHeader(graphics, x, y)
        sidebar.draw(graphics, x, y + HEADER_HEIGHT + 1f, mouseX, mouseY)

        val contentX = x + Sidebar.WIDTH + 1f
        val contentY = y + HEADER_HEIGHT + 1f
        val module = openModule
        when {
            sidebar.profileOpen -> profilePage.draw(graphics, contentX, contentY, CONTENT_WIDTH, CONTENT_HEIGHT, mouseX, mouseY)
            module != null -> settingsPage.draw(graphics, module, contentX, contentY, CONTENT_WIDTH, CONTENT_HEIGHT, mouseX, mouseY)
            else -> moduleList.draw(graphics, sidebar.selected, contentX, contentY, CONTENT_WIDTH, CONTENT_HEIGHT, mouseX, mouseY)
        }

        graphics.hollowRectangle(x, y, WIDTH, HEIGHT, 1f, theme.border, ALL_CORNERS)
    }

    fun mouseClicked(mouseX: Float, mouseY: Float, button: Int, doubleClick: Boolean): Boolean {
        if (sidebar.mouseClicked(mouseX, mouseY, button)) {
            unfocus()
            closeSettings()
            return true
        }
        return when {
            sidebar.profileOpen -> profilePage.mouseClicked(mouseX, mouseY, button, doubleClick)
            openModule != null -> settingsPage.mouseClicked(mouseX, mouseY, button)
            else -> moduleList.mouseClicked(mouseX, mouseY, button)
        }
    }

    fun mouseDragged(mouseX: Float, button: Int, deltaX: Float, deltaY: Float): Boolean = when {
        sidebar.profileOpen -> profilePage.mouseDragged(mouseX, button, deltaX, deltaY)
        openModule != null -> settingsPage.mouseDragged(mouseX, deltaY)
        else -> moduleList.mouseDragged(deltaY)
    }

    fun mouseReleased(button: Int) {
        profilePage.mouseReleased(button)
        settingsPage.mouseReleased(button)
        moduleList.mouseReleased()
    }

    fun mouseScrolled(mouseX: Float, mouseY: Float, amount: Float): Boolean {
        return when {
            sidebar.profileOpen -> profilePage.mouseScrolled(mouseX, mouseY, amount)
            openModule != null -> settingsPage.mouseScrolled(mouseX, mouseY, amount)
            else -> moduleList.mouseScrolled(mouseX, mouseY, amount)
        }
    }

    private fun openSettings(module: Module) {
        openModule = module
    }

    fun charTyped(event: CharacterEvent): Boolean = when {
        sidebar.profileOpen -> profilePage.charTyped(event)
        openModule != null -> settingsPage.charTyped(event)
        else -> false
    }

    fun keyPressed(event: KeyEvent): Boolean = when {
        sidebar.profileOpen -> profilePage.keyPressed(event)
        openModule != null -> settingsPage.keyPressed(event)
        else -> false
    }

    fun unfocus() {
        settingsPage.unfocus()
        profilePage.unfocus()
    }

    private fun closeSettings() {
        openModule = null
        settingsPage.unfocus()
    }

    private fun drawHeader(graphics: GuiGraphicsExtractor, x: Float, y: Float) {
        graphics.roundedRectangle(x, y, WIDTH, HEADER_HEIGHT, theme.headerTint, TOP_CORNERS)
        graphics.rectangle(x, y + HEADER_HEIGHT, WIDTH, 1f, theme.divider)

        font.extract(
            graphics, title,
            x + Sidebar.TEXT_X,
            y + (HEADER_HEIGHT - TITLE_SIZE) / 2f,
            theme.text,
            shadow = false,
            size = TITLE_SIZE
        )

        searchBar.draw(graphics, x + WIDTH - SEARCH_MARGIN - SearchBar.WIDTH, y + (HEADER_HEIGHT - SearchBar.HEIGHT) / 2f)
    }

    companion object {
        const val WIDTH = 465f
        const val HEADER_HEIGHT = 30f
        const val BODY_HEIGHT = 300f
        const val HEIGHT = HEADER_HEIGHT + BODY_HEIGHT
        const val CONTENT_WIDTH = WIDTH - Sidebar.WIDTH - 1f
        const val CONTENT_HEIGHT = Sidebar.HEIGHT

        private const val RADIUS = 4f
        private const val TITLE_SIZE = 10f
        private const val SEARCH_MARGIN = 8f

        private val ALL_CORNERS = CascadeGeometricRadius(RADIUS)
        private val TOP_CORNERS = CascadeGeometricRadius(RADIUS, RADIUS, 0f, 0f)

        private val font get() = CascadeFonts.sans
    }
}
