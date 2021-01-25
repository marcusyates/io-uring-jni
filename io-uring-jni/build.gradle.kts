import org.gradle.internal.jvm.Jvm

plugins {
    id("cpp-library")
}

val jniLib = configurations.create("jniLib") {
    isCanBeConsumed = true
    isCanBeResolved = false
}

library.targetMachines.add(machines.linux.x86_64)

tasks.withType<CppCompile>().configureEach {
    includes.from(
            "${projectDir}/src/main/c/include",
            "${Jvm.current().javaHome}/include",
            "${Jvm.current().javaHome}/include/linux")

    source.from(fileTree(mapOf("dir" to "src/main/c", "include" to "**/*.c")))

    // tell g++ to compile c instead of c++
    compilerArgs.addAll(listOf("-x", "c", "-std=c11"))
}

tasks.withType<LinkSharedLibrary>().configureEach {
    linkerArgs.add("-luring")
}

artifacts {
    add(jniLib.name, file("${buildDir}/lib/main/release/lib${project.name}.so")) {
        builtBy("linkRelease")
    }
}
