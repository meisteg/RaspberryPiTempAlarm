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

import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskHandle;
import com.google.appengine.api.taskqueue.TaskOptions;

import java.io.IOException;
import java.util.logging.Logger;

import static com.meiste.tempalarm.backend.OfyService.ofy;

public class TemperatureCommon {
    private static final Logger log = Logger.getLogger(TemperatureCommon.class.getName());

    public static void report(final String device, final float temperature,
                              final float humidity, final int light) throws IOException {
        // Retrieve last reported temperature from datastore
        final TemperatureRecord prevRecord = ofy().load().type(TemperatureRecord.class)
                .order("-timestamp").limit(1).first().now();
        final float prevTemp = (prevRecord != null) ? prevRecord.getFloatDegF() : Float.NaN;

        log.fine("prevTemp=" + prevTemp + ", newTemp=" + temperature +
                 ", humidity=" + humidity + ", light=" + light);

        // Check to make sure not being spammed with reports
        if ((prevRecord != null) &&
            ((System.currentTimeMillis() - prevRecord.getTimestamp()) < Constants.MINUTE_IN_MILLIS)) {
            log.warning("Received new report too soon. Ignoring.");
            return;
        }

        final TemperatureRecord record = new TemperatureRecord();
        record.setDegF(temperature);
        record.setLight(light);
        record.setHumidity(humidity);
        record.setDevice(device);
        ofy().save().entity(record);

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
                .method(TaskOptions.Method.POST);
        final TaskHandle taskHandle = QueueFactory.getDefaultQueue().add(taskOptions);
        SettingUtils.setValue(Constants.SETTING_TASK_NAME, taskHandle.getName());
    }

    public static void stop() throws IOException {
        deletePrevTask();
        Gcm.sendSensor(Gcm.SensorState.STOPPED);
        AlertEmail.sendStopped();
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
