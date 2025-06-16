plugins {
    kotlin("jvm") version "1.9.0"
    id("org.jetbrains.intellij") version "1.17.3"
}

group = "com.workday.plugin"
version = "1.0-beta"

repositories {
    mavenCentral()
}

intellij {
    version.set("2024.1")
    type.set("IC") // Compatible with IntelliJ Community Edition
    plugins.set(listOf("java"))
}


tasks {
    patchPluginXml {
        sinceBuild.set("240")
        untilBuild.set("999.*")
        changeNotes.set("Initial plugin to run Gradle test by class or method.")
    }
    compileKotlin {
        kotlinOptions.jvmTarget = "17"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "17"
    }
    compileJava {
        targetCompatibility = "17"
        sourceCompatibility = "17"
    }
}
