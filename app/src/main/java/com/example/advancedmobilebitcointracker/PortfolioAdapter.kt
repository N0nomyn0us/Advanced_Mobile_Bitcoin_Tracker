package com.example.advancedmobilebitcointracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.advancedmobilebitcointracker.data.PortfolioCoin

class PortfolioAdapter(
    private var coins: List<PortfolioCoin>,
    private val onDeleteClick: (PortfolioCoin) -> Unit
) : RecyclerView.Adapter<PortfolioAdapter.CoinViewHolder>() {

    class CoinViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSymbol: TextView = view.findViewById(R.id.tv_coin_symbol)
        val tvAmount: TextView = view.findViewById(R.id.tv_coin_amount)
        val btnDelete: ImageView = view.findViewById(R.id.btn_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoinViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_portfolio_coin, parent, false)
        return CoinViewHolder(view)
    }

    override fun onBindViewHolder(holder: CoinViewHolder, position: Int) {
        val coin = coins[position]
        holder.tvSymbol.text = coin.symbol.replaceFirstChar { it.uppercase() }
        holder.tvAmount.text = "${coin.amount} Coins"

        // Handle Delete Button
        holder.btnDelete.setOnClickListener { onDeleteClick(coin) }
    }

    override fun getItemCount() = coins.size

    fun updateData(newCoins: List<PortfolioCoin>) {
        coins = newCoins
        notifyDataSetChanged()
    }
}