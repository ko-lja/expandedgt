package lu.kolja.expandedgt

import com.gregtechceu.gtceu.api.GTCEuAPI
import com.gregtechceu.gtceu.api.machine.MachineDefinition
import com.gregtechceu.gtceu.common.data.GTCreativeModeTabs
import com.mojang.logging.LogUtils
import com.tterrag.registrate.providers.ProviderType
import com.tterrag.registrate.util.entry.RegistryEntry
import lu.kolja.expandedgt.datagen.ExpLangProvider
import lu.kolja.expandedgt.definiton.ExpandedGTMachines
import lu.kolja.expandedgt.lang.ExpGuiText
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTab
import net.minecraftforge.fml.common.Mod
import org.jetbrains.annotations.Contract
import org.slf4j.Logger
import thedarkcolour.kotlinforforge.forge.MOD_BUS

@Mod(ExpandedGT.MODID)
class ExpandedGT {
    companion object {
        const val MODID = "expandedgt"
        val LOGGER: Logger = LogUtils.getLogger()
        val REGISTRATE: ExpGTRegistrate = ExpGTRegistrate(MOD_BUS)
        val CREATIVE_TAB: RegistryEntry<CreativeModeTab> = REGISTRATE
            .defaultCreativeTab(MODID) {
                it.displayItems(GTCreativeModeTabs.RegistrateDisplayItemsGenerator(MODID, REGISTRATE))
                    .title(ExpGuiText.CreativeTab.text())
                    .icon(ExpandedGTMachines.ExpandedMEInputBus::asStack)
                    .build()
            }.register()

        @Contract("_ -> new")
        fun makeId(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(MODID, path)
    }

    init {
        REGISTRATE.registerEventListeners(MOD_BUS)
        REGISTRATE.addDataGenerator(ProviderType.LANG, ExpLangProvider::addTranslations)
        MOD_BUS.addGenericListener(MachineDefinition::class.java, ::registerMachines)
        LOGGER.info("Expanded GT is now loaded")
    }

    private fun registerMachines(event: GTCEuAPI.RegisterEvent<ResourceLocation, MachineDefinition>) {
        ExpandedGTMachines // Initializes the Object
    }
}