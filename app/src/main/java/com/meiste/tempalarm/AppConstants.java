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
package com.meiste.tempalarm;

import android.text.format.DateUtils;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.HttpTransport;

public class AppConstants {
    public static final String CLIENT_AUDIENCE =
            "server:client_id:233631917633-guka8vakql5qnni9j519jh7tl8brnqjt.apps.googleusercontent.com";

    public static final String PREF_ACCOUNT_NAME = "account_name";
    public static final String PREF_GCM_LAST_TIME = "gcm_last_time";
    public static final String PREF_GCM_TOKEN = "gcm_token";
    public static final String PREF_LAST_SYNC = "last_sync";
    public static final String PREF_NOTIFICATIONS = "notifications_enabled";
    public static final String PREF_REPORT_RATE = "report_rate";
    public static final String PREF_SYNC_FREQ = "sync_frequency";

    public static final int DATE_FORMAT_FLAGS =
            DateUtils.FORMAT_SHOW_DATE |
            DateUtils.FORMAT_NO_YEAR |
            DateUtils.FORMAT_SHOW_TIME |
            DateUtils.FORMAT_NO_NOON |
            DateUtils.FORMAT_NO_MIDNIGHT;
    public static final int DATE_FORMAT_FLAGS_GRAPH =
            DateUtils.FORMAT_NO_YEAR |
            DateUtils.FORMAT_SHOW_TIME |
            DateUtils.FORMAT_NO_NOON |
            DateUtils.FORMAT_NO_MIDNIGHT;

    public static final int GRAPH_NUM_HORIZONTAL_LABELS = 4;

    public static final HttpTransport HTTP_TRANSPORT = AndroidHttp.newCompatibleTransport();
    public static final AndroidJsonFactory JSON_FACTORY = new AndroidJsonFactory();
}
