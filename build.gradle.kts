import net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission.Default as PermDefault

plugins {
    id("java-library")
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
    maven("https://repo.subkek.space/maven-public") {
        name = "subkek"
    }
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT")
    //compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
 
    compileOnly("de.maxhenkel.voicechat:voicechat-api:2.5.0")
    compileOnly("com.comphenix.protocol:ProtocolLib:5.3.0")
    compileOnly("me.yiski:lavaplayer-lib:1.0.0")

    shadow("com.googlecode.soundlibs:mp3spi:1.9.5.4")
    shadow("org.jflac:jflac-codec:1.5.2")
    shadow("commons-io:commons-io:2.16.1")
    shadow("com.github.technicallycoded:FoliaLib:0.4.3") {
        exclude("org.slf4j")
    }

    shadow("dev.jorel:commandapi-bukkit-shade:10.0.1")

    shadow(platform("net.kyori:adventure-bom:4.17.0"))
    shadow("net.kyori:adventure-api")
    shadow("net.kyori:adventure-text-minimessage")
    shadow("net.kyori:adventure-platform-bukkit:4.3.4")

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
    apiVersion = "1.16"

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
        register("$pluginId.createyt") {
            default = PermDefault.TRUE
        }
        register("$pluginId.distance") {
            default = PermDefault.TRUE
        }
    }

    depend = listOf(
        "voicechat",
        "ProtocolLib"
    )

    softDepend = listOf(
        "lavaplayer-lib"
    )
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
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
    relocate("net.kyori")
    relocate("net.iharder")
    relocate("com.tcoded")
    relocate("com.grack")
    relocate("dev.lavalink")
    relocate("org.intellij")
    relocate("org.jetbrains")
    relocate("com.sedmelluq")
    relocate("dev.jorel.commandapi")
}
