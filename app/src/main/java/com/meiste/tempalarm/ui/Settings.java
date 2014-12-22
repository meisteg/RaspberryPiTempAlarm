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
package com.meiste.tempalarm.ui;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;

import com.meiste.tempalarm.AppConstants;
import com.meiste.tempalarm.BuildConfig;
import com.meiste.tempalarm.R;
import com.meiste.tempalarm.sync.AccountUtils;

import timber.log.Timber;

public class Settings extends ActionBarActivity {

    private static final String KEY_ACCOUNT = "account";
    private static final String KEY_BUILD = "build";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final Fragment f = getFragmentManager().findFragmentById(android.R.id.content);
        if (f == null || !(f instanceof SettingsFragment)) {
            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, new SettingsFragment()).commit();
        }
    }

    public static class SettingsFragment extends PreferenceFragment
            implements SharedPreferences.OnSharedPreferenceChangeListener {

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Timber.v("onCreate");
            addPreferencesFromResource(R.xml.preferences);

            findPreference(KEY_ACCOUNT).setSummary(AccountUtils.getAccountName(getActivity()));
            findPreference(KEY_BUILD).setSummary(BuildConfig.VERSION_NAME);
        }

        @Override
        public void onResume() {
            super.onResume();
            Timber.v("onResume");

            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            prefs.registerOnSharedPreferenceChangeListener(this);
            setListPrefSummary(prefs, AppConstants.PREF_SYNC_FREQ);
        }

        @Override
        public void onPause() {
            super.onPause();
            Timber.v("onPause");
            PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            Timber.v("onDestroy");
        }

        @Override
        public void onSharedPreferenceChanged(final SharedPreferences prefs, final String key) {
            if (key.equals(AppConstants.PREF_SYNC_FREQ)) {
                AccountUtils.setPeriodicSync(getActivity(), prefs);
                setListPrefSummary(prefs, key);
            }
        }

        private void setListPrefSummary(final SharedPreferences prefs, final String key) {
            final ListPreference listPreference = (ListPreference) findPreference(key);
            final int index = listPreference.findIndexOfValue(prefs.getString(key, null));

            listPreference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
        }
    }
}
