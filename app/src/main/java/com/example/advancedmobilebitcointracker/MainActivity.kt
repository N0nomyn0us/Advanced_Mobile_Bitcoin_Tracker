package com.example.advancedmobilebitcointracker // Make sure this matches your package name!

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.advancedmobilebitcointracker.databinding.ActivityMainBinding
import org.json.JSONException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    // Declare the binding object. It's name is generated from your layout file name: activity_main.xml -> ActivityMainBinding
    private lateinit var binding: ActivityMainBinding

    // The URL for the CoinGecko API to get Bitcoin prices
    private val apiUrl = "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin&vs_currencies=usd,gbp,eur"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate the layout and set up binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set an OnClickListener for the refresh button using the binding object
        binding.refreshButton.setOnClickListener {
            fetchBitcoinPrice()
        }

        // Fetch the price for the first time when the app opens
        fetchBitcoinPrice()
    }

    private fun fetchBitcoinPrice() {
        // Access UI elements directly through the binding object
        binding.loadingSpinner.visibility = View.VISIBLE
        binding.refreshButton.isEnabled = false

        // Create a new Volley request queue
        val queue = Volley.newRequestQueue(this)

        // Create the JSON object request
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, apiUrl, null,
            { response ->
                try {
                    // Navigate through the JSON response to get the price data
                    val bitcoinObject = response.getJSONObject("bitcoin")
                    val priceUsd = bitcoinObject.getDouble("usd")
                    val priceGbp = bitcoinObject.getDouble("gbp")
                    val priceEur = bitcoinObject.getDouble("eur")

                    // Format the prices and set them to the TextViews
                    binding.priceUsdText.text = String.format("$%,.2f", priceUsd)
                    binding.priceGbpText.text = String.format("£%,.2f", priceGbp)
                    binding.priceEurText.text = String.format("€%,.2f", priceEur)

                    // Update the "Last updated" text with the current time
                    updateTimestamp()

                } catch (e: JSONException) {
                    // Handle cases where the JSON is not as expected
                    Log.e("API_ERROR", "JSON parsing error: ${e.message}")
                    Toast.makeText(this, "Error parsing data", Toast.LENGTH_SHORT).show()
                } finally {
                    // Hide the spinner and re-enable the button
                    binding.loadingSpinner.visibility = View.GONE
                    binding.refreshButton.isEnabled = true
                }
            },
            { error ->
                // Handle network errors
                Log.e("API_ERROR", "Volley error: ${error.message}")
                Toast.makeText(this, "Error fetching data", Toast.LENGTH_SHORT).show()
                // Hide the spinner and re-enable the button even if there's an error
                binding.loadingSpinner.visibility = View.GONE
                binding.refreshButton.isEnabled = true
            }
        )

        // Add the request to the queue to execute it
        queue.add(jsonObjectRequest)
    }

    private fun updateTimestamp() {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val currentTime = sdf.format(Date())
        binding.lastUpdatedText.text = "Last updated: $currentTime"
    }
}

