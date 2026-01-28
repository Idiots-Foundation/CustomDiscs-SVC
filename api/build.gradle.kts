plugins {
    `maven-publish`
}

version = properties["api_version"]!!

dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.voicechat.api)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "${rootProject.name.lowercase()}-api"
            version = project.version.toString()

            from(components["java"])
        }
    }

    repositories {
        maven {
            name = "subkek"
            url = uri("https://repo.subkek.space/maven-public/")
            credentials(PasswordCredentials::class)
        }
    }
}
