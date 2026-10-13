plugins {
    id("com.diffplug.spotless") version "8.10.4" apply false
    id("com.gradleup.shadow") version "9.6.1" apply false
}

subprojects {
    group = "io.synapseproject"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Synapse-Engine/synapse-api")
            val user =
                providers.gradleProperty("gpr.user").orElse(providers.environmentVariable("GITHUB_ACTOR"))
            val key =
                providers.gradleProperty("gpr.key").orElse(providers.environmentVariable("GITHUB_TOKEN"))
            if (user.isPresent && key.isPresent) {
                credentials {
                    username = user.get()
                    password = key.get()
                }
            }
        }
    }
}
