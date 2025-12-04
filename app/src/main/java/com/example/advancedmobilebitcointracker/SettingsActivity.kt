package com.example.advancedmobilebitcointracker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.advancedmobilebitcointracker.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
