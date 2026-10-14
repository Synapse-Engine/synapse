plugins {
    `java-library`
    id("com.diffplug.spotless")
}

dependencies {
    api("io.synapseproject:synapse-api:0.1.0-SNAPSHOT")
    implementation("org.xerial:sqlite-jdbc:3.53.4.0")
    implementation("org.jdbi:jdbi3-core:3.55.0")
    implementation("org.jdbi:jdbi3-sqlobject:3.55.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.22.3")
    runtimeOnly("org.slf4j:slf4j-jdk14:2.0.17")

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
