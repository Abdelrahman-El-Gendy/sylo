plugins {
    alias(libs.plugins.sylo.android.library)
    alias(libs.plugins.sylo.android.hilt)
}

android {
    namespace = "com.sylo.core.security"
}

dependencies {
    implementation(project(":core:core-common"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.security.crypto)
    // api: BiometricAuthenticator's public signature exposes FragmentActivity, so
    // consumers (feature-auth, app) need biometric/fragment on their compile path.
    api(libs.androidx.biometric)
    api(libs.androidx.fragment.ktx)
    // Credential Manager: verified-email retrieval via the Digital Credentials API.
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.kotlinx.coroutines.core)
}
