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

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class PwrOutServlet extends HttpServlet {

    private static final Logger log = Logger.getLogger(PwrOutServlet.class.getSimpleName());

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
            throws IOException {
        final String thisTaskName = req.getHeader("X-AppEngine-TaskName");
        final String expectedTaskName = SettingUtils.getSettingValue(
                Constants.SETTING_TASK_NAME, Constants.DEFAULT_TASK_NAME);

        log.info("thisTaskName=" + thisTaskName + ", expected=" + expectedTaskName);

        if (thisTaskName == null || thisTaskName.equals(expectedTaskName)) {
            SettingUtils.setValue(Constants.SETTING_TASK_NAME, Constants.DEFAULT_TASK_NAME);

            Gcm.sendSensor(Gcm.SensorState.PWR_OUT);
            AlertEmail.sendPwrOut();
        } else {
            log.warning("Ignoring request since not expected task name.");
        }

        resp.setStatus(HttpServletResponse.SC_OK);
    }
}
