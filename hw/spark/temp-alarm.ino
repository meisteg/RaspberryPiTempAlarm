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

#define EEPROM_PROGRAM_KEY 0xA5A5A5A5
#define EEPROM_REPORT_MS   60000

struct settingsEEPROM {
    unsigned int programmedKey;
    unsigned int sensorReportMillis;
};

union {
    settingsEEPROM settings;
    char raw[sizeof(settingsEEPROM)];
} EEPROM_Data;

double currentTempF = 0.0;
double currentHumid = 0.0;

bool isReporting = true;
volatile unsigned long buttonPressMillis = 0;
volatile unsigned long buttonReleaseMillis = 0;

DHT dht(DHT_PIN, DHT_TYPE);

static void readEEPROM(void) {
    unsigned int i;
    for (i = 0; i < sizeof(settingsEEPROM); ++i) {
        EEPROM_Data.raw[i] = EEPROM.read(i);
    }
    SERIAL.print(i);
    SERIAL.println(" bytes read from EEPROM");
}

static void writeEEPROM(void) {
    unsigned int i;
    for (i = 0; i < sizeof(settingsEEPROM); ++i) {
        EEPROM.write(i, EEPROM_Data.raw[i]);
    }
    SERIAL.print(i);
    SERIAL.println(" bytes wrote to EEPROM");
}

static int setReportRate(String rate) {
    unsigned int sensorReportMillis = atoi(rate.c_str());

    if (sensorReportMillis >= EEPROM_REPORT_MS) {
        SERIAL.print("Changing sensorReportMillis from ");
        SERIAL.print(EEPROM_Data.settings.sensorReportMillis);
        SERIAL.print(" to ");
        SERIAL.println(sensorReportMillis);
    
        EEPROM_Data.settings.sensorReportMillis = sensorReportMillis;
        writeEEPROM();

        return 0;
    }
    
    SERIAL.println("New sensorReportMillis value is too small. Rejecting.");
    return -1;
}

static void buttonPress(void) {
    buttonPressMillis = millis();
    buttonReleaseMillis = 0;
}

static void checkStateChange(void) {
    if (buttonPressMillis > 0) {
        if (digitalRead(BUTTON_PIN) == HIGH) {
            // False button press, ignore.
            buttonPressMillis = 0;
        } else if ((millis() - buttonPressMillis) >= BUTTON_PRESS_MS) {
            // Button press debounced
            if (isReporting) {
                digitalWrite(LED_PIN, HIGH);
                SERIAL.println("Reporting stopped!");
                Spark.publish("sensorStopped");
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

static void doMonitorIfTime(void) {
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

static void doReportIfTime(void) {
    static bool firstTime = true;
    static unsigned long lastReportMillis = 0;
    unsigned long now = millis();
    char publishString[64];

    if (firstTime || ((now - lastReportMillis) >= EEPROM_Data.settings.sensorReportMillis)) {
        SERIAL.println("Reporting sensor data to server");
        
        snprintf(publishString, sizeof(publishString), "{\"tempF\": %.1f, \"humid\": %.1f}", currentTempF, currentHumid);
        Spark.publish("sensorData", publishString);
        
        lastReportMillis = now;
        firstTime = false;
    }
}

void setup(void) {
    SERIAL.begin(SERIAL_BAUD);

    SERIAL.println("Initializing DHT22 sensor");
    dht.begin();
    delay(2000);

    readEEPROM();
    if (EEPROM_PROGRAM_KEY == EEPROM_Data.settings.programmedKey) {
        SERIAL.print("sensorReportMillis = ");
        SERIAL.println(EEPROM_Data.settings.sensorReportMillis);
    } else {
        SERIAL.println("EEPROM not programmed! Setting default values.");
        EEPROM_Data.settings.programmedKey = EEPROM_PROGRAM_KEY;
        EEPROM_Data.settings.sensorReportMillis = EEPROM_REPORT_MS;
        writeEEPROM();
    }

    pinMode(LED_PIN, OUTPUT);

    pinMode(BUTTON_PIN, INPUT_PULLUP);
    attachInterrupt(BUTTON_PIN, buttonPress, FALLING);
    
    Spark.variable("currentTempF", &currentTempF, DOUBLE);
    Spark.variable("currentHumid", &currentHumid, DOUBLE);
    
    Spark.function("reportRate", setReportRate);
}

void loop(void) {
    checkStateChange();

    if (isReporting) {
        doMonitorIfTime();
        doReportIfTime();
    }
}

