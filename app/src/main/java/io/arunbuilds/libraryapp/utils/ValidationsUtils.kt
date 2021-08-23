package io.arunbuilds.libraryapp.utils

import androidx.annotation.VisibleForTesting
import com.google.gson.Gson
import io.arunbuilds.libraryapp.data.Library
import org.json.JSONArray
import org.json.JSONObject

object ValidationsUtils {
    /*
       * This method is responsible for checking if the scanned raw data from the QR code is a valid Library information.
       * Valid library means @Library class fields all should not be null and empty.
       * Returns Boolean for validity and if valid constructs Library object as well.
       * */
    @VisibleForTesting
    fun isValidLibraryData(scannedRawQRData: String?): Pair<Boolean, Library?> {

        if (scannedRawQRData.isNullOrEmpty()) return Pair(false, null)
        return try {
            // prepare for unmarshalling into Library object
            val data = cleanUpRawStringToJSONFormat(scannedRawQRData)
            val library = unmarshallJsonStringToJsonObject(data)
            Pair(true, library)
        } catch (e: Exception) {
            Pair(false, null)
        }
    }

    /*
    * Clean up the raw string to JSON format or throws invalid error.
    * */
    @VisibleForTesting
    fun cleanUpRawStringToJSONFormat(toCleanup: String): String {
        return try {
            var data = toCleanup
            data = data.replace("\\", "")
            data = data.removeRange(0, 1)
            if (data.startsWith("\"")) {
                data = data.substring(1)
            }
            if (data.endsWith("\"")) {
                data = data.substring(0, data.length - 1)
            }
            data
        } catch (e: Exception) {
            throw InvalidLibraryDataException("Error preparing for unmarshalling : $e}")
        }
    }

    @VisibleForTesting
    fun unmarshallJsonStringToJsonObject(jsonString: String): Library {
        return try {
            // Construct Library Object
            val libraryInfo = Gson().fromJson(jsonString, Library::class.java)
            // Perform null and empty checks
            val isValid = libraryInfo.location_details.isNullOrEmpty()
                .not() && libraryInfo.location_id.isNullOrEmpty().not() &&
                libraryInfo.price_per_min != null
            if (isValid)
                libraryInfo
            else
                throw InvalidLibraryDataException("Not a valid library info")
        } catch (e: Exception) {
            throw e
        }
    }

    private fun isJSONValid(data: String): Boolean {
        var result: JSONObject? = null
        return try {
            result = JSONObject(data)
            true
        } catch (e: Exception) {
            result = null
            // edited, to include @Arthur's comment
            // e.g. in case JSONArray is valid as well...
            try {
                result = null
                JSONArray(data)
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}

class InvalidLibraryDataException(msg: String) : Exception(msg)
