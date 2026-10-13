plugins {
    `java-library`
    id("com.diffplug.spotless")
}

dependencies {
    api("io.synapseproject:synapse-api:0.1.0-SNAPSHOT")

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks.test {
    useJUnitPlatform()
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
