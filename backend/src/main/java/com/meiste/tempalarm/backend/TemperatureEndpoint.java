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

    private static final int RECORD_LIMIT = 120;

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
        /*
         * The sensor does not get the report rate from the server any longer,
         * so the server does not know what rate the sensor will use. The app
         * still calls this API, so return a sane value to it.
         *
         * A hard coded value is returned instead of one read from the datastore
         * to reduce datastore quota usage.
         */
        final SettingRecord setting = new SettingRecord();
        setting.setName(Constants.SETTING_REPORT_RATE);
        setting.setValue(Constants.DEFAULT_REPORT_RATE);
        return setting;
    }

    /**
     * Return a collection of temperature data
     *
     * @param count The number of temperature records to return (or 0 for max)
     * @return a list of temperature records
     */
    @ApiMethod(name = "get")
    public CollectionResponse<TemperatureRecord> listRecords(@Named("count") final int count) {
        int limit = RECORD_LIMIT;
        if ((count > 0) && (count < limit)) {
            limit = count;
        }
        final List<TemperatureRecord> records = ofy().load().type(TemperatureRecord.class)
                .order("-timestamp").limit(limit).list();
        return CollectionResponse.<TemperatureRecord>builder().setItems(records).build();
    }
}
