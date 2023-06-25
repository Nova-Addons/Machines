rootProject.name = "machines"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("nova", "0.14")
            version("spigot", "1.20.1-R0.1-SNAPSHOT")
            version("kotlin", "1.8.22")
            
            plugin("kotlin", "org.jetbrains.kotlin.jvm").versionRef("kotlin")
            plugin("nova", "xyz.xenondevs.nova.nova-gradle-plugin").versionRef("nova")
            plugin("stringremapper", "xyz.xenondevs.string-remapper-gradle-plugin").version("1.3")
            plugin("specialsource", "xyz.xenondevs.specialsource-gradle-plugin").version("1.1")
            
            library("nova", "xyz.xenondevs.nova", "nova").versionRef("nova")
        }
    }
}

pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://repo.xenondevs.xyz/releases")
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") // for nova-gradle-plugin
    }
}

plugins {
    id("com.gradle.enterprise") version "3.13"
}

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
    }
}