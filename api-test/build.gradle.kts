dependencies {
    compileOnly(project(":api"))
    compileOnly(libs.paper.api)
    compileOnly(libs.voicechat.api)
}

tasks.processResources {
    filteringCharset = "UTF-8"

    val props = mutableMapOf<String, String>()

    props["version"] = project.version.toString()

    inputs.properties(props)
    filesMatching("paper-plugin.yml") {
        expand(props)
    }
}

tasks.jar {
    archiveBaseName.set("${rootProject.name}-${project.name}")
}
