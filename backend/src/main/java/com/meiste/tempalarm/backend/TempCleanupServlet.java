/*
 * Copyright (C) 2014-2015 Gregory S. Meiste  <http://gregmeiste.com>
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
package com.meiste.tempalarm.backend;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.googlecode.objectify.Key;
import com.meiste.tempalarm.backend.service.Firebase;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.meiste.tempalarm.backend.OfyService.ofy;

public class TempCleanupServlet extends HttpServlet {

    private static final Logger log = Logger.getLogger(TempCleanupServlet.class.getSimpleName());

    @Override
    public void doGet(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {
        final String days = SettingUtils.getSettingValue(Constants.SETTING_RECORD_EXPIRE,
                Constants.DEFAULT_RECORD_EXPIRE);
        final long exp_timestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(Long.valueOf(days));

        resp.setContentType("text/plain");
        print(Level.INFO, resp, "Cleaning up temperature records older than " + exp_timestamp);

        final List<Key<TemperatureRecord>> keys =
                ofy().load().type(TemperatureRecord.class).filter("timestamp <", exp_timestamp).keys().list();
        ofy().delete().keys(keys);

        print(Level.INFO, resp, "Cleaned up " + keys.size() + " local records");

        final Firebase firebase = new Firebase();
        firebase.addQuery("orderBy", "\"timestamp\"")
                .addQuery("endAt", String.valueOf(exp_timestamp));
        Firebase.Response response = firebase.get();
        if ((response.code == HttpURLConnection.HTTP_OK) && (response.body != null)) {
            final Type mapType = new TypeToken<Map<String, TemperatureRecord>>(){}.getType();
            final Map<String, TemperatureRecord> map = new Gson().fromJson(response.body, mapType);
            int deleted = 0;

            for (final String key : map.keySet()) {
                try {
                    response = firebase.delete(key);
                    if ((response.code == HttpURLConnection.HTTP_OK) ||
                            (response.code == HttpURLConnection.HTTP_NO_CONTENT)) {
                        deleted++;
                    }
                } catch (final IOException e) {
                    /* Ignore error and keep looping */
                }
            }

            /* Check if all records were successfully deleted */
            if (deleted == map.size()) {
                print(Level.INFO, resp, "Cleaned up " + map.size() + " Firebase records");
            } else {
                print(Level.SEVERE, resp, "Only cleaned up " + deleted + " of " + map.size() +
                        " Firebase records");
            }
        } else {
            print(Level.SEVERE, resp, "Failed to get Firebase records to delete!");
        }
    }

    private void print(final Level level, final HttpServletResponse resp, final String msg)
            throws IOException {
        log.log(level, msg);
        resp.getWriter().println(msg);
    }
}
