# 🏃 SimpleFitness Educational App

Welcome to the **SimpleFitness** project! Building upon your experience with the **SimpleSensor** app, this project takes the next big step: **Networking** and **Advanced Data Visualization**.

Instead of reading local hardware sensors, this app communicates with a remote server to fetch "Live" fitness data and displays it in a professional-grade scrollable graph.

## 🎓 The "Discovery" Approach
We continue our **"Reverse Engineering"** approach:
1.  **Explore**: Run the app and watch the pulse graph update in real-time as data comes in from the cloud.
2.  **Understand**: Trace the path from a JSON string on the internet, through the **Ktor** client, into a Kotlin data class, and finally onto the **Vico** chart.
3.  **Modify**: Your goal is to customize the dashboard, handle new data fields, and tune the graphing behavior.

---

## 🏗️ Architecture: The Web Pipeline
We follow the same 3-layer architecture, but the "source" has changed from hardware to the web:

### 1. The Repository (The "Gateway")
*   **Location**: `com.example.main.repositories.HttpRepository`
*   **Purpose**: This layer manages the internet connection.
*   **Concepts**: 
    *   **Ktor**: An asynchronous HTTP client for Android.
    *   **JSON Serialization**: Using `kotlinx-serialization` to automatically turn a JSON string (like `{"puls": 80, ...}`) into a `FitnessData` object.

### 2. The ViewModel (The "Orchestrator")
*   **Location**: `com.example.main.statemgmt.AppViewModel`
*   **Purpose**: Handles the **Polling Logic**.
*   **Concept**: It uses a "Pulse" timer to fetch new data every 2 seconds and maintains a `fitnessHistory` list that powers the graph.

### 3. The UI (The "Dashboard")
*   **Location**: `com.example.main.ui.screens`
*   **Purpose**: Visualizing data using **Jetpack Compose** and **Vico**.
*   **Key Files**: 
    *   `HomeScreen.kt`: Shows the raw current values.
    *   `GraphScreen.kt`: The advanced graphing implementation.
    *   `Helper.kt`: Contains `TimeHelper` which handles the tricky conversion from UTC "Web Time" to your local Time Zone.

---

## 📈 Key Technologies

### 🌐 Ktor & JSON
The app talks to a REST API. You can see the data structure in `UiState.kt`. Notice how the `@Serializable` annotation tells the compiler how to handle the data.

### 📊 Vico Graphing (v3.0)
We use the **Vico** library for the line chart.
*   **Real-time**: The `CartesianChartHost` updates instantly when new data arrives.
*   **Interaction**: You can pinch to zoom and swipe to scroll through the history.
*   **Formatting**: Look at the `xAxisValueFormatter` in `GraphScreen.kt` to see how we turn indices into timestamps.

---

## 🛠️ Your Tasks

### Task 1: Add the "Fitness" Score
Currently, the graph only shows the **Pulse**. The API also returns a `fitness` value (a double).
1.  Modify `GraphScreen.kt` to either show a second line for fitness or switch the current line to display the fitness value instead of pulse.
2.  Update the Y-axis scale (in `rangeProvider`) to reflect the range of the fitness score.

### Task 2: UI Styling
Customize the look of the graph in `GraphScreen.kt`:
1.  Change the color of the line and the dots to something else (e.g., `Color.Red` or `Color.Green`).
2.  Change the `CircleShape` of the dots to a `RectangleShape` or something custom.

### Task 3: Polling Control
1.  Find where the polling interval is defined. Can you make the app update every 5 seconds instead of 2?
2.  What happens to the graph's auto-scroll behavior when you change the interval?

---

## 📚 Helpful Resources
*   **Ktor Documentation**: [Getting started with Ktor on Android](https://ktor.io/docs/http-client-android.html)
*   **Vico Documentation**: [Vico Graphing Guide](https://guide.vico.patrykandpatrick.com/)
*   **Kotlin Serialization**: [JSON handling in Kotlin](https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/json.md)

---

## 📁 Project Quick-Link
*   **Networking**: `HttpRepository.kt` -> Where the web requests happen.
*   **Logic**: `AppViewModel.kt` -> Where the data is polled and stored.
*   **Data Structure**: `UiState.kt` -> Where the JSON format is defined.
*   **Time Logic**: `Helper.kt` -> Where Time Zones are managed.
*   **UI Layout**: `ui/screens/` -> Where the screens and charts are designed.

---

## ⚖️ License
This project is licensed under the **MIT License**.
