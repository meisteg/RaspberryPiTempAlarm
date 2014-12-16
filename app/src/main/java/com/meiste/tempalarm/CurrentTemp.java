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

import android.accounts.AccountManager;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.meiste.greg.gcm.GCMHelper;
import com.meiste.tempalarm.backend.registration.Registration;
import com.meiste.tempalarm.sync.AccountUtils;

import java.io.IOException;

public class CurrentTemp extends ActionBarActivity implements GCMHelper.OnGcmRegistrationListener {

    private static final String TAG = CurrentTemp.class.getSimpleName();

    private static final int GPS_REQUEST = 1337;
    private static final int ACCOUNT_PICKER_REQUEST = 1338;

    private Dialog mDialog;
    private GoogleAccountCredential mCredential;
    private Registration mRegService;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.current_temp);

        mCredential = GoogleAccountCredential.usingAudience(getApplicationContext(),
                AppContants.CLIENT_AUDIENCE);
        mCredential.setSelectedAccountName(AccountUtils.getAccountName(this));
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (checkPlayServices()) {
            if (mCredential.getSelectedAccountName() != null) {
                GCMHelper.setRegisterOnServerLifespan(getApplicationContext(),
                        BuildConfig.DEBUG ? 0 : GCMHelper.DEFAULT_ON_SERVER_LIFESPAN_MS);
                GCMHelper.registerIfNeeded(getApplicationContext(), AppContants.GCM_SENDER_ID, this);
            } else {
                startActivityForResult(mCredential.newChooseAccountIntent(), ACCOUNT_PICKER_REQUEST);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_current_temp, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        final int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        // Hide dialogs to prevent window leaks on orientation changes
        if ((mDialog != null) && mDialog.isShowing()) {
            mDialog.dismiss();
        }

        super.onDestroy();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case ACCOUNT_PICKER_REQUEST:
                if ((data != null) && (data.getExtras() != null)) {
                    final String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (!TextUtils.isEmpty(accountName)) {
                        Log.d(TAG, "User selected " + accountName);
                        AccountUtils.setAccount(this, accountName);
                        mCredential.setSelectedAccountName(accountName);
                    }
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        final int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                if ((mDialog != null) && mDialog.isShowing()) {
                    mDialog.dismiss();
                }

                mDialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        GPS_REQUEST, new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(final DialogInterface dialog) {
                                finish();
                            }
                        });
                mDialog.show();
            } else {
                Log.e(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    @Override
    public boolean onSendRegistrationIdToBackend(Context context, String regId) throws IOException {
        Log.d(TAG, "Device registered with GCM: regId = " + regId);

        if (mRegService == null) {
            mRegService = new Registration.Builder(AndroidHttp.newCompatibleTransport(),
                    new AndroidJsonFactory(), mCredential).build();
        }

        mRegService.register(regId).execute();
        return true;
    }
}
