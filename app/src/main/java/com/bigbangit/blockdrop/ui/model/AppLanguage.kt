package com.bigbangit.blockdrop.ui.model

data class AppLanguage(
    val tag: String,
    val displayName: String,
)

object AppLanguages {
    val Supported = listOf(
        AppLanguage("en", "English"),
        AppLanguage("ar", "العربية"),
        AppLanguage("cs", "Čeština"),
        AppLanguage("de", "Deutsch"),
        AppLanguage("es", "Español"),
        AppLanguage("fr", "Français"),
        AppLanguage("hi", "हिन्दी"),
        AppLanguage("id", "Bahasa Indonesia"),
        AppLanguage("it", "Italiano"),
        AppLanguage("ja", "日本語"),
        AppLanguage("ko", "한국어"),
        AppLanguage("pl", "Polski"),
        AppLanguage("pt-BR", "Português (Brasil)"),
        AppLanguage("ru", "Русский"),
        AppLanguage("sk", "Slovenčina"),
        AppLanguage("th", "ไทย"),
        AppLanguage("tr", "Türkçe"),
        AppLanguage("uk", "Українська"),
        AppLanguage("vi", "Tiếng Việt"),
        AppLanguage("zh-CN", "简体中文"),
        AppLanguage("zh-TW", "繁體中文"),
    )

    fun normalize(tag: String?): String? {
        val normalizedTag = tag?.takeIf { it.isNotBlank() } ?: return null
        return Supported.firstOrNull { it.tag == normalizedTag }?.tag
    }

    fun displayNameFor(tag: String?, systemDefaultLabel: String): String {
        return normalize(tag)
            ?.let { normalized -> Supported.firstOrNull { it.tag == normalized }?.displayName }
            ?: systemDefaultLabel
    }
}
