package io.arunbuilds.libraryapp.data

/**
 * Details about the on-going session in the library.
 * */
data class SessionDetails(
    val locationId: String,
    val endTimeStamp: Long,
    val timeSpentInMins: Int,
    val totalPrice: Float,
)
