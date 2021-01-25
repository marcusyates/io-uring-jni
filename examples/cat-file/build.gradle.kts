plugins {
    id("java")
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
}

application {
    mainClass.set("com.marcusyates.iouring.examples.catfile.CatFileMain")
}

dependencies {
    implementation(project(":io-uring"))
    testImplementation("junit:junit:4.12")
}
