plugins {
    java
}

group = "io.github.gaming32"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.jemnetworks.com/releases")
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    implementation("commons-io:commons-io:2.16.1")
    implementation("com.google.protobuf:protobuf-java:4.26.1")
    implementation("it.unimi.dsi:fastutil:8.5.13")
    implementation("com.google.guava:guava:33.1.0-jre")
    implementation("io.github.gaming32:generators:1.0")
    implementation("io.github.gaming32:szs-lib:1.0.1")
    compileOnly("org.jetbrains:annotations:24.1.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "io.github.gaming32.squidbeatz2.Main"
    }
    from(configurations.runtimeClasspath.get().files.map { if (it.isDirectory) it else zipTree(it) })
}
