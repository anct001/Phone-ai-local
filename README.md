# Phone AI — Local LLM on Android

Chạy **Gemma 3 4B** hoàn toàn offline trên Android với Snapdragon 8s Gen 4.  
Engine: **llama.cpp** + Vulkan backend (Adreno 740 GPU offload).

## Yêu cầu

- Android 10+ (API 29)
- RAM: 6 GB khả dụng trở lên (16 GB total)
- Chip: Snapdragon 8s Gen 4 hoặc tương đương (Vulkan 1.1)
- Dung lượng: ~3 GB cho model file

## Cài đặt model

1. Tải file GGUF từ HuggingFace:

```
https://huggingface.co/bartowski/gemma-3-4b-it-GGUF
```

| File | Size | Tốc độ | Chất lượng |
|---|---|---|---|
| `gemma-3-4b-it-Q4_K_M.gguf` | 2.5 GB | ~35 tok/s | ⭐⭐⭐⭐ ← Khuyến nghị |
| `gemma-3-4b-it-Q5_K_M.gguf` | 3.0 GB | ~25 tok/s | ⭐⭐⭐⭐⭐ |
| `gemma-3-4b-it-Q3_K_M.gguf` | 2.0 GB | ~50 tok/s | ⭐⭐⭐ |

2. Sao chép file vào thư mục internal storage của app:

```bash
adb push gemma-3-4b-it-Q4_K_M.gguf /data/data/com.phoneai.local/files/
```

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
│   │   └── InferenceManager.kt # High-level API, chat template
│   ├── model/
│   │   ├── ChatMessage.kt      # Data class + Gemma 3 chat template
│   │   └── ModelConfig.kt      # Preset configs (Q3/Q4/Q5)
│   ├── ui/
│   │   ├── ChatActivity.kt     # Main UI
│   │   ├── ChatViewModel.kt    # StateFlow ViewModel
│   │   └── MessageAdapter.kt   # RecyclerView + Markdown render
│   └── utils/
│       ├── DeviceInfo.kt       # RAM check, GPU layer suggestion
│       └── ModelDownloader.kt  # Resume-capable HF downloader
```

## Tuning cho Snapdragon 8s Gen 4

| Tham số | Giá trị | Lý do |
|---|---|---|
| `n_gpu_layers` | 28 | Offload lên Adreno 740, tiết kiệm CPU |
| `n_threads` | 4 | Để lại core cho UI thread |
| `n_ctx` | 4096 | ~600 MB KV cache, cân bằng tốt |
| Quantization | Q4_K_M | 2.5 GB, ~35 tok/s, chất lượng tốt |

## License

Apache 2.0
