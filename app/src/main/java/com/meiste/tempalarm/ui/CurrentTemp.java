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

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.meiste.tempalarm.AppConstants;
import com.meiste.tempalarm.R;
import com.meiste.tempalarm.adapters.SensorAdapter;
import com.meiste.tempalarm.gcm.RegistrationService;
import com.meiste.tempalarm.util.DividerItemDecoration;

import butterknife.Bind;
import butterknife.ButterKnife;
import timber.log.Timber;

public class CurrentTemp extends AppCompatActivity {

    private static final int GPS_REQUEST = 1337;

    private Dialog mDialog;
    private SensorAdapter mAdapter;
    private Firebase mFirebase;
    private Snackbar mSnackbar;

    @Bind(R.id.temp_list)
    protected RecyclerView mRecyclerView;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.current_temp);
        ButterKnife.bind(this);

        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, 1));

        mAdapter = new SensorAdapter(this);
        mRecyclerView.setAdapter(mAdapter);

        mFirebase = new Firebase(AppConstants.FIREBASE_URL_CONNECTED);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mFirebase.addValueEventListener(mValueEventListener);

        if (checkPlayServices()) {
            // Start RegistrationService to register this application with GCM.
            final Intent intent = new Intent(this, RegistrationService.class);
            startService(intent);

            mAdapter.startSync();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, Settings.class));
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mAdapter.stopSync();
        mFirebase.removeEventListener(mValueEventListener);
    }

    @Override
    protected void onDestroy() {
        Timber.v("onDestroy");

        // Hide dialogs to prevent window leaks on orientation changes
        if ((mDialog != null) && mDialog.isShowing()) {
            mDialog.dismiss();
        }

        super.onDestroy();
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        final GoogleApiAvailability gApi = GoogleApiAvailability.getInstance();
        final int resultCode = gApi.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (gApi.isUserResolvableError(resultCode)) {
                if ((mDialog != null) && mDialog.isShowing()) {
                    mDialog.dismiss();
                }

                mDialog = gApi.getErrorDialog(this, resultCode,
                        GPS_REQUEST, new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(final DialogInterface dialog) {
                                finish();
                            }
                        });
                mDialog.show();
            } else {
                Timber.e("This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    private final ValueEventListener mValueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(final DataSnapshot snapshot) {
            final boolean connected = snapshot.getValue(Boolean.class);
            Timber.d("Firebase %sconnected", connected ? "" : "dis");

            if (connected) {
                if (mSnackbar != null) {
                    mSnackbar.dismiss();
                    mSnackbar = null;
                }
            } else {
                mSnackbar = Snackbar.make(mRecyclerView, R.string.disconnected,
                        Snackbar.LENGTH_INDEFINITE);
                mSnackbar.show();
            }
        }

        @Override
        public void onCancelled(final FirebaseError error) {
            Timber.e("Connected listener was cancelled");
        }
    };
}
