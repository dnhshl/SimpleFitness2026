# 🚀 SimpleFitness & IoT BLE Integration (V2.0)

Willkommen zur Lerneinheit **Bluetooth Low Energy (BLE) & ESP32-Sensorintegration**!

Da ihr BLE bereits aus dem letzten Semester kennt, konzentrieren wir uns in dieser Lerneinheit voll und ganz auf die **Software-Architektur und das Zusammenspiel von Hardware-Daten und Cloud-Diensten**.

---

## 🎯 Die Kern-Lernziele dieser Lerneinheit

### a) Das Adapter-Pattern (Mapping von Hardware auf Domain-Modelle)
Sensoren (wie das Poti am ESP32) liefern rohe ADC-Werte (z. B. `0..4095`). Das `BleRepository` fungiert als **Adapter**: Es empfängt die rohen JSON-Bytes, parst sie und normalisiert die Hardware-Rohsignale in unser einheitliches Domain-Modell (`FitnessData` mit Herzfrequenz in BPM und Fitness-Score).

### b) Polymorphe Datenquellen (Umschalten im laufenden Betrieb)
Über den Data Source Selector auf dem Home Screen kann nahtlos zwischen dem Internet-Livestream (`DataSource.HTTP`) und dem ESP32-Sensor (`DataSource.BLE`) umgeschaltet werden. Das ViewModel steuert dabei bidirektional auch die Hardware (z. B. das Blinken der ESP32-LED bei aktivem Live-Stream).

### c) Architektonische Konsistenz (Single Source of Truth)
Dank unserer sauberen App-Architektur (Unified Data Ingestion in `handleNewFitnessData`) ist es für Room DB, Cloud Firestore, Tacho, Vico-Graphen und den History-Screen **völlig egal**, woher die Daten kommen. Die nachgelagerte Logik arbeitet für HTTP und BLE absolut identisch!

---

## 🛠️ Aufgaben & Übungen

### Aufgabe 1: End-to-End Test & Daten-Mapping prüfen
1. **Ziel**: Verifiziere das Zusammenspiel von BLE-Sensor und Cloud-Architektur.
2. **Schritte**:
   - Verbinde dich im ESP32 BLE Tab mit dem ESP32 (`ESP32-LAB-PROJECT` oder *Connect Last*).
   - Wähle auf dem Home Screen **ESP32 BLE** als Datenquelle und aktiviere `Live Streaming: ON`.
   - Drehe am Joystick / Poti: Beobachte, wie die LED am ESP32 blinkt und die Werte live in den Tacho, den Graphen, die Room-DB und (bei Login) nach Firestore fließen.

### Aufgabe 2: ESP32 On-Demand Streaming (Command & Response)
1. **Ziel**: Optimiere den ESP32-Code, sodass er **nicht** mehr dauerhaft beim Verbindungsaufbau sendet, sondern nur noch dann, wenn die App aktiv einen Live-Stream anfordert.
2. **Schritte**:
   - Aktuell sendet der ESP32 in der `loop()` ununterbrochen Daten, sobald `deviceConnected == true`.
   - Passe den Arduino-Code so an, dass der ESP32 nur dann Messwerte in den Notification-Stream schreibt, wenn er von der App ein Start-Kommando (z. B. über das bestehende JSON-Protokoll mit `LED = "H"`) erhalten hat.
   - Teste, ob der Stream beim Ausschalten von `Live Streaming: OFF` auf dem ESP32 sofort stoppt.

### Aufgabe 3: Sensor-Bereich im `BleRepository` anpassen
1. **Ziel**: Verstehe die Poti-Transformation im Code.
2. **Schritte**:
   - Schau dir an, wie im `BleRepository` der rohe Poti-Wert (`0..4095`) auf den Puls (`60..180` BPM) gemappt wird.
   - Passe die Formel so an, dass der Pulsbereich bei maximalem Poti-Ausschlag bis zu `200` BPM anzeigt und ein Fitness-Score von bis zu `150%` erreicht werden kann.

---

## 📚 Hilfreiche Ressourcen
*   **Kable BLE Library**: [Kable Documentation by Juul Labs](https://github.com/JuulLabs/Kable)
*   **ESP32 BLE Arduino**: [Arduino ESP32 BLE Guide](https://github.com/nkolban/esp32-snippets/tree/master/cpp_utils/BLE)
