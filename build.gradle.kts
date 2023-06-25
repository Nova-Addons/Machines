import org.gradle.configurationcache.extensions.capitalized
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "xyz.xenondevs"
version = "0.4.2"

val mojangMapped = project.hasProperty("mojang-mapped")

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.nova)
    alias(libs.plugins.stringremapper)
    alias(libs.plugins.specialsource)
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.xenondevs.xyz/releases")
    
    // include xenondevs-nms repository if requested
    if (project.hasProperty("xenondevsNms")) {
        maven("https://repo.papermc.io/repository/maven-public/") // authlib, brigadier, etc.
        maven {
            name = "xenondevsNms"
            url = uri("https://repo.xenondevs.xyz/nms/")
            credentials(PasswordCredentials::class)
        }
    }
}

dependencies {
    implementation(libs.nova)
    implementation("xyz.xenondevs:simple-upgrades:1.0-SNAPSHOT")
}

addon {
    id.set(project.name)
    name.set(project.name.capitalized())
    version.set(project.version.toString())
    novaVersion.set(libs.versions.nova)
    main.set("xyz.xenondevs.nova.machines.Machines")
    depend.add("simple_upgrades")
    authors.addAll("StudioCode", "ByteZ", "Javahase")
}

spigotRemap {
    spigotVersion.set(libs.versions.spigot.get().substringBefore('-'))
    sourceJarTask.set(tasks.jar)
}

remapStrings {
    remapGoal.set(if (mojangMapped) "mojang" else "spigot")
    spigotVersion.set(libs.versions.spigot.get())
}

generateWailaTextures {
    filter.set { !it.name.contains(Regex("\\d")) }
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
        }
    }
    
    register<Copy>("addonJar") {
        group = "build"
        dependsOn("addon", if (mojangMapped) "jar" else "remapObfToSpigot")
        
        from(File(File(project.buildDir, "libs"), "${project.name}-${project.version}.jar"))
        into((project.findProperty("outDir") as? String)?.let(::File) ?: project.buildDir)
        rename { it.replace(project.name, addon.get().addonName.get()) }
    }
}