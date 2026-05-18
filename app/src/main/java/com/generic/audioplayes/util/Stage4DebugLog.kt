package com.generic.audioplayes.util

import timber.log.Timber

/**
 * Filter logcat: `adb logcat -s Stage4Fix`
 */
object Stage4DebugLog {
    private const val TAG = "Stage4Fix"

    fun i(message: String) {
        Timber.tag(TAG).i(message)
    }

    fun w(message: String) {
        Timber.tag(TAG).w(message)
    }

    fun e(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Timber.tag(TAG).e(throwable, message)
        } else {
            Timber.tag(TAG).e(message)
        }
    }
}
