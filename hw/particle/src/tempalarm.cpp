/*
 * Copyright (C) 2015-2019 Gregory S. Meiste  <http://gregmeiste.com>
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

#include <Adafruit_DHT.h>

#define SERIAL             Serial   // USB port
//#define SERIAL           Serial1  // TX/RX pins
#define SERIAL_BAUD        115200

#define DHT_PIN            D2
#define DHT_TYPE           DHT22

#define SENSOR_CHECK_MS    2000

#define EEPROM_PROGRAM_KEY 0xA5A5A5A5
#define EEPROM_REPORT_MS   60000

#define MAX_READING_DELTA  5.0

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

static void doMonitorIfTime(void) {
    static unsigned long lastReadingMillis = 0;
    static bool haveValidReading = false;
    unsigned long now = millis();

    if ((now - lastReadingMillis) >= SENSOR_CHECK_MS) {
        float h = dht.getHumidity();
        float f = dht.getTempFarenheit();

        float delta_temp = f - currentTempF;

        // Check if reading failed
        if (isnan(h) || isnan(f)) {
            SERIAL.println("Failed to read from DHT sensor!");
        }
        // Sanity check temperature read from sensor. If delta from previous
        // reading is greater than MAX_READING_DELTA degrees, it probably is bogus.
        else if (haveValidReading && (fabsf(delta_temp) > MAX_READING_DELTA)) {
            SERIAL.print("Bad reading (");
            SERIAL.print(f);
            SERIAL.println(") from DHT sensor!");
        }
        // Reading is good, keep it
        else {
            currentHumid = h;
            currentTempF = f;

            SERIAL.print("Humid: ");
            SERIAL.print(currentHumid);
            SERIAL.print("% - ");
            SERIAL.print("Temp: ");
            SERIAL.print(currentTempF);
            SERIAL.print("*F ");
            SERIAL.println(Time.timeStr());

            haveValidReading = true;
        }

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
        Particle.publish("sensorData", publishString);

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

    Particle.variable("currentTempF", &currentTempF, DOUBLE);
    Particle.variable("currentHumid", &currentHumid, DOUBLE);

    Particle.function("reportRate", setReportRate);
}

void loop(void) {
    doMonitorIfTime();
    doReportIfTime();
}
