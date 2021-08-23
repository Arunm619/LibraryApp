package io.arunbuilds.libraryapp

import com.google.gson.JsonSyntaxException
import io.arunbuilds.libraryapp.data.Library
import io.arunbuilds.libraryapp.utils.InvalidLibraryDataException
import io.arunbuilds.libraryapp.utils.ValidationsUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ValidationsUtilsTest {

    @Test
    fun `cleanUpRawStringToJSONFormat - expect proper json formated string as the given raw string from scanning is valid`() {

        val expected = """
            {"location_id":"ButterKnifeLib-1234","location_details":"ButterKnife Lib, 80 Feet Rd, Koramangala 1A Block, Bangalore","price_per_min":5.50}
        """.trimIndent()
        assertEquals(
            expected,
            ValidationsUtils.cleanUpRawStringToJSONFormat(VALID_RAW_SCANNED_INPUT)
        )
    }

    @Test
    fun `cleanUpRawStringToJSONFormat - expect error as the given raw string from scanning is invalid`() {
        assertThrows(InvalidLibraryDataException::class.java) {
            ValidationsUtils.cleanUpRawStringToJSONFormat(INVALID_RAW_SCANNED_INPUT_EMPTY)
        }
    }

    @Test
    fun `isValidLibraryData - test if proper qr scanned string is properly transformed`() {
        val expected = Pair(
            true,
            Library(
                "ButterKnife Lib, 80 Feet Rd, Koramangala 1A Block, Bangalore",
                "ButterKnifeLib-1234",
                5.50F

            )
        )
        assertEquals(expected, ValidationsUtils.isValidLibraryData(VALID_RAW_SCANNED_INPUT))
    }

    @Test
    fun `isValidLibraryData - test if malformed qr scanned string gives empty result 1`() {
        val expected = Pair(
            false,
            null
        )
        assertEquals(
            expected,
            ValidationsUtils.isValidLibraryData(
                INVALID_RAW_SCANNED_INPUT_MALFORMED
            )
        )
    }

    @Test
    fun `isValidLibraryData - test if malformed qr scanned string gives empty result 2`() {
        val expected = Pair(
            false,
            null
        )
        assertEquals(expected, ValidationsUtils.isValidLibraryData(INVALID_RAW_SCANNED_INPUT_EMPTY))
    }

    @Test
    fun `unmarshallJsonStringToJsonObject - test if correct JSON string properly gets created as Library`() {
        val expected = Library(
            "ButterKnife Lib, 80 Feet Rd, Koramangala 1A Block, Bangalore",
            "ButterKnifeLib-1234",
            5.50F
        )
        assertEquals(
            expected,
            ValidationsUtils.unmarshallJsonStringToJsonObject(VALID_CLEANED_UP_JSON_INPUT)
        )
    }

    @Test(expected = JsonSyntaxException::class)
    fun `test if malformed JSON string throws error`() {
        val expected = Library(
            "ButterKnife Lib, 80 Feet Rd, Koramangala 1A Block, Bangalore",
            "ButterKnifeLib-1234",
            5.50F
        )
        ValidationsUtils.unmarshallJsonStringToJsonObject(INVALID_RAW_SCANNED_INPUT_MALFORMED)
    }

    companion object {
        val VALID_RAW_SCANNED_INPUT = """
            "{\"location_id\":\"ButterKnifeLib-1234\",\"location_details\":\"ButterKnife Lib, 80 Feet Rd, Koramangala 1A Block, Bangalore\",\"price_per_min\":5.50}"
        """.trimIndent()
        val INVALID_RAW_SCANNED_INPUT_MALFORMED = """
            {\"location_id\":\"ButterKnifeLib-1234\",\"location_details\":\"ButterKnife Lib, 80 Feet Rd, Koramangala 1A Block, Bangalore\",\"price_per_min\":5.50}"
        """.trimIndent()
        const val INVALID_RAW_SCANNED_INPUT_EMPTY = ""
        val VALID_CLEANED_UP_JSON_INPUT = """
            {"location_id":"ButterKnifeLib-1234","location_details":"ButterKnife Lib, 80 Feet Rd, Koramangala 1A Block, Bangalore","price_per_min":5.50}
        """.trimIndent()
    }
}
