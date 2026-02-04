package lu.kolja.expandedgt.interfaces

import appeng.api.networking.IGrid
import com.gregtechceu.gtceu.api.pattern.MultiblockState
import net.minecraft.world.entity.player.Player

interface IBlockPattern {
    fun exAutoBuild(player: Player, worldState: MultiblockState, grid: IGrid)
}