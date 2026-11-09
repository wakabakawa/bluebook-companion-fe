package com.achievo.bluebook.ui.components

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun mmss(totalSeconds: Int): String {
    val s = totalSeconds.coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

fun dateText(millis: Long): String =
    SimpleDateFormat("M/d/yyyy", Locale.getDefault()).format(Date(millis))
