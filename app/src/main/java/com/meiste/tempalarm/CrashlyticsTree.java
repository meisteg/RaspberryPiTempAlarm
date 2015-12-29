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

import android.util.Log;

import com.crashlytics.android.Crashlytics;

import timber.log.Timber;

public class CrashlyticsTree extends Timber.DebugTree {

    @Override
    protected void log(final int priority, final String tag, final String msg, final Throwable t) {
        Crashlytics.log(priorityToString(priority) + "/" + tag + ": " + msg);
    }

    private static String priorityToString(final int priority) {
        switch (priority) {
            case Log.ASSERT:
                return "WTF";
            case Log.ERROR:
                return "E";
            case Log.WARN:
                return "W";
            case Log.INFO:
                return "I";
            case Log.DEBUG:
                return "D";
            case Log.VERBOSE:
                return "V";
            default:
                return String.valueOf(priority);
        }
    }
}
