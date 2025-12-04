package com.example.advancedmobilebitcointracker

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.example.advancedmobilebitcointracker.databinding.FragmentSearchBinding
import org.json.JSONException

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    // To prevent spamming the API with every keystroke
    private var lastSearchTime: Long = 0
    private val SEARCH_DELAY = 500L // milliseconds

    // Simple data class to hold our coin info
    data class CoinSuggestion(val id: String, val thumbUrl: String) {
        // This toString() is important because it's what AutoCompleteTextView uses to fill the text box when clicked
        override fun toString(): String {
            return id
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup the AutoComplete TextWatcher
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()

                // If the user has already selected a coin (the text matches exactly), don't search again
                // This fixes the "Menu pops back up" issue
                if (binding.searchResultText.text.startsWith("${query.replaceFirstChar { it.uppercase() }}:")) {
                    return
                }

                // Only search if we have at least 2 characters and haven't searched too recently
                if (query.length >= 2) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastSearchTime > SEARCH_DELAY) {
                        lastSearchTime = currentTime
                        fetchSuggestions(query)
                    }
                }
            }
        })

        // Handle Dropdown Selection
        binding.searchInput.setOnItemClickListener { parent, _, position, _ ->
            val selectedCoin = parent.getItemAtPosition(position) as CoinSuggestion
            fetchCoinPrice(selectedCoin.id)
            binding.searchInput.dismissDropDown() // Force close the menu
        }

        // Setup the button click (Manual Search)
        binding.searchBtn.setOnClickListener {
            val coinId = binding.searchInput.text.toString().lowercase().trim()
            if (coinId.isNotEmpty()) {
                fetchCoinPrice(coinId)
            }
        }
    }

    private fun fetchSuggestions(query: String) {
        // CoinGecko search endpoint
        val url = "https://api.coingecko.com/api/v3/search?query=$query"

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                try {
                    val coinsArray = response.getJSONArray("coins")
                    val suggestions = mutableListOf<CoinSuggestion>()

                    // Limit to top 5 results to keep it clean
                    val limit = if (coinsArray.length() > 5) 5 else coinsArray.length()

                    for (i in 0 until limit) {
                        val coin = coinsArray.getJSONObject(i)
                        val id = coin.getString("id")
                        val thumb = coin.getString("thumb") // Get the icon URL
                        suggestions.add(CoinSuggestion(id, thumb))
                    }

                    // Use our Custom Adapter with Icons
                    val adapter = CoinAdapter(requireContext(), suggestions)
                    binding.searchInput.setAdapter(adapter)

                    // Only show dropdown if the text doesn't already match the top result exactly
                    // This prevents the menu from showing up right after you click an item
                    if (suggestions.isNotEmpty() && suggestions[0].id != binding.searchInput.text.toString()) {
                        binding.searchInput.showDropDown()
                    }

                } catch (e: JSONException) {
                    Log.e("SearchFragment", "JSON Error: ${e.message}")
                }
            },
            { error ->
                Log.e("SearchFragment", "API Error: ${error.message}")
            }
        )
        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun fetchCoinPrice(coinId: String) {
        binding.searchLoading.visibility = View.VISIBLE
        binding.searchResultText.text = ""

        val url = "https://api.coingecko.com/api/v3/simple/price?ids=$coinId&vs_currencies=usd"

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                binding.searchLoading.visibility = View.GONE
                if (response.has(coinId)) {
                    val price = response.getJSONObject(coinId).getDouble("usd")
                    binding.searchResultText.text = "${coinId.replaceFirstChar { it.uppercase() }}: $${String.format("%,.2f", price)}"
                } else {
                    binding.searchResultText.text = "Coin not found."
                }
            },
            {
                binding.searchLoading.visibility = View.GONE
                binding.searchResultText.text = "Error fetching data."
            }
        )
        Volley.newRequestQueue(requireContext()).add(request)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // --- Custom Adapter to show Icons ---
    inner class CoinAdapter(context: Context, private val coins: List<CoinSuggestion>) :
        ArrayAdapter<CoinSuggestion>(context, R.layout.item_coin_suggestion, coins) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_coin_suggestion, parent, false)

            val coin = getItem(position)
            val textView = view.findViewById<TextView>(R.id.coin_name)
            val imageView = view.findViewById<ImageView>(R.id.coin_icon)

            if (coin != null) {
                textView.text = coin.id.replaceFirstChar { it.uppercase() }

                // Use Glide to load the image from the URL into the ImageView
                Glide.with(context)
                    .load(coin.thumbUrl)
                    .placeholder(android.R.drawable.ic_menu_help) // Fallback icon
                    .circleCrop() // Make it round
                    .into(imageView)
            }

            return view
        }
    }
}