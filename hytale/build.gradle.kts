plugins {
    java
    id("com.diffplug.spotless")
    id("com.gradleup.shadow")
}

repositories {
    maven {
        name = "hytale"
        url = uri("https://maven.hytale.com/release")
    }
}

dependencies {
    implementation(project(":common"))
    compileOnly("com.hypixel.hytale:Server:0.7.0")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

spotless {
    java {
        googleJavaFormat("1.36.1")
        formatAnnotations()
        removeUnusedImports()
    }
}

tasks.check {
    dependsOn(tasks.spotlessCheck)
}

tasks.jar {
    enabled = false
}

tasks.shadowJar {
    archiveBaseName.set("synapse")
    archiveClassifier.set("")
    mergeServiceFiles()
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}
