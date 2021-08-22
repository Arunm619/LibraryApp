package io.arunbuilds.libraryapp.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import io.arunbuilds.libraryapp.data.Library
import io.arunbuilds.libraryapp.data.SessionDetails
import io.arunbuilds.libraryapp.network.APIResult
import io.arunbuilds.libraryapp.network.RemoteRepository
import io.arunbuilds.libraryapp.service.LibraryTimerService
import io.arunbuilds.libraryapp.utils.Resource
import io.arunbuilds.libraryapp.utils.SingleEvent
import io.arunbuilds.libraryapp.utils.isServiceRunningInForeground
import io.arunbuilds.libraryapp.utils.rx.SchedulerProvider
import io.reactivex.rxjava3.core.SingleObserver
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import retrofit2.Response
import timber.log.Timber
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sessionController: SessionController,
    private val remoteRepository: RemoteRepository,
    application: Application,
    private val schedulerProvider: SchedulerProvider
) : AndroidViewModel(application) {

    val app = application
    private val compositeDisposable: CompositeDisposable = CompositeDisposable()

    /*
    * Current library details.
    * */
    private val _libraryData = MutableLiveData<Library>()
    val libraryData: LiveData<Library> get() = _libraryData

    private val _events = MutableLiveData<SingleEvent<Event>>()
    val events: LiveData<SingleEvent<Event>> get() = _events

    /*
    * Current Session Details - gets populated after the session is ended.
    * */
    private val _sessionEndedData = MutableLiveData<Pair<Boolean, SessionDetails?>>()
    val sessionEndedData: LiveData<Pair<Boolean, SessionDetails?>> get() = _sessionEndedData


    /*
    * Timer data that is displayed as a timer in the screen.
    * */
    private val _chronoTime = MutableLiveData<String>()
    val chronoTime: LiveData<String> get() = _chronoTime

    private val _actions = MutableLiveData<SingleEvent<Action>>()
    val actions: LiveData<SingleEvent<Action>> get() = _actions

    private val _serviceBound = MutableLiveData<Boolean>()
    val serviceBound: LiveData<Boolean> get() = _serviceBound

    private val _submitSessionLoading = MutableLiveData<Resource<Boolean>>()
    val submitSessionLoading: LiveData<Resource<Boolean>> get() = _submitSessionLoading

    fun initialise() {
        Timber.d("Initializing MainViewModel!")
        //Get the current session and store it in live data
        if (_libraryData.value == null) {
            fetchCurrentSessionfromController()
        }

        // Start the service only if the session is new
        if (_sessionEndedData.value == null)
            _events.value = SingleEvent(Event.StartTimerService)

        // initialise the session end data as not happened
        if (_sessionEndedData.value == null)
            _sessionEndedData.value = Pair(false, null)

        // check if service is running
        _serviceBound.value =
            app.isServiceRunningInForeground(LibraryTimerService::class.java).also {
                Timber.d("service is running? - $it")
            }
    }

    private fun fetchCurrentSessionfromController() {
        val libraryJSON = sessionController.getCurrentSession()
        val library = Gson().fromJson(libraryJSON, Library::class.java)
        _libraryData.value = library
    }

    fun onClickedOk() {
        processOkClicked()
    }

    /*
    * Clear the current Session and Exit the activity
    * */
    private fun processOkClicked() {
        sessionController.clearCurrentSession()
        _actions.value = SingleEvent(Action.OkClicked)
    }

    fun onCancelScan() {
        //no-op
    }

    fun onQRCodeScanDone(qrContentString: String?) {
        processQRCodeScanDone(qrContentString)
    }

    private fun processQRCodeScanDone(qrContentString: String?) {
        // 1. validate
        // 2. clean up
        // 3. Check for match with ongoing session
        val (isValid, scannedLibraryInfo) = isValidLibraryData(qrContentString)
        if (isValid && scannedLibraryInfo != null && isMatchWithOngoingSession(scannedLibraryInfo)) {
            processValidMatch(scannedLibraryInfo)
        } else {
            processInvalidQRScanned()
        }
    }

    private fun processInvalidQRScanned() {
        _events.value =
            SingleEvent(Event.ShowSnackBarEvent("Invalid QR Code scanned, Please Scan the one at ${libraryData.value?.location_id}"))
    }

    /**
     * 1. Stop the timer service
     * 2. Calculate and display the session details.
     * 3. Submit the session to server
     * */
    private fun processValidMatch(libraryInfo: Library) {

        //Stop the timer service
        _events.value = SingleEvent(Event.StopTimerService)

        val (locationId, endTimeStamp, minutes, _) =
            processEndSessionCalculationAndDisplayResults(libraryInfo)

        // submit the session to the server

        val singleObserver = object : SingleObserver<Response<APIResult>> {
            override fun onSubscribe(d: Disposable?) {
                Timber.d("Subscribed " + d.toString())
                _submitSessionLoading.value = Resource.Loading()
                d?.let {
                    compositeDisposable.add(it)
                }
            }

            override fun onSuccess(t: Response<APIResult>?) {
                Timber.d(
                    "Success ${t.toString()}\n" +
                            "Body - ${t?.body()} "
                )
                _submitSessionLoading.value = Resource.Success(t?.body()?.success ?: false)
            }

            override fun onError(e: Throwable?) {
                Timber.d("Error ${e.toString()}")
                _submitSessionLoading.value = Resource.DataError(-1)
            }
        }

        remoteRepository.submitSessionAsFields(locationId, minutes, endTimeStamp)
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribe(singleObserver)
        /*
               remoteRepository.submitSessionAsBody(PostBody(locationId, minutes, endTimeStamp))
                   .subscribeOn(schedulerProvider.io())
                   .observeOn(schedulerProvider.ui())
                   .subscribe(singleObserver)
        */
    }

    private fun processEndSessionCalculationAndDisplayResults(libraryInfo: Library): SessionDetails {
        // Returns the number of milliseconds since January 1, 1970, 00:00:00 GMT represented by this Date object.
        val endTimeStamp = Date().time

        val startTimeStamp = sessionController.getSessionStartTimeStamp()
        val diffInMs = (endTimeStamp - startTimeStamp)

        // formula for conversion for
        // milliseconds to minutes.
        val minutes: Long = diffInMs / 1000 / 60
        val totalPrice = minutes * libraryInfo.price_per_min!!
        val locationId = libraryInfo.location_id!!


        val result = SessionDetails(
            locationId,
            endTimeStamp,
            minutes.toInt(),
            totalPrice
        )
        // Display the data
        _events.value = SingleEvent(
            Event.SessionEnd(
                result
            )
        )
        _sessionEndedData.value = Pair(true, result)
        return SessionDetails(locationId, endTimeStamp, minutes.toInt(), totalPrice)
    }

    private fun isMatchWithOngoingSession(scannedlibraryInfo: Library): Boolean {
        libraryData.value?.let {
            return scannedlibraryInfo == it
        }
        return false
    }

    fun onServiceConnectionChanged(serviceConnection: CONNECTION) {
        Timber.d("Service Connection Changed $serviceConnection")
        when (serviceConnection) {
            CONNECTION.CONNECTED -> {
                _serviceBound.value = true
            }
            CONNECTION.DISCONNECTED -> {
                _serviceBound.value = false
            }
            CONNECTION.UNKNOWN -> {
                _serviceBound.value =
                    app.isServiceRunningInForeground(LibraryTimerService::class.java)
            }
        }
    }

    fun onConfirmedExitSession() {
        processConfirmedExitSession()
    }

    private fun processConfirmedExitSession() {
        _actions.value = SingleEvent(Action.ConfirmExitSession)
    }

    fun onTick(time: String) {
        _chronoTime.value = time
    }

    fun onEndSessionButtonClicked() {
        processEndSessionButtonClicked()
    }

    private fun processEndSessionButtonClicked() {
        _actions.value = SingleEvent(Action.EndSessionButtonClicked)
    }

    /**
     * Different possibilities of Service connection states.
     * */
    enum class CONNECTION {
        CONNECTED,
        DISCONNECTED,
        UNKNOWN
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

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.dispose()
    }
}

sealed class Action {
    object OkClicked : Action()
    object ConfirmExitSession : Action()
    object EndSessionButtonClicked : Action()
}

sealed class Event {
    class ShowSnackBarEvent(val data: Any) : Event()
    class SessionEnd(val sessionDetails: SessionDetails) : Event()
    object StopTimerService : Event()
    object StartTimerService : Event()
}