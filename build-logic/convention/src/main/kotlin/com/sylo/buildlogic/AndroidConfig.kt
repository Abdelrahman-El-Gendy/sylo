package com.sylo.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/** Single source of truth for the Android SDK levels used across every module. */
object AndroidConfig {
    // AndroidX (core-ktx 1.19, lifecycle 2.11, …) require compiling against API 37.
    const val COMPILE_SDK = 37
    // API 28 (Android 9): required by the Credential Manager Digital Credentials
    // (verified email) flow; also the floor for hardware-backed keystore StrongBox.
    const val MIN_SDK = 28
    const val TARGET_SDK = 35
    val JAVA_VERSION = JavaVersion.VERSION_11
}

/**
 * Shared Kotlin + Android base config: SDK levels, Java compatibility and the
 * Kotlin JVM target. Called by both the library and application conventions.
 */
internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension,
) {
    // AGP 9's CommonExtension exposes these as properties (getters), not `{ }`
    // lambda blocks — those only exist on the concrete Library/Application subtypes.
    commonExtension.compileSdk = AndroidConfig.COMPILE_SDK
    commonExtension.defaultConfig.minSdk = AndroidConfig.MIN_SDK
    commonExtension.compileOptions.sourceCompatibility = AndroidConfig.JAVA_VERSION
    commonExtension.compileOptions.targetCompatibility = AndroidConfig.JAVA_VERSION

    extensions.getByType<KotlinAndroidProjectExtension>().apply {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
}

/**
 * Enables Compose and wires in the BOM + core Compose dependencies so that every
 * Compose-enabled module gets a consistent, version-aligned set.
 */
internal fun Project.configureAndroidCompose(
    commonExtension: CommonExtension,
) {
    commonExtension.buildFeatures.compose = true

    dependencies {
        val bom = libs.findLibrary("androidx-compose-bom").get()
        add("implementation", platform(bom))
        add("androidTestImplementation", platform(bom))

        add("implementation", libs.findLibrary("androidx-compose-ui").get())
        add("implementation", libs.findLibrary("androidx-compose-ui-graphics").get())
        add("implementation", libs.findLibrary("androidx-compose-material3").get())
        add("implementation", libs.findLibrary("androidx-compose-ui-tooling-preview").get())

        add("debugImplementation", libs.findLibrary("androidx-compose-ui-tooling").get())
        add("debugImplementation", libs.findLibrary("androidx-compose-ui-test-manifest").get())
        add("androidTestImplementation", libs.findLibrary("androidx-compose-ui-test-junit4").get())
    }
}
