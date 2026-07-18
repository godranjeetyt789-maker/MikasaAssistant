# Mikasa — Futuristic AI Assistant for Android

A fully local, privacy-first Jarvis-style voice assistant powered by **xAI Grok**.  
Built with Kotlin + Jetpack Compose, MVVM + Clean Architecture.

---

## What it does

| Feature | How |
|---|---|
| Human-like Hinglish/Hindi/English conversation | Grok via xAI API |
| Persistent conversation memory | Room (SQLite, encrypted path) |
| Voice → text | Android on-device SpeechRecognizer (no cloud) |
| Text → voice | Android on-device TTS with mood-based pitch/rate |
| Open any app | `getLaunchIntentForPackage` (standard Intent) |
| Web search | Opens browser with Google query |
| Phone calls | `ACTION_CALL` / `ACTION_DIAL` |
| Send SMS | `SmsManager` (confirmed before sending) |
| Read SMS | `content://sms/inbox` (needs READ_SMS permission) |
| Set alarms | `AlarmClock.ACTION_SET_ALARM` (no Clock permission needed) |
| Calendar events | `CalendarContract.Events.CONTENT_URI` (user confirms in Calendar app) |
| Play music/video | Deep-links to Spotify / YouTube |
| Read notifications | NotificationListenerService (read-only, on-demand) |
| OCR from camera | ML Kit on-device TextRecognition |
| Screenshot + OCR | MediaProjection (Android shows its own consent dialog every call) |
| Floating bubble | `TYPE_APPLICATION_OVERLAY` (user grants in Settings) |
| Encrypted API key | EncryptedSharedPreferences (AES-256, Android Keystore) |
| Plugin system | Implement `AssistantTool` and register in `AppContainer` |

---

## Quick start (build)

### Prerequisites
- Android Studio Ladybug (2024.2+) or newer
- JDK 17+
- Android device / emulator running **Android 11+** (API 30+)

### Steps
```bash
git clone <this-repo>   # or open the folder directly in Android Studio
```
1. Open the project root (`MikasaAssistant/`) in Android Studio.
2. Let Gradle sync (first sync downloads dependencies — give it a minute).
3. **Run** on a device or emulator.

> **Tip:** If you see a KSP / Room annotation error, ensure you're on the
> AGP version listed in `build.gradle.kts`. Android Studio will offer to
> upgrade — accept it.

---

## First-run setup

1. Open the app → tap **Settings** (bottom tab).
2. Paste your **xAI API key** (the one from console.x.ai).  
   It's stored encrypted with AES-256 — never leaves your device.
3. (Optional) Change the assistant's name — it changes the system prompt too.
4. Grant permissions as prompted:
   - **Microphone** — needed to listen (Android asks automatically on first voice tap).
   - **Phone / SMS / Contacts** — asked on first call/SMS command.
   - **Notification access** — tap "Grant" in Settings → goes to the system page.
   - **Overlay** — tap "Grant" for the floating bubble.
5. Tap the **mic orb** and say something: *"Hey, mujhe kal subah 7 baje alarm set karo."*

---

## Wake word ("Hey Mikasa" hands-free)

The background `AssistantForegroundService` is scaffolded for
[Picovoice Porcupine](https://picovoice.ai/products/porcupine/), which runs
on-device with ~10 MB RAM and minimal battery impact.

**Wiring it in:**

1. Sign up at https://console.picovoice.ai — free tier is enough.
2. Create a custom wake word "Hey Mikasa" (or "Jarvis") and download the `.ppn` file.
3. Copy the `.ppn` into `app/src/main/assets/`.
4. In `settings.gradle.kts`, uncomment the `maven.picovoice.ai` repository line.
5. In `app/build.gradle.kts`, uncomment the `porcupine-android` dependency line.
6. In `AssistantForegroundService.onCreate()`, add:
   ```kotlin
   val porcupineManager = PorcupineManager.Builder()
       .setAccessKey("YOUR_PICOVOICE_ACCESS_KEY")
       .setKeywordPath("hey_mikasa_android.ppn")
       .setSensitivity(0.7f)
       .build(applicationContext) { onWakeWordDetected() }
   porcupineManager.start()
   ```
7. In `onDestroy()`, add `porcupineManager.stop(); porcupineManager.delete()`.

Until then, the mic button in the main screen + the floating bubble work fine.

---

## Adding a new skill (plugin)

```kotlin
class SearchFlightsSkill(private val context: Context) : AssistantTool {
    override val name = "search_flights"
    override val description = "Search for flights between two cities and open the results."
    override val parametersJson = """
        {"type":"object","properties":{
          "from":{"type":"string"},"to":{"type":"string"},"date":{"type":"string"}
        },"required":["from","to","date"]}
    """
    override suspend fun execute(arguments: JSONObject): ToolResult {
        val url = "https://www.google.com/travel/flights?q=..." // build from args
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).also {
            it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
        return ToolResult(true, "Opening flight search.")
    }
}
```
Then in `AppContainer.kt`:
```kotlin
register(SearchFlightsSkill(context))
```
Grok will automatically discover it on the next turn.

---

## Architecture

```
MikasaAssistant/
├── data/
│   ├── local/        ConversationDatabase (Room) · SecurePrefs (EncryptedSharedPrefs)
│   └── remote/       GrokApiService (OkHttp) · GrokModels
├── domain/
│   ├── ConversationEngine   brain loop: Grok ↔ tools ↔ memory
│   └── tools/        Tool.kt (interface + registry) · PhoneActionTools · AppAndMediaTools · …
├── voice/            SpeechToTextManager · TextToSpeechManager
├── vision/           CameraOcrAnalyzer · ScreenshotAnalyzer
├── service/          AssistantForegroundService · FloatingBubbleService · NotificationReaderService
├── presentation/
│   ├── viewmodel/    AssistantViewModel
│   └── ui/           AssistantScreen · SettingsScreen · Components · Theme
└── di/               AppContainer
```

---

## Privacy notes

- The API key is **never** in source code — only in EncryptedSharedPreferences on your device.
- **Nothing is sent to Anthropic** (this app talks directly to xAI's servers only).
- Conversation history lives in a local SQLite database — you can clear it via "Clear chat" (add a menu item to `SettingsScreen` calling `conversationDao.clearAll()`).
- The Notification Listener only reads app name + title (not expanded body / actions).
- MediaProjection (screenshot) triggers the **OS consent dialog every single time** — no silent captures.

---

## License
MIT — free to modify, self-host and redistribute.
