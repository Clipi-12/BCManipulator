val ktVer = "1.7.10"
plugins {
    id("co.uzzu.dotenv.gradle") version "2.0.0"
    kotlin("jvm") version "1.7.10"
    id("me.clipi.gradle") version "latest.release"
    id("me.clipi.gradle.conventions.github-publish") version "latest.release"
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
