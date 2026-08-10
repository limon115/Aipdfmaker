package com.example.utils

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    data class LogEntry(
        val timestamp: Long = System.currentTimeMillis(),
        val level: String,
        val tag: String,
        val message: String
    ) {
        val formattedTime: String
            get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(timestamp))
    }

    fun d(tag: String, message: String) {
        Log.d(tag, message)
        addLog("D", tag, message)
    }

    fun i(tag: String, message: String) {
        Log.i(tag, message)
        addLog("I", tag, message)
    }

    fun w(tag: String, message: String) {
        Log.w(tag, message)
        addLog("W", tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        val fullMessage = if (throwable != null) "$message\n${throwable.stackTraceToString()}" else message
        addLog("E", tag, fullMessage)
    }

    private fun addLog(level: String, tag: String, message: String) {
        _logs.update { current ->
            val newLogs = current + LogEntry(level = level, tag = tag, message = message)
            if (newLogs.size > 1000) newLogs.takeLast(1000) else newLogs
        }
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
