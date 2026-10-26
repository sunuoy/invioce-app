package com.example

import android.app.Application
import android.content.Context
import com.example.data.InvoiceDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class InvoiceApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Asynchronously warm up the SQLite database and SharedPreferences on background thread
        // so that by the time the UI and ViewModel initialize, disk handles and caches are ready.
        applicationScope.launch {
            try {
                // Pre-warm SharedPreferences in memory
                getSharedPreferences("invoice_generator_prefs", Context.MODE_PRIVATE)
                getSharedPreferences("app_settings", Context.MODE_PRIVATE)

                // Pre-warm Room SQLite connection
                InvoiceDatabase.getDatabase(this@InvoiceApplication).openHelper.writableDatabase
            } catch (e: Exception) {
                android.util.Log.e("InvoiceApplication", "Error during async warm-up: ${e.message}")
            }
        }
    }
}
