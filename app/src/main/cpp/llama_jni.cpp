#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <atomic>
#include <thread>

#include "llama.h"
#include "ggml.h"

#define TAG "PhoneAI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static llama_model*   g_model   = nullptr;
static llama_context* g_ctx     = nullptr;
static std::atomic<bool> g_stop_requested{false};

extern "C" {

// ── Load model ────────────────────────────────────────────────────────────────
JNIEXPORT jboolean JNICALL
Java_com_phoneai_local_llm_LlamaEngine_nativeLoadModel(
        JNIEnv* env, jobject /*thiz*/,
        jstring model_path,
        jint    n_ctx,
        jint    n_threads,
        jint    n_gpu_layers)
{
    const char* path = env->GetStringUTFChars(model_path, nullptr);
    LOGI("Loading model: %s", path);

    llama_backend_init();

    llama_model_params mparams = llama_model_default_params();
    mparams.n_gpu_layers = n_gpu_layers;   // layers offloaded to Vulkan GPU
    mparams.use_mmap     = true;           // memory-map file — saves RAM

    g_model = llama_load_model_from_file(path, mparams);
    env->ReleaseStringUTFChars(model_path, path);

    if (!g_model) {
        LOGE("Failed to load model");
        return JNI_FALSE;
    }

    llama_context_params cparams = llama_context_default_params();
    cparams.n_ctx     = static_cast<uint32_t>(n_ctx);
    cparams.n_threads = static_cast<uint32_t>(n_threads);
    // Vulkan backend handles GPU threading automatically

    g_ctx = llama_new_context_with_model(g_model, cparams);
    if (!g_ctx) {
        LOGE("Failed to create context");
        llama_free_model(g_model);
        g_model = nullptr;
        return JNI_FALSE;
    }

    LOGI("Model loaded. Context size: %d", n_ctx);
    return JNI_TRUE;
}

// ── Generate (streaming via callback) ─────────────────────────────────────────
JNIEXPORT void JNICALL
Java_com_phoneai_local_llm_LlamaEngine_nativeGenerate(
        JNIEnv* env, jobject thiz,
        jstring prompt_str,
        jint    max_new_tokens,
        jfloat  temperature,
        jfloat  top_p)
{
    if (!g_model || !g_ctx) {
        LOGE("Model not loaded");
        return;
    }

    g_stop_requested.store(false);

    const char* prompt = env->GetStringUTFChars(prompt_str, nullptr);

    // Tokenize
    std::vector<llama_token> tokens(llama_n_ctx(g_ctx));
    int n_tokens = llama_tokenize(
        g_model, prompt, static_cast<int>(strlen(prompt)),
        tokens.data(), static_cast<int>(tokens.size()),
        /*add_special=*/true, /*parse_special=*/false);
    env->ReleaseStringUTFChars(prompt_str, prompt);

    if (n_tokens < 0) {
        LOGE("Tokenization failed");
        return;
    }
    tokens.resize(n_tokens);

    // Evaluate prompt
    if (llama_decode(g_ctx, llama_batch_get_one(tokens.data(), n_tokens)) != 0) {
        LOGE("llama_decode failed");
        return;
    }

    // Sampler chain: temp + top-p + greedy
    llama_sampler* smpl = llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add(smpl, llama_sampler_init_temp(temperature));
    llama_sampler_chain_add(smpl, llama_sampler_init_top_p(top_p, 1));
    llama_sampler_chain_add(smpl, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));

    // Java callback: void onToken(String token)
    jclass  clazz  = env->GetObjectClass(thiz);
    jmethodID onTok = env->GetMethodID(clazz, "onToken", "(Ljava/lang/String;)V");

    char piece[32];
    for (int i = 0; i < max_new_tokens && !g_stop_requested.load(); ++i) {
        llama_token tok = llama_sampler_sample(smpl, g_ctx, -1);

        if (llama_token_is_eog(g_model, tok)) break;

        int len = llama_token_to_piece(g_model, tok, piece, sizeof(piece), 0, false);
        if (len > 0) {
            piece[len] = '\0';
            jstring jtok = env->NewStringUTF(piece);
            env->CallVoidMethod(thiz, onTok, jtok);
            env->DeleteLocalRef(jtok);
        }

        llama_batch batch = llama_batch_get_one(&tok, 1);
        if (llama_decode(g_ctx, batch) != 0) break;
    }

    llama_sampler_free(smpl);
    llama_kv_cache_clear(g_ctx);
}

// ── Stop generation ───────────────────────────────────────────────────────────
JNIEXPORT void JNICALL
Java_com_phoneai_local_llm_LlamaEngine_nativeStop(
        JNIEnv* /*env*/, jobject /*thiz*/)
{
    g_stop_requested.store(true);
}

// ── Free model ────────────────────────────────────────────────────────────────
JNIEXPORT void JNICALL
Java_com_phoneai_local_llm_LlamaEngine_nativeFree(
        JNIEnv* /*env*/, jobject /*thiz*/)
{
    if (g_ctx)   { llama_free(g_ctx);        g_ctx   = nullptr; }
    if (g_model) { llama_free_model(g_model); g_model = nullptr; }
    llama_backend_free();
    LOGI("Model freed");
}

// ── Context size query ─────────────────────────────────────────────────────────
JNIEXPORT jint JNICALL
Java_com_phoneai_local_llm_LlamaEngine_nativeGetContextSize(
        JNIEnv* /*env*/, jobject /*thiz*/)
{
    return g_ctx ? static_cast<jint>(llama_n_ctx(g_ctx)) : 0;
}

} // extern "C"
