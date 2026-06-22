package com.phoneai.formchecker.api

import android.util.Base64
import com.google.gson.Gson
import com.phoneai.formchecker.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

data class FieldResult(
    val fieldName: String = "",
    val value: String = "",
    val status: String = "",   // "OK", "ERROR", "WARNING", "MISSING"
    val message: String = ""
)

data class FormAnalysisResult(
    val formType: String = "",
    val formTitle: String = "",
    val overallStatus: String = "",   // "PASS", "FAIL", "WARNING"
    val summary: String = "",
    val fields: List<FieldResult> = emptyList()
)

class ClaudeApiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    fun analyzeForm(imageFile: File): FormAnalysisResult {
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
        val systemPrompt = """
あなたは日本の製造業で使われる帳票（ロット管理票・作業日報など）の品質チェック専門家です。
画像に写っている帳票を解析し、各フィールドの記入状況を確認してください。

以下のルールで検証してください：
1. 必須フィールドが未記入でないか
2. 日付フォーマットが正しいか（YYYY/MM/DD または MM/DD 形式）
3. 数値の整合性チェック：
   - 累計値 = 前日累計 + 当日値 になっているか
   - 合計欄の数値が各行の合計と一致しているか
   - 出庫累計 ≤ 日産累計 になっているか
4. 製造ロット番号・型番の形式が正しいか
5. 担当者名・確認者名などの署名欄が記入済みか
6. 不良数が異常に多くないか（日産数の10%超は警告）

必ずJSON形式で回答してください：
{
  "formType": "FORM_TYPE",
  "formTitle": "帳票タイトル",
  "overallStatus": "PASS|FAIL|WARNING",
  "summary": "全体的な評価コメント",
  "fields": [
    {
      "fieldName": "フィールド名",
      "value": "記入値（空白の場合は空文字）",
      "status": "OK|ERROR|WARNING|MISSING",
      "message": "問題がある場合の説明"
    }
  ]
}

formTypeは以下から選択: LOT_MANAGEMENT, DAILY_REPORT, PRODUCTION_SUMMARY, OTHER
""".trimIndent()

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
                    "text" to "この帳票を解析して、各フィールドの記入内容が正確かどうか検証してください。JSON形式で回答してください。"
                )
            )
        )

        return mapOf(
            "model" to "claude-sonnet-4-6",
            "max_tokens" to 4096,
            "system" to systemPrompt,
            "messages" to listOf(userMessage)
        )
    }

    private fun parseResponse(responseBody: String): FormAnalysisResult {
        val responseMap = gson.fromJson(responseBody, Map::class.java)
        val content = (responseMap["content"] as? List<*>)?.firstOrNull()
        val text = ((content as? Map<*, *>)?.get("text") as? String)
            ?: throw RuntimeException("No text content in response")

        // Extract JSON from the response (Claude sometimes wraps in markdown)
        val jsonText = extractJson(text)
        val parsed = gson.fromJson(jsonText, FormAnalysisResult::class.java)
            ?: throw RuntimeException("Failed to parse analysis result")

        // Sanitize: Gson can inject nulls into non-null Kotlin fields
        return FormAnalysisResult(
            formType = parsed.formType ?: "",
            formTitle = parsed.formTitle ?: "",
            overallStatus = (parsed.overallStatus ?: "").ifBlank { "WARNING" },
            summary = parsed.summary ?: "",
            fields = (parsed.fields ?: emptyList()).map { f ->
                FieldResult(
                    fieldName = f.fieldName ?: "",
                    value = f.value ?: "",
                    status = (f.status ?: "").ifBlank { "WARNING" },
                    message = f.message ?: ""
                )
            }
        )
    }

    private fun extractJson(text: String): String {
        val jsonRegex = Regex("""\{[\s\S]*\}""")
        return jsonRegex.find(text)?.value
            ?: throw RuntimeException("No JSON found in response: $text")
    }
}
