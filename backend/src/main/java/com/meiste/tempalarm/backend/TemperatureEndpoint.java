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
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiNamespace;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import javax.inject.Named;

import static com.meiste.tempalarm.backend.OfyService.ofy;

@Api(
        name = "temperature",
        version = "v1",
        namespace = @ApiNamespace(
                ownerDomain = "backend.tempalarm.meiste.com",
                ownerName = "backend.tempalarm.meiste.com",
                packagePath = ""
        )
)
public class TemperatureEndpoint {
    private static final Logger log = Logger.getLogger(TemperatureEndpoint.class.getName());

    /**
     * Query the desired reporting rate
     *
     * @return The rate the temperature should be reported to the backend
     */
    public SettingRecord getReportRate() {
        return SettingUtils.getSettingRecord(Constants.SETTING_REPORT_RATE,
                Constants.DEFAULT_REPORT_RATE);
    }

    /**
     * Report the current temperature to the backend
     *
     * @param temperature The current temperature in degrees fahrenheit
     */
    public void report(@Named("temperature") final float temperature) throws IOException {
        final TemperatureRecord record = new TemperatureRecord();
        record.setDegF(temperature);
        ofy().save().entity(record).now();

        // TODO: Only send GCM on first report below threshold
        // TODO: Send email
        if (temperature < getLowTempThreshold()) {
            sendMessage("Temperature is " + record.getDegF() + " degrees");
        }
    }

    /**
     * Notifies the backend that temperature reporting has stopped.
     */
    public void stop() throws IOException {
        // TODO: Implement actual logic
        sendMessage("Temperature reporting has stopped");
    }

    // TODO: Add collapseKey
    private void sendMessage(String message) throws IOException {
        if (message == null || message.trim().length() == 0) {
            log.warning("Not sending message because it is empty");
            return;
        }
        // crop longer messages
        if (message.length() > 1000) {
            message = message.substring(0, 1000) + "[...]";
        }
        Sender sender = new Sender(getApiKey());
        Message msg = new Message.Builder().addData("message", message).build();
        List<RegistrationRecord> records = ofy().load().type(RegistrationRecord.class).list();
        for (RegistrationRecord record : records) {
            Result result = sender.send(msg, record.getRegId(), 5);
            if (result.getMessageId() != null) {
                log.info("Message sent to " + record.getRegId());
                String canonicalRegId = result.getCanonicalRegistrationId();
                if (canonicalRegId != null) {
                    // if the regId changed, we have to update the datastore
                    log.info("Registration Id changed for " + record.getRegId() + " updating to " + canonicalRegId);
                    record.setRegId(canonicalRegId);
                    ofy().save().entity(record).now();
                }
            } else {
                String error = result.getErrorCodeName();
                if (error.equals(com.google.android.gcm.server.Constants.ERROR_NOT_REGISTERED)) {
                    log.warning("Registration Id " + record.getRegId() + " no longer registered with GCM, removing from datastore");
                    // if the device is no longer registered with Gcm, remove it from the datastore
                    ofy().delete().entity(record).now();
                } else {
                    log.warning("Error when sending message : " + error);
                }
            }
        }
    }

    private static float getLowTempThreshold() {
        return Float.valueOf(SettingUtils.getSettingValue(Constants.SETTING_THRES_LOW,
                Constants.DEFAULT_THRES_LOW));
    }

    private static String getApiKey() {
        return SettingUtils.getSettingValue(Constants.SETTING_GCM_API_KEY,
                Constants.DEFAULT_GCM_API_KEY);
    }
}
