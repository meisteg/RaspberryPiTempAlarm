/*
 * Copyright (C) 2017 Gregory S. Meiste  <http://gregmeiste.com>
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
package com.meiste.tempalarm.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import com.meiste.tempalarm.R;
import com.meiste.tempalarm.ui.CurrentTemp;

/**
 * Helper class to manage notification channels, and create notifications.
 */
public class NotificationHelper extends ContextWrapper {
    private NotificationManager manager;
    private static final String PRIMARY_CHANNEL = "default";

    /**
     * Registers notification channels, which can be used later by individual notifications.
     *
     * @param context The application context
     */
    public NotificationHelper(final Context context) {
        super(context);
    }

    public void initializeNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final NotificationChannel chan = new NotificationChannel(PRIMARY_CHANNEL,
                    getString(R.string.noti_channel_name), NotificationManager.IMPORTANCE_HIGH);
            chan.enableLights(true);
            chan.setLightColor(getColor(R.color.accent));
            chan.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            chan.enableVibration(true);
            getManager().createNotificationChannel(chan);
        }
    }

    public void showNotification() {
        final Intent intent = new Intent(this, CurrentTemp.class);
        final PendingIntent pi = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        final String notifyText = getString(R.string.alarm_temp_out_range);
        final Notification notification = new NotificationCompat.Builder(this, PRIMARY_CHANNEL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setTicker(notifyText)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(notifyText)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setShowWhen(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build();
        getManager().notify(0, notification);
    }

    /**
     * Get the notification manager.
     *
     * @return The system service NotificationManager
     */
    private NotificationManager getManager() {
        if (manager == null) {
            manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return manager;
    }
}