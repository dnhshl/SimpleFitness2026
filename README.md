# 🏃 SimpleFitnessSQL Educational App

Willkommen zu **SimpleFitnessSQL**! 

Diese Lerneinheit baut auf der bekannten **SimpleFitness**-App auf und konzentriert sich rein auf **Lokale Persistenz (Room DB)** sowie **Sitzungs-Verwaltung (Session Management)**. 

Nachdem ihr in der Vorlesung die absoluten Grundlagen zur Room Database (SQL, Entities, DAOs) kennengelernt habt, erarbeitet ihr euch die konkrete Umsetzung in dieser App im **Reverse Engineering**.

---

## 🏗️ Architektur der App
Die App folgt strikt einer sauberen, modularen Architektur:
1. **Entity**: [`FitnessEntity.kt`](file:///C:/Users/Noll/Sciebo/MCAppsCompose/SimpleFitness2026V2/app/src/main/java/com/example/main/db/FitnessEntity.kt) definiert die Tabellenstruktur inkl. `sessionId`.
2. **DAO**: [`FitnessDao.kt`](file:///C:/Users/Noll/Sciebo/MCAppsCompose/SimpleFitness2026V2/app/src/main/java/com/example/main/db/FitnessDao.kt`) enthält die SQL-Abfragen (inkl. `DISTINCT` für Sessions und `WHERE`-Filterung).
3. **Repository**: [`LocalFitnessRepository.kt`](file:///C:/Users/Noll/Sciebo/MCAppsCompose/SimpleFitness2026V2/app/src/main/java/com/example/main/repositories/LocalFitnessRepository.kt`) kapselt die Datenbankoperationen.
4. **ViewModel**: [`AppViewModel.kt`](file:///C:/Users/Noll/Sciebo/MCAppsCompose/SimpleFitness2026V2/app/src/main/java/com/example/main/statemgmt/AppViewModel.kt`) orchestriert Netzwerk-Pollling, Session-Generierung und reaktive Flows.

---

## 🔍 Reverse Engineering Lab: Auf den Spuren der Daten
Geht die folgenden Stationen im Code durch und versucht nachzuvollziehen, wie die Konzepte implementiert wurden:

### 1. Die Digitale Erinnerung (Entity & DAO)
* **Dateien**: [`FitnessEntity.kt`](file:///C:/Users/Noll/Sciebo/MCAppsCompose/SimpleFitness2026V2/app/src/main/java/com/example/main/db/FitnessEntity.kt`) & [`FitnessDao.kt`](file:///C:/Users/Noll/Sciebo/MCAppsCompose/SimpleFitness2026V2/app/src/main/java/com/example/main/db/FitnessDao.kt`)
* **Aufgabe**: Schaut euch an, wie über `sessionId` Messungen von verschiedenen Trainingstagen getrennt werden. Warum verwenden wir in SQL `SELECT DISTINCT sessionId`?

### 2. Session Management & Polling
* **Datei**: [`AppViewModel.kt`](file:///C:/Users/Noll/Sciebo/MCAppsCompose/SimpleFitness2026V2/app/src/main/java/com/example/main/statemgmt/AppViewModel.kt`) -> `setPollingEnabled(...)`
* **Aufgabe**: Beobachtet, was passiert, wenn ihr den "Live Streaming"-Schalter auf dem Home-Screen einschaltet. Wie wird sichergestellt, dass jede Live-Sitzung eine neue `SessionID` erhält?

### 3. Reaktiver Datenfluss (Single Source of Truth)
* **Datei**: [`AppViewModel.kt`](file:///C:/Users/Noll/Sciebo/MCAppsCompose/SimpleFitness2026V2/app/src/main/java/com/example/main/statemgmt/AppViewModel.kt`) -> `fitnessHistoryLog`
* **Aufgabe**: Studiert die Kommentare zu `map`, `distinctUntilChanged` und `flatMapLatest`. Wie reagiert die App, wenn ihr im History-Screen die Session oder den Puls-Filter ändert?

---

## 🛠️ Aufgaben für Studierende

### Aufgabe 1: Session-Filterung nachvollziehen
1. Startet die App und lasst ein paar Live-Daten einlesen (wird als Session gespeichert).
2. Schaltet das Live-Streaming aus, wartet kurz und schaltet es wieder ein (dadurch startet eine neue Session).
3. Öffnet den **History-Screen** und den **Graph-Screen**: Überprüft das Session-Dropdown. Wie verhalten sich die Graphen und Tabellen beim Umschalten zwischen den Sessions?

### Aufgabe 2: Erweitertes Filtern (Fitness Score)
Aktuell wird nach Puls und Zeit gefiltert. Eure Aufgabe ist es, einen dritten Filter hinzuzufügen: **Fitness Score**.
1. **DAO**: Erweitert `getFilteredLogsBySession` in [`FitnessDao.kt`](file:///C:/Users/Noll/Sciebo/MCAppsCompose/SimpleFitness2026V2/app/src/main/java/com/example/main/db/FitnessDao.kt`) um eine Bedingung für den minimalen Fitness Score (`AND fitnessScore >= :minFitness`).
2. **ViewModel**: Passt `UiState` und `fitnessHistoryLog` in [`AppViewModel.kt`](file:///C:/Users/Noll/Sciebo/MCAppsCompose/SimpleFitness2026V2/app/src/main/java/com/example/main/statemgmt/AppViewModel.kt`) an.
3. **UI**: Fügt in [`HistoryScreen.kt`](file:///C:/Users/Noll/Sciebo/MCAppsCompose/SimpleFitness2026V2/app/src/main/java/com/example/main/ui/screens/HistoryScreen.kt`) einen weiteren Slider für den Fitness Score hinzu.

### Aufgabe 3: Statistik-Berechnung
1. Berechnet im `HistoryScreen` den **Durchschnittspuls** aller angezeigten Messungen der aktiven Session und zeigt diesen als Kennzahl an.
2. Fügt einen Button hinzu, um die Historie der aktuellen Session zu löschen.

---

## 📚 Hilfreiche Ressourcen
*   **Room Database**: [Offizieller Android Guide](https://developer.android.com/training/data-storage/room)
*   **Kotlin Flows**: [Asynchronous Flow Guide](https://kotlinlang.org/docs/flow.html)
