package com.example.advancedmobilebitcointracker

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.advancedmobilebitcointracker.databinding.ActivityMainBinding
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Navigation
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.navView.setupWithNavController(navController)

        // Check for Biometric Lock
        val sharedPrefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val isLockEnabled = sharedPrefs.getBoolean("biometric_enabled", false)

        if (isLockEnabled) {
            setupBiometrics()
            authenticateUser()
        }
    }

    private fun setupBiometrics() {
        // Initially hide the app content to prevent peeking
        binding.navHostFragment.visibility = View.INVISIBLE
        binding.navView.visibility = View.INVISIBLE

        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    // Show app content on success
                    binding.navHostFragment.visibility = View.VISIBLE
                    binding.navView.visibility = View.VISIBLE
                    Toast.makeText(applicationContext, "Unlocked!", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Authentication failed", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // If they cancel or fail too many times, close the app
                    if (errorCode != BiometricPrompt.ERROR_NO_BIOMETRICS && errorCode != BiometricPrompt.ERROR_HW_UNAVAILABLE) {
                        finish()
                    } else {
                        // If no hardware, just let them in
                        binding.navHostFragment.visibility = View.VISIBLE
                        binding.navView.visibility = View.VISIBLE
                    }
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Login")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Exit App") // Mandatory button
            .build()
    }

    private fun authenticateUser() {
        biometricPrompt.authenticate(promptInfo)
    }
}