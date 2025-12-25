import net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission.Default as PermDefault

plugins {
    id("java-library")
    id ("com.modrinth.minotaur") version "2.+"
    id("io.github.goooler.shadow") version "8.1.8"
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
}

allprojects {
    group = "io.github.subkek"
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
    compileOnly("dev.jorel:commandapi-paper-core:11.0.0")
    compileOnly("me.yiski:lavaplayer-lib:1.0.6")

    shadow("com.googlecode.soundlibs:mp3spi:1.9.5.4")
    shadow("com.googlecode.json-simple:json-simple:1.1.1")
    shadow("org.jflac:jflac-codec:1.5.2")
    shadow("commons-io:commons-io:2.16.1")
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

bukkit {
    name = rootProject.name
    version = rootProject.version as String
    main = "io.github.subkek.customdiscs.CustomDiscs"

    authors = listOf("subkek")
    website = "https://discord.gg/eRvwvmEXWz"
    apiVersion = "1.21"

    foliaSupported = true

    permissions {
        register("$pluginId.help") {
            default = PermDefault.TRUE
        }
        register("$pluginId.reload") {
            default = PermDefault.OP
        }
        register("$pluginId.download") {
            default = PermDefault.TRUE
        }
        register("$pluginId.create") {
          default = PermDefault.TRUE
        }
        register("$pluginId.create.local") {
            default = PermDefault.TRUE
        }
        register("$pluginId.create.remote") {
          default = PermDefault.TRUE
        }
        register("$pluginId.create.remote.youtube") {
          default = PermDefault.TRUE
        }
        register("$pluginId.create.remote.soundcloud") {
          default = PermDefault.TRUE
        }
        register("$pluginId.distance") {
            default = PermDefault.TRUE
        }
    }

    depend = listOf(
        "voicechat",
        "ProtocolLib",
        "CommandAPI"
    )
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
    loaders.addAll("bukkit", "paper", "purpur", "folia")
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

    fun relocate(pkg: String) = relocate(pkg, "${rootProject.group}.customdiscs.libs.$pkg") {
        exclude("com/sedmelluq/discord/lavaplayer/natives/**")
    }

    relocate("org.apache")
    relocate("org.jsoup")
    relocate("com.fasterxml")
    relocate("org.yaml.snakeyaml")
    relocate("org.simpleyaml")
    relocate("org.jflac")
    relocate("org.json")
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
    relocate("com.sedmelluq")
}
