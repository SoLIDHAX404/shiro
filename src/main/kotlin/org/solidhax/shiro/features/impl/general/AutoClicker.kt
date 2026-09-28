package org.solidhax.shiro.features.impl.general

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.ChatFormatting
import net.minecraft.client.KeyMapping
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import org.solidhax.shiro.Shiro.mc
import org.solidhax.shiro.events.TickEvent
import org.solidhax.shiro.events.core.on
import org.solidhax.shiro.features.Module
import org.solidhax.shiro.features.ModuleManager
import org.solidhax.shiro.gui.settings.impl.BooleanSetting
import org.solidhax.shiro.gui.settings.impl.ListSetting
import org.solidhax.shiro.gui.settings.impl.RangeSetting
import org.solidhax.shiro.utils.modMessage
import java.util.Random

object AutoClicker : Module(
    name = "Auto Clicker",
    description = "Clicks for you while you hold attack or use, at a randomised CPS."
) {
    private val whitelistEnabled by BooleanSetting("Whitelist", true, desc = "Only clicks while holding a whitelisted item. Hold one and use /shiro autoclicker add or remove with left or right.")

    private val left = Clicker(
        "left",
        { mc.options.keyAttack },
        +RangeSetting("Left CPS", 9.0..12.0, 1, 20, 1, desc = "Range the left clicks per second are picked from."),
        +ListSetting("Left Whitelist Items", mutableSetOf<String>()).apply { hide() },
    )

    private val right = Clicker(
        "right",
        { mc.options.keyUse },
        +RangeSetting("Right CPS", 9.0..12.0, 1, 20, 1, desc = "Range the right clicks per second are picked from."),
        +ListSetting("Right Whitelist Items", mutableSetOf<String>()).apply { hide() },
    )

    private val clickers = mapOf(left.name to left, right.name to right)

    init {
        on<TickEvent.Start> {
            left.tick(respectBlocks = true)
            right.tick(respectBlocks = false)
        }
    }

    override fun onDisable() {
        super.onDisable()
        clickers.values.forEach(Clicker::release)
    }

    fun addHeldItem(button: String) {
        val clicker = clickers[button] ?: return
        val id = heldItemId() ?: return
        if (!clicker.whitelist.value.add(id)) return modMessage(message("$id is already whitelisted for $button click.", ChatFormatting.YELLOW))
        ModuleManager.saveConfigurations()
        modMessage(message("Added $id to the $button click whitelist.", ChatFormatting.GREEN))
    }

    fun removeHeldItem(button: String) {
        val clicker = clickers[button] ?: return
        val id = heldItemId() ?: return
        if (!clicker.whitelist.value.remove(id)) return modMessage(message("$id is not whitelisted for $button click.", ChatFormatting.YELLOW))
        ModuleManager.saveConfigurations()
        modMessage(message("Removed $id from the $button click whitelist.", ChatFormatting.GREEN))
    }

    private fun heldItemId(): String? {
        val id = itemId(mc.player?.mainHandItem ?: ItemStack.EMPTY)
        if (id == null) modMessage(message("Hold an item first.", ChatFormatting.RED))
        return id
    }

    private fun message(text: String, color: ChatFormatting): Component = Component.literal(text).withStyle(color)

    private fun itemId(stack: ItemStack): String? {
        if (stack.isEmpty) return null
        val uuid = stack.get(DataComponents.CUSTOM_DATA)?.copyTag()?.getStringOr("uuid", "").orEmpty()
        return uuid.ifEmpty { BuiltInRegistries.ITEM.getKey(stack.item).toString() }
    }

    private fun isPhysicallyDown(key: InputConstants.Key): Boolean = when {
        key == InputConstants.UNKNOWN -> false
        key.type == InputConstants.Type.MOUSE -> when (key.value) {
            InputConstants.MOUSE_BUTTON_LEFT -> mc.mouseHandler.isLeftPressed
            InputConstants.MOUSE_BUTTON_MIDDLE -> mc.mouseHandler.isMiddlePressed
            InputConstants.MOUSE_BUTTON_RIGHT -> mc.mouseHandler.isRightPressed
            else -> false
        }
        else -> InputConstants.isKeyDown(key.value)
    }

    private class Clicker(
        val name: String,
        private val mapping: () -> KeyMapping,
        private val cps: RangeSetting,
        val whitelist: ListSetting<String, MutableSet<String>>,
    ) {
        private val random = Random()
        private var drift = 0.5
        private var nextClickAt = 0L

        private val key: InputConstants.Key get() = InputConstants.getKey(mapping().saveString())

        fun tick(respectBlocks: Boolean) {
            val player = mc.player
            val level = mc.level
            val key = key

            if (player == null || level == null || mc.gui.screen() != null || !isPhysicallyDown(key)) {
                nextClickAt = 0L
                return
            }

            if (player.isUsingItem || (respectBlocks && mc.gameMode?.isDestroying == true)) return

            if (whitelistEnabled && itemId(player.mainHandItem) !in whitelist.value) {
                KeyMapping.set(key, true)
                return
            }

            val hit = mc.hitResult
            if (respectBlocks && hit is BlockHitResult && hit.type == HitResult.Type.BLOCK && !level.getBlockState(hit.blockPos).isAir) {
                KeyMapping.set(key, true)
                nextClickAt = 0L
                return
            }

            val now = System.nanoTime()
            if (nextClickAt == 0L) nextClickAt = now + nextInterval()

            var clicks = 0
            while (now >= nextClickAt && clicks < MAX_CLICKS_PER_TICK) {
                KeyMapping.set(key, true)
                KeyMapping.click(key)
                KeyMapping.set(key, false)
                nextClickAt += nextInterval()
                clicks++
            }
            if (now >= nextClickAt) nextClickAt = now + nextInterval()
        }

        fun release() {
            nextClickAt = 0L
            val key = key
            KeyMapping.set(key, isPhysicallyDown(key))
        }

        private fun nextInterval(): Long {
            val range = cps.value
            drift = (drift + random.nextGaussian() * DRIFT_STEP).coerceIn(0.0, 1.0)
            val target = range.start + (range.endInclusive - range.start) * drift
            val jitter = (1.0 + random.nextGaussian() * JITTER).coerceIn(MIN_JITTER, MAX_JITTER)
            val rate = (target / jitter).coerceIn(range.start, range.endInclusive)
            return (NANOS_PER_SECOND / rate).toLong()
        }
    }

    private const val MAX_CLICKS_PER_TICK = 3
    private const val DRIFT_STEP = 0.1
    private const val JITTER = 0.15
    private const val MIN_JITTER = 0.6
    private const val MAX_JITTER = 1.5
    private const val NANOS_PER_SECOND = 1_000_000_000.0
}
