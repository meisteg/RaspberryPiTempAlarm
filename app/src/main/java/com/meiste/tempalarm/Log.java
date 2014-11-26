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
package com.meiste.tempalarm;

public class Log {
    private static final String TAG = "RaspPiTempAlarm";

    public static void e(final String tag, final String msg) {
        android.util.Log.e(TAG + "." + tag, msg);
    }

    public static void w(final String tag, final String msg) {
        android.util.Log.w(TAG + "." + tag, msg);
    }

    public static void i(final String tag, final String msg) {
        android.util.Log.i(TAG + "." + tag, msg);
    }

    public static void d(final String tag, final String msg) {
        if (BuildConfig.DEBUG) {
            android.util.Log.d(TAG + "." + tag, msg);
        }
    }

    public static void v(final String tag, final String msg) {
        if (BuildConfig.DEBUG) {
            android.util.Log.v(TAG + "." + tag, msg);
        }
    }

    public static void wtf(final String tag, final String msg) {
        android.util.Log.wtf(TAG + "." + tag, msg);
    }
}
