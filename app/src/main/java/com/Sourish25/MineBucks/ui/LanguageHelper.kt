package com.Sourish25.MineBucks.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LanguageHelper {
    // Supported Languages: Code -> Display Name
    val supportedLanguages = mapOf(
        "en" to "English",
        "ar" to "Arabic (العربية)",
        "zh" to "Chinese (中文)",
        "kw" to "Cornish (Kernewek)",
        "cs" to "Czech (Čeština)",
        "nl" to "Dutch (Nederlands)",
        "tl" to "Filipino (Tagalog)",
        "fr" to "French (Français)",
        "de" to "German (Deutsch)",
        "el" to "Greek (Ελληνικά)",
        "he" to "Hebrew (עברית)",
        "hi" to "Hindi (हिन्दी)",
        "it" to "Italian (Italiano)",
        "ja" to "Japanese (日本語)",
        "kk" to "Kazakh (Қазақша)",
        "ko" to "Korean (한국어)",
        "pl" to "Polish (Polski)",
        "pt" to "Portuguese (Português)",
        "ru" to "Russian (Русский)",
        "es" to "Spanish (Español)",
        "vi" to "Vietnamese (Tiếng Việt)"
    )

    fun setLanguage(languageCode: String) {
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    fun getCurrentLanguage(): String {
        return AppCompatDelegate.getApplicationLocales().toLanguageTags().split(",").firstOrNull() ?: "en"
    }
}
