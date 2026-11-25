# 🖐️ SimpleFitness On-Device AI: Hand-Goniometrie & Reha-Trainer

Willkommen zur On-Device AI Lerneinheit für **Gesundheits- und Sportingenieurwesen**!

Diese App demonstriert die Integration von lokaler künstlicher Intelligenz (**Google MediaPipe HandLandmarker**) mit **biomechanischer Signalverarbeitung (Goniometrie)** in einer modernen Jetpack Compose Android-App. Die Tests können bequem direkt **am Schreibtisch ("am Platz")** mit der Frontkamera durchgeführt werden.

---

## 💡 Was ist Google MediaPipe & welche Möglichkeiten bietet es?

**Google MediaPipe** ist ein extrem performantes Open-Source-Framework für On-Device Machine Learning (KI auf dem Endgerät). Im Gegensatz zu Cloud-basierten KI-Diensten führt MediaPipe alle Modelle **zu 100 % lokal** auf der GPU/NPU/CPU des Smartphones aus:

* **Vorteile**: Zero Latency (Echtzeit-Verarbeitung mit 30–60 FPS), 100 % Datenschutz (keine Videodaten verlassen das Gerät), voll funktionsfähig ohne Internetverbindung.
* **Möglichkeiten (MediaPipe Vision Tasks)**:
  - **Hand Landmarker**: Erkennung von 21 3D-Handgelenkpunkten (unsere Lösung).
  - **Pose Landmarker**: Erkennung von 33 3D-Körpergelenkpunkten (z. B. für Kniebeugen, Laufanalysen).
  - **Face Landmarker**: 468 Punkte zur Gesichts- & Mimik-Erkennung.
  - **Object Detection & Interactive Segmentation**: Objekte im Kamerabild erkennen und freistellen.
  - **Gesture Recognition**: Vordefinierte Gesten (Faust, Daumen hoch etc.) direkt klassifizieren.

---

## 🛠️ Wie wird MediaPipe eingebunden & wie wechselt man Modelle?

### 1. Einbindung im Projekt
1. **Gradle-Abhängigkeit (`app/build.gradle.kts`)**:
   ```kotlin
   implementation("com.google.mediapipe:tasks-vision:0.10.20")
   ```
2. **Modell-Datei im Assets-Ordner (`app/src/main/assets/`)**:
   Die Modell-Datei `hand_landmarker.task` wird direkt aus dem Google AI Edge Repository heruntergeladen und in den `assets/`-Ordner gelegt.
3. **Komprimierung verhindern (`app/build.gradle.kts`)**:
   Damit Android Studio die TensorFlow/MediaPipe-Modelldateien beim Erstellen der APK nicht beschädigt oder komprimiert, muss folgendes konfiguriert sein:
   ```kotlin
   androidResources {
       noCompress += "task"
   }
   ```

---

### 🔄 Praxis-Guide: Wie würde man auf das **Pose Landmarker Modell** umstellen?

Wenn ihr statt der Hand-Erkennung die Ganzkörper-Posen-Erkennung (z. B. für Kniebeugen oder Gang-Analysen) nutzen wolltet, sind folgende Anpassungen erforderlich:

1. **Modell-Asset austauschen**:
   - Die Datei `pose_landmarker_lite.task` (oder `pose_landmarker_full.task`) herunterladen und in `app/src/main/assets/` ablegen.
2. **Repository-Code anpassen (`GestureRepository.kt` / `AiFitnessRepository.kt`)**:
   - **Modellpfad ändern**: `.setModelAssetPath("pose_landmarker_lite.task")`
   - **Landmarker-Klasse tauschen**: Statt `HandLandmarker` wird `PoseLandmarker.createFromOptions(context, options)` verwendet.
   - **Optionen-Builder**: `PoseLandmarker.PoseLandmarkerOptions.builder()` nutzen.
3. **Resultat-Callback anpassen**:
   - Der Listener empfängt ein `PoseLandmarkerResult` anstelle von `HandLandmarkerResult`.
4. **Landmark-Mapping ändern**:
   - Pose liefert **33 Ganzkörper-Landmarks** (z. B. Schulter 11/12, Ellbogen 13/14, Knie 25/26, Knöchel 27/28) statt 21 Hand-Landmarks.

---

## 🏗️ Architektur & Signalverarbeitung in dieser App

Die App baut auf einer sauberen 3-Schichten-Architektur auf:

1. **Kamera-Stream (`CameraXPreview`)**: Kamera-Frames der Frontkamera werden in Echtzeit an die KI-Schicht übergeben.
2. **On-Device KI Repository (`GestureRepository.kt`)**: Nutzt das `hand_landmarker.task` Modell zur Erkennung von **21 Hand-Gelenkpunkten (Landmarks)**.
3. **Biomechanische Signalverarbeitung (`HandMath.kt`)**: Berechnet den Gelenkwinkel (Goniometrie) am Zeigefinger-Mittelgelenk (**PIP Joint**: Landmarks 5 `MCP` $\rightarrow$ 6 `PIP` $\rightarrow$ 7 `DIP`) mittels Vektorgeometrie:
   $$\theta = \arccos\left(\frac{\vec{v}_1 \cdot \vec{v}_2}{\|\vec{v}_1\| \|\vec{v}_2\|}\right) \times \frac{180}{\pi}$$
4. **State Machine & Zähler (`AppViewModel.kt`)**: Zählt vollständige Beuge- und Streckzyklen in der Hand-Rehabilitation:
   - Beugung erkannt: Winkeleinbruch $< 105^\circ \rightarrow$ `isFingerFlexed = true`
   - Vollständige Streckung: Wiederanstieg $> 155^\circ \rightarrow$ `rehabRepCount + 1`
5. **Echtzeit-Visualisierung (`HandGoniometerScreen.kt`)**: 
   - `HandGoniometerOverlay`: Zeichnet das 21-Gelenke-Skelett und hebt das Zeigefinger-Mittelgelenk farblich hervor (Grün = gestreckt, Orange = gebeugt).
   - `GoniometerHudCard`: Schwebende Statuskarte mit Reha-Zähler und Live-Winkel in Grad.

---

## 🔍 Reverse Engineering Lab: Auf den Spuren der Signale

### 1. Das Handskelett & MediaPipe Landmarks
* **Dateien**: [`GestureRepository.kt`](file:///C:/Users/Noll/Sciebo/MCAppsCompose/SimpleFitness2026V2/app/src/main/java/com/example/main/repositories/GestureRepository.kt) & [`HandGoniometerScreen.kt`](file:///C:/Users/Noll/Sciebo/MCAppsCompose/SimpleFitness2026V2/app/src/main/java/com/example/main/ui/screens/HandGoniometerScreen.kt)
* **Aufgabe**: Untersucht, wie die 21 Hand-Landmarks von MediaPipe als normierte `Offset(x, y)` Koordinaten an das `AppViewModel` übergeben und im `HandGoniometerOverlay` auf Bildschirmpixel skaliert werden.

### 2. Biomechanische Vektorgeometrie
* **Datei**: [`HandMath.kt`](file:///C:/Users/Noll/Sciebo/MCAppsCompose/SimpleFitness2026V2/app/src/main/java/com/example/main/ai/HandMath.kt)
* **Aufgabe**: Vollzieht nach, wie die beiden Vektoren $\vec{v}_1 = P_5 - P_6$ und $\vec{v}_2 = P_7 - P_6$ aus den Landmark-Koordinaten gebildet werden und wie das Skalarprodukt den Innenwinkel $\theta$ berechnet.

### 3. Die Reha-State-Machine
* **Datei**: [`AppViewModel.kt`](file:///C:/Users/Noll/Sciebo/MCAppsCompose/SimpleFitness2026V2/app/src/main/java/com/example/main/statemgmt/AppViewModel.kt) (`processGestureData`)
* **Aufgabe**: Nachvollziehen, warum Hysterese-Schwellenwerte ($105^\circ$ für Beugung, $155^\circ$ für Streckung) gewählt werden, um Prellen und Fehlzählungen bei kleinen Hand-Zuckungen zu vermeiden.

---

## 🛠️ Aufgaben für Studierende

### Aufgabe 1: Goniometrie-Messung analysieren & testen
1. Startet den Tab **Hand-AI** (`HandGoniometerScreen`).
2. Haltet eure Hand vor die Frontkamera. Beugt den Zeigefinger und beobachtet die Live-Gradzahl im HUD sowie die Farbänderung am Mittelgelenk.
3. Absolviert 5 vollständige Beuge- und Streckzyklen und prüft, ob der Reha-Zähler präzise hochzählt.

### Aufgabe 2: Zweiten Gelenkwinkel hinzufügen (Mittelfinger PIP)
Aktuell wird der Winkel des Zeigefingers gemessen. Eure Aufgabe ist es, einen zweiten Winkel für das **Mittelfinger-Mittelgelenk (PIP)** einzubauen:
1. **Landmarks**: Nutzt die Mittelfinger-Landmarks 9 (`MCP`), 10 (`PIP`) und 11 (`DIP`).
2. **Mathematik**: Ergänzt eine Funktion `calculateMiddlePipAngle` in [`HandMath.kt`](file:///C:/Users/Noll/Sciebo/MCAppsCompose/SimpleFitness2026V2/app/src/main/java/com/example/main/ai/HandMath.kt).
3. **UI-Anzeige**: Zeigt den Mittelfinger-Winkel zusätzlich im HUD an.

### Aufgabe 3: Ergonomie & Haltungs-Feedback (Form Feedback)
1. Erweitert den Status-Text im HUD: Wenn der Finger beim Strecken nicht mindestens $160^\circ$ erreicht, soll eine Warnung *"Finger weiter strecken!"* eingeblendet werden, um eine unvollständige Bewegungsausführung zu verhindern.

### Aufgabe 4: Finger-Spreizungs-Messung (Abduktion & Adduktion)
Im gestreckten Zustand der Hand sollen Zeigefinger und Mittelfinger zusammengeführt und gespreizt werden:
1. **Landmarks**: Nutzt die Fingerspitzen 8 (`INDEX_FINGER_TIP`) und 12 (`MIDDLE_FINGER_TIP`).
2. **Signalverarbeitung**: Berechnet die euklidische Distanz zwischen den beiden Fingerspitzen in `HandMath.kt`.
3. **Klassifikation**: Unterscheidet zwischen *"Finger zusammengeführt"* (Adduktion, z. B. Distanz $< 0.06$) und *"Finger gespreizt"* (Abduktion, z. B. Distanz $> 0.15$).
4. **UI-Feedback**: Blendet den Spreiz-Status live im Overlay oder HUD aus.

---

## 📚 Hilfreiche Ressourcen
* **Google MediaPipe Hand Landmarker**: [Developer Guide](https://developers.google.com/mediapipe/solutions/vision/hand_landmarker)
* **Google MediaPipe Pose Landmarker**: [Developer Guide](https://developers.google.com/mediapipe/solutions/vision/pose_landmarker)
* **CameraX Android Guide**: [Official Documentation](https://developer.android.com/use-cases/camera/camerax)
