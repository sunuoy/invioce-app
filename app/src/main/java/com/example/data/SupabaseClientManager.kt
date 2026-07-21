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
    private var supabaseUrl: String = "https://hrjexnphspstixrfzunb.supabase.co"
    private var supabaseAnonKey: String = "sb_publishable_wj0LpVyXXhRHQ2WgKvGGiw_Qe_uSWJW"

    private var currentUserToken: String = ""
    private var currentUserId: String = ""

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences("invoice_generator_prefs", Context.MODE_PRIVATE)
        supabaseUrl = prefs.getString("supabase_url", "https://hrjexnphspstixrfzunb.supabase.co")?.trimEnd('/') ?: "https://hrjexnphspstixrfzunb.supabase.co"
        supabaseAnonKey = prefs.getString("supabase_anon_key", "sb_publishable_wj0LpVyXXhRHQ2WgKvGGiw_Qe_uSWJW") ?: "sb_publishable_wj0LpVyXXhRHQ2WgKvGGiw_Qe_uSWJW"
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
                    JSONObject(responseBody).optString("msg", JSONObject(responseBody).optString("error_description", "Sign up failed"))
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
                    JSONObject(responseBody).optString("error_description", JSONObject(responseBody).optString("msg", "Invalid email or password"))
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
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to send reset email"))
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
