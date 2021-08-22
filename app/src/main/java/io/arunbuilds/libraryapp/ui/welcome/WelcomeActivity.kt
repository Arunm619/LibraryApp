package io.arunbuilds.libraryapp.ui.welcome

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.integration.android.IntentIntegrator
import dagger.hilt.android.AndroidEntryPoint
import io.arunbuilds.libraryapp.data.Library
import io.arunbuilds.libraryapp.databinding.ActivityWelcomeBinding
import io.arunbuilds.libraryapp.ui.main.HomeActivity
import timber.log.Timber

@AndroidEntryPoint
class WelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeBinding
    private val viewModel: WelcomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnScan.setOnClickListener {
            viewModel.onScanButtonClicked()
        }

        observerViewModels()
        viewModel.initialise()
    }

    private fun observerViewModels() {
        viewModel.actions
            .observe(this) { event ->
                event.getContentIfNotHandled()?.let { action ->
                    when (action) {
                        WelcomeViewModel.Action.TapScanAction -> {
                            launchQRCodeScannerActivity()
                        }
                    }

                }
            }
        viewModel.events
            .observe(this) {
                it.getContentIfNotHandled()?.let { event ->
                    when (event) {
                        is WelcomeViewModel.Event.ShowSnackBarEvent -> {
                            showSnackBar(event.data)
                        }
                        is WelcomeViewModel.Event.LaunchHomeActivityEvent -> {
                            launchHomeActivity()
                        }
                    }
                }
            }
    }

    private fun launchHomeActivity() {
        Timber.i("Launching Home Activity")
        startActivity(HomeActivity.getIntent(this))
        finish()
    }

    private fun launchQRCodeScannerActivity() {
        IntentIntegrator(this).apply {
            setOrientationLocked(false)
            setPrompt("Scan QR Code")
            initiateScan()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                viewModel.onCancelScan()
                Timber.i("User cancelled the QR code scanning process.")
            } else {
                val qrContentString = result.contents
                viewModel.onQRCodeScanDone(qrContentString)
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
                    viewModel.onStartSessionClicked(content)
                }.show()
            }
        }
    }

}