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
package com.meiste.tempalarm.backend.service;

import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;
import com.meiste.tempalarm.backend.Constants;
import com.meiste.tempalarm.backend.SettingUtils;

import java.io.IOException;
import java.util.logging.Logger;

public class Gcm {

    private static final Logger log = Logger.getLogger(Gcm.class.getSimpleName());

    private static final String TOPIC_TEMP_LOW = "/topics/tempLow";
    private static final String COLLAPSE_KEY_ALARM = "alarm";
    private static final int NUM_RETRIES = 3;
    private static final int TTL_SECONDS = 600; // 10 minutes

    public static void sendLowTemp() throws IOException {
        send(TOPIC_TEMP_LOW);
    }

    private static void send(final String topic) throws IOException {
        final Sender sender = new Sender(getApiKey());
        final Message msg = new Message.Builder()
                .collapseKey(COLLAPSE_KEY_ALARM)
                .priority(Message.Priority.HIGH)
                .timeToLive(TTL_SECONDS)
                .build();
        final Result result = sender.send(msg, topic, NUM_RETRIES);
        if (result.getMessageId() != null) {
            log.fine("Message sent to " + topic);
        } else {
            log.warning("Error when sending message: " + result.getErrorCodeName());
        }
    }

    private static String getApiKey() {
        return SettingUtils.getSettingValue(Constants.SETTING_GCM_API_KEY,
                Constants.DEFAULT_GCM_API_KEY);
    }
}
