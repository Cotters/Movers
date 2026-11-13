package com.jcotters.presentation

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.dateWithTimeIntervalSince1970

private const val MILLISECONDS_PER_SECOND = 1000.0

actual fun formatActivityTime(millis: Long): String = NSDateFormatter().apply {
    dateStyle = NSDateFormatterMediumStyle; timeStyle = NSDateFormatterShortStyle
}.stringFromDate(NSDate.dateWithTimeIntervalSince1970(millis / MILLISECONDS_PER_SECOND))

actual fun formatActivityDay(millis: Long): String = NSDateFormatter().apply {
    dateStyle = NSDateFormatterMediumStyle; timeStyle = NSDateFormatterNoStyle
}.stringFromDate(NSDate.dateWithTimeIntervalSince1970(millis / MILLISECONDS_PER_SECOND))
