// Adapted from SkyblockAPI (https://github.com/SkyblockAPI/SkyblockAPI), Copyright (c) 2025, SkyblockAPI contributors, MIT License.
package org.solidhax.shiro.utils.skyblock

import net.minecraft.client.multiplayer.PlayerInfo
import net.minecraft.network.chat.Component
import net.minecraft.world.scores.PlayerTeam
import org.solidhax.shiro.Shiro.mc
import org.solidhax.shiro.events.LevelEvent
import org.solidhax.shiro.events.TabListChangeEvent
import org.solidhax.shiro.events.TabListHeaderFooterChangeEvent
import org.solidhax.shiro.events.TabWidgetChangeEvent
import org.solidhax.shiro.events.TickEvent
import org.solidhax.shiro.events.core.on
import org.solidhax.shiro.mixin.PlayerTabOverlayAccessor
import org.solidhax.shiro.utils.noControlCodes
import java.util.EnumMap

object TabListUtils {

    private const val COLUMN_SIZE = 20
    private const val REFRESH_INTERVAL = 20

    internal class WidgetState(
        val title: String,
        val lines: List<String>,
        val values: Map<String, String>,
        val components: List<Component>,
        val match: MatchResult?,
    )

    private val infoColumnRegex = Regex("(?:Info|Account Info|Player Stats|Dungeon Stats)$")

    private val overlay: PlayerTabOverlayAccessor
        get() = mc.gui.hud.tabList as PlayerTabOverlayAccessor

    internal val widgets = EnumMap<TabWidget, WidgetState>(TabWidget::class.java)

    private var ticks = 0

    val entries: List<PlayerInfo>
        get() = if (mc.player == null) emptyList() else overlay.shiroPlayerInfos()

    var columns: List<List<Component>> = emptyList()
        private set

    var strippedColumns: List<List<String>> = emptyList()
        private set

    val lines: List<String>
        get() = strippedColumns.flatten()

    var header: Component = Component.empty()
        private set

    var footer: Component = Component.empty()
        private set

    val headerSections: List<List<String>>
        get() = sectionsOf(header)

    val footerSections: List<List<String>>
        get() = sectionsOf(footer)

    val activeWidgets: Set<TabWidget>
        get() = widgets.keys.toSet()

    init {
        on<TickEvent.End> {
            updateHeaderFooter()
            if (LocationUtils.isInSkyblock && ++ticks % REFRESH_INTERVAL == 0) refresh()
        }

        on<LevelEvent.Load> {
            reset()
        }
    }

    fun refresh() {
        val newColumns = entries.map { it.tabDisplayName }.chunked(COLUMN_SIZE)
        val newStripped = newColumns.map { column -> column.map { it.stripped } }
        if (newStripped == strippedColumns) return

        val old = strippedColumns
        columns = newColumns
        strippedColumns = newStripped
        TabListChangeEvent(old, newStripped, newColumns).postAndCatch()
        updateWidgets(newColumns)
    }

    fun sectionsOf(component: Component): List<List<String>> {
        val sections = mutableListOf<List<String>>()
        var current = mutableListOf<String>()
        for (line in component.stripped.split('\n')) {
            if (line.isNotBlank()) current += line
            else if (current.isNotEmpty()) {
                sections += current
                current = mutableListOf()
            }
        }
        if (current.isNotEmpty()) sections += current
        return sections
    }

    private fun updateHeaderFooter() {
        if (mc.player == null) return
        val newHeader = overlay.shiroHeader() ?: Component.empty()
        val newFooter = overlay.shiroFooter() ?: Component.empty()
        if (newHeader == header && newFooter == footer) return

        val oldHeader = header
        val oldFooter = footer
        header = newHeader
        footer = newFooter
        TabListHeaderFooterChangeEvent(oldHeader, oldFooter, newHeader, newFooter).postAndCatch()
    }

    private fun updateWidgets(columns: List<List<Component>>) {
        val lines = columns
            .filter { it.isNotEmpty() && infoColumnRegex.containsMatchIn(it.first().stripped) }
            .flatMap { it.drop(1) }

        val found = EnumMap<TabWidget, Pair<List<Component>, MatchResult?>>(TabWidget::class.java)
        val section = mutableListOf<Component>()
        var current: TabWidget? = null
        var currentMatch: MatchResult? = null

        fun flush() {
            current?.let { if (section.isNotEmpty()) found[it] = section.toList() to currentMatch }
            section.clear()
        }

        for (line in lines) {
            val stripped = line.stripped
            if (stripped.isBlank()) continue

            if (!stripped.startsWith(" ")) {
                val header = TabWidget.entries.firstNotNullOfOrNull { widget -> widget.regex.matchAt(stripped, 0)?.let { widget to it } }
                if (header != null) {
                    flush()
                    current = header.first
                    currentMatch = header.second
                } else if (!continuesWidget(current, stripped)) {
                    flush()
                    current = null
                    currentMatch = null
                }
            }
            if (current != null) section += line
        }
        flush()

        for ((widget, data) in found) {
            val (components, match) = data
            val stripped = components.map { it.stripped }
            val title = stripped.first()
            val lines = stripped.drop(1).map { it.trim() }
            val old = widgets[widget]
            widgets[widget] = WidgetState(title, lines, parseValues(lines), components, match)
            if (old?.title != title || old.lines != lines) TabWidgetChangeEvent(widget, old?.title, old?.lines.orEmpty()).postAndCatch()
        }

        for (widget in widgets.keys - found.keys) {
            val removed = widgets.remove(widget) ?: continue
            TabWidgetChangeEvent(widget, removed.title, removed.lines).postAndCatch()
        }
    }

    private fun parseValues(lines: List<String>): Map<String, String> {
        val values = LinkedHashMap<String, String>()
        for (line in lines) {
            val split = line.indexOf(": ")
            if (split <= 0) continue
            values.putIfAbsent(line.substring(0, split).trim(), line.substring(split + 2).trim())
        }
        return values
    }

    private fun continuesWidget(widget: TabWidget?, line: String): Boolean = when (widget) {
        TabWidget.JACOBS_CONTEST -> line == "ACTIVE"
        TabWidget.MINING_EVENT -> line.startsWith("Ends in: ")
        else -> false
    }

    private fun reset() {
        ticks = 0
        val removedWidgets = EnumMap(widgets)
        widgets.clear()
        val oldColumns = strippedColumns
        columns = emptyList()
        strippedColumns = emptyList()

        for ((widget, state) in removedWidgets) {
            TabWidgetChangeEvent(widget, state.title, state.lines).postAndCatch()
        }
        if (oldColumns.isNotEmpty()) TabListChangeEvent(oldColumns, emptyList(), emptyList()).postAndCatch()
    }

    private val PlayerInfo.tabDisplayName: Component
        get() = tabListDisplayName ?: PlayerTeam.formatNameForTeam(team, Component.literal(profile.name))

    private val Component.stripped: String
        get() = string.noControlCodes
}
