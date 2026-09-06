// The root project holds no code: `core`, `server` and `frontend` do. The Kotlin plugin is
// declared here, and only here, so every module compiles against one version — applying it
// separately per module with its own toolchain is what used to make a cold `./gradlew`
// diverge from the Docker build.
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

allprojects {
    group = "fr.ftnl"
    version = "1.0.0-SNAPSHOT"
}
