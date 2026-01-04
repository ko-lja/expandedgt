package lu.kolja.expandedgt.xmod

import appeng.api.config.Actionable
import appeng.api.stacks.AEFluidKey
import appeng.api.stacks.AEKey
import appeng.api.stacks.GenericStack
import appeng.util.prioritylist.IPartitionList
import com.glodblock.github.extendedae.common.me.taglist.TagPriorityList
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity
import com.gregtechceu.gtceu.integration.ae2.machine.MEStockingHatchPartMachine
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder
import lu.kolja.expandedgt.ExpandedGT
import lu.kolja.expandedgt.interfaces.ITagFilterPartMachine
import net.minecraft.nbt.CompoundTag
import java.util.*

class METagFilterStockHatchPartMachine(holder: IMachineBlockEntity): MEStockingHatchPartMachine(holder), ITagFilterPartMachine {
    val managedFieldHolder = ManagedFieldHolder(METagFilterStockHatchPartMachine::class.java, MANAGED_FIELD_HOLDER)

    @Persisted
    @DescSynced
    var whitelist = ""
    @Persisted
    @DescSynced
    var blacklist = ""

    var filter: IPartitionList? = null

    init {
        if (filter == null) filter = TagPriorityList(whitelist, blacklist)
        setAutoPullTest {
            filter!!.isListed(it.what)
        }
    }

    override fun refreshList() {
        val grid = this.mainNode.grid
        if (grid == null) {
            aeFluidHandler.clearInventory(0)
            return
        }
        val counter = grid.storageService.cachedInventory
        if (counter.isEmpty) return

        val storage = grid.storageService.inventory
        val queue = PriorityQueue<GenericStack>(CONFIG_SIZE, compareBy { it.amount })

        try {
            for (entry in counter) {
                val amount = entry.longValue
                if (amount <= 0) continue
                val what = entry.key
                if (what !is AEFluidKey) continue
                val free = queue.size < CONFIG_SIZE
                if (!free && queue.peek().amount >= amount) continue
                if (!test(what)) continue
                val stack = GenericStack(what, amount)
                if (testConfiguredInOtherPart(stack)) continue
                if (free) queue.add(stack)
                else {
                    queue.poll()
                    queue.offer(stack)
                }
            }
        } catch (e: NullPointerException) {
            ExpandedGT.LOGGER.error("Failed to refresh tag filter stocking hatch", e)
        }
        var index = 0
        val size = queue.size
        for (i in 0..<CONFIG_SIZE) {
            if (queue.isEmpty()) break
            val stack = queue.poll()

            val what = stack.what
            val amount = stack.amount

            val request = storage.extract(what, amount, Actionable.SIMULATE, this.actionSource)

            val slot = this.aeFluidHandler.inventory[size - i - 1]
            slot.config = GenericStack(what, 1)
            slot.stock = GenericStack(what, request)
            index++
        }
        aeFluidHandler.clearInventory(index)
    }

    fun test(key: AEKey): Boolean {
        if (filter == null) filter = TagPriorityList(whitelist, blacklist)
        return filter!!.isListed(key)
    }

    override fun attachConfigurators(configuratorPanel: ConfiguratorPanel) {
        super.attachConfigurators(configuratorPanel)
        configuratorPanel.attachConfigurators(ITagFilterPartMachine.TagFilterConfigurator(this))
    }

    override fun writeConfigToTag(): CompoundTag {
        val tag = super.writeConfigToTag()
        tag.putString(ITagFilterPartMachine.TAG_WHITELIST, whitelist)
        tag.putString(ITagFilterPartMachine.TAG_BLACKLIST, blacklist)
        return tag
    }

    override fun readConfigFromTag(tag: CompoundTag) {
        super.readConfigFromTag(tag)
        if (tag.contains(ITagFilterPartMachine.TAG_WHITELIST)) {
            whitelist = tag.getString(ITagFilterPartMachine.TAG_WHITELIST)
        }
        if (tag.contains(ITagFilterPartMachine.TAG_BLACKLIST)) {
            blacklist = tag.getString(ITagFilterPartMachine.TAG_BLACKLIST)
        }
    }

    override fun getTagWhiteList() = whitelist

    override fun getTagBlackList() = blacklist

    override fun setTagWhiteList(tagWhiteList: String) {
        this.whitelist = tagWhiteList
        filter = TagPriorityList(tagWhiteList, blacklist)
    }

    override fun setTagBlackList(tagBlackList: String) {
        this.blacklist = tagBlackList
        filter = TagPriorityList(whitelist, tagBlackList)
    }

    override fun getFieldHolder() = managedFieldHolder
}