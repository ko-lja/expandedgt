package lu.kolja.expandedgt

import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate
import net.minecraftforge.eventbus.api.IEventBus

class ExpGTRegistrate(val bus: IEventBus): GTRegistrate(ExpandedGT.MODID) {
    /**
     * Needed to override the default implementation,
     * because it forces the use of the [net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext]
     * instead of the [thedarkcolour.kotlinforforge.KotlinModLoadingContext], which causes a crash.
     */
    override fun getModEventBus() = bus
}