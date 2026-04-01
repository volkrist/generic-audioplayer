package com.generic.audioplayes.data.music

sealed class ScanStatus {
    object ScanStarted: ScanStatus()
    data class ScanProgress(val parsed: Int, val total: Int): ScanStatus()
    object ScanComplete: ScanStatus()
    object ScanNotRunning: ScanStatus()
}
