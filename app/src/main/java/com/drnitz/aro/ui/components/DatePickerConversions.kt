package com.drnitz.aro.ui.components

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Material 3 DatePicker represents calendar days as UTC midnight.
 *
 * Keeping the conversion here ensures the picker and the rest of the app always agree on
 * the calendar day, even when the device is west or east of UTC.
 */
internal fun LocalDate.toDatePickerMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

internal fun datePickerMillisToLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
