package com.example.advancedmobilebitcointracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// This class represents a table named "portfolio_table" in your database
@Entity(tableName = "portfolio_table")
data class PortfolioCoin(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,       // Unique ID for each row
    val symbol: String,    // e.g., "bitcoin"
    val amount: Double     // e.g., 0.5
)