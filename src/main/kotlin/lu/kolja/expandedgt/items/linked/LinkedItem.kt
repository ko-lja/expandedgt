package lu.kolja.expandedgt.items.linked

import appeng.api.features.IGridLinkableHandler
import appeng.api.implementations.blockentities.IWirelessAccessPoint
import appeng.api.networking.IGrid
import appeng.core.localization.PlayerMessages
import appeng.items.AEBaseItem
import appeng.util.Platform
import com.gregtechceu.gtceu.api.item.ComponentItem
import com.mojang.datafixers.util.Pair
import net.minecraft.Util
import net.minecraft.core.GlobalPos
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.Tag
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import org.jline.utils.Log

abstract class LinkedItem(properties: Properties): ComponentItem(properties) {
    companion object{
        val handler: IGridLinkableHandler = LinkedTerminalHandler();
        const val nbtAccessPoint = "accessPoint"
    }

    class LinkedTerminalHandler: IGridLinkableHandler {
        override fun canLink(stack: ItemStack) = stack.item is LinkedItem

        override fun link(stack: ItemStack, pos: GlobalPos) {
            GlobalPos.CODEC.encodeStart(
                NbtOps.INSTANCE,
                pos
            ).result().ifPresent { stack.orCreateTag.put(nbtAccessPoint, it) }
        }

        override fun unlink(stack: ItemStack) {
            stack.removeTagKey(nbtAccessPoint)
        }
    }

    fun linkedPos(stack: ItemStack): GlobalPos? {
        val tag = stack.tag
        if (tag == null || !tag.contains(nbtAccessPoint, Tag.TAG_COMPOUND.toInt())) return null
        return GlobalPos.CODEC.decode(
            NbtOps.INSTANCE,
            tag[nbtAccessPoint])
            .resultOrPartial { Util.prefix("Linked position", Log::error) }
            .map { it.first }
            .orElse(null)
    }

    fun getLinkedGrid(stack: ItemStack, level: Level, who: Player?): IGrid? {
        if (level.isClientSide) return null
        val pos = linkedPos(stack)
        if (pos == null) {
            who?.displayClientMessage(PlayerMessages.DeviceNotLinked.text(), true);
            return null;
        }
        val linkedLevel = level.server!!.getLevel(pos.dimension());
        if (linkedLevel == null) {
            who?.displayClientMessage(PlayerMessages.LinkedNetworkNotFound.text(), true);
            return null;
        }

        val be = Platform.getTickingBlockEntity(linkedLevel, pos.pos());
        if (be !is IWirelessAccessPoint) {
            who?.displayClientMessage(PlayerMessages.LinkedNetworkNotFound.text(), true);
            return null;
        }

        val grid = be.grid
        if (grid == null) {
            who?.displayClientMessage(PlayerMessages.LinkedNetworkNotFound.text(), true);
            return null; // Remove if we want infinite range
        }
        return grid;
    }
}