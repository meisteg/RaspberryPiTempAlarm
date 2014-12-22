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
package com.meiste.tempalarm.gcm;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import timber.log.Timber;

public class GcmIntentService extends IntentService {

    private static final String MSG_KEY = "collapse_key";
    private static final String MSG_KEY_ALARM = "alarm";
    private static final String MSG_KEY_SENSOR = "sensor";

    private static final String STATE_KEY = "state";
    private static final String STATE_TEMP_TOO_LOW = "TEMP_TOO_LOW";
    private static final String STATE_TEMP_NORMAL = "TEMP_NORMAL";
    private static final String STATE_SENSOR_STOPPED = "STOPPED";
    private static final String STATE_SENSOR_RUNNING = "RUNNING";
    private static final String STATE_SENSOR_PWR_OUT = "PWR_OUT";

    public GcmIntentService() {
        super(GcmIntentService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        final Bundle extras = intent.getExtras();
        final GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        final String messageType = gcm.getMessageType(intent);

        if (extras != null && !extras.isEmpty() && extras.containsKey(MSG_KEY)) {
            if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                final String type = extras.getString(MSG_KEY);
                final String state = extras.getString(STATE_KEY, "UNKNOWN");
                Timber.d("Received %s message with state %s", type, state);

                switch (type) {
                    case MSG_KEY_ALARM:
                        handleAlarm(state);
                        break;
                    case MSG_KEY_SENSOR:
                        handleSensor(state);
                        break;
                    default:
                        Timber.i("Message type unknown. Ignoring.");
                        break;
                }
            }
        }

        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    private void handleAlarm(final String state) {
        switch (state) {
            case STATE_TEMP_TOO_LOW:
                break;
            case STATE_TEMP_NORMAL:
                break;
        }
    }

    private void handleSensor(final String state) {
        switch (state) {
            case STATE_SENSOR_STOPPED:
                break;
            case STATE_SENSOR_RUNNING:
                break;
            case STATE_SENSOR_PWR_OUT:
                break;
        }
    }
}
