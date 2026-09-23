package org.solidhax.shiro.gui

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState
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
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.AbstractSkullBlock
import org.joml.Quaternionf
import org.joml.Vector2f
import org.joml.Vector3f
import org.solidhax.shiro.Shiro.mc
import org.solidhax.shiro.utils.ui.animation.AnimationManager
import org.solidhax.shiro.utils.ui.isAreaHovered
import java.util.EnumMap
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.pow

class EntityPreview(var entity: () -> Entity? = { mc.player }) {

    val equipment = EnumMap<EquipmentSlot, ItemStack>(EquipmentSlot::class.java)

    var armorStandPose: ArmorStandPose? = null

    var heightScale = 1f

    var autoSpin = true

    var modelTop = 0f
        private set

    var blockSize = 0f
        private set

    private var yaw = 0f
    private var pitch = 0f
    private var zoom = 1f
    private var dragging = false
    private var lastInteraction = 0L

    private var x = 0f
    private var y = 0f
    private var width = 0f
    private var height = 0f

    fun draw(graphics: GuiGraphicsExtractor, x: Float, y: Float, width: Float, height: Float) {
        this.x = x
        this.y = y
        this.width = width
        this.height = height

        if (autoSpin && !dragging && Util.getMillis() - lastInteraction > IDLE_DELAY_MS) yaw += SPIN_SPEED * AnimationManager.deltaSeconds

        val entity = entity() ?: return
        val state = mc.entityRenderDispatcher.getRenderer(entity).createRenderState(entity, 1f)
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
        if (state is ArmorStandRenderState) {
            state.yRot = FACING
            state.wiggle = 0f
            armorStandPose?.applyTo(state)
        }

        val modelHeight = state.boundingBoxHeight * heightScale
        val camera = Quaternionf().rotateX(pitch * DEG_TO_RAD)
        val rotation = Quaternionf().rotateZ(PI.toFloat()).mul(camera).rotateY(-yaw * DEG_TO_RAD)
        val translation = rotation.transform(Vector3f(0f, modelHeight / 2f, 0f)).negate()
        blockSize = height * FIT * zoom / max(state.boundingBoxHeight, state.boundingBoxWidth)
        modelTop = y + (height - modelHeight * blockSize) / 2f

        val pose = graphics.pose()
        val min = pose.transformPosition(Vector2f(x + 1f, y + 1f))
        val max = pose.transformPosition(Vector2f(x + width - 1f, y + height - 1f))
        graphics.entity(state, blockSize * pose.m00(), translation, rotation, camera, min.x.toInt(), min.y.toInt(), max.x.toInt(), max.y.toInt())
    }

    fun mouseClicked(mouseX: Float, mouseY: Float, doubleClick: Boolean): Boolean {
        if (!isHovered(mouseX, mouseY)) return false
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
        if (!dragging) return false
        yaw -= deltaX * ROTATE_SPEED
        pitch = (pitch - deltaY * ROTATE_SPEED).coerceIn(-MAX_PITCH, MAX_PITCH)
        lastInteraction = Util.getMillis()
        return true
    }

    fun mouseReleased() {
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
        private const val FIT = 0.684f
        private const val ROTATE_SPEED = 1.2f
        private const val SPIN_SPEED = 20f
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

class DummyEntity<T : Entity>(private val type: EntityType<T>, private val setup: (T) -> Unit = {}) : () -> T? {

    private var entity: T? = null

    override fun invoke(): T? {
        val level = mc.level ?: return null
        entity?.takeIf { it.level() === level }?.let { return it }
        return type.create(level, EntitySpawnReason.LOAD)?.also {
            setup(it)
            entity = it
        }
    }
}
