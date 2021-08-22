package io.arunbuilds.libraryapp.ui

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import io.arunbuilds.libraryapp.data.Library
import timber.log.Timber
import javax.inject.Inject

class SessionController @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)

    private var _librarySessionData: String
        get() = prefs.getString(SESSION_ID_KEY, EMPTY) ?: EMPTY
        set(value) = prefs.edit().putString(SESSION_ID_KEY, value).apply()

    private var _sessionStartTimeStamp: Long
        get() = prefs.getLong(SESSION_START_TIME_STAMP_KEY, 0)
        set(value) = prefs.edit().putLong(SESSION_START_TIME_STAMP_KEY, value).apply()


    fun isSessionActive(): Boolean {
        return isValidSessionQR(getCurrentSession()).also {
            Timber.d(" Is Valid Session? $it ")
        }
    }

    /*
    * Stores the JSON String of the Library object.
    * */
    fun setCurrentSession(data: Library) {
        val gson = Gson()
        val json = gson.toJson(data)
        _librarySessionData = json.also {
            Timber.d(" Setting current session as $it ")
        }
    }

    /*
    * Returns the JSON string for the persisted Library info.
    * */
    fun getCurrentSession(): String {
        return _librarySessionData.also {
            Timber.d(" Current session returned is $it ")
        }
    }

    /*
    * Clears the stored session details
    * Library data & start time stamp
    * */
    fun clearCurrentSession() {
        Timber.d("Clearing current session ${getCurrentSession()}")
        _librarySessionData = EMPTY
        _sessionStartTimeStamp = 0
    }


    /*
    * Stores the JSON String of the Library object.
    * */
    fun setSessionStartTimeStamp(startTimeStamp: Long) {
        _sessionStartTimeStamp = startTimeStamp.also {
            Timber.d(" Setting start time stamp as  $it")
        }
    }


    /*
    * Returns the session start time for the current session.
    * */
    fun getSessionStartTimeStamp(): Long {
        return _sessionStartTimeStamp.also {
            Timber.d(" Returning the session start timestamp as $it")
        }
    }


    private fun isValidSessionQR(libraryInfo: String): Boolean {
        return libraryInfo != EMPTY
    }

    companion object {
        /**
         *  Name of the preferences file.
         * */
        const val PREFS_FILENAME = "io.arunbuilds.test.PREF_FILE"

        /**
         * Name of the key to store the session
         * */
        private const val SESSION_ID_KEY = "SESSION_ID_KEY"

        /**
         * Name of the key to store the start time stamp for the session
         * */
        private const val SESSION_START_TIME_STAMP_KEY = "SESSION_START_TIME_STAMP_KEY"

        private const val EMPTY = "EMPTY"
    }
}

