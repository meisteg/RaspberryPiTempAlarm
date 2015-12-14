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

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.CollectionResponse;

import java.io.IOException;
import java.util.List;

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
        TemperatureCommon.report("raspi", temperature, 0, light);
    }

    /**
     * Notifies the backend that temperature reporting has stopped.
     */
    @ApiMethod(name = "stop")
    public void stop() throws IOException {
        /* Do nothing */
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

    private static int getRecordLimit() {
        return Integer.valueOf(SettingUtils.getSettingValue(Constants.SETTING_RECORD_LIMIT,
                Constants.DEFAULT_RECORD_LIMIT));
    }
}
