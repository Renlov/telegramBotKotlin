import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val coroutinesVersion = "1.6.1"
val ktor_version = "1.6.8"

plugins {
    application
    kotlin("jvm") version "1.6.20"
    kotlin("plugin.serialization") version "1.4.21"
}

buildscript {
    extra.set("kotlinVersion", "1.6.1")
}

sourceSets {
    main {
        kotlin.sourceSets.create("kotlin")
    }
}

group = "com.sometime"
version = "1.0-SNAPSHOT"

application{
    mainClass.set("main")
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
    }
}

dependencies {
    testImplementation(kotlin("test"))
    implementation ("dev.inmo:tgbotapi:0.38.21")

    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")

    implementation ("io.ktor:ktor-client-core:$ktor_version")
    implementation ("io.ktor:ktor-client-cio:$ktor_version")
    implementation ("io.ktor:ktor-client-serialization:$ktor_version")
    implementation ("io.ktor:ktor-client-websockets:$ktor_version")
    implementation ("io.ktor:ktor-client-logging:$ktor_version")

    implementation ("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.create("stage") {
    dependsOn("installDist")
}
/*
tasks.create("stage2") {
    dependsOn("installDist")
}*/
