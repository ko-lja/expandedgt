package lu.kolja.expandedgt.datagen

import appeng.core.localization.LocalizationEnum
import com.tterrag.registrate.providers.RegistrateLangProvider
import lu.kolja.expandedgt.lang.ExpGuiText
import lu.kolja.expandedgt.lang.ExpTooltips

object ExpLangProvider {
    fun addTranslations(provider: RegistrateLangProvider) {
        fun <T> addEnum(enumClass: Class<T>) where T : Enum<T>, T : LocalizationEnum {
            enumClass.enumConstants.forEach {
                provider.add(it.translationKey, it.englishText)
            }
        }

        addEnum(ExpTooltips::class.java)
        addEnum(ExpGuiText::class.java)
    }
}