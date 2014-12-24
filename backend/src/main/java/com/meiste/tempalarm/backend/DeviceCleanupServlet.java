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
package com.meiste.tempalarm.backend;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.meiste.tempalarm.backend.OfyService.ofy;

public class DeviceCleanupServlet extends HttpServlet {

    private static final Logger log = Logger.getLogger(DeviceCleanupServlet.class.getSimpleName());

    @Override
    public void doGet(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {
        final String days = SettingUtils.getSettingValue(Constants.SETTING_GCM_EXPIRE,
                Constants.DEFAULT_GCM_EXPIRE);
        final long exp_timestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(Long.valueOf(days));

        log.info("Cleaning up registrations older than " + exp_timestamp);

        final StringBuilder sb = new StringBuilder();
        final List<RegistrationRecord> records =
                ofy().load().type(RegistrationRecord.class).filter("timestamp <", exp_timestamp).list();
        for (final RegistrationRecord record : records) {
            sb.append(record.toString()).append("\n\n");
            ofy().delete().entity(record).now();
        }

        final String subject = "TempAlarm: Cleaned up " + records.size() + " registrations";
        final String body = sb.toString();
        AlertEmail.sendToAdmins(subject, body);

        resp.setContentType("text/plain");
        resp.getWriter().print(subject + "\n\n" + body);
    }
}
