# 🚀 SimpleFitness & Firebase Auth Template (Zwischenstand)

Willkommen zur Lerneinheit **Firebase Authentication & State Management**! 

Dieses Projekt dient als **Master-Template** und wichtiger **Zwischenstand** für eure Projektarbeiten. Wir haben lokale Persistenz (**Room Database**) erfolgreich mit Cloud-Authentifizierung (**Firebase Auth**) verknüpft. In kommenden Lerneinheiten werden wir diesen Stand Schritt für Schritt um weitere Cloud-Dienste (wie z. B. **Cloud Firestore** für die Cloud-Synchronisation) erweitern.

Nachdem ihr in der Vorlesung die absoluten Grundlagen kennengelernt habt, erarbeitet ihr euch die neuen Konzepte in dieser App im **Reverse Engineering**.

---

## 🎨 Neue UI-Konzepte: Verstehen & Anwenden

In dieser Lerneinheit führen wir zwei wichtige UI-Entwurfsmuster ein, die euch in fast jeder professionellen Android-App begegnen werden:

### 1. Was ist eine Snackbar und wofür nutzen wir sie?
* **Was ist das?** Eine `Snackbar` ist eine kompakte, schwebende Benachrichtigung (Toast-ähnlich, aber mächtiger), die kurzzeitig am unteren Bildschirmrand erscheint.
* **Wofür nutzen wir sie?** In unserer App nutzen wir sie als **zentralen Feedback-Kanal** für Erfolgsmeldungen (z. B. *„Password reset email sent successfully.“*) oder Fehlermeldungen (z. B. falsches Passwort beim Login).
* **Warum keine Inline-Fehlertexte?** Statt Formulare mit Fehlermeldungen zu überladen, hält die Snackbar das UI sauber und verschwindet nach kurzer Zeit von selbst (oder lässt sich wegwischen).
* **Wie funktioniert sie technisch bei uns?** 
  - Das ViewModel setzt eine Nachricht in den `UiState` (`snackbarMessage`).
  - Die `MainActivity` lauscht reaktiv per `LaunchedEffect(uiState.snackbarMessage)` darauf und zeigt sie über einen globalen `SnackbarHost` an.

### 2. Einbinden von Menüs in die Statusleiste (TopAppBar Actions & DropdownMenu)
* **Das Problem**: Auf den Inhalts-Screens (Home, Graph, History) wollen wir Platz sparen und den Nutzer nicht mit Login-Formularen überladen.
* **Die Lösung**: Die obere Statusleiste (`TopAppBar`) bietet sogenannte **Actions** auf der rechten Seite. 
* **Die Umsetzung**: Wir haben dort ein Account-Icon (`AccountCircle`) platziert. Ein Klick darauf öffnet ein reaktives **`DropdownMenu`** (ein Overlay-Menü).
* **Was bietet das Menü?** Es zeigt den aktuellen Status (*„User: mail@...“* oder *„Mode: Guest“*), färbt sich bei erfolgreichem Login **grün** ein und bietet kontextabhängige Aktionen wie *„Edit Nickname“*, *„Logout“* oder *„Login / Register“*.

---

## 🏗️ Architektur & Logische Konzepte

### 1. Das Repository-Pattern für Firebase (`FirebaseAuthRepository.kt`)
Das Firebase SDK wird sauber in ein eigenes Repository gekapselt. Das Repository übernimmt alle direkten Aufrufe von Firebase Auth und liefert Kotlin `Result`-Objekte zurück.

### 2. Provider-Agnostischer Auth-Zustand im `UiState`
Statt den Login-Status fest an eine spezifische Anmeldemethode (wie E-Mail) zu binden, hält der zentrale `UiState` allgemeine Flags vor:
```kotlin
val isAuthenticated: Boolean = false,
val currentUserId: String? = null,
val currentUserEmail: String? = null,
val currentUserDisplayName: String? = null
```
Jeder Screen kann diesen Zustand über `viewModel.uiState.collectAsState()` beobachten.

### 3. Asynchrone Rückgaben mit Lambda-Callbacks (`onResult`) & Zusammenspiel mit der Snackbar
Da Authentifizierungs-Operationen in Firebase asynchron ablaufen, nutzen wir **Lambdas als Funktionsparameter** (Callbacks), um Erfolge oder Fehlermeldungen an den Aufrufer (z. B. das UI) zurückzumelden:
```kotlin
fun sendPasswordReset(email: String, onResult: (Boolean, String?) -> Unit)
```

#### Wie wird das im UI (`LoginScreen.kt`) aufgerufen?
Beim Klick auf *„Forgot Password? Reset“* übergibt das UI einen Lambda-Block als Argument für `onResult`:
```kotlin
viewModel.sendPasswordReset(email) { _, msg ->
    // Dieser Codeblock wird erst ausgeführt, wenn Firebase das Ergebnis liefert!
    viewModel.showSnackbar(msg ?: "")
}
```

#### Das Zusammenspiel mit der Snackbar:
1. Das ViewModel führt den asynchronen Netzwerkaufruf aus.
2. Sobald Firebase antwortet, ruft das ViewModel den Callback `onResult(true, "Password reset email sent successfully.")` auf.
3. Im UI fängt das Lambda diesen String (`msg`) ab und ruft `viewModel.showSnackbar(msg)` auf.
4. `showSnackbar` speichert den Text im `UiState` (`snackbarMessage`).
5. Die `MainActivity` bemerkt dies über `LaunchedEffect(uiState.snackbarMessage)` und blendet die Nachricht elegant als **Snackbar** am unteren Bildschirmrand ein.

---

## 🔍 Leitfaden zum Reverse Engineering

Um dieses Template für eure eigenen Projekte zu verstehen und anzupassen, solltet ihr den Weg eines Events von der UI bis zum Backend nachvollziehen:

1. **Der Startpunkt (UI)**: Schaut in `LoginScreen.kt`. Wenn ein Nutzer auf *Sign In* klickt, wird `viewModel.signIn(email, password)` aufgerufen.
2. **Die Geschäftslogik (ViewModel)**: Im `AppViewModel.kt` startet eine Coroutine, die `authRepository.signIn(...)` aufruft.
3. **Die Status-Aktualisierung**: Nach erfolgreichem Login aktualisiert `updateAuthState()` den `uiState`. 
4. **Die Reaktivität (UI-Update)**: Die `MainActivity` bemerkt den neuen Zustand, färbt das Account-Icon in der TopBar grün und zeigt den Nickname an.

---

## 🛠️ Aufgaben & Übungen für euer Verständnis

### Aufgabe 1: Anonyme Anmeldung (Anonymous Auth) hinzufügen
1. Aktiviert in der Firebase Console unter **Authentication -> Sign-in method** den Anbieter **Anonym**.
2. Fügt im `FirebaseAuthRepository` eine Methode `signInAnonymously()` hinzu.
3. Ergänzt im Account-Menü in der `MainActivity` einen Button „Als Gast fortfahren“, der diese Methode aufruft.

### Aufgabe 2: Passwort-Sichtbarkeit umschalten
Auf dem `LoginScreen` ist das Passwort derzeit fest maskiert (`PasswordVisualTransformation`). 
- Fügt ein Icon (Auge-Symbol) an das Passwort-Textfeld an, mit dem der Nutzer sein eingegebenes Passwort ein- und ausblenden kann (Tipp: Ein booleaner `var passwordVisible by remember { mutableStateOf(false) }`-State im Composable).

### Aufgabe 3: Profil-Screen erstellen
1. Erstellt einen neuen Screen `ProfileScreen.kt`.
2. Zeigt dort die `currentUserId`, die E-Mail und den aktuellen Nickname an.
3. Ermöglicht es dem Nutzer dort direkt, seinen Nickname zu ändern (nutzt dafür die bereits vorhandene `viewModel.setNickname(...)`-Funktion).

---

## 📚 Hilfreiche Ressourcen
*   **Material Design Snackbars**: [Android Compose Snackbar Guide](https://developer.android.com/jetpack/compose/components/snackbar)
*   **Material Design Menus**: [Android Compose Menus](https://developer.android.com/jetpack/compose/menu)
*   **Firebase Auth Guide**: [Android Firebase Authentication](https://firebase.google.com/docs/auth/android/start)
