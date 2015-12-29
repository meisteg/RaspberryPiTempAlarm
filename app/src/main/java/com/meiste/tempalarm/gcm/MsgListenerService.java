/*
 * Copyright (C) 2014-2015 Gregory S. Meiste  <http://gregmeiste.com>
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
import android.telephony.TelephonyManager;

import com.google.android.gms.gcm.GcmListenerService;
import com.meiste.tempalarm.AppConstants;
import com.meiste.tempalarm.R;
import com.meiste.tempalarm.sync.SyncAdapter;
import com.meiste.tempalarm.ui.Alarm;
import com.meiste.tempalarm.ui.CurrentTemp;

import timber.log.Timber;

public class MsgListenerService extends GcmListenerService {

    @Override
    public void onMessageReceived(final String from, final Bundle data) {
        Timber.d("Received message from GCM");

        // Only show notification if the user wants notifications
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean ok2notify = prefs.getBoolean(AppConstants.PREF_NOTIFICATIONS, true);

        SyncAdapter.requestSync(this, ok2notify);

        if (ok2notify) {
            final TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            final boolean inCall = tm.getCallState() != TelephonyManager.CALL_STATE_IDLE;

            if (inCall) {
                showNotification();
            } else {
                showAlarm();
            }
        }
    }

    private void showNotification() {
        final Intent intent = new Intent(this, CurrentTemp.class);
        final PendingIntent pi = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        final String notifyText = getString(R.string.alarm_low_temp);
        final Notification notification = new NotificationCompat.Builder(this)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))
                .setTicker(notifyText)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(notifyText)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build();

        final NotificationManager nm =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(0, notification);
    }

    private void showAlarm() {
        final Intent intent = new Intent(this, Alarm.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}
