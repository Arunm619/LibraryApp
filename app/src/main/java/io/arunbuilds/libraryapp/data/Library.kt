package io.arunbuilds.libraryapp.data

/**
 * Data class for the current Library that the user has visited.
 * */
data class Library(
    val location_details: String? = null,
    val location_id: String? = null,
    val price_per_min: Float? = null,
) {
    override fun toString(): String {
        return """
            Location - $location_id
            Address- $location_details
            Price Per Min - ₹$price_per_min
            """.trimIndent()
    }
}