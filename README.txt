VITESSE INTERNET — itel P32 (Android 8+)

Fonctions :
- Affiche la vitesse réseau reçue + envoyée, mise à jour chaque seconde.
- Notification persistante pour consulter la vitesse en arrière-plan.
- Boutons DÉMARRER et ARRÊTER.

Installation / création de l’APK :
1. Sur un ordinateur, installe Android Studio.
2. Décompresse InternetSpeedP32.zip.
3. Dans Android Studio, choisis Open et sélectionne le dossier InternetSpeedP32.
4. Attends la synchronisation Gradle.
5. Menu Build > Build Bundle(s) / APK(s) > Build APK(s).
6. Copie app/build/outputs/apk/debug/app-debug.apk sur le téléphone et installe-le.
   Si demandé, autorise l’installation depuis cette source.

Compatibilité : Android 8.0 (API 26) et plus.
Remarque : l’application mesure le trafic total du téléphone (Wi‑Fi ou données mobiles),
réception + envoi. Ce n’est pas un test de débit maximal comme Speedtest.

Compilation en ligne avec GitHub Actions :
1. Importer le contenu de ce dossier dans un dépôt GitHub, à la racine (le dossier app, settings.gradle, build.gradle et .github doivent être à la racine du dépôt).
2. Faire un commit/push sur la branche main, ou ouvrir Actions et lancer "Build Android APK" avec Run workflow.
3. Ouvrir le run terminé puis télécharger l'artefact "VitesseInternet-debug-apk".
4. Extraire app-debug.apk et l'installer sur le téléphone.
Le workflow utilise JDK 17 et Gradle 8.7, sans avoir besoin d'un fichier gradlew.
