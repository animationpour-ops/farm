# Néant — mod Forge 1.20.1

Thème : **le Néant**. Un mod pensé pour une vidéo « Minecraft mais… » rapide : peu d'objets, mais chacun a un pouvoir
très destructeur, avec des effets visuels (VFX) et des sons faits maison. Rien n'est une copie de Minecraft : les VFX
sont dessinés par un moteur d'effets personnalisé (halos additifs, rubans, éclairs, anneaux, trou noir…).

## La boucle de jeu

1. **Une faille du Néant s'ouvre dans le ciel** près de toi (toute seule, ou avec `/neant faille`).
   Des créatures du Néant (zombies, squelettes, araignées, endermen boostés, avec une aura rouge) tombent par vagues,
   puis le **Gardien du Néant** apparaît (barre de boss). Tout ce qui meurt laisse des **fragments de Néant**.
2. **Fabrique la Forge du Néant** : 8 obsidiennes + 1 œil d'Ender (recette en carré, œil au centre).
3. **Jette sur la forge** un objet de base + des fragments, puis **clic droit** sur la forge : un rituel s'active
   (les objets lévitent, foudre, onde de choc) et l'objet du Néant apparaît.

| Objet du Néant | Base à jeter | Fragments |
|---|---|---|
| Casque | casque en diamant | 20 |
| Plastron | plastron en diamant | 32 |
| Jambières | jambières en diamant | 28 |
| Bottes | bottes en diamant | 16 |
| Épée | épée en diamant | 24 |
| Pioche | pioche en diamant | 24 |
| Pelle | pelle en diamant | 16 |
| Hache | hache en diamant | 24 |
| Briquet | briquet (silex et acier) | 20 |

## Les pouvoirs

- **Épée** : chaque coup ouvre une entaille dans le Néant, blesse et aspire les ennemis proches, et te soigne.
  *Clic droit* : onde de lame de 36 blocs qui tranche le terrain et projette tout.
- **Pioche** : casse une zone 5×5×3 d'un coup (les drops volent jusqu'à toi). *Clic droit* : la **Foreuse**,
  un faisceau qui perce un tunnel de 46 blocs.
- **Pelle** : efface d'un coup toute la terre / le sable / le gravier connectés. *Clic droit* : un **trou noir**
  qui aspire et dévore tout, terrain compris.
- **Hache** : abat l'arbre entier, feuilles comprises. *Clic droit* : **tourbillon de lames** autour de toi.
- **Briquet** : *clic* sur un bloc ou dans les airs : **vague de feu du Néant** qui s'étend sur 17 blocs
  (embrase tout, fait exploser la TNT).
- **Armure complète** : aura qui brûle et aspire tout dans 6 blocs, sprint « bulldozer » (tu écrases tout),
  immunité aux chutes et au vide (le Néant te ramène au dernier sol sûr), résistance II, régénération, immunité au feu.
  Chaque pièce donne aussi un bonus (vision nocturne, résistance, vitesse, saut).

Les outils ont une recharge pour leur pouvoir en clic droit. Les textures vanilla sont réutilisées pour les outils et
l'armure en inventaire ; l'armure portée utilise une texture « portail de l'End » modifiée.

## Commandes (niveau 2 / opérateur)

- `/neant faille` : ouvre une faille tout de suite.
- `/neant fragments <n>` : te donne n fragments.
- `/neant kit` : te donne tout l'équipement du Néant (pour tester ou lancer la vidéo).

## Réglages (`config/neant-common.toml`)

- `autoRifts` (true) : failles automatiques. La première arrive ~45 s après l'arrivée dans le monde.
- `riftIntervalSeconds` (180) : délai entre deux failles.
- `shardMultiplier` (1) : multiplie les fragments lâchés (farm accéléré pour la vidéo).
- `terrainDestruction` (true) : mets `false` si tu veux que les pouvoirs ne détruisent pas le décor.

## Compiler avec GitHub (sans rien installer)

1. Crée un dépôt GitHub vide, puis envoie-y **tout le contenu de ce dossier** (y compris `.github`, `gradle`, `gradlew`).
2. Onglet **Actions** → le workflow « Build mod » se lance tout seul (ou **Run workflow**).
3. Quand il est vert, ouvre l'exécution → **Artifacts** → télécharge `neant-jar` → dans le zip, prends le `.jar`.
4. Mets le `.jar` dans le dossier `mods` d'un Minecraft **1.20.1 avec Forge 47.3.0** (ou plus récent en 47.x).

> Ce projet n'a pas pu être compilé là où il a été écrit (pas d'accès aux dépendances de Minecraft). S'il y a une
> erreur de compilation, copie le message rouge de l'onglet Actions : ce sera une correction d'une ligne.

## Idées pour la vidéo

- `/neant fragments 200` + `/neant kit` pour montrer les pouvoirs en 10 secondes, puis refaire « à la main » :
  faille → combat → fragments → forge.
- Mets `shardMultiplier` à 5 ou 10 pour un farm rapide.
- Joue en 1.20.1 sans shaders pour que les halos additifs ressortent au maximum.
