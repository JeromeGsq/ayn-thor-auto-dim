# Ayn Thor Auto Dim

Application Android pour éteindre (afficher un écran noir) sur l'écran secondaire après 10 secondes d'inactivité.

## Fonctionnalités
- Détection de l'écran secondaire (externe/bottom screen).
- Timer de 10 secondes sans interaction tactile.
- Affichage d'un overlay noir (transparent au tactile) pour simuler une veille.
- Réveil automatique au toucher (le toucher est transmis à l'application en dessous).

## Compilation

Le projet utilise Gradle. Assurez-vous d'avoir le SDK Android installé.

### Via Terminal
Utilisez le wrapper Gradle inclus pour assurer la compatibilité des versions :

```bash
chmod +x gradlew
./gradlew assembleDebug
```

L'APK sera généré dans `app/build/outputs/apk/debug/app-debug.apk`.

### Via Android Studio
1. Ouvrez le dossier du projet dans Android Studio.
2. Laissez le projet se synchroniser.
3. Exécutez `Run` ou `Build > Build Bundle(s) / APK(s) > Build APK(s)`.

## Installation et Configuration
1. Installez l'APK sur l'appareil.
2. Lancez l'application "Ayn Thor Auto Dim".
3. **Permissions requises** :
   - **Overlay (Superposition)** : Cliquez sur le bouton pour accorder la permission de dessiner par-dessus les autres applications.
   - **Service d'Accessibilité** : Cliquez sur "Enable Service" pour activer le service d'accessibilité "Auto Dim Service". Ce service est nécessaire pour détecter l'activité tactile et gérer l'overlay sur l'écran externe.

Une fois configuré, l'écran du bas devrait s'assombrir après 10 secondes d'inactivité sur celui-ci.

