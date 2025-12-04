package com.example.advancedmobilebitcointracker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.advancedmobilebitcointracker.databinding.ActivityHistoryBinding
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding

    private val historyUrl =
        "https://api.coingecko.com/api/v3/coins/bitcoin/market_chart?vs_currency=usd&days=7"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fetchHistory()
    }

    private fun fetchHistory() {
        val queue = Volley.newRequestQueue(this)

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, historyUrl, null,
            { response ->
                val pricesArray = response.getJSONArray("prices")

                val entries = ArrayList<Entry>()

                for (i in 0 until pricesArray.length()) {
                    val point = pricesArray.getJSONArray(i)
                    val time = i.toFloat()
                    val price = point.getDouble(1).toFloat()
                    entries.add(Entry(time, price))
                }

                val dataSet = LineDataSet(entries, "Bitcoin Price (7 Days)")
                val lineData = LineData(dataSet)
                binding.historyChart.data = lineData
                binding.historyChart.invalidate()

            },
            { error ->
                Log.e("CHART_ERROR", error.toString())
            }
        )

        queue.add(jsonObjectRequest)
    }
}
