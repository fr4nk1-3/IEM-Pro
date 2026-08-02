# IEM Personal Monitor Mixer

A modern, high-performance Android application built with **Kotlin** and **Jetpack Compose** for controlling personal In-Ear Monitor (IEM) mixes on **Behringer & Midas** digital mixing consoles (X32, M32, XR18, MR18, X-Air).

---

## Key Features

### 1. Streamlined Setup Flow
- **Console & IP Selection**: Easily connect to your hardware mixer over Wi-Fi by entering the IP address.
- **Persistent IP Memory**: Keyed-in IP addresses are stored and retained across connection drops, app restarts, and profile updates.
- **Mixbus Locking**: Step 2 of the setup flow prompts musicians to pick and lock their assigned monitor MixBus (Buses 1–16).
- **Settings Flexibility**: Musicians can quickly switch their assigned MixBus anytime from the Settings/Profiles screen.

### 2. Custom Musician Profiles
- Store and switch between custom profiles (e.g., Drummer, Vocalist, Guitarist, Bass, Keys).
- Save profile preferences, favorite channels, theme accents, and custom channel groupings to local **Room database**.

### 3. Channel MCA Submix Groups (Off by Default)
- Optional custom MCA (Mix Control Association) groups to adjust multiple channel sends with a single master slider (e.g., "Drums", "Vocals", "Guitars").
- **Groups Disabled by Default**: Kept clean by default for simple mixing; toggleable per profile in Settings.
- **Clean Group Lifecycle**: Deleting custom groups immediately cleans up fader displays, tabs, and submix levels without leaving leftover items.

### 4. Robust Real-Time OSC Networking
- High-speed **UDP OSC (Open Sound Control)** client supporting X32/M32 (port 10023) and X-Air/M-Air (port 8900).
- **Automated Heartbeat Keep-Alive**: Sends `/xremote` keep-alives every 2.5s to ensure continuous subscription to console parameter changes.
- **Auto Reconnection & Recovery**: Seamlessly recovers from temporary network drops without losing persistent IP configuration.
- **Built-in Offline Simulator**: Virtual console simulator for testing UI and faders offline without hardware.

### 5. Landscape-Optimized Ergonomic UI
- Designed for rapid stage adjustments with large touch targets (48dp+), neon status badges, bank navigation (groups of 4 channels), and responsive channel meters.

---

## Tech Stack & Architecture

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose & Material Design 3
- **Architecture**: MVVM (Model-View-ViewModel) + Clean Architecture principles
- **Asynchronous State**: Kotlin Coroutines & `StateFlow`
- **Data Persistence**: Room Database (`UserProfileEntity`, `MixerDeviceEntity`, `MixPresetEntity`)
- **Networking**: Custom Java DatagramSocket UDP client (`OscSocketClient`) for bi-directional OSC protocol messages

---

## Project Structure

```
app/src/main/java/com/example/
├── data/
│   ├── AppDatabase.kt          # Room Database definition
│   ├── IemRepository.kt        # Local data repository
│   ├── UserProfileEntity.kt    # Musician profile entity
│   └── MixerDeviceEntity.kt    # Saved mixer connections
├── model/
│   ├── ChannelState.kt         # Live channel send levels & mutes
│   ├── MixBusState.kt          # Bus master levels & names
│   ├── MixerModelInfo.kt       # Mixer hardware metadata
│   └── ConnectionStatus.kt     # Network state enum
├── network/
│   ├── OscSocketClient.kt      # UDP socket client with heartbeat & reconnect logic
│   ├── OscMessage.kt          # OSC binary encoder/decoder
│   └── VirtualMixerSimulator.kt # Offline mixer simulation engine
├── ui/
│   ├── IemViewModel.kt         # Single source of truth for UI state
│   ├── screens/
│   │   ├── DashboardScreen.kt     # Main IEM mixing faders & banks
│   │   ├── DiscoveryScreen.kt     # Step 1: Console discovery & IP input
│   │   ├── MixbusSelectionScreen.kt # Step 2: Monitor Mixbus selection & lock
│   │   ├── ProfilesScreen.kt      # Profile management & group settings
│   │   ├── EngineerScreen.kt      # Master engineer override console
│   │   └── DiagnosticsScreen.kt   # Network latency & OSC packet stats
│   ├── components/            # Reusable UI widgets & channel cards
│   └── theme/                 # Dark aesthetic colors & typography
└── MainActivity.kt             # Navigation host & entry point
```

---

## Getting Started

### Prerequisites
- Android Studio Ladybug (or newer)
- JDK 17
- Android SDK 34+
- Physical Android device connected to the same Wi-Fi network as your mixing console (or use the built-in Offline Virtual Simulator).

### Build & Run
1. Open the project directory in Android Studio.
2. Build and run on an Android device or emulator.
3. On first launch, select your mixer model and enter its local IP address (e.g., `192.168.1.100`), or select **Virtual Offline Console**.
4. Select your assigned MixBus (1–16) on the **Select MixBus** setup page.
5. Tap **Lock MixBus & Start Mixing** to enter the mixing dashboard!
