package lu.kolja.expandedgt.interfaces

import com.gregtechceu.gtceu.api.cover.filter.Filter
import com.gregtechceu.gtceu.api.cover.filter.FluidFilter
import com.gregtechceu.gtceu.api.cover.filter.ItemFilter
import com.gregtechceu.gtceu.api.gui.GuiTextures
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfigurator
import com.gregtechceu.gtceu.api.gui.widget.PhantomSlotWidget
import com.gregtechceu.gtceu.api.gui.widget.ScrollablePhantomFluidWidget
import com.gregtechceu.gtceu.api.machine.feature.IDropSaveMachine
import com.gregtechceu.gtceu.api.transfer.fluid.CustomFluidTank
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget
import com.lowdragmc.lowdraglib.gui.widget.Widget
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup
import com.lowdragmc.lowdraglib.utils.Position
import lu.kolja.expandedgt.lang.ExpGuiText
import lu.kolja.expandedgt.mixins.AccessorCustomFluidTank
import lu.kolja.expandedgt.mixins.AccessorItemStackHandler
import lu.kolja.expandedgt.util.literal
import lu.kolja.expandedgt.widgets.MlTextField
import lu.kolja.expandedgt.widgets.TagLabelContainer
import lu.kolja.expandedgt.xmod.METagFilterStockBusPartMachine
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.tags.TagKey
import net.minecraft.world.item.ItemStack
import net.minecraftforge.fluids.FluidStack
import java.util.stream.Stream

interface ITagFilterPartMachine: IDropSaveMachine {
    companion object {
        const val TAG_WHITELIST = "TagWhitelist"
        const val TAG_BLACKLIST = "TagBlacklist"
    }

    fun getTagWhiteList(): String

    fun getTagBlackList(): String

    fun setTagWhiteList(tagWhiteList: String)

    fun setTagBlackList(tagBlackList: String)

    override fun saveToItem(tag: CompoundTag) {
        super.saveToItem(tag)
        tag.putString(TAG_WHITELIST, getTagWhiteList())
        tag.putString(TAG_BLACKLIST, getTagBlackList())
    }

    override fun loadFromItem(tag: CompoundTag) {
        super.loadFromItem(tag)
        if (tag.contains(TAG_WHITELIST)) {
            setTagWhiteList(tag.getString(TAG_WHITELIST))
        }
        if (tag.contains(TAG_BLACKLIST)) {
            setTagBlackList(tag.getString(TAG_BLACKLIST))
        }
    }

    class TagFilterConfigurator(val machine: ITagFilterPartMachine): IFancyConfigurator {
        override fun getTitle(): Component = ExpGuiText.TagFilterConfig.text()

        override fun getIcon(): IGuiTexture = GuiTextures.BUTTON_BLACKLIST.getSubTexture(0f, 0f, 1f, .5f)

        override fun createConfigurator(): Widget? {
            val whitelistField = MlTextField(
                9, 6, 114, 32,
                machine::getTagWhiteList,
                machine::setTagWhiteList,
                "...".literal()
            )

            val blacklistField = MlTextField(
                9, 50, 114, 32,
                machine::getTagBlackList,
                machine::setTagBlackList,
                "...".literal()
            )

            val isItem = machine is METagFilterStockBusPartMachine
            val whitelistWidget: StackHandlerWidget<*, *> =
                if (isItem) PhantomSlot(CustomItemStackHandler(1)) else TankSlot(CustomFluidTank(1))
            val blacklistWidget: StackHandlerWidget<*, *> =
                if (isItem) PhantomSlot(CustomItemStackHandler(1)) else TankSlot(CustomFluidTank(1))

            val group = WidgetGroup(-25, 0, 150, 146)
                .addWidget(LabelWidget(9, -4) { ExpGuiText.Whitelist.translationKey })
                .addWidget(whitelistField)
                .addWidget(whitelistWidget as Widget)
                .addWidget(LabelWidget(9, 40) { ExpGuiText.Blacklist.translationKey })
                .addWidget(blacklistField)
                .addWidget(blacklistWidget as Widget)
                .addWidget(LabelWidget(10, 86) { ExpGuiText.Wildcard.translationKey })
                .addWidget(LabelWidget(8, 102) { ExpGuiText.Priority.translationKey })
                .addWidget(LabelWidget(9, 118) { ExpGuiText.AndOr.translationKey })
                .addWidget(LabelWidget(9, 134) { ExpGuiText.XorNot.translationKey })

            val container = DraggableScrollableWidgetGroup(0, 149, group.sizeWidth, 80)
            container.setClientSideWidget()
                .setActive(false)
                .setVisible(false)
                .setBackground(GuiTextures.BACKGROUND_INVERSE)
                .setSelfPosition((group.sizeWidth - container.sizeWidth) / 2, group.sizeHeight)

            val callback: (StackHandlerWidget<*, *>, MlTextField) -> Runnable = { widget, field ->
                Runnable {
                    val tags = widget.getTags().map { it.location.toString() }.toList()

                    val newWidgets = TagLabelContainer.createTagLabelContainer(field, tags)
                    container.clearAllWidgets()
                    container.addWidgets(*newWidgets)

                    if (!widget.isEmpty()) container.setVisible(true).isActive = true
                    else container.setVisible(false).isActive = false
                }
            }

            whitelistWidget.setOnContentsChanged(callback(whitelistWidget, whitelistField))
            whitelistWidget.selfPosition = Position(
                whitelistField.selfPositionX + whitelistField.sizeWidth + 4,
                whitelistField.selfPositionY
            )

            blacklistWidget.setOnContentsChanged(callback(blacklistWidget, blacklistField))
            blacklistWidget.selfPosition = Position(
                blacklistField.selfPositionX + blacklistField.sizeWidth + 4,
                blacklistField.selfPositionY
            )

            return group.addWidget(container)
        }
    }

    interface StackHandlerWidget<STACK, FILTER: Filter<STACK, FILTER>> {
        fun getStack(): STACK

        fun setOnContentsChanged(onContentsChanged: Runnable)

        fun isEmpty(): Boolean

        fun getTags(): Stream<TagKey<*>>
    }

    class PhantomSlot(val handler: CustomItemStackHandler): PhantomSlotWidget(handler, 0, 90, 30), StackHandlerWidget<ItemStack, ItemFilter> {
        init {
            setBackground(GuiTextures.SLOT)
        }

        override fun updateScreen() {
            super.updateScreen()
            setMaxStackSize(1)
        }

        override fun detectAndSendChanges() {
            super.detectAndSendChanges()
            setMaxStackSize(1)
        }

        override fun getStack() = (handler as AccessorItemStackHandler).stacks[0]

        override fun setOnContentsChanged(onContentsChanged: Runnable) {
            handler.onContentsChanged = onContentsChanged
        }

        override fun isEmpty() = getStack().isEmpty

        override fun getTags(): Stream<TagKey<*>> = getStack().tags.map { it }
    }
    class TankSlot(var customFluidTank: CustomFluidTank):
        ScrollablePhantomFluidWidget(customFluidTank, 0, 90, 30, 18, 18, customFluidTank::getFluid, customFluidTank::setFluid),
        StackHandlerWidget<FluidStack, FluidFilter> {

        init {
            setBackground(GuiTextures.SLOT)
            setClientSideWidget()
        }

        override fun readUpdateInfo(id: Int, buffer: FriendlyByteBuf?) {
            if (id == 12) {
                (customFluidTank as AccessorCustomFluidTank).invokeOnContentsChanged()
                return
            }
            super.readUpdateInfo(id, buffer)
        }

        override fun getStack() = customFluidTank.getFluidInTank(0)

        override fun setOnContentsChanged(onContentsChanged: Runnable) {
            customFluidTank.onContentsChanged = Runnable {
                if (!isRemote) writeUpdateInfo(12) { it.writeBoolean(true) }
                else onContentsChanged.run()
            }
        }

        override fun isEmpty() = getStack().isEmpty

        override fun getTags(): Stream<TagKey<*>> = getStack().fluid.defaultFluidState().tags.map { it }
    }
}