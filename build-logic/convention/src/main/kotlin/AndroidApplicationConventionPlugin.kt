import com.android.build.api.dsl.ApplicationExtension
import com.sylo.buildlogic.AndroidConfig
import com.sylo.buildlogic.configureKotlinAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Base convention for the :app module. Applies AGP application + Kotlin and the
 * shared SDK/Java config. Module-specific bits (applicationId, versionCode,
 * buildTypes) stay in app/build.gradle.kts.
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        // AGP 9 provides Kotlin via built-in Kotlin support (applying the standalone
        // org.jetbrains.kotlin.android plugin is incompatible with AGP 9).
        pluginManager.apply("com.android.application")

        extensions.configure<ApplicationExtension> {
            configureKotlinAndroid(this)
            defaultConfig.targetSdk = AndroidConfig.TARGET_SDK
        }
    }
}
