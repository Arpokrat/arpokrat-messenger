pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
    plugins {
        kotlin("multiplatform").version(extra["kotlin.version"] as String)
        kotlin("android").version(extra["kotlin.version"] as String)
        id("com.android.application").version(extra["gradle.plugin.version"] as String)
        id("com.android.library").version(extra["gradle.plugin.version"] as String)
        id("org.jetbrains.compose").version(extra["compose.version"] as String)
        id("org.jetbrains.kotlin.plugin.compose").version(extra["kotlin.version"] as String)
        id("org.jetbrains.kotlin.plugin.serialization").version(extra["kotlin.version"] as String)
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven { url = uri("https://jitpack.io") }

        maven {
            url = uri("https://maven.pkg.github.com/trustwallet/wallet-core")
            credentials {
                val props = java.util.Properties()
                val localPropsFile = file("local.properties")
                if (localPropsFile.exists()) {
                    props.load(localPropsFile.inputStream())
                }
                username = props.getProperty("gpr.user") ?: System.getenv("GITHUB_USER")
                password = props.getProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

rootProject.name = "app"
include(":android", ":desktop", ":common")