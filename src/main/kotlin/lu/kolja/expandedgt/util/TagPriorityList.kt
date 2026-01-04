
package lu.kolja.expandedgt.util
/*

import appeng.api.stacks.AEKey
import appeng.util.prioritylist.IPartitionList
import com.gregtechceu.gtceu.utils.TagExprFilter
import it.unimi.dsi.fastutil.objects.Reference2BooleanMap
import it.unimi.dsi.fastutil.objects.Reference2BooleanOpenHashMap
import java.util.WeakHashMap
import java.util.function.Predicate

class TagPriorityList(whitelistExpression: String?, blacklistExpression: String?): IPartitionList {
    companion object {
        val INVALIDATOR: Map<TagPriorityList, Runnable> = WeakHashMap()
    }

    val rawWhitelistExpression: String
    val rawBlacklistExpression: String

    init {
        this.rawWhitelistExpression = removeBlank(whitelistExpression ?: "")
        this.rawBlacklistExpression = removeBlank(blacklistExpression ?: "")

    }
    val whitelistPredicate: Predicate<Set<String>>


    val blacklistPredicate: Predicate<Set<String>>

    val memory: Reference2BooleanMap<Object> = Reference2BooleanOpenHashMap()

    override fun isListed(p0: AEKey?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getItems(): Iterable<AEKey?>? {
        TODO("Not yet implemented")
    }

    fun removeBlank(raw: String) = raw.replace("\\s+".toRegex(), "")
}*/
