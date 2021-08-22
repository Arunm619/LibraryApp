package io.arunbuilds.libraryapp.ui.welcome

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import io.arunbuilds.libraryapp.ui.main.SessionController
import io.arunbuilds.libraryapp.utils.SingleEvent
import io.arunbuilds.libraryapp.data.Library
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val sessionController: SessionController
) : ViewModel() {

    private val _actions = MutableLiveData<SingleEvent<Action>>()
    val actions: LiveData<SingleEvent<Action>> get() = _actions

    private val _events = MutableLiveData<SingleEvent<Event>>()
    val events: LiveData<SingleEvent<Event>> get() = _events


    /**
     * Tapped on Scan Button
     * */
    fun onScanButtonClicked() {
        processScanButtonClicked()
    }

    /**
     * Cancelled from Scanning QR code
     * */
    fun onCancelScan() {
        //no-op
    }

    /**
     *QR code was successfully scanned
     * @param qrContentString - This is the scanned raw string
     * */
    fun onQRCodeScanDone(qrContentString: String?) {
        processQRCodeScanDone(qrContentString)
    }

    /**
     * Process the given QR code raw string to see if its valid.
     * Check for validity otherwise report as invalid QR.
     * */
    private fun processQRCodeScanDone(qrContentString: String?) {
        val (isValid, libraryInfo) = isValidLibraryData(qrContentString)
        if (isValid && libraryInfo != null) {
            processSuccessfulLibraryQRScanned(libraryInfo)
        } else {
            processInvalidQRScanned()
        }
    }

    /**
     * Process Invalid QR code, send an erorr in Snackbar reporting the same to the user.
     * */
    private fun processInvalidQRScanned() {
        val errorMessage =
            "Invalid QR Code has been scanned, Please check with librarian for further assistance"
        _events.value = SingleEvent(Event.ShowSnackBarEvent(errorMessage))
    }

    /**
     * Process Valid QR code, show the user a snackbar asking for confirmation if they wish to start the session.
     * */
    private fun processSuccessfulLibraryQRScanned(libraryInfo: Library) {
        _events.value = SingleEvent(Event.ShowSnackBarEvent(libraryInfo))
    }

    /**
     * Process the user tap action on the scan button.
     * */
    private fun processScanButtonClicked() {
        _actions.value = SingleEvent(Action.TapScanAction)
    }

    /**
     * User tapped on Start Session
     * */
    fun onStartSessionClicked(libraryInfo: Library) {
        processStartSession(libraryInfo)
    }

    /**
     * Process the session initialisation by capturing the start time stamp and launching the home activity.
     * */
    private fun processStartSession(libraryInfo: Library) {
        sessionController.setCurrentSession(libraryInfo)
        sessionController.setSessionStartTimeStamp(Date().time)
        launchHomeActivity()
    }

    private fun launchHomeActivity() {
        _events.value = SingleEvent(Event.LaunchHomeActivityEvent)
    }

    /*
    * All the init logic goes here.
    * */
    fun initialise() {
        if (sessionController.isSessionActive()) {
            launchHomeActivity()
        }
    }


    /*
    * User Actions
    * */
    sealed class Action {
        object TapScanAction : Action()
    }

    /*
    * Different possible events that can happen in this screen.
    * */
    sealed class Event {
        class ShowSnackBarEvent(val data: Any) : Event()
        object LaunchHomeActivityEvent : Event()
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
