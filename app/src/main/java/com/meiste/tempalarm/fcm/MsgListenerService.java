/*
 * Copyright (C) 2014-2017 Gregory S. Meiste  <http://gregmeiste.com>
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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.meiste.tempalarm.AppConstants;
import com.meiste.tempalarm.ui.Alarm;
import com.meiste.tempalarm.util.NotificationHelper;

import timber.log.Timber;

public class MsgListenerService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Timber.d("Received FCM message from %s", remoteMessage.getFrom());

        /* Determine how user should be notified */
        if (isAlarmEnabled() && !isInCall()) {
            showAlarm();
        } else {
            new NotificationHelper(this).showNotification();
        }
    }

    private void showAlarm() {
        final Intent intent = new Intent(this, Alarm.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private boolean isAlarmEnabled() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        return prefs.getBoolean(AppConstants.PREF_ALARM_ENABLED, true);
    }

    private boolean isInCall() {
        final TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        return (tm != null) && (tm.getCallState() != TelephonyManager.CALL_STATE_IDLE);
    }
}
