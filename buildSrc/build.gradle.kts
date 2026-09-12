plugins {
    kotlin("jvm") version "2.4.10"
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(gradleApi())
    implementation(localGroovy())
    implementation("org.apache.xmlgraphics:batik-all:1.19")
}
