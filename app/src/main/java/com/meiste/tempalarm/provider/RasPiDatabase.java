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

package com.meiste.tempalarm.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * SQLite backend for @{link RasPiProvider}.
 *
 * Provides access to an disk-backed, SQLite datastore which is utilized by PtwProvider. This
 * database should never be accessed by other parts of the application directly.
 */
public class RasPiDatabase extends SQLiteOpenHelper {
    /** Schema version. */
    public static final int DATABASE_VERSION = 1;

    /** Filename for SQLite file. */
    public static final String DATABASE_NAME = "raspi.db";

    /** SQL statement to create reports table. */
    private static final String SQL_CREATE_REPORTS =
            "CREATE TABLE " + RasPiContract.RasPiReport.TABLE_NAME + " (" +
                    RasPiContract.RasPiReport._ID + " INTEGER PRIMARY KEY," +
                    RasPiContract.RasPiReport.COLUMN_NAME_DEGF + " INTEGER," +
                    RasPiContract.RasPiReport.COLUMN_NAME_LIGHT + " INTEGER," +
                    RasPiContract.RasPiReport.COLUMN_NAME_TIMESTAMP + " INTEGER)";

    /** SQL statement to drop reports table. */
    private static final String SQL_DELETE_REPORTS =
            "DROP TABLE IF EXISTS " + RasPiContract.RasPiReport.TABLE_NAME;

    public RasPiDatabase(final Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(final SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_REPORTS);
    }

    @Override
    public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
        // Upgrade policy is to simply to discard the data and start over
        db.execSQL(SQL_DELETE_REPORTS);
        onCreate(db);
    }
}
