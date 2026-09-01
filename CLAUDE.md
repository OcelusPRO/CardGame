# CardGame — instructions projet

## Build

**Ne jamais compiler le serveur/core en lançant `./gradlew` directement sur la machine hôte.**
La compilation JVM passe **toujours** par le `Dockerfile` (multi-stage : build du front avec Node,
puis `:server:buildFatJar --no-daemon`, puis image JRE). Pour vérifier une modif Kotlin :

```bash
docker build -t cardgame .
```

Raisons : la racine `build.gradle.kts` et les modules ont eu des versions de plugin Kotlin
divergentes ; un démon Gradle chaud masquait le conflit, un appel `gradlew` à froid le fait
ressortir. Le `Dockerfile` fige un environnement reproductible (`--no-daemon`, toolchain 21).

Le frontend seul peut être vérifié sans Docker :

```bash
cd frontend && npm run typecheck && npm run test:run
```
