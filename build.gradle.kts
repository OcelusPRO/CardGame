plugins {
    kotlin("jvm") version "2.4.0"
}
allprojects {
    group = "fr.ftnl"
    version = "1.0.0-SNAPSHOT"
}
repositories {
    mavenCentral()
}
dependencies {
    testImplementation(kotlin("test"))
}
kotlin {
    jvmToolchain(8)
}