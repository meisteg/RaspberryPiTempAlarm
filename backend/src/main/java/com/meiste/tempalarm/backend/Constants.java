/*
 * Copyright (C) 2014-2016 Gregory S. Meiste  <http://gregmeiste.com>
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
    public static final String FIREBASE_URL = "https://incandescent-heat-6272.firebaseio.com/shoptemps";

    public static final String SETTING_EMAILS = "emails";
    public static final String SETTING_FB_AUTHTOKEN = "fb.authToken";
    public static final String SETTING_GCM_API_KEY = "gcm.api.key";
    public static final String SETTING_THRES_HIGH = "thres.high";
    public static final String SETTING_THRES_LOW = "thres.low";
    public static final String SETTING_RECORD_EXPIRE = "record.expire";

    public static final String DEFAULT_EMAILS = "admins";
    public static final String DEFAULT_FB_AUTHTOKEN = null;
    public static final String DEFAULT_GCM_API_KEY = "replace_this_text_with_the_real_API_Key";
    public static final String DEFAULT_THRES_HIGH = Float.toString(80.0f);   /* degrees */
    public static final String DEFAULT_THRES_LOW = Float.toString(45.0f);    /* degrees */
    public static final String DEFAULT_RECORD_EXPIRE = Integer.toString(2);  /* days */
}
