package lu.kolja.expandedgt

import com.gregtechceu.gtceu.api.addon.GTAddon
import com.gregtechceu.gtceu.api.addon.IGTAddon

@GTAddon
class ExpandedGTAddon: IGTAddon {
    override fun getRegistrate() = ExpandedGT.REGISTRATE

    override fun initializeAddon() {}

    override fun addonModId() = ExpandedGT.MODID
}