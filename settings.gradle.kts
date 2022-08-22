rootProject.name = "bc-manipulator"

pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        maven {
            name = "Clipi"
            url = uri("https://clipi-repo.herokuapp.com/")
        }
    }
}
