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
package com.meiste.tempalarm.ui;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import com.meiste.tempalarm.BuildConfig;
import com.meiste.tempalarm.R;

import timber.log.Timber;

import static com.meiste.tempalarm.AppConstants.DEFAULT_NUM_RECORDS;
import static com.meiste.tempalarm.AppConstants.PREF_NUM_RECORDS;

public class Settings extends AppCompatActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Fragment f = getFragmentManager().findFragmentById(android.R.id.content);
        if (f == null || !(f instanceof SettingsFragment)) {
            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, new SettingsFragment()).commit();
        }
    }

    public static class SettingsFragment extends PreferenceFragment implements
            Preference.OnPreferenceChangeListener {

        private static final String KEY_BUILD = "build";
        private static final String KEY_NUM_RECORDS = "num_records_to_display";

        private ListPreference mNumRecordsPref;

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Timber.v("onCreate");
            addPreferencesFromResource(R.xml.preferences);

            findPreference(KEY_BUILD).setSummary(BuildConfig.VERSION_NAME);

            final SharedPreferences prefs =
                    PreferenceManager.getDefaultSharedPreferences(getActivity());
            final int numRecords = prefs.getInt(PREF_NUM_RECORDS, DEFAULT_NUM_RECORDS);
            mNumRecordsPref = (ListPreference) findPreference(KEY_NUM_RECORDS);
            mNumRecordsPref.setValue(String.valueOf(numRecords));
            mNumRecordsPref.setOnPreferenceChangeListener(this);
            setRecordsPrefSummary(numRecords);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            Timber.v("onDestroy");
        }

        @Override
        public boolean onPreferenceChange(final Preference preference, final Object objValue) {
            final String key = preference.getKey();
            if (KEY_NUM_RECORDS.equals(key)) {
                final SharedPreferences prefs =
                        PreferenceManager.getDefaultSharedPreferences(preference.getContext());
                final int value = Integer.parseInt((String) objValue);

                Timber.d("%s = %d", key, value);

                prefs.edit().putInt(PREF_NUM_RECORDS, value).apply();
                setRecordsPrefSummary(value);
            }
            return true;
        }

        private void setRecordsPrefSummary(final int value) {
            final Resources res = mNumRecordsPref.getContext().getResources();
            mNumRecordsPref.setSummary(res.getQuantityString(
                    R.plurals.pref_summary_num_records, value, value));
        }
    }
}
