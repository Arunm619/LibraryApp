package io.arunbuilds.libraryapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.integration.android.IntentIntegrator
import dagger.hilt.android.AndroidEntryPoint
import io.arunbuilds.libraryapp.databinding.ActivityWelcomeBinding
import timber.log.Timber

@AndroidEntryPoint
class WelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeBinding
    private val welcomeViewModel: WelcomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnScan.setOnClickListener {
            welcomeViewModel.onScanButtonClicked()
        }

        welcomeViewModel.scanClick
            .observe(this) { event ->
                event.getContentIfNotHandled()?.let {
                    launchQRCodeScannerActivity()
                }
            }

        welcomeViewModel.events
            .observe(this) { event ->
                event.getContentIfNotHandled()?.let { action ->
                    when (action) {
                        is WelcomeViewModel.Event.ShowSnackBarEvent -> {
                            if (action.data is String) {
                                showSnackBar(action.data)
                            }
                            if (action.data is Library) {
                                showSnackBar(action.data)
                            }
                        }
                        is WelcomeViewModel.Event.LaunchHomeActivity -> {
                            launchHomeActivity()
                        }
                    }
                }
            }
    }

    private fun launchHomeActivity() {
        Timber.i("Launching Home Activity")
        //startActivity(HomeActivity.getIntent(this))
        //finish()
    }

    private fun launchQRCodeScannerActivity() {
        IntentIntegrator(this).apply {
            setOrientationLocked(false)
            setPrompt("Scan QR Code")
            initiateScan()
        }
    }

    // Get the results:
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                // User pressed on cancel button from QR code scanner activity
                welcomeViewModel.onCancelScan()
                Timber.i("User cancelled the QR code scanning process.")
            } else {
                val qrContentString = result.contents
                welcomeViewModel.onQRCodeScanDone(qrContentString)
                Timber.i("User scanned a QR code data. Sending for processing.")
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
            is Library -> {
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "Hey you are at ${content.location_id}",
                    Snackbar.LENGTH_INDEFINITE
                ).setAction("Start Session") {
                    welcomeViewModel.onStartSessionClicked(content)
                }.show()
            }
        }

    }


}