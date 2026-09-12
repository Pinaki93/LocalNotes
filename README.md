# LocalNotes

LocalNotes is an offline-first Android notes app with an optional web interface for devices on the same local network. Notes live in a Room database on the Android device and can be created and edited from the Jetpack Compose app or, while the built-in server is running, from a browser.

<img src="artwork/local-notes-icon-source.png" alt="LocalNotes app icon" width="160">

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

| Method | Path | Purpose | Successful response |
| --- | --- | --- | --- |
| `GET` | `/api/notes` | List all notes | `200 OK` with a JSON array |
| `GET` | `/api/notes/{id}` | Get one note | `200 OK` with a note |
| `POST` | `/api/notes` | Create a note | `201 Created` with the note and a `Location` header |
| `PUT` | `/api/notes/{id}` | Replace a note's title and content | `200 OK` with the updated note |
| `DELETE` | `/api/notes/{id}` | Delete a note | `204 No Content` |

Create and update requests use this shape:

```json
{
  "title": "Shopping list",
  "content": "Coffee\nBread"
}
```

A note response has this shape:

```json
{
  "id": 1,
  "title": "Shopping list",
  "content": "Coffee\nBread",
  "dateModified": 1789209000000
}
```

`dateModified` is Unix time in milliseconds. Invalid JSON returns `400 Bad Request`; an invalid or missing note ID returns `404 Not Found`; and unsupported operations return `405 Method Not Allowed`.

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

The project has two Gradle modules:

| Module | Responsibility |
| --- | --- |
| `app` | Android UI, Room persistence, navigation, dependency container, note controllers, browser pages, and foreground-service lifecycle |
| `server` | Reusable JVM HTTP server plus REST and HTML controller contracts |

The main data flow is:

```text
Compose UI ───────────────┐
                         ├─> NotesRepository ─> Room ─> on-device SQLite database
Browser ─> HTTP server ─> NotesController ─────┘
```

Important components include:

- `NotesRepository`: the note CRUD boundary over Room's `NoteDao`.
- `ServerStateRepository`: stores whether the user left the local server enabled.
- `AppContainer`: constructs and exposes application-scoped dependencies.
- `NotesServer`: connects Android lifecycle commands to the reusable server.
- `NotesServerService`: owns the running server and foreground notification.
- `ServerBootReceiver`: restores an enabled server after boot.
- `Server`: parses HTTP requests, resolves controller routes, and sends responses.
- `NotesController`: maps `/api/notes` requests to repository operations.
- `NotesHtmlController`: maps browser routes to packaged HTML resources.

### Browser routes

| Path | Page |
| --- | --- |
| `/` or `/notes` | Note list and delete actions |
| `/notes/create` | Create form |
| `/notes/update?id={id}` | Edit form |

The pages are static resources under `app/src/main/resources/public/`. They call the REST API with the browser Fetch API and require no separate web build step.

## Project structure

```text
LocalNotes/
├── app/
│   ├── schemas/                    Room schema exports
│   └── src/main/
│       ├── java/.../data/          Entities, DAO, database, repositories
│       ├── java/.../di/            Application dependency container
│       ├── java/.../feature/       Compose screens and view models
│       ├── java/.../navigation/    Navigation graph and commands
│       ├── java/.../server/        Android server integration/controllers
│       └── resources/public/       Browser UI
├── server/
│   └── src/main/kotlin/.../server/ HTTP server and controller interfaces
├── artwork/                        Source artwork
└── gradle/                         Version catalog and wrapper configuration
```

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
