package com.jcotters.details.ui

import platform.Foundation.NSLocale
import platform.Foundation.NSLocaleLanguageCode
import platform.Foundation.currentLocale

internal actual fun languageName(code: String): String =
    NSLocale.currentLocale.displayNameForKey(NSLocaleLanguageCode, code) ?: code
