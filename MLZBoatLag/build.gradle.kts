// Build file for the MLZBoatLag plugin.
//
// Handy commands (run them in the Codespaces terminal):
//   gradle build       -> compiles, runs the tests, puts the jar in build/libs/
//   gradle test        -> just runs the tests
//   gradle runServer   -> starts a throwaway Paper 1.21.4 test server with ProtocolLib + this plugin

plugins {
    java
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "com.mlz"
version = "2.0.0"

java {
    // Paper 1.21.4 needs Java 21.
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // "compileOnly" = the server already has these at runtime, so they don't go inside our jar.
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("net.dmulloy2:ProtocolLib:5.4.0")

    testImplementation(platform("org.junit:junit-bom:5.11.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    // Puts the version number above into plugin.yml automatically.
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.runServer {
    minecraftVersion("1.21.4")
    downloadPlugins {
        github("dmulloy2", "ProtocolLib", "5.4.0", "ProtocolLib.jar")
    }
}
