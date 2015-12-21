/*
 * Copyright (C) 2014-2015 Gregory S. Meiste  <http://gregmeiste.com>
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

import java.io.IOException;
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
            log.warning("Reported temperature is below threshold!");

            // Retrieve previous reported temperature from datastore
            final TemperatureRecord prevRecord = ofy().load().type(TemperatureRecord.class)
                    .order("-timestamp").limit(1).first().now();
            final float prevTemp = (prevRecord != null) ? prevRecord.getFloatDegF() : Float.NaN;

            // Only alarm if this is first reading below threshold
            if (prevTemp >= lowTempThreshold) {
                Gcm.sendAlarm(Gcm.AlarmState.TEMP_TOO_LOW);
                AlertEmail.sendLowTemp(record.getDegF());
            }
        }

        // New record must be saved after previous temperature check
        ofy().save().entity(record);
    }

    public static float getLowTempThreshold() {
        return Float.valueOf(SettingUtils.getSettingValue(Constants.SETTING_THRES_LOW,
                Constants.DEFAULT_THRES_LOW));
    }
}
