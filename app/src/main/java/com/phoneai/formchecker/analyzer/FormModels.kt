package com.phoneai.formchecker.analyzer

import java.io.File

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

/** Engine dùng để phân tích biểu mẫu. */
enum class AnalyzerEngine { LOCAL_GEMMA, CLAUDE_CLOUD }

/** Interface chung cho mọi engine phân tích (local hoặc cloud). */
interface FormAnalyzer {
    /** Phân tích ảnh biểu mẫu và trả về kết quả kiểm tra từng trường. */
    fun analyze(imageFile: File): FormAnalysisResult

    /** Giải phóng tài nguyên (model on-device...). Mặc định không làm gì. */
    fun close() {}
}

/** Prompt + helper dùng chung cho cả engine local và cloud. */
object FormPrompt {

    val SYSTEM_PROMPT = """
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

    const val USER_PROMPT =
        "この帳票を解析して、各フィールドの記入内容が正確かどうか検証してください。JSON形式のみで回答してください。"

    /** Lấy đoạn JSON từ phản hồi của model (model có thể bọc trong markdown/giải thích). */
    fun extractJson(text: String): String {
        val jsonRegex = Regex("""\{[\s\S]*\}""")
        return jsonRegex.find(text)?.value
            ?: throw RuntimeException("Không tìm thấy JSON trong phản hồi: $text")
    }

    /** Chuẩn hoá kết quả: tránh null do parser sinh ra, đặt giá trị mặc định hợp lý. */
    fun sanitize(parsed: FormAnalysisResult?): FormAnalysisResult {
        val p = parsed ?: throw RuntimeException("Không phân tích được kết quả")
        return FormAnalysisResult(
            formType = p.formType ?: "",
            formTitle = p.formTitle ?: "",
            overallStatus = (p.overallStatus ?: "").ifBlank { "WARNING" },
            summary = p.summary ?: "",
            fields = (p.fields ?: emptyList()).map { f ->
                FieldResult(
                    fieldName = f.fieldName ?: "",
                    value = f.value ?: "",
                    status = (f.status ?: "").ifBlank { "WARNING" },
                    message = f.message ?: ""
                )
            }
        )
    }
}
