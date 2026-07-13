// Top-level build file. Plugins are declared here with `apply false` so their
// versions are resolved once (from the version catalog) and then applied per
// module — either directly or, preferably, through the :build-logic convention
// plugins.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
