plugins {
    alias(libs.plugins.sylo.android.library)
    alias(libs.plugins.sylo.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.sylo.core.network"

    buildFeatures {
        // Exposes BuildConfig.DEBUG so we only enable HTTP body logging in debug
        // builds (never log bearer tokens in release).
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:core-common"))

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
}
