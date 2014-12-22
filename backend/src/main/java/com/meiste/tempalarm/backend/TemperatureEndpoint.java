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
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskHandle;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.taskqueue.TaskOptions.Method;

import java.io.IOException;
import java.util.List;
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
    @ApiMethod(
            name = "getReportRate",
            path = "getReportRate"
    )
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
    @ApiMethod(name = "report")
    public void report(@Named("temperature") final float temperature,
                       @Named("light") final int light) throws IOException {
        // Retrieve last reported temperature from datastore
        final TemperatureRecord prevRecord = ofy().load().type(TemperatureRecord.class)
                .order("-timestamp").limit(1).first().now();
        final float prevTemp = (prevRecord != null) ? prevRecord.getFloatDegF() : Float.NaN;

        log.fine("prevTemp=" + prevTemp + ", newTemp=" + temperature + ", light=" + light);

        final TemperatureRecord record = new TemperatureRecord();
        record.setDegF(temperature);
        record.setLight(light);
        ofy().save().entity(record).now();

        final float lowTempThreshold = getLowTempThreshold();
        if (!isTaskRunning()) {
            Gcm.sendSensor(Gcm.SensorState.RUNNING);
            AlertEmail.sendRunning(record.getDegF());
        } else if ((temperature < lowTempThreshold) && (prevTemp >= lowTempThreshold)) {
            Gcm.sendAlarm(Gcm.AlarmState.TEMP_TOO_LOW);
            AlertEmail.sendLowTemp(record.getDegF());
        } else if ((temperature >= lowTempThreshold) && (prevTemp < lowTempThreshold)) {
            Gcm.sendAlarm(Gcm.AlarmState.TEMP_NORMAL);
            AlertEmail.sendNormTemp(record.getDegF());
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
    @ApiMethod(name = "stop")
    public void stop() throws IOException {
        deletePrevTask();
        Gcm.sendSensor(Gcm.SensorState.STOPPED);
        AlertEmail.sendStopped();
    }

    /**
     * Return a collection of temperature data
     *
     * @param count The number of temperature records to return (or 0 for max)
     * @return a list of temperature records
     */
    @ApiMethod(name = "get")
    public CollectionResponse<TemperatureRecord> listRecords(@Named("count") final int count) {
        int limit = getRecordLimit();
        if ((count > 0) && (count < limit)) {
            limit = count;
        }
        final List<TemperatureRecord> records = ofy().load().type(TemperatureRecord.class)
                .order("-timestamp").limit(limit).list();
        return CollectionResponse.<TemperatureRecord>builder().setItems(records).build();
    }

    /**
     * Query the light threshold setting
     *
     * @return The threshold value for lights on/off
     */
    @ApiMethod(
            name = "getLightThreshold",
            path = "getLightThreshold"
    )
    public SettingRecord getLightThreshold() {
        return SettingUtils.getSettingRecord(Constants.SETTING_THRES_LIGHT,
                Constants.DEFAULT_THRES_LIGHT);
    }

    private static float getLowTempThreshold() {
        return Float.valueOf(SettingUtils.getSettingValue(Constants.SETTING_THRES_LOW,
                Constants.DEFAULT_THRES_LOW));
    }

    private static int getRecordLimit() {
        return Integer.valueOf(SettingUtils.getSettingValue(Constants.SETTING_RECORD_LIMIT,
                Constants.DEFAULT_RECORD_LIMIT));
    }

    private static long getCountdownMillis() {
        final int rate = Integer.valueOf(SettingUtils.getSettingValue(
                Constants.SETTING_REPORT_RATE, Constants.DEFAULT_REPORT_RATE));
        return (rate * Constants.REPORTS_MISSED_BEFORE_ALARM * Constants.SECOND_IN_MILLIS) +
                Constants.MINUTE_IN_MILLIS;
    }

    private static boolean isTaskRunning() {
        final String prevTask = SettingUtils.getSettingValue(Constants.SETTING_TASK_NAME,
                Constants.DEFAULT_TASK_NAME);
        return !prevTask.equals(Constants.DEFAULT_TASK_NAME);
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
