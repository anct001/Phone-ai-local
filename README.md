# Phone AI — Local LLM on Android

Trợ lý AI chạy **hoàn toàn offline** trên Android, tối ưu cho Snapdragon 8s Gen 4 / 16 GB RAM.  
Engine: **llama.cpp** + Vulkan backend (Adreno GPU offload).

## Tính năng

- **Chọn & tải model ngay trong app** — không cần adb, không cần PC
- **Đánh giá độ phù hợp theo thiết bị** — mỗi model hiển thị badge
  *Khuyên dùng / Phù hợp / Hơi nặng / Thiếu RAM* dựa trên RAM trống thực tế
- Mô tả chi tiết từng model: tốc độ ước tính, chất lượng, điểm mạnh
- Tải có **resume** (tạm dừng/tiếp tục), GPU offload **thích ứng theo RAM**
- Chat streaming token, render Markdown, đổi model bất kỳ lúc nào

## Yêu cầu

- Android 10+ (API 29), chip hỗ trợ Vulkan 1.1
- RAM: 8 GB+ (16 GB lý tưởng)
- Dung lượng trống: 1–5 GB tuỳ model

## Cách dùng

1. Mở app → màn hình **Chọn model AI** hiện ra
2. Chọn model phù hợp (xem badge độ phù hợp + mô tả) → **Tải xuống**
3. Tải xong → nhấn **Dùng model này** → bắt đầu chat
4. Đổi model bất cứ lúc nào qua nút ⚙ trên thanh tiêu đề

## Model trong catalog

| Model | Quant | Size | Tốc độ | Điểm mạnh |
|---|---|---|---|---|
| Gemma 3 4B | Q4_K_M | 2.6 GB | ~30-40 tok/s | Cân bằng — **khuyến nghị** |
| Gemma 3 4B | Q5_K_M | 3.1 GB | ~25-32 tok/s | Chất lượng cao hơn |
| Gemma 3 1B | Q4_K_M | 0.8 GB | ~80-100 tok/s | Siêu nhẹ, nhanh nhất |
| Qwen2.5 3B | Q4_K_M | 2.0 GB | ~40-50 tok/s | Tiếng Việt + code tốt |
| Qwen2.5 7B | Q4_K_M | 4.7 GB | ~15-22 tok/s | Thông minh nhất |
| Llama 3.2 3B | Q4_K_M | 2.0 GB | ~40-55 tok/s | Nhanh, phổ biến |
| Phi-3.5 mini | Q4_K_M | 2.4 GB | ~30-40 tok/s | Suy luận, toán, code |

> Tốc độ là ước tính trên Snapdragon 8s Gen 4. Model tải vào internal storage
> (`/data/data/com.phoneai.local/files/`), không cần quyền truy cập file ngoài.
> Nếu URL HuggingFace thay đổi gây lỗi tải, cập nhật `downloadUrl` trong
> `ModelCatalog.kt`.

## Tại sao không dùng Rust?

Tốc độ sinh token do **compute kernel của llama.cpp** (Vulkan shader / CPU SIMD)
quyết định — đã là C++ tối ưu cao nhất. Lớp cầu nối JNI chỉ tốn vài µs nên viết
lại bằng Rust **không tăng tốc** mà chỉ thêm độ phức tạp build (cargo-ndk,
cross-compile). Cách tăng tốc thật sự: chỉnh `n_gpu_layers`, quantization,
context size — đã được app tự điều chỉnh theo RAM máy.

## Build

```bash
# Yêu cầu: Android Studio Hedgehog+, NDK 26+, CMake 3.22+
./gradlew assembleDebug
```

## Cấu trúc project

```
app/src/main/
├── cpp/
│   ├── CMakeLists.txt      # Build llama.cpp với Vulkan
│   └── llama_jni.cpp       # JNI bridge → llama.cpp API
├── java/com/phoneai/local/
│   ├── llm/
│   │   ├── LlamaEngine.kt      # JNI wrapper + Kotlin Flow streaming
│   │   └── InferenceManager.kt # API cấp cao, GPU offload thích ứng
│   ├── model/
│   │   ├── ChatMessage.kt      # Data class + PromptTemplates (đa họ model)
│   │   ├── ModelConfig.kt      # Metadata 1 model
│   │   └── ModelCatalog.kt     # Danh mục model tải được
│   ├── ui/
│   │   ├── ChatActivity.kt           # Màn hình chat
│   │   ├── ChatViewModel.kt          # StateFlow ViewModel
│   │   ├── MessageAdapter.kt         # RecyclerView + Markdown
│   │   ├── ModelSelectionActivity.kt # Màn hình chọn/tải model
│   │   ├── ModelSelectionViewModel.kt# Quản lý tải xuống
│   │   └── ModelAdapter.kt           # Card model + badge độ phù hợp
│   └── utils/
│       ├── DeviceInfo.kt       # RAM check, đánh giá độ phù hợp, GPU layers
│       └── ModelDownloader.kt  # Tải HF có resume
```

## Tuning cho Snapdragon 8s Gen 4

| Tham số | Giá trị | Lý do |
|---|---|---|
| `n_gpu_layers` | tự động | Tính theo RAM trống (DeviceInfo), offload tối đa lên Adreno |
| `n_threads` | 4 | Để lại core cho UI thread |
| `n_ctx` | 4096 | ~600 MB KV cache, cân bằng tốt |
| Quantization | Q4_K_M | Cân bằng size / tốc độ / chất lượng |

Chat template được chọn tự động theo họ model (Gemma / ChatML cho Qwen /
Llama 3 / Phi-3) trong `PromptTemplates`.

## License

Apache 2.0
