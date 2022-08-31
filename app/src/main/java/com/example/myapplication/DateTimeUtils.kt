package com.example.myapplication

import java.text.SimpleDateFormat
import java.util.*

object DateTimeUtils {
    fun now(): String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
}