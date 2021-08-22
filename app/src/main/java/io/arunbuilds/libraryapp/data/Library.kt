package io.arunbuilds.libraryapp.data

data class Library(
    val location_details: String? = null,
    val location_id: String? = null,
    val price_per_min: Float? = null,
) {
    override fun toString(): String {
        return """
            Location ID - $location_id
            Location Address - $location_details
            Price Per Min - $$price_per_min           
        """.trimIndent()
    }
}