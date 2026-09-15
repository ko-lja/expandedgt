package lu.kolja.expandedgt.items

import com.gregtechceu.gtceu.api.blockentity.IPaintable
import com.gregtechceu.gtceu.api.gui.GuiTextures
import com.gregtechceu.gtceu.api.gui.widget.ColorBlockWidget
import com.gregtechceu.gtceu.api.item.component.IAddInformation
import com.gregtechceu.gtceu.api.item.component.IDurabilityBar
import com.gregtechceu.gtceu.api.item.component.IInteractionItem
import com.gregtechceu.gtceu.api.item.component.IItemUIFactory
import com.gregtechceu.gtceu.api.pipenet.IPipeNode
import com.gregtechceu.gtceu.common.data.GTSoundEntries
import com.gregtechceu.gtceu.config.ConfigHolder
import com.gregtechceu.gtceu.utils.BreadthFirstBlockSearch
import com.gregtechceu.gtceu.utils.GradientUtil
import com.lowdragmc.lowdraglib.gui.factory.HeldItemUIFactory
import com.lowdragmc.lowdraglib.gui.modular.ModularUI
import com.lowdragmc.lowdraglib.gui.widget.HsbColorWidget
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget
import it.unimi.dsi.fastutil.ints.IntIntPair
import lu.kolja.expandedgt.lang.ExpGuiText
import lu.kolja.expandedgt.lang.ExpPlayerMessages
import lu.kolja.expandedgt.lang.ExpTooltips
import lu.kolja.expandedgt.util.isNull
import net.minecraft.core.Direction
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraftforge.common.util.TriPredicate
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max
import kotlin.math.min

class RGBSprayBehavior(val paintItem: () -> ItemStack): IDurabilityBar, IInteractionItem, IItemUIFactory, IAddInformation {
    companion object {
        const val TOTAL_USES = 512
        const val DYE_REFILL = 256
        const val SELECTED_COLOR_TAG = "selectedColor"
        const val USES_LEFT_TAG = "usesLeft"
        const val DEFAULT_COLOR = 0xFFFFFF
    }

    @Suppress("UNCHECKED_CAST")
    private val gtPipePredicate: TriPredicate<IPipeNode<*, *>, IPipeNode<*, *>, Direction> = TriPredicate { parent, child, direction ->
        if (parent == null) return@TriPredicate true
        if (!paintablePredicate.test(parent, child, direction)) {
            return@TriPredicate false
        }
        parent.isConnected(direction) && child.isConnected(direction.opposite)
    }

    private val paintablePredicate: TriPredicate<IPaintable, IPaintable, Direction> = TriPredicate { parent, child, dir ->
        if (parent == null) return@TriPredicate true
        if (parent::class != child::class) {
            return@TriPredicate false
        }
        parent.paintingColor == child.paintingColor
    }

    override fun onItemUseFirst(stack: ItemStack, context: UseOnContext): InteractionResult {
        val player = context.player ?: return InteractionResult.PASS
        val first = context.level.getBlockEntity(context.clickedPos) ?: return InteractionResult.PASS

        val limit = if (player.isShiftKeyDown) ConfigHolder.INSTANCE.tools.sprayCanChainLength else 1
        val painted = paintCollected(first, limit, context)
        if (painted) {
            GTSoundEntries.SPRAY_CAN_TOOL.play(context.level, null, player.position(), 1f, 1f)
        }
        return InteractionResult.SUCCESS
    }

    fun paintCollected(first: BlockEntity, limit: Int, context: UseOnContext): Boolean {
        when (first) {
            is IPipeNode<*, *> -> {
                val collected = BreadthFirstBlockSearch.conditionalSearch(
                    IPipeNode::class.java,
                    first,
                    first.level,
                    IPipeNode<*, *>::getPipePos,
                    gtPipePredicate,
                    limit,
                    limit * 6
                )
                return paintPaintables(collected, context)
            }

            is IPaintable -> {
                val collected = BreadthFirstBlockSearch.conditionalSearch(
                    IPaintable::class.java,
                    first,
                    first.level,
                    { p -> (p as BlockEntity).blockPos },
                    paintablePredicate,
                    limit,
                    limit * 6
                )
                return paintPaintables(collected, context)
            }

            else -> return false
        }
    }

    fun <T : IPaintable> paintPaintables(paintables: Collection<T>, context: UseOnContext): Boolean {
        var painted = false
        val color = selectedColor(context.itemInHand)
        for (paintable in paintables) {
            if (paintable.paintingColor == color) continue
            val player = context.player ?: break
            if (!usePaint(player, context.hand, context.itemInHand)) break
            paintable.paintingColor = color
            painted = true
        }
        return painted
    }

    fun usePaint(player: Player, hand: InteractionHand, stack: ItemStack): Boolean {
        if (player.isCreative) return true

        // Try topping up every time the can is used.
        refillFromInventory(player, stack)

        val usesLeft = usesLeft(stack)
        if (usesLeft <= 0) {
            player.displayClientMessage(ExpPlayerMessages.RgbSprayRequiresFuel.text(), true)
            return false
        }

        usesLeft(stack, usesLeft - 1)
        player.setItemInHand(hand, stack)
        return true
    }

    fun refillFromInventory(player: Player, stack: ItemStack): Int {
        val paint = paintItem()
        if (paint.isEmpty) return 0

        val currentUses = usesLeft(stack)

        // Number of whole dyes that can fit without wasting any refill.
        var dyesNeeded = (TOTAL_USES - currentUses) / DYE_REFILL
        if (dyesNeeded <= 0) return 0

        var consumed = 0
        val inv = player.inventory

        for (i in 0..<inv.containerSize) {
            if (dyesNeeded <= 0) break

            val fuelStack = inv.getItem(i)
            if (!fuelStack.`is`(paint.item)) continue

            val toConsume = min(fuelStack.count, dyesNeeded)

            fuelStack.shrink(toConsume)
            consumed += toConsume
            dyesNeeded -= toConsume
        }

        if (consumed > 0) {
            usesLeft(stack, currentUses + consumed * DYE_REFILL)
        }

        return consumed
    }

    override fun createUI(
        holder: HeldItemUIFactory.HeldItemHolder,
        player: Player
    ): ModularUI? {
        val held = holder.held
        val colorPicker = HsbColorWidget(10, 22, 112, 112)
            .setShowAlpha(false)
            .setColorSupplier { opaque(selectedColor(held)) }
        val hexFieldRef = AtomicReference<TextFieldWidget>()

        colorPicker.setOnChanged { argb ->
            val color = argb and 0xFFFFFF
            selectedColor(held, color)
            holder.markAsDirty()

            val hexField = hexFieldRef.get()
            if (!hexField.isNull()) {
                hexField.setCurrentString(formatColor(color))
            }
        }
        colorPicker.setColor(opaque(selectedColor(held)))

        val hexField = TextFieldWidget(132, 88, 62, 16, null) { value ->
            val parsed = parseCompleteHex(value) ?: return@TextFieldWidget
            selectedColor(held, parsed)
            holder.markAsDirty()
            colorPicker.setColor(opaque(parsed))
        }
        hexField.setMaxStringLength(7)
        hexField.setValidator(::normalizeHex)
        hexField.setCurrentString(formatColor(selectedColor(held)))
        hexFieldRef.set(hexField)

        return ModularUI(206, 146, holder, player)
            .background(GuiTextures.BACKGROUND)
            .widget(LabelWidget(8, 8, ExpGuiText.RgbSprayTitle.translationKey))
            .widget(colorPicker)
            .widget(LabelWidget(132, 22, ExpGuiText.RgbSprayPreview.translationKey))
            .widget(ColorBlockWidget(132, 34, 46, 46)
                .setColorSupplier { opaque(selectedColor(held)) })
            .widget(LabelWidget(132, 90, ExpGuiText.RgbSprayHex.translationKey))
            .widget(hexField);
    }

    override fun appendHoverText(stack: ItemStack, p1: Level?, tooltip: MutableList<Component>, flag: TooltipFlag) {
        tooltip.add(ExpTooltips.RgbSprayColor.text(formatColor(selectedColor(stack))))
        tooltip.add(ExpTooltips.RgbSprayUses.text(usesLeft(stack), TOTAL_USES))
        tooltip.add(ExpTooltips.RgbSprayFuel.text())
        tooltip.add(ExpTooltips.RgbSprayCopyColor.text())
    }

    override fun getDurabilityForDisplay(stack: ItemStack): Float {
        return usesLeft(stack) / TOTAL_USES.toFloat()
    }

    override fun getMaxDurability(stack: ItemStack?) = TOTAL_USES

    override fun getBarColor(stack: ItemStack): Int {
        val colors = getDurabilityColorsForDisplay(stack)
        val ratio = max(0f, getDurabilityForDisplay(stack))
        return mixColors(ratio, colors.leftInt(), colors.rightInt())
    }

    override fun getDurabilityColorsForDisplay(itemStack: ItemStack): IntIntPair {
        return GradientUtil.getGradient(selectedColor(itemStack), 10)
    }

    fun selectedColor(stack: ItemStack): Int {
        val tag = stack.tag
        if (tag.isNull() || !tag!!.contains(SELECTED_COLOR_TAG, Tag.TAG_INT.toInt())) return DEFAULT_COLOR
        return tag.getInt(SELECTED_COLOR_TAG) and 0xFFFFFF
    }

    fun selectedColor(stack: ItemStack, color: Int) {
        stack.orCreateTag.putInt(SELECTED_COLOR_TAG, color and 0xFFFFFF)
    }

    fun targetColor(target: BlockEntity?): Int? {
        val paintable = target as? IPaintable ?: return null
        if (!paintable.isPainted) return null
        return paintable.paintingColor and 0xFFFFFF
    }

    fun copyTargetColor(stack: ItemStack, target: BlockEntity?): Boolean {
        val color = targetColor(target) ?: return false
        selectedColor(stack, color)
        return true
    }

    fun usesLeft(stack: ItemStack): Int {
        val tag = stack.tag
        if (tag.isNull() || !tag!!.contains(USES_LEFT_TAG, Tag.TAG_INT.toInt())) return TOTAL_USES
        return max(0, min(TOTAL_USES, tag.getInt(USES_LEFT_TAG)))
    }

    fun usesLeft(stack: ItemStack, usesLeft: Int) {
        stack.orCreateTag.putInt(USES_LEFT_TAG, max(0, min(TOTAL_USES, usesLeft)))
    }

    fun normalizeHex(hex: String?): String {
        if (hex.isNullOrBlank()) return ""

        var raw = hex.trim()
        if (raw.startsWith("#")) raw = raw.substring(1)
        val normalized = StringBuilder("#")
        for (c in raw) {
            if (normalized.length >= 7) break
            val upperC = c.uppercaseChar()
            if (upperC in '0'..'9' || upperC in 'A'..'F') {
                normalized.append(upperC)
            }
        }
        return normalized.toString()
    }

    fun parseCompleteHex(hex: String?): Int? {
        val normalized = normalizeHex(hex)
        if (normalized.length != 7) return null
        return normalized.substring(1).toInt(16)
    }

    fun formatColor(color: Int) = "#%06X".format(Locale.ROOT, color and 0xFFFFFF)

    fun opaque(color: Int) = 0xFF000000.toInt() or (color and 0xFFFFFF)

    fun mixColors(ratio: Float, vararg colors: Int): Int {
        var r = 0
        var g = 0
        var b = 0
        val ratio = ratio * (1f / colors.size)
        for (color in colors) {
            r += (((color shr 16) and 0xFF) * ratio).toInt()
            g += (((color shr 8) and 0xFF) * ratio).toInt()
            b += (((color) and 0xFF) * ratio).toInt()
        }
        return opaque((r shl 16) or (g shl 8) or b)
    }
}
