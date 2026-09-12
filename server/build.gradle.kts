plugins {
    `java-library`
    application
    alias(libs.plugins.kotlin.jvm)
}

application {
    mainClass = "dev.pinaki.localnotes.server.Server"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(11)
    }
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
}
