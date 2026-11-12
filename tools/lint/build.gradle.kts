plugins {
    `java-base`
}

val detekt by configurations.creating
dependencies {
    detekt(libs.detekt.cli)
}

val repository = layout.projectDirectory.dir("../..").asFile.canonicalFile
val kotlinSources = providers.exec {
    workingDir(repository)
    commandLine("git", "ls-files", "--cached", "--others", "--exclude-standard", "-z")
}.standardOutput.asText.map { output ->
    output.split('\u0000')
        .filter { it.endsWith(".kt") || it.endsWith(".kts") }
        .map { repository.resolve(it).canonicalFile }
        .filter { it.isFile }
        .onEach {
            require(it.toPath().startsWith(repository.toPath())) {
                "Lint source must remain inside the repository."
            }
        }
        .distinct()
}

val lintKotlin by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Checks public Kotlin code for smells; warnings fail without compiling app targets."
    mainClass.set("dev.detekt.cli.Main")
    classpath = detekt
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(17)) })
    workingDir(repository)
    args(
        "--input", kotlinSources.get().joinToString(File.pathSeparator),
        "--config", repository.resolve("config/detekt.yml").path,
        "--build-upon-default-config",
        "--analysis-mode", "light",
        "--language-version", "2.4",
        "--fail-on-severity", "Warning",
        "--report", "checkstyle:${layout.buildDirectory.file("reports/detekt.xml").get().asFile}",
        "--report", "html:${layout.buildDirectory.file("reports/detekt.html").get().asFile}",
    )
}

tasks.check { dependsOn(lintKotlin) }
