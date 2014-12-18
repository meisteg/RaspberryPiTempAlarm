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

package com.meiste.tempalarm.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.meiste.tempalarm.R;
import com.meiste.tempalarm.backend.temperature.Temperature;
import com.meiste.tempalarm.backend.temperature.model.SettingRecord;
import com.meiste.tempalarm.provider.RasPiContract;

import java.io.IOException;

import timber.log.Timber;

public class SyncAdapter extends AbstractThreadedSyncAdapter {

    private final Temperature mTempService;

    /**
     * Whether there is any network connected.
     */
    private static boolean isNetworkConnected(final Context context) {
        final ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return false;
        }
        final NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return (activeNetworkInfo != null) && activeNetworkInfo.isConnected();
    }

    public static boolean requestSync(final Context context, final boolean isUserRequested) {
        if (isUserRequested && !isNetworkConnected(context)) {
            return false;
        }

        final Account account = AccountUtils.getAccount(context);
        if (account == null) {
            return false;
        }

        Timber.d("Requesting network synchronization");

        final Bundle b = new Bundle();
        b.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, isUserRequested);
        b.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, isUserRequested);
        ContentResolver.requestSync(account, RasPiContract.CONTENT_AUTHORITY, b);

        return true;
    }

    public SyncAdapter(final Context context, final boolean autoInitialize) {
        super(context, autoInitialize);
        Timber.d("Creating sync adapter");

        mTempService = new Temperature.Builder(AndroidHttp.newCompatibleTransport(),
                new AndroidJsonFactory(), AccountUtils.getCredential(context))
                .setApplicationName(context.getString(R.string.app_name))
                .build();
    }

    @Override
    public void onPerformSync(final Account account, final Bundle extras, final String authority,
                              final ContentProviderClient provider, final SyncResult syncResult) {
        Timber.d("Beginning network sync for " + account.name);

        try {
            // TODO: Download temp records. Downloading report rate for now to prove setup.
            final SettingRecord record = mTempService.temperatureEndpoint().getReportRate().execute();
            Timber.i("Report rate is %s seconds", record.getValue());
        } catch (final IOException e) {
            Timber.e("Failed to download report rate");
        }

        Timber.d("Network synchronization complete");
    }
}
