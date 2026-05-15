import java.io.File
import java.util.*

buildscript {
    val prop = java.util.Properties().apply {
        try {
            load(java.io.FileInputStream(File(rootProject.rootDir, "local.properties")))
        } catch (e: Exception) {
        }
    }
    fun ExtraPropertiesExtension.getOrNull(name: String): Any? = if (has(name)) get("name") else null

    extra.set("compose.version", prop["compose.version"] ?: extra["compose.version"])
    extra.set("kotlin.version", prop["kotlin.version"] ?: extra["kotlin.version"])
    extra.set("gradle.plugin.version", prop["gradle.plugin.version"] ?: extra["gradle.plugin.version"])
    extra.set("abi_filter", prop["abi_filter"] ?: "arm64-v8a")
    extra.set("app.name", prop["app.name"] ?: "@string/app_name")
    extra.set("enable_debuggable", prop["debuggable"] != "false")
    extra.set("application_id.suffix", prop["application_id.suffix"] ?: "")
    extra.set("compression.level", (prop["compression.level"] as String?)?.toIntOrNull() ?: 0)

    extra.set("desktop.mac.signing.identity", prop["desktop.mac.signing.identity"] ?: extra.getOrNull("compose.desktop.mac.signing.identity"))
    extra.set("desktop.mac.signing.keychain", prop["desktop.mac.signing.keychain"] ?: extra.getOrNull("compose.desktop.mac.signing.keychain"))
    extra.set("desktop.mac.notarization.apple_id", prop["desktop.mac.notarization.apple_id"] ?: extra.getOrNull("compose.desktop.mac.notarization.appleID"))
    extra.set("desktop.mac.notarization.password", prop["desktop.mac.notarization.password"] ?: extra.getOrNull("compose.desktop.mac.notarization.password"))
    extra.set("desktop.mac.notarization.team_id", prop["desktop.mac.notarization.team_id"] ?: extra.getOrNull("compose.desktop.mac.notarization.teamID"))

    dependencies {
        classpath("com.android.tools.build:gradle:${rootProject.extra["gradle.plugin.version"]}")
        classpath(kotlin("gradle-plugin", version = rootProject.extra["kotlin.version"] as String))
        classpath("dev.icerock.moko:resources-generator:0.23.0")
        classpath("com.squareup:kotlinpoet:1.16.0")
    }
}

group = "com.arpokrat"
version = extra["android.version_name"] as String

plugins {
    kotlin("multiplatform") apply false
    kotlin("android") apply false
    id("com.android.application") apply false
    id("com.android.library") apply false
    id("org.jetbrains.compose") apply false
    id("org.jetbrains.kotlin.plugin.serialization") apply false
}

val jvmVersion: Provider<String> = providers.gradleProperty("kotlin.jvm.target")

configure(subprojects) {
    plugins.withType<com.android.build.gradle.BasePlugin>().configureEach {
        extensions.findByType<com.android.build.gradle.BaseExtension>()?.apply {
            jvmVersion.map { JavaVersion.toVersion(it) }.orNull?.let {
                compileOptions {
                    sourceCompatibility = it
                    targetCompatibility = it
                }
            }
        }
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
            if (jvmVersion.isPresent) jvmTarget = jvmVersion.get()
        }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}