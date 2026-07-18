package com.example.data

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONObject

object GoogleDriveService {

    fun findBackupFile(token: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            val query = URLEncoder.encode("name = 'invoice_app_backup.json' and trashed = false", "UTF-8")
            val url = URL("https://www.googleapis.com/drive/v3/files?q=$query&fields=files(id)")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            android.util.Log.d("GoogleDriveService", "findBackupFile response code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                val json = JSONObject(response.toString())
                val files = json.optJSONArray("files")
                if (files != null && files.length() > 0) {
                    files.getJSONObject(0).optString("id", null)
                } else {
                    null
                }
            } else {
                val errorStream = connection.errorStream
                val errorText = errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                android.util.Log.e("GoogleDriveService", "findBackupFile error response: $errorText")
                throw Exception("HTTP $responseCode: $errorText")
            }
        } catch (e: Exception) {
            android.util.Log.e("GoogleDriveService", "findBackupFile exception: ${e.message}", e)
            throw e
        } finally {
            connection?.disconnect()
        }
    }

    fun uploadBackupFile(token: String, jsonContent: String, fileId: String?): Boolean {
        var connection: HttpURLConnection? = null
        return try {
            if (fileId != null) {
                // Update existing file (PATCH)
                val url = URL("https://www.googleapis.com/upload/drive/v3/files/$fileId?uploadType=media")
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "PATCH"
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val writer = OutputStreamWriter(connection.outputStream, "UTF-8")
                writer.write(jsonContent)
                writer.flush()
                writer.close()

                val responseCode = connection.responseCode
                android.util.Log.d("GoogleDriveService", "uploadBackupFile (PATCH) response code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    true
                } else {
                    val errorStream = connection.errorStream
                    val errorText = errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    android.util.Log.e("GoogleDriveService", "uploadBackupFile (PATCH) error response: $errorText")
                    throw Exception("HTTP $responseCode: $errorText")
                }
            } else {
                val url = URL("https://www.googleapis.com/drive/v3/files")
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val metadata = JSONObject()
                metadata.put("name", "invoice_app_backup.json")
                
                val writer = OutputStreamWriter(connection.outputStream, "UTF-8")
                writer.write(metadata.toString())
                writer.flush()
                writer.close()

                val responseCode = connection.responseCode
                android.util.Log.d("GoogleDriveService", "uploadBackupFile (POST) response code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()
                    
                    val createdFile = JSONObject(response.toString())
                    val newFileId = createdFile.optString("id", null)
                    
                    if (newFileId != null) {
                        connection.disconnect()
                        uploadBackupFile(token, jsonContent, newFileId)
                    } else {
                        throw Exception("Failed to get created file ID from response")
                    }
                } else {
                    val errorStream = connection.errorStream
                    val errorText = errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    android.util.Log.e("GoogleDriveService", "uploadBackupFile (POST) error response: $errorText")
                    throw Exception("HTTP $responseCode: $errorText")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("GoogleDriveService", "uploadBackupFile exception: ${e.message}", e)
            throw e
        } finally {
            connection?.disconnect()
        }
    }

    fun downloadBackupFile(token: String, fileId: String): String {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL("https://www.googleapis.com/drive/v3/files/$fileId?alt=media")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()
                response.toString()
            } else {
                throw Exception("Failed to download file, response code: ${connection.responseCode}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        } finally {
            connection?.disconnect()
        }
    }

    fun invalidateToken(context: Context, token: String) {
        try {
            val accountManager = android.accounts.AccountManager.get(context)
            accountManager.invalidateAuthToken("com.google", token)
            android.util.Log.d("GoogleDriveService", "Locally invalidated cached auth token in AccountManager")
        } catch (e: Exception) {
            android.util.Log.e("GoogleDriveService", "Failed to invalidate token locally: ${e.message}")
        }

        var connection: HttpURLConnection? = null
        Thread {
            try {
                val url = URL("https://oauth2.googleapis.com/revoke?token=$token")
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.responseCode
                android.util.Log.d("GoogleDriveService", "Revoked auth token on Google servers")
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                connection?.disconnect()
            }
        }.start()
    }
}
