/*
 * Copyright (C) 2014-2016 Gregory S. Meiste  <http://gregmeiste.com>
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

package com.meiste.tempalarm.backend;

import com.meiste.tempalarm.backend.service.Firebase;
import com.meiste.tempalarm.backend.service.Gcm;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.logging.Logger;

import static com.meiste.tempalarm.backend.OfyService.ofy;

public class TemperatureCommon {
    private static final Logger log = Logger.getLogger(TemperatureCommon.class.getName());

    public static void report(final float temperature, final float humidity)
            throws IOException {
        log.fine("temperature=" + temperature + ", humidity=" + humidity);

        final TemperatureRecord record = new TemperatureRecord();
        record.setDegF(temperature);
        record.setHumidity(humidity);

        final float lowTempThreshold = getLowTempThreshold();
        if (temperature < lowTempThreshold) {
            log.warning("Reported temperature is below low threshold!");

            // Only alarm if this is first reading below low threshold
            if (getPrevTemp() >= lowTempThreshold) {
                Gcm.sendLowTemp();
                AlertEmail.sendLowTemp(record.getDegF());
            }
        } else {
            final float highTempThreshold = getHighTempThreshold();
            if (temperature > highTempThreshold) {
                log.warning("Reported temperature is above high threshold!");

                // Only alarm if this is first reading above high threshold
                if (getPrevTemp() <= highTempThreshold) {
                    Gcm.sendHighTemp();
                    AlertEmail.sendHighTemp(record.getDegF());
                }
            }
        }

        // New record must be saved after previous temperature check
        ofy().save().entity(record);

        /* Catch exceptions from Firebase so they don't stop the Particle webhook */
        try {
            final Firebase firebase = new Firebase();
            firebase.addQuery("print", "silent");
            final Firebase.Response response = firebase.post(record.toJson());
            if ((response.code != HttpURLConnection.HTTP_OK) &&
                    (response.code != HttpURLConnection.HTTP_NO_CONTENT)) {
                log.severe("Failed to post to Firebase: code=" + response.code);
            }
        } catch (final IOException e) {
            log.severe("Failed to post to Firebase: " + e);
        }
    }

    @SuppressWarnings("WeakerAccess") // Used in JSP code
    public static float getLowTempThreshold() {
        return Float.valueOf(SettingUtils.getSettingValue(Constants.SETTING_THRES_LOW,
                Constants.DEFAULT_THRES_LOW));
    }

    @SuppressWarnings("WeakerAccess") // Used in JSP code
    public static float getHighTempThreshold() {
        return Float.valueOf(SettingUtils.getSettingValue(Constants.SETTING_THRES_HIGH,
                Constants.DEFAULT_THRES_HIGH));
    }

    private static float getPrevTemp() {
        final TemperatureRecord prevRecord = ofy().load().type(TemperatureRecord.class)
                .order("-timestamp").limit(1).first().now();
        return (prevRecord != null) ? prevRecord.getFloatDegF() : Float.NaN;
    }
}
