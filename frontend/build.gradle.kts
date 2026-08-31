plugins {
    base
}

val isWindows = System.getProperty("os.name").lowercase().contains("windows")
val npm = if (isWindows) "npm.cmd" else "npm"

/** Installs the node dependencies; skipped when `node_modules` is already up to date. */
val npmInstall by tasks.registering(Exec::class) {
    inputs.file("package.json")
    inputs.file("package-lock.json").optional()
    outputs.dir("node_modules")
    commandLine(npm, "install")
}

/** Produces the production bundle in `frontend/dist`, later served by Ktor. */
val buildWeb by tasks.registering(Exec::class) {
    dependsOn(npmInstall)
    inputs.dir("src")
    inputs.file("index.html")
    inputs.file("vite.config.ts")
    inputs.file("package.json")
    outputs.dir("dist")
    commandLine(npm, "run", "build")
}

/** Runs the vitest suite, wired into `gradle check` so CI covers both sides. */
val testWeb by tasks.registering(Exec::class) {
    dependsOn(npmInstall)
    commandLine(npm, "run", "test:run")
}

tasks.assemble { dependsOn(buildWeb) }
tasks.check { dependsOn(testWeb) }
tasks.clean { delete("dist", "node_modules/.vite") }
