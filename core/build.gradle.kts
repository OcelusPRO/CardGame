plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // The domain stays framework free; kotlinx.serialization is only used to snapshot
    // a running game into Redis. The public HTTP/WebSocket contract lives in :server.
    api(libs.kotlinx.serialization.json)

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
