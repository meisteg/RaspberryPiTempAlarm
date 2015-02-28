/*
 * Copyright (C) 2015 Gregory S. Meiste  <http://gregmeiste.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "Adafruit_DHT/Adafruit_DHT.h"

#define SERIAL             Serial1
#define SERIAL_BAUD        115200

#define DHT_PIN            D2
#define DHT_TYPE           DHT22

#define BUTTON_PIN         D1
#define BUTTON_PRESS_MS    50

#define LED_PIN            D7

#define SENSOR_CHECK_MS    2000
#define SENSOR_REPORT_MS   30000

double currentTempF = 0.0;
double currentHumid = 0.0;

bool isReporting = true;
volatile unsigned long buttonPressMillis = 0;
volatile unsigned long buttonReleaseMillis = 0;

DHT dht(DHT_PIN, DHT_TYPE);

void buttonPress() {
    buttonPressMillis = millis();
    buttonReleaseMillis = 0;
}

void checkStateChange() {
    if (buttonPressMillis > 0) {
        if (digitalRead(BUTTON_PIN) == HIGH) {
            // False button press, ignore.
            buttonPressMillis = 0;
        } else if ((millis() - buttonPressMillis) >= BUTTON_PRESS_MS) {
            // Button press debounced
            if (isReporting) {
                digitalWrite(LED_PIN, HIGH);
                SERIAL.println("Reporting stopped!");
            } else {
                digitalWrite(LED_PIN, LOW);
                SERIAL.println("Reporting started!");
            }
            isReporting = !isReporting;
            buttonPressMillis = 0;
        }
    } else if (!isReporting && (digitalRead(BUTTON_PIN) == HIGH)) {
        if (buttonReleaseMillis > 0) {
            if ((millis() - buttonReleaseMillis) >= BUTTON_PRESS_MS) {
                SERIAL.println("Entering STOP mode");
                delay(20);
                Spark.sleep(BUTTON_PIN, FALLING);
            }
        } else {
            buttonReleaseMillis = millis();
        }
    }
}

void doMonitorIfTime() {
    static unsigned long lastReadingMillis = 0;
    unsigned long now = millis();

    if ((now - lastReadingMillis) >= SENSOR_CHECK_MS) {
        float h = dht.getHumidity();
        float f = dht.getTempFarenheit();

        // Check if any reads failed and exit early (to try again).
        if (isnan(h) || isnan(f)) {
            SERIAL.println("Failed to read from DHT sensor!");
            return;
        }

        currentHumid = h;
        currentTempF = f;

        SERIAL.print("Humid: "); 
        SERIAL.print(currentHumid);
        SERIAL.print("% - ");
        SERIAL.print("Temp: "); 
        SERIAL.print(currentTempF);
        SERIAL.print("*F ");
        SERIAL.println(Time.timeStr());

        lastReadingMillis = now;
    }
}

void doReportIfTime() {
    static bool firstTime = true;
    static unsigned long lastReportMillis = 0;
    unsigned long now = millis();
    char publishString[64];

    if (firstTime || ((now - lastReportMillis) >= SENSOR_REPORT_MS)) {
        SERIAL.println("Reporting sensor data to server");
        
        snprintf(publishString, sizeof(publishString), "{\"tempF\": %.1f, \"humid\": %.1f}", currentTempF, currentHumid);
        Spark.publish("sensorData", publishString);
        
        lastReportMillis = now;
        firstTime = false;
    }
}

void setup() {
    SERIAL.begin(SERIAL_BAUD);

    SERIAL.println("Initializing DHT22 sensor");
    dht.begin();
    delay(2000);

    pinMode(LED_PIN, OUTPUT);

    pinMode(BUTTON_PIN, INPUT_PULLUP);
    attachInterrupt(BUTTON_PIN, buttonPress, FALLING);
    
    Spark.variable("currentTempF", &currentTempF, DOUBLE);
    Spark.variable("currentHumid", &currentHumid, DOUBLE);
}

void loop() {
    checkStateChange();

    if (isReporting) {
        doMonitorIfTime();
        doReportIfTime();
    }
}

