plugins {
alias(libs.plugins.sylo.android.library)
    alias(libs.plugins.sylo.android.hilt)
}

android {
    namespace = "com.sylo.core.common"
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.kotlinx.coroutines.test)
}
