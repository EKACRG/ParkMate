# ParkMate

ParkMate is a native Android app for tracking where you parked, how long you've been there, and
what it's costing you. It runs fully offline: every vehicle, parking session, and saved location
lives in a local database on the device, with no account and no backend.

## Features

- **Vehicles** — full CRUD for cars, motorcycles, and bicycles. Deleting a vehicle archives it
  instead of erasing it: it disappears from pickers and management, but its past sessions stay
  resolvable from the history.
- **Parking sessions**, three types:
  - **Free** — just vehicle, position, and start time.
  - **Hourly paid** — an hourly rate, with elapsed time and accumulated cost shown live while the
    session is active.
  - **Fixed ticket** — a fixed cost and an expiry time, with a countdown and expiry notifications.

  Starting a new session for a vehicle that already has one active closes the previous one
  automatically. Each session can carry a free-text note and a photo taken with the device camera.
- **Position picking** — GPS (auto-filled when permission is already granted, otherwise a manual
  tap on the map), a saved location, or manual adjustment on an embedded mini-map — always
  available as a fallback or a correction.
- **Main map** — shows a marker for every currently parked vehicle (never one for the user's own
  position); it frames itself automatically around whatever is visible, tapping a marker opens
  live session details. Saved locations are a separate, togglable layer.
- **Saved locations** — full CRUD for named, fixed positions, optionally paired with a geofence:
  entering or leaving the area triggers a notification offering to start or end a session there.
- **History** — a filterable chronological list of past sessions (by vehicle type, session type,
  and start date, combinable), with a read-only detail view that reconstructs the full event.
- **Statistics** — two interactive, fully on-device visualizations: a geographic heat map of past
  sessions and a bar chart of cost per day, both filterable by period.
- **Notifications** — a configurable warning before a ticket's expiry plus a mandatory one exactly
  at expiry, periodic reminders while a session is active, and geofence enter/exit prompts.

## Screenshots

| Map | Vehicles | History | Statistics |
|---|---|---|---|
| ![Map](docs/screenshots/mappa.png) | ![Vehicles](docs/screenshots/veicoli.png) | ![History](docs/screenshots/storico.png) | ![Statistics](docs/screenshots/statistiche.png) |

## Tech stack

- **Kotlin** + **Jetpack Compose** with Material 3
- **MVVM**: Compose UI → ViewModel → Repository → Room DAO, with `LiveData` as the observable
  type, consumed via `observeAsState()`
- **Room** for local persistence
- **Maps SDK for Android** via `maps-compose`, with **`android-maps-utils-heatmaps`** for the
  geographic heat map
- **FusedLocationProviderClient** and the **Geofencing API** (`play-services-location`)
- **AlarmManager** (`setExactAndAllowWhileIdle()`) for the exact ticket-expiry alarm
- **WorkManager** (`PeriodicWorkRequest`) for the active-session reminder
- **BroadcastReceiver** for geofence, alarm, and boot-completed events
- **NotificationManagerCompat** with dedicated notification channels
- Camera capture via the implicit `ACTION_IMAGE_CAPTURE` intent and a `FileProvider`
- **Navigation Compose** for navigation
- **Canvas** (Compose) for the cost chart

## Architecture

The codebase is layered (`data/`, `ui/`, `background/`), with `ui/` further split by feature
(map, vehicles, history, stats, settings). Each ViewModel receives its Repository through the
constructor, built once in `ParkMateApplication` and handed out through a hand-written
`ViewModelProvider.Factory`. Repositories are the single source of truth for domain rules — for
example, closing a vehicle's previous active session when a new one starts is enforced there,
inside a Room `@Transaction`, not in the UI layer or the database schema.

## Getting started

1. Clone the repository and open it in Android Studio.
2. Get a Google Maps API key with the **Maps SDK for Android** enabled, and add it to
   `local.properties` (not committed) in the project root:
   ```
   MAPS_API_KEY=your_key_here
   ```
3. Let Gradle sync, then run the app on a device or emulator with Google Play services
   (required for location and geofencing), API level 26 or higher.

## Requirements

- `minSdk` 26, `targetSdk` 37
- Google Play services on the target device/emulator
