package com.phoneai.formchecker.api

import android.util.Base64
import com.google.gson.Gson
import com.phoneai.formchecker.BuildConfig
import com.phoneai.formchecker.analyzer.FormAnalysisResult
import com.phoneai.formchecker.analyzer.FormAnalyzer
import com.phoneai.formchecker.analyzer.FormPrompt
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

/** Engine phân tích bằng Claude Vision API (cloud, cần mạng + API key). */
class ClaudeApiService : FormAnalyzer {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    override fun analyze(imageFile: File): FormAnalysisResult {
        val imageBytes = imageFile.readBytes()
        val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

        val requestBody = buildRequestBody(base64Image)
        val json = gson.toJson(requestBody)

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", BuildConfig.CLAUDE_API_KEY)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()
            ?: throw RuntimeException("Empty response from API")

        if (!response.isSuccessful) {
            throw RuntimeException("API error ${response.code}: $responseBody")
        }

        return parseResponse(responseBody)
    }

    private fun buildRequestBody(base64Image: String): Map<String, Any> {
        val userMessage = mapOf(
            "role" to "user",
            "content" to listOf(
                mapOf(
                    "type" to "image",
                    "source" to mapOf(
                        "type" to "base64",
                        "media_type" to "image/jpeg",
                        "data" to base64Image
                    )
                ),
                mapOf(
                    "type" to "text",
                    "text" to FormPrompt.USER_PROMPT
                )
            )
        )

        return mapOf(
            "model" to "claude-sonnet-4-6",
            "max_tokens" to 4096,
            "system" to FormPrompt.SYSTEM_PROMPT,
            "messages" to listOf(userMessage)
        )
    }

    private fun parseResponse(responseBody: String): FormAnalysisResult {
        val responseMap = gson.fromJson(responseBody, Map::class.java)
        val content = (responseMap["content"] as? List<*>)?.firstOrNull()
        val text = ((content as? Map<*, *>)?.get("text") as? String)
            ?: throw RuntimeException("No text content in response")

        val jsonText = FormPrompt.extractJson(text)
        val parsed = gson.fromJson(jsonText, FormAnalysisResult::class.java)
        return FormPrompt.sanitize(parsed)
    }
}
