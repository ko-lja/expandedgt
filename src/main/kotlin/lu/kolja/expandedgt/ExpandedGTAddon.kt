package lu.kolja.expandedgt

import com.glodblock.github.extendedae.common.EPPItemAndBlock
import com.gregtechceu.gtceu.api.GTValues.ULV
import com.gregtechceu.gtceu.api.GTValues.VA
import com.gregtechceu.gtceu.api.addon.GTAddon
import com.gregtechceu.gtceu.api.addon.IGTAddon
import com.gregtechceu.gtceu.common.data.GTItems
import com.gregtechceu.gtceu.common.data.GTRecipeTypes.*
import lu.kolja.expandedgt.definiton.ExpandedGTItems
import lu.kolja.expandedgt.definiton.ExpandedGTMachines
import net.minecraft.data.recipes.FinishedRecipe
import net.minecraft.world.item.Items
import net.minecraft.world.level.material.Fluids
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fml.ModList
import net.minecraftforge.fml.loading.FMLEnvironment
import java.util.function.Consumer

@GTAddon
class ExpandedGTAddon: IGTAddon {
    override fun getRegistrate() = ExpandedGT.REGISTRATE

    override fun initializeAddon() {}

    override fun addonModId() = ExpandedGT.MODID

    override fun addRecipes(provider: Consumer<FinishedRecipe>) {
        Recipes.addRecipes(provider)
    }
    object Recipes {
        fun addRecipes(provider: Consumer<FinishedRecipe>) {
            addRgbSprayRecipes(provider)

            if (ModList.get().isLoaded("expatternprovider")) {
                COMPRESSOR_RECIPES.recipeBuilder(ExpandedGT.makeId("fishbig"))
                    .inputItems(Items.PUFFERFISH, 8)
                    .outputItems(EPPItemAndBlock.FISHBIG.asItem())
                    .duration(160)
                    .EUt(128)
                    .save(provider)
            }

            if (!FMLEnvironment.production) {
                MACERATOR_RECIPES.recipeBuilder("test")
                    .inputItems(ExpandedGTMachines.ExpandedMEInputBus.asStack()::getItem)
                    .duration(1)
                    .EUt(100)
                    .outputItems(Items.STONE, 100_000_000)
                    .outputFluids(FluidStack(Fluids.LAVA, 100_000_000))
                    .save(provider)
            }
        }

        private fun addRgbSprayRecipes(provider: Consumer<FinishedRecipe>) {
            MIXER_RECIPES.recipeBuilder(ExpandedGT.makeId("universal_dye"))
                .inputItems(Items.RED_DYE)
                .inputItems(Items.GREEN_DYE)
                .inputItems(Items.BLUE_DYE)
                .outputItems(ExpandedGTItems.UniversalDye)
                .duration(100)
                .EUt(VA[ULV].toLong())
                .save(provider)

            CANNER_RECIPES.recipeBuilder(ExpandedGT.makeId("rgb_spray_can"))
                .inputItems(GTItems.SPRAY_EMPTY)
                .inputItems(ExpandedGTItems.UniversalDye)
                .outputItems(ExpandedGTItems.RGBSprayCan)
                .duration(200)
                .EUt(VA[ULV].toLong())
                .save(provider)
        }
    }
}
