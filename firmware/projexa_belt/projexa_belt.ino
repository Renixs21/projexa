/*
  PROJEXA - SMART HAPTIC NAVIGATION BELT
  ESP32-WROOM-32 DevKit
  4 Ultrasonic Sensors + 4 Vibration Motors
  + Bluetooth Classic SPP ("PROJEXA")

  Detection threshold: 50 cm
  Serial Monitor: 115200 baud

  FRONT: TRIG 13, ECHO 34, MOTOR 21
  BACK:  TRIG 14, ECHO 35, MOTOR 22
  LEFT:  TRIG 18, ECHO 32, MOTOR 23
  RIGHT: TRIG 19, ECHO 33, MOTOR 27

  CHANGES vs. the original firmware (sensor logic is untouched):
    * Bluetooth Classic SPP added (device name "PROJEXA").
    * Sensor snapshot sent to the phone after every full scan, as
      newline-delimited JSON. Invalid readings are sent as null.
    * Motors are now driven by one arbitration function (updateMotors):
        1. Obstacle within 50 cm  -> motor ON (steady). HIGHEST priority.
        2. Phone motor test       -> motor ON for 500 ms.
        3. Phone navigation cue   -> pulsed ON/OFF pattern. LOWEST priority.
    * The 70 ms inter-sensor delay is preserved, but it is now spent in a
      loop that keeps servicing Bluetooth and motor patterns.
    * With no phone connected the belt behaves exactly like the original.

  PHONE -> BELT COMMANDS (ASCII, one per line, '\n' terminated)
    T,<F|B|L|R>                 pulse that motor for 500 ms (hardware test)
    N,<F|B|L|R>,<onMs>,<offMs>  pulse navigation cue on that motor
                                (onMs 40..1000, offMs 40..2000)
    N,X                         clear navigation cue
  A navigation cue expires after NAV_TIMEOUT_MS unless the phone refreshes it,
  so a crashed app or lost link can never leave a cue stuck on.
  Navigation cues are pulsed; obstacle alerts are steady, so the two can be
  told apart by feel.

  BELT -> PHONE (one line per scan cycle)
    {"front":35.2,"back":null,"left":18.5,"right":75.0,
     "motors":{"front":true,"back":false,"left":true,"right":false}}
*/

#include "BluetoothSerial.h"

#if !defined(CONFIG_BT_ENABLED) || !defined(CONFIG_BLUEDROID_ENABLED)
#error Bluetooth is not enabled. Select an ESP32 board (ESP32 Dev Module) in the Arduino IDE.
#endif

BluetoothSerial SerialBT;

struct Sensor {
  const char* name;
  int trigPin;
  int echoPin;
  int motorPin;
};

Sensor devices[] = {
  {"FRONT", 13, 34, 21},
  {"BACK",  14, 35, 22},
  {"LEFT",  18, 32, 23},
  {"RIGHT", 19, 33, 27}
};

const int DEVICE_COUNT = 4;
const float DETECTION_LIMIT = 50.0;

const unsigned long ECHO_TIMEOUT_US = 6500;

// ---- Added for Bluetooth / arbitration --------------------------------
const char* JSON_KEYS[DEVICE_COUNT] = {"front", "back", "left", "right"};
const char  DIR_CODES[DEVICE_COUNT] = {'F', 'B', 'L', 'R'};

const unsigned long SCAN_GAP_MS     = 70;    // original ultrasonic interference delay
const unsigned long TEST_PULSE_MS   = 500;
const unsigned long NAV_TIMEOUT_MS  = 3000;
const unsigned long NAV_ON_MIN = 40,  NAV_ON_MAX = 1000;
const unsigned long NAV_OFF_MIN = 40, NAV_OFF_MAX = 2000;

float lastDistance[DEVICE_COUNT] = {-1, -1, -1, -1};
bool  obstacle[DEVICE_COUNT]     = {false, false, false, false};
bool  motorState[DEVICE_COUNT]   = {false, false, false, false};

int           testDir = -1;
unsigned long testUntil = 0;

int           navDir = -1;
unsigned long navOnMs = 0, navOffMs = 0, navStart = 0, navExpires = 0;

char cmdBuf[40];
int  cmdLen = 0;
bool cmdOverflow = false;
// -----------------------------------------------------------------------

float readDistanceCM(const Sensor &s) {
  digitalWrite(s.trigPin, LOW);
  delayMicroseconds(3);

  digitalWrite(s.trigPin, HIGH);
  delayMicroseconds(10);
  digitalWrite(s.trigPin, LOW);

  unsigned long duration =
    pulseIn(s.echoPin, HIGH, ECHO_TIMEOUT_US);

  if (duration == 0) {
    return -1.0;
  }

  float distance = duration * 0.0343f / 2.0f;

  if (distance > 100.0 || distance < 2.0) {
    return -1.0;
  }

  return distance;
}

// ---- Motor arbitration ------------------------------------------------
bool desiredMotor(int i, unsigned long now) {
  // 1. Obstacle protection always wins.
  if (obstacle[i]) return true;

  // 2. Hardware test pulse from the phone.
  if (testDir == i && (long)(testUntil - now) > 0) return true;

  // 3. Navigation cue (pulsed).
  if (navDir == i && (long)(navExpires - now) > 0) {
    unsigned long period = navOnMs + navOffMs;
    unsigned long phase = (now - navStart) % period;
    return phase < navOnMs;
  }
  return false;
}

void updateMotors() {
  unsigned long now = millis();

  if (testDir >= 0 && (long)(testUntil - now) <= 0) testDir = -1;
  if (navDir >= 0 && (long)(navExpires - now) <= 0) navDir = -1;

  for (int i = 0; i < DEVICE_COUNT; i++) {
    bool want = desiredMotor(i, now);
    if (want != motorState[i]) {
      digitalWrite(devices[i].motorPin, want ? HIGH : LOW);
      motorState[i] = want;
    }
  }
}

// ---- Bluetooth command handling ---------------------------------------
int dirIndex(char c) {
  for (int i = 0; i < DEVICE_COUNT; i++) {
    if (DIR_CODES[i] == c) return i;
  }
  return -1;
}

void handleCommand(char* line) {
  // Strip trailing CR / spaces.
  int n = strlen(line);
  while (n > 0 && (line[n - 1] == '\r' || line[n - 1] == ' ')) line[--n] = 0;

  if (n >= 3 && line[0] == 'T' && line[1] == ',' && n == 3) {
    int d = dirIndex(line[2]);
    if (d >= 0) {
      testDir = d;
      testUntil = millis() + TEST_PULSE_MS;
      Serial.print("BT: test motor ");
      Serial.println(devices[d].name);
      return;
    }
  } else if (n >= 3 && line[0] == 'N' && line[1] == ',') {
    if (n == 3 && line[2] == 'X') {
      navDir = -1;
      Serial.println("BT: nav cleared");
      return;
    }
    int d = dirIndex(line[2]);
    char* p = line + 3;
    if (d >= 0 && *p == ',') {
      char* end1;
      unsigned long on = strtoul(p + 1, &end1, 10);
      if (end1 != p + 1 && *end1 == ',') {
        char* end2;
        unsigned long off = strtoul(end1 + 1, &end2, 10);
        if (end2 != end1 + 1 && *end2 == 0 &&
            on >= NAV_ON_MIN && on <= NAV_ON_MAX &&
            off >= NAV_OFF_MIN && off <= NAV_OFF_MAX) {
          unsigned long now = millis();
          // Restart the pattern only when the cue changes, so periodic
          // refreshes from the phone do not disturb the rhythm.
          if (navDir != d || navOnMs != on || navOffMs != off ||
              (long)(navExpires - now) <= 0) {
            navStart = now;
          }
          navDir = d;
          navOnMs = on;
          navOffMs = off;
          navExpires = now + NAV_TIMEOUT_MS;
          Serial.print("BT: nav ");
          Serial.println(devices[d].name);
          return;
        }
      }
    }
  }
  Serial.print("BT: rejected command: ");
  Serial.println(line);
}

void pollBluetooth() {
  if (!SerialBT.hasClient()) {
    // Phone gone: drop phone-originated cues. Obstacle protection is unaffected.
    navDir = -1;
    testDir = -1;
    cmdLen = 0;
    cmdOverflow = false;
    return;
  }
  while (SerialBT.available()) {
    char c = (char)SerialBT.read();
    if (c == '\n') {
      if (!cmdOverflow && cmdLen > 0) {
        cmdBuf[cmdLen] = 0;
        handleCommand(cmdBuf);
      }
      cmdLen = 0;
      cmdOverflow = false;
    } else if (cmdLen < (int)sizeof(cmdBuf) - 1) {
      cmdBuf[cmdLen++] = c;
    } else {
      cmdOverflow = true;  // discard over-long line
    }
  }
}

// Keeps the original 70 ms spacing between sensor reads while still
// servicing Bluetooth and navigation/test patterns.
void waitServicing(unsigned long ms) {
  unsigned long start = millis();
  while (millis() - start < ms) {
    pollBluetooth();
    updateMotors();
    delay(2);
  }
}

void sendSnapshot() {
  if (!SerialBT.hasClient()) return;

  SerialBT.print("{");
  for (int i = 0; i < DEVICE_COUNT; i++) {
    SerialBT.print("\"");
    SerialBT.print(JSON_KEYS[i]);
    SerialBT.print("\":");
    if (lastDistance[i] < 0) {
      SerialBT.print("null");
    } else {
      SerialBT.print(lastDistance[i], 1);
    }
    SerialBT.print(",");
  }
  SerialBT.print("\"motors\":{");
  for (int i = 0; i < DEVICE_COUNT; i++) {
    SerialBT.print("\"");
    SerialBT.print(JSON_KEYS[i]);
    SerialBT.print("\":");
    SerialBT.print(motorState[i] ? "true" : "false");
    if (i < DEVICE_COUNT - 1) SerialBT.print(",");
  }
  SerialBT.println("}}");
}

void setup() {
  Serial.begin(115200);
  delay(1000);
  Serial.println();
  Serial.println("[boot] serial OK");

  for (int i = 0; i < DEVICE_COUNT; i++) {
    pinMode(devices[i].trigPin, OUTPUT);
    pinMode(devices[i].echoPin, INPUT);
    pinMode(devices[i].motorPin, OUTPUT);

    digitalWrite(devices[i].trigPin, LOW);
    digitalWrite(devices[i].motorPin, LOW);
  }
  Serial.println("[boot] pins OK");

  Serial.println("[boot] starting Bluetooth (can take a few seconds)...");
  if (!SerialBT.begin("PROJEXA")) {
    Serial.println("Bluetooth start FAILED - belt continues in standalone mode");
  } else {
    Serial.println("[boot] Bluetooth started");
  }

  Serial.println();
  Serial.println("================================");
  Serial.println(" PROJEXA SMART HAPTIC BELT");
  Serial.println(" Four Direction Detection");
  Serial.println(" Detection Limit: 50 CM");
  Serial.println(" Bluetooth SPP name: PROJEXA");
  Serial.println("================================");
}

void loop() {
  for (int i = 0; i < DEVICE_COUNT; i++) {

    float distance = readDistanceCM(devices[i]);

    bool obstacleDetected =
      (distance >= 2.0 && distance <= DETECTION_LIMIT);

    lastDistance[i] = distance;
    obstacle[i] = obstacleDetected;
    updateMotors();

    Serial.print(devices[i].name);
    Serial.print(": ");

    if (distance < 0) {
      Serial.print("NO OBJECT");
    } else {
      Serial.print(distance, 1);
      Serial.print(" cm");
    }

    Serial.print(" | MOTOR: ");
    Serial.println(motorState[i] ? "ON" : "OFF");

    // Prevent ultrasonic interference
    waitServicing(SCAN_GAP_MS);
  }
  Serial.println("--------------------------------");

  sendSnapshot();
}
