package com.generic.audioplayes.util

import java.io.File

/** Paths that may match Room / MediaStore when the stored string differs slightly. */
fun pathCandidatesForLookup(filePath: String): List<String> = buildList {
    add(filePath)
    try {
        add(File(filePath).canonicalPath)
    } catch (_: Exception) {
    }
    try {
        add(File(filePath).absolutePath)
    } catch (_: Exception) {
    }
}.distinct()
