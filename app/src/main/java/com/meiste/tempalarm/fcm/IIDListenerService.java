/*
 * Copyright (C) 2015-2017 Gregory S. Meiste  <http://gregmeiste.com>
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
package com.meiste.tempalarm.fcm;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.google.firebase.messaging.FirebaseMessaging;
import com.meiste.tempalarm.BuildConfig;

import timber.log.Timber;

public class IIDListenerService extends FirebaseInstanceIdService {

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is also called
     * when the InstanceID token is initially generated.
     */
    @Override
    public void onTokenRefresh() {
        subscribeToTopics();
    }

    public static void subscribeToTopics() {
        final String token = FirebaseInstanceId.getInstance().getToken();
        Timber.d("FCM Registration Token: %s", token);

        FirebaseMessaging.getInstance().subscribeToTopic("tempLow");

        if (BuildConfig.DEBUG) {
            Timber.v("Also subscribing to FCM debug topic");
            FirebaseMessaging.getInstance().subscribeToTopic("debug");
        }
    }
}
