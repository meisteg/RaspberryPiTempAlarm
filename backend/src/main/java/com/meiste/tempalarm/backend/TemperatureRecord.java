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

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;

@Entity
@Cache
public class TemperatureRecord {

    @Id
    Long id;

    @Index
    private long timestamp;

    private float degF;
    private float humidity;

    public TemperatureRecord() {
        timestamp = System.currentTimeMillis();
    }

    public String getDegF() {
        return String.format("%.1f", degF);
    }

    public float getFloatDegF() {
        return degF;
    }

    public void setDegF(final float degF) {
        this.degF = degF;
    }

    public String getDateTimeString() {
        final Date date = new Date(timestamp);
        final DateFormat df = DateFormat.getDateTimeInstance();
        df.setTimeZone(TimeZone.getTimeZone("America/Chicago"));

        return df.format(date);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getHumidity() {
        return String.format("%.1f", humidity);
    }

    public float getFloatHumidity() {
        return humidity;
    }

    public void setHumidity(final float humidity) {
        this.humidity = humidity;
    }
}
