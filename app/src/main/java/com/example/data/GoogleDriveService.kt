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

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
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
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
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

                connection.responseCode == HttpURLConnection.HTTP_OK
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

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
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
                        false
                    }
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
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
        var connection: HttpURLConnection? = null
        Thread {
            try {
                val url = URL("https://oauth2.googleapis.com/revoke?token=$token")
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.responseCode
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                connection?.disconnect()
            }
        }.start()
    }
}
