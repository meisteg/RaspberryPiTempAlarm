/*
 * Copyright 2012-2013 Google Inc.
 * Copyright (C) 2013-2014 Gregory S. Meiste  <http://gregmeiste.com>
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

import java.io.IOException;
import java.sql.Timestamp;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.format.DateUtils;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.meiste.tempalarm.BuildConfig;
import com.meiste.tempalarm.Log;
import com.meiste.tempalarm.backend.registration.Registration;

/**
 * Utilities for device registration.
 * <p>
 * <strong>Note:</strong> this class uses a private {@link SharedPreferences}
 * object to keep track of the registration token.
 */
public final class Gcm {

    private static final String TAG = Gcm.class.getSimpleName();

    private static final long ON_SERVER_LIFESPAN_MS = 7 * DateUtils.DAY_IN_MILLIS;

    private static final String SENDER_ID = "233631917633";

    private static final boolean ALWAYS_FORCE_REGISTRATION = BuildConfig.DEBUG;

    private static final String GCM_PREFS = "com.google.android.gcm";
    private static final String PROPERTY_REG_ID = "regId";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final String PROPERTY_ON_SERVER = "onServer";
    private static final String PROPERTY_ON_SERVER_EXPIRATION_TIME = "onServerExpirationTime";

    private static Registration sRegService = null;

    /**
     * Checks whether the application was successfully registered on GCM
     * service.
     */
    public static boolean isRegistered(final Context context) {
        return getRegistrationId(context).length() > 0;
    }

    /**
     * Checks whether the device was successfully registered in the server side.
     */
    public static boolean isRegisteredOnServer(final Context context) {
        if (ALWAYS_FORCE_REGISTRATION) {
            return false;
        }

        final SharedPreferences prefs = getGcmPrefs(context);
        final boolean isRegistered = prefs.getBoolean(PROPERTY_ON_SERVER, false);
        Log.d(TAG, "Is registered on server: " + isRegistered);
        if (isRegistered) {
            // checks if the information is not stale
            final long expirationTime =
                    prefs.getLong(PROPERTY_ON_SERVER_EXPIRATION_TIME, -1);
            if (System.currentTimeMillis() > expirationTime) {
                Log.d(TAG, "flag expired on: " + new Timestamp(expirationTime));
                return false;
            }
        }
        return isRegistered;
    }

    /**
     * Gets the current registration ID for application on GCM service.
     * <p>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     *         registration ID.
     */
    public static String getRegistrationId(final Context context) {
        final SharedPreferences prefs = getGcmPrefs(context);
        final String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.d(TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        final int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        if (registeredVersion != BuildConfig.VERSION_CODE) {
            Log.d(TAG, "App version changed from " + registeredVersion + " to " + BuildConfig.VERSION_CODE);
            return "";
        }
        return registrationId;
    }

    /**
     * Checks if the application needs to be registered, and registers if needed.
     */
    public static void registerIfNeeded(final Context context,
                                        final GoogleAccountCredential credential) {
        if (!isRegistered(context) || !isRegisteredOnServer(context)) {
            Log.d(TAG, "Registering with GCM");
            register(context, credential);
        } else {
            Log.d(TAG, "Already registered with GCM: " + getRegistrationId(context));
        }
    }

    /**
     * Registers the device with GCM service.
     */
    public static void register(final Context context, final GoogleAccountCredential credential) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (Gcm.class) {
                    try {
                        final GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
                        final String regId = gcm.register(SENDER_ID);
                        storeRegistrationId(context, regId);
                        sendRegistrationIdToBackend(context, regId, credential);
                    } catch (final IOException e) {
                        Log.e(TAG, "GCM registration failed: " + e);
                    }
                }
            }
        }).start();
    }

    /**
     * @return Application's {@code SharedPreferences}.
     */
    private static SharedPreferences getGcmPrefs(final Context context) {
        return context.getSharedPreferences(GCM_PREFS, Context.MODE_PRIVATE);
    }

    /**
     * Stores the registration ID and app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * @param context application's context.
     * @param regId registration ID
     */
    private static void storeRegistrationId(final Context context, final String regId) {
        final SharedPreferences prefs = getGcmPrefs(context);
        Log.d(TAG, "Saving regId on app version " + BuildConfig.VERSION_CODE);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, BuildConfig.VERSION_CODE);
        editor.apply();
    }

    /**
     * Sets whether the device was successfully registered in the server side.
     */
    private static void setRegisteredOnServer(final Context context, final boolean flag) {
        final SharedPreferences prefs = getGcmPrefs(context);
        final Editor editor = prefs.edit();
        editor.putBoolean(PROPERTY_ON_SERVER, flag);
        // set the flag's expiration date
        final long expirationTime = System.currentTimeMillis() + ON_SERVER_LIFESPAN_MS;
        Log.d(TAG, "Setting registeredOnServer status as " + flag + " until " +
                new Timestamp(expirationTime));
        editor.putLong(PROPERTY_ON_SERVER_EXPIRATION_TIME, expirationTime);
        editor.apply();
    }

    /**
     * Sends the registration ID to app server.
     */
    private static void sendRegistrationIdToBackend(final Context context, final String regId,
                                                    final GoogleAccountCredential credential)
            throws IOException {
        Log.d(TAG, "Device registered with GCM: regId = " + regId);

        if (sRegService == null) {
            sRegService = new Registration.Builder(AndroidHttp.newCompatibleTransport(),
                    new AndroidJsonFactory(), credential).build();
        }

        sRegService.register(regId).execute();
        setRegisteredOnServer(context, true);
    }

    private Gcm() {
        throw new UnsupportedOperationException();
    }
}
