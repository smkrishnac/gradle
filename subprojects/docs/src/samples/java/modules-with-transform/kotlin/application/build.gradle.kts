plugins {
    application
}

dependencies {
    implementation("com.google.code.gson:gson:2.8.6")           // real module
    implementation("org.apache.commons:commons-lang3:3.10")     // automatic module
    implementation("commons-beanutils:commons-beanutils:1.9.4") // plain library (also brings in other libraries transitively)
    implementation("commons-cli:commons-cli:1.4")               // plain library
}

application {
    mainModule.set("org.gradle.sample.app")
    mainClass.set("org.gradle.sample.app.Main")
}


