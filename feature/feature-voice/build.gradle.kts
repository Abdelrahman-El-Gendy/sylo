plugins {
    alias(libs.plugins.sylo.android.feature)
}

android {
    namespace = "com.sylo.feature.voice"
}

dependencies {
    // Voice capture parses speech into a transaction and saves it to the encrypted
    // local DB — so this feature depends on :core-database (a core module, allowed).
    implementation(project(":core:core-database"))
    implementation(libs.androidx.activity.compose)
}
