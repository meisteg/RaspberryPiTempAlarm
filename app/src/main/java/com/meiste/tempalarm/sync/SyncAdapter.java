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

import com.meiste.tempalarm.AppConstants;
import com.meiste.tempalarm.R;
import com.meiste.tempalarm.backend.temperature.Temperature;
import com.meiste.tempalarm.backend.temperature.model.CollectionResponseTemperatureRecord;
import com.meiste.tempalarm.backend.temperature.model.TemperatureRecord;
import com.meiste.tempalarm.provider.RasPiContract;

import java.io.IOException;
import java.util.List;

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

        mTempService = new Temperature.Builder(AppConstants.HTTP_TRANSPORT,
                AppConstants.JSON_FACTORY, AccountUtils.getCredential(context))
                .setApplicationName(context.getString(R.string.app_name))
                .build();
    }

    @Override
    public void onPerformSync(final Account account, final Bundle extras, final String authority,
                              final ContentProviderClient provider, final SyncResult syncResult) {
        Timber.d("Beginning network sync for " + account.name);

        try {
            // TODO: Store temperature records
            final CollectionResponseTemperatureRecord tempCollection = mTempService.get(10).execute();
            final List<TemperatureRecord> records = tempCollection.getItems();
            for (final TemperatureRecord record : records) {
                Timber.i("%s: temp=%s, light=%s", record.getTimestamp(), record.getDegF(), record.getLight());
            }
        } catch (final IOException e) {
            Timber.e("Failed to download temperature records");
        }

        Timber.d("Network synchronization complete");
    }
}
