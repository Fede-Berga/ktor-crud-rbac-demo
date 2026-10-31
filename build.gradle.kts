
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(ktorLibs.plugins.ktor)
}

group = "com.example.com"
version = "1.0.0-SNAPSHOT"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(21)
}
dependencies {
    implementation(ktorLibs.serialization.kotlinx.json)
    implementation(ktorLibs.server.auth)
    implementation(ktorLibs.server.auth.jwt)
    implementation(ktorLibs.server.callLogging)
    implementation(ktorLibs.server.config.yaml)
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.metrics.micrometer)
    implementation(ktorLibs.server.netty)
    implementation(ktorLibs.server.statusPages)
    implementation(libs.bcrypt)
    implementation(libs.exposed.core)
    implementation(libs.exposed.r2dbc)
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)
    implementation(libs.kotlinx.coroutines.reactive)
    implementation(libs.lettuce.core)
    implementation(libs.logback.classic)
    implementation(libs.micrometer.registryPrometheus)
    implementation(libs.r2dbc.postgresql)

    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.client.contentNegotiation)
    testImplementation(ktorLibs.server.testHost)
}
