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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.SyncStatusObserver;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.meiste.greg.gcm.GCMHelper;
import com.meiste.tempalarm.AppConstants;
import com.meiste.tempalarm.R;
import com.meiste.tempalarm.backend.registration.Registration;
import com.meiste.tempalarm.provider.RasPiContract;
import com.meiste.tempalarm.sync.AccountUtils;
import com.meiste.tempalarm.sync.SyncAdapter;

import java.io.IOException;

import butterknife.ButterKnife;
import butterknife.InjectView;
import timber.log.Timber;

public class CurrentTemp extends ActionBarActivity
        implements GCMHelper.OnGcmRegistrationListener, LoaderManager.LoaderCallbacks<Cursor> {

    private static final int GPS_REQUEST = 1337;
    private static final int ACCOUNT_PICKER_REQUEST = 1338;

    /**
     * Projection for querying the content provider.
     */
    private static final String[] PROJECTION = new String[]{
            RasPiContract.RasPiReport._ID,
            RasPiContract.RasPiReport.COLUMN_NAME_TIMESTAMP,
            RasPiContract.RasPiReport.COLUMN_NAME_DEGF,
            RasPiContract.RasPiReport.COLUMN_NAME_LIGHT,
    };

    private static final int COLUMN_TIMESTAMP = 1;
    private static final int COLUMN_DEGF = 2;
    private static final int COLUMN_LIGHT = 3;

    /**
     * List of Cursor columns to read from when preparing an adapter to populate the ListView.
     */
    private static final String[] FROM_COLUMNS = new String[]{
            RasPiContract.RasPiReport.COLUMN_NAME_TIMESTAMP,
            RasPiContract.RasPiReport.COLUMN_NAME_DEGF,
            RasPiContract.RasPiReport.COLUMN_NAME_LIGHT,
    };

    /**
     * List of Views which will be populated by Cursor data.
     */
    private static final int[] TO_FIELDS = new int[]{
            R.id.timestamp,
            R.id.degF,
            R.id.lights,
    };

    private Dialog mDialog;
    private Registration mRegService;
    private Object mSyncObserverHandle;
    private Menu mOptionsMenu;
    private SimpleCursorAdapter mAdapter;
    private int mLightThreshold;

    @InjectView(R.id.temp_list)
    protected ListView mListView;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.current_temp);
        ButterKnife.inject(this);

        mAdapter = new SimpleCursorAdapter(this, R.layout.record, null, FROM_COLUMNS, TO_FIELDS, 0);
        mAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(final View view, final Cursor cursor, final int columnIndex) {
                final TextView textView = (TextView) view;
                switch (columnIndex) {
                    case COLUMN_TIMESTAMP:
                        // Convert timestamp to human-readable date
                        textView.setText(DateUtils.formatDateTime(getApplicationContext(),
                                cursor.getLong(columnIndex), AppConstants.DATE_FORMAT_FLAGS));
                        return true;
                    case COLUMN_DEGF:
                        // Restrict to one decimal place
                        textView.setText(String.format("%.1f", cursor.getFloat(columnIndex)));
                        return true;
                    case COLUMN_LIGHT:
                        if (cursor.getInt(columnIndex) < mLightThreshold) {
                            textView.setText(getText(R.string.lights_on));
                        } else {
                            textView.setText(getText(R.string.lights_off));
                        }
                        return true;
                }
                return false;
            }
        });
        mListView.setAdapter(mAdapter);
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSyncStatusObserver.onStatusChanged(0);

        if (checkPlayServices()) {
            final GoogleAccountCredential credential = AccountUtils.getCredential(this);
            if (credential.getSelectedAccountName() != null) {
                GCMHelper.registerIfNeeded(getApplicationContext(), AppConstants.GCM_SENDER_ID, this);
            } else {
                startActivityForResult(credential.newChooseAccountIntent(), ACCOUNT_PICKER_REQUEST);
            }

            // Watch for sync state changes
            final int mask = ContentResolver.SYNC_OBSERVER_TYPE_PENDING |
                    ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE;
            mSyncObserverHandle = ContentResolver.addStatusChangeListener(mask, mSyncStatusObserver);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        mOptionsMenu = menu;
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                // TODO: Launch settings activity
                return true;
            case R.id.action_refresh:
                if (!SyncAdapter.requestSync(this, true)) {
                    Toast.makeText(this, R.string.error_refresh, Toast.LENGTH_LONG).show();
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mSyncObserverHandle != null) {
            ContentResolver.removeStatusChangeListener(mSyncObserverHandle);
            mSyncObserverHandle = null;
        }
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
                        Timber.d("User selected " + accountName);
                        AccountUtils.setAccount(this, accountName);
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
                Timber.e("This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    @Override
    public boolean onSendRegistrationIdToBackend(final Context context, final String regId)
            throws IOException {
        Timber.d("Device registered with GCM: regId = " + regId);

        if (mRegService == null) {
            mRegService = new Registration.Builder(AppConstants.HTTP_TRANSPORT,
                    AppConstants.JSON_FACTORY, AccountUtils.getCredential(this))
                    .setApplicationName(context.getString(R.string.app_name))
                    .build();
        }

        mRegService.register(regId).execute();
        return true;
    }

    /**
     * Set the state of the Refresh button. If a sync is active, turn on the ProgressBar widget.
     * Otherwise, turn it off.
     *
     * @param refreshing True if an active sync is occurring, false otherwise
     */
    public void setRefreshActionButtonState(final boolean refreshing) {
        if (mOptionsMenu == null) {
            return;
        }

        final MenuItem refreshItem = mOptionsMenu.findItem(R.id.action_refresh);
        if (refreshItem != null) {
            if (refreshing) {
                refreshItem.setActionView(R.layout.actionbar_indeterminate_progress);
            } else {
                refreshItem.setActionView(null);
            }
        }
    }

    /**
     * Create a new anonymous SyncStatusObserver. It's attached to the app's ContentResolver in
     * onResume(), and removed in onPause(). If status changes, it sets the state of the Refresh
     * button. If a sync is active or pending, the Refresh button is replaced by an indeterminate
     * ProgressBar; otherwise, the button itself is displayed.
     */
    private final SyncStatusObserver mSyncStatusObserver = new SyncStatusObserver() {
        /** Callback invoked when the sync adapter status changes. */
        @Override
        public void onStatusChanged(final int which) {
            runOnUiThread(new Runnable() {
                /**
                 * The SyncAdapter runs on a background thread. To update the UI, onStatusChanged()
                 * runs on the UI thread.
                 */
                @Override
                public void run() {
                    final Account account = AccountUtils.getAccount(getApplicationContext());
                    if (account == null) {
                        // This shouldn't happen, but set the status to "not refreshing".
                        setRefreshActionButtonState(false);
                        return;
                    }

                    // Test the ContentResolver to see if the sync adapter is active or pending.
                    // Set the state of the refresh button accordingly.
                    final boolean syncActive = ContentResolver.isSyncActive(
                            account, RasPiContract.CONTENT_AUTHORITY);
                    final boolean syncPending = ContentResolver.isSyncPending(
                            account, RasPiContract.CONTENT_AUTHORITY);
                    setRefreshActionButtonState(syncActive || syncPending);
                }
            });
        }
    };

    /**
     * Query the content provider for data.
     *
     * <p>Loaders do queries in a background thread. They also provide a ContentObserver that is
     * triggered when data in the content provider changes. When the sync adapter updates the
     * content provider, the ContentObserver responds by resetting the loader and then reloading
     * it.
     */
    @Override
    public Loader<Cursor> onCreateLoader(final int id, final Bundle bundle) {
        return new CursorLoader(
                this,                                        // Context
                RasPiContract.RasPiReport.CONTENT_URI,       // URI
                PROJECTION,                                  // Projection
                null,                                        // Selection
                null,                                        // Selection args
                RasPiContract.RasPiReport.SORT_NEWEST_FIRST  // Sort
        );
    }

    /**
     * Move the Cursor returned by the query into the ListView adapter. This refreshes the existing
     * UI with the data in the Cursor.
     */
    @Override
    public void onLoadFinished(final Loader<Cursor> loader, final Cursor cursor) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mLightThreshold = prefs.getInt(AppConstants.PREF_THRES_LIGHT, 0);

        mAdapter.changeCursor(cursor);
    }

    /**
     * Called when the ContentObserver defined for the content provider detects that data has
     * changed. The ContentObserver resets the loader, and then re-runs the loader. In the adapter,
     * set the Cursor value to null. This removes the reference to the Cursor, allowing it to be
     * garbage-collected.
     */
    @Override
    public void onLoaderReset(final Loader<Cursor> loader) {
        mAdapter.changeCursor(null);
    }
}
