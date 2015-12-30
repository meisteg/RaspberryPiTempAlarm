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
package com.meiste.tempalarm.items;

import android.content.Context;
import android.text.format.DateUtils;

import com.meiste.tempalarm.AppConstants;

public class SensorData {

    public long timestamp;
    public float degF;
    public float humidity;

    public SensorData() {
        /* Empty default constructor, required by Firebase */
    }

    public String getTime(final Context context) {
        return DateUtils.formatDateTime(context, timestamp, AppConstants.DATE_FORMAT_FLAGS);
    }

    public String getDegF() {
        return String.format("%.1f", degF);
    }

    public String getHumidity() {
        return String.format("%.1f %%", humidity);
    }

    @Override
    public boolean equals(final Object o) {
        if ((o != null) && (o instanceof SensorData)) {
            final SensorData sd = (SensorData) o;
            return (timestamp == sd.timestamp) &&
                    (degF == sd.degF) &&
                    (humidity == sd.humidity);
        }
        return false;
    }

    @Override
    public String toString() {
        return "[ time=" + timestamp + ", degF=" + degF + ", humidity=" + humidity + " ]";
    }
}
