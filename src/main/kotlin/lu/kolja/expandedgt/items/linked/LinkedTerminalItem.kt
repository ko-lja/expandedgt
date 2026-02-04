package lu.kolja.expandedgt.items.linked

import appeng.core.localization.GuiText
import appeng.core.localization.Tooltips
import com.gregtechceu.gtceu.api.item.component.IItemUIFactory
import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController
import lu.kolja.expandedgt.interfaces.IBlockPattern
import lu.kolja.expandedgt.lang.ExpTooltips
import lu.kolja.expandedgt.util.bold
import lu.kolja.expandedgt.util.shiftInfo
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level

class LinkedTerminalItem(properties: Properties): LinkedItem(properties) {

    override fun useOn(context: UseOnContext): InteractionResult {
        val player = context.player
        val level = context.level
        if (player == null || !player.isShiftKeyDown || level.isClientSide) return InteractionResult.PASS
        val pos = context.clickedPos
        val item = context.itemInHand
        val machine = MetaMachine.getMachine(level, pos)
        if (machine !is IMultiController) return InteractionResult.PASS
        if (machine.isFormed) return InteractionResult.PASS // TODO: destroy if formed
        val grid = getLinkedGrid(item, level, player)
        grid?.let {
            (machine.pattern as IBlockPattern).exAutoBuild(player, machine.multiblockState, it)
            player.cooldowns.addCooldown(this, 20)
        }
        return InteractionResult.sidedSuccess(false)
    }

    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        lines: MutableList<Component>,
        isAdvanced: TooltipFlag
    ) {
        val pos = linkedPos(stack)
        pos?.let {
            lines.add(Tooltips.of(GuiText.Linked, Tooltips.GREEN))
            lines.shiftInfo(ExpTooltips.BoundTo.text("${pos.dimension().location().path}[${pos.pos().toShortString()}]").bold())
        }.ifNull {
            lines.add(Tooltips.of(GuiText.Unlinked, Tooltips.RED))
        }
        super.appendHoverText(stack, level, lines, isAdvanced)
    }
    fun <T> T?.ifNull(run: () -> Unit) = if (this == null) run() else Unit
}