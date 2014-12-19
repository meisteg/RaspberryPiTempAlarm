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
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;

import com.meiste.tempalarm.AppConstants;
import com.meiste.tempalarm.R;
import com.meiste.tempalarm.backend.temperature.Temperature;
import com.meiste.tempalarm.backend.temperature.model.CollectionResponseTemperatureRecord;
import com.meiste.tempalarm.backend.temperature.model.TemperatureRecord;
import com.meiste.tempalarm.provider.RasPiContract;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import timber.log.Timber;

public class SyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String[] PROJECTION = new String[] {
            RasPiContract.RasPiReport._ID,
            RasPiContract.RasPiReport.COLUMN_NAME_TIMESTAMP
    };

    // Constants representing column positions from PROJECTION.
    public static final int COLUMN_ID = 0;
    public static final int COLUMN_TIMESTAMP = 1;

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
        final boolean expedited = extras.getBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, false);
        Timber.i("Beginning %s sync for %s", expedited ? "immediate" : "normal", account.name);

        // TODO: Only sync if enough time has passed

        final ContentResolver contentResolver = getContext().getContentResolver();
        final ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        try {
            // TODO Remove limit of 10 on query once quotas are checked
            final CollectionResponseTemperatureRecord tempCollection = mTempService.get(10).execute();
            final List<TemperatureRecord> records = tempCollection.getItems();
            Timber.d("Downloaded %s records", records.size());

            // Build hash table of incoming entries
            final HashMap<Long, TemperatureRecord> recordMap = new HashMap<>();
            for (final TemperatureRecord record : records) {
                recordMap.put(record.getTimestamp(), record);
            }

            final Uri uri = RasPiContract.RasPiReport.CONTENT_URI;
            final Cursor c = contentResolver.query(uri, PROJECTION, null, null, null);
            Timber.d("Found %s local records. Computing merge solution...", c.getCount());

            int id;
            long timestamp;
            while (c.moveToNext()) {
                syncResult.stats.numEntries++;
                id = c.getInt(COLUMN_ID);
                timestamp = c.getLong(COLUMN_TIMESTAMP);
                final TemperatureRecord match = recordMap.get(timestamp);
                if (match != null) {
                    // Entry exists. Remove from entry map to prevent insert later.
                    recordMap.remove(timestamp);
                    // No need to check if record is updated since server will never update them
                } else {
                    // Entry doesn't exist. Remove it from the database.
                    final Uri deleteUri = RasPiContract.RasPiReport.CONTENT_URI.buildUpon()
                            .appendPath(Integer.toString(id)).build();
                    Timber.v("Scheduling delete of record %s", timestamp);
                    batch.add(ContentProviderOperation.newDelete(deleteUri).build());
                    syncResult.stats.numDeletes++;
                }
            }
            c.close();

            // Add new items
            for (final TemperatureRecord record : recordMap.values()) {
                Timber.v("Scheduling insert of record %s", record.getTimestamp());
                batch.add(ContentProviderOperation.newInsert(RasPiContract.RasPiReport.CONTENT_URI)
                        .withValue(RasPiContract.RasPiReport.COLUMN_NAME_DEGF, record.getFloatDegF())
                        .withValue(RasPiContract.RasPiReport.COLUMN_NAME_LIGHT, record.getLight())
                        .withValue(RasPiContract.RasPiReport.COLUMN_NAME_TIMESTAMP, record.getTimestamp())
                        .build());
                syncResult.stats.numInserts++;
            }

            Timber.d("Merge solution ready. Applying batch update");
            contentResolver.applyBatch(RasPiContract.CONTENT_AUTHORITY, batch);
            contentResolver.notifyChange(
                    RasPiContract.RasPiReport.CONTENT_URI, // URI where data was modified
                    null,                                  // No local observer
                    false);                                // Do not sync to network
        } catch (final IOException e) {
            Timber.e("Failed to download temperature records");
            syncResult.stats.numIoExceptions++;
        } catch (final RemoteException | OperationApplicationException e) {
            Timber.e("Failed to update database");
            syncResult.databaseError = true;
        }

        Timber.i("Network synchronization complete");
    }
}
