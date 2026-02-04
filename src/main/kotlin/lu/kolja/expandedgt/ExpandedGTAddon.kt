package lu.kolja.expandedgt

import com.gregtechceu.gtceu.api.addon.GTAddon
import com.gregtechceu.gtceu.api.addon.IGTAddon
import com.gregtechceu.gtceu.common.data.GTRecipeTypes.MACERATOR_RECIPES
import lu.kolja.expandedgt.definiton.ExpandedGTMachines
import net.minecraft.data.recipes.FinishedRecipe
import net.minecraft.world.item.Items
import net.minecraft.world.level.material.Fluids
import net.minecraftforge.fluids.FluidStack
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
            MACERATOR_RECIPES.recipeBuilder("test")
                .inputItems(ExpandedGTMachines.ExpandedMEInputBus.asStack()::getItem)
                .duration(1)
                .EUt(100)
                .outputItems(Items.STONE, 100_000_000)
                .outputFluids(FluidStack(Fluids.LAVA, 100_000_000))
                .save(provider)
        }
    }
}