# HardcoreLives — plugin Paper de vies hardcore

Plugin serveur **Paper 1.21.x**. Zero installation cote joueur : tes potes
rejoignent avec un Minecraft normal. Tout se passe sur le serveur.

Trois modes au choix, switchables dans `config.yml` :

- **LIVES** : chaque joueur a N vies (1 a 5). A 0 vie -> spectateur (ou ban).
- **REVIVE** : comme LIVES, mais un joueur vivant peut reanimer un spectateur
  avec un **Totem de Reanimation** (craftable).
- **POTION** : comme LIVES, mais on regagne une vie en craftant et consommant
  une **Potion de Resurrection**.

Bonus inclus : vies partagees en equipe (pool commun), sortie en spectateur ou
ban, messages personnalisables, persistance sur disque (les vies survivent a un
redemarrage).

---

## 1. Compiler le .jar

Tu as besoin de **Java 21** (verifie avec `java -version`).
Tout le reste est gere par le wrapper Gradle inclus.

Depuis le dossier du projet :

**Windows :**
```
gradlew.bat build
```

**Linux / macOS :**
```
./gradlew build
```

La premiere fois, Gradle se telecharge tout seul (~100 Mo) puis compile.
Le resultat apparait ici :

```
build/libs/HardcoreLives-1.0.0.jar
```

C'est ce fichier que tu deposes sur le serveur.

> Si `./gradlew` refuse de s'executer sous Linux : `chmod +x gradlew` puis relance.

---

## 2. Verifier la version du serveur

Le plugin cible l'API **1.21**. Ton serveur doit tourner sous **Paper 1.21.x**
(1.21, 1.21.1, ... jusqu'aux dernieres 1.21.x). Pour le savoir, tape `version`
dans la console du serveur, ou regarde le panel de ton hebergeur.

- Si le serveur est en **Spigot/Bukkit** classique : le plugin marche aussi
  (il n'utilise que de l'API Bukkit standard), mais Paper est recommande.
- Si le serveur est en **Forge/Fabric** : ce plugin ne marchera pas, il faut un
  serveur Paper/Spigot. Dis-le moi, on adaptera.

---

## 3. Deployer sur ton serveur Bisect (pas a pas)

1. **Compile le jar** chez toi (voir section 1). Tu obtiens
   `build/libs/HardcoreLives-1.0.0.jar`.
2. Connecte-toi au **panel Bisect (Starbase)**.
3. Verifie que le serveur est bien en **Paper 1.21.x** :
   onglet **Startup** -> champ type de serveur / version. Si ce n'est pas
   Paper, change-le pour Paper avant d'aller plus loin.
4. **Demarre une fois** le serveur puis **arrete-le** : ca cree le dossier
   `plugins/` s'il n'existe pas encore.
5. Onglet **Files** -> entre dans le dossier **`plugins/`**.
6. **Upload** `HardcoreLives-1.0.0.jar` (glisser-deposer).
7. **Start** le serveur.
8. Le plugin cree `plugins/HardcoreLives/config.yml`.
9. Edite `config.yml` (onglet Files), choisis ton `mode`, sauvegarde.
10. En jeu (OP) ou en console : `/hcl reload`.

Tes potes rejoignent avec l'IP du serveur, Minecraft normal, rien a installer.

> Ne passe PAS par l'installeur CurseForge/modpack de Bisect : c'est pour les
> mods Forge/Fabric. Toi tu deposes le .jar a la main dans `plugins/`.

---

## 4. Reset de la map (2 methodes)

### Methode A — la plus fiable, via le panel Bisect (recommandee)

Un plugin ne peut pas supprimer le monde pendant que le serveur tourne. La
methode officielle Bisect est la plus sure :

1. Panel Bisect -> **Stop** le serveur.
2. Onglet **Startup** -> champ **World Name** : mets un nouveau nom
   (ex. `world2`, `saison2`...).
3. **Start**. Paper genere une map toute neuve.

Avantage : l'ancienne map n'est pas detruite. Tu peux y revenir en remettant
l'ancien nom dans World Name. C'est le "bouton reset facile" le plus propre.

Variante (efface vraiment) : Stop -> onglet **Files** -> coche le dossier
`world` (et `world_nether`, `world_the_end`) -> **Delete** -> **Start**.

### Methode B — depuis le jeu, via le plugin

Pratique pour remettre les vies a zero et couper le serveur d'un coup, sans
ouvrir le panel :

```
/hcl resetmap            -> affiche un avertissement
/hcl resetmap CONFIRM    -> efface les vies + arrete le serveur
```

Ensuite, comme le serveur est arrete, applique la **Methode A** (changer le
World Name ou supprimer le dossier) puis redemarre. La commande te fait gagner
les etapes "reset des vies" et "arret propre", mais la regeneration de map se
fait toujours serveur eteint.

> Astuce : garde la Methode A (champ World Name) comme reflexe. C'est une seule
> ligne a changer dans le panel, reversible, et ca ne casse jamais rien.

---

## 4. Choisir le mode

Apres le premier demarrage, edite `plugins/HardcoreLives/config.yml` :

```yaml
mode: LIVES        # ou REVIVE ou POTION
starting-lives: 3  # 1 a 5
on-death-out: SPECTATOR   # ou BAN
```

Puis en jeu (en tant qu'OP) ou en console :

```
/hcl reload
```

Pas besoin de redemarrer pour la plupart des reglages. Exception : changer
`mode` vers/depuis POTION enregistre une recette de craft au demarrage, donc
pour celui-la, fais un **restart complet** du serveur.

---

## 5. Recettes de craft

**Totem de Reanimation** (mode REVIVE) :
```
Bloc d'or   Diamant     Bloc d'or
Diamant     Totem*      Diamant
Bloc d'or   Diamant     Bloc d'or
```
*Totem = Totem d'immortalite vanilla.

**Potion de Resurrection** (mode POTION) :
```
Bloc d'or   Diamant         Bloc d'or
Diamant     Etoile du Nether Diamant
Bloc d'or   Diamant         Bloc d'or
```

Tu peux ajuster la difficulte de ces recettes dans `ReviveManager.java` /
`ResurrectionPotion.java` (methode `registerRecipe`) avant de recompiler.

---

## 6. Commandes

| Commande | Effet | Qui |
|---|---|---|
| `/lives` | Affiche tes vies | tous |
| `/lives <joueur>` | Affiche les vies d'un joueur | tous |
| `/hcl reload` | Recharge la config | admin |
| `/hcl mode` | Affiche le mode actif | admin |
| `/hcl set <joueur> <n>` | Fixe les vies | admin |
| `/hcl give <joueur> <n>` | Ajoute des vies | admin |
| `/hcl revive <joueur>` | Reanime de force | admin |
| `/hcl resetmap CONFIRM` | Efface les vies + arrete le serveur (voir section 4) | admin |

"admin" = joueur OP par defaut (permission `hardcorelives.admin`).

---

## 7. Comment utiliser le mode REVIVE en jeu

1. Un joueur meurt, epuise ses vies -> il passe spectateur.
2. Un joueur vivant craft un **Totem de Reanimation**.
3. Il s'approche a moins de 5 blocs du spectateur a reanimer.
4. **Clic droit** avec le totem en main, reste immobile quelques secondes.
5. Le spectateur revient a la vie a cote de lui. Le totem est consomme.

---

## 8. Structure du projet (pour modifier le code)

```
hardcorelives/
├── build.gradle              # config de build (version API ici)
├── settings.gradle
├── gradlew / gradlew.bat     # wrapper, lance le build
├── gradle/wrapper/...
└── src/main/
    ├── java/fr/filipe/hardcorelives/
    │   ├── HardcoreLives.java      # classe principale, commandes
    │   ├── LivesManager.java       # compteur de vies + persistance
    │   ├── DeathListener.java      # logique a la mort
    │   ├── ReviveManager.java      # mode REVIVE (totem)
    │   ├── ResurrectionPotion.java # item potion
    │   └── PotionListener.java     # mode POTION
    └── resources/
        ├── plugin.yml              # declaration du plugin
        └── config.yml             # config par defaut
```

---

## Notes

- Les vies sont sauvegardees dans `plugins/HardcoreLives/lives.yml`.
- Pour repartir a zero (nouvelle partie), supprime ce fichier et restart.
- Si tu veux des blocs/objets/textures vraiment custom (pas juste des items
  vanilla renommes), il faudra passer cote Fabric avec install client. Dis-le
  moi si besoin.
