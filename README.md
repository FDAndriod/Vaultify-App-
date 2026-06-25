# Vaultify

A secure Android vault app to hide and protect your private photos, videos, documents, and more behind PIN and biometric authentication.

## Features

- **Secure vault** – Store images, videos, audio, and documents in an encrypted-style vault protected by PIN
- **PIN & biometrics** – Unlock with a 4-digit PIN and optional fingerprint setup for quick access
- **Security questions** – Set recovery questions and reset your PIN if you forget it
- **Disguise app icon** – Launch the app from a disguised launcher icon (e.g. Weather, Converter, Music, Calendar, Mealio) so the app looks like something else on your home screen
- **OCR text scanner** – Scan images or documents with the camera; extract and copy text using ML Kit text recognition
- **Media organization** – Browse vault content by type: Images, Videos, Audios, Files, and Docs
- **Image tools** – Crop images, extract text from images, and view images in a gallery-style viewer
- **Video & audio playback** – Play videos and audio files directly inside the app
- **Modern UI** – Built with Jetpack Compose and Material 3, with Lottie animations and a polished theme

## Tech Stack

- **Kotlin** with **Jetpack Compose** for UI
- **Room** for local database (vault items, PIN, security questions)
- **Hilt** for dependency injection
- **Navigation Compose** for in-app navigation
- **WorkManager** for background tasks
- **ML Kit** (Play Services) for on-device text recognition (OCR)
- **AndroidX Biometric** for fingerprint authentication
- **Coil** for image loading

## Requirements

- Android 7.0 (API 24) or higher
- Target SDK 36

## Setup

1. Clone the repository:
2. Open the project in Android Studio (Hedgehog or later recommended).
3. Sync Gradle and run on a device or emulator.
