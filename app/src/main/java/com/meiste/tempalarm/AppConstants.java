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
package com.meiste.tempalarm;

import android.text.format.DateUtils;

public class AppConstants {
    private static final String FIREBASE_URL_BASE = "https://incandescent-heat-6272.firebaseio.com/";
    public static final String FIREBASE_URL_SENSOR = FIREBASE_URL_BASE + "shoptemps";
    public static final String FIREBASE_URL_CONNECTED = FIREBASE_URL_BASE + ".info/connected";

    public static final String PREF_NIGHT_MODE = "night_mode";
    public static final String PREF_ALARM_ENABLED = "notifications_enabled";
    public static final String PREF_NUM_RECORDS = "num_records";

    public static final String DEFAULT_NIGHT_MODE = "1";
    public static final int DEFAULT_NUM_RECORDS = 120;

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
}
