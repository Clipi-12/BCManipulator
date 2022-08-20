val ktVer = "1.7.10"
plugins {
    kotlin("jvm") version "1.7.10"
    id("me.clipi.gradle") version "latest.release"
    `maven-publish`
}

group = "me.clipi"
version = "1.0"

kotlin {
    explicitApi()
}

publishing {
    publications {
        create<MavenPublication>("MainProject")
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
