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
package com.meiste.gcm;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.format.DateUtils;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;
import java.sql.Timestamp;

/**
 * Utilities for device registration.
 * <p>
 * <strong>Note:</strong> this class uses a private {@link android.content.SharedPreferences}
 * object to keep track of the registration token.
 */
public final class GCMHelper {

    private static final String TAG = GCMHelper.class.getSimpleName();

    private static final String PREFERENCES = "com.google.android.gcm";
    private static final String PROPERTY_REG_ID = "regId";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final String PROPERTY_ON_SERVER = "onServer";
    private static final String PROPERTY_ON_SERVER_EXPIRATION_TIME = "onServerExpirationTime";
    private static final String PROPERTY_ON_SERVER_LIFESPAN = "onServerLifeSpan";

    public static final long DEFAULT_ON_SERVER_LIFESPAN_MS = 7 * DateUtils.DAY_IN_MILLIS;

    /**
     * Callback for GCM registration process.
     */
    public interface OnGcmRegistrationListener {
        /**
         * Called to notify that GCM registration ID should be sent to backend.
         *
         * @param context Application context
         * @param regId   GCM registration ID to be sent to backend
         *
         * @return True on success, false on failure
         */
        public boolean onSendRegistrationIdToBackend(final Context context, final String regId)
                throws IOException;
    }

    /**
     * Checks whether the application was successfully registered on GCM service.
     *
     * @param context Application context
     *
     * @return True if registered, false if not registered with GCM service.
     */
    public static boolean isRegistered(final Context context) {
        return getRegistrationId(context).length() > 0;
    }

    /**
     * Checks whether the device was successfully registered in the server side.
     *
     * @param context Application context
     */
    public static boolean isRegisteredOnServer(final Context context) {
        final SharedPreferences prefs = getGcmPrefs(context);
        final boolean isRegistered = prefs.getBoolean(PROPERTY_ON_SERVER, false);
        log(context, Log.VERBOSE, "Is registered on server: " + isRegistered);
        if (isRegistered) {
            // checks if the information is not stale
            final long expirationTime =
                    prefs.getLong(PROPERTY_ON_SERVER_EXPIRATION_TIME, -1);
            if (System.currentTimeMillis() > expirationTime) {
                log(context, Log.VERBOSE, "flag expired on: " + new Timestamp(expirationTime));
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
            log(context, Log.VERBOSE, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        final int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        final int appVersion = getAppVersion(context);
        if (registeredVersion != appVersion) {
            log(context, Log.VERBOSE, "App version changed from " + registeredVersion + " to " +
                    appVersion);
            return "";
        }
        return registrationId;
    }

    /**
     * Checks if the application needs to be registered, and registers if needed.
     */
    public static void registerIfNeeded(final Context context, final String senderId,
                                        final OnGcmRegistrationListener l) {
        if (!isRegistered(context) || !isRegisteredOnServer(context)) {
            log(context, Log.VERBOSE, "Registering with GCM");
            register(context, senderId, l);
        } else {
            log(context, Log.VERBOSE, "Already registered with GCM: " + getRegistrationId(context));
        }
    }

    /**
     * Registers the device with GCM service.
     *
     * @param context  Application context.
     * @param senderId Google Project ID of the accounts authorized to send messages to
     *                 this application.
     */
    public static void register(final Context context, final String senderId,
                                final OnGcmRegistrationListener l) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (GCMHelper.class) {
                    try {
                        final GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
                        final String regId = gcm.register(senderId);
                        storeRegistrationId(context, regId);
                        setRegisteredOnServer(context, l.onSendRegistrationIdToBackend(context, regId));
                    } catch (final IOException e) {
                        log(context, Log.ERROR, "GCM registration failed: " + e);
                    }
                }
            }
        }).start();
    }

    /**
     * Gets how long (in milliseconds) the {@link #isRegisteredOnServer(android.content.Context)}
     * property is valid.
     */
    public static long getRegisterOnServerLifespan(final Context context) {
        final SharedPreferences prefs = getGcmPrefs(context);
        return prefs.getLong(PROPERTY_ON_SERVER_LIFESPAN, DEFAULT_ON_SERVER_LIFESPAN_MS);
    }

    /**
     * Sets how long (in milliseconds) the {@link #isRegisteredOnServer(android.content.Context)}
     * flag is valid.
     */
    public static void setRegisterOnServerLifespan(final Context context, final long lifespan) {
        final SharedPreferences prefs = getGcmPrefs(context);
        prefs.edit().putLong(PROPERTY_ON_SERVER_LIFESPAN, lifespan).apply();
    }

    /**
     * @return Application's {@code SharedPreferences}.
     */
    private static SharedPreferences getGcmPrefs(final Context context) {
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
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
        final int appVersion = getAppVersion(context);
        log(context, Log.VERBOSE, "Saving regId on app version " + appVersion);
        final Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
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
        final long expirationTime = System.currentTimeMillis() + getRegisterOnServerLifespan(context);
        log(context, Log.VERBOSE, "Setting registeredOnServer status as " + flag + " until " +
                new Timestamp(expirationTime));
        editor.putLong(PROPERTY_ON_SERVER_EXPIRATION_TIME, expirationTime);
        editor.apply();
    }

    /**
     * Gets the application version.
     */
    private static int getAppVersion(Context context) {
        try {
            final PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (final PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    /**
     * Logs a message on logcat.
     *
     * @param context application's context.
     * @param priority logging priority
     * @param template message's template
     * @param args list of arguments
     */
    private static void log(Context context, int priority, String template,
                            Object... args) {
        if (Log.isLoggable(TAG, priority)) {
            String message = String.format(template, args);
            Log.println(priority, TAG, "[" + context.getPackageName() + "]: "
                    + message);
        }
    }

    private GCMHelper() {
        throw new UnsupportedOperationException();
    }
}
