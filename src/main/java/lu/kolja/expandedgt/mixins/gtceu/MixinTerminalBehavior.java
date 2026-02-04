package lu.kolja.expandedgt.mixins.gtceu;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.pattern.error.PatternError;
import com.gregtechceu.gtceu.common.item.TerminalBehavior;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = TerminalBehavior.class, remap = false)
public class MixinTerminalBehavior {
    /**
     * @author
     * @reason
     */
    @Overwrite
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            Level level = context.getLevel();
            BlockPos blockPos = context.getClickedPos();
            if (context.getPlayer() == null) return InteractionResult.PASS;
            MetaMachine machine = MetaMachine.getMachine(level, blockPos);
            if (!(machine instanceof IMultiController controller)) return InteractionResult.PASS;
            if (controller.isFormed()) return InteractionResult.PASS;
            var state = controller.getMultiblockState();
            if (state.error != null) {
                expgt$showError(context.getPlayer(), state.error, context.getItemInHand());
            }
            if (!level.isClientSide) {
                controller.getPattern().autoBuild(context.getPlayer(), controller.getMultiblockState());
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Unique
    private void expgt$showError(Player player, PatternError error, ItemStack stack) {
        player.sendSystemMessage(error.getErrorInfo());
        addPos(stack, error.getPos());
    }

    private void addPos(ItemStack stack, BlockPos pos) {
        var tag = stack.getOrCreateTagElement("error_pos");
        if (tag.contains("pos", Tag.TAG_LIST)) {
            var list = tag.getList("pos", Tag.TAG_COMPOUND);
            list.add(NbtUtils.writeBlockPos(pos));
        } else {
            var list = new ListTag();
            list.add(NbtUtils.writeBlockPos(pos));
            tag.put("pos", list);
        }
    }

    private BlockPos[] getPos(ItemStack stack) {
        var tag = stack.getOrCreateTagElement("error_pos");
        if (tag.contains("pos", Tag.TAG_LIST)) {
            return tag.getList("pos", Tag.TAG_COMPOUND)
                    .stream()
                    .map(t -> (CompoundTag) t)
                    .map(NbtUtils::readBlockPos)
                    .toArray(BlockPos[]::new);
        }
        return null;
    }
}
