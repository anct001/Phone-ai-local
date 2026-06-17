package com.phoneai.local.model

/**
 * Curated catalog of on-device models that run well on Snapdragon-class chips
 * with 8 GB+ RAM (tuned descriptions for Snapdragon 8s Gen 4 / 16 GB).
 *
 * All quants are sourced from bartowski's GGUF repos on HuggingFace.
 * If a download URL 404s, the repo/file naming may have changed — update here.
 */
object ModelCatalog {

    val MODELS: List<ModelConfig> = listOf(

        // ── Gemma 3 4B Q4 — the recommended default ────────────────────────────
        ModelConfig(
            id            = "gemma3-4b-q4",
            displayName   = "Gemma 3 4B Instruct",
            family        = "Gemma 3",
            paramsLabel   = "4B",
            quant         = "Q4_K_M",
            fileName      = "gemma-3-4b-it-Q4_K_M.gguf",
            downloadUrl   = "https://huggingface.co/bartowski/google_gemma-3-4b-it-GGUF/resolve/main/google_gemma-3-4b-it-Q4_K_M.gguf",
            sizeMb        = 2600,
            ramRequiredMb = 3600,
            totalLayers   = 34,
            nCtx          = 4096,
            nThreads      = 4,
            speedLabel    = "~30-40 tok/s",
            qualityStars  = 4,
            tags          = listOf("Khuyến nghị", "Đa ngôn ngữ", "Cân bằng"),
            description   =
                "Lựa chọn cân bằng tốt nhất cho máy của bạn. Google tối ưu riêng cho " +
                "thiết bị di động, hỗ trợ tiếng Việt khá tốt, suy luận ổn và tốc độ " +
                "nhanh. Q4_K_M giữ ~99% chất lượng gốc trong khi chỉ tốn ~2.6 GB. " +
                "Phù hợp cho chat hằng ngày, tóm tắt, dịch thuật, viết nội dung."
        ),

        // ── Gemma 3 4B Q5 — higher quality ─────────────────────────────────────
        ModelConfig(
            id            = "gemma3-4b-q5",
            displayName   = "Gemma 3 4B Instruct",
            family        = "Gemma 3",
            paramsLabel   = "4B",
            quant         = "Q5_K_M",
            fileName      = "gemma-3-4b-it-Q5_K_M.gguf",
            downloadUrl   = "https://huggingface.co/bartowski/google_gemma-3-4b-it-GGUF/resolve/main/google_gemma-3-4b-it-Q5_K_M.gguf",
            sizeMb        = 3100,
            ramRequiredMb = 4200,
            totalLayers   = 34,
            nCtx          = 4096,
            nThreads      = 4,
            speedLabel    = "~25-32 tok/s",
            qualityStars  = 5,
            tags          = listOf("Chất lượng cao", "Đa ngôn ngữ"),
            description   =
                "Cùng model Gemma 3 4B nhưng nén ở mức Q5 — chất lượng câu trả lời " +
                "nhỉnh hơn Q4 một chút, đổi lại chậm hơn ~20% và tốn thêm RAM. " +
                "Chọn bản này nếu bạn ưu tiên độ chính xác hơn tốc độ và máy còn dư RAM."
        ),

        // ── Gemma 3 1B — ultra light / fastest ─────────────────────────────────
        ModelConfig(
            id            = "gemma3-1b-q4",
            displayName   = "Gemma 3 1B Instruct",
            family        = "Gemma 3",
            paramsLabel   = "1B",
            quant         = "Q4_K_M",
            fileName      = "gemma-3-1b-it-Q4_K_M.gguf",
            downloadUrl   = "https://huggingface.co/bartowski/google_gemma-3-1b-it-GGUF/resolve/main/google_gemma-3-1b-it-Q4_K_M.gguf",
            sizeMb        = 800,
            ramRequiredMb = 1400,
            totalLayers   = 26,
            nCtx          = 8192,
            nThreads      = 4,
            speedLabel    = "~80-100 tok/s",
            qualityStars  = 2,
            tags          = listOf("Siêu nhẹ", "Nhanh nhất", "Tiết kiệm pin"),
            description   =
                "Model siêu nhẹ, gần như tức thời và rất tiết kiệm pin/nhiệt. " +
                "Phù hợp cho tác vụ đơn giản: trả lời nhanh, gợi ý văn bản, " +
                "phân loại, chatbot cơ bản. Không mạnh về suy luận phức tạp hay " +
                "kiến thức sâu — nếu cần thông minh hơn hãy chọn bản 3B/4B."
        ),

        // ── Qwen2.5 3B — best Vietnamese in a small size ───────────────────────
        ModelConfig(
            id            = "qwen25-3b-q4",
            displayName   = "Qwen2.5 3B Instruct",
            family        = "Qwen2.5",
            paramsLabel   = "3B",
            quant         = "Q4_K_M",
            fileName      = "qwen2.5-3b-instruct-q4_k_m.gguf",
            downloadUrl   = "https://huggingface.co/bartowski/Qwen2.5-3B-Instruct-GGUF/resolve/main/Qwen2.5-3B-Instruct-Q4_K_M.gguf",
            sizeMb        = 2000,
            ramRequiredMb = 2900,
            totalLayers   = 36,
            nCtx          = 4096,
            nThreads      = 4,
            speedLabel    = "~40-50 tok/s",
            qualityStars  = 4,
            tags          = listOf("Tiếng Việt tốt", "Nhanh", "Lập trình"),
            description   =
                "Alibaba Qwen2.5 nổi tiếng xử lý tiếng Việt và tiếng Trung rất tốt, " +
                "đồng thời mạnh về code và toán so với kích thước. Bản 3B vừa nhanh " +
                "vừa thông minh, là lựa chọn thay thế tuyệt vời cho Gemma nếu bạn dùng " +
                "tiếng Việt nhiều hoặc cần hỗ trợ lập trình."
        ),

        // ── Qwen2.5 7B — strongest, heavier ────────────────────────────────────
        ModelConfig(
            id            = "qwen25-7b-q4",
            displayName   = "Qwen2.5 7B Instruct",
            family        = "Qwen2.5",
            paramsLabel   = "7B",
            quant         = "Q4_K_M",
            fileName      = "qwen2.5-7b-instruct-q4_k_m.gguf",
            downloadUrl   = "https://huggingface.co/bartowski/Qwen2.5-7B-Instruct-GGUF/resolve/main/Qwen2.5-7B-Instruct-Q4_K_M.gguf",
            sizeMb        = 4700,
            ramRequiredMb = 6200,
            totalLayers   = 28,
            nCtx          = 4096,
            nThreads      = 4,
            speedLabel    = "~15-22 tok/s",
            qualityStars  = 5,
            tags          = listOf("Thông minh nhất", "Tiếng Việt tốt", "Nặng"),
            description   =
                "Model mạnh nhất trong danh sách — chất lượng trả lời, suy luận và " +
                "kiến thức tiệm cận trợ lý đám mây. Đổi lại tốn ~6 GB RAM và chậm hơn " +
                "đáng kể, máy sẽ nóng hơn khi dùng lâu. Với 16 GB RAM máy bạn vẫn chạy " +
                "tốt; chọn bản này khi cần chất lượng cao nhất và chấp nhận tốc độ vừa phải."
        ),

        // ── Llama 3.2 3B — fast, solid English ─────────────────────────────────
        ModelConfig(
            id            = "llama32-3b-q4",
            displayName   = "Llama 3.2 3B Instruct",
            family        = "Llama 3.2",
            paramsLabel   = "3B",
            quant         = "Q4_K_M",
            fileName      = "llama-3.2-3b-instruct-q4_k_m.gguf",
            downloadUrl   = "https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_K_M.gguf",
            sizeMb        = 2000,
            ramRequiredMb = 2900,
            totalLayers   = 28,
            nCtx          = 4096,
            nThreads      = 4,
            speedLabel    = "~40-55 tok/s",
            qualityStars  = 3,
            tags          = listOf("Nhanh", "Phổ biến"),
            description   =
                "Model của Meta, rất nhanh và nhẹ, hệ sinh thái cộng đồng lớn. " +
                "Mạnh nhất ở tiếng Anh; tiếng Việt ở mức khá. Lựa chọn tốt nếu bạn " +
                "muốn tốc độ cao mà vẫn thông minh hơn bản 1B."
        ),

        // ── Phi-3.5 mini — reasoning per parameter ─────────────────────────────
        ModelConfig(
            id            = "phi35-mini-q4",
            displayName   = "Phi-3.5 Mini Instruct",
            family        = "Phi-3.5",
            paramsLabel   = "3.8B",
            quant         = "Q4_K_M",
            fileName      = "phi-3.5-mini-instruct-q4_k_m.gguf",
            downloadUrl   = "https://huggingface.co/bartowski/Phi-3.5-mini-instruct-GGUF/resolve/main/Phi-3.5-mini-instruct-Q4_K_M.gguf",
            sizeMb        = 2400,
            ramRequiredMb = 3300,
            totalLayers   = 32,
            nCtx          = 4096,
            nThreads      = 4,
            speedLabel    = "~30-40 tok/s",
            qualityStars  = 4,
            tags          = listOf("Suy luận tốt", "Toán & Code"),
            description   =
                "Microsoft Phi-3.5 mạnh về suy luận, toán và lập trình một cách " +
                "đáng ngạc nhiên so với kích thước nhỏ. Tiếng Việt ở mức trung bình. " +
                "Chọn bản này nếu bạn thiên về giải bài toán logic, code, phân tích."
        ),
    )

    fun byId(id: String): ModelConfig? = MODELS.firstOrNull { it.id == id }

    val default: ModelConfig
        get() = byId(ModelConfig.DEFAULT_ID) ?: MODELS.first()
}
