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

import android.text.TextUtils;

import com.crashlytics.android.Crashlytics;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

/**
 * Mostly copy of {@link timber.log.Timber.DebugTree}, but logs to Crashlytics.
 */
public class CrashlyticsTree implements Timber.TaggedTree {

    private static final Pattern ANONYMOUS_CLASS = Pattern.compile("\\$\\d+$");
    private static final ThreadLocal<String> NEXT_TAG = new ThreadLocal<>();

    private static String createTag() {
        String tag = NEXT_TAG.get();
        if (tag != null) {
            NEXT_TAG.remove();
            return tag;
        }

        tag = new Throwable().getStackTrace()[5].getClassName();
        final Matcher m = ANONYMOUS_CLASS.matcher(tag);
        if (m.find()) {
            tag = m.replaceAll("");
        }
        return tag.substring(tag.lastIndexOf('.') + 1);
    }

    @Override
    public void tag(final String tag) {
        NEXT_TAG.set(tag);
    }

    @Override
    public void v(final String message, final Object... args) {
        logToCrashlytics("V", message, args);
    }

    @Override
    public void v(final Throwable t, final String message, final Object... args) {
        logToCrashlytics("V", message, args);
    }

    @Override
    public void d(final String message, final Object... args) {
        logToCrashlytics("D", message, args);
    }

    @Override
    public void d(final Throwable t, final String message, final Object... args) {
        logToCrashlytics("D", message, args);
    }

    @Override
    public void i(final String message, final Object... args) {
        logToCrashlytics("I", message, args);
    }

    @Override
    public void i(final Throwable t, final String message, final Object... args) {
        logToCrashlytics("I", message, args);
    }

    @Override
    public void w(final String message, final Object... args) {
        logToCrashlytics("W", message, args);
    }

    @Override
    public void w(final Throwable t, final String message, final Object... args) {
        logToCrashlytics("W", message, args);
    }

    @Override
    public void e(final String message, final Object... args) {
        logToCrashlytics("E", message, args);
    }

    @Override
    public void e(final Throwable t, final String message, final Object... args) {
        logToCrashlytics("E", message, args);
        Crashlytics.logException(t);
    }

    private void logToCrashlytics(final String level, final String message, final Object... args) {
        if (TextUtils.isEmpty(message)) {
            return;
        }
        Crashlytics.log(level + "/" + createTag() + ": " + String.format(message, args));
    }
}
