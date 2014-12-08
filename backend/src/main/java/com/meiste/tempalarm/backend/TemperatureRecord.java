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

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

@Entity
public class TemperatureRecord {

    @Id
    Long id;

    @Index
    private long timestamp;

    @Index
    private float degF;

    public TemperatureRecord() {
        timestamp = System.currentTimeMillis();
    }

    public float getDegF() {
        return degF;
    }

    public void setDegF(final float degF) {
        this.degF = degF;
    }

    public String getRelativeTimeSpanString() {
        final long delta = System.currentTimeMillis() - timestamp;

        if (delta < Constants.HOUR_IN_MILLIS) {
            return (delta / Constants.MINUTE_IN_MILLIS) + " minute(s) ago";
        } else if (delta < Constants.DAY_IN_MILLIS) {
            return (delta / Constants.HOUR_IN_MILLIS) + " hour(s) ago";
        }
        return (delta / Constants.DAY_IN_MILLIS) + " day(s) ago";
    }
}