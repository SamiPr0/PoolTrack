import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.androidApplication)
  alias(libs.plugins.ktfmt)
  alias(libs.plugins.kotlinCompose)
  alias(libs.plugins.kotlinSerialization)
}

android {
  namespace = "com.github.se.pooltrack"
  compileSdk = 37

  defaultConfig {
    applicationId = "com.github.se.pooltrack"
    minSdk = 29
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    vectorDrawables { useSupportLibrary = true }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(
          getDefaultProguardFile("proguard-android-optimize.txt"),
          "proguard-rules.pro",
      )
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  buildFeatures { compose = true }
}

// With AGP 9+ we have to set the JVM target on a kotlin block outside the Android block.
kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

dependencies {
  // Version alignment. Every androidx.compose.* artifact below takes its version from this BOM.
  implementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(platform(libs.androidx.compose.bom))

  // Core
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.kotlinx.serialization.json)

  // Jetpack Compose UI
  implementation(libs.androidx.ui)
  implementation(libs.androidx.ui.tooling.preview)
  implementation(libs.androidx.ui.graphics)
  implementation(libs.androidx.material3)
  implementation(libs.androidx.material.icons.core)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  debugImplementation(libs.androidx.ui.tooling)
  debugImplementation(libs.androidx.ui.test.manifest)

  // Navigation
  implementation(libs.androidx.navigation.compose)

  // Testing
  testImplementation(libs.junit)
  testImplementation(libs.robolectric)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.ui.test.junit4)
}
