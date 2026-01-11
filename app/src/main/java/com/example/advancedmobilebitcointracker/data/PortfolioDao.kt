package com.example.advancedmobilebitcointracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PortfolioDao {

    // CREATE: Add a new coin
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addCoin(coin: PortfolioCoin)

    // READ: Get all coins, sorted by newest first
    @Query("SELECT * FROM portfolio_table ORDER BY id DESC")
    fun getAllCoins(): Flow<List<PortfolioCoin>>

    // DELETE: Remove a coin
    @Delete
    suspend fun deleteCoin(coin: PortfolioCoin)
}