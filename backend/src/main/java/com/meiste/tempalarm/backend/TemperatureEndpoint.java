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

import com.google.android.gcm.server.Constants;
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

    private static final String SETTING_GCM_API_KEY = "gcm.api.key";

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
        // TODO: Get threshold from settings datastore
        // TODO: Send email
        if (temperature < 45.0f) {
            sendMessage("Temperature is " + record.getDegF() + " degrees");
        }
    }

    // TODO: Add collapseKey, remove limit
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
        List<RegistrationRecord> records = ofy().load().type(RegistrationRecord.class).limit(10).list();
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
                if (error.equals(Constants.ERROR_NOT_REGISTERED)) {
                    log.warning("Registration Id " + record.getRegId() + " no longer registered with GCM, removing from datastore");
                    // if the device is no longer registered with Gcm, remove it from the datastore
                    ofy().delete().entity(record).now();
                } else {
                    log.warning("Error when sending message : " + error);
                }
            }
        }
    }

    private String getApiKey() {
        SettingRecord setting = ofy().load().type(SettingRecord.class)
                .filter("name", SETTING_GCM_API_KEY).first().now();
        if (setting == null) {
            setting = new SettingRecord();
            setting.setName(SETTING_GCM_API_KEY);
            setting.setValue("replace_this_text_with_the_real_API_Key");
            ofy().save().entity(setting).now();

            log.severe("Created fake GCM API key! Please go to App Engine admin console, "
                    + "change its value to your API Key, then restart the server!");
        }
        return setting.getValue();
    }
}
