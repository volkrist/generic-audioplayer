package com.generic.audioplayes.data

import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject

class AudioPlayerCrashReporter @Inject constructor(
    private val firebase: FirebaseCrashlytics?,
) {

    fun logException(e: Exception?) {
        if (e != null) firebase?.recordException(e)
    }

    fun logData(message: String) {
        firebase?.log(message)
    }

    fun sendCrashData(reportData: Boolean) {
        firebase?.setCrashlyticsCollectionEnabled(reportData)
    }
}