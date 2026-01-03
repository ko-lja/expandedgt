package lu.kolja.expandedgt.lang

import appeng.core.localization.LocalizationEnum

enum class ExpGuiText(var text: String) : LocalizationEnum {
    CreativeTab("Expanded GT");

    override fun getTranslationKey() = "gui.expandedgt.$name"

    override fun getEnglishText() = text
}