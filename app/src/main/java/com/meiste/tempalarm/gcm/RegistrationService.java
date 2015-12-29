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
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.meiste.tempalarm.AppConstants;
import com.meiste.tempalarm.R;
import com.meiste.tempalarm.backend.registration.Registration;
import com.meiste.tempalarm.sync.AccountUtils;

import java.io.IOException;

import timber.log.Timber;

public class RegistrationService extends IntentService {

    private static final long ON_SERVER_LIFESPAN_MS = 7 * DateUtils.DAY_IN_MILLIS;

    public RegistrationService() {
        super(RegistrationService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

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

            sendTokenToServer(prefs, token);

            final SharedPreferences.Editor editor = prefs.edit();
            editor.putLong(AppConstants.PREF_GCM_LAST_TIME, System.currentTimeMillis());
            editor.putString(AppConstants.PREF_GCM_TOKEN, token);
            editor.apply();
        } catch (final IOException e) {
            Timber.e(e, "Failed to complete token refresh");

            /*
             * If an exception happens while fetching the new token or updating
             * the registration data on the server, this ensures that we'll
             * attempt the update at a later time.
             */
            prefs.edit().putLong(AppConstants.PREF_GCM_LAST_TIME, 0).apply();
        }
    }

    public void sendTokenToServer(final SharedPreferences prefs, final String token)
            throws IOException {
        final long expirationTime = System.currentTimeMillis() - ON_SERVER_LIFESPAN_MS;
        if ((prefs.getLong(AppConstants.PREF_GCM_LAST_TIME, 0) > expirationTime) &&
                token.equals(prefs.getString(AppConstants.PREF_GCM_TOKEN, null))) {
            Timber.v("GCM registration on server is still valid");
            return;
        }

        Timber.d("Sending GCM registration to server");

        final Registration registration = new Registration.Builder(AppConstants.HTTP_TRANSPORT,
                AppConstants.JSON_FACTORY, AccountUtils.getCredential(this))
                .setApplicationName(getString(R.string.app_name))
                .build();
        registration.register(token).execute();
    }
}
