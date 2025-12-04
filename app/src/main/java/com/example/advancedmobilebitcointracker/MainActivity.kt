package com.example.advancedmobilebitcointracker // Make sure this matches your package!

import android.content.Context
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.advancedmobilebitcointracker.databinding.ActivityMainBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import org.json.JSONException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityMainBinding

    // API URLs
    private val currentPriceUrl = "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin&vs_currencies=usd,gbp,eur"
    private val historicalDataUrl = "https://api.coingecko.com/api/v3/coins/bitcoin/market_chart?vs_currency=usd&days=7"

    // Sensor Variables
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var lastShakeTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Sensors
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Set up Button
        binding.refreshButton.setOnClickListener {
            fetchAllData()
        }

        // Load Initial Data
        fetchAllData()
    }

    // --- Sensor Logic (Shake to Refresh) ---
    override fun onResume() {
        super.onResume()
        sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val x = it.values[0]
            val y = it.values[1]
            val z = it.values[2]

            // Calculate acceleration magnitude
            val acceleration = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH

            // Check if shake is strong enough and not too frequent
            if (acceleration > 12) { // Threshold for shake
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastShakeTime > 2000) { // 2-second debounce
                    lastShakeTime = currentTime
                    Toast.makeText(this, "Shake detected! Refreshing...", Toast.LENGTH_SHORT).show()
                    fetchAllData()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this app
    }

    // --- API & Data Logic ---
    private fun fetchAllData() {
        binding.loadingSpinner.visibility = View.VISIBLE
        binding.refreshButton.isEnabled = false

        fetchCurrentPrice()
        fetchHistoricalData()
    }

    private fun fetchCurrentPrice() {
        val queue = Volley.newRequestQueue(this)
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, currentPriceUrl, null,
            { response ->
                try {
                    val bitcoinObject = response.getJSONObject("bitcoin")
                    val priceUsd = bitcoinObject.getDouble("usd")
                    val priceGbp = bitcoinObject.getDouble("gbp")
                    val priceEur = bitcoinObject.getDouble("eur")

                    binding.priceUsdText.text = String.format("$%,.2f", priceUsd)
                    binding.priceGbpText.text = String.format("£%,.2f", priceGbp)
                    binding.priceEurText.text = String.format("€%,.2f", priceEur)

                    updateTimestamp()
                } catch (e: JSONException) {
                    handleApiError("Current Price JSON parsing error", e)
                }
            },
            { error ->
                handleApiError("Current Price Volley error", error)
            }
        )
        queue.add(jsonObjectRequest)
    }

    private fun fetchHistoricalData() {
        val queue = Volley.newRequestQueue(this)
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, historicalDataUrl, null,
            { response ->
                try {
                    val pricesArray = response.getJSONArray("prices")
                    val chartEntries = mutableListOf<Entry>()

                    // Convert API data to Chart Entries
                    for (i in 0 until pricesArray.length()) {
                        val pricePoint = pricesArray.getJSONArray(i)
                        val timestamp = pricePoint.getLong(0)
                        val price = pricePoint.getDouble(1).toFloat()
                        chartEntries.add(Entry(timestamp.toFloat(), price))
                    }
                    setupChart(chartEntries)
                } catch (e: JSONException) {
                    handleApiError("Historical Data JSON parsing error", e)
                } finally {
                    binding.loadingSpinner.visibility = View.GONE
                    binding.refreshButton.isEnabled = true
                }
            },
            { error ->
                handleApiError("Historical Data Volley error", error)
                binding.loadingSpinner.visibility = View.GONE
                binding.refreshButton.isEnabled = true
            }
        )
        queue.add(jsonObjectRequest)
    }

    private fun setupChart(entries: List<Entry>) {
        val dataSet = LineDataSet(entries, "Bitcoin Price (USD)").apply {
            color = Color.parseColor("#FBBF24") // Yellow line
            valueTextColor = Color.WHITE
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = 2f
            mode = LineDataSet.Mode.CUBIC_BEZIER // Smooth curves
            setDrawFilled(true)
            fillColor = Color.parseColor("#FBBF24")
            fillAlpha = 60
        }

        val lineData = LineData(dataSet)
        binding.priceChart.apply {
            data = lineData
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = Color.LTGRAY
                setDrawGridLines(false)
                valueFormatter = object : ValueFormatter() {
                    private val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
                    override fun getFormattedValue(value: Float): String {
                        return sdf.format(Date(value.toLong()))
                    }
                }
            }

            axisLeft.apply {
                textColor = Color.LTGRAY
                setDrawGridLines(true)
                gridColor = Color.parseColor("#4B5563")
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return "$${"%,.0f".format(value)}"
                    }
                }
            }
            axisRight.isEnabled = false
            invalidate() // Refresh the chart
        }
    }

    private fun updateTimestamp() {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val currentTime = sdf.format(Date())
        binding.lastUpdatedText.text = "Last updated: $currentTime"
    }

    private fun handleApiError(logMessage: String, error: Exception) {
        Log.e("API_ERROR", "$logMessage: ${error.message}")
        // We suppress the toast here to avoid spamming the user if one API fails but others work
    }
}