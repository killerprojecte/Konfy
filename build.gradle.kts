import com.palantir.gradle.gitversion.VersionDetails
import groovy.lang.Closure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.20"
    id("com.palantir.git-version") version "4.0.0"
    id("maven-publish")
    id("com.gradleup.shadow") version "9.2.1"
    id("org.jetbrains.dokka") version "2.1.0"
}

val versionDetails: Closure<VersionDetails> by extra
val details = versionDetails()

group = "dev.rgbmc"
version = "1.0.0"

val shade: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

configurations {
    implementation {
        extendsFrom(shade)
    }
}

repositories {
    mavenCentral()
    maven("https://repo.fastmcmirror.org/content/repositories/releases/")
}


dependencies {
    testImplementation(kotlin("test"))
    api("it.krzeminski:snakeyaml-engine-kmp:4.0.1")
    shade("com.charleskorn.kaml:kaml:0.104.7") {
        isTransitive = false
    }
    api("org.jetbrains.kotlinx:kotlinx-serialization-core:1.9.0")
    api("com.squareup.okio:okio-jvm:3.16.4")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    sourceCompatibility = JavaVersion.VERSION_1_8.toString()
    targetCompatibility = JavaVersion.VERSION_1_8.toString()
}

java {
    withSourcesJar()
    withJavadocJar()
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget = JvmTarget.JVM_1_8
    }
}


dokka {
    moduleName = "Konfy"
    description = "YAML utility toolkit based on `kaml` library"

    dokkaPublications.html {
        outputDirectory.set(layout.buildDirectory.dir("docs"))
    }

    dokkaSourceSets.named("main") {
        sourceLink {
            remoteUrl("https://github.com/killerproject/Konfy")
        }

        externalDocumentationLinks {
            // Kotlin Stdlib
            register("stdlib") {
                url("https://kotlinlang.org/api/latest/jvm/stdlib/")
            }
            register("snakeyaml-engine-kmp") {
                url("https://krzema12.github.io/snakeyaml-engine-kmp/")
                packageListUrl("https://krzema12.github.io/snakeyaml-engine-kmp/-snake-y-a-m-l%20-engine%20-k-m-p/package-list")
            }
        }
    }

    pluginsConfiguration.html {
        footerMessage.set("(c) Copyright 2025 killerprojecte")
    }
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.shadowJar {
    archiveClassifier.set("")
    configurations = listOf(shade)
    mergeServiceFiles()

    relocate("com.charleskorn.kaml", "dev.rgbmc.konfy.internal")
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
            artifact(tasks.shadowJar)
            artifact(tasks.javadoc)
            from(components["java"])
            groupId = project.group.toString()
            artifactId = project.name
            version += "-${details.gitHash.substring(0, 7)}"

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