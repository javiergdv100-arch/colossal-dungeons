plugins {
    id("net.neoforged.gradle.userdev") version "${property("neo_gradle_version")}"
    java
}

version = property("mod_version") as String
group = property("mod_group_id") as String

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

minecraft {
    accessTransformers {
        file("src/main/resources/META-INF/accesstransformer.cfg")
    }
}

runs {
    configureEach {
        systemProperty("forge.logging.markers", "REGISTRIES")
        systemProperty("forge.logging.console.level", "debug")
        modSource(project.sourceSets.main.get())
    }

    create("client") {
        systemProperty("forge.enabledGameTestNamespaces", property("mod_id") as String)
    }

    create("server") {
        systemProperty("forge.enabledGameTestNamespaces", property("mod_id") as String)
        programArgument("--nogui")
    }

    create("data") {
        programArguments.addAll(
            "--mod", property("mod_id") as String,
            "--all",
            "--output", file("src/generated/resources/").absolutePath,
            "--existing", file("src/main/resources/").absolutePath
        )
    }
}

sourceSets.main.get().resources.srcDir("src/generated/resources")

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        name = "GeckoLib"
        url = uri("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/")
        content {
            includeGroup("software.bernie.geckolib")
        }
    }
    maven {
        name = "Curios"
        url = uri("https://maven.theillusivec4.top/")
        content {
            includeGroup("top.theillusivec4.curios")
        }
    }
    maven {
        name = "SmartBrainLib"
        url = uri("https://maven.tslat.net/")
        content {
            includeGroup("net.tslat.smartbrainlib")
        }
    }
    maven {
        name = "Modrinth Maven"
        url = uri("https://api.modrinth.com/maven")
        content {
            includeGroup("maven.modrinth")
        }
    }
    maven {
        name = "CurseMaven"
        url = uri("https://cursemaven.com")
        content {
            includeGroup("curse.maven")
        }
    }
}

dependencies {
    // NeoForge
    implementation("net.neoforged:neoforge:${property("neoforge_version")}")

    // GeckoLib - Entity animations
    implementation("software.bernie.geckolib:geckolib-neoforge:${property("geckolib_version")}")

    // SmartBrainLib - Advanced entity AI
    implementation("net.tslat.smartbrainlib:SmartBrainLib-neoforge-1.21.1:${property("smartbrainlib_version")}")

    // Curios API - Equipment slots
    compileOnly("top.theillusivec4.curios:curios-neoforge:${property("curios_version")}:api")
    runtimeOnly("top.theillusivec4.curios:curios-neoforge:${property("curios_version")}")

    // AAA Particles (shimmermare)
    implementation("maven.modrinth:aaa-particles:1.0.4-neoforge")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.withType<ProcessResources>().configureEach {
    val replaceProperties = mapOf(
        "minecraft_version" to property("minecraft_version") as String,
        "neoforge_version" to property("neoforge_version") as String,
        "mod_id" to property("mod_id") as String,
        "mod_name" to property("mod_name") as String,
        "mod_version" to property("mod_version") as String,
        "mod_authors" to property("mod_authors") as String,
        "mod_description" to property("mod_description") as String,
        "mod_license" to property("mod_license") as String
    )
    inputs.properties(replaceProperties)
    filesMatching(listOf("META-INF/neoforge.mods.toml", "pack.mcmeta")) {
        expand(replaceProperties)
    }
}
