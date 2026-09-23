package org.solidhax.shiro.gui

import foo.starred.cascade.graphics.extensions.rectangle.hollow.hollowRectangle
import foo.starred.cascade.graphics.extensions.rectangle.rounded.roundedRectangle
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.resources.Identifier
import org.solidhax.shiro.features.Category
import org.solidhax.shiro.features.Module
import org.solidhax.shiro.features.ModuleManager
import org.solidhax.shiro.gui.ClickGUI.theme
import org.solidhax.shiro.utils.ui.Radius
import org.solidhax.shiro.utils.ui.icon
import org.solidhax.shiro.utils.ui.isAreaHovered

class SearchBar(private val onSearch: () -> Unit) {

    var query = ""
        private set

    private val input = TextInput({ query }, ::update)

    private var x = 0f
    private var y = 0f

    val searching: Boolean get() = query.isNotBlank()

    val bestMatch: Module?
        get() = if (searching) ModuleManager.modules.values.filter { score(it) > 0 }.maxByOrNull(::score) else null

    fun draw(graphics: GuiGraphicsExtractor, x: Float, y: Float) {
        this.x = x
        this.y = y
        graphics.roundedRectangle(x, y, WIDTH, HEIGHT, theme.card, Radius.LARGE)
        graphics.hollowRectangle(x, y, WIDTH, HEIGHT, 1f, theme.divider, Radius.LARGE)
        graphics.icon(ICON, x + INSET, y + (HEIGHT - ICON_SIZE) / 2f, ICON_SIZE, theme.textMuted)
        input.draw(graphics, x + TEXT_X, y, WIDTH - TEXT_X - INSET, HEIGHT, PLACEHOLDER)
    }

    fun score(module: Module): Int {
        val search = query.trim().lowercase()
        val name = module.name.lowercase()
        return when {
            search.isEmpty() -> 1
            name == search -> 5
            name.startsWith(search) -> 4
            name.split(' ').any { it.startsWith(search) } -> 3
            search in name -> 2
            search in module.description.lowercase() -> 1
            else -> 0
        }
    }

    fun matches(category: Category): Boolean =
        !searching || ModuleManager.modulesByCategory[category].orEmpty().any { score(it) > 0 }

    fun mouseClicked(mouseX: Float, mouseY: Float): Boolean {
        if (!isAreaHovered(mouseX, mouseY, x, y, WIDTH, HEIGHT)) return false
        input.click(mouseX)
        return true
    }

    fun mouseDragged(mouseX: Float): Boolean = input.drag(mouseX)

    fun charTyped(event: CharacterEvent): Boolean = input.charTyped(event)

    fun keyPressed(event: KeyEvent): Boolean = input.keyPressed(event)

    fun unfocus() = input.unfocus()

    private fun update(text: String) {
        if (text.length > MAX_LENGTH) return
        query = text
        onSearch()
    }

    companion object {
        const val WIDTH = 140f
        const val HEIGHT = 18f

        private const val PLACEHOLDER = "Search..."
        private const val MAX_LENGTH = 32
        private const val INSET = 6f
        private const val ICON_SIZE = 9f
        private const val TEXT_X = INSET + ICON_SIZE + 5f

        private val ICON = Identifier.fromNamespaceAndPath("shiro", "search.svg")
    }
}
