# GBA 4K Ultimate — base de projet Android

## État réel du livrable

Ce ZIP contient une **base de projet Android Studio / prototype d'interface**, pas un APK final et pas encore un émulateur jouable. L'interface démarre en paysage immersif, permet de sélectionner un fichier et présente les réglages graphiques envisagés.

Le jeu ne peut pas encore démarrer : il manque l'intégration complète du frontend LibretroDroid avec un cœur mGBA natif (`mGBA_libretro_android.so` / bibliothèque compatible), le cycle d'émulation, l'audio, les entrées tactiles/manette et les sauvegardes. Les curseurs graphiques sont pour l'instant visuels et ne pilotent pas encore de shaders.

## Construction

1. Installer Android Studio récent avec Android SDK 35 et JDK 17.
2. Ouvrir ce dossier dans Android Studio.
3. Laisser Gradle synchroniser les dépendances (connexion Internet nécessaire).
4. Compiler `app > Tasks > build > assembleDebug`.
5. Le paquet de test serait généré dans `app/build/outputs/apk/debug/`.

Aucun APK compilé n'est inclus, car aucun SDK Android/outil Gradle n'est disponible dans l'environnement qui a créé cette archive.

## Moteur prévu

Le projet référence LibretroDroid 0.13.2, frontend Android qui annonce tester le cœur mGBA. Références :
- LibretroDroid : https://github.com/Swordfish90/LibretroDroid
- mGBA core : https://docs.libretro.com/library/mgba/

Le cœur mGBA doit être compilé/embarqué pour les ABI Android requises (au minimum `arm64-v8a` pour le Galaxy S25+), puis chargé par le frontend. Il faut également vérifier les obligations de licence GPL-3.0 de LibretroDroid et MPL-2.0 de mGBA, fournir les notices et le code source correspondant selon le mode de distribution.

## IA et remaster

Une reconstruction IA locale n'est pas incluse dans cette base. Pour une vraie fonction locale, il faudra intégrer un modèle d'upscaling adapté aux sprites 2D (par exemple un modèle ONNX/TFLite sous licence compatible), mesurer son coût GPU/NPU sur le S25+, et prévoir un mode shader plus léger. Un filtre de lissage ne doit pas être présenté comme une IA de reconstruction.

## ROM et BIOS

Aucune ROM commerciale ni BIOS Nintendo n'est fourni. L'utilisateur doit importer ses propres fichiers légalement obtenus.
