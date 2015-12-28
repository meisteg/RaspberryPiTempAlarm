/*
 * Copyright (C) 2015 Gregory S. Meiste  <http://gregmeiste.com>
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
package com.meiste.tempalarm.backend.service;

import com.meiste.tempalarm.backend.Constants;
import com.meiste.tempalarm.backend.SettingUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class Firebase {

    private static final String API_EXTENSION = ".json";
    private static final int TIMEOUT_MS = 10000;

    private static final Logger log = Logger.getLogger(Firebase.class.getSimpleName());

    private final String mAuthToken;
    private final Map<String, String> mQuery;

    public class Response {
        public final int code;
        public final String body;

        public Response(final int code, final String body) {
            this.code = code;
            this.body = body;
        }
    }

    public Firebase() {
        mAuthToken = SettingUtils.getSettingValue(Constants.SETTING_FB_AUTHTOKEN,
                Constants.DEFAULT_FB_AUTHTOKEN);
        mQuery = new HashMap<>();
    }

    public Response get() throws IOException {
        return get(null);
    }

    public synchronized Response get(final String path) throws IOException {
        final HttpURLConnection connection = openConnection(path);
        connection.setRequestMethod("GET");

        return processResponse(connection);
    }

    public Response post(final String json) throws IOException {
        return post(null, json);
    }

    public synchronized Response post(final String path, final String json) throws IOException {
        final HttpURLConnection connection = openConnection(path);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");

        final OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
        writer.write(json);
        writer.close();

        return processResponse(connection);
    }

    public synchronized Response delete(final String path) throws IOException {
        final HttpURLConnection connection = openConnection(path);
        connection.setDoOutput(true);
        connection.setRequestMethod("DELETE");

        return processResponse(connection);
    }

    public synchronized Firebase addQuery(final String query, final String param) {
        mQuery.put(query, param);
        return this;
    }

    private HttpURLConnection openConnection(String path) throws IOException {
        final StringBuilder sb = new StringBuilder();
        sb.append(Constants.FIREBASE_URL);

        path = (path == null) ? "" : path.trim();
        if (!path.startsWith("/")) {
            sb.append("/");
        }
        sb.append(path).append(API_EXTENSION);

        if (!mQuery.isEmpty()) {
            boolean isAmp = false;
            for (final String key : mQuery.keySet()) {
                sb.append(isAmp ? "&" : "?");
                sb.append(key).append("=").append(URLEncoder.encode(mQuery.get(key), "UTF-8"));
                isAmp = true;
            }
        }

        if (mAuthToken != null) {
            sb.append(mQuery.isEmpty() ? "?" : "&");
            sb.append("auth=").append(mAuthToken);
        }

        mQuery.clear();

        final String fullUrl = sb.toString();
        final URL url = new URL(fullUrl);

        log.fine("Opening connection to " + fullUrl);
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setConnectTimeout(TIMEOUT_MS);
        connection.setReadTimeout(TIMEOUT_MS);

        return connection;
    }

    private Response processResponse(HttpURLConnection conn) throws IOException {
        final int code = conn.getResponseCode();

        final StringBuilder sb = new StringBuilder();
        final BufferedReader reader =
                new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }

        conn.disconnect();

        return new Response(code, sb.toString());
    }
}
