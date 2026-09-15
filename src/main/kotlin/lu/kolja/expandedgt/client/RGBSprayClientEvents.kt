package lu.kolja.expandedgt.client

import lu.kolja.expandedgt.definiton.ExpandedGTItems
import lu.kolja.expandedgt.network.ExpandedGTNetwork
import net.minecraft.client.Minecraft
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.BlockHitResult
import net.minecraftforge.client.event.InputEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.eventbus.api.SubscribeEvent

object RGBSprayClientEvents {
    fun register() {
        MinecraftForge.EVENT_BUS.register(this)
    }

    @SubscribeEvent
    fun onInteractionKeyMapping(event: InputEvent.InteractionKeyMappingTriggered) {
        if (!event.isPickBlock) return

        val minecraft = Minecraft.getInstance()
        if (minecraft.screen != null) return

        val player = minecraft.player ?: return
        val hand = sprayCanHand(player.mainHandItem, player.offhandItem) ?: return
        val hit = minecraft.hitResult as? BlockHitResult ?: return
        val target = minecraft.level?.getBlockEntity(hit.blockPos) ?: return
        if (ExpandedGTItems.RGBSprayCanBehavior.targetColor(target) == null) return

        ExpandedGTNetwork.copySprayColor(hit.blockPos, hand)
        event.isCanceled = true
        event.setSwingHand(false)
    }

    private fun sprayCanHand(mainHand: ItemStack, offHand: ItemStack): InteractionHand? {
        return when {
            mainHand.`is`(ExpandedGTItems.RGBSprayCan.get()) -> InteractionHand.MAIN_HAND
            offHand.`is`(ExpandedGTItems.RGBSprayCan.get()) -> InteractionHand.OFF_HAND
            else -> null
        }
    }
}
