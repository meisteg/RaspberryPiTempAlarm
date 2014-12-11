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
package com.meiste.tempalarm.backend;

public class Constants {
    public static final String WEB_CLIENT_ID = "233631917633-guka8vakql5qnni9j519jh7tl8brnqjt.apps.googleusercontent.com";
    public static final String ANDROID_CLIENT_ID_1 = "233631917633-qgilqmf6kcc3gcq7n1ofhk9upaom9mem.apps.googleusercontent.com";
    public static final String ANDROID_CLIENT_ID_2 = "233631917633-djo90qn056itd2qjocvmpiu5icigi4m0.apps.googleusercontent.com";
    public static final String ANDROID_AUDIENCE = WEB_CLIENT_ID;
    public static final String EMAIL_SCOPE = "https://www.googleapis.com/auth/userinfo.email";

    public static final String SETTING_GCM_API_KEY = "gcm.api.key";
    public static final String SETTING_THRES_LIGHT = "thres.light";
    public static final String SETTING_THRES_LOW = "thres.low";
    public static final String SETTING_REPORT_RATE = "report.rate";
    public static final String SETTING_TASK_NAME = "task.pwr_out";

    public static final String DEFAULT_GCM_API_KEY = "replace_this_text_with_the_real_API_Key";
    public static final String DEFAULT_THRES_LIGHT = Integer.toString(500);
    public static final String DEFAULT_THRES_LOW = Float.toString(45.0f);
    public static final String DEFAULT_REPORT_RATE = Integer.toString(300);
    public static final String DEFAULT_TASK_NAME = "none";

    public static final int REPORTS_MISSED_BEFORE_ALARM = 2;

    public static final long SECOND_IN_MILLIS = 1000;
    public static final long MINUTE_IN_MILLIS = 60000;
    public static final long HOUR_IN_MILLIS = 3600000;
    public static final long DAY_IN_MILLIS = 86400000;
}
