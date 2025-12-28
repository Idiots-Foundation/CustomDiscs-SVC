import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    id("java-library")
    id ("com.modrinth.minotaur") version "2.+"
    id("com.gradleup.shadow") version "9.3.0"
    id("de.eldoria.plugin-yml.paper") version "0.8.0"
}

allprojects {
    group = "space.subkek"
    version = properties["plugin_version"]!!
}

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.maxhenkel.de/repository/public")
    maven("https://jitpack.io") {
        content {
            includeModule("me.carleslc.Simple-YAML", "Simple-Yaml")
            includeModule("me.carleslc.Simple-YAML", "Simple-Configuration")
            includeModule("me.carleslc.Simple-YAML", "Simple-YAML-Parent")
            includeModule("com.github.technicallycoded", "FoliaLib")
        }
    }
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc"
    }
    maven("https://repo.subkek.space/maven-public") {
        name = "subkek"
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")

    compileOnly("de.maxhenkel.voicechat:voicechat-api:2.6.0")
    compileOnly("net.dmulloy2:ProtocolLib:5.4.0")
    compileOnly("dev.jorel:commandapi-paper-core:11.1.0")
    compileOnly("me.yiski:lavaplayer-bundle:1.0.7")

    shadow("commons-io:commons-io:2.21.0")
    shadow("org.bstats:bstats-bukkit:3.1.0")
    shadow("com.github.technicallycoded:FoliaLib:0.4.3") {
        exclude("org.slf4j")
    }

    shadow("org.yaml:snakeyaml:2.2")
    shadow ("me.carleslc.Simple-YAML:Simple-Yaml:1.8.4") {
        exclude(group="org.yaml", module="snakeyaml")
    }

    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")
}

val pluginId = properties["plugin_id"]

paper {
    name = rootProject.name
    version = rootProject.version as String
    main = "space.subkek.customdiscs.CustomDiscs"
    loader = "space.subkek.customdiscs.CustomDiscsLoader"

    authors = listOf("subkek", "yiski")
    website = "https://discord.gg/eRvwvmEXWz"
    apiVersion = "1.21"

    foliaSupported = true

    permissions {
        register("$pluginId.help") {
            default = BukkitPluginDescription.Permission.Default.TRUE
        }
        register("$pluginId.reload") {
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("$pluginId.download") {
            default = BukkitPluginDescription.Permission.Default.TRUE
        }
        register("$pluginId.create") {
          default = BukkitPluginDescription.Permission.Default.TRUE
        }
        register("$pluginId.create.local") {
            default = BukkitPluginDescription.Permission.Default.TRUE
        }
        register("$pluginId.create.remote") {
          default = BukkitPluginDescription.Permission.Default.TRUE
        }
        register("$pluginId.create.remote.youtube") {
          default = BukkitPluginDescription.Permission.Default.TRUE
        }
        register("$pluginId.create.remote.soundcloud") {
          default = BukkitPluginDescription.Permission.Default.TRUE
        }
        register("$pluginId.distance") {
            default = BukkitPluginDescription.Permission.Default.TRUE
        }
    }

    serverDependencies {
        register("voicechat")
        register("ProtocolLib")
        register("CommandAPI")
    }
}

// ./gradlew modrinth -Pmodrinth.token=token
modrinth {
    val rawToken = findProperty("modrinth.token")?.toString() ?: ""

    token.set(rawToken)
    changelog.set(rootProject.file("changelog.md").readText())
    versionName.set("CustomDiscs-SVC $version")
    projectId.set("customdiscs-svc")
    versionNumber.set(version as String)
    versionType.set("release")
    gameVersions.addAll("1.21.11", "1.21.10", "1.21.9", "1.21.8", "1.21.7", "1.21.6", "1.21.5", "1.21.4", "1.21.3", "1.21.2", "1.21.1", "1.21", "1.20.6")
    loaders.addAll("paper", "purpur", "folia")
    uploadFile.set(tasks.named("shadowJar"))
    dependencies {
        required.project("simple-voice-chat", "commandapi")
    }
}

tasks.named("modrinth") {
    val changelogFile = project.file("changelog.md")
    doFirst {
        if (modrinth.token.orNull.isNullOrBlank()) {
            throw GradleException("token is empty! Use -Pmodrinth.token=...")
        }
        if (!changelogFile.exists() && changelogFile.length() == 0L) {
            throw GradleException("changelog is empty!")
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    disableAutoTargetJvm()
}

tasks.jar {
    enabled = false
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.shadowJar {
    archiveFileName = "${rootProject.name}-$version.jar"

    configurations = listOf(project.configurations.shadow.get())
    mergeServiceFiles()

    fun relocate(pkg: String) = relocate(pkg, "${rootProject.group}.customdiscs.libs.$pkg")

    relocate("org.apache")
    relocate("org.jsoup")
    relocate("com.fasterxml")
    relocate("org.yaml.snakeyaml")
    relocate("org.simpleyaml")
    relocate("org.jflac")
    relocate("org.tritonus")
    relocate("mozilla")
    relocate("junit")
    relocate("javazoom")
    relocate("certificates")
    relocate("org.hamcrest")
    relocate("org.junit")
    relocate("net.sourceforge.jaad.aac")
    relocate("net.iharder")
    relocate("com.tcoded")
    relocate("com.grack")
    relocate("dev.lavalink")
    relocate("org.intellij")
    relocate("org.jetbrains")
    relocate("org.bstats")
}
