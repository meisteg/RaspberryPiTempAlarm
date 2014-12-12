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

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskHandle;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.taskqueue.TaskOptions.Method;

import java.io.IOException;
import java.util.logging.Logger;

import javax.inject.Named;

import static com.meiste.tempalarm.backend.OfyService.ofy;

@Api(
        name = "temperature",
        version = "v1",
        namespace = @ApiNamespace(
                ownerDomain = "backend.tempalarm.meiste.com",
                ownerName = "backend.tempalarm.meiste.com",
                packagePath = ""
        )
)
public class TemperatureEndpoint {
    private static final Logger log = Logger.getLogger(TemperatureEndpoint.class.getName());

    /**
     * Query the desired reporting rate
     *
     * @return The rate the temperature should be reported to the backend
     */
    public SettingRecord getReportRate() {
        return SettingUtils.getSettingRecord(Constants.SETTING_REPORT_RATE,
                Constants.DEFAULT_REPORT_RATE);
    }

    /**
     * Report the current temperature to the backend
     *
     * @param temperature The current temperature in degrees fahrenheit
     * @param light       The current light level measured by RC circuit
     */
    public void report(@Named("temperature") final float temperature,
                       @Named("light") final int light) throws IOException {
        log.fine("temperature=" + temperature + ", light=" + light);

        final TemperatureRecord record = new TemperatureRecord();
        record.setDegF(temperature);
        record.setLight(light);
        ofy().save().entity(record).now();

        // TODO: Only send GCM on first report below threshold
        // TODO: Send email
        if (temperature < getLowTempThreshold()) {
            Gcm.sendMessage("Temperature is " + record.getDegF() + " degrees");
        }

        // Add the power outage monitor task to the default queue, removing previously
        // scheduled task (if applicable).
        deletePrevTask();
        final TaskOptions taskOptions = TaskOptions.Builder
                .withUrl("/tasks/pwr_out")
                .countdownMillis(getCountdownMillis())
                .method(Method.POST);
        final TaskHandle taskHandle = QueueFactory.getDefaultQueue().add(taskOptions);
        SettingUtils.setValue(Constants.SETTING_TASK_NAME, taskHandle.getName());
    }

    /**
     * Notifies the backend that temperature reporting has stopped.
     */
    public void stop() throws IOException {
        deletePrevTask();
        Gcm.sendMessage("Temperature reporting has stopped");
    }

    private static float getLowTempThreshold() {
        return Float.valueOf(SettingUtils.getSettingValue(Constants.SETTING_THRES_LOW,
                Constants.DEFAULT_THRES_LOW));
    }

    private static long getCountdownMillis() {
        final int rate = Integer.valueOf(SettingUtils.getSettingValue(
                Constants.SETTING_REPORT_RATE, Constants.DEFAULT_REPORT_RATE));
        return (rate * Constants.REPORTS_MISSED_BEFORE_ALARM * Constants.SECOND_IN_MILLIS) +
                Constants.MINUTE_IN_MILLIS;
    }

    private static void deletePrevTask() {
        final String prevTask = SettingUtils.getSettingValue(Constants.SETTING_TASK_NAME,
                Constants.DEFAULT_TASK_NAME);
        if (!prevTask.equals(Constants.DEFAULT_TASK_NAME)) {
            QueueFactory.getDefaultQueue().deleteTask(prevTask);
            SettingUtils.setValue(Constants.SETTING_TASK_NAME, Constants.DEFAULT_TASK_NAME);
        }
    }
}
