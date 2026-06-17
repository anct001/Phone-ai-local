plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.phoneai.local"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.phoneai.local"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            // Only arm64-v8a — Snapdragon 8s Gen 4 is 64-bit only
            abiFilters += setOf("arm64-v8a")
        }

        externalNativeBuild {
            cmake {
                // -O3 + ARM NEON (mandatory on arm64-v8a, no extra flags needed)
                // Vulkan disabled: requires glslc on the host — install Vulkan SDK
                // and flip GGML_VULKAN to ON in CMakeLists.txt to enable GPU offload.
                cppFlags += "-std=c++17 -O3"
                arguments += listOf(
                    "-DGGML_VULKAN=OFF",
                    "-DGGML_NATIVE=OFF",
                    "-DLLAMA_BUILD_TESTS=OFF",
                    "-DLLAMA_BUILD_EXAMPLES=OFF",
                    "-DLLAMA_BUILD_SERVER=OFF"
                )
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.ktx)   // required for by viewModels() delegate
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.coroutines.android)
    implementation(libs.okhttp)                  // model downloader
    implementation(libs.gson)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.markwon.core)
    annotationProcessor(libs.room.compiler)
}
