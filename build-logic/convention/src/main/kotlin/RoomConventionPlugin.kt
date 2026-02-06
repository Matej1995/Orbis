import androidx.room.gradle.RoomExtension
import com.packeta.orbis.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.internal.declarativedsl.schemaBuilder.schemaFromTypes
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class RoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.google.devtools.ksp")
                apply("androidx.room")
            }

            extensions.configure<RoomExtension> {
                schemaDirectory("$projectDir/schemas")
            }

            dependencies {
                "commonMainApi"(libs.findLibrary("androidx-room-runtime").get())
                "commonMainApi"(libs.findLibrary("sqlite-bundled").get())
            }

            // KSP configurations are created lazily after KMP plugin evaluation,
            // so we need to defer adding KSP dependencies.
            afterEvaluate {
                dependencies {
                    "kspAndroid"(libs.findLibrary("androidx-room-compiler").get())
                    "kspIosSimulatorArm64"(libs.findLibrary("androidx-room-compiler").get())
                    "kspIosArm64"(libs.findLibrary("androidx-room-compiler").get())
                    "kspIosX64"(libs.findLibrary("androidx-room-compiler").get())
                }
            }
        }
    }
}