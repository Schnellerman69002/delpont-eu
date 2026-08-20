# Morphée 🌙

Application Android pensée pour se rendormir pendant les insomnies :
lecteur local de musiques et de podcasts, playlists, et **timer de sommeil
qui s'arme automatiquement à chaque lancement de la lecture** — y compris
depuis le bouton Play d'une télécommande Bluetooth, téléphone verrouillé.

## Fonctionnement nocturne visé

1. Vous préparez une playlist (musiques et/ou podcasts locaux) et réglez la
   durée du timer (par défaut 20 min).
2. La nuit, un appui sur **Play** de la télécommande Bluetooth reprend la
   playlist **exactement là où elle s'était arrêtée** et relance le timer.
3. À l'expiration du timer, le volume baisse en fondu (10 s par défaut),
   la lecture se met en pause et la position est sauvegardée.
4. Nouveau réveil → nouvel appui sur Play → la lecture repart du même
   endroit, pour la durée du timer. Les boutons volume / piste suivante /
   précédente de la télécommande fonctionnent aussi (AVRCP standard).

## Fonctionnalités

- **Bibliothèque locale** : analyse des fichiers audio du téléphone via
  MediaStore, avec filtres Musique / Podcasts (détection par le tag podcast
  ou par un dossier contenant « podcast »).
- **Playlists** : création, suppression, ajout de pistes, réordonnancement,
  mélange musiques + podcasts possible.
- **Timer de sommeil** : durée réglable par pas de 5 min, armement
  automatique à chaque lecture (désactivable), prolongation « +5 min »,
  fondu de volume configurable.
- **Reprise de lecture** : file d'attente et position persistées ; la
  reprise fonctionne même si Android a tué l'application (mécanisme
  officiel de *playback resumption* de Media3).
- **Interface nuit** : fond noir pur, texte blanc, accent ambre (faible
  lumière bleue), gros boutons, contraste élevé.

## Compatibilité

- `minSdk 26` (Android 8.0) → `targetSdk 35` : fonctionne sur Android 16.
- Télécommandes multimédia Bluetooth standard (AVRCP) : play/pause,
  suivant, précédent, volume.

## Compilation

### Via GitHub Actions (recommandé)

Chaque push touchant `android-app/` déclenche le workflow **Android APK**
qui produit l'artefact `morphee-debug-apk` (APK installable directement).
Onglet *Actions* du dépôt → dernier run → *Artifacts*.

### En local

Ouvrir `android-app/` dans Android Studio, ou :

```bash
cd android-app
./gradlew assembleDebug
# APK : app/build/outputs/apk/debug/app-debug.apk
```

## Installation

1. Télécharger `app-debug.apk` sur le téléphone.
2. Autoriser l'installation de sources inconnues, installer.
3. Au premier lancement, accorder l'accès aux fichiers audio et aux
   notifications.
4. Appairer la télécommande Bluetooth ; lancer une première fois la
   playlist depuis l'application pour initialiser la file de lecture.

## Architecture

| Composant | Rôle |
|---|---|
| `playback/PlaybackService` | Service Media3 (`MediaSessionService`) : session média, boutons Bluetooth, reprise de lecture, timer + fondu |
| `playback/TimerBus` | Pont UI ↔ service pour l'état et les commandes du timer |
| `data/MediaLibrary` | Analyse MediaStore |
| `data/PlaylistRepository` | Playlists persistées en JSON |
| `data/PlaybackStateStore` | File d'attente + position sauvegardées |
| `data/SettingsRepository` | Réglages (DataStore) |
| `ui/` | Interface Jetpack Compose, thème nuit à contraste élevé |
