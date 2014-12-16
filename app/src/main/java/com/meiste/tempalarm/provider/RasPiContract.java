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

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Field and table name constants for {@link RasPiProvider}.
 */
public class RasPiContract {
    private RasPiContract() {
    }

    public static final String CONTENT_AUTHORITY = "com.meiste.tempalarm";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static class RasPiReport implements BaseColumns {

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.raspi.reports";

        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.raspi.report";

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath("races").build();

        public static final String TABLE_NAME = "reports";

        public static final String COLUMN_NAME_DEGF = "degF";
        public static final String COLUMN_NAME_LIGHT = "light";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";

        public static final int COLUMN_ID = 0;
        public static final int COLUMN_DEGF= 1;
        public static final int COLUMN_LIGHT = 2;
        public static final int COLUMN_TIMESTAMP = 3;

        public static final String SORT_OLDEST_FIRST = COLUMN_NAME_TIMESTAMP + " ASC";
        public static final String SORT_NEWEST_FIRST = COLUMN_NAME_TIMESTAMP + " DESC";
    }
}