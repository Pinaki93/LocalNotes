# LocalNotes

<img src="artwork/local-notes-icon-source.png" alt="LocalNotes app icon" width="160">

LocalNotes is an offline-first Android notes app with an optional web interface for devices on the same local network. Notes live in a Room database on the Android device and can be created and edited from the Jetpack Compose app or, while the built-in server is running, from a browser.

> [!WARNING]
> This is a pet project of mine to test out server capabilities using a Native Android App. I am most likely not going to upload it to any app store or may not maintain this. 

## Features

- Create, view, and edit notes in a native Android app.
- Store note data locally with Room—no cloud account or remote service is required.
- Start an HTTP server from the app and manage the same notes in a browser.
- Discover usable IPv4 addresses for devices connected to the same network.
- Keep the web server alive as an Android foreground service.
- Restore a server that was left enabled after the device reboots.
- Use the JSON REST API directly from other clients on the local network.
- Follow the system light or dark theme in both the Android and browser interfaces.

## Requirements

- Android Studio with Android SDK 37 installed
- An Android device or emulator running Android 7.0 (API 24) or newer
- The repository's Gradle wrapper; no system Gradle installation is needed
- Network access on the first build so Gradle can obtain its distribution, toolchain, and dependencies

The app targets Android API 37. The `server` module compiles against a Java 11 toolchain, while the Gradle daemon toolchain is configured by `gradle/gradle-daemon-jvm.properties`.

## Getting started

1. Clone the repository and enter it:

   ```shell
   git clone <repository-url>
   cd LocalNotes
   ```

2. Open the project in Android Studio and allow Gradle sync to finish.
3. Select the `app` run configuration and an API 24+ device or emulator.
4. Run the app.

You can also build a debug APK from the command line:

```shell
./gradlew :app:assembleDebug
```

The APK is written under `app/build/outputs/apk/debug/`.

## Using LocalNotes

The main notes screen lists notes stored on the device. Create a note from the app navigation, or select an existing note to edit it. Each note contains a title, content, creation time, and last-modified time.

### Browser access

1. Open **Try on Browser** in the Android app.
2. Tap **start browsing on web**.
3. On Android 13 or newer, grant notification permission when prompted. The server runs as a foreground service and displays an ongoing notification.
4. Open one of the displayed addresses, such as `http://192.168.1.20:8080`, on a device connected to the same Wi-Fi or local network.
5. Use the browser interface to list, create, edit, or delete notes.

Stop the server from the app or from the notification action. If the device reboots while the server is enabled, LocalNotes attempts to restore it after boot.

> [!WARNING]
> The web server binds to all network interfaces and currently has no authentication, authorization, or TLS. Anyone who can reach port 8080 can read and change the notes. Only enable it on a trusted local network; do not expose it to the public internet or configure router port forwarding for it.

If another application already uses port 8080, the server cannot start. Local firewall rules, guest Wi-Fi isolation, VPNs, and emulator networking may also prevent another device from reaching the displayed address.

## REST API

The browser UI and REST API are served from the same origin. The API accepts and returns UTF-8 JSON.

Example requests, with the phone address adjusted for your network:

```shell
LOCAL_NOTES_URL=http://192.168.1.20:8080

curl "$LOCAL_NOTES_URL/api/notes"

curl -X POST "$LOCAL_NOTES_URL/api/notes" \
  -H 'Content-Type: application/json' \
  -d '{"title":"From curl","content":"Stored on the Android device"}'

curl -X PUT "$LOCAL_NOTES_URL/api/notes/1" \
  -H 'Content-Type: application/json' \
  -d '{"title":"Updated","content":"New content"}'

curl -X DELETE "$LOCAL_NOTES_URL/api/notes/1"
```

## Architecture

This uses MVVM architecture with a service locator pattern. For the server, it uses MVC.

## Development

Run all JVM unit tests:

```shell
./gradlew test
```

Run the server tests only:

```shell
./gradlew :server:test
```

Run Android lint:

```shell
./gradlew :app:lintDebug
```

Run connected instrumentation tests with a device or emulator available:

```shell
./gradlew :app:connectedDebugAndroidTest
```

Before submitting a change, a useful local check is:

```shell
./gradlew test :app:lintDebug
```

When changing the Room schema, include the updated schema JSON in `app/schemas/` and provide the necessary migration before shipping the change.

## Technology stack

- Kotlin and coroutines
- Jetpack Compose with Material 3
- AndroidX Navigation and Lifecycle
- Room and SQLite
- Kotlin Serialization
- A small socket-based HTTP server implemented in the `server` module
- Gradle Kotlin DSL and a centralized version catalog
