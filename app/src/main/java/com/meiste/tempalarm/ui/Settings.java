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
package com.meiste.tempalarm.ui;

import android.Manifest;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;

import com.meiste.tempalarm.BuildConfig;
import com.meiste.tempalarm.R;

import timber.log.Timber;

import static com.meiste.tempalarm.AppConstants.DEFAULT_NUM_RECORDS;
import static com.meiste.tempalarm.AppConstants.PREF_NIGHT_MODE;
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        /* If request is cancelled, the result arrays are empty. */
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Timber.v("Permission granted to ACCESS_COARSE_LOCATION");
        } else {
            Timber.e("Permission denied to ACCESS_COARSE_LOCATION");
            /*
             * This is okay because the Twilight Manager will fall back to
             * hardcoded sunrise/sunset values.
             */
        }
    }

    public static class SettingsFragment extends PreferenceFragment implements
            Preference.OnPreferenceChangeListener, SharedPreferences.OnSharedPreferenceChangeListener {

        private static final String KEY_BUILD = "build";
        private static final String KEY_NUM_RECORDS = "num_records_to_display";

        private ListPreference mNightModePref;
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

            mNightModePref = (ListPreference) findPreference(PREF_NIGHT_MODE);
            mNightModePref.setSummary(mNightModePref.getEntry());
            prefs.registerOnSharedPreferenceChangeListener(this);

            if ((Integer.valueOf(mNightModePref.getValue()) == AppCompatDelegate.MODE_NIGHT_AUTO) &&
                    (ContextCompat.checkSelfPermission(getActivity(),
                            Manifest.permission.ACCESS_COARSE_LOCATION) !=
                            PackageManager.PERMISSION_GRANTED)) {
                Timber.w("Location permission not granted. Requesting now...");

                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
            }
        }

        @Override
        public void onDestroy() {
            Timber.v("onDestroy");

            final SharedPreferences prefs =
                    PreferenceManager.getDefaultSharedPreferences(getActivity());
            prefs.unregisterOnSharedPreferenceChangeListener(this);

            super.onDestroy();
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

        @SuppressWarnings("WrongConstant")
        @Override
        public void onSharedPreferenceChanged(final SharedPreferences prefs, final String key) {
            if (PREF_NIGHT_MODE.equals(key)) {
                Timber.d("Night mode: %s", mNightModePref.getEntry());

                final int value = Integer.valueOf(mNightModePref.getValue());
                AppCompatDelegate.setDefaultNightMode(value);

                /*
                 * If the night mode is changed after any inflation, it will not
                 * take effect. In this instance, recreate() must be called.
                 */
                getActivity().recreate();
            }
        }

        private void setRecordsPrefSummary(final int value) {
            final Resources res = mNumRecordsPref.getContext().getResources();
            mNumRecordsPref.setSummary(res.getQuantityString(
                    R.plurals.pref_summary_num_records, value, value));
        }
    }
}
