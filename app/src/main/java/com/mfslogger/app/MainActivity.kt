package com.mfslogger.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.mfslogger.app.databinding.ActivityMainBinding
import com.mfslogger.app.network.SheetUploader
import com.mfslogger.app.parser.ParsedPayment
import com.mfslogger.app.ui.SenderListActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val requestPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val allGranted = grants.values.all { it }
        updatePermissionStatus()
        if (!allGranted) {
            Toast.makeText(
                this,
                "SMS permission is required for the app to detect payment messages",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.editScriptUrl.setText(Prefs.getScriptUrl(this))
        binding.editSecret.setText(Prefs.getSecret(this))
        updateSenderSummary()
        updatePermissionStatus()

        binding.btnGrantPermission.setOnClickListener {
            requestPermissionLauncher.launch(
                arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
            )
        }

        binding.btnManageSenders.setOnClickListener {
            startActivity(Intent(this, SenderListActivity::class.java))
        }

        binding.btnSaveConfig.setOnClickListener {
            Prefs.setScriptUrl(this, binding.editScriptUrl.text.toString().trim())
            Prefs.setSecret(this, binding.editSecret.text.toString().trim())
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
        }

        binding.btnSendTest.setOnClickListener {
            Prefs.setScriptUrl(this, binding.editScriptUrl.text.toString().trim())
            Prefs.setSecret(this, binding.editSecret.text.toString().trim())
            val test = ParsedPayment(
                payerNumber = "01700000000",
                transactionId = "TEST" + System.currentTimeMillis().toString().takeLast(6),
                amount = "1.00",
                rawMessage = "Test row sent from MFS Payment Logger",
                senderId = "TestSender"
            )
            SheetUploader.upload(this, test) { success, response ->
                runOnUiThread {
                    Toast.makeText(
                        this,
                        if (success) "Test row sent \u2014 check your sheet" else "Failed: $response",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateSenderSummary()
        updatePermissionStatus()
    }

    private fun updateSenderSummary() {
        val senders = Prefs.getWatchedSenders(this)
        binding.textSenderSummary.text = if (senders.isEmpty()) {
            "No senders selected yet \u2014 tap below to add e.g. \"bKash\" or \"Nagad\""
        } else {
            "Watching: " + senders.joinToString(", ")
        }
    }

    private fun updatePermissionStatus() {
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED
        binding.textPermissionStatus.text = if (granted) {
            "SMS permission: granted \u2705"
        } else {
            "SMS permission: not granted \u274c"
        }
        binding.btnGrantPermission.isEnabled = !granted
    }
}
