plugins {
    alias(libs.plugins.sylo.android.library)
    alias(libs.plugins.sylo.android.compose)
}

android {
    namespace = "com.sylo.core.ui"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    // api: the icon set is part of the design system, so every feature that depends
    // on core-ui gets Material Symbols without re-declaring the dependency.
    api(libs.androidx.compose.material.icons.extended)
}
