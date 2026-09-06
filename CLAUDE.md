# CardGame — instructions projet

## Build

La compilation JVM de référence passe par le `Dockerfile` (multi-stage : build du front avec
Node, puis `:server:buildFatJar --no-daemon`, puis image JRE). Pour vérifier une modif Kotlin :

```bash
docker build -t cardgame .
```

Pour compiler les tests et jouer la suite `core` (qui n'a besoin d'aucune base) :

```bash
docker build --target build -t cardgame-build . && docker run --rm cardgame-build sh -c "./gradlew :core:test :server:compileTestKotlin -PincludeFrontend=false --no-daemon"
```

> **Ce qui a changé.** La règle « ne jamais lancer `./gradlew` sur la machine hôte » existait
> à cause d'un squelette Ktor mort à la racine (`src/`), que la racine compilait avec son
> propre plugin Kotlin en `jvmToolchain(8)` et sans les dépendances qu'il importait. Un démon
> chaud masquait le conflit, un appel à froid le faisait ressortir. Le `src/` racine est
> supprimé et le plugin Kotlin n'est plus déclaré qu'une fois, à la racine, en `apply false`.
> Docker reste la référence — c'est l'environnement reproductible — mais un `./gradlew` à
> froid ne devrait plus diverger. À confirmer sur la machine hôte avant de retirer cette note.

Le frontend seul se vérifie sans Docker :

```bash
cd frontend && npm run typecheck && npm run test:run
```
