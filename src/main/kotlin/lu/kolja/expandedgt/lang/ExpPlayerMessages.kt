package lu.kolja.expandedgt.lang

import appeng.core.localization.LocalizationEnum

enum class ExpPlayerMessages(var text: String): LocalizationEnum {
    PatternError("Expected [%s] at [%s]");

    override fun getTranslationKey() = "messages.expandedgt.$name"

    override fun getEnglishText() = text
}