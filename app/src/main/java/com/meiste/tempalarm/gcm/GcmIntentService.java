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
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.meiste.tempalarm.AppConstants;
import com.meiste.tempalarm.R;
import com.meiste.tempalarm.sync.SyncAdapter;
import com.meiste.tempalarm.ui.Alarm;
import com.meiste.tempalarm.ui.CurrentTemp;

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

        if (extras != null && !extras.isEmpty() && extras.containsKey(MSG_KEY) &&
            GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
            final String type = extras.getString(MSG_KEY);
            final String state = extras.getString(STATE_KEY, "UNKNOWN");
            Timber.d("Received %s message with state %s", type, state);

            // Only show notification if user wants results notifications
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            final boolean ok2notify = prefs.getBoolean(AppConstants.PREF_NOTIFICATIONS, true);

            SyncAdapter.requestSync(this, ok2notify);

            if (ok2notify) {
                cancelNotification();

                final Intent killIntent = new Intent(AppConstants.INTENT_ACTION_KILL_ALARM);
                LocalBroadcastManager.getInstance(this).sendBroadcastSync(killIntent);

                final TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                final boolean inCall = tm.getCallState() != TelephonyManager.CALL_STATE_IDLE;

                switch (type) {
                    case MSG_KEY_ALARM:
                        handleAlarm(state, inCall);
                        break;
                    case MSG_KEY_SENSOR:
                        handleSensor(state, inCall);
                        break;
                    default:
                        Timber.i("Message type unknown. Ignoring.");
                        break;
                }
            }
        }

        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    private void handleAlarm(final String state, final boolean inCall) {
        switch (state) {
            case STATE_TEMP_TOO_LOW:
                if (inCall) {
                    showNotification(R.string.alarm_low_temp);
                } else {
                    showAlarm(R.string.alarm_low_temp);
                }
                break;
            case STATE_TEMP_NORMAL:
                showNotification(R.string.notify_temp_normal);
                break;
        }
    }

    private void handleSensor(final String state, final boolean inCall) {
        switch (state) {
            case STATE_SENSOR_STOPPED:
                showNotification(R.string.notify_state_stopped);
                break;
            case STATE_SENSOR_RUNNING:
                showNotification(R.string.notify_state_running);
                break;
            case STATE_SENSOR_PWR_OUT:
                if (inCall) {
                    showNotification(R.string.alarm_pwr_out);
                } else {
                    showAlarm(R.string.alarm_pwr_out);
                }
                break;
        }
    }

    private void showNotification(final int resId) {
        final Intent intent = new Intent(this, CurrentTemp.class);
        final PendingIntent pi = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        final String notifyText = getString(resId);
        final Notification notification = new NotificationCompat.Builder(this)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))
                .setTicker(notifyText)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(notifyText)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL).build();

        final NotificationManager nm =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(0, notification);
    }

    private void cancelNotification() {
        final NotificationManager nm =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancelAll();
    }

    private void showAlarm(final int resId) {
        final Intent intent = new Intent(this, Alarm.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(AppConstants.INTENT_EXTRA_ALERT_MSG, resId);
        startActivity(intent);
    }
}
