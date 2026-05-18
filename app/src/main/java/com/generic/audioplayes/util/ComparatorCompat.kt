package com.generic.audioplayes.util

/** [Comparator.reversed] requires API 24; minSdk is 23. */
fun <T> Comparator<T>.reversedCompat(): Comparator<T> =
    Comparator { a, b -> compare(b, a) }
