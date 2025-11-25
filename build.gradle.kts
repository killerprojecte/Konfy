import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.20"
    id("com.palantir.git-version") version "4.0.0"
    id("maven-publish")
    id("com.gradleup.shadow") version "9.2.1"
}

val versionDetails: groovy.lang.Closure<com.palantir.gradle.gitversion.VersionDetails> by extra
val details = versionDetails()

group = "dev.rgbmc"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.fastmcmirror.org/content/repositories/releases/")
}

dependencies {
    testImplementation(kotlin("test"))
    api("it.krzeminski:snakeyaml-engine-kmp:4.0.1")
    api("com.charleskorn.kaml:kaml:0.104.7")
    api("org.jetbrains.kotlinx:kotlinx-serialization-core:1.9.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget = JvmTarget.JVM_1_8
    }
}

publishing {
    repositories {
        maven {
            credentials {
                username = System.getenv("REPO_USERNAME")
                password = System.getenv("REPO_PASSWORD")
            }
            url = uri("https://repo.fastmcmirror.org/content/repositories/releases/")
        }
    }
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = project.group.toString()
            artifactId = project.name
            version = this.version + "-${details.gitHash.substring(0, 7)}"

            println("Current version: $version")

            pom {
                name.set("Konfy")
                description.set("YAML utility toolkit based on `kaml` library")
                url.set("https://github.com/killerprojecte/UIKit")
                licenses {
                    license {
                        name.set("All rights reserved")
                    }
                }
                developers {
                    developer {
                        id.set("killerprojecte")
                        name.set("K.")
                        email.set("i@awa.ng")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/killerprojecte/Konfy.git")
                    developerConnection.set("scm:git:ssh://github.com/killerprojecte/Konfy.git")
                    url.set("https://github.com/killerprojecte/Konfy")
                }
            }
        }
    }
}