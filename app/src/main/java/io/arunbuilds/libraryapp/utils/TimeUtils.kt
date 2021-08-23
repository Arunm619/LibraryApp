package io.arunbuilds.libraryapp.utils

object TimeUtils {
    private const val TIME_STAMP_HH_MM_SS = "%02d:%02d:%02d"

    fun getTimeStampFromElapsedSeconds(givenTimeinSeconds: Long): String {
        val hour = givenTimeinSeconds / 3600
        val mins = (givenTimeinSeconds % 3600) / 60
        val seconds = givenTimeinSeconds % 60
        return String.format(
            TIME_STAMP_HH_MM_SS,
            hour,
            mins,
            seconds
        )
    }
}
