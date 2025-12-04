package com.example.advancedmobilebitcointracker

import android.content.Context
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.advancedmobilebitcointracker.databinding.FragmentHomeBinding
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import org.json.JSONException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sqrt

class HomeFragment : Fragment(), SensorEventListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // APIs
    private val currentPriceUrl = "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin&vs_currencies=usd,gbp,eur"
    private val historicalDataUrl = "https://api.coingecko.com/api/v3/coins/bitcoin/market_chart?vs_currency=usd&days=7"

    // Sensors
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var lastShakeTime: Long = 0

    // Portfolio
    private var currentBtcPriceUsd: Double = 0.0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Sensors
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Load saved portfolio amount
        val prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val savedAmount = prefs.getString("btc_amount", "")
        binding.etBtcAmount.setText(savedAmount)

        // Listener for Portfolio Input
        binding.etBtcAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val amountStr = s.toString()

                // Save the new amount
                prefs.edit().putString("btc_amount", amountStr).apply()

                // Recalculate
                calculatePortfolioValue()
            }
        })

        binding.refreshButton.setOnClickListener { fetchAllData() }
        fetchAllData()
    }

    private fun calculatePortfolioValue() {
        val amountStr = binding.etBtcAmount.text.toString()
        if (amountStr.isNotEmpty() && currentBtcPriceUsd > 0) {
            try {
                val amount = amountStr.toDouble()
                val value = amount * currentBtcPriceUsd
                binding.tvPortfolioValue.text = String.format("$%,.2f", value)
            } catch (e: NumberFormatException) {
                binding.tvPortfolioValue.text = "$0.00"
            }
        } else {
            binding.tvPortfolioValue.text = "$0.00"
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // --- Sensor & Data Logic ---
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val x = it.values[0]
            val y = it.values[1]
            val z = it.values[2]
            val acceleration = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH

            if (acceleration > 12) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastShakeTime > 2000) {
                    lastShakeTime = currentTime
                    Toast.makeText(context, "Shake detected!", Toast.LENGTH_SHORT).show()
                    fetchAllData()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun fetchAllData() {
        binding.loadingSpinner.visibility = View.VISIBLE
        binding.refreshButton.isEnabled = false
        fetchCurrentPrice()
        fetchHistoricalData()
    }

    private fun fetchCurrentPrice() {
        val queue = Volley.newRequestQueue(requireContext())
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, currentPriceUrl, null,
            { response ->
                try {
                    val bitcoinObject = response.getJSONObject("bitcoin")
                    val priceUsd = bitcoinObject.getDouble("usd")

                    // Store for portfolio calculation
                    currentBtcPriceUsd = priceUsd
                    calculatePortfolioValue()

                    binding.priceUsdText.text = String.format("$%,.2f", priceUsd)
                    binding.priceGbpText.text = String.format("£%,.2f", bitcoinObject.getDouble("gbp"))
                    binding.priceEurText.text = String.format("€%,.2f", bitcoinObject.getDouble("eur"))
                    updateTimestamp()
                } catch (e: JSONException) { Log.e("API", "Error: ${e.message}") }
            },
            { error -> Log.e("API", "Error: ${error.message}") }
        )
        queue.add(jsonObjectRequest)
    }

    private fun fetchHistoricalData() {
        val queue = Volley.newRequestQueue(requireContext())
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, historicalDataUrl, null,
            { response ->
                try {
                    val pricesArray = response.getJSONArray("prices")
                    val chartEntries = mutableListOf<Entry>()
                    for (i in 0 until pricesArray.length()) {
                        val pricePoint = pricesArray.getJSONArray(i)
                        chartEntries.add(Entry(pricePoint.getLong(0).toFloat(), pricePoint.getDouble(1).toFloat()))
                    }
                    setupChart(chartEntries)
                } catch (e: JSONException) { Log.e("API", "Chart Error: ${e.message}") }
                finally {
                    binding.loadingSpinner.visibility = View.GONE
                    binding.refreshButton.isEnabled = true
                }
            },
            { error ->
                binding.loadingSpinner.visibility = View.GONE
                binding.refreshButton.isEnabled = true
            }
        )
        queue.add(jsonObjectRequest)
    }

    private fun setupChart(entries: List<Entry>) {
        val dataSet = LineDataSet(entries, "Price").apply {
            color = Color.parseColor("#FBBF24")
            valueTextColor = Color.WHITE
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = 2f
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = Color.parseColor("#FBBF24")
            fillAlpha = 60
            isHighlightEnabled = true
            highLightColor = Color.parseColor("#FBBF24")
        }

        binding.priceChart.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.textColor = Color.LTGRAY
            xAxis.setDrawGridLines(false)
            xAxis.valueFormatter = object : ValueFormatter() {
                private val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
                override fun getFormattedValue(value: Float): String = sdf.format(Date(value.toLong()))
            }
            axisLeft.textColor = Color.LTGRAY
            axisLeft.gridColor = Color.parseColor("#4B5563")
            axisRight.isEnabled = false

            // Attach Custom Marker View
            val markerView = CustomMarkerView(requireContext(), R.layout.marker_view)
            markerView.chartView = this
            marker = markerView

            invalidate()
        }
    }

    private fun updateTimestamp() {
        binding.lastUpdatedText.text = "Last updated: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}"
    }

    // --- Helper Class for the Custom Marker ---
    inner class CustomMarkerView(context: Context, layoutResource: Int) : MarkerView(context, layoutResource) {
        private val tvContent: TextView = findViewById(R.id.tvPrice)
        private val tvDate: TextView = findViewById(R.id.tvDate)
        private val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

        override fun refreshContent(e: Entry?, highlight: Highlight?) {
            e?.let {
                tvContent.text = "$${String.format("%,.2f", it.y)}"
                tvDate.text = sdf.format(Date(it.x.toLong()))
            }
            super.refreshContent(e, highlight)
        }
        override fun getOffset(): MPPointF = MPPointF(-(width / 2f), -height.toFloat())
    }
}