# PROJEXA

Smart haptic navigation belt for deafblind users: an ESP32-WROOM-32 belt (4 ultrasonic sensors, 4 vibration motors) plus an Android companion app.

## Repository layout

| Path | What it is |
|---|---|
| `firmware/projexa_belt/` | Belt firmware with Bluetooth Classic SPP (device name `PROJEXA`) |
| `firmware/original_belt/` | Original standalone firmware (no Bluetooth), kept as a restore point |
| `app/` | Android app (Kotlin, Jetpack Compose, MVVM) |

## Belt firmware

Pins: FRONT 13/34/21, BACK 14/35/22, LEFT 18/32/23, RIGHT 19/33/27 (trig/echo/motor). Obstacle threshold 50 cm.

- Obstacles within 50 cm turn the matching motor on (steady) even with no phone connected.
- Sends one JSON line per scan: `{"front":35.2,"back":null,"left":18.5,"right":75.0,"motors":{...}}` (`null` = no valid reading, not "clear").
- Accepts `T,<F|B|L|R>` (motor test), `N,<dir>,<onMs>,<offMs>` (pulsed navigation cue, expires after 3 s), `N,X` (clear).
- Motor priority: obstacle > test > navigation.

Flash with Arduino IDE: board **ESP32 Dev Module**, 115200 baud serial monitor.

## Android app

- Live four-direction monitoring (belt radar), motor tests, sensor health
- Walking turn-by-turn navigation (OpenStreetMap search + public foot routing) with belt haptic cues and off-route recalculation
- Text-to-speech, speech-to-text conversation, Braille output via TalkBack
- Settings: text size, high contrast, simple mode, pulse length, cue distance, speech rate
- English, Hindi, Kannada (translations need native-speaker review)

Build: `./gradlew :app:assembleDebug` (needs the Android SDK; set `sdk.dir` in `local.properties`). Pair "PROJEXA" in Android Bluetooth settings, then press **Connect to belt** in the app.

> GPS cannot confirm that a road crossing is safe. The public OSM servers are for light use; self-host routing before real deployment.
