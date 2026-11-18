/*
  PROJEXA - SMART HAPTIC NAVIGATION BELT
  ESP32-WROOM-32 DevKit
  4 Ultrasonic Sensors + 4 Vibration Motors

  ORIGINAL FIRMWARE (no Bluetooth). Backup copy: upload this to restore the belt
  to its original behaviour.

  Detection threshold: 50 cm
  Serial Monitor: 115200 baud

  FRONT: TRIG 13, ECHO 34, MOTOR 21
  BACK:  TRIG 14, ECHO 35, MOTOR 22
  LEFT:  TRIG 18, ECHO 32, MOTOR 23
  RIGHT: TRIG 19, ECHO 33, MOTOR 27
*/

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

void setup() {
  Serial.begin(115200);
  delay(1000);

  for (int i = 0; i < DEVICE_COUNT; i++) {
    pinMode(devices[i].trigPin, OUTPUT);
    pinMode(devices[i].echoPin, INPUT);
    pinMode(devices[i].motorPin, OUTPUT);

    digitalWrite(devices[i].trigPin, LOW);
    digitalWrite(devices[i].motorPin, LOW);
  }

  Serial.println();
  Serial.println("================================");
  Serial.println(" PROJEXA SMART HAPTIC BELT");
  Serial.println(" Four Direction Detection");
  Serial.println(" Detection Limit: 50 CM");
  Serial.println("================================");
}

void loop() {
  for (int i = 0; i < DEVICE_COUNT; i++) {

    float distance = readDistanceCM(devices[i]);

    bool obstacleDetected =
      (distance >= 2.0 && distance <= DETECTION_LIMIT);

    digitalWrite(
      devices[i].motorPin,
      obstacleDetected ? HIGH : LOW
    );

    Serial.print(devices[i].name);
    Serial.print(": ");

    if (distance < 0) {
      Serial.print("NO OBJECT");
    } else {
      Serial.print(distance, 1);
      Serial.print(" cm");
    }

    Serial.print(" | MOTOR: ");
    Serial.println(obstacleDetected ? "ON" : "OFF");

    // Prevent ultrasonic interference
    delay(70);
  }

  Serial.println("--------------------------------");
}
