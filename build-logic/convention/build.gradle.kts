import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "com.packeta.convention.buildlogic"

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.android.tools.common)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.androidx.room.gradle.plugin)
    implementation(libs.buildkonfig.gradlePlugin)
    implementation(libs.buildkonfig.compiler)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("androidApplication"){
            id = "com.packeta.orbis.convention.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }

        register("androidComposeApplication"){
            id = "com.packeta.orbis.convention.android.application.compose"
            implementationClass = "AndroidApplicationComposeConventionPlugin"
        }

        register("kmpApplicationCompose"){
            id = "com.packeta.orbis.convention.kmp.application.compose"
            implementationClass = "KmpApplicationComposeConventionPlugin"
        }

        register("kmpLibrary"){
            id = "com.packeta.orbis.convention.kmp.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }

        register("kmpLibraryCompose"){
            id = "com.packeta.orbis.convention.kmp.library.compose"
            implementationClass = "KmpLibraryComposeConventionPlugin"
        }

        register("buildKonfig"){
            id = "com.packeta.orbis.convention.buildkonfig"
            implementationClass = "BuildKonfigConventionPlugin"
        }

        register("room"){
            id = "com.packeta.orbis.convention.room"
            implementationClass = "RoomConventionPlugin"
        }
    }
}