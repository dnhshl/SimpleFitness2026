# 🖐️ SimpleFitness On-Device AI: Hand-Goniometrie & Reha-Trainer

Willkommen zur On-Device AI Lerneinheit für **Gesundheits- und Sportingenieurwesen**!

Diese App demonstriert die Integration von lokaler künstlicher Intelligenz (**Google MediaPipe HandLandmarker**) mit **biomechanischer Signalverarbeitung (Goniometrie)** in einer modernen Jetpack Compose Android-App. Die Tests können bequem direkt **am Schreibtisch ("am Platz")** mit der Frontkamera durchgeführt werden.

---

## 🏗️ Architektur & Signalverarbeitung

Die App baut auf einer sauberen 3-Schichten-Architektur auf:

1. **Kamera-Stream (`CameraXPreview.kt`)**: Kamera-Frames der Frontkamera werden in Echtzeit an die KI-Schicht übergeben.
2. **On-Device KI Repository (`GestureRepository.kt`)**: Nutzt das vortrainierte `hand_landmarker.task` Modell zur Erkennung von **21 Hand-Gelenkpunkten (Landmarks)** in 3D/2D.
3. **Biomechanische Signalverarbeitung (`HandMath.kt`)**: Berechnet den Gelenkwinkel (Goniometrie) am Zeigefinger-Mittelgelenk (**PIP Joint**: Landmarks 5 `MCP` $\rightarrow$ 6 `PIP` $\rightarrow$ 7 `DIP`) mittels Vektorgeometrie:
   $$\theta = \arccos\left(\frac{\vec{v}_1 \cdot \vec{v}_2}{\|\vec{v}_1\| \|\vec{v}_2\|}\right) \times \frac{180}{\pi}$$
4. **State Machine & Zähler (`AppViewModel.kt`)**: Zählt vollständige Beuge- und Streckzyklen in der Hand-Rehabilitation:
   - Beugung erkannt: Winkeleinbruch $< 105^\circ \rightarrow$ `isFingerFlexed = true`
   - Vollständige Streckung: Wiederanstieg $> 155^\circ \rightarrow$ `rehabRepCount + 1`
5. **Echtzeit-Visualisierung (`CubeScreen.kt`)**: 
   - `HandGoniometerOverlay`: Zeichnet das 21-Gelenke-Skelett und hebt das Zeigefinger-Mittelgelenk farblich hervor (Grün = gestreckt, Orange = gebeugt).
   - `GoniometerHudCard`: Schwebende Statuskarte mit Reha-Zähler und Live-Winkel in Grad.

---

## 🔍 Reverse Engineering Lab: Auf den Spuren der Signale

### 1. Das Handskelett & MediaPipe Landmarks
* **Dateien**: [`GestureRepository.kt`](file:///C:/Users/Noll/Sciebo/MCAppsCompose/SimpleFitness2026V2/app/src/main/java/com/example/main/repositories/GestureRepository.kt) & [`CubeScreen.kt`](file:///C:/Users/Noll/Sciebo/MCAppsCompose/SimpleFitness2026V2/app/src/main/java/com/example/main/ui/screens/CubeScreen.kt)
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
1. Startet den Tab **Hand-AI** (`CubeScreen`).
2. Halture eure Hand vor die Frontkamera. Beugt den Zeigefinger und beobachtet die Live-Gradzahl im HUD sowie die Farbänderung am Mittelgelenk.
3. Absolviert 5 vollständige Beuge- und Streckzyklen und prüft, ob der Reha-Zähler präzise hochzählt.

### Aufgabe 2: Zweiten Gelenkwinkel hinzufügen (Mittelfinger PIP)
Aktuell wird der Winkel des Zeigefingers gemessen. Eure Aufgabe ist es, einen zweiten Winkel für das **Mittelfinger-Mittelgelenk (PIP)** einzubauen:
1. **Landmarks**: Nutzt die Mittelfinger-Landmarks 9 (`MCP`), 10 (`PIP`) und 11 (`DIP`).
2. **Mathematik**: Ergänzt eine Funktion `calculateMiddlePipAngle` in [`HandMath.kt`](file:///C:/Users/Noll/Sciebo/MCAppsCompose/SimpleFitness2026V2/app/src/main/java/com/example/main/ai/HandMath.kt).
3. **UI-Anzeige**: Zeigt den Mittelfinger-Winkel zusätzlich im HUD an.

### Aufgabe 3: Ergonomie & Haltungs-Feedback (Form Feedback)
1. Erweitert den Status-Text im HUD: Wenn der Finger beim Strecken nicht mindestens $160^\circ$ erreicht, soll eine Warnung *"Finger weiter strecken!"* eingeblendet werden, um eine unvollständige Bewegungsausführung zu verhindern.

---

## 📚 Hilfreiche Ressourcen
* **Google MediaPipe Hand Landmarker**: [Developer Guide](https://developers.google.com/mediapipe/solutions/vision/hand_landmarker)
* **CameraX Android Guide**: [Official Documentation](https://developer.android.com/use-cases/camera/camerax)
