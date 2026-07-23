package com.example.data

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object SupabaseClientManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    // Live Supabase project credentials
    private var supabaseUrl: String = "https://iznvsbwdhopikdejaolv.supabase.co"
    private var supabaseAnonKey: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Iml6bnZzYndkaG9waWtkZWphb2x2Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODQ4MTg1MjIsImV4cCI6MjEwMDM5NDUyMn0.tSCwXfzeAUtwPjWXvSrzjA9ak6CY740qkh_SChbMXl0"

    private var currentUserToken: String = ""
    private var currentUserId: String = ""

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences("invoice_generator_prefs", Context.MODE_PRIVATE)
        supabaseUrl = prefs.getString("supabase_url", "https://iznvsbwdhopikdejaolv.supabase.co")?.trimEnd('/') ?: "https://iznvsbwdhopikdejaolv.supabase.co"
        supabaseAnonKey = prefs.getString("supabase_anon_key", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Iml6bnZzYndkaG9waWtkZWphb2x2Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODQ4MTg1MjIsImV4cCI6MjEwMDM5NDUyMn0.tSCwXfzeAUtwPjWXvSrzjA9ak6CY740qkh_SChbMXl0") ?: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Iml6bnZzYndkaG9waWtkZWphb2x2Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODQ4MTg1MjIsImV4cCI6MjEwMDM5NDUyMn0.tSCwXfzeAUtwPjWXvSrzjA9ak6CY740qkh_SChbMXl0"
        currentUserToken = prefs.getString("supabase_user_token", "") ?: ""
        currentUserId = prefs.getString("supabase_user_id", "") ?: ""
    }

    fun configureCredentials(context: Context, url: String, anonKey: String) {
        supabaseUrl = url.trimEnd('/')
        supabaseAnonKey = anonKey.trim()
        context.getSharedPreferences("invoice_generator_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("supabase_url", supabaseUrl)
            .putString("supabase_anon_key", supabaseAnonKey)
            .apply()
    }

    fun isConfigured(): Boolean {
        return supabaseUrl.isNotEmpty() &&
               !supabaseUrl.contains("your-supabase-project") &&
               supabaseAnonKey.isNotEmpty() &&
               !supabaseAnonKey.contains("your-supabase-anon-key")
    }

    // --- AUTHENTICATION API ---

    suspend fun signUpUser(context: Context, email: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Result.failure(Exception("Supabase credentials not configured"))
        }

        try {
            val jsonBody = JSONObject().apply {
                put("email", email)
                put("password", password)
            }.toString()

            val request = Request.Builder()
                .url("$supabaseUrl/auth/v1/signup")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                val token = json.optString("access_token", "")
                val user = json.optJSONObject("user")
                val uid = user?.optString("id", "") ?: ""

                if (token.isNotEmpty()) {
                    saveSession(context, token, uid)
                    Result.success("LOGGED_IN")
                } else {
                    Result.success("CONFIRMATION_REQUIRED")
                }
            } else {
                val errorMsg = try {
                    val jsonObj = JSONObject(responseBody)
                    val rawMsg = jsonObj.optString("msg", jsonObj.optString("error_description", "Sign up failed"))
                    if (rawMsg.contains("rate limit", ignoreCase = true)) {
                        "Supabase email sending limit reached for this hour. Please confirm user manually in Supabase Dashboard > Auth > Users."
                    } else {
                        rawMsg
                    }
                } catch (e: Exception) {
                    "Sign up failed (${response.code})"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInUser(context: Context, email: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Result.failure(Exception("Supabase credentials not configured"))
        }

        try {
            val jsonBody = JSONObject().apply {
                put("email", email)
                put("password", password)
            }.toString()

            val request = Request.Builder()
                .url("$supabaseUrl/auth/v1/token?grant_type=password")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                val token = json.optString("access_token", "")
                val user = json.optJSONObject("user")
                val uid = user?.optString("id", "") ?: ""

                saveSession(context, token, uid)
                Result.success(token)
            } else {
                val errorMsg = try {
                    val jsonObj = JSONObject(responseBody)
                    val desc = jsonObj.optString("error_description", jsonObj.optString("msg", ""))
                    if (desc.contains("Email not confirmed", ignoreCase = true)) {
                        "Email not confirmed. Please check your inbox and verify your email before logging in."
                    } else if (desc.contains("Invalid login credentials", ignoreCase = true)) {
                        "Invalid email or password. Please check your credentials."
                    } else {
                        desc.ifEmpty { "Authentication failed (${response.code})" }
                    }
                } catch (e: Exception) {
                    "Authentication failed (${response.code})"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetPassword(email: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Result.failure(Exception("Supabase credentials not configured"))
        }

        try {
            val jsonBody = JSONObject().apply {
                put("email", email)
            }.toString()

            val request = Request.Builder()
                .url("$supabaseUrl/auth/v1/recover")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                Result.success(true)
            } else {
                val errorMsg = try {
                    val jsonObj = JSONObject(responseBody)
                    val rawMsg = jsonObj.optString("msg", jsonObj.optString("error_description", "Failed to send reset email"))
                    if (rawMsg.contains("rate limit", ignoreCase = true)) {
                        "Supabase email sending limit reached for this hour. Please try again in 1 hour."
                    } else {
                        rawMsg
                    }
                } catch (e: Exception) {
                    "Failed to send reset email (${response.code})"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun saveSession(context: Context, token: String, uid: String) {
        currentUserToken = token
        currentUserId = uid
        context.getSharedPreferences("invoice_generator_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("supabase_user_token", token)
            .putString("supabase_user_id", uid)
            .apply()
    }

    fun clearSession(context: Context) {
        currentUserToken = ""
        currentUserId = ""
        context.getSharedPreferences("invoice_generator_prefs", Context.MODE_PRIVATE)
            .edit()
            .remove("supabase_user_token")
            .remove("supabase_user_id")
            .apply()
    }

    // --- DATABASE POSTGREST DATA STORAGE API ---

    suspend fun syncInvoiceToSupabase(invoice: Invoice): Result<Boolean> = withContext(Dispatchers.IO) {
        if (!isConfigured() || currentUserToken.isEmpty()) {
            return@withContext Result.failure(Exception("Unauthenticated or Supabase not configured"))
        }

        try {
            val jsonBody = JSONObject().apply {
                put("invoice_number", invoice.invoiceNumber)
                put("business_name", invoice.businessName)
                put("customer_id", invoice.customerId)
                put("grand_total", invoice.grandTotal)
                put("status", invoice.status)
                put("user_id", currentUserId) // Ensures Row Level Security (RLS) data isolation
            }.toString()

            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/invoices")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $currentUserToken")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("Supabase sync failed: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
