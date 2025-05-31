plugins {
    kotlin("jvm") version "2.1.21"
    kotlin("plugin.serialization") version "2.1.21"
    id("io.ktor.plugin") version "3.1.3"
    id("me.qoomon.git-versioning") version "6.4.4"
}

gitVersioning.apply {

    rev {
        version = "\${commit.short}"
    }
}

application {

    mainClass.set("ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

sourceSets {
    main {
        kotlin {
            srcDir(layout.buildDirectory.dir("generated/source"))
        }
    }
}

ktor {
    docker {

        localImageName.set("ghcr.io/stefanoltmann/emma-webapi")
        imageTag.set("latest")

        jreVersion.set(JavaVersion.VERSION_21)
    }
}

repositories {
    mavenCentral()
}

dependencies {

    implementation("io.ktor:ktor-client-cio")

    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-cio-jvm")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("ch.qos.logback:logback-classic:1.5.18")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    implementation("com.ghgande:j2mod:3.2.1")
}

// region Version
project.afterEvaluate {

    logger.lifecycle("Generate Version.kt")

    val outputDir = layout.buildDirectory.file("generated/source/").get().asFile

    outputDir.mkdirs()

    val file = File(outputDir.absolutePath, "Version.kt")

    file.printWriter().use { writer ->

        writer.println("const val VERSION: String = \"$version\"")

        writer.flush()
    }
}
// endregion
