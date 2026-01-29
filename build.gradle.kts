import net.minecrell.pluginyml.bukkit.BukkitPluginDescription
import net.minecrell.pluginyml.paper.PaperPluginDescription

plugins {
    `java-library`
    alias(libs.plugins.minotaur)
    alias(libs.plugins.shadow)
    alias(libs.plugins.paper.yml)
}

allprojects {
    apply(plugin = "java-library")

    group = "space.subkek"
    version = properties["plugin_version"]!!

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
        disableAutoTargetJvm()
    }
}

dependencies {
    shadow(project(":api"))
    shadow(libs.bstats)

    compileOnly(libs.paper.api)
    compileOnly(libs.voicechat.api)
    compileOnly(libs.protocollib)
    compileOnly(libs.commandapi)

    implementation(libs.lavaplayer)
    implementation(libs.lavaplayer.youtube)

    implementation(libs.commons.io)
    implementation(libs.simple.yaml)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
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
            default = BukkitPluginDescription.Permission.Default.OP
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
        register("voicechat") { load = PaperPluginDescription.RelativeLoadOrder.BEFORE }
        register("ProtocolLib") { load = PaperPluginDescription.RelativeLoadOrder.BEFORE }
        register("CommandAPI") { load = PaperPluginDescription.RelativeLoadOrder.BEFORE }
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
    gameVersions.addAll(
        "1.21.11",
        "1.21.10",
        "1.21.9",
        "1.21.8",
        "1.21.7",
        "1.21.6",
        "1.21.5",
        "1.21.4",
        "1.21.3",
        "1.21.2",
        "1.21.1",
        "1.21",
        "1.20.6"
    )
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

tasks.processResources {
    filteringCharset = "UTF-8"

    val catalog = project.extensions.getByType<VersionCatalogsExtension>().named("libs")
    val props = mutableMapOf<String, String>()

    catalog.libraryAliases.forEach { alias ->
        val lib = catalog.findLibrary(alias).get().get()
        val coords =
            "${lib.module.group}:${lib.module.name}:${lib.versionConstraint.requiredVersion}"

        props[alias.replace(".", "_")] = coords
    }
    // props.forEach { (k, v) -> println("DEBUG: Key '$k' = $v") }

    inputs.properties(props)

    filesMatching("deps.json") {
        expand(props)
    }
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}

tasks.shadowJar {
    archiveFileName.set("${rootProject.name}-$version.jar")

    configurations = listOf(project.configurations.shadow.get())
    mergeServiceFiles()

    fun relocate(pkg: String) = relocate(pkg, "${rootProject.group}.customdiscs.deps.$pkg")
    relocate("org.bstats")
}
