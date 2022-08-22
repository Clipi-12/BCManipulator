val ktVer = "1.5.31"
plugins {
    kotlin("jvm") version "1.5.31"
    id("me.clipi.gradle") version "latest.release"
    id("me.clipi.gradle.conventions.git-publish") version "latest.release"
}

group = "me.clipi"
version = "1.0"

kotlin {
    explicitApi()
}

publishing {
    publications {
        create<MavenPublication>("MainProject") {
            from(components["java"])
        }
    }
    repositories {
        mavenLocal()
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8", ktVer))
    implementation(kotlin("reflect", ktVer))
    implementation("net.bytebuddy:byte-buddy:1.12.12")
    implementation("net.bytebuddy:byte-buddy-agent:1.12.12")
}
