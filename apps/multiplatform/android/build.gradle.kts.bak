@file:Suppress("UnstableApiUsage")

plugins {
  id("com.android.application")
  id("org.jetbrains.compose")
  kotlin("android")
  id("org.jetbrains.kotlin.plugin.serialization")
  id("org.jetbrains.kotlin.plugin.compose")
}

android {
lint {abortOnError = false}
lint {abortOnError = false}
lint {abortOnError = false}
lint {abortOnError = false}
lint {abortOnError = false}
lint {abortOnError = false}
lint {abortOnError = false}
lint {abortOnError = false}
lint {abortOnError = false}
lint {abortOnError = false}
  compileSdk = 35

  defaultConfig {
    applicationId = "com.arpokrat.app"
    namespace = "com.arpokrat.app"
    minSdk = 26
    targetSdk = 35
    versionCode = (extra["android.version_code"] as String).toInt()
    versionName = extra["android.version_name"] as String

    testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
    vectorDrawables {
      useSupportLibrary = true
    }
    externalNativeBuild {
      cmake {
        cppFlags("")
      }
    }
    manifestPlaceholders["app_name"] = "@string/app_name"
    manifestPlaceholders["provider_authorities"] = "com.arpokrat.app.provider"
    manifestPlaceholders["extract_native_libs"] = rootProject.extra["compression.level"] as Int != 0
  }

  buildTypes {
    debug {
      applicationIdSuffix = rootProject.extra["application_id.suffix"] as String
      isDebuggable = rootProject.extra["enable_debuggable"] as Boolean
      manifestPlaceholders["app_name"] = rootProject.extra["app.name"] as String
      manifestPlaceholders["provider_authorities"] = "com.arpokrat.app${rootProject.extra["application_id.suffix"]}.provider"
    }
    release {
      isMinifyEnabled = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  kotlinOptions {
    freeCompilerArgs += "-opt-in=kotlinx.coroutines.DelicateCoroutinesApi"
    freeCompilerArgs += "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi"
    freeCompilerArgs += "-opt-in=androidx.compose.ui.text.ExperimentalTextApi"
    freeCompilerArgs += "-opt-in=androidx.compose.material.ExperimentalMaterialApi"
    freeCompilerArgs += "-opt-in=com.google.accompanist.insets.ExperimentalAnimatedInsets"
    freeCompilerArgs += "-opt-in=com.google.accompanist.permissions.ExperimentalPermissionsApi"
    freeCompilerArgs += "-opt-in=kotlinx.serialization.InternalSerializationApi"
    freeCompilerArgs += "-opt-in=kotlinx.serialization.ExperimentalSerializationApi"
  }
  externalNativeBuild {
    cmake {
      path(File("../common/src/commonMain/cpp/android/CMakeLists.txt"))
    }
  }
  buildTypes {
    getByName("release") {
      isMinifyEnabled = true
    }
  }
  buildFeatures {
    buildConfig = true
  }
  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
      pickFirsts.add("google/protobuf/**")
      pickFirsts.add("google/protobuf/field_mask.proto")
      pickFirsts.add("google/protobuf/any.proto")
      pickFirsts.add("google/protobuf/api.proto")
      pickFirsts.add("google/protobuf/descriptor.proto")
      pickFirsts.add("google/protobuf/duration.proto")
      pickFirsts.add("google/protobuf/empty.proto")
      pickFirsts.add("google/protobuf/source_context.proto")
      pickFirsts.add("google/protobuf/struct.proto")
      pickFirsts.add("google/protobuf/timestamp.proto")
      pickFirsts.add("google/protobuf/type.proto")
      pickFirsts.add("google/protobuf/wrappers.proto")
      pickFirsts.add("META-INF/maven/com.google.protobuf/**")
      pickFirsts.add("META-INF/DEPENDENCIES")
      pickFirsts.add("META-INF/LICENSE")
      pickFirsts.add("META-INF/LICENSE.txt")
      pickFirsts.add("META-INF/NOTICE")
    }
    jniLibs.useLegacyPackaging = true
  }

  configurations.all {
    exclude(group = "com.google.protobuf", module = "protobuf-java")
  }

  android.sourceSets["main"].assets.setSrcDirs(listOf("../common/src/commonMain/resources/assets"))
  val isRelease = gradle.startParameter.taskNames.find { it.lowercase().contains("release") } != null
  val isBundle = gradle.startParameter.taskNames.find { it.lowercase().contains("bundle") } != null
  android.defaultConfig.resourceConfigurations += listOf(
    "en",
    "ar",
    "bg",
    "ca",
    "cs",
    "de",
    "es",
    "fa",
    "fi",
    "fr",
    "hu",
    "in",
    "it",
    "iw",
    "ja",
    "lt",
    "nl",
    "pl",
    "pt",
    "pt-rBR",
    "ro",
    "ru",
    "th",
    "tr",
    "uk",
    "vi",
    "zh-rCN",
    "zh-rTW"
  )
  ndkVersion = "23.1.7779620"
  if (isBundle) {
    defaultConfig.ndk.abiFilters("arm64-v8a")
  } else {
    splits {
      abi {
        isEnable = true
        reset()
        if (isRelease) {
          include("arm64-v8a")
        } else {
          include("arm64-v8a")
          isUniversalApk = false
        }
      }
    }
  }
}

dependencies {
  implementation(project(":common"))

  implementation("androidx.core:core-ktx:1.13.1")
  implementation("androidx.appcompat:appcompat:1.7.0")
  implementation("androidx.activity:activity-compose:1.9.1")
  implementation("androidx.work:work-multiprocess:2.9.1")
  implementation("com.jakewharton:process-phoenix:3.0.0")
  implementation("androidx.lifecycle:lifecycle-process:2.8.4")
  implementation("com.google.accompanist:accompanist-permissions:0.34.0")

  // Testing
  testImplementation("junit:junit:4.13.2")
  androidTestImplementation("androidx.test.ext:junit:1.2.1")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
  debugImplementation("androidx.compose.ui:ui-tooling:1.6.4")
}

