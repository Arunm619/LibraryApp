package io.arunbuilds.libraryapp.ui.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.ServiceConnection
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.IBinder
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.integration.android.IntentIntegrator
import dagger.hilt.android.AndroidEntryPoint
import io.arunbuilds.libraryapp.Constants
import io.arunbuilds.libraryapp.data.SessionDetails
import io.arunbuilds.libraryapp.databinding.ActivityHomeBinding
import io.arunbuilds.libraryapp.service.LibraryTimerService
import io.arunbuilds.libraryapp.utils.Resource
import io.arunbuilds.libraryapp.ui.main.HomeViewModel.CONNECTION.*
import io.arunbuilds.libraryapp.ui.welcome.WelcomeActivity
import timber.log.Timber

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private val viewModel: HomeViewModel by viewModels()

    private val serviceIntent by lazy {
        Intent(this@HomeActivity, LibraryTimerService::class.java)
    }

    private lateinit var timerService: LibraryTimerService
    private val receiver: TickerTimeReceiver by lazy {
        TickerTimeReceiver()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.initialise()

        viewModel.serviceBound.observe(this) { isActive ->
            if (isActive) {
                //when the activity is destroyed, the service will unbind from it, still running in foreground.
                // When the app is restarted, it has to be re-bound.
                bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE)
            }
        }

        viewModel.libraryData.observe(this) { library ->
            binding.tvAppTitle.text = library.location_id
            binding.tvQRDetails.text = library.toString()
        }

        viewModel.chronoTime.observe(this) {
            binding.tvTimer.text = it
        }
        viewModel.actions.observe(this) { action ->
            action.getContentIfNotHandled()?.let {
                when (it) {
                    is Action.ConfirmExitSession -> {
                        launchQRCodeScannerActivity()
                    }
                    is Action.OkClicked -> {
                        exitActivityAndlaunchWelcomeActivity()
                    }
                    is Action.EndSessionButtonClicked -> {
                        displayConfirmationDialog()
                    }
                }
            }
        }

        viewModel.sessionEndedData.observe(this) { (isDataPresent, sessionDetail) ->
            if (isDataPresent && sessionDetail != null) {
                showSessionEndedView(sessionDetail)
            }
        }

        viewModel.submitSessionLoading.observe(this) {
            when (it) {
                is Resource.DataError -> {
                    binding.pbCircularLoading.visibility = View.GONE
                    binding.btnOk.visibility = View.VISIBLE
                    showSnackBar("Error Occurred while submitting the session to server")
                }
                is Resource.Loading -> {
                    binding.pbCircularLoading.visibility = View.VISIBLE
                    binding.btnOk.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.pbCircularLoading.visibility = View.GONE
                    binding.btnOk.visibility = View.VISIBLE
                }
            }
        }

        viewModel.events.observe(this) { event ->
            event.getContentIfNotHandled()?.let { content ->
                when (content) {
                    is Event.ShowSnackBarEvent -> {
                        showSnackBar(content.data)
                    }
                    is Event.SessionEnd -> {
                        stopTimerService()
                        showSessionEndedView(content.sessionDetails)
                    }
                    is Event.StartTimerService -> {
                        startTimerService()
                    }
                    is Event.StopTimerService -> {
                        stopTimerService()
                    }
                }
            }
        }


        binding.btnEndSession.setOnClickListener {
            viewModel.onEndSessionButtonClicked()
        }

        binding.btnOk.setOnClickListener {
            viewModel.onClickedOk()
        }
    }

    private fun displayConfirmationDialog() {
        val builder = AlertDialog.Builder(this@HomeActivity)
        builder.setMessage("Are you sure you want to end the session?")
            .setCancelable(false)
            .setPositiveButton("Yes") { dialog, id ->
                viewModel.onConfirmedExitSession()
            }
            .setNegativeButton("No") { dialog, id ->
                dialog.dismiss()
            }
        val alert = builder.create()
        alert.show()
    }

    private fun launchQRCodeScannerActivity() {
        IntentIntegrator(this).apply {
            setOrientationLocked(false)
            setPrompt("Scan QR Code")
            initiateScan()
        }
    }

    private fun showSessionEndedView(sessionDetails: SessionDetails) {

        binding.tvTotalMinsSpent.text = "Total Minutes Spent - ${sessionDetails.timeSpentInMins}"
        binding.tvTotalMinsSpent.visibility = View.VISIBLE

        binding.tvPrice.text = "Price $ ${sessionDetails.totalPrice}"
        binding.tvPrice.visibility = View.VISIBLE

        binding.btnEndSession.visibility = View.GONE
        binding.btnOk.visibility = View.GONE

    }

    private fun exitActivityAndlaunchWelcomeActivity() {
        startActivity(Intent(this, WelcomeActivity::class.java))
        finish()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onServiceConnectionChanged(UNKNOWN)
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(receiver, IntentFilter(Constants.ACTION_LIBRARY_APP_KEY_TIME))
    }

    private fun startTimerService() {
        startService(serviceIntent)
        bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun stopTimerService() {
        if (viewModel.serviceBound.value!!) {
            unbindService(mServiceConnection)
            viewModel.onServiceConnectionChanged(DISCONNECTED)
        }
        stopService(serviceIntent)
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
    }

    override fun onStop() {
        super.onStop()
        if (viewModel.serviceBound.value!!) {
            unbindService(mServiceConnection)
            viewModel.onServiceConnectionChanged(DISCONNECTED)
        }
    }

    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName) {
            // no-op
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val myBinder = service as LibraryTimerService.TimerBinder
            timerService = myBinder.service
            viewModel.onServiceConnectionChanged(CONNECTED)
        }
    }

    /**
     * Gets the time from the [LibraryTimerService] service.
     */
    inner class TickerTimeReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Constants.ACTION_LIBRARY_APP_KEY_TIME -> {
                    if (intent.hasExtra(Constants.ACTION_LIBRARY_APP_KEY_DATA)) {
                        val time = intent.getStringExtra(Constants.ACTION_LIBRARY_APP_KEY_DATA)
                        time?.let { viewModel.onTick(time) }
                    }
                }
            }
        }
    }

    // Get the results
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                // User exited from QR code scanner activity without completing.
                viewModel.onCancelScan()
                Timber.i("User cancelled the QR code scanning process.")
            } else {
                //Toast.makeText(this, "Scanned: " + result.contents, Toast.LENGTH_LONG).show()
                val qrContentString = result.contents
                viewModel.onQRCodeScanDone(qrContentString)
                Timber.i("User scanned a QR code data $qrContentString. Sending for processing.")
            }
        }
    }

    private fun showSnackBar(content: Any) {
        when (content) {
            is String -> {
                Snackbar.make(
                    findViewById(android.R.id.content),
                    content,
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, HomeActivity::class.java)
        }
    }
}