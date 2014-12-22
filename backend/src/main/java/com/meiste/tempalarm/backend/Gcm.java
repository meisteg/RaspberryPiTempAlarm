/*
 * Copyright (C) 2014 Gregory S. Meiste  <http://gregmeiste.com>
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

import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import static com.meiste.tempalarm.backend.OfyService.ofy;

public class Gcm {

    private static final Logger log = Logger.getLogger(Gcm.class.getSimpleName());

    private static final String COLLAPSE_KEY_ALARM = "alarm";
    private static final String COLLAPSE_KEY_SENSOR = "sensor";
    private static final String STATE_KEY = "state";

    public static enum AlarmState {
        TEMP_TOO_LOW, TEMP_NORMAL
    }
    public static void sendAlarm(final AlarmState state) throws IOException {
        send(COLLAPSE_KEY_ALARM, state.name());
    }

    public static enum SensorState {
        STOPPED, RUNNING, PWR_OUT
    }
    public static void sendSensor(final SensorState state) throws IOException {
        send(COLLAPSE_KEY_SENSOR, state.name());
    }

    private static void send(final String key, final String state) throws IOException {
        final Sender sender = new Sender(getApiKey());
        final Message msg = new Message.Builder().collapseKey(key).addData(STATE_KEY, state).build();
        final List<RegistrationRecord> records = ofy().load().type(RegistrationRecord.class).list();
        for (final RegistrationRecord record : records) {
            final Result result = sender.send(msg, record.getRegId(), 5);
            if (result.getMessageId() != null) {
                log.fine("Message sent to " + record.getRegId());
                final String canonicalRegId = result.getCanonicalRegistrationId();
                if (canonicalRegId != null) {
                    // if the regId changed, we have to update the datastore
                    log.info("Registration Id changed for " + record.getRegId() +
                            " updating to " + canonicalRegId);
                    if (findRecord(canonicalRegId) == null) {
                        record.setRegId(canonicalRegId);
                        ofy().save().entity(record).now();
                    } else {
                        // Device already told us new regId, so simply delete the old one
                        ofy().delete().entity(record).now();
                    }
                }
            } else {
                final String error = result.getErrorCodeName();
                if (error.equals(com.google.android.gcm.server.Constants.ERROR_NOT_REGISTERED)) {
                    log.warning("Registration Id " + record.getRegId() +
                            " no longer registered with GCM, removing from datastore");
                    // if the device is no longer registered with GCM, remove it from the datastore
                    ofy().delete().entity(record).now();
                } else {
                    log.warning("Error when sending message : " + error);
                }
            }
        }
    }

    public static RegistrationRecord findRecord(final String regId) {
        return ofy().load().type(RegistrationRecord.class).filter("regId", regId).first().now();
    }

    private static String getApiKey() {
        return SettingUtils.getSettingValue(Constants.SETTING_GCM_API_KEY,
                Constants.DEFAULT_GCM_API_KEY);
    }
}
