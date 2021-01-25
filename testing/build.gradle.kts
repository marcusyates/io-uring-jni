plugins {
    id("java")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation(project(":io-uring"))
    implementation("junit:junit:4.12")
}
