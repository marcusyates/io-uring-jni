plugins {
    id("java-library")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
}

val ioUringJni = configurations.create("ioUringJni") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    ioUringJni(project(mapOf("path" to ":io-uring-jni", "configuration" to "jniLib")))
    testImplementation("junit:junit:4.12")
}

tasks {
    classes {
        dependsOn(ioUringJni)
    }

    jar {
        from(
                { ioUringJni.filter { it.name.endsWith("so") }.map { it } },
                { into("native") })
    }

    test {
        systemProperty("java.library.path", file(ioUringJni.singleFile.parentFile).absoluteFile)
    }
}
