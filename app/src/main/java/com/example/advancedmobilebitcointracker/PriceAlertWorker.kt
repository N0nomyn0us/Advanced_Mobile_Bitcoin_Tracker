package com.example.advancedmobilebitcointracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.RequestFuture
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class PriceAlertWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        // This method runs in the background
        return try {
            fetchPriceAndNotify()
            Result.success()
        } catch (e: Exception) {
            Log.e("PriceAlertWorker", "Error: ${e.message}")
            Result.retry()
        }
    }

    private fun fetchPriceAndNotify() {
        // We use a Synchronous request here because we are already on a background thread
        val url = "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin&vs_currencies=usd"
        val future = RequestFuture.newFuture<JSONObject>()
        val request = JsonObjectRequest(url, null, future, future)

        Volley.newRequestQueue(applicationContext).add(request)

        try {
            // Wait up to 30 seconds for the API
            val response = future.get(30, TimeUnit.SECONDS)
            val price = response.getJSONObject("bitcoin").getDouble("usd")

            showNotification(price)
        } catch (e: Exception) {
            Log.e("PriceAlertWorker", "API Failed: ${e.message}")
            throw e // Let WorkManager know it failed
        }
    }

    private fun showNotification(price: Double) {
        val channelId = "bitcoin_price_alerts"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Channel (Required for Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Price Alerts", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val formattedPrice = String.format("$%,.2f", price)

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Default icon
            .setContentTitle("Bitcoin Price Update")
            .setContentText("Current Price: $formattedPrice")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        // ID 1 means we update the same notification every time
        notificationManager.notify(1, notification)
    }
}