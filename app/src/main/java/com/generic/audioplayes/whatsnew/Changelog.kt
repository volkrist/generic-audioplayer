package com.generic.audioplayes.whatsnew

data class Changelog(
    val versionCode: Int,
    val versionName: String,
    val changes: List<String>,
    val date: String,
)