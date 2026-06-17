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

# CPU-only build — no Vulkan SDK required on the Windows host machine.
# To enable Vulkan later: install Vulkan SDK, add glslc to PATH,
# then flip GGML_VULKAN to ON and add `vulkan` to target_link_libraries.
set(GGML_VULKAN  OFF CACHE BOOL "" FORCE)
set(GGML_METAL   OFF CACHE BOOL "" FORCE)
set(GGML_CUDA    OFF CACHE BOOL "" FORCE)
set(GGML_OPENCL  OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP  OFF CACHE BOOL "" FORCE)   # NDK OpenMP linkage is fragile
set(GGML_NATIVE  OFF CACHE BOOL "" FORCE)   # must be OFF for cross-compilation

# ARM NEON is mandatory on arm64-v8a — llama.cpp enables it automatically.
set(GGML_F16C OFF CACHE BOOL "" FORCE)      # x86-only flag, must be OFF
set(GGML_AVX  OFF CACHE BOOL "" FORCE)
set(GGML_AVX2 OFF CACHE BOOL "" FORCE)

set(LLAMA_BUILD_TESTS    OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER   OFF CACHE BOOL "" FORCE)
set(BUILD_SHARED_LIBS    OFF CACHE BOOL "" FORCE)

FetchContent_MakeAvailable(llama_cpp)

# ── JNI bridge ────────────────────────────────────────────────────────────────
add_library(phoneai SHARED llama_jni.cpp)

target_include_directories(phoneai PRIVATE
    ${llama_cpp_SOURCE_DIR}
    ${llama_cpp_SOURCE_DIR}/ggml/include
    ${llama_cpp_SOURCE_DIR}/include
)

target_link_libraries(phoneai
    llama
    ggml
    android
    log
)
