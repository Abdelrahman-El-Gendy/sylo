plugins {
    alias(libs.plugins.sylo.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.sylo.core.navigation"
}

dependencies {
    // Route contracts are @Serializable types implementing Nav3's NavKey. The
    // entry/NavDisplay wiring lives in the features/app that consume Navigation 3.
    implementation(libs.kotlinx.serialization.json)
    api(libs.androidx.navigation3.runtime)
}
