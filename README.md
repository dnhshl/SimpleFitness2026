# 🏃 SimpleFitness Educational App

Welcome to the latest version of the **SimpleFitness** project! 

While the initial version of **SimpleFitness** (from last term) was built upon your experience with the **SimpleSensor** app, this updated version takes that foundation and adds professional architectural concepts: **Adaptive UI** and **Advanced Visualization**.

## 🎓 Learning Module: Configuration & Adaptivity
This version introduces a "Configuration-First" approach. Instead of hard-coding buttons and bars, we define the app structure in one place (`AppConfig.kt`), and the UI adapts automatically.

### 📱 1. Adaptive Navigation (The "Rail" Pattern)
Mobile apps must work in both **Portrait** and **Landscape**. 
*   **Portrait**: The app uses a standard `NavigationBar` (Bottom Bar).
*   **Landscape**: To save vertical space for the graph, the app automatically switches to a `NavigationRail` (Side Bar) and hides the `TopAppBar`.
*   **The Magic**: Both components are driven by the same list of screens in `AppConfig.kt`.

### 📊 2. Graph Customization (Vico Library)
We've moved beyond "default" charts to a professional, readable implementation:
*   **Axis Control**: We use `ItemPlacer` to control how often labels appear.
*   **Stability**: We fixed the "flicker" by giving the Y-axis a fixed width. This is a common requirement in live-data dashboards.

---

## 🏗️ Architecture Reminder
We strictly follow the 3-layer architecture:
1.  **Repository**: `HttpRepository.kt` manages the **Ktor** connection.
2.  **ViewModel**: `AppViewModel.kt` handles data polling and the navigation state.
3.  **UI**: `ui/screens/` displays the data. The entry point is `MainActivity.kt`.

---

## 🛠️ Student Tasks

### Task 1: The "Automatic" UI (Configuration)
Your "Control Center" is `AppConfig.kt`. 
1.  **Modify**: Change the icons for the `Home` or `Graph` screens in `AppScreenConfigs`.
2.  **Verify**: Run the app and rotate your phone. Notice how your new icons appear in both the Bottom Bar AND the Side Rail automatically.

### Task 2: Adding a New Feature
Suppose you want to add a "Settings" tab.
1.  **Action**: Add a new `ScreenConfig` object to the list in `AppConfig.kt`. Give it a title and an icon.
2.  **Result**: Without touching any layout code, your new tab should appear in the navigation. (Note: Clicking it will show an error until you also add it to the `when` block in `MainActivity.kt`).

### Task 3: Graph Precision
In `GraphScreen.kt`, the Y-axis shows a label every 20 BPM.
1.  **Modify**: Find the `VerticalAxis.ItemPlacer.step` and change it to show a label every **10 BPM**.
2.  **Analyze**: Look at the `size = BaseAxis.Size.Fixed(72.dp)` line. Why is this fixed size important when the heart rate changes from 99 to 100?

---

## 📚 Helpful Resources
*   **Vico Documentation**: [Axis customization](https://guide.vico.patrykandpatrick.com/core/axis/)
*   **Material 3**: [Adaptive layouts with Navigation Rail](https://m3.material.io/components/navigation-rail/overview)

---

## 📁 Project Quick-Link
*   **The Config**: `AppConfig.kt` -> Start here!
*   **The Brain**: `AppViewModel.kt`.
*   **The Layout**: `MainActivity.kt` -> See how it handles rotation.
*   **The Screens**: `GraphScreen.kt`, `HomeScreen.kt`, `SpeedometerScreen.kt`.
