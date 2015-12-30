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
package com.meiste.tempalarm.gcm;

import android.app.IntentService;
import android.content.Intent;

import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.meiste.tempalarm.R;

import java.io.IOException;

import timber.log.Timber;

public class RegistrationService extends IntentService {

    private static final String[] TOPICS = {"tempLow"};

    public RegistrationService() {
        super(RegistrationService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        try {
            /*
             * Initially getToken goes out to the network to retrieve the token
             * and subsequent calls are local. R.string.gcm_defaultSenderId (the
             * Sender ID) is derived from google-services.json.
             *
             * https://developers.google.com/cloud-messaging/android/start has
             * more details on this file.
             */
            final InstanceID instanceID = InstanceID.getInstance(this);
            final String token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            Timber.d("GCM Registration Token: %s", token);

            subscribeToTopics(token);
        } catch (final IOException e) {
            Timber.e(e, "Failed to complete token refresh");
        }
    }

    private void subscribeToTopics(final String token) throws IOException {
        final GcmPubSub pubSub = GcmPubSub.getInstance(this);
        for (final String topic : TOPICS) {
            pubSub.subscribe(token, "/topics/" + topic, null);
        }
    }
}
