# Extension Twitch — « écrire une carte »

Un panneau sous le stream, et un encart sur l'image, depuis lesquels un spectateur écrit une
situation ou une réponse qui rejoint le paquet de la partie en cours. Le tarif — gratuit,
points de chaîne, ou bits — est choisi par l'hôte **dans le salon du jeu**, pas ici.

L'extension ne sait qu'une chose sur l'endroit où elle tourne : l'identifiant de chaîne que
Twitch signe dans le jeton du spectateur. Le serveur retrouve la table à partir de cet
identifiant seul — jamais à partir de ce que la page raconte —, ce qui fait qu'un spectateur
ne peut pas viser la table de quelqu'un d'autre en bricolant un formulaire.

## Fichiers

| Fichier | Rôle |
| --- | --- |
| `panel.html` | La vue « Panel », sous le lecteur |
| `video_overlay.html` | La vue « Video - Fullscreen », par-dessus l'image |
| `config.html` | La page de configuration du streamer (informative : les réglages vivent dans le salon) |
| `app.js` | Toute la logique, partagée par les trois vues |
| `app.css` | Le style, repris de celui du jeu |
| `config.js` | **La seule ligne à changer** : l'adresse de votre serveur |

## Mise en place

### 1. Créer l'extension

Sur la [console développeur Twitch](https://dev.twitch.tv/console/extensions), créez une
extension et activez les vues **Panel** et **Video - Fullscreen**.

Dans **Asset Hosting**, déclarez :

| Champ | Valeur |
| --- | --- |
| Panel Viewer Path | `panel.html` |
| Video - Fullscreen Viewer Path | `video_overlay.html` |
| Config Path | `config.html` |
| Panel Height | `420` |

Dans **Capabilities**, ajoutez votre serveur à **Allowlist for URL Fetching Domains** — par
exemple `sans-filtre.example`. Sans cela, la politique de sécurité de Twitch bloque
silencieusement tous les appels du panneau.

Si vous voulez les bits, cochez **Bits Enabled** et déclarez vos produits dans
**Monetization → Bits Products**. Les SKU doivent correspondre à ceux du serveur.

### 2. Renseigner le serveur

Deux valeurs, de part et d'autre :

```js
// twitch-extension/config.js
window.CARDGAME_API = 'https://votre-serveur'
```

```bash
# .env du serveur — client id et secret de l'EXTENSION, pas ceux du bouton « Se connecter »
TWITCH_EXTENSION_CLIENT_ID=...
TWITCH_EXTENSION_SECRET=...              # base64, tel que la console l'affiche
TWITCH_EXTENSION_PRODUCTS=carte_100:100,carte_500:500
```

`TWITCH_EXTENSION_PRODUCTS` est ce qui donne son prix à un reçu. Le serveur ne croit jamais
le montant annoncé par le panneau : il lit le SKU du reçu signé par Twitch, et cherche son
tarif dans cette liste. Renommer un produit côté Twitch ne peut donc pas rendre les
propositions gratuites par accident.

### 3. Envoyer et tester

```bash
cd twitch-extension && zip -r ../extension.zip .
```

Téléversez l'archive dans **Files → Asset Hosting**, puis passez l'extension en
**Local Test** ou **Hosted Test** et installez-la sur votre chaîne.

En test local, Twitch sert la page depuis `localhost:8080` : c'est pour cela que le serveur
autorise aussi cette origine en CORS.

## Comment un paiement devient une carte

1. Le spectateur écrit sa carte, puis clique sur le tarif.
2. `Twitch.ext.bits.useBits(sku)` ouvre le paiement ; Twitch rend un **reçu signé**.
3. Le panneau envoie la carte **et le reçu** au serveur.
4. Le serveur vérifie la signature avec le secret de l'extension, lit le SKU, cherche son
   tarif, le compare au minimum demandé par l'hôte — et **consomme le reçu** : Twitch rejoue
   une transaction qu'un panneau n'a pas acquittée, et un spectateur peut rejouer la sienne
   tout aussi facilement.
5. La carte rejoint le paquet, mélangée, à une profondeur aléatoire : personne ne peut
   chronométrer une proposition pour tomber sur une manche précise.

## Points de chaîne

Une récompense de points de chaîne **n'arrive jamais jusqu'à une extension**. Dans ce mode le
panneau ne fait donc que nommer les récompenses et leur prix, et renvoyer le spectateur vers
la liste sous le tchat ; ce qu'il y écrit part directement au jeu.

Ces récompenses n'ont pas à être créées à la main : dans le salon, l'hôte choisit pour chaque
pile une récompense déjà sur sa chaîne, ou en fait créer une par le jeu — auquel cas elle est
retirée à la fin de la partie. Le jeu suit les échanges par EventSub, et sur une récompense
qu'il a créée il peut aussi **rendre les points** d'une carte que la table n'a pas prise.

Sur une récompense créée dans le tableau de bord Twitch, il lit les échanges mais ne peut pas
les valider : Twitch réserve cela à l'application qui a créé la récompense, et le streamer
les valide donc lui-même. Rien de tout cela ne passe par l'extension, ni par son secret :
c'est l'autorisation Twitch donnée par l'hôte dans le salon qui en répond.
