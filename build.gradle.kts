import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.31"
    kotlin("plugin.serialization") version "1.4.31"
}

group = "me.sebastian"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}
val ktor_version = "1.5.1"
val logback_version = "1.2.3"


dependencies {
    testImplementation(kotlin("test-junit"))
    implementation(kotlin("scripting-jvm-host"))
    implementation(kotlin("scripting-jvm"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.1.0")

    // ktor stuff

    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-html-builder:$ktor_version")
    implementation("io.ktor:ktor-server-host-common:$ktor_version")
    implementation("io.ktor:ktor-websockets:$ktor_version")

}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}