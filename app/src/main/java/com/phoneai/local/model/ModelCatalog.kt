package com.phoneai.local.model

/**
 * Curated catalog of on-device models for Snapdragon-class Android devices.
 *
 * Speed labels are for CPU-only ARM NEON inference (6 threads, Snapdragon 8s Gen 4).
 * Enabling Vulkan GPU offload roughly 2-3× the tok/s.
 *
 * All quants from bartowski's HuggingFace repos.
 * If a URL 404s the repo name may have changed — update [downloadUrl] here.
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
            nThreads      = 6,
            speedLabel    = "~10-14 tok/s (CPU)",
            qualityStars  = 4,
            tags          = listOf("Khuyến nghị", "Đa ngôn ngữ", "Cân bằng"),
            description   =
                "Lựa chọn cân bằng tốt nhất. Google tối ưu riêng cho thiết bị di động, " +
                "hỗ trợ tiếng Việt khá tốt. Q4_K_M giữ ~99% chất lượng gốc trong khi " +
                "chỉ tốn ~2.6 GB. Phù hợp cho chat hằng ngày, tóm tắt, dịch thuật."
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
            nThreads      = 6,
            speedLabel    = "~8-11 tok/s (CPU)",
            qualityStars  = 5,
            tags          = listOf("Chất lượng cao", "Đa ngôn ngữ"),
            description   =
                "Cùng model Gemma 3 4B nhưng nén ở mức Q5 — chất lượng câu trả lời " +
                "nhỉnh hơn Q4 một chút, đổi lại chậm hơn ~20% và tốn thêm RAM. " +
                "Chọn bản này nếu bạn ưu tiên độ chính xác hơn tốc độ."
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
            nThreads      = 6,
            speedLabel    = "~30-45 tok/s (CPU)",
            qualityStars  = 2,
            tags          = listOf("Siêu nhẹ", "Nhanh nhất", "Tiết kiệm pin"),
            description   =
                "Model siêu nhẹ, phản hồi nhanh và rất tiết kiệm pin/nhiệt. " +
                "Phù hợp cho tác vụ đơn giản: trả lời nhanh, gợi ý văn bản, " +
                "phân loại, chatbot cơ bản. Không mạnh về suy luận phức tạp."
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
            nThreads      = 6,
            speedLabel    = "~14-20 tok/s (CPU)",
            qualityStars  = 4,
            tags          = listOf("Tiếng Việt tốt", "Lập trình", "Cân bằng"),
            description   =
                "Alibaba Qwen2.5 nổi tiếng xử lý tiếng Việt và tiếng Trung rất tốt, " +
                "đồng thời mạnh về code và toán. Bản 3B vừa nhanh vừa thông minh — " +
                "lựa chọn thay thế tuyệt vời cho Gemma nếu bạn dùng tiếng Việt nhiều."
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
            nThreads      = 6,
            speedLabel    = "~5-8 tok/s (CPU)",
            qualityStars  = 5,
            tags          = listOf("Thông minh nhất", "Tiếng Việt tốt", "Nặng"),
            description   =
                "Model mạnh nhất trong danh sách — chất lượng trả lời, suy luận và " +
                "kiến thức tiệm cận trợ lý đám mây. Chạy chậm hơn trên CPU, máy sẽ " +
                "nóng hơn khi dùng lâu. Chọn khi cần chất lượng cao nhất."
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
            nThreads      = 6,
            speedLabel    = "~14-20 tok/s (CPU)",
            qualityStars  = 3,
            tags          = listOf("Nhanh", "Phổ biến"),
            description   =
                "Model của Meta, nhẹ và có hệ sinh thái cộng đồng lớn. " +
                "Mạnh nhất ở tiếng Anh; tiếng Việt ở mức khá. Lựa chọn tốt " +
                "nếu bạn muốn tốc độ cao mà vẫn thông minh hơn bản 1B."
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
            nThreads      = 6,
            speedLabel    = "~11-16 tok/s (CPU)",
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
