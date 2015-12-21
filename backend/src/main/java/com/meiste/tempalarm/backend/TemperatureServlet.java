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
package com.meiste.tempalarm.backend;

import com.google.gson.Gson;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TemperatureServlet extends HttpServlet {

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {
        final String data = req.getParameter("data");

        if (data == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        final SensorReport report = new Gson().fromJson(data, SensorReport.class);
        TemperatureCommon.report(report.tempF, report.humid);

        resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    private static class SensorReport {
        public float tempF;
        public float humid;

        public SensorReport() {
            // Needed for Gson
        }
    }
}
