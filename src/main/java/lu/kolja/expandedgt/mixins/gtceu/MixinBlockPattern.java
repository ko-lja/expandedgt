package lu.kolja.expandedgt.mixins.gtceu;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.me.helpers.PlayerSource;
import com.gregtechceu.gtceu.api.item.MetaMachineItem;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.predicates.SimplePredicate;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lu.kolja.expandedgt.interfaces.IBlockPattern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

@Mixin(value = BlockPattern.class, remap = false)
public abstract class MixinBlockPattern implements IBlockPattern {

    @Shadow @Final protected int[] centerOffset;

    @Shadow @Final protected int fingerLength;

    @Shadow @Final public int[][] aisleRepetitions;

    @Shadow @Final protected int thumbLength;

    @Shadow @Final protected int palmLength;

    @Shadow @Final protected TraceabilityPredicate[][][] blockMatches;

    @Shadow protected abstract BlockPos setActualRelativeOffset(int x, int y, int z, Direction facing, Direction upwardsFacing, boolean isFlipped);

    @Shadow protected abstract void resetFacing(BlockPos pos, BlockState blockState, Direction facing, BiPredicate<BlockPos, Direction> checker, Consumer<BlockState> consumer);

    @Unique
    @Override
    public void exAutoBuild(Player player, MultiblockState worldState, IGrid grid, boolean useHatches) {
        Map<AEItemKey, Integer> missingItems = new HashMap<>();
        Map<AEItemKey, Integer> extractionFailedItems = new HashMap<>();
        Map<AEItemKey, Integer> placementFailedItems = new HashMap<>();
        Level world = player.level();
        int minZ = -centerOffset[4];
        worldState.clean();
        IMultiController controller = worldState.getController();
        BlockPos centerPos = controller.self().getPos();
        Direction facing = controller.self().getFrontFacing();
        Direction upwardsFacing = controller.self().getUpwardsFacing();
        boolean isFlipped = controller.self().isFlipped();
        Object2IntOpenHashMap<SimplePredicate> cacheGlobal = worldState.getGlobalCount();
        Object2IntOpenHashMap<SimplePredicate> cacheLayer = worldState.getLayerCount();
        Map<BlockPos, Object> blocks = new HashMap<>();
        Set<BlockPos> placeBlockPos = new HashSet<>();
        var meInventory = grid.getStorageService().getInventory();
        var meContents = player.isCreative() ? null : meInventory.getAvailableStacks();
        var actionSource = player.isCreative() ? null : new PlayerSource(player);
        blocks.put(centerPos, controller);
        for (int c = 0, z = minZ++, r; c < this.fingerLength; c++) {
            for (r = 0; r < aisleRepetitions[c][0]; r++) {
                cacheLayer.clear();
                for (int b = 0, y = -centerOffset[1]; b < this.thumbLength; b++, y++) {
                    for (int a = 0, x = -centerOffset[0]; a < this.palmLength; a++, x++) {
                        TraceabilityPredicate predicate = this.blockMatches[c][b][a];
                        BlockPos pos = setActualRelativeOffset(x, y, z, facing, upwardsFacing, isFlipped)
                                .offset(centerPos.getX(), centerPos.getY(), centerPos.getZ());
                        worldState.update(pos, predicate);
                        if (!world.isEmptyBlock(pos)) {
                            blocks.put(pos, world.getBlockState(pos));
                            for (SimplePredicate limit : predicate.limited) {
                                limit.testLimited(worldState);
                            }
                        } else {
                            boolean find = false;
                            BlockInfo[] infos = new BlockInfo[0];
                            for (SimplePredicate limit : predicate.limited) {
                                if (limit.minLayerCount > 0) {
                                    int curr = cacheLayer.getInt(limit);
                                    if (curr < limit.minLayerCount &&
                                            (limit.maxLayerCount == -1 || curr < limit.maxLayerCount)) {
                                        cacheLayer.addTo(limit, 1);
                                    } else {
                                        continue;
                                    }
                                } else {
                                    continue;
                                }
                                infos = limit.candidates == null ? null : limit.candidates.get();
                                find = true;
                                break;
                            }
                            if (!find) {
                                for (SimplePredicate limit : predicate.limited) {
                                    if (limit.minCount > 0) {
                                        int curr = cacheGlobal.getInt(limit);
                                        if (curr < limit.minCount && (limit.maxCount == -1 || curr < limit.maxCount)) {
                                            cacheGlobal.addTo(limit, 1);
                                        } else {
                                            continue;
                                        }
                                    } else {
                                        continue;
                                    }
                                    infos = limit.candidates == null ? null : limit.candidates.get();
                                    find = true;
                                    break;
                                }
                            }
                            if (!find) { // no limited
                                for (SimplePredicate limit : predicate.limited) {
                                    if (limit.maxLayerCount != -1 &&
                                            cacheLayer.getOrDefault(limit, Integer.MAX_VALUE) == limit.maxLayerCount) {
                                        continue;
                                    }
                                    if (limit.maxCount != -1 &&
                                            cacheGlobal.getOrDefault(limit, Integer.MAX_VALUE) == limit.maxCount) {
                                        continue;
                                    }
                                    cacheLayer.addTo(limit, 1);
                                    cacheGlobal.addTo(limit, 1);
                                    infos = ArrayUtils.addAll(infos,
                                            limit.candidates == null ? null : limit.candidates.get());
                                }
                                for (SimplePredicate common : predicate.common) {
                                    infos = ArrayUtils.addAll(infos,
                                            common.candidates == null ? null : common.candidates.get());
                                }
                            }

                            List<ItemStack> candidates = new ArrayList<>();
                            if (infos != null) {
                                for (BlockInfo info : infos) {
                                    if (info.getBlockState().getBlock() != Blocks.AIR) {
                                        candidates.add(info.getItemStackForm());
                                    }
                                }
                            }
                            if (candidates.isEmpty()) continue;

                            ItemStack found = null;
                            AEItemKey key = null;
                            if (!player.isCreative()) {
                                key = eae$getAvailableCandidate(candidates, meContents, useHatches);
                                if (key == null) {
                                    AEItemKey missing = eae$getFirstAllowedCandidate(candidates, useHatches);
                                    if (missing != null) missingItems.merge(missing, 1, Integer::sum);
                                    continue;
                                }
                                found = key.toStack();
                            } else {
                                for (ItemStack candidate : candidates) {
                                    if (!eae$isAllowedCandidate(candidate, useHatches)) continue;
                                    found = candidate.copy();
                                    break;
                                }
                            }
                            if (found == null) {
                                continue;
                            }
                            BlockItem itemBlock = (BlockItem) found.getItem();
                            BlockPlaceContext context = new BlockPlaceContext(world, player, InteractionHand.MAIN_HAND,
                                    found, BlockHitResult.miss(player.getEyePosition(0), Direction.UP, pos));

                            if (key != null) {
                                long extracted = meInventory.extract(key, 1, Actionable.MODULATE, actionSource);
                                if (extracted != 1) {
                                    meContents.set(key, 0);
                                    extractionFailedItems.merge(key, 1, Integer::sum);
                                    continue;
                                }
                                meContents.remove(key, 1);
                            }

                            InteractionResult interactionResult = itemBlock.place(context);
                            if (interactionResult != InteractionResult.FAIL) {
                                placeBlockPos.add(pos);
                            } else {
                                AEItemKey failed = AEItemKey.of(found);
                                if (failed != null) placementFailedItems.merge(failed, 1, Integer::sum);
                                if (key != null) {
                                    meInventory.insert(key, 1, Actionable.MODULATE, actionSource);
                                    meContents.add(key, 1);
                                }
                            }
                            if (world.getBlockEntity(pos) instanceof IMachineBlockEntity machineBlockEntity) {
                                blocks.put(pos, machineBlockEntity.getMetaMachine());
                            } else {
                                blocks.put(pos, world.getBlockState(pos));
                            }
                        }
                    }
                }
                z++;
            }
        }
        Direction frontFacing = controller.self().getFrontFacing();
        blocks.forEach((pos, block) -> { // adjust facing
            if (!(block instanceof IMultiController)) {
                if (block instanceof BlockState && placeBlockPos.contains(pos)) {
                    resetFacing(pos, (BlockState) block, frontFacing, (p, f) -> {
                        Object object = blocks.get(p.relative(f));
                        return object == null ||
                                (object instanceof BlockState && ((BlockState) object).getBlock() == Blocks.AIR);
                    }, state -> world.setBlock(pos, state, 3));
                } else if (block instanceof MetaMachine machine) {
                    resetFacing(pos, machine.getBlockState(), frontFacing, (p, f) -> {
                        Object object = blocks.get(p.relative(f));
                        if (object == null || (object instanceof BlockState blockState && blockState.isAir())) {
                            return machine.isFacingValid(f);
                        }
                        return false;
                    }, state -> world.setBlock(pos, state, 3));
                }
            }
        });
        eae$sendFailureReport(player, missingItems, extractionFailedItems, placementFailedItems);
    }

    @Unique
    @Nullable
    private static AEItemKey eae$getAvailableCandidate(List<ItemStack> candidates, KeyCounter storageContents, boolean useHatches) {
        for (ItemStack candidate : candidates) {
            if (!eae$isAllowedCandidate(candidate, useHatches)) continue;

            AEItemKey key = AEItemKey.of(candidate);
            if (key != null && storageContents.get(key) > 0) return key;
        }
        return null;
    }

    @Unique
    @Nullable
    private static AEItemKey eae$getFirstAllowedCandidate(List<ItemStack> candidates, boolean useHatches) {
        for (ItemStack candidate : candidates) {
            if (!eae$isAllowedCandidate(candidate, useHatches)) continue;
            AEItemKey key = AEItemKey.of(candidate);
            if (key != null) return key;
        }
        return null;
    }

    @Unique
    private static void eae$sendFailureReport(
            Player player,
            Map<AEItemKey, Integer> missingItems,
            Map<AEItemKey, Integer> extractionFailedItems,
            Map<AEItemKey, Integer> placementFailedItems) {
        if (missingItems.isEmpty() && extractionFailedItems.isEmpty() && placementFailedItems.isEmpty()) return;

        int missing = missingItems.values().stream().mapToInt(Integer::intValue).sum();
        int extractionFailed = extractionFailedItems.values().stream().mapToInt(Integer::intValue).sum();
        int placementFailed = placementFailedItems.values().stream().mapToInt(Integer::intValue).sum();
        player.sendSystemMessage(Component.literal(
                "Linked Terminal: " + missing + " missing, " + extractionFailed + " extraction failures, " +
                        placementFailed + " placement failures."));
        eae$sendItemList(player, "Missing", missingItems);
        eae$sendItemList(player, "Extraction failed", extractionFailedItems);
        eae$sendItemList(player, "Placement failed", placementFailedItems);
    }

    @Unique
    private static void eae$sendItemList(Player player, String label, Map<AEItemKey, Integer> items) {
        for (var entry : items.entrySet()) {
            player.sendSystemMessage(Component.literal("  " + label + ": " + entry.getValue() + "x ")
                    .append(entry.getKey().toStack().getHoverName()));
        }
    }

    @Unique
    private static boolean eae$isAllowedCandidate(ItemStack candidate, boolean useHatches) {
        if (candidate.isEmpty() || !(candidate.getItem() instanceof BlockItem blockItem)) return false;
        return useHatches || !(blockItem instanceof MetaMachineItem);
    }

}
