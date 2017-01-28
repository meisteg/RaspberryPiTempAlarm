/*
 * Copyright (C) 2017 Gregory S. Meiste  <http://gregmeiste.com>
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

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SensorDataTest {

    private static final long TEST_TIMESTAMP = 1234567890;
    private static final float TEST_DEG_F = 56.78f;
    private static final float TEST_HUMIDITY = 12.34f;

    private SensorData mSensorData;

    @Before
    public void setUp() throws Exception {
        mSensorData = new SensorData();
        mSensorData.timestamp = TEST_TIMESTAMP;
        mSensorData.degF = TEST_DEG_F;
        mSensorData.humidity = TEST_HUMIDITY;
    }

    /* Verify getDegF returns tenths of a degree. */
    @Test
    public void getDegF() throws Exception {
        assertEquals("56.8", mSensorData.getDegF());
    }

    /* Verify getHumidity returns tenths of a degree and adds percent sign. */
    @Test
    public void getHumidity() throws Exception {
        assertEquals("12.3 %", mSensorData.getHumidity());
    }

    /* Verify equals only returns true when all member variables match. */
    @Test
    public void equals() throws Exception {
        final SensorData sensorData = new SensorData();
        assertFalse(mSensorData.equals(sensorData));

        sensorData.timestamp = TEST_TIMESTAMP;
        assertFalse(mSensorData.equals(sensorData));

        sensorData.degF = TEST_DEG_F;
        assertFalse(mSensorData.equals(sensorData));

        sensorData.humidity = TEST_HUMIDITY;
        assertTrue(mSensorData.equals(sensorData));
    }
}