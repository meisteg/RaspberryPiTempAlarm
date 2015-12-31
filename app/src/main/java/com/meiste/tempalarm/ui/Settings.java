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
package com.meiste.tempalarm.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;

import com.meiste.tempalarm.BuildConfig;
import com.meiste.tempalarm.R;

import timber.log.Timber;

public class Settings extends AppCompatActivity {

    private static final String KEY_BUILD = "build";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Fragment f = getFragmentManager().findFragmentById(android.R.id.content);
        if (f == null || !(f instanceof SettingsFragment)) {
            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, new SettingsFragment()).commit();
        }
    }

    public static class SettingsFragment extends PreferenceFragment {

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Timber.v("onCreate");
            addPreferencesFromResource(R.xml.preferences);

            findPreference(KEY_BUILD).setSummary(BuildConfig.VERSION_NAME);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            Timber.v("onDestroy");
        }
    }
}
