# 🚀 SimpleFitness & Firebase Cloud Architecture (V1.0)

Willkommen zur erweiterten Lerneinheit **Cloud Firestore & Offline-First Sync**! 

Dieses Projekt dient als **Master-Template** und zeigt die vollständige Verbindung von lokaler Persistenz (**Room Database / SQL**), reaktivem State Management (**Jetpack Compose & StateFlow**), Cloud-Authentifizierung (**Firebase Auth**) und Cloud-Datenbank (**Cloud Firestore / NoSQL**).

---

## 📚 1. Grundlagen: Was ist Cloud Firestore & wie unterscheidet es sich von SQL (Room)?

### Was ist Cloud Firestore?
Cloud Firestore ist eine flexible, skalierbare **NoSQL-Dokumentendatenbank** in der Cloud von Google Firebase. Im Gegensatz zu relationalen Datenbanken speichert Firestore keine Daten in starren Tabellen mit Zeilen und Spalten.

### Die wichtigsten Unterschiede zu SQL (Room):
| Merkmal | Relationale DB (Room / SQLite) | NoSQL DB (Cloud Firestore) |
| :--- | :--- | :--- |
| **Datenstruktur** | Tabellen, Zeilen, Spalten, Foreign Keys | Collections (Sammlungen) & Documents (Dokumente) |
| **Schema** | **Starr** (Änderungen erfordern DB-Migrationen) | **Dynamisch / Schemalos** (Dokumente in derselben Collection können unterschiedliche Felder haben) |
| **Einsatzort** | Lokal auf dem Smartphone (Offline-First) | In der Cloud (Multi-Device, Serverless, Echtzeit) |
| **Abfragen** | Komplexe Joins (`JOIN`), Aggregationen | Flache Abfragen, Dokumenten-ID-basiert, Subcollections |

---

## 🗂️ 2. Die Datenstruktur in Firestore und „Warum sie so ist“

Um Daten sauber nach Nutzern und Trainingseinheiten zu trennen, verwenden wir eine **hierarchische Dokumenten- und Subcollection-Struktur**:

```text
users/
  └── {uid}/ (Dokument für jeden registrierten Nutzer, basierend auf der Auth-UID)
      └── sessions/ (Collection aller Trainingseinheiten dieses Nutzers)
          └── {sessionId}/ (Ein Dokument pro Session, z.B. "Session 27.10.2026...")
              └── readings/ (Subcollection aller Messwerte dieser Session)
                  └── {timestamp} (Einzelnes Messwert-Dokument: Puls, Fitness-Score, Timestamp)
```

### Warum diese Struktur?
1. **Datenschutz & Security Rules**: Durch die Wurzel-Kollektion `users/{uid}/...` greift die Firestore-Sicherheitsregel (`request.auth.uid == userId`) perfekt. Niemand kann jemals die Daten eines anderen Nutzers lesen oder schreiben.
2. **Vermeidung des 1-MB-Limits**: Firestore-Dokumente haben ein hartes Limit von 1 MB pro Dokument. Hätten wir alle Messwerte einer stundenlangen Session als Array in ein einziges Dokument geschrieben, würde dieses Limit schnell gesprengt werden. Durch die Unterteilung in `readings/{timestamp}` bleibt jedes Dokument federleicht.
3. **Effizientes Laden**: Die App kann zuerst nur die Liste der Sessions (`sessions`) abrufen und erst beim Öffnen einer Session die dazugehörigen `readings` laden oder filtern.

---

## 🔄 3. Datenfluss & Use-Case-Szenarien (Offline-First & Sync)

Die App implementiert ein robustes **Offline-First-Synchronisationsmodell**, das zwei Richtungen unterscheidet:

### Szenario A: Push-Sync (Lokal ➡️ Cloud)
- **Ablauf**: Der HTTP-Poller (oder später der ESP32-Sensor) erzeugt Messwerte. Die App speichert diese sofort lokal in der **Room DB** (mit `isSynced = false`).
- **Cloud-Backup**: Ist der Nutzer eingeloggt, werden die Daten im Hintergrund in **Cloud Firestore** geschrieben. Bei Erfolg wird das lokale Flag auf `isSynced = true` gesetzt.
- **Offline-Puffer**: Hat das Smartphone kein Internet, bleiben die Daten sicher in Room. Sobald wieder Netz da ist oder die App neu startet, lädt der `syncOfflineDataToCloud()`-Job die ungesyncten Daten im Batch-Verfahren nach Firestore hoch.

### Szenario B: Pull-Sync / Diff-Sync (Cloud ➡️ Lokal)
- **Ablauf**: Ist der Nutzer eingeloggt, kann er explizit den **„Sync with Cloud“**-Button auf dem Home Screen betätigen.
- **Intelligenter Abgleich**: Die App prüft welche Sessions bereits lokal in Room vorliegen (`localRepository.getAllSessionsList`), vergleicht sie mit Firestore und lädt **nur** die fehlenden Sessions herunter (`fetchMissingCloudReadings`).
- **Use Case**: *Multi-Device-Support*. Trainiert der Nutzer auf einem Zweitgerät, holt er sich per Knopfdruck seine historischen Daten auf das Hauptgerät, ohne lokale Offline-Sessions zu überschreiben.

---

## 🛠️ Aufgaben & Übungen für euer Verständnis

### Aufgabe 1: Testen des Cloud-Syncs (Pull-Sync)
1. **Ziel**: Verifiziere, dass der explizite Cloud-Sync fehlerfrei funktioniert.
2. **Schritte**: 
   - Zeichne eine Live-Session auf (während du eingeloggt bist).
   - Klicke auf den **„Clear History“**-Button (Ergebnis: Die lokale Room-DB ist leer, der History-Screen zeigt keine Daten mehr).
   - Klicke auf den **„Sync with Cloud“**-Button auf dem Home Screen.
   - Prüfe den History-Screen: Sind alle Daten originalgetreu aus der Cloud zurückgekehrt?

### Aufgabe 2: Erweiterte Lösch-Optionen (Dialog & Cloud-Löschung)
1. **Ziel**: Erweitere den bestehenden Lösch-Button um einen Auswahldialog mit feineren Lösch-Szenarien.
2. **Schritte**:
   - Wenn der Nutzer auf den Mülleimer-Button drückt, öffne ein AlertDialog (`AlertDialog`).
   - Biete dem Nutzer 4 klare Optionen an:
     - **a) Alles lokal löschen** (Room DB leeren)
     - **b) Alles lokal und in der Cloud löschen** (Room DB + Firestore `users/{uid}/sessions` komplett löschen)
     - **c) Aktuell angezeigte Session nur lokal löschen**
     - **d) Aktuell angezeigte Session lokal und in der Cloud löschen**
   - Nutze im `FirestoreRepository` Methoden wie `document.delete()` für einzelne Sessions oder deren Subcollections.

### Aufgabe 3: App-Config via Cloud (Spruch des Tages / Master Data & Security Rules)
1. **Ziel**: Lade globale Admin-Inhalte (z. B. Motivationssprüche) beim Start direkt aus Firestore.
2. **Schritte**:
   - Da die `quotes`-Kollektion auf der **Root-Ebene** (außerhalb von `users/{uid}`) liegt, musst du in der Firebase Console unter **Rules** den Lesezugriff für diese Kollektion erlauben (z. B. `match /quotes/{quoteId} { allow read: if request.auth != null; }`).
   - Lege in deiner Firebase Console eine Kollektion namens `quotes` an und erstelle dort Dokumente mit einem Feld `text` (z. B. *"Consistency is key!"*, *"Push your limits!"*, *"Rest is part of training"*).
   - Erweitere das Repository so, dass alle Sprüche aus `quotes` abgerufen und per `random()` ausgewählt werden.
   - Zeige diesen Spruch als Banner auf dem `HomeScreen` an.

### Aufgabe 4: Server-Filter (Direkter Cloud-Abruf ohne Room)
1. **Ziel**: Vergleiche den lokalen SQL-Ansatz mit direktem NoSQL-Cloud-Abruf.
2. **Schritte**:
   - Erstelle einen neuen Screen **`History2Screen.kt`** als exakte Kopie des `HistoryScreen.kt`.
   - Statt die Daten reaktiv über `localRepository.getFilteredDataBySession` aus Room zu laden, frage die Daten in `History2Screen` asynchron direkt über `FirestoreRepository` von den Firebase-Servern ab.
   - Diskutiert im Team: Wo liegen die Vor- und Nachteile von Room (lokaler Cache) gegenüber direktem Cloud-Abruf?

---

## 📚 Hilfreiche Ressourcen
*   **Firestore Data Model**: [Official Firestore Data Model Guide](https://firebase.google.com/docs/firestore/data-model)
*   **Firestore Queries**: [Perform Simple and Compound Queries in Firestore](https://firebase.google.com/docs/firestore/query-data/queries)
*   **Offline Data**: [Enable Offline Data in Firestore](https://firebase.google.com/docs/firestore/manage-data/enable-offline)
