# Sans Filtre

Un jeu de cartes en ligne inspiré de Limite Limite : une **carte situation** pose le décor,
des **cartes réponses** viennent le saboter, et tout le monde vote pour la pire idée.

Tout se joue dans le navigateur, sans installation : l'hôte crée une table, partage un code
à cinq caractères (ou son QR code), et les autres s'assoient.

---

## Sommaire

- [Ce que le jeu sait faire](#ce-que-le-jeu-sait-faire)
- [Règles et points](#règles-et-points)
- [Architecture](#architecture)
- [Démarrage rapide (Docker)](#démarrage-rapide-docker)
- [Développement](#développement)
- [Configuration](#configuration)
- [Connexion Discord et administration](#connexion-discord-et-administration)
- [Connexion Twitch et vote du tchat](#connexion-twitch-et-vote-du-tchat)
- [Tests](#tests)
- [Surface HTTP et WebSocket](#surface-http-et-websocket)

---

## Ce que le jeu sait faire

| Fonction | Détail |
| --- | --- |
| Créer une partie | Code unique à 5 caractères (alphabet sans `O`, `0`, `I`, `1`, `L` ni `Q`), masqué par défaut, avec QR code à la demande |
| Rejoindre | L'adresse de la partie `/game/CODE` est l'invitation : elle affiche la table pour un joueur assis, le formulaire pour les autres |
| Identité | Pseudo et avatar gardés dans le navigateur ; la connexion Discord ou Twitch suggère le pseudo sans jamais l'imposer |
| Avatar | Deux moitiés personnalisables séparément (tête et corps), la photo du compte connecté (Discord ou Twitch) venant se poser dans la tête |
| Mode cartes | Main de cartes réponses distribuée à chaque manche |
| Mode « sans limites » | Aucune carte réponse : chacun écrit la sienne directement sur une carte vierge |
| Mode personnalisé | Un sélecteur unique mêle les packs officiels (marqués d'une étoile) et les decks gardés dans le navigateur, plus des cartes écrites à la volée |
| Vote ou maître du jeu | Choisi par l'hôte dans le salon, les deux modes cohabitent dans le moteur |
| Reconnexion | Le siège, le score et la main survivent à un rafraîchissement ou à une coupure réseau |
| Administration | Édition des packs et des cartes officielles, statistiques d'usage et compteurs en direct |
| Fin de partie | Podium pour les trois premiers, classement simple pour la suite |
| Vote du tchat Twitch | Un troisième mode de jeu : les joueurs répondent, et c'est le tchat de l'hôte — plus, s'il le veut, celui des autres joueurs streamers — qui désigne la meilleure réponse au numéro de la carte |
| Bruitages | Clics, sélection de carte, vote, révélation et derniers battements du chrono, coupables d'un seul bouton |

Les deux modes se cumulent : un paquet de situations maison avec des réponses écrites à la volée
fonctionne exactement comme le reste.

---

## Règles et points

Une manche se déroule en trois temps : **répondre**, **départager**, **compter**.
Chaque temps a un chronomètre, et se ferme tout seul dès que tout le monde a joué.

**Mode vote** (par défaut) — tout le monde vote :

- chaque vote reçu rapporte **`pointsPerVote`** points, 1 par défaut ;
- une réponse choisie par **tous ceux qui pouvaient la choisir** rapporte **`unanimityBonus`**
  points de plus, 3 par défaut. Un seul vote ailleurs et le bonus tombe à zéro.

**Mode maître du jeu** — un joueur différent tranche à chaque manche et ne joue pas :
son choix vaut une voix, donc la réponse retenue rapporte le même **`pointsPerVote`**.

**Mode tchat** — les joueurs répondent et ne votent plus : **chaque spectateur compte pour une
voix** sur la carte qu'il choisit, et la réponse qui en récolte le plus **remporte la manche,
qui vaut 1 point**. Ni points par vote, ni bonus d'unanimité : sinon une communauté de trois
mille personnes réglerait la partie en deux manches. Voir
[Connexion Twitch et vote du tchat](#connexion-twitch-et-vote-du-tchat).

Personne ne saute un tour pour avoir hésité : à l'expiration du chronomètre, la réponse
déjà sélectionnée part d'elle-même, et une main restée intacte joue une carte au hasard.

La partie dure un **nombre de manches fixé par l'hôte** (8 par défaut), autrement dit un
nombre de cartes situation. Il n'y a pas de score à atteindre : à la fin, le meilleur score
l'emporte. Une partie s'arrête aussi plus tôt si le paquet de situations est épuisé.

> « Tous ceux qui pouvaient la choisir » laisse l'auteur de côté, puisqu'il n'a normalement
> pas le droit de voter pour lui-même. Le réglage **« autoriser à voter pour sa propre
> carte »** le remet dans le lot : le bonus exige alors que l'auteur vote pour sa carte aussi.

---

## Architecture

```
CardGame
├── core/       Le jeu, en Kotlin pur : aucune dépendance à Ktor, à une base ou à un socket
├── server/     Ktor : HTTP, WebSocket, PostgreSQL, Redis, OAuth Discord et Twitch
├── frontend/   React 19 + TypeScript + Tailwind CSS v4, servi par Ktor en production
├── Dockerfile
└── docker-compose.yml
```

### `core` — le moteur

`GameEngine` est une fonction pure : on lui donne un état et une commande, il rend un nouvel état.
Il ne connaît ni le réseau, ni l'horloge système (une `GameClock` est injectée), ni le hasard
(un `Shuffler` est injecté). C'est ce qui permet de rejouer une partie entière dans un test
en quelques lignes, sans démarrer quoi que ce soit.

Chaque commande a son propre gestionnaire (`JoinHandler`, `AnswerHandler`, `ChoiceHandler`…),
et les deux modes de sélection sont deux implémentations de `RoundScoring`.

### `server` — la plomberie

- **PostgreSQL** conserve ce qui doit durer : le catalogue de cartes officielles et les
  statistiques agrégées (usage par carte, et surtout le duo situation × réponse avec son ratio de votes).
  Le schéma appartient aux migrations Flyway de `server/src/main/resources/db/migration`, jamais
  au code Kotlin : les tests appliquent exactement les mêmes fichiers.
- **Redis** conserve les parties en cours, avec une expiration rafraîchie à chaque écriture :
  une table abandonnée disparaît d'elle-même, sans tâche de ménage.
- **Rien d'autre n'est stocké** : ni le résultat des parties, ni les comptes Discord ou Twitch
  des joueurs, ni les votes venus d'un tchat. L'identité d'un joueur tient dans un cookie signé,
  le temps de la session.
- `GameService` sérialise les commandes d'une même partie derrière un verrou, puis prévient
  ses `GameListener` : diffusion WebSocket, statistiques, et minuteurs de phase.
- `GameViewFactory` est le seul endroit qui décide **qui voit quoi**. Une main n'est jamais
  envoyée à un autre joueur, et les auteurs des réponses restent masqués jusqu'à la révélation.

### `frontend` — l'interface

Une partie a une adresse et une seule, `/game/CODE` : c'est la page où l'on joue, et celle où
un nouveau venu prend sa place. Copier le lien de la page, c'est copier l'invitation.

Le bundle compilé est servi par le module `singlePageApplication` de Ktor : un fichier réel
garde son type, et tout le reste retombe sur `index.html` pour qu'un lien profond survive à un
rechargement. Un seul `GameView` arrive par le socket et pilote tout l'écran. Les cartes s'inclinent en 3D
sous le pointeur (uniquement sur les appareils avec une souris), les cartes vierges s'écrivent
avec une police manuscrite, et l'ensemble reste utilisable au pouce sur un téléphone.

Le son vit dans `frontend/src/audio/`. Les bruitages ne sont pas des fichiers mais des
descriptions — quelques oscillateurs et bouffées de bruit que `engine.ts` synthétise à la
volée à partir du catalogue de `sounds.ts` : rien à télécharger, rien à créditer, et un
son se retouche en changeant un nombre. Le haut-parleur de l'en-tête coupe tout d'un geste,
et le choix reste dans le navigateur.

Pour remplacer un bruitage par un vrai enregistrement, poser le fichier dans
`frontend/public/sounds/` et le nommer dans la table `SAMPLES` de `sounds.ts` ; un fichier
absent ou illisible retombe sur la version synthétisée. Pour une musique de fond, la
tuyauterie est en place dans `music.ts` : déposer une boucle dans `frontend/public/music/`
et la déclarer dans `TRACKS`, qui associe une piste à chaque temps de la partie (salon,
manche, fin). Les fondus, le rappel de la piste après une coupure et le respect de
l'interrupteur sont déjà gérés.

---

## Démarrage rapide (Docker)

```bash
cp .env.example .env
```

```bash
docker compose up --build
```

Le site répond sur <http://localhost:8080>. Au premier démarrage la base est vide, et le paquet
de démonstration français est chargé automatiquement (`SEED_DEV_DECK=true` dans `.env.example`).

> Ce paquet de démonstration est un jeu de test volontairement grinçant. Passez `SEED_DEV_DECK=false`
> pour une mise en production : il n'est chargé que dans une base vide, jamais par-dessus vos cartes.

Pour n'ouvrir que les deux bases de données, par exemple pour lancer le serveur depuis l'IDE :

```bash
docker compose up postgres redis
```

---

## Développement

### Backend

```bash
./gradlew :server:run -PincludeFrontend=false
```

`-PincludeFrontend=false` évite de recompiler le site à chaque itération sur le serveur.

### Frontend

```bash
cd frontend && npm install && npm run dev
```

Vite écoute sur <http://localhost:5173> et relaie `/api`, `/auth` et `/ws` vers le backend.
Le navigateur ne parle donc qu'à une seule origine, et le cookie de session fonctionne
exactement comme en production — aucune configuration CORS n'est nécessaire.

Pour viser un backend ailleurs :

```bash
BACKEND_URL=http://autre-hote:8080 npm run dev
```

### Build complet

```bash
./gradlew build
```

Compile le site, le place dans les ressources du serveur, et produit `server/build/libs/server-all.jar`.

---

## Configuration

Toutes les valeurs se pilotent par variables d'environnement (voir `.env.example`).

| Variable | Défaut | Rôle |
| --- | --- | --- |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/cardgame` | Catalogue et statistiques |
| `DATABASE_USER` / `DATABASE_PASSWORD` | `cardgame` | Identifiants PostgreSQL |
| `REDIS_ENABLED` | `true` | À `false`, les parties vivent en mémoire (mono-instance) |
| `REDIS_URL` | `redis://localhost:6379` | Parties en cours |
| `REDIS_SESSION_TTL_MINUTES` | `180` | Durée de vie d'une table inactive |
| `SESSION_SIGN_KEY` | valeur de développement | **À remplacer en production** : signe les cookies |
| `SEED_DEV_DECK` | `false` | Charge le paquet de démonstration dans une base vide |
| `DISCORD_CLIENT_ID` / `DISCORD_CLIENT_SECRET` | vide | Connexion Discord ; vide = bouton masqué |
| `DISCORD_REDIRECT_URL` | `http://localhost:8080/auth/discord/callback` | URL de retour OAuth |
| `TWITCH_CLIENT_ID` / `TWITCH_CLIENT_SECRET` | vide | Connexion Twitch ; vide = bouton masqué |
| `TWITCH_REDIRECT_URL` | `http://localhost:8080/auth/twitch/callback` | URL de retour OAuth |
| `ADMIN_DISCORD_IDS` | vide | Identifiants Discord admin, séparés par des virgules |
| `ADMIN_TWITCH_IDS` | vide | Comptes Twitch admin (identifiant **ou** nom de chaîne), séparés par des virgules |
| `DISCORD_BOT_TOKEN` | vide | Facultatif : permet à l'administration de retrouver un pseudo Discord depuis un identifiant |
| `ADULT_MIN_ACCOUNT_AGE_DAYS` | `1095` | Âge à partir duquel un compte est cru adulte ; `0` pour n'écouter que la liste |

---

## Connexion Discord et administration

La connexion est **facultative** : sans identifiants configurés, le bouton **« Se connecter »**
de l'en-tête disparaît et le jeu fonctionne normalement. Ce bouton ouvre une fenêtre qui propose
les comptes que le serveur sait gérer — Discord, Twitch — et on se connecte avec **l'un ou
l'autre**. Quel que soit le compte, il sert à proposer le pseudo et à poser la photo de profil
dans la tête de l'avatar.

La connexion Discord sert en plus à ouvrir l'administration, réservée aux identifiants listés
dans `ADMIN_DISCORD_IDS`.

Sur le [portail développeur Discord](https://discord.com/developers/applications), créez une
application, ajoutez `http://localhost:8080/auth/discord/callback` comme *redirect URI*, puis
reportez l'identifiant et le secret dans `.env`. Le scope demandé est `identify`, et rien d'autre.

L'administration s'ouvre aux comptes listés dans `ADMIN_DISCORD_IDS` **ou** `ADMIN_TWITCH_IDS` :
les deux services distribuent de simples nombres, alors les deux listes restent séparées et un
identifiant n'a de sens qu'à côté du service qui l'a émis. Côté Twitch, le **nom de chaîne** est
accepté aussi bien que le numéro : `ADMIN_TWITCH_IDS=kameto` suffit.

L'administration (`/admin`) permet de créer et corriger les packs et les cartes officielles, et
affiche l'activité des 30 derniers jours, les cartes les plus jouées, les meilleurs duos
situation × réponse classés par ratio de votes, ainsi que le nombre de parties et de joueurs
connectés poussé en direct par un WebSocket.

---

## Connexion Twitch et vote du tchat

La connexion Twitch est **facultative** elle aussi, et indépendante de Discord : un joueur peut
n'avoir ni l'une ni l'autre, l'une des deux, ou les deux. Créez une application sur la
[console développeur Twitch](https://dev.twitch.tv/console/apps), ajoutez
`http://localhost:8080/auth/twitch/callback` comme *OAuth Redirect URL*, puis reportez
l'identifiant et le secret dans `.env`. **Aucun scope n'est demandé** : lire le profil du compte
connecté n'en réclame pas, et le tchat est lu anonymement.

Une fois l'hôte connecté avec Twitch, la question **« Qui désigne la meilleure réponse ? »**
gagne un troisième choix, à côté de « tout le monde vote » et du maître du jeu tournant :

> **Le tchat de _votre chaîne_ vote** — les joueurs répondent, puis **eux ne votent plus du
> tout** : chaque réponse porte un numéro, et ce sont les spectateurs qui le tapent dans le
> tchat (`2`, `!2`, `#2`, `!vote 2`).

Choisir ce mode fait apparaître, juste en dessous, **« Inclure les tchats des autres joueurs »**,
qui lit aussi le tchat de chaque joueur connecté avec Twitch — une table de streamers joue alors
devant toutes ses communautés à la fois. Rien de tout cela n'apparaît si l'hôte n'est pas
connecté avec Twitch, et repasser sur un autre mode rend la main à la table.

Le salon suit la même règle partout : une option qui ne peut pas s'appliquer n'est pas grisée,
elle **disparaît** (pas de « cartes en main » en mode sans limites, pas de bonus d'unanimité
quand un maître du jeu décide seul).

**Comment le tchat pèse.** Chaque spectateur vaut **une voix** sur la carte qu'il choisit, et
la réponse la plus choisie gagne la manche — **1 point**, que le tchat compte trente personnes
ou trois mille. Une partie est donc un compte de manches gagnées, pas un sondage d'audience ;
une égalité au sommet donne le point à chacune des réponses concernées. Un spectateur ne vote
qu'une fois par manche — son premier message compte, et taper dans deux tchats à la fois ne
change rien.

Sous chaque réponse, le jeu affiche les **avatars Twitch** des votants, au plus quinze, puis
« +X votes ». Les photos viennent de l'API Twitch, lues avec un jeton applicatif : seules les
quinze premières têtes de chaque réponse sont demandées, et un habitué du tchat n'est cherché
qu'une fois. Sans photo trouvée, l'initiale du pseudo tient lieu de portrait.

**Accès aux packs 18+.** Un compte Twitch y a droit exactement comme un compte Discord : parce
qu'il est administrateur, parce qu'il figure dans la liste d'accès de l'administration, ou parce
qu'il a **plus de trois ans** (`ADULT_MIN_ACCOUNT_AGE_DAYS`). Dans l'administration, il suffit
de taper un **nom de chaîne Twitch** : le serveur retrouve l'identifiant à stocker et remplit le
pseudo tout seul. Sur Discord, le pseudo ne se retrouve qu'avec un `DISCORD_BOT_TOKEN` — sans
lui, l'identifiant numérique reste obligatoire et le nom se saisit à la main. L'âge d'un compte Discord se lit
dans son identifiant ; celui d'un compte Twitch vient de la date de création que Twitch renvoie
à la connexion — dans les deux cas sans appel supplémentaire et sans rien demander au joueur.

Dans ce mode, l'étape de sélection **ne se ferme plus en avance** — il n'y a personne à la table
à attendre : elle va au bout de son chronomètre, pour laisser aux spectateurs le temps de lire
les réponses. Le décompte des tchats s'affiche en direct sous chaque carte, et les consignes
pour les spectateurs restent à l'écran.

Le tchat est lu par IRC anonyme (`justinfan`), en lecture seule : le serveur ne peut écrire
aucun message, et rien n'est conservé — les compteurs vivent le temps de la manche, et un
spectateur n'est retenu que le temps de l'empêcher de voter deux fois.

---

## Tests

Les tests du serveur parlent au **vrai PostgreSQL**, celui de `docker compose`, sur une base
`cardgame_test` qu'ils créent eux-mêmes au premier lancement. Démarrez-le d'abord :

```bash
docker compose up -d postgres
```

```bash
./gradlew test
```

```bash
cd frontend && npm run test:run
```

Les compteurs exacts sortent de `./gradlew test` et de `npm run test:run` ; l'ensemble couvre
le moteur de jeu, les migrations, les dépôts SQL, les routes HTTP, le WebSocket côté JVM, et
les composants côté navigateur.

Aucun H2, aucun dialecte de compatibilité : les tests appliquent les migrations Flyway sur le
même moteur que la production, puis démarrent la vraie application Ktor et rejouent une manche
complète à deux joueurs à travers le vrai protocole WebSocket.

Pour viser une autre base : `TEST_DATABASE_URL`, `TEST_DATABASE_USER`, `TEST_DATABASE_PASSWORD`.

### Migrations de base de données

Le schéma vit dans `server/src/main/resources/db/migration`. Pour le faire évoluer, ajoutez un
fichier `V2__ce_que_ca_fait.sql` à côté de `V1__initial_schema.sql` : Flyway l'applique au
démarrage du serveur, et les tests l'appliquent aussi.

Le *baselining* est volontairement désactivé. Sur une base contenant des tables que Flyway n'a
pas créées, le serveur refuse de démarrer plutôt que de les enregistrer comme déjà migrées —
c'est ce qui évite un serveur qui démarre puis échoue à la première requête.

---

## Surface HTTP et WebSocket

### Public

| Méthode | Chemin | Rôle |
| --- | --- | --- |
| `POST` | `/api/games` | Créer une partie, renvoie le code et le lien du QR code |
| `GET` | `/api/games/{code}` | Aperçu d'une table avant de la rejoindre |
| `POST` | `/api/games/{code}/players` | Prendre une place |
| | | (l'aperçu dit aussi si le visiteur est déjà assis, ce qui fait de `/game/CODE` la seule adresse utile) |
| `GET` | `/api/packs` | Packs officiels disponibles |
| `GET` | `/api/me` | Identité du navigateur |
| `POST` | `/api/logout` | Oublier la session |
| `GET` | `/auth/discord` | Démarrer la connexion Discord |
| `GET` | `/auth/twitch` | Démarrer la connexion Twitch |
| `WS` | `/ws/game/{code}` | La partie elle-même |

### Administration (Discord + allowlist)

| Méthode | Chemin |
| --- | --- |
| `GET` `POST` `DELETE` | `/api/admin/packs` |
| `GET` `POST` `DELETE` | `/api/admin/situations` |
| `GET` `POST` `DELETE` | `/api/admin/punchlines` |
| `GET` | `/api/admin/accounts/{provider}/{query}` (retrouve un compte par identifiant ou nom de chaîne) |
| `GET` | `/api/admin/stats/overview`, `/activity`, `/cards`, `/combos` |
| `WS` | `/ws/admin/stats` |

### Protocole du socket

Chaque message porte un champ `type`.

- **Client vers serveur** : `play`, `write`, `choose`, `settings`, `deck`, `kick`, `start`, `next`, `leave`, `ping`
- **Serveur vers client** : `state` (l'état complet vu par ce joueur), `error` (un code de refus), `pong`

Le client n'a aucune logique de règles : il envoie une intention, et redessine ce que le
serveur lui renvoie.
