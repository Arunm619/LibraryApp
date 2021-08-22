package io.arunbuilds.libraryapp.ui.welcome

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import io.arunbuilds.libraryapp.ui.SessionController
import io.arunbuilds.libraryapp.utils.SingleEvent
import io.arunbuilds.libraryapp.data.Library
import java.util.*
import javax.inject.Inject

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val sessionController: SessionController
) : ViewModel() {

    private val _scanClick = MutableLiveData<SingleEvent<Action.TapScan>>()
    val scanClick: LiveData<SingleEvent<Action.TapScan>> get() = _scanClick

    private val _events = MutableLiveData<SingleEvent<Event>>()
    val events: LiveData<SingleEvent<Event>> get() = _events


    fun onScanButtonClicked() {
        processScanButtonClicked()
    }

    /*
    * Currently No-Op.
    * */
    fun onCancelScan() {
        processCancelScan()
    }

    fun onQRCodeScanDone(qrContentString: String?) {
        processQRCodeScanDone(qrContentString)
    }

    private fun processQRCodeScanDone(qrContentString: String?) {
        // 1. validate
        // 2. clean up
        val (isValid, libraryInfo) = isValidLibraryData(qrContentString)
        if (isValid && libraryInfo != null) {
            processSuccessfulLibraryQRScanned(libraryInfo)
        } else {
            processInvalidQRScanned()
        }
    }

    private fun processInvalidQRScanned() {
        val errorMessage =
            "Invalid QR Code has been scanned, Please check with librarian for further assistance"
        _events.value = SingleEvent(Event.ShowSnackBarEvent(errorMessage))
    }

    private fun processSuccessfulLibraryQRScanned(libraryInfo: Library) {
        _events.value = SingleEvent(Event.ShowSnackBarEvent(libraryInfo))
    }

    private fun processCancelScan() {
        //no- op
    }

    private fun processScanButtonClicked() {
        _scanClick.value = SingleEvent(Action.TapScan)
    }

    fun onStartSessionClicked(libraryInfo: Library) {
        processStartSession(libraryInfo)
    }

    private fun processStartSession(libraryInfo: Library) {
        // 3. persist session
        //4 . take to main activity
        // user tapped on start session
        // take note of session
        sessionController.setCurrentSession(libraryInfo)
        sessionController.setSessionStartTimeStamp(Date().time)
        launchHomeActivity()
    }

    private fun launchHomeActivity() {
        _events.value = SingleEvent(Event.LaunchHomeActivity)
    }

    /*
    * All the init logic goes here.
    * */
    fun initialise() {
        if (sessionController.isSessionActive()) {
            launchHomeActivity()
        }
    }


    sealed class Action {
        object TapScan : Action()
    }

    sealed class Event {
        class ShowSnackBarEvent(val data: Any) : Event()
        object LaunchHomeActivity : Event()
    }

    companion object {

        /*
        * This method is responsible for checking if the scanned raw data from the QR code is a valid Library information.
        * Valid library means @Library class fields all should not be null and empty.
        * Returns Boolean for validity and if valid constructs Library object as well.
        * */
        fun isValidLibraryData(scannedRawQRData: String?): Pair<Boolean, Library?> {

            if (scannedRawQRData.isNullOrEmpty()) return Pair(false, null)

            // prepare for unmarshalling into Library object
            val data = prepareForUnMarshalling(scannedRawQRData)

            return try {
                // Construct Library Object
                val libraryInfo = Gson().fromJson(data, Library::class.java)
                //Perform null and empty checks
                val isValid = libraryInfo.location_details.isNullOrEmpty()
                    .not() && libraryInfo.location_id.isNullOrEmpty().not()
                        && libraryInfo.price_per_min != null

                Pair(isValid, libraryInfo)
            } catch (e: Exception) {
                Pair(false, null)
            }

        }

        /*
        * Clean up the raw string to JSON format.
        * */
        private fun prepareForUnMarshalling(toCleanup: String): String {
            var data = toCleanup
            data = data.replace("\\", "")
            data = data.removeRange(0, 1)
            if (data.startsWith("\"")) {
                data = data.substring(1)
            }
            if (data.endsWith("\"")) {
                data = data.substring(0, data.length - 1)
            }
            return data
        }
    }
}
