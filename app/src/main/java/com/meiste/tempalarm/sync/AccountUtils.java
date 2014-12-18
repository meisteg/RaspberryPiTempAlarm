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
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.meiste.tempalarm.AppContants;
import com.meiste.tempalarm.provider.RasPiContract;

public class AccountUtils {

    private static GoogleAccountCredential sCredential;

    public static synchronized GoogleAccountCredential getCredential(final Context context) {
        if (sCredential == null) {
            sCredential = GoogleAccountCredential.usingAudience(context.getApplicationContext(),
                    AppContants.CLIENT_AUDIENCE);
            sCredential.setSelectedAccountName(AccountUtils.getAccountName(context));
        }
        return sCredential;
    }

    public static synchronized String getAccountName(final Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String account = prefs.getString(AppContants.PREF_ACCOUNT_NAME, null);

        if (TextUtils.isEmpty(account)) {
            // Account not setup at all
            return null;
        }

        final AccountManager mgr = AccountManager.get(context);
        final Account[] accts = mgr.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
        for (final Account acct : accts) {
            if (acct.name.equals(account)) {
                // Account setup and found on system
                return account;
            }
        }

        // Account setup, but no longer present on system
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString(AppContants.PREF_ACCOUNT_NAME, null);
        editor.apply();

        return null;
    }

    public static synchronized Account getAccount(final Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String account = prefs.getString(AppContants.PREF_ACCOUNT_NAME, null);
        final AccountManager mgr = AccountManager.get(context);
        final Account[] accounts = mgr.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);

        for (final Account acct : accounts) {
            if (acct.name.equals(account)) {
                return acct;
            }
        }

        return null;
    }

    public static synchronized void setAccount(final Context context, final String accountName) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String oldAccountName = prefs.getString(AppContants.PREF_ACCOUNT_NAME, null);

        if (!TextUtils.isEmpty(oldAccountName)) {
            final Account oldAccount = getAccount(context);
            ContentResolver.setIsSyncable(oldAccount, RasPiContract.CONTENT_AUTHORITY, 0);
            ContentResolver.setSyncAutomatically(oldAccount, RasPiContract.CONTENT_AUTHORITY, false);
        }

        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString(AppContants.PREF_ACCOUNT_NAME, accountName);
        editor.apply();

        final Account account = getAccount(context);

        // Inform the system that this account supports sync
        ContentResolver.setIsSyncable(account, RasPiContract.CONTENT_AUTHORITY, 1);
        // Inform the system that this account is eligible for auto sync when the network is up
        ContentResolver.setSyncAutomatically(account, RasPiContract.CONTENT_AUTHORITY, true);
        // Recommend a schedule for automatic synchronization. The system may modify this based
        // on other scheduled syncs and network utilization.
        ContentResolver.addPeriodicSync(account, RasPiContract.CONTENT_AUTHORITY,
                new Bundle(), AppContants.AUTO_SYNC_FREQUENCY);

        if (sCredential != null) {
            sCredential.setSelectedAccountName(accountName);
        }
    }
}
