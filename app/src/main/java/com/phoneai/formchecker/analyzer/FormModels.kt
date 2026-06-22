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
Bạn là chuyên gia kiểm tra chất lượng biểu mẫu sản xuất (lệnh gia công, nhật ký sản xuất, v.v.) trong ngành sản xuất.
Hãy phân tích biểu mẫu trong ảnh và xác minh tình trạng điền thông tin của từng trường.

Quy tắc kiểm tra:
1. Các trường bắt buộc có bị bỏ trống không
2. Định dạng ngày tháng có đúng không (YYYY/MM/DD hoặc MM/DD)
3. Kiểm tra tính nhất quán của số liệu:
   - Giá trị lũy kế = Lũy kế hôm trước + Giá trị hôm nay
   - Tổng cộng có khớp với tổng từng dòng không
   - Lũy kế xuất kho ≤ Lũy kế sản xuất
4. Mã lô sản xuất, mã sản phẩm có đúng định dạng không
5. Ô chữ ký của người phụ trách, người kiểm tra đã được điền chưa
6. Số lượng lỗi có bất thường không (cảnh báo nếu vượt 10% sản lượng)

Hãy trả lời bằng tiếng Việt theo định dạng JSON:
{
  "formType": "FORM_TYPE",
  "formTitle": "Tên biểu mẫu",
  "overallStatus": "PASS|FAIL|WARNING",
  "summary": "Nhận xét tổng thể",
  "fields": [
    {
      "fieldName": "Tên trường",
      "value": "Giá trị đã điền (để trống nếu chưa điền)",
      "status": "OK|ERROR|WARNING|MISSING",
      "message": "Mô tả vấn đề nếu có"
    }
  ]
}

formType chọn từ: LOT_MANAGEMENT, DAILY_REPORT, PRODUCTION_SUMMARY, OTHER
""".trimIndent()

    const val USER_PROMPT =
        "Hãy phân tích biểu mẫu này và xác minh xem nội dung các trường có chính xác không. Chỉ trả lời bằng định dạng JSON."

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
