import com.android.build.api.dsl.LibraryExtension
import com.sylo.buildlogic.configureKotlinAndroid
import com.sylo.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Base convention for every Android library module (all :core and :feature libs).
 * Applies AGP + Kotlin, shared SDK/Java config, and common test deps.
 */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        // AGP 9 provides Kotlin via built-in Kotlin support, so applying
        // com.android.library also registers the Kotlin `kotlin` extension.
        pluginManager.apply("com.android.library")

        extensions.configure<LibraryExtension> {
            configureKotlinAndroid(this)
            // NOTE: AGP 9 dropped `targetSdk` from library modules — only the app
            // module declares it. Libraries just compile against compileSdk.
            defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        dependencies {
            add("implementation", libs.findLibrary("timber").get())
            add("testImplementation", libs.findLibrary("junit").get())
            add("androidTestImplementation", libs.findLibrary("androidx-junit").get())
        }
    }
}
