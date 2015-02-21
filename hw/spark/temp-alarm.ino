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

#define DHT_PIN         D2
#define DHT_TYPE        DHT22

#define BUTTON_PIN      D1
#define BUTTON_PRESS_MS 50

#define LED_PIN         D7

unsigned long reportIntervalMillis = 2000;
bool isReporting = true;

volatile unsigned long buttonPressMillis = 0;

DHT dht(DHT_PIN, DHT_TYPE);

void buttonPress() {
    buttonPressMillis = millis();
}

void checkStateChange() {
    if (buttonPressMillis > 0) {
        if (digitalRead(BUTTON_PIN) == HIGH) {
            buttonPressMillis = 0;
        } else if ((millis() - buttonPressMillis) >= BUTTON_PRESS_MS) {
            if (isReporting) {
                digitalWrite(LED_PIN, HIGH);
                Serial.println("Reporting stopped!");
            } else {
                digitalWrite(LED_PIN, LOW);
                Serial.println("Reporting started!");
            }
            isReporting = !isReporting;
            buttonPressMillis = 0;
        }
    }
}

void doReportIfTime() {
    static unsigned long lastReadingMillis = 0;
    unsigned long now = millis();

    if ((now - lastReadingMillis) >= reportIntervalMillis) {
        float h = dht.getHumidity();
        float f = dht.getTempFarenheit();

        // Check if any reads failed and exit early (to try again).
        if (isnan(h) || isnan(f)) {
            Serial.println("Failed to read from DHT sensor!");
            return;
        }

        Serial.print("Humid: "); 
        Serial.print(h);
        Serial.print("% - ");
        Serial.print("Temp: "); 
        Serial.print(f);
        Serial.print("*F ");
        Serial.println(Time.timeStr());

        lastReadingMillis = now;
    }
}

void setup() {
    Serial.begin(9600);

    pinMode(LED_PIN, OUTPUT);

    pinMode(BUTTON_PIN, INPUT_PULLUP);
    attachInterrupt(BUTTON_PIN, buttonPress, FALLING);

    dht.begin();
}

void loop() {
    checkStateChange();

    if (isReporting) {
        doReportIfTime();
    }
}

