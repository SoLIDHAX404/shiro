package org.solidhax.shiro.gui

import foo.starred.cascade.graphics.extensions.rectangle.hollow.hollowRectangle
import foo.starred.cascade.graphics.extensions.scissor.scissor
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState
import net.minecraft.client.renderer.entity.state.AvatarRenderState
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.client.renderer.entity.state.HumanoidRenderState
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.client.renderer.item.ItemStackRenderState
import net.minecraft.core.Rotations
import net.minecraft.core.component.DataComponents
import net.minecraft.util.LightCoordsUtil
import net.minecraft.util.Util
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.player.PlayerSkin
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.AbstractSkullBlock
import net.minecraft.world.phys.AABB
import org.joml.Quaternionf
import org.joml.Vector2f
import org.joml.Vector3f
import org.solidhax.shiro.Shiro.mc
import org.solidhax.shiro.gui.settings.impl.LabelPositionSetting
import org.solidhax.shiro.utils.render.ModelBounds
import org.solidhax.shiro.utils.ui.Bounds
import org.solidhax.shiro.utils.ui.LabelPosition
import org.solidhax.shiro.utils.ui.NameTagSegments
import org.solidhax.shiro.utils.ui.Radius
import org.solidhax.shiro.utils.ui.isAreaHovered
import org.solidhax.shiro.utils.ui.nameTag
import org.solidhax.shiro.utils.ui.nameTagBounds
import org.solidhax.shiro.utils.ui.nearestNameTagPosition
import java.util.EnumMap
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.pow

class EntityPreview(
    private val entity: () -> Entity? = { mc.player },
    val label: PreviewLabel? = null,
    private val setup: EntityPreview.() -> Unit = {}
) {

    private val initialized by lazy { setup() }

    val equipment = EnumMap<EquipmentSlot, ItemStack>(EquipmentSlot::class.java)

    var armorStandPose: ArmorStandPose? = null

    var skin: PlayerSkin? = null

    var sitting = false

    var autoSpin = true

    var x = 0f
        private set

    var y = 0f
        private set

    var width = 0f
        private set

    var height = 0f
        private set

    /**
     * The on-screen rectangle covering the entity's bounding box, as drawn this frame.
     */
    var bounds = Bounds(0f, 0f, 0f, 0f)
        private set

    private var labelBounds: Bounds? = null
    private var draggingLabel = false
    private val labelMouse = Vector2f()
    private val labelGrab = Vector2f()

    private var yaw = 0f
    private var pitch = 0f
    private var zoom = 1f
    private var dragging = false
    private var lastInteraction = 0L
    private var lastFrame = 0L

    fun draw(graphics: GuiGraphicsExtractor, x: Float, y: Float, width: Float, height: Float) {
        this.x = x
        this.y = y
        this.width = width
        this.height = height
        initialized

        val now = Util.getMillis()
        val deltaSeconds = ((now - lastFrame) / 1000f).coerceIn(0f, MAX_DELTA)
        lastFrame = now
        if (autoSpin && !dragging && now - lastInteraction > IDLE_DELAY_MS) yaw += SPIN_SPEED * deltaSeconds

        val entity = entity() ?: return
        val renderer = mc.entityRenderDispatcher.getRenderer(entity)
        val state = renderer.createRenderState(entity, 1f)
        state.shadowPieces.clear()
        state.outlineColor = 0
        state.nameTag = null
        state.scoreText = null
        state.lightCoords = LightCoordsUtil.FULL_BRIGHT

        if (state is LivingEntityRenderState) {
            state.bodyRot = FACING
            state.yRot = 0f
            state.xRot = 0f
            state.boundingBoxWidth /= state.scale
            state.boundingBoxHeight /= state.scale
            state.scale = 1f
            if (entity is LivingEntity) applyEquipment(state, entity)
        }
        if (state is AvatarRenderState) skin?.let { state.skin = it }
        if (state is ArmorStandRenderState) {
            state.yRot = FACING
            state.wiggle = 0f
            armorStandPose?.applyTo(state)
        }

        // framed around the posed model, so sitting or posed entities stay centered and fit the preview
        val box = ModelBounds.of(renderer, state) ?: hitbox(state)
        val center = box.center.toVector3f()
        val camera = Quaternionf().rotateX(pitch * DEG_TO_RAD)
        val rotation = Quaternionf().rotateZ(PI.toFloat()).mul(camera).rotateY(-yaw * DEG_TO_RAD)
        val translation = rotation.transform(Vector3f(center)).negate()
        val blockSize = height * FIT * zoom / max(box.ysize, max(box.xsize, box.zsize)).toFloat()
        bounds = projectBounds(box, center, rotation, blockSize)

        val pose = graphics.pose()
        val min = pose.transformPosition(Vector2f(x + 1f, y + 1f))
        val max = pose.transformPosition(Vector2f(x + width - 1f, y + height - 1f))
        graphics.entity(state, blockSize * pose.m00(), translation, rotation, camera, min.x.toInt(), min.y.toInt(), max.x.toInt(), max.y.toInt())
        drawLabel(graphics)
    }

    private fun hitbox(state: EntityRenderState): AABB {
        val halfWidth = state.boundingBoxWidth / 2.0
        return AABB(-halfWidth, 0.0, -halfWidth, halfWidth, state.boundingBoxHeight.toDouble(), halfWidth)
    }

    // the preview is orthographic and centered on the box, so each corner lands at the preview center plus its rotated offset
    private fun projectBounds(box: AABB, center: Vector3f, rotation: Quaternionf, blockSize: Float): Bounds {
        val centerX = x + width / 2f
        val centerY = y + height / 2f
        val corners = ArrayList<Vector2f>(8)
        for (cornerX in doubleArrayOf(box.minX, box.maxX)) for (cornerY in doubleArrayOf(box.minY, box.maxY)) for (cornerZ in doubleArrayOf(box.minZ, box.maxZ)) {
            val offset = rotation.transform(Vector3f(cornerX.toFloat(), cornerY.toFloat(), cornerZ.toFloat()).sub(center))
            corners += Vector2f(centerX + offset.x * blockSize, centerY + offset.y * blockSize)
        }
        return Bounds.of(corners)
    }

    private fun drawLabel(graphics: GuiGraphicsExtractor) {
        val label = label ?: return
        val segments = label.segments()
        if (segments.isEmpty()) {
            labelBounds = null
            return
        }
        val position = label.position?.value ?: LabelPosition.TOP
        val tag = nameTagBounds(bounds, position, segments)
        labelBounds = tag

        graphics.scissor(x, y, width, height) {
            if (draggingLabel) graphics.hollowRectangle(bounds.left, bounds.top, bounds.width, bounds.height, 1f, ClickGUI.theme.divider, Radius.SMALL)
            graphics.nameTag(tag, segments)
        }
    }

    fun mouseClicked(mouseX: Float, mouseY: Float, doubleClick: Boolean): Boolean {
        if (!isHovered(mouseX, mouseY)) return false
        val position = label?.position
        val tag = labelBounds
        if (position != null && tag != null && tag.contains(mouseX, mouseY)) {
            if (doubleClick) position.value = position.default
            draggingLabel = true
            labelMouse.set(mouseX, mouseY)
            labelGrab.set(tag.centerX - mouseX, tag.centerY - mouseY)
            return true
        }
        if (doubleClick) {
            yaw = 0f
            pitch = 0f
            zoom = 1f
        }
        dragging = true
        lastInteraction = Util.getMillis()
        return true
    }

    fun mouseDragged(deltaX: Float, deltaY: Float): Boolean {
        if (draggingLabel) {
            val label = label ?: return false
            labelMouse.add(deltaX, deltaY)
            label.position?.value = nearestNameTagPosition(bounds, label.segments(), labelMouse.x + labelGrab.x, labelMouse.y + labelGrab.y)
            return true
        }
        if (!dragging) return false
        yaw -= deltaX * ROTATE_SPEED
        pitch = (pitch - deltaY * ROTATE_SPEED).coerceIn(-MAX_PITCH, MAX_PITCH)
        lastInteraction = Util.getMillis()
        return true
    }

    fun mouseReleased() {
        draggingLabel = false
        if (!dragging) return
        dragging = false
        lastInteraction = Util.getMillis()
    }

    fun mouseScrolled(mouseX: Float, mouseY: Float, amount: Float): Boolean {
        if (!isHovered(mouseX, mouseY)) return false
        zoom = (zoom * ZOOM_STEP.pow(amount)).coerceIn(MIN_ZOOM, MAX_ZOOM)
        lastInteraction = Util.getMillis()
        return true
    }

    fun isHovered(mouseX: Float, mouseY: Float): Boolean = isAreaHovered(mouseX, mouseY, x, y, width, height)

    private fun applyEquipment(state: LivingEntityRenderState, entity: LivingEntity) {
        val head = item(EquipmentSlot.HEAD)
        val skull = ((head.item as? BlockItem)?.block as? AbstractSkullBlock)
        state.wornHeadType = skull?.type
        state.wornHeadProfile = skull?.let { head.get(DataComponents.PROFILE) }
        if (skull == null && !HumanoidArmorLayer.shouldRender(head, EquipmentSlot.HEAD)) resolveItem(state.headItem, head, ItemDisplayContext.HEAD, entity)
        else state.headItem.clear()

        if (state is HumanoidRenderState) {
            state.headEquipment = armor(EquipmentSlot.HEAD)
            state.chestEquipment = armor(EquipmentSlot.CHEST)
            state.legsEquipment = armor(EquipmentSlot.LEGS)
            state.feetEquipment = armor(EquipmentSlot.FEET)
            state.isUsingItem = false
            state.isPassenger = sitting
        }

        if (state is ArmedEntityRenderState) {
            val rightHanded = state.mainArm == HumanoidArm.RIGHT
            val right = item(if (rightHanded) EquipmentSlot.MAINHAND else EquipmentSlot.OFFHAND)
            val left = item(if (rightHanded) EquipmentSlot.OFFHAND else EquipmentSlot.MAINHAND)
            resolveItem(state.rightHandItemState, right, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, entity)
            resolveItem(state.leftHandItemState, left, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, entity)
            state.rightHandItemStack = right.copy()
            state.leftHandItemStack = left.copy()
            state.rightArmPose = armPose(right)
            state.leftArmPose = armPose(left)
        }
    }

    private fun item(slot: EquipmentSlot): ItemStack = equipment[slot] ?: ItemStack.EMPTY

    private fun armor(slot: EquipmentSlot): ItemStack = item(slot).takeIf { HumanoidArmorLayer.shouldRender(it, slot) }?.copy() ?: ItemStack.EMPTY

    private fun armPose(stack: ItemStack): HumanoidModel.ArmPose = if (stack.isEmpty) HumanoidModel.ArmPose.EMPTY else HumanoidModel.ArmPose.ITEM

    private fun resolveItem(target: ItemStackRenderState, stack: ItemStack, context: ItemDisplayContext, entity: LivingEntity) =
        mc.itemModelResolver.updateForLiving(target, stack, context, entity)

    companion object {
        private const val FACING = 180f
        private const val FIT = 0.55f
        private const val ROTATE_SPEED = 1.2f
        private const val SPIN_SPEED = 20f
        private const val MAX_DELTA = 0.1f
        private const val IDLE_DELAY_MS = 2000L
        private const val MAX_PITCH = 80f
        private const val ZOOM_STEP = 1.1f
        private const val MIN_ZOOM = 0.5f
        private const val MAX_ZOOM = 4f
        private const val DEG_TO_RAD = (PI / 180.0).toFloat()
    }
}

data class ArmorStandPose(
    val head: Rotations = ArmorStand.DEFAULT_HEAD_POSE,
    val body: Rotations = ArmorStand.DEFAULT_BODY_POSE,
    val leftArm: Rotations = ArmorStand.DEFAULT_LEFT_ARM_POSE,
    val rightArm: Rotations = ArmorStand.DEFAULT_RIGHT_ARM_POSE,
    val leftLeg: Rotations = ArmorStand.DEFAULT_LEFT_LEG_POSE,
    val rightLeg: Rotations = ArmorStand.DEFAULT_RIGHT_LEG_POSE,
    val small: Boolean = false,
    val showArms: Boolean = true,
    val showBasePlate: Boolean = true
) {
    fun applyTo(state: ArmorStandRenderState) {
        state.headPose = head
        state.bodyPose = body
        state.leftArmPose = leftArm
        state.rightArmPose = rightArm
        state.leftLegPose = leftLeg
        state.rightLegPose = rightLeg
        state.isSmall = small
        state.showArms = showArms
        state.showBasePlate = showBasePlate
    }
}

class DummyEntity<T : Entity>(private val create: (ClientLevel) -> T?) : () -> T? {

    constructor(type: EntityType<T>) : this({ type.create(it, EntitySpawnReason.LOAD) })

    private var entity: T? = null

    override fun invoke(): T? {
        val level = mc.level ?: return null
        entity?.takeIf { it.level() === level }?.let { return it }
        return create(level)?.also {
            it.id = nextId--
            entity = it
        }
    }

    private companion object {
        var nextId = -1
    }
}

/**
 * A name tag drawn around the preview's bounding box. With a [position] setting it can be dragged anywhere around the box
 * in the preview, which stores where it sits there; without one it stays on top.
 */
class PreviewLabel(val position: LabelPositionSetting? = null, val segments: () -> NameTagSegments)
