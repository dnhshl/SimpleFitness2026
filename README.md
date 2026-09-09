# 🏃 SimpleFitnessSQL Educational App

Welcome to **SimpleFitnessSQL**! 

This learning session focuses purely on **Local Persistence (Room DB)**. It builds directly upon our previous work where we introduced **Adaptive UI** (orientation support) and **Advanced Visualization** (real-time time-series graphs).

---

## 🏗️ Building on Previous Concepts
To get the most out of this session, remember how we previously handled:
*   **Adaptive Navigation**: The app automatically swaps between a `NavigationBar` (Portrait) and a `NavigationRail` (Landscape) using a "Configuration-First" approach in `AppConfig.kt`.
*   **Advanced Visualization**: The `GraphScreen.kt` uses a real-time X-axis where spacing reflects actual time intervals between measurements.

---

## 💾 NEW: Learning Module: Local Persistence (RoomDB)
In this version, we solve the problem of data loss. By adding **RoomDB**, the app now has a "Digital Memory."

### 🏗️ The Architecture of Memory
1.  **Entity**: `FitnessEntity.kt` defines the table structure (Timestamp, BPM, Fitness).
2.  **DAO**: `FitnessDao.kt` contains the SQL logic (Store, Retrieve, Filter).
3.  **Local Repository**: `LocalFitnessRepository.kt` manages the database operations.
4.  **Master Polling Switch**: Located on the **Home Screen**, this is the "Engine Start" button. It controls network polling globally for the entire app.
5.  **Reactive Switching**: The `GraphScreen.kt` now has a switch to toggle between **Live Data** (last 100 points in RAM) and **Historic Data** (everything saved in the DB).

---

## 🎓 The Re-Engineering Lab: Trace the Data Flow
To truly understand how this professional-grade app works, you must "trace" the path of data from the internet all the way to the local database and the screen.

### 1. Data Ingress (The Start)
*   **File**: `repositories/HttpRepository.kt`
*   **Explore**: See how **Ktor** handles the network request and how `kotlinx-serialization` turns a raw JSON string into a structured `FitnessData` object.

### 2. The Engine (Master Control)
*   **File**: `statemgmt/AppViewModel.kt` -> Look at `setPollingEnabled(...)` and `startPollingInternal()`.
*   **Explore**: Notice how we use a single `Job` to manage the background loop. This job only runs when the "Live Streaming" switch on the Home Screen is ON.

### 3. The Persistence "Pipe" (Saving)
*   **File**: `statemgmt/AppViewModel.kt` -> Look inside the `performHttpRequest()` function.
*   **Explore**: Notice the call to `localRepository.saveReading(...)`. Every cloud reading is automatically piped into the "Digital Memory."

### 4. The Reactive Loop (The Magic)
This is the core of modern Android. We use the **Single Source of Truth (SSOT)** pattern.
*   **The Brain**: `AppViewModel.kt` defines `fitnessHistoryLog`. It uses `flatMapLatest` to automatically refresh your training log whenever the filters or the polling state change.
*   **The UI**: `HistoryScreen.kt` collects this flow. It doesn't know about the network; it only knows about the database.

### 5. Adaptive Scrolling
*   **File**: `ui/screens/GraphScreen.kt` -> Look at the `scrollState` definition.
*   **Explore**: See how the graph decides to "Auto-Scroll" only when in Live mode. In History mode, auto-scroll is disabled so you can manually inspect the log.

---

## 🛠️ Student Tasks

### Task 1: Analyze the "Digital Memory"
Open `AppViewModel.kt` and find the `activeGraphData` flow.
1.  **Identify**: How does the app decide which list of data to send to the graph when you flip the switch?
2.  **Logic**: Why do we use `.reversed()` when loading data from the database for the graph? (Hint: Look at the `ORDER BY` in `FitnessDao.kt`).

### Task 2: Advanced Filtering (The Sports Engineer's Task)
Currently, the History tab allows filtering by **Time** AND **Heart Rate**. Your task is to add a third filter: **Fitness Score**.
1.  **DAO**: Update `getFilteredLogs` in `FitnessDao.kt` to include a minimum fitness score check (e.g., `AND fitnessScore >= :minFitness`).
2.  **ViewModel**: Update the `uiState` to include `minFitnessFilter` and integrate it into the `flatMapLatest` logic.
3.  **UI**: Add another `Slider` to `HistoryScreen.kt` for the Fitness Score.

### Task 3: The "Sports Data Science" Mini-Project
1.  **Analysis**: Can you calculate the **Average Heart Rate** of all stored sessions and display it at the top of the History tab? (Hint: Use Kotlin's `.map { it.heartRate }.average()` on the history list).
2.  **UI**: Add a "Clear All" button in the `HistoryScreen.kt` header that calls `viewModel.clearHistory()`.

---

## 📚 Helpful Resources
*   **Room Database**: [Official Android Guide](https://developer.android.com/training/data-storage/room)
*   **Vico Documentation**: [Axis customization](https://guide.vico.patrykandpatrick.com/core/axis/)

---

## 📁 Project Quick-Link
*   **The DB**: `db/` folder -> Entity, DAO, and Database definition.
*   **The Logic**: `AppViewModel.kt` -> Orchestrates Web, DB, and Navigation.
*   **The Config**: `AppConfig.kt` -> The central configuration for the adaptive UI.
