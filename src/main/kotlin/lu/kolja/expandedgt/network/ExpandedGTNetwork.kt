package lu.kolja.expandedgt.network

import lu.kolja.expandedgt.ExpandedGT
import lu.kolja.expandedgt.definiton.ExpandedGTItems
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraftforge.network.NetworkDirection
import net.minecraftforge.network.NetworkEvent
import net.minecraftforge.network.NetworkRegistry
import net.minecraftforge.network.simple.SimpleChannel
import java.util.function.Supplier

object ExpandedGTNetwork {
    private const val PROTOCOL_VERSION = "1"
    private var nextPacketId = 0

    private val channel: SimpleChannel = NetworkRegistry.newSimpleChannel(
        ExpandedGT.makeId("network"),
        { PROTOCOL_VERSION },
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    )

    fun register() {
        channel.messageBuilder(
            CopySprayColorPacket::class.java,
            nextPacketId++,
            NetworkDirection.PLAY_TO_SERVER
        )
            .encoder(CopySprayColorPacket::encode)
            .decoder(CopySprayColorPacket::decode)
            .consumerMainThread(CopySprayColorPacket::handle)
            .add()
    }

    fun copySprayColor(pos: BlockPos, hand: InteractionHand) {
        channel.sendToServer(CopySprayColorPacket(pos, hand))
    }

    data class CopySprayColorPacket(
        val pos: BlockPos,
        val hand: InteractionHand
    ) {
        fun encode(buffer: FriendlyByteBuf) {
            buffer.writeBlockPos(pos)
            buffer.writeEnum(hand)
        }

        fun handle(context: Supplier<NetworkEvent.Context>) {
            val player = context.get().sender ?: return
            copyColor(player)
        }

        private fun copyColor(player: ServerPlayer) {
            if (!player.canReach(pos, 1.5)) return

            val stack = player.getItemInHand(hand)
            if (!stack.`is`(ExpandedGTItems.RGBSprayCan.get())) return

            val target = player.level().getBlockEntity(pos)
            if (ExpandedGTItems.RGBSprayCanBehavior.copyTargetColor(stack, target)) {
                player.inventory.setChanged()
            }
        }

        companion object {
            fun encode(packet: CopySprayColorPacket, buffer: FriendlyByteBuf) {
                packet.encode(buffer)
            }

            fun decode(buffer: FriendlyByteBuf) = CopySprayColorPacket(
                buffer.readBlockPos(),
                buffer.readEnum(InteractionHand::class.java)
            )

            fun handle(packet: CopySprayColorPacket, context: Supplier<NetworkEvent.Context>) {
                packet.handle(context)
            }
        }
    }
}
