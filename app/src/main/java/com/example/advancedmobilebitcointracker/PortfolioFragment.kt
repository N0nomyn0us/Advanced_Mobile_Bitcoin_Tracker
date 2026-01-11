package com.example.advancedmobilebitcointracker

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.advancedmobilebitcointracker.data.AppDatabase
import com.example.advancedmobilebitcointracker.data.PortfolioCoin
import com.example.advancedmobilebitcointracker.databinding.FragmentPortfolioBinding
import kotlinx.coroutines.launch
import org.json.JSONException

class PortfolioFragment : Fragment() {

    private var _binding: FragmentPortfolioBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: PortfolioAdapter
    private lateinit var sharedPreferences: SharedPreferences

    // Default starting cash for new users
    private val STARTING_CASH = 10000.0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPortfolioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

        // Initialize cash if first time
        if (!sharedPreferences.contains("fiat_cash")) {
            sharedPreferences.edit().putFloat("fiat_cash", STARTING_CASH.toFloat()).apply()
        }

        updateCashDisplay()

        // Setup RecyclerView
        adapter = PortfolioAdapter(emptyList()) { coin ->
            sellCoin(coin) // Changed "delete" to "sell" logic
        }
        binding.rvPortfolio.layoutManager = LinearLayoutManager(context)
        binding.rvPortfolio.adapter = adapter

        loadPortfolio()

        binding.fabAddCoin.setOnClickListener {
            showBuyCoinDialog()
        }
    }

    private fun updateCashDisplay() {
        val cash = sharedPreferences.getFloat("fiat_cash", 0f)
        binding.tvCashBalance.text = "$${String.format("%,.2f", cash)}"
    }

    private fun loadPortfolio() {
        val database = AppDatabase.getDatabase(requireContext())

        lifecycleScope.launch {
            database.portfolioDao().getAllCoins().collect { coins ->
                adapter.updateData(coins)
                // Just calculating coin count for asset value placeholder
                // In a real app, you'd need to fetch prices for ALL coins to get true asset value
                var totalAssets = 0.0
                coins.forEach { totalAssets += it.amount }
                binding.tvTotalBalance.text = "${String.format("%.2f", totalAssets)} Coins"
            }
        }
    }

    private fun showBuyCoinDialog() {
        val context = requireContext()
        val layout = android.widget.LinearLayout(context)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        val inputSymbol = EditText(context)
        inputSymbol.hint = "Coin Symbol (e.g. bitcoin)"
        layout.addView(inputSymbol)

        val inputAmount = EditText(context)
        inputAmount.hint = "Amount to Buy (e.g. 0.5)"
        inputAmount.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        layout.addView(inputAmount)

        AlertDialog.Builder(context)
            .setTitle("Buy Crypto")
            .setMessage("Check price & buy with your cash balance.")
            .setView(layout)
            .setPositiveButton("Buy") { _, _ ->
                val symbol = inputSymbol.text.toString().lowercase().trim()
                val amountStr = inputAmount.text.toString()

                if (symbol.isNotEmpty() && amountStr.isNotEmpty()) {
                    executeBuyOrder(symbol, amountStr.toDouble())
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun executeBuyOrder(symbol: String, amount: Double) {
        // 1. Fetch Price
        val url = "https://api.coingecko.com/api/v3/simple/price?ids=$symbol&vs_currencies=usd"

        val request = object : JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                try {
                    if (response.has(symbol)) {
                        val price = response.getJSONObject(symbol).getDouble("usd")
                        val totalCost = price * amount
                        val currentCash = sharedPreferences.getFloat("fiat_cash", 0f).toDouble()

                        // 2. Check Balance
                        if (currentCash >= totalCost) {
                            // 3. Deduct Cash & Add Coin
                            val newCash = currentCash - totalCost
                            sharedPreferences.edit().putFloat("fiat_cash", newCash.toFloat()).apply()
                            updateCashDisplay()

                            addCoinToDb(symbol, amount)

                            Toast.makeText(context, "Bought $amount $symbol for $${String.format("%,.2f", totalCost)}", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Insufficient Funds! Cost: $${String.format("%,.2f", totalCost)}", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, "Coin not found!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: JSONException) {
                    Toast.makeText(context, "Error parsing price", Toast.LENGTH_SHORT).show()
                }
            },
            {
                Toast.makeText(context, "Network Error: Cannot verify price", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["User-Agent"] = "Mozilla/5.0"
                return headers
            }
        }
        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun addCoinToDb(symbol: String, amount: Double) {
        val database = AppDatabase.getDatabase(requireContext())
        lifecycleScope.launch {
            database.portfolioDao().addCoin(PortfolioCoin(symbol = symbol, amount = amount))
        }
    }

    // Sell logic: We just delete for now, but ideally we'd add cash back
    private fun sellCoin(coin: PortfolioCoin) {
        val database = AppDatabase.getDatabase(requireContext())
        lifecycleScope.launch {
            database.portfolioDao().deleteCoin(coin)
            Toast.makeText(context, "Sold (Asset Removed)", Toast.LENGTH_SHORT).show()
            // Optional: Implement fetch price -> add cash back logic here for full trading
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}