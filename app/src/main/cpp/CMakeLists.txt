cmake_minimum_required(VERSION 3.22.1)
project(phoneai)

# ── llama.cpp via FetchContent ─────────────────────────────────────────────────
include(FetchContent)
FetchContent_Declare(
    llama_cpp
    GIT_REPOSITORY https://github.com/ggerganov/llama.cpp.git
    GIT_TAG        b3447          # pinned stable tag
    GIT_SHALLOW    TRUE
)
set(GGML_VULKAN ON  CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS    OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
FetchContent_MakeAvailable(llama_cpp)

# ── JNI bridge ────────────────────────────────────────────────────────────────
add_library(phoneai SHARED llama_jni.cpp)

target_include_directories(phoneai PRIVATE
    ${llama_cpp_SOURCE_DIR}
    ${llama_cpp_SOURCE_DIR}/ggml/include
)

target_link_libraries(phoneai
    llama
    ggml
    android
    log
    vulkan
)
