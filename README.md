# ParkMate

ParkMate is a native Android app for managing where you park. Register your vehicles, start
and end parking sessions, and get notified when a ticket is about to expire. Every session
records where and when you parked, so you can always find your vehicle again. All data is
stored locally on the device.

## Screenshots

| Map | Vehicles | History | Statistics |
|:---:|:---:|:---:|:---:|
| <img src="docs/screenshots/mappa.png" width="200"> | <img src="docs/screenshots/veicoli.png" width="200"> | <img src="docs/screenshots/storico.png" width="200"> | <img src="docs/screenshots/statistiche.png" width="200"> |

## Features

- **Vehicles**: add, edit and delete cars, motorcycles and bicycles. A deleted vehicle is
  archived, so its past sessions remain in the history.
- **Parking sessions**, three types:
  - *Free*: vehicle, position and start time.
  - *Hourly*: an hourly rate, with elapsed time and cost updated live.
  - *Fixed ticket*: a fixed cost and an expiry time, with a countdown.

  Starting a new session for a vehicle closes its previous one automatically. Each session
  can have a note and a photo.
- **Position**: taken from GPS or from a saved location, and adjustable on a mini-map.
- **Map**: a marker for every parked vehicle, framed automatically. Tap a marker to see the
  session details. Saved locations can be shown as a separate layer.
- **Saved locations**: named places, each with an optional geofence that suggests starting
  or ending a session when you arrive or leave.
- **History**: past sessions, filterable by vehicle type, session type and date.
- **Statistics**: a heat map of where you park and a chart of daily costs, over the last 7
  days, 30 days or all time.
- **Notifications**: a configurable warning before a ticket expires, an alert at the exact
  moment of expiry, and periodic reminders while a session is active.

## Tech stack

- Kotlin, Jetpack Compose, Material 3
- MVVM with LiveData
- Room
- Navigation Compose
- Google Maps (maps-compose) with android-maps-utils heatmaps
- Play Services Location and Geofencing API
- AlarmManager and WorkManager

## Getting started

1. Clone the repository and open it in Android Studio.
2. Get a Google Maps API key with the Maps SDK for Android enabled and add it to
   `local.properties` in the project root (this file is not committed):
   ```
   MAPS_API_KEY=your_key_here
   ```
3. Let Gradle sync, then run the app on a device or emulator with Google Play services,
   running Android 8.0 or later.
