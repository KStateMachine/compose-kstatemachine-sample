import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    kotlin("plugin.serialization") version "1.5.30"
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    sourceSets {
        
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.android)
            implementation(libs.koin.compose)
            implementation(libs.koin.androidx.compose)
        }
        commonMain.dependencies {
            implementation(libs.kermit)
            implementation(libs.landscapist.coil3)

            api(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.navigator.koin)
            implementation(libs.navigator.screen.model)

            // Ktor for networking
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.cio) // or another engine
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.serialization.kotlinx.json)

//            implementation(libs.kstatemachine)
//            implementation(libs.kstatemachine.coroutines)
            val voyagerVersion = "1.1.0-beta02"

            implementation("cafe.adriel.voyager:voyager-screenmodel:$voyagerVersion")
//            implementation("cafe.adriel.voyager:voyager-navigator:$voyagerVersion")
//            implementation("cafe.adriel.voyager:voyager-bottom-sheet-navigator:$voyagerVersion")
//            implementation("cafe.adriel.voyager:voyager-tab-navigator:$voyagerVersion")
//            implementation("cafe.adriel.voyager:voyager-transitions:$voyagerVersion")
            //implementation("cafe.adriel.voyager:voyager-lifecycle-kmp:$voyagerVersion")
//            implementation("media.kamel:kamel-image:0.9.5")
            implementation(libs.kstatemachine)
            implementation(libs.kstatemachine.coroutines)


            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
        }
    }
}

android {
    namespace = "org.example.project"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        applicationId = "org.example.project"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    dependencies {
        debugImplementation(compose.uiTooling)
    }
}
dependencies {
    implementation(libs.androidx.startup.runtime)
}

