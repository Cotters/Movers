package com.jcotters.details.ui

import java.util.Locale

internal actual fun languageName(code: String): String =
    Locale.forLanguageTag(code).getDisplayLanguage(Locale.getDefault()).ifBlank { code }
