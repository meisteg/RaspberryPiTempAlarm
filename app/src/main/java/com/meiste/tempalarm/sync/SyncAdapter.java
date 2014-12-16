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

import com.meiste.tempalarm.Log;
import com.meiste.tempalarm.provider.RasPiContract;

public class SyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String TAG = SyncAdapter.class.getSimpleName();

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

        Log.d(TAG, "Requesting network synchronization");

        final Bundle b = new Bundle();
        b.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, isUserRequested);
        b.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, isUserRequested);
        ContentResolver.requestSync(account, RasPiContract.CONTENT_AUTHORITY, b);

        return true;
    }

    public SyncAdapter(final Context context, final boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(final Account account, final Bundle extras, final String authority,
                              final ContentProviderClient provider, final SyncResult syncResult) {
        Log.d(TAG, "Beginning network sync for " + account.name);

        // TODO

        Log.d(TAG, "Network synchronization complete");
    }
}
