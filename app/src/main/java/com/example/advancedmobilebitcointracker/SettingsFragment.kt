package com.example.advancedmobilebitcointracker

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.advancedmobilebitcointracker.databinding.FragmentSettingsBinding
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.TimeUnit

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedPreferences: SharedPreferences

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            scheduleBackgroundWork()
        } else {
            binding.switchNotifications.isChecked = false
            Toast.makeText(context, "Notifications permission required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

        loadSettings()

        binding.btnSaveProfile.setOnClickListener { saveProfile() }

        // --- NEW: LOGOUT LOGIC ---
        binding.btnLogout.setOnClickListener {
            // 1. Sign out from Firebase
            FirebaseAuth.getInstance().signOut()

            // 2. Clear local session data (Optional but good practice)
            // sharedPreferences.edit().clear().apply()

            // 3. Navigate back to Login
            Toast.makeText(context, "Logged Out", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_settings_to_login)
        }
        // -------------------------

        // Switch Logic
        binding.switchBiometric.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("biometric_enabled", isChecked).apply()
            val status = if (isChecked) "Enabled" else "Disabled"
            Toast.makeText(context, "App Lock $status", Toast.LENGTH_SHORT).show()
        }

        binding.switchShake.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("shake_enabled", isChecked).apply()
        }

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("notifications_enabled", isChecked).apply()
            if (isChecked) {
                checkPermissionAndSchedule()
            } else {
                cancelBackgroundWork()
            }
        }
    }

    private fun checkPermissionAndSchedule() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED) {
                scheduleBackgroundWork()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            scheduleBackgroundWork()
        }
    }

    private fun scheduleBackgroundWork() {
        val workRequest = PeriodicWorkRequestBuilder<PriceAlertWorker>(15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
            "BitcoinPriceAlert",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
        Toast.makeText(context, "Background Alerts Enabled", Toast.LENGTH_SHORT).show()
    }

    private fun cancelBackgroundWork() {
        WorkManager.getInstance(requireContext()).cancelUniqueWork("BitcoinPriceAlert")
        Toast.makeText(context, "Background Alerts Disabled", Toast.LENGTH_SHORT).show()
    }

    private fun loadSettings() {
        binding.etName.setText(sharedPreferences.getString("user_name", ""))
        binding.etBio.setText(sharedPreferences.getString("user_bio", ""))

        binding.switchBiometric.isChecked = sharedPreferences.getBoolean("biometric_enabled", false)
        binding.switchShake.isChecked = sharedPreferences.getBoolean("shake_enabled", true)
        binding.switchNotifications.isChecked = sharedPreferences.getBoolean("notifications_enabled", false)
    }

    private fun saveProfile() {
        val editor = sharedPreferences.edit()
        editor.putString("user_name", binding.etName.text.toString())
        editor.putString("user_bio", binding.etBio.text.toString())
        editor.apply()
        Toast.makeText(context, "Profile Saved", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}